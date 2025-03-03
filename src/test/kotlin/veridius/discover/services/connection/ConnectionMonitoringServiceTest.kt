package veridius.discover.services.connection


import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mu.KLogger
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import veridius.discover.pojo.client.ConnectionState
import veridius.discover.pojo.client.DatabaseClient
import veridius.discover.utils.TestDatabaseConfigurations
import java.util.*

@ExperimentalCoroutinesApi
class ConnectionMonitoringServiceTest {
    private lateinit var connectionService: ConnectionService
    private lateinit var monitoringService: ConnectionMonitoringService
    private lateinit var logger: KLogger

    @BeforeEach
    fun setup() {
        connectionService = mockk<ConnectionService>()
        logger = mockk<KLogger>(relaxed = true)
        monitoringService = ConnectionMonitoringService(connectionService, logger)
    }

    @Test
    fun `test monitoring service detects disconnected clients`() = runTest {
        val config = TestDatabaseConfigurations.createPostgresConfig()
        val mockClient = mockk<DatabaseClient>()
        val connectionStateFlow: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)

        coEvery { mockClient.id } returns config.id
        coEvery { mockClient.config } returns config
        coEvery { mockClient.isConnected() } returns false
        // Return our MutableStateFlow
        every { mockClient.connectionState } returns connectionStateFlow

        coEvery { mockClient.connect() } answers {
            connectionStateFlow.value = ConnectionState.Connected
            Any()
        }

        coEvery { connectionService.getAllClients() } returns listOf(mockClient)

        // Start monitoring
        monitoringService.monitorDatabaseConnections()

        // Verify that the monitoring service detected the disconnected state and attempted to reconnect
        coVerify(exactly = 1, timeout = 5000) { mockClient.connect() }
    }

    @Test
    fun `test monitoring service ignores paused clients`() = runTest {
        val config = TestDatabaseConfigurations.createPostgresConfig()
        val mockClient = mockk<DatabaseClient>()
        val connectionStateFlow: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Paused)

        coEvery { mockClient.id } returns config.id
        coEvery { mockClient.config } returns config
        coEvery { mockClient.isConnected() } returns false
        // Return our MutableStateFlow
        every { mockClient.connectionState } returns connectionStateFlow

        coEvery { mockClient.connect() } answers {
            connectionStateFlow.value = ConnectionState.Connected
            Any()
        }

        coEvery { connectionService.getAllClients() } returns listOf(mockClient)

        // Start monitoring
        monitoringService.monitorDatabaseConnections()

        // Verify that the monitoring service detected the disconnected state and attempted to reconnect
        coVerify(exactly = 0, timeout = 5000) { mockClient.connect() }
    }

    @Test
    fun `test monitoring service handles connection state changes`() = runTest {
        val config = TestDatabaseConfigurations.createPostgresConfig()
        val mockClient = mockk<DatabaseClient>()
        val connectionStateFlow: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Connected)

        coEvery { mockClient.id } returns config.id
        coEvery { mockClient.config } returns config
        coEvery { mockClient.isConnected() } returns true
        every { mockClient.connectionState } returns connectionStateFlow
        coEvery { connectionService.getAllClients() } returns listOf(mockClient)

        monitoringService.monitorDatabaseConnections()

        // Verify that the monitoring service detected the connected state
        coVerify(timeout = 5000) { mockClient.isConnected() }
    }

    @Test
    fun `test monitoring service with multiple clients and connection failure`() = runTest {
        val config1 = TestDatabaseConfigurations.createPostgresConfig()
        val config2 = TestDatabaseConfigurations.createPostgresConfig()

        //Mock Multiple Database Clients
        val mockClient1 = mockk<DatabaseClient>()
        val connectionStateFlow1: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)
        val mockClient2 = mockk<DatabaseClient>()
        val connectionStateFlow2: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)

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
            connectionStateFlow1.value = ConnectionState.Connected
            Any()
        }

        // Mock Client 2 will fail to connect
        coEvery { mockClient2.connect() } throws Exception("Failed to connect") // Simulate failure

        coEvery { connectionService.getAllClients() } returns listOf(mockClient1, mockClient2)

        monitoringService.monitorDatabaseConnections() //start monitoring
        advanceUntilIdle()
        coVerify(exactly = 1, timeout = 5000) { mockClient1.connect() } //verify client 1 connects
        coVerify(exactly = 1, timeout = 5000) { mockClient2.connect() } //verify client 2 attempts to connect
        advanceUntilIdle()

        // Expect one logging exception (Client 2 Connection)
        verify(exactly = 1, timeout = 10000) {
            logger.error(
                t = any(),
                msg = any()
            )

        } //verify error log
        assertTrue(connectionStateFlow1.value == ConnectionState.Connected) //check the mutable state flow
    }


    @Test
    fun `Test Monitoring Service connection Delay`() = runTest {
        val config = TestDatabaseConfigurations.createPostgresConfig()
        val mockClient = mockk<DatabaseClient>()
        val connectionStateFlow: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)

        coEvery { mockClient.id } returns config.id
        coEvery { mockClient.config } returns config
        coEvery { mockClient.isConnected() } returns false andThen true andThen true
        // Return our MutableStateFlow
        every { mockClient.connectionState } returns connectionStateFlow

        every { mockClient.updateConnectionState(ConnectionState.Connected) } answers {
            connectionStateFlow.value = ConnectionState.Connected
        }

        coEvery { mockClient.connect() } answers {
            connectionStateFlow.value = ConnectionState.Connected
            Any()
        }

        coEvery { connectionService.getAllClients() } returns listOf(mockClient)

        // Start monitoring
        monitoringService.monitorDatabaseConnections()
        // Advance time just enough to process initial checks but not hit the second loop iteration


    }

//    @Test
//    fun `test endMonitoring cancels the scope`() = runTest {
//        //Since we don't expose the scope, we can check if it active after calling a function that uses the scope, then checking again after calling endMonitoring().
//
//        monitoringService.monitorDatabaseConnections() // Start a long-running operation
//        Assertions.assertTrue(monitoringService.scope.isActive)
//
//        monitoringService.endMonitoring()
//        kotlinx.coroutines.delay(100) //allow for clean up
//        assertTrue(monitoringService.scope.isActive.not())
//    }

    @Test
    fun `test observeConnectionStates with zero clients`() = runTest {
        coEvery { connectionService.getAllClients() } returns emptyList()

        val states = monitoringService.observeConnectionStates().toList()
        // The flow should emit an empty map and then complete immediately.
        assertTrue(states.isEmpty() || (states.size == 1 && states[0].isEmpty()))
    }

    @Test
    fun `test observeConnectionStates with multiple state changes`() = runTest {
        val config1 = TestDatabaseConfigurations.createPostgresConfig()
        val mockClient1 = mockk<DatabaseClient>()
        val connectionStateFlow1: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)

        every { mockClient1.id } returns config1.id
        every { mockClient1.config } returns config1
        every { mockClient1.connectionState } returns connectionStateFlow1

        coEvery { connectionService.getAllClients() } returns listOf(mockClient1)

        val states = mutableListOf<Map<UUID, ConnectionState>>()
        val job = launch { //Need to launch this to collect in the background
            monitoringService.observeConnectionStates().toList(states)
        }


        connectionStateFlow1.value = ConnectionState.Connecting  // Simulate state changes
        connectionStateFlow1.value = ConnectionState.Connected
        connectionStateFlow1.value = ConnectionState.Disconnected

        advanceTimeBy(100)  // Give the flow time to process

        job.cancel() //cancel collection

        assertTrue(states.size >= 4) // Initial + 3 changes (could be more due to timing)
        //Check the states
        assertTrue(states[0].isEmpty())
        assertTrue(states[1][config1.id] == ConnectionState.Disconnected)
        assertTrue(states[2][config1.id] == ConnectionState.Connecting)
        assertTrue(states[3][config1.id] == ConnectionState.Connected)
        //Further checks could be added to assert order.


    }
} 