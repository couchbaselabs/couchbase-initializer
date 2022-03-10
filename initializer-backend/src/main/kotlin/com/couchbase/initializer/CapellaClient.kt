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

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.jsonMapper
import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.util.*
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8

data class Cluster(
    val environment: String,
    val id: String,
    val name: String,
    val projectId: String,
)

class CapellaClient(
    baseUrl: String = "https://cloudapi.cloud.couchbase.com",
    val accessKey: String,
    secretKey: String,
    httpClientOptions: HttpClient.Builder.() -> Unit = {},
) {
    private val baseUrl = baseUrl.removeSuffix("/")
    private val secretKey = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")

    private val httpClient = HttpClient.newBuilder().apply { httpClientOptions(this) }.build()

    fun newRequest(endpoint: String) =
        HttpRequest.newBuilder(URI(baseUrl + endpoint))
            .timeout(Duration.ofSeconds(10))

    fun <T> send(
        request: HttpRequest,
        responseBodyHandler: HttpResponse.BodyHandler<T>,
    ): HttpResponse<T> = httpClient.send(request.sign(accessKey, secretKey), responseBodyHandler)

    fun <T> send(
        endpoint: String, responseBodyHandler: HttpResponse.BodyHandler<T>,
        requestCustomizer: HttpRequest.Builder.() -> Unit,
    ): HttpResponse<T> {
        val requestBuilder = newRequest(endpoint)
        requestCustomizer(requestBuilder)
        return send(requestBuilder.build(), responseBodyHandler)
    }

    fun send(
        endpoint: String,
        requestCustomizer: HttpRequest.Builder.() -> Unit,
    ): HttpResponse<String> = send(endpoint, BodyHandlers.ofString(), requestCustomizer)
}

fun HttpRequest.sign(accessKey: String, secretKey: SecretKey): HttpRequest {
    val now = System.currentTimeMillis().toString()
    val endpoint = uri().rawQuery?.let { "${uri().rawPath}?$it" } ?: uri().rawPath
    val signMe = listOf(method(), endpoint, now).joinToString("\n")

    val signature = signMe.toByteArray().hmacSHA256(secretKey).base64
    val token = "$accessKey:$signature"

    return HttpRequest.newBuilder(this) { _, _ -> true } // retain existing headers
        .header("Authorization", "Bearer $token")
        .header("Couchbase-Timestamp", now)
        .build()
}

fun ByteArray.hmacSHA256(key: SecretKey): ByteArray {
    val algorithm = "HmacSHA256"
    val mac = Mac.getInstance(algorithm)
    mac.init(key)
    return mac.doFinal(this)
}

val ByteArray.base64: String
    get() = Base64.getEncoder().encodeToString(this)

fun <T> HttpResponse<T>.checkStatus(): HttpResponse<T> {
    if (statusCode() != 200) {
        throw RuntimeException("Unexpected HTTP status code: ${statusCode()} ${body()}")
    }
    return this
}

fun CapellaClient.clusters(page: Int = 1, perPage: Int = 100): List<Cluster> {
    val result = send(formatPath("/v3/clusters?page={}&perPage={}", page, perPage)) {
        GET()
    }

    return result.checkStatus()
        .json.path("data").path("items")
        .convertTo(jacksonTypeRef())
}

fun CapellaClient.connectionString(clusterId: String): String {
    val result = send(formatPath("/v3/clusters/{}", clusterId)) {
        GET()
    }

    return result.checkStatus()
        .json.path("endpointsSrv").asText()
}

fun <T> JsonNode.convertTo(toValueTypeRef: TypeReference<T>): T {
    return jsonMapper.convertValue(this, toValueTypeRef)
}

val HttpResponse<String>.json: JsonNode
    get() =
        try {
            jsonMapper.readTree(body())
        } catch (_: Exception) {
            TextNode(body())
        }

val jsonMapper = jsonMapper { addModule(KotlinModule.Builder().build()) }

fun urlEncode(s: String): String {
    return try {
        URLEncoder.encode(s, UTF_8)
            .replace("+", "%20") // Make sure spaces are encoded as "%20"
        // so the result can be used in path components and with "application/x-www-form-urlencoded"
    } catch (inconceivable: UnsupportedEncodingException) {
        throw AssertionError("UTF-8 not supported", inconceivable)
    }
}

private val PATH_PLACEHOLDER = Regex("\\{}")

/**
 * Replaces each `{}` placeholder in the template string with the
 * URL-encoded form of the corresponding list element.
 *
 * For example:
 * ```
 *     formatPath("/foo/{}/bar/{}", listOf("hello world", "a/b"))
 * ```
 * returns the string `"/foo/hello%20world/bar/a%2Fb"`
 *
 * @throws IllegalArgumentException if the number of placeholders
 * does not match the size of the list.
 */
fun formatPath(template: String, args: List<Any>): String {
    val i = args.iterator()
    val result = template.replace(PATH_PLACEHOLDER) {
        require(i.hasNext()) { "Too few arguments (${args.size}) for format string: $template" }
        urlEncode(i.next().toString())
    }
    require(!i.hasNext()) { "Too many arguments (${args.size}) for format string: $template" }
    return result
}

/**
 * Replaces each `{}` placeholder in the template string with the
 * URL-encoded form of the corresponding additional argument.
 *
 * For example:
 * ```
 *     formatPath("/foo/{}/bar/{}", "hello world", "a/b")
 * ```
 * returns the string `"/foo/hello%20world/bar/a%2Fb"`
 *
 * @throws IllegalArgumentException if the number of placeholders
 * does not match the number of additional arguments.
 */
fun formatPath(template: String, vararg args: Any): String =
    formatPath(template, listOf(*args))
