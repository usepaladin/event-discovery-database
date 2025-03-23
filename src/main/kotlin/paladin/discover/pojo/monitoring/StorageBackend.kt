package paladin.discover.pojo.monitoring

import io.debezium.storage.file.history.FileSchemaHistory
import io.debezium.storage.kafka.history.KafkaSchemaHistory
import org.apache.kafka.connect.storage.FileOffsetBackingStore
import org.apache.kafka.connect.storage.KafkaOffsetBackingStore
import paladin.discover.configuration.properties.DebeziumConfigurationProperties
import paladin.discover.enums.configuration.DatabaseType
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.util.monitor.ConnectorStorageConfiguration
import java.io.File
import java.util.*

enum class StorageBackendType {
    KAFKA,
    FILE
}

//https://debezium.io/documentation/reference/stable/development/engine.html#engine-properties
sealed class StorageBackend : ConnectorStorageConfiguration {
    data object Kafka : StorageBackend() {
        override fun validateConfig(config: DebeziumConfigurationProperties, client: DatabaseClient) {
            require(config.kafkaBootstrapServers != null) { "Kafka bootstrap servers must be provided" }
            require(config.offsetStorageTopic != null) { "Offset storage topic must be provided" }

            // Todo: Extend this to support other schema history required databases when support has been implemented
            if (client.config.databaseType == DatabaseType.MYSQL) {
                require(config.schemaHistoryTopic != null) { "Schema history topic must be provided" }
            }

        }

        override fun applyOffsetStorage(props: Properties, config: DebeziumConfigurationProperties, clientId: UUID) {
            props.apply {
                put("offset.storage", KafkaOffsetBackingStore::class.java.name)
                put("offset.storage.kafka.bootstrap.servers", config.kafkaBootstrapServers)
                put("offset.storage.topic", config.offsetStorageTopic)
            }

            config.offsetStoragePartition?.let { props.put("offset.storage.partitions", it.toString()) }
            config.offsetStorageReplication?.let { props.put("offset.storage.replication.factor", it.toString()) }
        }

        override fun applySchemaHistory(props: Properties, config: DebeziumConfigurationProperties, clientId: UUID) {
            props.apply {
                put("schema.history.internal", KafkaSchemaHistory::class.java.name)
                put("schema.history.kafka.bootstrap.servers", config.kafkaBootstrapServers)
                put("schema.history.kafka.topic", config.schemaHistoryTopic)
            }
        }
    }

    data object File : StorageBackend() {
        override fun validateConfig(config: DebeziumConfigurationProperties, client: DatabaseClient) {
            require(config.offsetStorageFileName != null) { "Offset storage file name must be provided" }
            require(config.offsetStorageDir != null) { "Offset storage directory must be provided" }


            // Ensure the offset storage directory exists
            val offsetDir = File(config.offsetStorageDir)
            if (!offsetDir.exists()) {
                offsetDir.mkdirs()
            }


            require(config.schemaHistoryFileName != null) { "Schema history file name must be provided" }
            require(config.schemaHistoryDir != null) { "Schema history directory must be provided" }
            val schemaHistoryDir = File(config.schemaHistoryDir)
            if (!schemaHistoryDir.exists()) {
                schemaHistoryDir.mkdirs()
            }


        }

        override fun applyOffsetStorage(props: Properties, config: DebeziumConfigurationProperties, clientId: UUID) {
            props.apply {
                put("offset.storage", FileOffsetBackingStore::class.java.name)
                put(
                    "offset.storage.file.filename",
                    "${config.offsetStorageDir}/${clientId}.${config.offsetStorageFileName}"
                )
            }
        }

        override fun applySchemaHistory(props: Properties, config: DebeziumConfigurationProperties, clientId: UUID) {
            props.apply {

                put("schema.history.internal", FileSchemaHistory::class.java.name)
                put(
                    "schema.history.internal.file.filename",
                    "${config.schemaHistoryDir}/${clientId}.${config.schemaHistoryFileName}"
                )
            }
        }

    }
}