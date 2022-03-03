/*
 * Copyright 2022 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.initializer

import com.couchbase.initializer.util.executable
import com.couchbase.initializer.util.permissions
import com.couchbase.initializer.util.rwxr_xr_x
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.Mustache
import com.github.mustachejava.MustacheFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.text.StringEscapeUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamSource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.io.*
import java.nio.file.Paths
import java.util.function.Function
import javax.servlet.http.HttpServletResponse
import kotlin.io.path.absolute
import kotlin.text.Charsets.UTF_8

fun File.readJsonObject(): ObjectNode {
    try {
        FileInputStream(this).use {
            return jsonMapper.readTree(it) as ObjectNode
        }
    } catch (e: FileNotFoundException) {
        throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
}

private val validPath = Regex("[a-z][a-z0-9\\-]*(/[a-z][a-z0-9\\-]*)*")

@Controller
@ResponseBody
@CrossOrigin
class InitializerController(
    @Value("classpath:manifest.json") manifestResource: Resource,
) {
    val manifest = manifestResource.readString()

    @GetMapping("/manifest.json", produces = ["application/json"])
    fun manifest(): String = manifest

    @GetMapping("/project/{*path}")
    fun project(
        @PathVariable("path") origPath: String,
    ): ObjectNode {
        val path = origPath.removePrefix("/").removeSuffix("/")
        if (!path.matches(validPath)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }

        val root = File("src/templates")

        val projectDir = File(root, path)
        if (!projectDir.exists()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }

        val params = File("$projectDir/parameters.json")
        if (!params.exists()) {
            return jsonMapper.convertValue(mapOf("sections" to emptyList<Any>()), ObjectNode::class.java)
        }

        return params.readJsonObject()
    }

    @RequestMapping(method = [RequestMethod.GET, RequestMethod.POST], path=["/download/{*path}"])
//    @PostMapping("/download/{*path}")
//    @GetMapping("/download/{*path}")
    fun download(
        @PathVariable("path") origPath: String,
        @RequestParam params: Map<String, String>,
        response: HttpServletResponse,
    ): Unit {
        val path = origPath.removePrefix("/").removeSuffix("/")
        if (!path.matches(validPath)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }

        val template = path

        val address = params.getOrDefault("address", "127.0.0.1")
        val tls = params.getOrDefault("tls", "false") == "true" || address.endsWith(".cloud.couchbase.com")
        val connectionStringScheme = if (tls) "couchbases" else "couchbase"
        val connectionString = "$connectionStringScheme://$address"

        val defaults = mapOf(
            "address" to "127.0.0.1",
            "package" to "com.example.demo",
            "packageSeparator" to ".",
            "username" to "Administrator",
            "password" to "password",
            "connectionString" to connectionString,

            "sdkVersion" to "3.2.5", // todo read this from common.properties

            "group" to "com.example",
            "artifact" to "demo",
            "javaVersion" to "11",
            "name" to "demo",
            "description" to "It's a demo project!",
        )

        val scope = defaults.toMutableMap()
        scope.putAll(params)

        val archiveFilename = File(template).name + ".zip"
        response.setHeader("Content-Type", "application/zip")
        response.setHeader("Content-Disposition", "attachment; filename=\"$archiveFilename\"")


        val templateDir = "src/templates/$template"
        val packageSeparator = scope["packageSeparator"]!!
        val packageAsPathComponents = scope["package"]!!.replace(packageSeparator, "/")
        val processExtensions = setOf("md", "adoc", "java", "xml", "json", "properties", "gradle")

        val root = Paths.get(templateDir).absolute().toString()
        val mixinsRoot = File("$root/../").canonicalFile.path

        val zip = ZipArchiveOutputStream(response.outputStream)
        try {

            val directoriesToCopy = mutableListOf<String>()

            val mixinsFile = File(templateDir, "mixins.json")
            if (mixinsFile.exists()) {
                FileInputStream(mixinsFile).use {
                    val mixins: List<String> = jsonMapper.readValue(it, jacksonTypeRef())
                    mixins.forEach { mixin -> directoriesToCopy.add("$mixinsRoot/$mixin") }
                }
            }

            directoriesToCopy.add(root)

            directoriesToCopy.forEach {
                zip.addDirectory(File("$it/files").canonicalPath, packageAsPathComponents, processExtensions, scope)
            }

        } finally {
            zip.close()
        }
    }

    private fun ZipArchiveOutputStream.addDirectory(
        root: String,
        packageAsPathComponents: String,
        processExtensions: Set<String>,
        scope: Map<String, String>,
    ) {
        val zip = this
        File(root).walk().forEach { file ->
            if (!file.isFile) return@forEach

            val entryName = file.path
                .removePrefix(root)
                .removePrefix("/")
                .replace("com/example/demo", packageAsPathComponents)

            val archiveEntry = ZipArchiveEntry(entryName)

            file.permissions?.let { perms ->
                // rather than copy the perms, just set the executable bits
                if (perms.executable) archiveEntry.unixMode = rwxr_xr_x
            }

            zip.putArchiveEntry(archiveEntry)

            //                val content = Files.readAllBytes(file.toPath()).toString(UTF_8)
            //                    .replace("com.example.demo", packageName)
            //                val mustache = mf.compile(StringReader(content), entryName)


            if (file.extensionMatches(processExtensions)) {
                val escaper = escapers[file.extensionOrFilename] ?: defaultEscaper

                val mf: MustacheFactory = object : DefaultMustacheFactory(File(root)) {
                    override fun encode(value: String, writer: Writer) = writer.write(escaper.apply(value))
                }

                val mustache = mf.compile(entryName)
                mustache.execute(zip, scope)
            } else {
                file.copyTo(zip)
            }

            zip.closeArchiveEntry()
        }
    }
}

private fun InputStreamSource.readString(): String {
    inputStream.use {
        return String(it.readAllBytes(), UTF_8)
    }
}

val escapeXml = Function<String, String> { StringEscapeUtils.escapeXml10(it) }
val escapeJava = Function<String, String> { StringEscapeUtils.escapeJava(it) }
val escapeEcmaScript = Function<String, String> { StringEscapeUtils.escapeEcmaScript(it) }
val escapeHtml = Function<String, String> { StringEscapeUtils.escapeHtml4(it) }
val escapeNone = Function<String, String> { it }

val escapers = mapOf(
    "xml" to escapeXml,
    "java" to escapeJava,
    "js" to escapeEcmaScript, // Javascript
    "ts" to escapeEcmaScript, // Typescript
    "html" to escapeHtml,
    "properties" to escapeNone,

    // plaintext formats
    "md" to escapeNone,
    "adoc" to escapeNone,
    "txt" to escapeNone,
    "README" to escapeNone,
)
val defaultEscaper = escapeJava // Good enough for most? We'll deal with exceptions as they arise.

private fun Mustache.execute(os: OutputStream, scope: Any) {
    val w = OutputStreamWriter(os, UTF_8)
    execute(w, scope)
    w.flush()
}

private fun File.copyTo(os: OutputStream): Long = inputStream().use { it.copyTo(os) }

private fun File.extensionMatches(allowedExtensions: Set<String>): Boolean {
    return allowedExtensions.contains(extensionOrFilename)
}

val File.extensionOrFilename: String
    get() = extension.ifEmpty { name }
