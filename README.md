# Couchbase Initializer PoC

Experimental, unsupported.

## Prerequisites

* Java 1.8 or later.

## Build and run

The server listens on port 8080.
Make sure nothing else is using that port.

Build and run:

```
./gradlew bootRun
```

For the fancy UI, visit http://localhost:8080

More info at http://localhost:8080/backend.html

Templates are defined [here](initializer-backend/src/templates).
Only `server/java/basic/*` and `server/node/basic/*` are "real" -- the others are placeholders.
