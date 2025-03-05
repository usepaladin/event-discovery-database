# Database Event Discovery Service - (DDS)

---

[![Project Status](https://img.shields.io/badge/status-Development-yellow)](https://your-project-website.com/status)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-v1.9+-orange.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)

## Service Overview

The **Data Event Discovery Service (DDS)** is one of the Core tools that is available for use within the Veridius EDAasS
suite. Its primary
responsibility is to automatically discover and capture data changes from user-configured data sources and transform
these changes into structured events published to a Kafka broker. The service is designed to be highly extensible,
robust and monitorable, allowing it to be a key tool in allowing businesses to develop scalable, reactive services and
applications.

DDS acts as a crucial bridge connecting user's existing data infrastructure to the event-driven architecture,
enabling simplistic automated event generation and streaming for building reactive applications and scalable services.

**Key Features**

- **Automated Event Discovery:** Intelligently detects relevant data changes without requiring manual instrumentation of
  user applications.
- **Database Change Data Capture (CDC):** Leverages Debezium Embedded Engine to capture real-time changes (CRUD
  operations) from various database types
    - MySQL, PostgreSQL, MariaDB, Oracle, SQL Server
        - **(Support for more databases will be added in future releases)**

- **Standardized Event Generation:** Transforms detected changes into structured events with a consistent schema
  (compatible with Avro, Protobuf, or other serialization formats).
- **Event Publishing to Event Bus:** Efficiently publishes generated events to a specified Kafka broker for downstream
  consumption by other services.
    - Will support seamless integrate with our proposed Event Bus Core service.
- **Dynamic Configuration:** Loads monitoring configurations from a central metadata store and supports dynamic updates
  without service restarts.
- **Robust Error Handling and Monitoring:** Implements comprehensive error handling and exposes metrics for monitoring
  service health and performance.

## Database Pre-requisites

### Postgres

- Debezium for PostgreSQL uses logical replication with pgoutput (recommended) or wal2json. You must enable logical
  replication.

```angular2html
ALTER SYSTEM SET wal_level = logical;
ALTER SYSTEM SET max_replication_slots = 10;
ALTER SYSTEM SET max_wal_senders = 10;
SELECT pg_reload_conf();
```

- When using pgoutput, this service must operate within the database as a user with the following permissions
    - REPLICATION
    - LOGIN

- A role for this would look like:

```angular2html
CREATE ROLE
<name> WITH REPLICATION LOGIN;
```

- The user provided in the configuration should then be granted this role.

```angular2html
GRANT REPLICATION TO
<name>;
```

- For all databases that are to be monitored, the user provided with the `REPLICATION` role should be granted
  appropriate permissions

```angular2html
GRANT CONNECT ON DATABASE mydatabase TO
<name>;
    GRANT USAGE ON SCHEMA public TO
    <name>;
        GRANT SELECT ON ALL TABLES IN SCHEMA
        <schema> TO
            <name>;
                ALTER DEFAULT PRIVILEGES IN SCHEMA
                <schema> GRANT SELECT ON TABLES TO
                    <name>;
```

- You should also create a replication slot to allow changes to be monitored and tracked

```angular2html
SELECT * FROM pg_create_logical_replication_slot('debezium_slot', 'pgoutput');
```

```angular2html
SELECT * FROM pg_create_logical_replication_slot('debezium_slot', 'pgoutput');
```

### MySQL

- Debezium for MySQL requires binlog replication.

```angular2html
[mysqld]
log_bin=mysql-bin
binlog_format=ROW
binlog_row_image=FULL
```

## Technological Stack

- Kotlin!

## Contributing

We welcome contributions to the Database Event Discovery Service or any other Veridius related service! Please see
the [`CONTRIBUTING.md`](CONTRIBUTING.md) file for guidelines on how to contribute.

## License

This project is licensed under the [Apache 2.0 License](LICENSE). See the `LICENSE` file for details.
