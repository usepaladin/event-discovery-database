package paladin.discover.pojo.monitoring

import org.apache.kafka.connect.storage.KafkaOffsetBackingStore
import paladin.discover.configuration.properties.DebeziumConfigurationProperties
import paladin.discover.util.monitor.ConnectorStorageConfiguration
import java.io.File
import java.util.*

sealed class StorageBackend : ConnectorStorageConfiguration {
    data object Kafka : StorageBackend() {
        override fun validateConfig(config: DebeziumConfigurationProperties) {
            require(config.kafkaBootstrapServers != null) { "Kafka bootstrap servers must be provided" }
            require(config.offsetStorageTopic != null) { "Offset storage topic must be provided" }
            require(config.schemaHistoryTopic != null) { "Schema history topic must be provided" }
        }

        override fun applyProperties(props: Properties, config: DebeziumConfigurationProperties) {
            props.apply {
                put("offset.storage", KafkaOffsetBackingStore::class.java.name)
                put("offset.storage.kafka.bootstrap.servers", config.kafkaBootstrapServers)
                put("offset.storage.topic", config.offsetStorageTopic)
                put("schema.history.kafka.bootstrap.servers", config.kafkaBootstrapServers)
                put("schema.history.kafka.topic", config.schemaHistoryTopic)
            }

            config.offsetStoragePartition?.let { props.put("offset.storage.partitions", it.toString()) }
            config.offsetStorageReplication?.let { props.put("offset.storage.replication.factor", it.toString()) }
            config.schemaHistoryPartition?.let { props.put("schema.history.kafka.topic.partitions", it.toString()) }
            config.schemaHistoryReplication?.let {
                props.put(
                    "schema.history.kafka.topic.replication.factor",
                    it.toString()
                )
            }
        }
    }

    data object File : StorageBackend() {
        override fun validateConfig(config: DebeziumConfigurationProperties) {
            require(config.offsetStorageFileName != null) { "Offset storage file name must be provided" }
            require(config.offsetStorageDir != null) { "Offset storage directory must be provided" }
            require(config.historyFileName != null) { "History file name must be provided" }
            require(config.historyDir != null) { "History directory must be provided" }

            // Ensure the offset storage directory exists
            val offsetDir = File(config.offsetStorageDir)
            if (!offsetDir.exists()) {
                offsetDir.mkdirs()
            }

            val historyDir = File(config.historyDir)
            if (!historyDir.exists()) {
                historyDir.mkdirs()
            }
        }

        override fun applyProperties(props: Properties, config: DebeziumConfigurationProperties) {
            props.apply {
                put("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                put("offset.storage.file.filename", config.offsetStorageFileName)
                put("offset.storage.file.dir", config.offsetStorageDir)
                put("offset.storage.file.filename", config.historyFileName)
                put("offset.storage.file.dir", config.historyDir)
            }
        }
    }
}