package paladin.discover.util.configuration

import paladin.discover.models.configuration.DatabaseTable

interface TableConfigurationBuilder {
    fun getDatabaseProperties(): List<DatabaseTable>
}