package veridius.discover.services.connection


import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import veridius.discover.pojo.client.DatabaseClient
import veridius.discover.pojo.client.DatabaseClient.ClientConnectionState
import veridius.discover.utils.TestDatabaseConfigurations
import veridius.discover.utils.TestLogAppender
import java.util.*

@ExperimentalCoroutinesApi
class ConnectionMonitoringServiceTest {
    private lateinit var connectionService: ConnectionService
    private lateinit var monitoringService: ConnectionMonitoringService
    private lateinit var testLogAppender: TestLogAppender
    private val testDispatcher = UnconfinedTestDispatcher()

    private val logger: KLogger = KotlinLogging.logger {}

    // Underlying logback logger
    private lateinit var logbackLogger: Logger

    @BeforeEach
    fun setup() {
        connectionService = mockk<ConnectionService>()
        logbackLogger = LoggerFactory.getLogger(logger.name) as Logger
        testLogAppender = TestLogAppender.factory(logbackLogger, Level.DEBUG)
        monitoringService = ConnectionMonitoringService(connectionService, logger, testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        logbackLogger.detachAppender(testLogAppender)
        testLogAppender.stop()
    }

    @Test
    fun `test monitoring service detects disconnected clients`() = runTest {
        val config = TestDatabaseConfigurations.createPostgresConfig()
        val mockClient = mockk<DatabaseClient>()
        val connectionStateFlow: MutableStateFlow<ClientConnectionState> =
            MutableStateFlow(ClientConnectionState.Disconnected)

        coEvery { mockClient.id } returns config.id
        coEvery { mockClient.config } returns config
        coEvery { mockClient.isConnected() } returns false
        // Return our MutableStateFlow
        every { mockClient.connectionState } returns connectionStateFlow

        coEvery { mockClient.connect() } answers {
            connectionStateFlow.value = ClientConnectionState.Connected
            Any()
        }

        coEvery { connectionService.getAllClients() } returns listOf(mockClient)

        // Start monitoring
        monitoringService.monitorDatabaseConnections()

        // Verify that the monitoring service detected the disconnected state and attempted to reconnect
        coVerify(exactly = 1, timeout = 5000) { mockClient.connect() }
    }

    @Test
    fun `test monitoring service with multiple clients and connection failure`() = runTest(testDispatcher) {
        val config1 = TestDatabaseConfigurations.createPostgresConfig()
        val config2 = TestDatabaseConfigurations.createPostgresConfig()

        //Mock Multiple Database Clients
        val mockClient1 = mockk<DatabaseClient>()
        val connectionStateFlow1: MutableStateFlow<ClientConnectionState> =
            MutableStateFlow(ClientConnectionState.Disconnected)
        val mockClient2 = mockk<DatabaseClient>()
        val connectionStateFlow2: MutableStateFlow<ClientConnectionState> =
            MutableStateFlow(ClientConnectionState.Disconnected)

        //Mock Configuration
        every { mockClient1.id } returns config1.id
        every { mockClient1.config } returns config1

        every { mockClient2.id } returns config2.id
        every { mockClient2.config } returns config2

        // Mock Clients initiate state disconnected
        coEvery { mockClient1.isConnected() } returns false
        every { mockClient1.connectionState } returns connectionStateFlow1

        coEvery { mockClient2.isConnected() } returns false
        every { mockClient2.connectionState } returns connectionStateFlow2

        // Mock Client 1 will connect successfully
        coEvery { mockClient1.connect() } answers {
            connectionStateFlow1.value = ClientConnectionState.Connected
            Any()
        }

        // Mock Client 2 will fail to connect
        coEvery { mockClient2.connect() } throws Exception("Failed to connect") // Simulate failure

        coEvery { connectionService.getAllClients() } returns listOf(mockClient1, mockClient2)

        monitoringService.monitorDatabaseConnections() //start monitoring
        advanceTimeBy(10000)
        coVerify(exactly = 1, timeout = 5000) { mockClient1.connect() } //verify client 1 connects
        coVerify(exactly = 1, timeout = 5000) { mockClient2.connect() } //verify client 2 attempts to connect

        advanceTimeBy(10000)
        assertTrue(connectionStateFlow1.value == ClientConnectionState.Connected) //check the mutable state flow
        monitoringService.endMonitoring()
//         Expect one logging exception (Client 2 Connection)
        assertTrue {
            testLogAppender.logs.any {
                it.level == Level.ERROR && it.message.contains("Database Reconnect Unsuccessful")
            }
        }
    }


    @Test
    fun `Test monitoring service repeated polls`() = runTest(testDispatcher) {
        val config = TestDatabaseConfigurations.createPostgresConfig()
        val mockClient = mockk<DatabaseClient>()
        val connectionStateFlow: MutableStateFlow<ClientConnectionState> =
            MutableStateFlow(ClientConnectionState.Disconnected)

        coEvery { mockClient.id } returns config.id
        coEvery { mockClient.config } returns config

        // Should fail on first iteration, and continue to pass to mimic a successful connection
        coEvery { mockClient.isConnected() } returnsMany listOf(false, true)

        every { mockClient.connectionState } returns connectionStateFlow

        every { mockClient.updateConnectionState(ClientConnectionState.Connected) } answers {
            connectionStateFlow.value = ClientConnectionState.Connected
        }

        coEvery { mockClient.connect() } answers {
            connectionStateFlow.value = ClientConnectionState.Connected
            Any()
        }
        coEvery { connectionService.getAllClients() } returns listOf(mockClient)

        monitoringService.monitorDatabaseConnections()

        advanceTimeBy(10000)
        coVerify(exactly = 1, timeout = 5000) { mockClient.isConnected() }
        coVerify(exactly = 1, timeout = 5000) { mockClient.connect() }

        advanceTimeBy(30000)

        coVerify(exactly = 2, timeout = 5000) { mockClient.isConnected() }
        coVerify(exactly = 1, timeout = 5000) { mockClient.connect() }

        advanceTimeBy(30000)
        coVerify(exactly = 3, timeout = 5000) { mockClient.isConnected() }
        coVerify(exactly = 1, timeout = 5000) { mockClient.connect() }
        monitoringService.endMonitoring()

    }

    @Test
    fun `test observeConnectionStates with zero clients`() = runTest {
        coEvery { connectionService.getAllClients() } returns emptyList()

        val states = monitoringService.observeConnectionStates().toList()
        // The flow should emit an empty map and then complete immediately.
        assertTrue(states.isEmpty() || (states.size == 1 && states[0].isEmpty()))
    }

    @Test
    fun `test observeConnectionStates with multiple state changes`() = runTest(testDispatcher) {
        val config1 = TestDatabaseConfigurations.createPostgresConfig()
        val mockClient1 = mockk<DatabaseClient>()
        val connectionStateFlow1: MutableStateFlow<ClientConnectionState> =
            MutableStateFlow(ClientConnectionState.Disconnected)

        every { mockClient1.id } returns config1.id
        every { mockClient1.config } returns config1
        every { mockClient1.connectionState } returns connectionStateFlow1

        coEvery { connectionService.getAllClients() } returns listOf(mockClient1)

        val states = mutableListOf<Map<UUID, ClientConnectionState>>()
        val job = launch { //Need to launch this to collect in the background
            monitoringService.observeConnectionStates().toList(states)
        }


        connectionStateFlow1.value = ClientConnectionState.Connecting  // Simulate state changes
        connectionStateFlow1.value = ClientConnectionState.Connected
        connectionStateFlow1.value = ClientConnectionState.Disconnected
        connectionStateFlow1.value = ClientConnectionState.Connected


        advanceTimeBy(1000)  // Give the flow time to process

        job.cancel() //cancel collection

        assertTrue(states.size >= 5)
        //Check the states
        assertTrue(states[0].isEmpty())
        assertTrue(states[1][config1.id] == ClientConnectionState.Disconnected)
        assertTrue(states[2][config1.id] == ClientConnectionState.Connecting)
        assertTrue(states[3][config1.id] == ClientConnectionState.Connected)
        assertTrue(states[4][config1.id] == ClientConnectionState.Disconnected)
        assertTrue(states[5][config1.id] == ClientConnectionState.Connected)
    }
}