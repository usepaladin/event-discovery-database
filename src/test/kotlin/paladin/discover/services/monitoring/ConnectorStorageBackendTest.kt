package paladin.discover.services.monitoring

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import paladin.discover.configuration.properties.DebeziumConfigurationProperties
import paladin.discover.enums.configuration.DatabaseType
import paladin.discover.models.connection.DatabaseConnectionConfiguration
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.pojo.monitoring.StorageBackend
import paladin.discover.utils.TestConnectionConfig
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class StorageBackendTest {

    @MockK
    private lateinit var client: DatabaseClient

    @MockK
    private lateinit var connectionConfig: DatabaseConnectionConfiguration

    private lateinit var properties: Properties
    private val clientId = UUID.randomUUID()

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        every { client.config } returns connectionConfig
        every { connectionConfig.databaseType } returns DatabaseType.POSTGRES
        properties = Properties()
    }

    @Test
    fun `test Kafka storage backend validation fails with incorrect configuration`() {
        // Setup
        val config: DebeziumConfigurationProperties = TestConnectionConfig.mockKafkaBackendStorage()
        every { config.kafkaBootstrapServers } returns null

        // Execute & Verify
        assertThrows<Exception> {
            StorageBackend.Kafka.validateConfig(config, client)
        }

        val config2: DebeziumConfigurationProperties = TestConnectionConfig.mockKafkaBackendStorage()
        every { config2.offsetStorageTopic } returns null

        // Execute & Verify
        assertThrows<Exception> {
            StorageBackend.Kafka.validateConfig(config2, client)
        }
    }

    @Test
    fun `test File based storage backend validation fails with incorrect configuration`() {
        // Setup
        val config: DebeziumConfigurationProperties = TestConnectionConfig.mockFileBackendStorage(tempDir)
        every { config.offsetStorageFileName } returns null

        // Execute & Verify
        assertThrows<Exception> {
            StorageBackend.File.validateConfig(config, client)
        }

        val config2: DebeziumConfigurationProperties = TestConnectionConfig.mockKafkaBackendStorage()
        every { config2.offsetStorageDir } returns null

        // Execute & Verify
        assertThrows<Exception> {
            StorageBackend.File.validateConfig(config2, client)
        }
    }


    @Test
    fun `test Kafka storage backend validation fails on additional properties for databases requiring Schema History`() {
        // Setup
        every { connectionConfig.databaseType } returns DatabaseType.MYSQL
        val config: DebeziumConfigurationProperties = TestConnectionConfig.mockKafkaBackendStorage()
        every { config.schemaHistoryTopic } returns null

        // Execute & Verify
        assertThrows<Exception> {
            StorageBackend.Kafka.validateConfig(config, client)
        }
    }

    @Test
    fun `test File storage backend validation fails on additional properties for databases requiring Schema History`() {
        // Setup
        every { connectionConfig.databaseType } returns DatabaseType.MYSQL
        val config: DebeziumConfigurationProperties = TestConnectionConfig.mockFileBackendStorage(tempDir)
        every { config.schemaHistoryDir } returns null

        // Execute & Verify
        assertThrows<Exception> {
            StorageBackend.File.validateConfig(config, client)
        }

        val config2: DebeziumConfigurationProperties = TestConnectionConfig.mockFileBackendStorage(tempDir)
        every { config2.schemaHistoryFileName } returns null

        // Execute & Verify
        assertThrows<Exception> {
            StorageBackend.File.validateConfig(config2, client)
        }
    }


    @Test
    fun `test Kafka storage backend applies offset storage properties`() {
        // Setup
        val config: DebeziumConfigurationProperties = TestConnectionConfig.mockKafkaBackendStorage()

        // Execute
        StorageBackend.Kafka.applyOffsetStorage(properties, config, clientId)

        // Verify
        assertEquals(
            "org.apache.kafka.connect.storage.KafkaOffsetBackingStore",
            properties.getProperty("offset.storage")
        )
        assertEquals("localhost:9092", properties.getProperty("offset.storage.kafka.bootstrap.servers"))
        assertEquals("debezium-offsets", properties.getProperty("offset.storage.topic"))
        assertEquals("1", properties.getProperty("offset.storage.partitions"))
        assertEquals("1", properties.getProperty("offset.storage.replication.factor"))
    }

    @Test
    fun `test Kafka storage backend applies schema history properties for Schema History supported Databases`() {
        // Setup
        every { connectionConfig.databaseType } returns DatabaseType.MYSQL
        val config = TestConnectionConfig.mockKafkaBackendStorage()
        // Execute
        StorageBackend.Kafka.applySchemaHistory(properties, config, clientId)

        // Verify
        assertEquals(
            "io.debezium.storage.kafka.history.KafkaSchemaHistory",
            properties.getProperty("schema.history.internal")
        )
        assertEquals("localhost:9092", properties.getProperty("schema.history.kafka.bootstrap.servers"))
        assertEquals("debezium-history", properties.getProperty("schema.history.kafka.topic"))
    }


    @Test
    fun `test File storage backend applies offset storage properties`() {
        // Execute
        val config = TestConnectionConfig.mockFileBackendStorage(tempDir)
        StorageBackend.File.applyOffsetStorage(properties, config, clientId)

        // Verify
        assertEquals(
            "org.apache.kafka.connect.storage.FileOffsetBackingStore",
            properties.getProperty("offset.storage")
        )
        val expectedPath = File(tempDir.toFile(), "${clientId}.debezium-offsets.dat").absolutePath
        assertEquals(expectedPath, properties.getProperty("offset.storage.file.filename"))
    }

    @Test
    fun `test File storage backend applies schema history properties for Schema History supported Databases`() {
        // Setup
        every { connectionConfig.databaseType } returns DatabaseType.MYSQL
        val config = TestConnectionConfig.mockFileBackendStorage(tempDir)

        // Execute
        StorageBackend.File.applySchemaHistory(properties, config, clientId)

        // Verify
        assertEquals(
            "io.debezium.storage.file.history.FileSchemaHistory",
            properties.getProperty("schema.history.internal")
        )
        val expectedPath = File(tempDir.toFile(), "${clientId}.debezium-history.dat").absolutePath
        assertEquals(expectedPath, properties.getProperty("schema.history.internal.file.filename"))
    }

}