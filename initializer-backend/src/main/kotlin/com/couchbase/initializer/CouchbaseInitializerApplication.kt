package com.couchbase.initializer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CouchbaseInitializerApplication

fun main(args: Array<String>) {
	runApplication<CouchbaseInitializerApplication>(*args)
}
