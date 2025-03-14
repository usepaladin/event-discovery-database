package paladin.discover.pojo.monitoring

import paladin.discover.enums.monitoring.ChangeEventOperation

data class ChangeEventData(
    private val operation: ChangeEventOperation,
    private val before: Map<String, Any>?,
    private val after: Map<String, Any>?,
    private val source: Map<String, Any>?,
    private val timestamp: Long
)
