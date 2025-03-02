package veridius.discover.util.configuration

import veridius.discover.models.configuration.DatabaseTable

interface TableConfigurationBuilder {
    fun getDatabaseProperties(): List<DatabaseTable>
}