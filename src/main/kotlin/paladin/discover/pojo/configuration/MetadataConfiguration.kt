package paladin.discover.pojo.configuration

import paladin.discover.models.configuration.ForeignKey
import paladin.discover.models.configuration.PrimaryKey

data class TableMetadataConfiguration(
    var primaryKey: PrimaryKey? = null,
    var foreignKeys: List<ForeignKey> = listOf(),
)

