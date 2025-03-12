package paladin.discover.services.configuration

import io.github.oshai.kotlinlogging.KLogger
import jakarta.transaction.Transactional
import kotlinx.coroutines.*
import org.springframework.stereotype.Service
import paladin.discover.entities.configuration.TableMonitoringConfigurationEntity
import paladin.discover.models.configuration.DatabaseTable
import paladin.discover.models.configuration.TableConfiguration
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.pojo.configuration.TableColumnConfiguration
import paladin.discover.pojo.configuration.TableMetadataConfiguration
import paladin.discover.repositories.configuration.TableConfigurationRepository
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class TableConfigurationService(
    private val tableMonitoringRepository: TableConfigurationRepository,
    private val logger: KLogger
) {

    private val tableConfigurations: ConcurrentHashMap<UUID, TableConfiguration> = ConcurrentHashMap()

    /**
     * Retrieves table configurations associated with the given database client.
     *
     * @param client The database client to retrieve table configurations for
     * @return List of table configurations associated with the client
     */
    fun getDatabaseClientTableConfiguration(client: DatabaseClient): List<TableConfiguration> {
        return client.config.tableConfigurations.mapNotNull { tableConfigurations[it.id] }
    }

    /**
     * Scan database client to obtain all relevant metadata for a tables database table configuration, which includes:
     *  - Database Schemas (If applicable)
     *  - Database Tables
     *  - Table Columns
     *  - Column Metadata
     *  - Table Primary Keys
     *  - Table Foreign Keys (If applicable)
     */
    @Transactional
    suspend fun scanDatabaseTableConfiguration(client: DatabaseClient) {
        try {
            val (scannedConfiguration, existingConfiguration) = coroutineScope {

                // Create an async call to scan database tables based on current connection
                val databaseConnectionConfigurationScan: Deferred<List<DatabaseTable>> = async {
                    client.getDatabaseProperties()
                }

                // Create an async call to fetch existing table configurations
                val storedDatabaseConnectionConfiguration: Deferred<List<TableMonitoringConfigurationEntity>> = async {
                    fetchExistingTableConfigurations(client)
                }

                // Await both calls simultaneously and return pair
                Pair(databaseConnectionConfigurationScan.await(), storedDatabaseConnectionConfiguration.await())
            }

            // Handle table configuration comparison to update existing tables, and create new tables
            handleTableConfigurationComparison(scannedConfiguration, existingConfiguration, client)

        } catch (e: Exception) {
            logger.error(e) {
                "${client.config.databaseType} Database => ${client.config.connectionName} => ${client.id} => Failed to retrieve database table configuration => Message: ${e.message}"
            }
        }
    }

    /**
     * Retrieves existing table configuration data from the database
     *
     * @param client Database client to retrieve table configurations for
     *
     * @return List of existing table configurations
     */
    private suspend fun fetchExistingTableConfigurations(client: DatabaseClient): List<TableMonitoringConfigurationEntity> {
        val tableConfiguration: List<TableMonitoringConfigurationEntity> =
            withContext(Dispatchers.IO) {
                // Fetch configurations from repository based on client id
                tableMonitoringRepository.findAllByDatabaseConnectionId(client.id)
            }
        if (tableConfiguration.isEmpty()) {
            logger.info {
                "${client.config.databaseType} Database => ${client.config.connectionName} => ${client.id} => No existing table configurations found"
            }
            return listOf()
        }

        return tableConfiguration
    }

    /**
     * Handles the comparison of scanned table configuration with existing table configuration, to determine
     * if new tables need to be stored in the database, or existing table configurations need to be updated
     * with new table metadata (ie. New Columns, new foreign keys, etc)
     *
     * @param scannedConfiguration List of scanned database tables
     * @param existingConfiguration List of existing table configurations
     * @param client Database client to retrieve table configurations for
     */
    @Transactional
    suspend fun handleTableConfigurationComparison(
        scannedConfiguration: List<DatabaseTable>,
        existingConfiguration: List<TableMonitoringConfigurationEntity>,
        client: DatabaseClient
    ) {
        // Compare scanned configuration with existing configuration
        scannedConfiguration.forEach { scannedTable ->
            // For now we can perform table comparison based on table name
            // Todo: Implement a unique identifier for tables
            val existingTable = existingConfiguration.find { it.tableName == scannedTable.tableName }
            if (existingTable == null) {
                // Create new table configuration
                val createdEntity: TableConfiguration = createTableConfiguration(client, scannedTable)
                tableConfigurations[createdEntity.id] = createdEntity
                return@forEach
            }

            // Update existing table configuration, if any changes are detected
            val updatedEntity: TableConfiguration =
                updateTableConfiguration(scannedTable, existingTable) ?: return@forEach

            tableConfigurations[updatedEntity.id] = updatedEntity
        }

    }

    /**
     * Create a new table configuration based on the scanned table metadata and stores in the database
     *
     * @param client Database client to retrieve table configurations for
     * @param scannedTable Scanned database table metadata
     *
     * @return Created table configuration representing database record
     */
    @Transactional
    suspend fun createTableConfiguration(
        client: DatabaseClient,
        scannedTable: DatabaseTable
    ): TableConfiguration {
        // Create new table configuration
        val tableColumns: List<TableColumnConfiguration> = scannedTable.columns.map {
            TableColumnConfiguration.fromColumn(it)
        }

        val tableMetadata = TableMetadataConfiguration(
            primaryKey = scannedTable.primaryKey,
            foreignKeys = scannedTable.foreignKeys,
        )

        val tableEntity = TableMonitoringConfigurationEntity(
            databaseConnectionId = client.id,
            tableName = scannedTable.tableName,
            namespace = scannedTable.schema,
            isEnabled = true,
            includeAllColumns = true,
            columns = tableColumns.toList(),
            metadata = tableMetadata,
        )

        val createdEntity: TableMonitoringConfigurationEntity = withContext(Dispatchers.IO) {
            tableMonitoringRepository.save(tableEntity)
        }

        return TableConfiguration.fromEntity(createdEntity)
    }

    /**
     * Update existing table configuration based on the scanned table metadata
     *
     * @param existingTable Existing table configuration to update
     * @param scannedTable Scanned database table metadata
     *
     * @return Updated table configuration representing database record
     */
    @Transactional
    suspend fun updateTableConfiguration(
        scannedTable: DatabaseTable,
        existingTable: TableMonitoringConfigurationEntity
    ): TableConfiguration? {
        if (!compareConfigurationDiff(scannedTable, existingTable)) return null

        // Update existing table configuration
        val tableColumns: List<TableColumnConfiguration> = scannedTable.columns.map {
            TableColumnConfiguration.fromColumn(it)
        }

        val tableMetadata = TableMetadataConfiguration(
            primaryKey = scannedTable.primaryKey,
            foreignKeys = scannedTable.foreignKeys,
        )

        existingTable.apply {
            this.columns = tableColumns
            this.metadata = tableMetadata
        }

        val updatedEntity: TableMonitoringConfigurationEntity = withContext(Dispatchers.IO) {
            tableMonitoringRepository.save(existingTable)
        }

        return TableConfiguration.fromEntity(updatedEntity)
    }

    /**
     * Compare the differences between the scanned table configuration and the existing table configuration
     * to determine if an update is required to the stored record
     *
     * todo: Implement comparison logic
     */
    private suspend fun compareConfigurationDiff(
        scannedConfig: DatabaseTable,
        existingTable: TableMonitoringConfigurationEntity
    ): Boolean {
        return true
    }


}