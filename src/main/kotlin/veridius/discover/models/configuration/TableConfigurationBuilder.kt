package veridius.discover.models.configuration

interface TableConfigurationBuilder {
    fun getDatabaseProperties(): List<DatabaseTable>
}