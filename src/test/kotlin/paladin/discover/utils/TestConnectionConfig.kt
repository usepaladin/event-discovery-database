package paladin.discover.utils

import io.mockk.every
import io.mockk.mockk
import paladin.discover.configuration.properties.DebeziumConfigurationProperties
import paladin.discover.pojo.monitoring.StorageBackendType
import java.nio.file.Path

object TestConnectionConfig {
    fun mockFileBackendStorage(tempDir: Path? = null): DebeziumConfigurationProperties {
        val debeziumConfigProperties = mockk<DebeziumConfigurationProperties>()
        // Configure debezium properties
        every { debeziumConfigProperties.storageBackend } returns StorageBackendType.FILE
        if (tempDir != null) {
            every { debeziumConfigProperties.offsetStorageDir } returns tempDir.toString()
        } else {
            every { debeziumConfigProperties.offsetStorageDir } returns "/tmp/debezium/offsets"
        }

        every { debeziumConfigProperties.offsetStorageFileName } returns "debezium-offsets.dat"

        if (tempDir != null) {
            every { debeziumConfigProperties.schemaHistoryDir } returns tempDir.toString()
        } else {
            every { debeziumConfigProperties.schemaHistoryDir } returns "/tmp/debezium/history"
        }

        every { debeziumConfigProperties.schemaHistoryFileName } returns "debezium-history.dat"
        return debeziumConfigProperties
    }

    fun mockKafkaBackendStorage(): DebeziumConfigurationProperties {
        val debeziumConfigProperties = mockk<DebeziumConfigurationProperties>()
        // Configure debezium properties
        every { debeziumConfigProperties.storageBackend } returns StorageBackendType.KAFKA
        every { debeziumConfigProperties.kafkaBootstrapServers } returns "localhost:9092"
        every { debeziumConfigProperties.offsetStorageTopic } returns "debezium-offsets"
        every { debeziumConfigProperties.schemaHistoryTopic } returns "debezium-history"
        every { debeziumConfigProperties.offsetStoragePartition } returns 1
        every { debeziumConfigProperties.offsetStorageReplication } returns 1

        return debeziumConfigProperties
    }

}