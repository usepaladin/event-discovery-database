package paladin.discover.pojo.configuration.database

import paladin.discover.models.configuration.database.ForeignKey
import paladin.discover.models.configuration.database.PrimaryKey

data class TableMetadataConfiguration(
    var primaryKey: PrimaryKey? = null,
    var foreignKeys: List<ForeignKey> = listOf(),
)

