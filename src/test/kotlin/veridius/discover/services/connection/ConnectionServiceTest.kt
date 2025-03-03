package veridius.discover.services.connection

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import mu.KLogger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import veridius.discover.exceptions.DatabaseConnectionNotFound
import veridius.discover.pojo.client.ConnectionState
import veridius.discover.utils.TestDatabaseConfigurations
import java.util.*

class ConnectionServiceTest {
    private lateinit var connectionService: ConnectionService
    private lateinit var logger: KLogger

    @BeforeEach
    fun setup() {
        logger = mockk<KLogger>()
        connectionService = ConnectionService(logger)
    }

    @Test
    fun `test create connection for all database types`() {
        // Test Postgres
        val postgresConfig = TestDatabaseConfigurations.createPostgresConfig()
        val postgresClient = connectionService.createConnection(postgresConfig, false)
        assertNotNull(postgresClient)
        assertEquals(postgresConfig.id, postgresClient?.id)

        // Test MySQL
        val mysqlConfig = TestDatabaseConfigurations.createMySQLConfig()
        val mysqlClient = connectionService.createConnection(mysqlConfig, false)
        assertNotNull(mysqlClient)
        assertEquals(mysqlConfig.id, mysqlClient?.id)

        // Test MongoDB
        val mongoConfig = TestDatabaseConfigurations.createMongoConfig()
        val mongoClient = connectionService.createConnection(mongoConfig, false)
        assertNotNull(mongoClient)
        assertEquals(mongoConfig.id, mongoClient?.id)

        // Test Cassandra
        val cassandraConfig = TestDatabaseConfigurations.createCassandraConfig()
        val cassandraClient = connectionService.createConnection(cassandraConfig, false)
        assertNotNull(cassandraClient)
        assertEquals(cassandraConfig.id, cassandraClient?.id)
    }

    @Test
    fun `validate client connection configuration`() {

    }

    @Test
    fun `test get client with valid id`() {
        val config = TestDatabaseConfigurations.createPostgresConfig()
        val client = connectionService.createConnection(config, false)

        val retrievedClient = connectionService.getClient(config.id)
        assertNotNull(retrievedClient)
        assertEquals(client?.id, retrievedClient?.id)
    }

    @Test
    fun `test get client with invalid id throws exception`() {
        assertThrows<DatabaseConnectionNotFound> {
            connectionService.getClient(UUID.randomUUID())
        }
    }

    @Test
    fun `test disconnect client`() = runBlocking {
        val config = TestDatabaseConfigurations.createPostgresConfig()
        val client = connectionService.createConnection(config, true)

        assertNotNull(client)
        connectionService.disconnectClient(config.id)

        val retrievedClient = connectionService.getClient(config.id)
        assertEquals(ConnectionState.Disconnected, retrievedClient?.connectionState?.value)
    }

    @Test
    fun `test connect all clients`() = runBlocking {
        // Create multiple connections
        val configs = listOf(
            TestDatabaseConfigurations.createPostgresConfig(),
            TestDatabaseConfigurations.createMySQLConfig(),
            TestDatabaseConfigurations.createMongoConfig(),
            TestDatabaseConfigurations.createCassandraConfig()
        )

        configs.forEach {
            connectionService.createConnection(it, false)
        }

        connectionService.connectAll()

        // Verify all clients are connected
        connectionService.getAllClients().forEach { client ->
            assertTrue(client.isConnected())
        }
    }

    @Test
    fun `test disconnect all clients`() = runBlocking {
        // Create multiple connections
        val configs = listOf(
            TestDatabaseConfigurations.createPostgresConfig(),
            TestDatabaseConfigurations.createMySQLConfig(),
            TestDatabaseConfigurations.createMongoConfig(),
            TestDatabaseConfigurations.createCassandraConfig()
        )

        configs.forEach {
            connectionService.createConnection(it, true)
        }

        connectionService.disconnectAll(true)

        // Verify all clients are removed
        assertTrue(connectionService.getAllClients().isEmpty())
    }
} 