# Couchbase Initializer PoC

Experimental, unsupported.

## Prerequisites

* Java 1.8 or later.
* Angular CLI (`ng`)

## Build and run

The server listens on port 8080.
Make sure nothing else is using that port.

Build and run:

```
./gradlew bootRun
```

TIP: It might look like the Gradle command is hanging, but the server is ready
when the Spring Boot banner appears and the last line of the output contains "Started CouchbaseInitializerApplicationKt"

For the fancy UI, visit http://localhost:8080

More info at http://localhost:8080/backend.html

Templates are defined [here](initializer-backend/src/templates).
Only `server/java/basic/*` and `server/node/basic/*` are "real" -- the others are placeholders.

Adding a new template to the PoC requires editing
[manifest.json](initializer-backend/src/main/resources/manifest.json).
(The long-term plan is to dynamically generate the manifest.)
