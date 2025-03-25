package paladin.discover.services.monitoring

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.format.Json
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import paladin.discover.configuration.properties.DebeziumConfigurationProperties
import paladin.discover.models.configuration.database.TableConfiguration
import paladin.discover.models.connection.DatabaseConnectionConfiguration
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.pojo.monitoring.ChangeEventFormatHandler
import paladin.discover.services.configuration.database.TableConfigurationService
import paladin.discover.services.connection.ConnectionService
import paladin.discover.utils.TestColumnConfigurations
import paladin.discover.utils.TestConnectionConfig
import paladin.discover.utils.TestDatabaseConfigurations
import paladin.discover.utils.TestLogAppender
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.function.Consumer
import kotlin.test.Test
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class MonitoringServiceTest {

    private lateinit var debeziumConfigProperties: DebeziumConfigurationProperties

    @MockK
    private lateinit var connectionService: ConnectionService

    @MockK
    private lateinit var configurationService: TableConfigurationService


    @MockK
    private lateinit var changeEventHandlerFactory: ChangeEventHandlerFactory

    @MockK
    private lateinit var mockEngine: DebeziumEngine<ChangeEvent<String, String>>

    @MockK
    private lateinit var mockExecutor: ExecutorService

    private lateinit var monitoringService: MonitoringService
    private lateinit var testAppender: TestLogAppender
    private val logger: KLogger = KotlinLogging.logger {}
    private lateinit var logbackLogger: Logger

    @BeforeEach
    fun setup() {
        // Initialize mocks
        debeziumConfigProperties = TestConnectionConfig.mockFileBackendStorage()

        // Initialize service
        monitoringService = MonitoringService(
            debeziumConfigProperties,
            connectionService,
            configurationService,
            changeEventHandlerFactory,
            logger
        )
        // Mock internal executor service
        val executorField = MonitoringService::class.java.getDeclaredField("executor")
        executorField.isAccessible = true
        mockExecutor = mockk<ExecutorService>()
        executorField.set(monitoringService, mockExecutor)

        every { mockExecutor.execute(any()) } just Runs
        every { mockExecutor.shutdown() } just Runs

        // Configure logger
        logbackLogger = LoggerFactory.getLogger(logger.name) as Logger
        testAppender = TestLogAppender.factory(logbackLogger, Level.DEBUG)
    }

    @AfterEach
    fun teardown() {
        // Detach the appender to prevent leaks and interference
        logbackLogger.detachAppender(testAppender)
        testAppender.stop()
    }

    @Test
    fun `test startMonitoring with multiple clients`() {
        // Create test data
        val postgresConfig = TestDatabaseConfigurations.createPostgresConfig()
        val mySqlConfig = TestDatabaseConfigurations.createMySQLConfig()

        val postgresMockClient = mockk<DatabaseClient>()
        val mySqlMockClient = mockk<DatabaseClient>()

        // Configure client mocks
        every { postgresMockClient.id } returns postgresConfig.id
        every { postgresMockClient.config } returns postgresConfig

        every { mySqlMockClient.id } returns mySqlConfig.id
        every { mySqlMockClient.config } returns mySqlConfig

        every { connectionService.getAllClients() } returns listOf(postgresMockClient, mySqlMockClient)

        val (primaryKey, columns) = TestColumnConfigurations.generateSampleTableColumn()

        val postgresTableConfigs: TableConfiguration =
            TestColumnConfigurations.createMockPostgresTableConfig(postgresConfig.id, primaryKey, columns)
        val mySqlTableConfigs: TableConfiguration =
            TestColumnConfigurations.createMockMySQLTableConfig(mySqlConfig.id, primaryKey, columns)

        every { configurationService.getDatabaseClientTableConfiguration(postgresMockClient) } returns listOf(
            postgresTableConfigs
        )
        every { configurationService.getDatabaseClientTableConfiguration(mySqlMockClient) } returns listOf(
            mySqlTableConfigs
        )

        // Mock engine creation with explicit type parameters
        val mockEngine: DebeziumEngine<ChangeEvent<*, *>> = mockk()
        val mockEventHandler: ChangeEventFormatHandler<*, *> = mockk()
        every {
            changeEventHandlerFactory.createChangeEventHandler(any())
        } returns mockEventHandler

        // Use specific argument matchers for the overloaded methods
        mockkStatic(DebeziumEngine::class)
        every {
            DebeziumEngine.create(Json::class.java)
        } returns mockk()

        every {
            mockEventHandler.createEngine()
        } returns mockEngine


        // Execute method under test
        monitoringService.startMonitoring()

        // Verify interactions
        verify(exactly = 1) { connectionService.getAllClients() }
        verify(exactly = 1) { configurationService.getDatabaseClientTableConfiguration(postgresMockClient) }
        verify(exactly = 1) { configurationService.getDatabaseClientTableConfiguration(mySqlMockClient) }
        verify(exactly = 2) { mockExecutor.execute(any()) }

        assertTrue { testAppender.logs.count { it.formattedMessage.contains("CDC Monitoring Service => Database Id: ${postgresConfig.id} => Starting Monitoring Engine") } == 1 }
        assertTrue { testAppender.logs.count { it.formattedMessage.contains("CDC Monitoring Service => Database Id: ${postgresConfig.id} => Monitoring Engine Instantiated and Started") } == 1 }
        assertTrue { testAppender.logs.count { it.formattedMessage.contains("CDC Monitoring Service => Database Id: ${mySqlConfig.id} => Starting Monitoring Engine") } == 1 }
        assertTrue { testAppender.logs.count { it.formattedMessage.contains("CDC Monitoring Service => Database Id: ${mySqlConfig.id} => Monitoring Engine Instantiated and Started") } == 1 }
    }

    @Test
    fun `test startMonitoringEngine with engine creation failure`() {
        // Create test data
        val postgresConfig = TestDatabaseConfigurations.createPostgresConfig()
        val mockClient = mockk<DatabaseClient>()

        // Configure client mock
        every { mockClient.id } returns postgresConfig.id
        every { mockClient.config } returns postgresConfig

        // Mock table configurations
        val (primaryKey, columns) = TestColumnConfigurations.generateSampleTableColumn()
        val postgresTableConfig: TableConfiguration =
            TestColumnConfigurations.createMockPostgresTableConfig(postgresConfig.id, primaryKey, columns)

        every { configurationService.getDatabaseClientTableConfiguration(mockClient) } returns listOf(
            postgresTableConfig
        )

        // Mock engine creation with exception
        mockkStatic(DebeziumEngine::class)
        val builderMock = mockk<DebeziumEngine.Builder<ChangeEvent<String, String>>>()
        val exception = RuntimeException("Failed to create engine")

        every {
            DebeziumEngine.create(Json::class.java)
        } returns builderMock

        every { builderMock.using(any<Properties>()) } returns builderMock
        every { builderMock.notifying(any<Consumer<ChangeEvent<String, String>>>()) } returns builderMock
        every { builderMock.build() } throws exception

        // Execute method under test
        monitoringService.startMonitoringEngine(mockClient)

        // Verify interactions
        verify(exactly = 1) { configurationService.getDatabaseClientTableConfiguration(mockClient) }
        verify(exactly = 0) { mockExecutor.execute(any()) }

        println(testAppender.logs)
        assertTrue { testAppender.logs.count { it.formattedMessage.contains("CDC Monitoring Service => Database Id: ${postgresConfig.id} => Starting Monitoring Engine") } == 1 }
        assertTrue {
            testAppender.logs.count {
                it.level == Level.ERROR && it.formattedMessage.contains("${postgresConfig.id}") && it.formattedMessage.contains(
                    "Failed to Start Monitoring Engine"
                )
            } == 1
        }
    }

    @Test
    fun `test stopMonitoringEngine success`() {
        // Create test data
        val databaseId = UUID.randomUUID()
        val mockClient: DatabaseClient = createMockkClient(databaseId)

        // Mock the engines map
        val enginesField = MonitoringService::class.java.getDeclaredField("monitoringEngines")
        enginesField.isAccessible = true
        val engines =
            enginesField.get(monitoringService) as MutableMap<UUID, DebeziumEngine<ChangeEvent<String, String>>>
        engines[mockClient.id] = mockEngine

        every { mockEngine.close() } just Runs

        // Execute method under test
        monitoringService.stopMonitoringEngine(mockClient)

        // Verify interactions
        verify(exactly = 1) { mockEngine.close() }

        // Verify logger calls
        assertTrue {
            testAppender.logs.count {
                it.formattedMessage.contains("Database Id: $databaseId") && it.formattedMessage.contains(
                    "Monitoring Engine Stopped"
                )
            } == 1
        }

        // Verify engine removed from map
        assertTrue(engines.isEmpty())
    }

    @Test
    fun `test stopMonitoringEngine with exception`() {
        // Create test data
        val databaseId = UUID.randomUUID()
        val mockClient: DatabaseClient = createMockkClient(databaseId)

        val exception = IOException("Failed to close engine")

        // Mock the engines map
        val enginesField = MonitoringService::class.java.getDeclaredField("monitoringEngines")
        enginesField.isAccessible = true
        val engines =
            enginesField.get(monitoringService) as MutableMap<UUID, DebeziumEngine<ChangeEvent<String, String>>>
        engines[databaseId] = mockEngine

        every { mockEngine.close() } throws exception

        // Execute method under test
        monitoringService.stopMonitoringEngine(mockClient)

        // Verify interactions
        verify(exactly = 1) { mockEngine.close() }

        assertTrue {
            testAppender.logs.count {
                it.level == Level.ERROR && it.formattedMessage.contains("$databaseId") && it.formattedMessage.contains(
                    "Failed to close engine"
                )
            } == 1
        }

        assertTrue(engines.isEmpty())
    }

    @Test
    fun `test stopMonitoringEngine with engine not found`() {
        // Create test data
        val databaseId = UUID.randomUUID()
        val mockClient: DatabaseClient = createMockkClient(databaseId)

        monitoringService.stopMonitoringEngine(mockClient)
        verify { mockEngine wasNot Called }

    }

    @Test
    fun `test stopMonitoringEngine on one client with multiple active clients`() {
        // Create test data
        val databaseId = UUID.randomUUID()
        val mockClient: DatabaseClient = createMockkClient(databaseId)
        val databaseId2 = UUID.randomUUID()
        val databaseId3 = UUID.randomUUID()

        // Mock the engines map
        val enginesField = MonitoringService::class.java.getDeclaredField("monitoringEngines")
        enginesField.isAccessible = true
        val engines =
            enginesField.get(monitoringService) as MutableMap<UUID, DebeziumEngine<ChangeEvent<String, String>>>

        // Simulate Multiple active clients
        engines[databaseId] = mockEngine
        engines[databaseId2] = mockEngine
        engines[databaseId3] = mockEngine

        every { mockEngine.close() } just Runs

        // Execute method under test
        monitoringService.stopMonitoringEngine(mockClient)

        // Verify interactions
        verify(exactly = 1) { mockEngine.close() }

        // Verify logger calls
        assertTrue {
            testAppender.logs.count {
                it.formattedMessage.contains("Database Id: $databaseId") && it.formattedMessage.contains(
                    "Monitoring Engine Stopped"
                )
            } == 1
        }

        // Verify engine removed from map
        assertTrue(engines.size == 2 && engines.containsKey(databaseId2) && engines.containsKey(databaseId3))
    }

    @Test
    fun `test shutdownMonitoring`() {
        // Create test data
        val databaseId1 = UUID.randomUUID()
        val databaseId2 = UUID.randomUUID()
        val mockEngine1 = mockk<DebeziumEngine<ChangeEvent<String, String>>>()
        val mockEngine2 = mockk<DebeziumEngine<ChangeEvent<String, String>>>()

        // Mock the engines map
        val enginesField = MonitoringService::class.java.getDeclaredField("monitoringEngines")
        enginesField.isAccessible = true
        val engines =
            enginesField.get(monitoringService) as MutableMap<UUID, DebeziumEngine<ChangeEvent<String, String>>>
        engines[databaseId1] = mockEngine1
        engines[databaseId2] = mockEngine2

        every { mockExecutor.awaitTermination(any(), any()) } returns true
        every { mockEngine1.close() } just Runs
        every { mockEngine2.close() } just Runs

        // Execute method under test
        monitoringService.shutdownMonitoring()

        // Verify interactions
        verify(exactly = 1) { mockEngine1.close() }
        verify(exactly = 1) { mockEngine2.close() }
        verify(exactly = 1) { mockExecutor.shutdown() }

        // Verify logger calls
        assertTrue { testAppender.logs.count { it.formattedMessage.contains("Database Id: $databaseId1") } == 1 }
        assertTrue { testAppender.logs.count { it.formattedMessage.contains("Database Id: $databaseId2") } == 1 }
        assertTrue { testAppender.logs.count { it.formattedMessage.contains("All Database Monitoring Connections Shut Down") } == 1 }

        // Verify engines removed from map
        assertTrue(engines.isEmpty())
    }

    private fun createMockkClient(id: UUID): DatabaseClient {
        val mockClient: DatabaseClient = mockk()
        val mockkClientConfig: DatabaseConnectionConfiguration = mockk()
        every { mockkClientConfig.connectionName } returns "Test Connection"
        every { mockClient.id } returns id
        every { mockClient.config } returns mockkClientConfig

        return mockClient
    }


}