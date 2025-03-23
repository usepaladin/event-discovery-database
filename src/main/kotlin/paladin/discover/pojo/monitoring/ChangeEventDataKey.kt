package paladin.discover.pojo.monitoring

import paladin.discover.enums.configuration.DatabaseType
import paladin.discover.enums.monitoring.ChangeEventOperation

data class ChangeEventDataKey(
    val database: DatabaseType,
    val operation: ChangeEventOperation,
    val namespace: String?,
    val table: String
)