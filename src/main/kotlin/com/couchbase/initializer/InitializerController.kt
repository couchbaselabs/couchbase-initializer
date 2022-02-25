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

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.Mustache
import com.github.mustachejava.MustacheFactory
import com.github.mustachejava.util.HtmlEscaper
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.io.*
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
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

        val mf: MustacheFactory = DefaultMustacheFactory(File(templateDir))

        val zip = ZipOutputStream(response.outputStream)
        try {
            File(root).walk().forEach { file ->
                if (!file.isFile) return@forEach

                val entryName = file.path.removePrefix(root)
                    .replace("com/example/demo", packageAsPathComponents)

                zip.putNextEntry(ZipEntry(entryName))

//                val content = Files.readAllBytes(file.toPath()).toString(UTF_8)
//                    .replace("com.example.demo", packageName)
//                val mustache = mf.compile(StringReader(content), entryName)

                if (file.extensionMatches(processExtensions)) {
                    val mustache = mf.compile(entryName)
                    mustache.execute(zip, mapOf(
                        "name" to name,
                        "package" to packageName,
                        "meta.group" to "com.example",
                        "meta.artifact" to "demo",
                        "meta.javaVersion" to "11",
                    ))
                } else {
                    file.copyTo(zip)
                }
            }
        } finally {
            zip.close()
        }
    }
}

private fun Mustache.execute(os: OutputStream, scope: Any) {
    val w = OutputStreamWriter(os, UTF_8)
    execute(w, scope)
    w.flush()
}

private fun File.copyTo(os: OutputStream) {
    inputStream().use { it.copyTo(os) }
}

private fun File.extensionMatches(allowedExtensions: Set<String>): Boolean {
    val ext = extension
    return if (ext.isEmpty()) allowedExtensions.contains(name)
    else allowedExtensions.contains(ext)
}
