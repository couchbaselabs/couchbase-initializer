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
import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.Mustache
import com.github.mustachejava.MustacheFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.text.StringEscapeUtils
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.io.File
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.file.Paths
import java.util.function.Function
import javax.servlet.http.HttpServletResponse
import kotlin.io.path.absolute
import kotlin.text.Charsets.UTF_8

@Controller
@ResponseBody
class InitializerController {
    @GetMapping("/download")
    fun sayHello(
        @RequestParam(name = "name", required = false, defaultValue = "Stranger")
        name: String,

        @RequestParam(name = "package", required = false, defaultValue = "com.example.demo")
        packageName: String,

        response: HttpServletResponse,
    ): Unit {
        val archiveFilename = "project.zip"
        response.setHeader("Content-Type", "application/zip")
        response.setHeader("Content-Disposition", "attachment; filename=\"$archiveFilename\"")

        val processExtensions = setOf("md", "adoc", "java", "xml", "json", "properties")

        val templateDir = "src/templates/java/maven"

        val packageAsPathComponents = packageName.replace(".", "/")

        val root = Paths.get(templateDir).absolute().toString() + "/"

        val zip = ZipArchiveOutputStream(response.outputStream)
        try {
            File(root).walk().forEach { file ->
                if (!file.isFile) return@forEach

                val entryName = file.path.removePrefix(root)
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

                val scope = mapOf(
                    "package" to packageName,
                    "meta.group" to "com.example",
                    "meta.artifact" to "demo",
                    "meta.javaVersion" to "11",
                    "meta.name" to "demo",
                    "meta.description" to "It's a demo project!",

                    "name" to name,
                )

                if (file.extensionMatches(processExtensions)) {
                    val escaper = escapers[file.extensionOrFilename] ?: defaultEscaper

                    val mf: MustacheFactory = object : DefaultMustacheFactory(File(templateDir)) {
                        override fun encode(value: String, writer: Writer) = writer.write(escaper.apply(value))
                    }

                    val mustache = mf.compile(entryName)
                    mustache.execute(zip, scope)
                } else {
                    file.copyTo(zip)
                }

                zip.closeArchiveEntry()
            }
        } finally {
            zip.close()
        }
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
