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
        response: HttpServletResponse,
    ): Unit {
        val archiveFilename = "project.zip"
        response.setHeader("Content-Type", "application/zip")
        response.setHeader("Content-Disposition", "attachment; filename=\"$archiveFilename\"")

        val root = Paths.get("src/templates/java").absolute().toString() + "/"

        val mf: MustacheFactory = DefaultMustacheFactory(File("src/templates/java"))

        val zip = ZipOutputStream(response.outputStream)
        try {
            File(root).walk().forEach { file ->
                if (!file.isFile) return@forEach

                val entryName = file.path.removePrefix(root)
                zip.putNextEntry(ZipEntry(entryName))

                val mustache = mf.compile(entryName)
                mustache.execute(zip, mapOf("name" to name))

//                file.copyTo(zip)
            }

            //    mapNotNull { if (it.isDirectory) null else it.path.removePrefix(root) }

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

//public fun main() {
//    val mf: MustacheFactory = DefaultMustacheFactory(File("src/templates/java"))
//    val mustache = mf.compile("src/main/java/com/example/HelloWorld.java")
//    mustache.execute(PrintWriter(System.out), mapOf("name" to "Stranger")).flush()
//
//}
