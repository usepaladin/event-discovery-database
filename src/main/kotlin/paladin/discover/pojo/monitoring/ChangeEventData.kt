package paladin.discover.pojo.monitoring

import paladin.discover.enums.monitoring.ChangeEventOperation

data class ChangeEventData(
    val operation: ChangeEventOperation,
    val before: Map<String, Any?>?,
    val after: Map<String, Any?>?,
    val source: Map<String, Any?>?,
    val timestamp: Long?,
    val table: String?
)
