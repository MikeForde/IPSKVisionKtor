# IPS KVision Ktor

IPS KVision Ktor is a Kotlin Multiplatform web application for viewing, converting, exporting, encrypting, and sharing International Patient Summary (IPS) records.

The application uses:

- Kotlin Multiplatform with a JVM backend and Kotlin/JS frontend
- Ktor Netty for the backend HTTP server
- KVision for the browser UI
- Kilua RPC for typed frontend/backend service calls
- Exposed and HikariCP for database access
- MySQL by default, with an in-memory H2 fallback if database environment variables are not set

## What The Application Does

The UI is named `IPS SK3` and provides these main areas:

- IPS record search by surname
- Patient detail display for demographics, medications, allergies, conditions, observations, and immunizations
- Export of selected IPS records as FHIR-style IPS Unified JSON Bundle, HL7 v2.3, or BEER text format
- Optional gzip plus AES-256 encryption for exported payloads
- QR code generation for IPS URLs or embedded IPS payloads
- NFC write support for IPS payloads in supported browsers/devices
- NFC read/import support for JSON FHIR Bundles, HL7 v2.3, and BEER messages
- Conversion endpoints for converting external IPS formats into the internal `IPSModel` schema

The internal IPS record model includes patient demographics plus child collections for medications, allergies, conditions, observations, and immunizations.

## Requirements

- JDK 21
- Node.js, available through the dev container or local install
- Gradle wrapper from this repository, invoked as `./gradlew`
- A MySQL-compatible database for persistent data, or no database settings for the H2 in-memory fallback

The included dev container installs Java, Gradle, Node.js, Kotlin, and supporting tools.

## Environment Variables

The backend requires the frontend origin for CORS. For local development, enter these commands in the terminal before starting the backend:

```bash
export FRONTEND_HOST=localhost:3000
export FRONTEND_SCHEME=http
```

For MySQL persistence, also set:

```bash
export DB_JDBC_URL='jdbc:mysql://localhost:3306/ips'
export DB_USER='your_database_user'
export DB_PASSWORD='your_database_password'
```

If `DB_JDBC_URL`, `DB_USER`, and `DB_PASSWORD` are not set, the application falls back to `jdbc:h2:mem:test`. That is useful for development startup checks, but data is not persistent.

For a deployed HTTPS frontend, use the deployed host without a scheme:

```bash
export FRONTEND_HOST=your-app.example.com
export FRONTEND_SCHEME=https
```

## Run Locally

Use two terminals: one for the Ktor backend and one for the KVision frontend.

Terminal 1, backend on `http://localhost:8080`:

```bash
export FRONTEND_HOST=localhost:3000
export FRONTEND_SCHEME=http
./gradlew jvmRun
```

Terminal 2, frontend dev server on `http://localhost:3000`:

```bash
./gradlew jsBrowserDevelopmentRun
```

Open the application in your browser:

```bash
xdg-open http://localhost:3000
```

If `xdg-open` is not available, open `http://localhost:3000` manually in a browser.

## Common Development Commands

Compile the backend:

```bash
./gradlew compileKotlinJvm
```

Compile the frontend:

```bash
./gradlew compileKotlinJs
```

Run all available checks/tests:

```bash
./gradlew check
```

Format Kotlin code with ktfmt:

```bash
./gradlew ktfmtFormat
```

Generate the translation template:

```bash
./gradlew generatePotFile
```

Build the production frontend bundle:

```bash
./gradlew jsBrowserDistribution
```

Build the backend jar:

```bash
./gradlew jvmJar
```

Build the combined backend jar with embedded frontend resources:

```bash
./gradlew jarWithJs
```

The combined jar is written to:

```text
build/libs/ips-kvision-ktor-1.0.0-SNAPSHOT.jar
```

## Run The Packaged Jar

Build the combined jar:

```bash
./gradlew jarWithJs
```

Run it locally:

```bash
export FRONTEND_HOST=localhost:8080
export FRONTEND_SCHEME=http
java -jar build/libs/ips-kvision-ktor-1.0.0-SNAPSHOT.jar
```

Then open:

```bash
xdg-open http://localhost:8080
```

## Docker

Build the combined jar first:

```bash
./gradlew jarWithJs
```

Build the Docker image:

```bash
docker build -t ips-kvision-ktor .
```

Run the container with the in-memory H2 fallback:

```bash
docker run --rm -p 8080:8080 \
  -e FRONTEND_HOST=localhost:8080 \
  -e FRONTEND_SCHEME=http \
  ips-kvision-ktor
```

Run the container with MySQL settings:

```bash
docker run --rm -p 8080:8080 \
  -e FRONTEND_HOST=localhost:8080 \
  -e FRONTEND_SCHEME=http \
  -e DB_JDBC_URL='jdbc:mysql://host.docker.internal:3306/ips' \
  -e DB_USER='your_database_user' \
  -e DB_PASSWORD='your_database_password' \
  ips-kvision-ktor
```

## API Overview

The backend runs on port `8080` by default.

Main REST endpoints include:

- `POST /api/patientName` returns a patient name for a package UUID
- `GET /api/ipsRecord?id=<packageUUID>` returns an IPS record by package UUID
- `POST /api/ipsRecord` returns an IPS record by package UUID in the request body
- `POST /api/ipsRecordByName` returns an IPS record by exact patient name
- `POST /api/ipsRecordFromBundle` converts an IPS Bundle to the internal schema and stores it
- `POST /api/ipsRecordFromHL72_x` parses an HL7 v2.x message and stores it
- `POST /api/convertIPS` converts an IPS Bundle to the internal schema
- `POST /api/parseHL7` converts an HL7 v2.x message to the internal schema
- `POST /api/parseBEER` converts a BEER message to the internal schema
- `POST /api/convertSchemaToUnified` converts the internal schema to an IPS Unified JSON Bundle
- `POST /api/convertSchemaToHL72_3` converts the internal schema to HL7 v2.3 text
- `POST /api/convertSchemaToBEER` converts the internal schema to BEER text
- `POST /api/encryptText` and `POST /api/decryptText` encrypt/decrypt text payloads
- `POST /api/gzipEncode` and `POST /api/gzipDecode` gzip/decode payloads
- `POST /api/gzipEncrypt` and `POST /api/gzipDecrypt` combine gzip with AES-256 encryption

The frontend also uses Kilua RPC endpoints under `/rpc/*` for typed application operations such as listing IPS records, searching by surname, generating export formats, importing records, encryption, and QR generation.

## Example API Calls

Get an IPS record by package UUID:

```bash
curl 'http://localhost:8080/api/ipsRecord?id=PACKAGE_UUID_HERE'
```

Get a patient name by package UUID:

```bash
curl -X POST 'http://localhost:8080/api/patientName' \
  -H 'Content-Type: application/json' \
  -d '{"id":"PACKAGE_UUID_HERE"}'
```

Convert HL7 text to the internal IPS schema:

```bash
curl -X POST 'http://localhost:8080/api/parseHL7' \
  -H 'Content-Type: text/plain' \
  --data-binary @message.hl7
```

Convert BEER text to the internal IPS schema:

```bash
curl -X POST 'http://localhost:8080/api/parseBEER' \
  -H 'Content-Type: text/plain' \
  --data-binary @message.beer
```

## Project Layout

```text
src/commonMain/kotlin/com/example/   Shared models and RPC service interface
src/jvmMain/kotlin/com/example/      Ktor backend, routes, database, converters, encryption helpers
src/jvmMain/resources/               Ktor config and logging config
src/jsMain/kotlin/com/example/       KVision frontend UI and browser model
src/jsMain/resources/                Frontend static assets, CSS, i18n files, manifest
webpack.config.d/                    KVision webpack customisation
```

## Notes

- The backend creates the database tables on startup through Exposed schema creation.
- Web NFC only works in browsers/devices that support the Web NFC API.
- QR payloads larger than about 3000 bytes are rejected in the UI to keep generated QR codes usable.
- Local frontend development uses port `3000`; the backend uses port `8080`.
