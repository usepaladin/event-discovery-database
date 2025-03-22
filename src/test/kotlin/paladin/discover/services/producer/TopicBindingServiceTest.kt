package paladin.discover.services.producer

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.cloud.stream.binding.BindingService
import org.springframework.cloud.stream.config.BindingProperties
import org.springframework.cloud.stream.config.BindingServiceProperties
import paladin.discover.enums.monitoring.ChangeEventHandlerType
import paladin.discover.pojo.producer.DynamicBindingProperties
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class TopicBindingServiceTest {

    @MockK
    private lateinit var bindingServiceProperties: BindingServiceProperties

    @MockK
    private lateinit var bindingService: BindingService

    private lateinit var topicBindingService: TopicBindingService
    private val bindings = HashMap<String, BindingProperties>()

    @BeforeEach
    fun setup() {
        bindings.clear()
        every { bindingServiceProperties.bindings } returns bindings
        topicBindingService = TopicBindingService(bindingServiceProperties, bindingService)
    }

    @Test
    fun `test createDynamicTopicBinding creates new binding`() {
        // Setup
        val bindingName = "test-binding"
        val topicBindingProperties = DynamicBindingProperties(
            topicName = "test-topic",
            groupName = "test-group",
            binder = "test-binder",
            contentType = ChangeEventHandlerType.JSON
        )

        // Use a real HashMap that we can verify
        val realBindings = HashMap<String, BindingProperties>()
        every { bindingServiceProperties.bindings } returns realBindings

        // Mock the bindProducer method
        every { bindingService.bindProducer(any<BindingProperties>(), eq(bindingName)) } returns mockk()

        // Execute
        topicBindingService.createDynamicTopicBinding(bindingName, topicBindingProperties)

        // Verify
        assertTrue(realBindings.containsKey(bindingName))
        assertEquals("test-topic", realBindings[bindingName]?.destination)
        assertEquals("test-binder", realBindings[bindingName]?.binder)
        assertEquals("application/json", realBindings[bindingName]?.contentType)
        assertTrue(realBindings[bindingName]?.producer?.isErrorChannelEnabled == true)
        verify { bindingService.bindProducer(any<BindingProperties>(), eq(bindingName)) }
    }

    @Test
    fun `test createDynamicTopicBinding doesn't create binding if it already exists`() {
        // Setup
        val bindingName = "existing-binding"
        val existingBinding = BindingProperties()
        bindings[bindingName] = existingBinding

        val topicBindingProperties = DynamicBindingProperties(
            topicName = "test-topic",
            groupName = "test-group"
        )

        // Execute
        topicBindingService.createDynamicTopicBinding(bindingName, topicBindingProperties)

        // Verify
        assertSame(existingBinding, bindings[bindingName])
        verify(exactly = 0) { bindingService.bindProducer(any<BindingProperties>(), any()) }
    }

    @Test
    fun `test getTopicContentType returns correct content type for JSON`() {
        // Setup - using reflection to access private method
        val method =
            TopicBindingService::class.java.getDeclaredMethod("getTopicContentType", ChangeEventHandlerType::class.java)
        method.isAccessible = true

        // Execute
        val result = method.invoke(topicBindingService, ChangeEventHandlerType.JSON)

        // Verify
        assertEquals("application/json", result)
    }

    @Test
    fun `test getTopicContentType returns correct content type for AVRO`() {
        // Setup - using reflection to access private method
        val method =
            TopicBindingService::class.java.getDeclaredMethod("getTopicContentType", ChangeEventHandlerType::class.java)
        method.isAccessible = true

        // Execute
        val result = method.invoke(topicBindingService, ChangeEventHandlerType.AVRO)

        // Verify
        assertEquals("application/avro", result)
    }

    @Test
    fun `test getTopicContentType returns correct content type for PROTOBUF`() {
        // Setup - using reflection to access private method
        val method =
            TopicBindingService::class.java.getDeclaredMethod("getTopicContentType", ChangeEventHandlerType::class.java)
        method.isAccessible = true

        // Execute
        val result = method.invoke(topicBindingService, ChangeEventHandlerType.PROTOBUF)

        // Verify
        assertEquals("application/protobuf", result)
    }
}