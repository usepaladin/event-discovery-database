package veridius.discover.pojo.configuration

import veridius.discover.models.configuration.ForeignKey
import veridius.discover.models.configuration.PrimaryKey

data class TableMetadataConfiguration(
    var primaryKey: PrimaryKey? = null,
    var foreignKeys: List<ForeignKey> = listOf(),
)

