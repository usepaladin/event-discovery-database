package paladin.discover.pojo.monitoring

import paladin.discover.enums.monitoring.ChangeEventOperation
import paladin.discover.models.common.DatabaseType

data class ChangeEventDataKey(
    val database: DatabaseType,
    val operation: ChangeEventOperation,
    val namespace: String?,
    val table: String
)