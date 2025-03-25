package paladin.discover.services.monitoring.event

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.fasterxml.jackson.databind.ObjectMapper
import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.format.Json
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import paladin.discover.enums.configuration.DatabaseType
import paladin.discover.enums.monitoring.ChangeEventOperation
import paladin.discover.models.connection.DatabaseConnectionConfiguration
import paladin.discover.models.monitoring.changeEvent.JsonChangeEventHandler
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.pojo.monitoring.ChangeEventData
import paladin.discover.pojo.monitoring.ChangeEventDataKey
import paladin.discover.pojo.monitoring.DatabaseMonitoringConnector
import paladin.discover.services.metrics.MonitoringMetricsService
import paladin.discover.services.producer.ProducerService
import paladin.discover.utils.TestLogAppender
import java.util.*
import java.util.function.Consumer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class JsonChangeEventHandlerTest {

    @MockK
    private lateinit var connector: DatabaseMonitoringConnector

    @MockK
    private lateinit var client: DatabaseClient

    @MockK
    private lateinit var producerService: ProducerService

    @MockK
    private lateinit var monitoringMetricsService: MonitoringMetricsService

    @MockK
    private lateinit var connectionConfig: DatabaseConnectionConfiguration

    @MockK
    private lateinit var engine: DebeziumEngine<ChangeEvent<String, String>>

    private lateinit var objectMapper: ObjectMapper
    private lateinit var handler: JsonChangeEventHandler

    private lateinit var testLogAppender: TestLogAppender
    private val logger: KLogger = KotlinLogging.logger {}
    private lateinit var logbackLogger: Logger

    @BeforeEach
    fun setup() {
        objectMapper = ObjectMapper()
        every { client.config } returns connectionConfig
        every { connectionConfig.connectionName } returns "test-db"
        every { client.id } returns UUID.randomUUID()
        every { connectionConfig.databaseType } returns DatabaseType.POSTGRES
        every { connector.getConnectorProps() } returns mockk()
        handler = JsonChangeEventHandler(
            connector,
            client,
            producerService,
            monitoringMetricsService,
            logger
        )

        mockkStatic(DebeziumEngine::class)

        val engineBuilder = mockk<DebeziumEngine.Builder<ChangeEvent<String, String>>>()
        every { DebeziumEngine.create(Json::class.java) } returns engineBuilder
        every { engineBuilder.using(any<Properties>()) } returns engineBuilder
        // If the notifying method actually takes a Consumer<ChangeEvent<String, String>>
        every {
            engineBuilder.notifying(any<Consumer<ChangeEvent<String, String>>>())
        } returns engineBuilder
        every { engineBuilder.build() } returns engine
        every { producerService.sendMessage(any(), any<ChangeEventDataKey>(), any<ChangeEventData>()) } just Runs


        // Configure logger
        logbackLogger = LoggerFactory.getLogger(logger.name) as Logger
        testLogAppender = TestLogAppender.factory(logbackLogger, Level.DEBUG)
    }

    @Test
    fun `test createEngine returns engine instance`() {
        val result = handler.createEngine()
        assertNotNull(result)
        verify { DebeziumEngine.create<ChangeEvent<String, String>>(any()) }
    }

    @Test
    fun `test decodeKey parses JSON key`() {
        val keyJson = """{"id": 123}"""

        val result = handler.decodeKey(keyJson)

        assertNotNull(result)
        assertEquals(123, result.get("id").asInt())
    }

    @Test
    fun `test handleObservation with null event`() {
        val event = mockk<ChangeEvent<String, String>>()
        every { event.key() } returns null
        every { event.value() } returns null

        handler.handleObservation(event)

        assertTrue { testLogAppender.logs.count { it.formattedMessage.contains("Record Ignored/Not published => Event Value was null") } == 1 }
        verify(exactly = 0) { producerService.sendMessage(any(), any<ChangeEventDataKey>(), any<ChangeEventData>()) }
    }

    /**
     * I haven't really fleshed out event sending in this ticket, so im going to leave these dormant untill
     * I finalise it, as event sending to Message brokers will most likely change heavily
     */

//    @Test
//    fun `test handleObservation with record change event`() {
//        // Create mock event
//        val event = mockk<ChangeEvent<String, String>>()
//        val keyJson = """{"id": 123}"""
//        val valueJson = """{
//            "op": "c",
//            "before": null,
//            "after": {"id": 123, "name": "Test"},
//            "source": {"db": "test_db", "table": "users"},
//            "ts_ms": 1623884461000
//        }"""
//
//        every { event.key() } returns keyJson
//        every { event.value() } returns valueJson
//
//        // Execute
//        handler.handleObservation(event)
//
//        // Verify
//        verify {
//            producerService.sendMessage(
//                match { it.contains("database-monitoring-record-change-event") },
//                any<ChangeEventDataKey>(),
//                any<ChangeEventData>()
//            )
//        }
//    }
//
//    @Test
//    fun `test handleObservation with metadata event`() {
//        // Create mock event with metadata content
//        val event = mockk<ChangeEvent<String, String>>()
//        val keyJson = """{}"""
//        val valueJson = """{
//            "source": {"db": "test_db"},
//            "heartbeat": true
//        }"""
//
//        every { event.key() } returns keyJson
//        every { event.value() } returns valueJson
//
//        // Execute
//        handler.handleObservation(event)
//
//        // Verify
//        verify {
//            producerService.sendMessage(
//                match { it.contains("database-monitoring-metadata") },
//                any(),
//                any()
//            )
//        }
//    }
//
    @Test
    fun `test handleObservation with incorrect format`() {
        val event = mockk<ChangeEvent<String, String>>()

        // These are technically
        every { event.value() } returns "{}"
        handler.handleObservation(event)
        assertTrue { testLogAppender.logs.count { it.formattedMessage.contains("Failed to process event") } == 1 }
        // Verify exception message was about empty value
        assertTrue { testLogAppender.logs.count { it.throwableProxy != null && it.throwableProxy.message.contains("Decoded Value was empty or null") } == 1 }


        val event2 = mockk<ChangeEvent<String, String>>()
        every { event2.value() } returns "[]"
        handler.handleObservation(event2)
        assertTrue { testLogAppender.logs.count { it.formattedMessage.contains("Failed to process event") } == 2 }
        assertTrue { testLogAppender.logs.count { it.throwableProxy != null && it.throwableProxy.message.contains("Decoded Value was empty or null") } == 2 }

        val event3 = mockk<ChangeEvent<String, String>>()
        every { event3.value() } returns "penis heeheheh :3"
        handler.handleObservation(event3)
        assertTrue { testLogAppender.logs.count { it.formattedMessage.contains("Failed to process event") } == 3 }
        assertTrue { testLogAppender.logs.count { it.throwableProxy != null && it.throwableProxy.message.contains("Value was of an incorrect format") } == 1 }
    }


    @Test
    fun `test decodeValue with CREATE operation`() {
        val jsonNode = objectMapper.readTree(
            """
        {
            "payload":
           {
            "op": "c",
            "before": null,
            "after": {"id": 123, "name": "Test"},
            "source": {"db": "test_db", "table": "users"},
            "ts_ms": 1623884461000
            }
        }
        """
        )

        val result = handler.decodeValue(jsonNode, ChangeEventOperation.CREATE)

        assertEquals(ChangeEventOperation.CREATE, result.operation)
        assertNull(result.before)
        assertNotNull(result.after)
        assertEquals(123, result.after!!["id"])
        assertEquals("Test", result.after!!["name"])
        assertEquals("users", result.table)
        assertEquals(1623884461000, result.timestamp)
    }

    @Test
    fun `test decodeValue with UPDATE operation`() {
        val jsonNode = objectMapper.readTree(
            """
        {
            "payload":
            {
            "op": "u",
            "before": {"id": 123, "name": "Old"},
            "after": {"id": 123, "name": "New"},
            "source": {"db": "test_db", "table": "users"},
            "ts_ms": 1623884461000
            }
        }
        """
        )

        val result = handler.decodeValue(jsonNode, ChangeEventOperation.UPDATE)

        assertEquals(ChangeEventOperation.UPDATE, result.operation)
        assertNotNull(result.before)
        assertEquals("Old", result.before!!["name"])
        assertNotNull(result.after)
        assertEquals("New", result.after!!["name"])
    }

    @Test
    fun `test decodeValue with DELETE operation`() {
        val jsonNode = objectMapper.readTree(
            """
        {
            "payload":
           {
            "op": "d",
            "before": {"id": 123, "name": "Test"},
            "after": null,
            "source": {"db": "test_db", "table": "users"},
            "ts_ms": 1623884461000
            }
        }
        """
        )

        val result = handler.decodeValue(jsonNode, ChangeEventOperation.DELETE)

        assertEquals(ChangeEventOperation.DELETE, result.operation)
        assertNotNull(result.before)
        assertNull(result.after)
    }
}