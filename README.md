# Couchbase Initializer PoC Backend

Experimental, unsupported.


## Prerequisites

* Java 1.8 or later.

## Build and run

The backend server listens on port 8080.
Make sure nothing else is using that port.

Build and run the backend:

```
./gradlew bootRun
```

Then visit http://localhost:8080 to see the backend interface.


## Fancy UI

The frontend requires Node.js.

Clone the frontend repo: https://github.com/daschl/initializer-ui

Build and run the UI:
```
npm install ng
ng serve
```

Visit the fancy UI at http://localhost:4200
