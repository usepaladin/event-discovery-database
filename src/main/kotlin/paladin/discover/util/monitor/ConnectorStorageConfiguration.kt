package paladin.discover.util.monitor

import paladin.discover.configuration.properties.DebeziumConfigurationProperties
import paladin.discover.pojo.client.DatabaseClient
import java.util.*

interface ConnectorStorageConfiguration {
    fun validateConfig(config: DebeziumConfigurationProperties, client: DatabaseClient)
    fun applyOffsetStorage(props: Properties, config: DebeziumConfigurationProperties, clientId: UUID)

    /**
     * Use for SQL Based Databases:
     *
     * MySQL
     * SQL Server
     * Oracle
     * Db2
     *
     * Will refuse to start without configuration of database schema history
     * */
    fun applySchemaHistory(props: Properties, config: DebeziumConfigurationProperties, clientId: UUID)
}