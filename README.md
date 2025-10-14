# Kafka with Spring Boot — Library Events Producer

A simple, production-leaning example of a Kafka producer built with Spring Boot. The primary module is `library-events-producer`, a REST API that accepts library events and publishes them to a Kafka topic.

> Note: This README focuses on the top-level project and the `library-events-producer` module. It intentionally ignores the folder `kafka-for-developers-using-spring-boot-v2` (teaching materials/alternate implementations). You don’t need that folder to run this app.

---

## Table of Contents
- [What you get](#what-you-get)
- [Modules](#modules)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Environment overrides](#environment-overrides)
- [Build and run](#build-and-run)
- [API](#api)
  - [Create Library Event (POST /v1/libraryevent)](#1-create-library-event)
  - [Update Library Event (PUT /v1/libraryevent)](#2-update-library-event)
  - [Sample curl requests](#sample-requests-curl)
- [How events are published](#how-events-are-published)
- [Topic creation](#topic-creation)
- [Running Kafka locally](#running-kafka-locally-notes)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Project layout](#project-layout-simplified)
- [License](#license)

---

## What you get
- Spring Boot 3.5 application that exposes REST endpoints to create and update library events
- Validation on incoming requests (Jakarta Validation)
- Kafka producer that publishes events as JSON strings
- Profiles and local defaults for rapid startup
- Unit and integration tests for the controller and producer

## Modules

| Module                   | Build | Java | Spring Boot | Depends on                                                 | Port | Kafka Topic       |
|--------------------------|-------|------|-------------|------------------------------------------------------------|------|-------------------|
| `library-events-producer`| Maven | 25   | 3.5.6       | spring-kafka, spring-boot-starter-web, validation          | 8080 | `library-events`  |

> See `library-events-producer/pom.xml` for full dependency details.

## Prerequisites
- JDK 25 installed and on your PATH
- Maven 3.9+ (`mvn`)
- A running Kafka cluster reachable at:
  - bootstrap servers: `localhost:9092, localhost:9093, localhost:9094`
  - topic: `library-events` (auto-created by the app when running with the `local` profile)

> You can run a single-broker Kafka locally if you prefer, but you must update the bootstrap servers in `application.yaml` or via environment variables accordingly.

## Configuration
The app is configured via `library-events-producer/src/main/resources/application.yaml` and supports Spring profiles.

- Active profile: `SPRING_PROFILES_ACTIVE` env var (defaults to `local`)
- Local profile (default):
  - topic: `library-events`
  - `spring.kafka.producer.bootstrap-servers`: `localhost:9092,9093,9094`
  - producer key serializer: Integer
  - producer value serializer: String
  - retries: 10, `retry.backoff.ms`: 1000, `acks`: all
  - admin/bootstrap-servers: `localhost:9092,9093,9094` (for topic creation)
- Server port: `8080`

## Environment overrides
You can override the active profile with:

- Linux/macOS:
  ```bash
  export SPRING_PROFILES_ACTIVE=dev
  ```
- Windows (PowerShell):
  ```powershell
  $env:SPRING_PROFILES_ACTIVE="dev"
  ```

## Build and run
1) Build (from project root):
```bash
mvn -f library-events-producer/pom.xml clean package
```

2) Run:
```bash
# Option A: Spring Boot plugin (hot run)
mvn -f library-events-producer/pom.xml spring-boot:run

# Option B: Run the packaged jar
java -jar library-events-producer/target/library-events-producer-0.0.1-SNAPSHOT.jar
```

3) Health check:
- Once started, the API listens on: http://localhost:8080

## API
- Base URL: `http://localhost:8080`
- Content-Type: `application/json`

### 1) Create Library Event
- Method/Path: `POST /v1/libraryevent`
- Purpose: Publish a NEW library event to Kafka.
- Request body shape:
```json
{
  "libraryEventId": null,
  "libraryEventType": "NEW",
  "book": {
    "bookId": 123,
    "bookName": "Dilip",
    "bookAuthor": "Kafka Using Spring Boot"
  }
}
```
- Validation:
  - `book.bookId`: required (integer)
  - `book.bookName`: required (non-empty)
  - `book.bookAuthor`: required (non-empty)
- Response: `201 Created` with the submitted payload

### 2) Update Library Event
- Method/Path: `PUT /v1/libraryevent`
- Purpose: Publish an UPDATE for an existing library event to Kafka.
- Requirements:
  - `libraryEventId` must be present (non-null)
  - `libraryEventType` must be `UPDATE`
- Request body example:
```json
{
  "libraryEventId": 123,
  "libraryEventType": "UPDATE",
  "book": {
    "bookId": 123,
    "bookName": "Dilip",
    "bookAuthor": "Kafka Using Spring Boot"
  }
}
```
- Possible error responses:
  - `400 Bad Request`: "Please pass the LibraryEventId" (if id is null)
  - `400 Bad Request`: "LibraryEventType should be UPDATE" (if not UPDATE)
- Response: `200 OK` with the submitted payload

### Sample requests (curl)
- Create (NEW):
```bash
curl -i -X POST "http://localhost:8080/v1/libraryevent" \
  -H "Content-Type: application/json" \
  --data @library-events-producer/src/main/resources/library-event-create.json
```

- Update (UPDATE):
```bash
curl -i -X PUT "http://localhost:8080/v1/libraryevent" \
  -H "Content-Type: application/json" \
  --data @library-events-producer/src/main/resources/library-event-update.json
```

## How events are published
- The controller uses `LibraryEventsProducer` to send events synchronously to Kafka using Spring for Apache Kafka.
- Key serializer: Integer; Value serializer: String (JSON payload serialized as a string)
- `acks` set to `all`; producer has basic retry configuration for resiliency.

## Topic creation
- On the `local` profile, an admin client is configured to connect to the same bootstrap servers. If your cluster allows auto-creation or the app includes auto-topic creation logic (see `AutoCreateTopicConfig`), the `library-events` topic will be created if missing. Otherwise, create it manually in your Kafka cluster before running.

## Running Kafka locally (notes)
- You need brokers reachable on `localhost:9092,9093,9094` to match the default configuration.
- If you don’t have a cluster with those exact ports, either:
  - Update `library-events-producer/src/main/resources/application.yaml` to point to your Kafka bootstrap servers, or
  - Override via environment/properties (e.g., `--spring.kafka.producer.bootstrap-servers=localhost:9092`)

## Testing
From project root:
```bash
mvn -f library-events-producer/pom.xml test
```
Includes unit tests for validation/controller and basic integration tests.

## Troubleshooting
- Connection errors to Kafka:
  - Ensure Kafka is running and reachable at the configured bootstrap servers.
  - Verify the topic exists if auto-creation is disabled in your cluster.
- Serialization errors:
  - Confirm request payload matches the schema shown above; value serializer is String (JSON text).
- 400 errors on update:
  - Ensure `libraryEventId` is set and `libraryEventType` is `UPDATE`.

## Project layout (simplified)
```
library-events-producer/
  src/main/java/com/learnkafka
    ├─ controller/LibraryEventsController.java      # REST endpoints (POST/PUT)
    ├─ producer/LibraryEventsProducer.java         # Kafka producer logic
    └─ domain/
       ├─ Book.java
       ├─ LibraryEvent.java
       └─ LibraryEventType.java                    # payload model
  src/main/java/com/learnkafka/config
    └─ AutoCreateTopicConfig.java                  # optional topic auto-creation
  src/main/resources/
    ├─ application.yaml                            # profiles and producer/admin configuration
    ├─ library-event-create.json                   # sample payload
    └─ library-event-update.json                   # sample payload
  src/test/java/com/learnkafka                     # unit and integration tests
```

## License
This repository is intended for learning and demonstration purposes. No explicit license file was provided at the time of writing.
