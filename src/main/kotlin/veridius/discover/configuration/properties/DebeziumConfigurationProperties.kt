package veridius.discover.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import veridius.discover.pojo.monitoring.StorageBackend

@ConfigurationProperties(prefix = "debezium")
data class DebeziumConfigurationProperties(
    val storageBackend: StorageBackend,

    // File Based Storage
    val offsetStorageFileName: String?,
    val offsetStorageDir: String?,
    val historyFileName: String?,
    val historyDir: String?,

    // Kafka Storage
    val kafkaBootstrapServers: String?,
    val offsetStorageTopic: String?,
    val offsetStoragePartition: Int?,
    val offsetStorageReplication: Short?,
    val schemaHistoryTopic: String?,
    val schemaHistoryPartition: Int?,
    val schemaHistoryReplication: Short?,

    // Database Storage
    val databaseStorageHostname: String?,
    val databaseStoragePort: Int?,
    val databaseStorageUser: String?,
    val databaseStoragePassword: String?,
    val databaseStorageDatabase: String?,
    val databaseStorageTable: String?,

    )