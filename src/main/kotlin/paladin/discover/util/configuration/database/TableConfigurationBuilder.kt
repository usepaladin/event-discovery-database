package paladin.discover.util.configuration.database

import paladin.discover.models.configuration.database.DatabaseTable

interface TableConfigurationBuilder {
    fun getDatabaseProperties(): List<DatabaseTable>
}