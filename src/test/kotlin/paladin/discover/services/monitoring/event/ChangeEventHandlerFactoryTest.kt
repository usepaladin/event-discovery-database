package paladin.discover.services.monitoring.event

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import paladin.discover.enums.monitoring.ChangeEventHandlerType
import paladin.discover.models.monitoring.changeEvent.AvroChangeEventHandler
import paladin.discover.models.monitoring.changeEvent.JsonChangeEventHandler
import paladin.discover.models.monitoring.changeEvent.ProtobufChangeEventHandler
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.pojo.monitoring.DatabaseMonitoringConnector
import paladin.discover.services.monitoring.ChangeEventHandlerFactory
import paladin.discover.services.monitoring.MonitoringMetricsService
import paladin.discover.services.producer.ProducerService
import paladin.discover.utils.TestLogAppender
import java.util.*
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class ChangeEventHandlerFactoryTest {

    @MockK
    private lateinit var producerService: ProducerService

    @MockK
    private lateinit var monitoringMetricsService: MonitoringMetricsService

    @MockK
    private lateinit var connector: DatabaseMonitoringConnector

    @MockK
    private lateinit var client: DatabaseClient

    private lateinit var factory: ChangeEventHandlerFactory
    private lateinit var testAppender: TestLogAppender
    private val logger: KLogger = KotlinLogging.logger {}
    private lateinit var logbackLogger: Logger

    @BeforeEach
    fun setup() {
        factory = ChangeEventHandlerFactory(producerService, monitoringMetricsService, logger)

        // Configure logger
        logbackLogger = LoggerFactory.getLogger(logger.name) as Logger
        testAppender = TestLogAppender.factory(logbackLogger, Level.DEBUG)

        every { connector.client } returns client
        every { connector.getDatabaseChangeEventHandler() } returns ChangeEventHandlerType.JSON
        every { connector.getConnectorProps() } returns mockk<Properties>()
    }

    @Test
    fun `test createChangeEventHandler returns JsonChangeEventHandler for JSON type`() {
        // Setup
        every { connector.getDatabaseChangeEventHandler() } returns ChangeEventHandlerType.JSON
        // Execute
        val result = factory.createChangeEventHandler(connector)

        // Verify
        assertTrue(result is JsonChangeEventHandler)
        verify { connector.getDatabaseChangeEventHandler() }
    }

    @Test
    fun `test createChangeEventHandler returns AvroChangeEventHandler for AVRO type`() {
        // Setup
        every { connector.getDatabaseChangeEventHandler() } returns ChangeEventHandlerType.AVRO

        // Execute
        val result = factory.createChangeEventHandler(connector)

        // Verify
        assertTrue(result is AvroChangeEventHandler)
        verify { connector.getDatabaseChangeEventHandler() }
    }

    @Test
    fun `test createChangeEventHandler returns ProtobufChangeEventHandler for PROTOBUF type`() {
        // Setup
        every { connector.getDatabaseChangeEventHandler() } returns ChangeEventHandlerType.PROTOBUF

        // Execute
        val result = factory.createChangeEventHandler(connector)

        // Verify
        assertTrue(result is ProtobufChangeEventHandler)
        verify { connector.getDatabaseChangeEventHandler() }
    }
}