# Database Event Discovery Service - (DDS)

---

[![Project Status](https://img.shields.io/badge/status-Development-yellow)](https://your-project-website.com/status)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-v1.9+-orange.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)

## Service Overview

The **Data Event Discovery Service (DDS)** is one of the Core tools that is available for use within the paladin EDAasS
suite. Its primary
responsibility is to automatically discover and capture data changes from user-configured data sources and transform
these changes into structured events published to a Kafka broker. The service is designed to be highly extensible,
robust and monitorable, allowing it to be a key tool in allowing businesses to develop scalable, reactive services and
applications.

DDS acts as a crucial bridge connecting user's existing data infrastructure to the event-driven architecture,
enabling simplistic automated event generation and streaming for building reactive applications and scalable services.

## Key Features

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

# Database Pre-requisites

## Postgres

### Altering Database Configurations

- Debezium for PostgreSQL uses logical replication with pgoutput (recommended) or wal2json. You must enable logical
  replication.

```postgresql
ALTER SYSTEM SET wal_level = logical;
ALTER SYSTEM SET max_replication_slots = 10;
ALTER SYSTEM SET max_wal_senders = 10;
SELECT pg_reload_conf();
```

### User Permissions

- When using pgoutput, this service must operate within the database as a role/user with the following permissions
    - REPLICATION
    - LOGIN

- A role for this would look like:

```postgresql
CREATE ROLE '<name>' WITH REPLICATION LOGIN PASSWORD '<password>';
```

- The role would then also need to be granted access to the database and schema(s) that are to be monitored:

```postgresql
GRANT CONNECT ON DATABASE mydatabase TO '<name>';

-- Repeat for all schemas that are to be monitored
GRANT USAGE ON SCHEMA public TO '<name>';
GRANT SELECT ON ALL TABLES IN SCHEMA '<schema>' TO '<name>';
```

- The role can then be used to configure the service and monitor the database successfully.

## MySQL

### Altering Database Configurations

- Change monitoring on MySQL utilizes the MySQL binary log. To enable this, the following steps must be taken:
    - Ensure that the MySQL server is configured to use binary logging. There are numerous ways of doing this, depending
      on your platform (i.e., local deployment vs. cloud hosted)

- You can determine if binary logging is enabled by running the following command:

```mysql
-- returns ON or OFF

-- for MySQL 5.x
SELECT variable_value as "BINARY LOGGING STATUS (log-bin) ::"
FROM information_schema.global_variables
WHERE variable_name = 'log_bin';

-- for MySQL 8.x
SELECT variable_value as "BINARY LOGGING STATUS (log-bin) ::"
FROM performance_schema.global_variables
WHERE variable_name = 'log_bin'; 
```

#### Query Console (Temporary)

```mysql
SET GLOBAL server_id = 1;
SET GLOBAL log_bin = 'mysql-bin';
SET GLOBAL binlog_format = 'ROW';
SET GLOBAL binlog_row_image = 'FULL';
```

- You can alter your global configuration values, however, upon server restart, these values will be lost. To make these
  changes permanent.

#### Configuration File (Permanent)

1. Locate MySQL Configuration File
   Linux/macOS: /etc/mysql/my.cnf or /etc/my.cnf
   Windows: C:\ProgramData\MySQL\MySQL Server X.X\my.ini
2. Edit my.cnf or my.ini and Add the Following

```text
[mysqld]
server-id=1
log-bin=mysql-bin
binlog_format=ROW
binlog_row_image=FULL
expire_logs_days=7
```

3. Restart MySQL to Apply the Changes

``` shell
sudo systemctl restart mysql
# or..
sudo service mysql restart
```

- Alternatively, on Windows:
    1. Open the Services application
    2. Locate MySQL
    3. Restart the service

Most cloud providers will not necessarily allow you to directly edit the configuration file. Instead, you can use
inbuilt tools and settings to make these changes.

#### AWS RDS

1. Go to the AWS RDS Console.
2. Select Parameter Groups → Create Parameter Group.
3. Modify the following parameters:
    - binlog_format = ROW
    - binlog_row_image = FULL
    - log_bin = mysql-bin (Not always needed, as RDS enables it by default)
    - expire_logs_days = 7
    - server_id = 1 (Must be unique across replicas)
4. Apply the parameter group to your MySQL instance.
5. Restart the instance for changes to take effect.

If you run MySQL on Amazon RDS, you must enable automated backups for your database instance for binary logging to
occur.
If the database instance is not configured to perform automated backups, the binlog is disabled, even if you apply the
settings described in the previous steps.

#### Google Cloud SQL

1. Open Cloud SQL in Google Cloud Console.
2. Select your MySQL instance.
3. Go to Configuration > Edit Flags.
    - Add or update the following:
    - binlog_format = ROW
    - binlog_row_image = FULL
    - expire_logs_days = 7
    - server_id = 1
4. Restart the instance to apply the changes.

#### Azure Database for MySQL

1. Go to Azure Portal → MySQL Server.
2. Navigate to Settings > Server parameters.
3. Find the following parameters and update them:
    - binlog_format = ROW
    - binlog_row_image = FULL
    - expire_logs_days = 7
    - server_id = 1
4. Save the changes and restart the MySQL server.

#### Self Hosted on VPS

- If you're running MySQL on a cloud VM (e.g., AWS EC2, GCP Compute Engine, DigitalOcean Droplet), you do have access to
  my.cnf (as you are just accessing a remote instance). In this case, modify it as follows:

1. SSH into your server.

```shell
ssh user@your-server-ip
```

2. Open my.cnf in a text editor (e.g., nano, vim).

```shell
sudo nano /etc/mysql/my.cnf
```

3. Add these lines under [mysqld]:

```text
[mysqld]
server-id=1
log-bin=mysql-bin
binlog_format=ROW
binlog_row_image=FULL
expire_logs_days=7
```

4. Save the file and restart MySQL.

```shell
sudo systemctl restart mysql
```

### Verifying Configuration Changes

After restarting MySQL, check if the settings are correctly applied:

```mysql
SHOW VARIABLES LIKE 'server_id';
SHOW VARIABLES LIKE 'log_bin';
SHOW VARIABLES LIKE 'binlog_format';
SHOW VARIABLES LIKE 'binlog_row_image';
SHOW VARIABLES LIKE 'expire_logs_days';
```

### User Permissions

- A user should be created and be granted the follow permissions to allow access to database monitoring functionality:

```mysql
CREATE USER '<name>'@'<host>' IDENTIFIED BY '<password>';
GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO '<user>' IDENTIFIED BY '<password>';
```

- The user should also have the following permissions on the database and schema(s) that are to be monitored:

```mysql
GRANT SELECT ON *.* TO '<name>'@'%';
```

- You can then run the following to finalize the permissions:

```mysql
FLUSH PRIVILEGES;
```

## Technological Stack

- Kotlin!

## Contributing

We welcome contributions to the Database Event Discovery Service or any other paladin related service! Please see
the [`CONTRIBUTING.md`](CONTRIBUTING.md) file for guidelines on how to contribute.

## License

This project is licensed under the [Apache 2.0 License](LICENSE). See the `LICENSE` file for details.
