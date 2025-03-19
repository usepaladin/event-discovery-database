package paladin.discover.services.producer

import org.springframework.cloud.stream.binding.BindingService
import org.springframework.cloud.stream.config.BindingProperties
import org.springframework.cloud.stream.config.BindingServiceProperties
import org.springframework.stereotype.Service
import paladin.discover.enums.monitoring.ChangeEventHandlerType
import paladin.discover.pojo.producer.DynamicBindingProperties

@Service
class TopicBindingService(
    private val bindingServiceProperties: BindingServiceProperties,
    private val bindingService: BindingService
) {

    fun createDynamicTopicBinding(bindingName: String, topicBindingProperties: DynamicBindingProperties) {
        if (hasBinding(bindingName)) return
        val bindingProperties = bindingServiceProperties.bindings.computeIfAbsent(bindingName) {
            BindingProperties().apply {
                destination = topicBindingProperties.topicName
                binder = topicBindingProperties.binder
                contentType = getTopicContentType(topicBindingProperties.contentType)
            }
        }

        bindingProperties.producer.isErrorChannelEnabled = true
        bindingService.bindProducer(bindingProperties, bindingName)
    }

    fun hasBinding(bindingName: String): Boolean {
        return bindingServiceProperties.bindings.containsKey(bindingName)
    }

    private fun getTopicContentType(type: ChangeEventHandlerType): String {
        return when (type) {
            ChangeEventHandlerType.JSON -> "application/json"
            ChangeEventHandlerType.AVRO -> "application/avro"
            ChangeEventHandlerType.PROTOBUF -> "application/protobuf"
        }
    }
}