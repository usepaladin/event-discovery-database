# Database Event Discovery Service - (DDS)

---

[![Project Status](https://img.shields.io/badge/status-Development-yellow)](https://your-project-website.com/status)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-v1.9+-orange.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)

**Service Overview**

The **Data Event Discovery Service (DDS)** is one of the Core tools within the planned Veridius lineup. Its primary
responsibility is to automatically discover and capture data changes from user-configured data sources and transform
these changes into structured events published to a Kafka broker. The DDS is designed to be a key component in the
Veridius event-driven architecture, enabling the development of reactive applications that respond to real-time data
changes.

DDS acts as the crucial bridge connecting user's existing data infrastructure to the event-driven architecture,
enabling "zero-config" automated event generation and streaming for building reactive applications.

**Key Features**

- **Automated Event Discovery:** Intelligently detects relevant data changes without requiring manual instrumentation of
  user applications.
- **Database Change Data Capture (CDC):** Leverages Debezium Embedded Engine to capture real-time changes (CRUD
  operations) from various database types
    - MySQL, PostgreSQL, MongoDB, Cassandra (with more support planned).
- **Standardized Event Generation:** Transforms detected changes into structured events with a consistent schema
  (compatible with Avro, Protobuf, or other serialization formats).
- **Event Publishing to Event Bus:** Efficiently publishes generated events to a specified Kafka broker for downstream
  consumption by other services.
    - Will support seamless integrate with our proposed Event Bus Core service.
- **Dynamic Configuration:** Loads monitoring configurations from a central metadata store and supports dynamic updates
  without service restarts.
- **Robust Error Handling and Monitoring:** Implements comprehensive error handling and exposes metrics for monitoring
  service health and performance.

**Technological Stack**

**Getting Started - Development Setup**

1. **Prerequisites:**

    - [JDK 17 or later](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html)
    - [Gradle](https://gradle.org/install/) (or use Gradle wrapper included in the project)
    - [Docker](https://www.docker.com/get-started) (for containerization)
    - (Optionally) A running instance of [Kafka](https://kafka.apache.org/)
      and [Schema Registry](https://docs.confluent.io/platform/schema-registry/overview/index.html) for local testing (
      if you have dependencies on these already set up). For basic development, you might use in-memory mocks or
      testcontainers initially.

2. **Clone the Repository:**

   ```bash
   git clone git@github.com:VeridiusApp/event-discovery-database.git event-discovery
   cd event-discovery
   ```

3. **Build the Service:**

   ```bash
   ./gradlew build
   ```

   (or `gradlew.bat build` on Windows)

4. **Run the Service (Locally - for Development/Testing):**

   ```bash
   ./gradlew run
   ```

   or run directly from your IDE (e.g., IntelliJ IDEA) after importing the Gradle project.

   **Note:** For local development, you'll need to configure the EDS to connect to your local database instances and
   Event Bus Core (if you have a mock or local setup for the Event Bus). See the [Configuration](#configuration) section
   below.

**Configuration**

The EDS service is configured via [Typesafe Config](https://github.com/lightbend/config). Configuration settings are
typically loaded from:

- `application.conf` (in `src/main/resources/`) - Default configurations.
- Environment variables - For overriding configurations in different environments.

**Key Configuration Parameters:**

- Ill detail configuration when i actually get a service to build lol

See the `application.conf` file in `src/main/resources/` for a full list of configuration options and their default
values.

**Running in Docker**

1. **Build the Docker Image:**

   ```bash
   ./gradlew dockerBuild
   ```

   This will build a Docker image for the EDS service.

2. **Run the Docker Container:**
   ```bash
   docker run -d \
     -e EDS_INSTANCE_ID="your-instance-id" \ # Replace with your instance ID
     -e METADATA_DATABASE_JDBC_URL="..." \ # Configure Metadata DB connection via env vars
     -e METADATA_DATABASE_USERNAME="..." \
     -e METADATA_DATABASE_PASSWORD="..." \
     -e EVENTBUS_KAFKA_BOOTSTRAP_SERVERS="..." \ # Kafka brokers for Event Bus
     -e SCHEMA_REGISTRY_URL="..." \ # Schema Registry URL
     your-eds-docker-image:latest
   ```
   **Important:**
    - Replace placeholder environment variables with your actual configuration values.
    - Ensure the EDS container has network access to the Metadata Database, Kafka brokers, Schema Registry, and the user
      data sources it needs to monitor (databases, APIs - remember to use `host.docker.internal` or appropriate network
      addressing for local setups, and proper service discovery in deployed environments).
    - For production deployments, consider using a container orchestration platform like Kubernetes.

**Contributing**

We welcome contributions to the Database Event Discovery Service or any other Veridius related service! Please see
the [`CONTRIBUTING.md`](CONTRIBUTING.md) file for guidelines on how to contribute.

**License**

This project is licensed under the [Apache 2.0 License](LICENSE). See the `LICENSE` file for details.
