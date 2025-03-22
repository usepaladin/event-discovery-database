package paladin.discover.services.producer

import ch.qos.logback.classic.Level
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.Message
import paladin.discover.configuration.CloudBinderConfiguration
import paladin.discover.enums.monitoring.ChangeEventHandlerType
import paladin.discover.pojo.producer.DynamicBindingProperties
import paladin.discover.utils.TestLogAppender

@ExtendWith(MockKExtension::class)
class ProducerServiceTest {

    @MockK
    private lateinit var streamBridge: StreamBridge

    @MockK
    private lateinit var topicBindingService: TopicBindingService

    @MockK
    private lateinit var cloudBinderConfiguration: CloudBinderConfiguration

    private lateinit var producerService: ProducerService

    private lateinit var testAppender: TestLogAppender
    private val logger: KLogger = KotlinLogging.logger {}
    private lateinit var logbackLogger: ch.qos.logback.classic.Logger

    @BeforeEach
    fun setup() {
        // Configure logger
        logbackLogger = LoggerFactory.getLogger(logger.name) as ch.qos.logback.classic.Logger
        testAppender = TestLogAppender.factory(logbackLogger, Level.DEBUG)
        producerService = ProducerService(streamBridge, cloudBinderConfiguration, topicBindingService, logger)
    }

    @Test
    fun `test sendMessage with binder parameter`() {
        // Setup
        val binding = "test-binding"
        val binder = "test-binder"
        val key = "test-key"
        val payload = "test-payload"
        every { streamBridge.send(any(), any(), any<Message<*>>()) } returns true
        // Execute
        producerService.sendMessage(binding, binder, key, payload)

        // Verify
        verify { streamBridge.send(eq(binding), eq(binder), any<Message<*>>()) }
    }

    @Test
    fun `test sendMessage with binding config creates new binding if it doesn't exist`() {
        // Setup
        val binding = "test-binding"
        val bindingConfig = DynamicBindingProperties(
            topicName = "test-topic",
            groupName = "test-group",
            binder = "test-binder",
            contentType = ChangeEventHandlerType.JSON
        )
        val key = "test-key"
        val payload = "test-payload"

        every { topicBindingService.hasBinding(any()) } returns false
        every { topicBindingService.createDynamicTopicBinding(any(), any()) } just Runs
        every { streamBridge.send(any(), any<Message<*>>()) } returns true

        // Execute
        producerService.sendMessage(binding, bindingConfig, key, payload)

        // Verify
        verify { topicBindingService.hasBinding(eq(binding)) }
        verify { topicBindingService.createDynamicTopicBinding(eq(binding), eq(bindingConfig)) }
        verify { streamBridge.send(eq(binding), any<Message<*>>()) }
    }

    @Test
    fun `test sendMessage with binding config reuses existing binding`() {
        // Setup
        val binding = "test-binding"
        val bindingConfig = DynamicBindingProperties(
            topicName = "test-topic",
            groupName = "test-group",
            binder = "test-binder",
            contentType = ChangeEventHandlerType.JSON
        )
        val key = "test-key"
        val payload = "test-payload"

        every { topicBindingService.hasBinding(any()) } returns true
        every { streamBridge.send(any(), any<Message<*>>()) } returns true

        // Execute
        producerService.sendMessage(binding, bindingConfig, key, payload)

        // Verify
        verify { topicBindingService.hasBinding(eq(binding)) }
        verify(exactly = 0) { topicBindingService.createDynamicTopicBinding(any(), any()) }
        verify { streamBridge.send(eq(binding), any<Message<*>>()) }
    }
    
}