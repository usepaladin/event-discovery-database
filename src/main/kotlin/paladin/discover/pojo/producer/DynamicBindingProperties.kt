package paladin.discover.pojo.producer

import paladin.discover.enums.monitoring.ChangeEventHandlerType

data class DynamicBindingProperties(
    val topicName: String,
    val groupName: String,
    val binder: String? = null,
    val contentType: ChangeEventHandlerType = ChangeEventHandlerType.JSON
)

