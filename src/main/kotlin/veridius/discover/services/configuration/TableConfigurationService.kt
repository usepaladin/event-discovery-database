package veridius.discover.services.configuration

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.springframework.stereotype.Service
import veridius.discover.entities.configuration.TableMonitoringConfigurationEntity
import veridius.discover.models.configuration.DatabaseTable
import veridius.discover.models.configuration.TableConfiguration
import veridius.discover.pojo.client.DatabaseClient
import veridius.discover.pojo.configuration.TableColumnConfiguration
import veridius.discover.pojo.configuration.TableMetadataConfiguration
import veridius.discover.repositories.configuration.TableConfigurationRepository
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class TableConfigurationService(
    private val tableMonitoringRepository: TableConfigurationRepository,
) {
    private val logger = KotlinLogging.logger {}
    private val tableConfigurations: ConcurrentHashMap<UUID, TableConfiguration> = ConcurrentHashMap()

    /**
     * Scan database client to obtain all relevant metadata for a tables database table configuration, which includes:
     *  - Database Schemas (If applicable)
     *  - Database Tables
     *  - Table Columns
     *  - Column Metadata
     *  - Table Primary Keys
     *  - Table Foreign Keys (If applicable)
     */
    suspend fun scanDatabaseTableConfiguration(client: DatabaseClient) {
        try {
            val (scannedConfiguration, existingConfiguration) = coroutineScope {

                // Create an async call to scan database tables based on current connection
                val databaseConnectionConfigurationScan: Deferred<List<DatabaseTable>> = async {
                    client.getDatabaseProperties()
                }

                // Create an async call to fetch existing table configurations
                val storedDatabaseConnectionConfiguration: Deferred<List<TableConfiguration>> = async {
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
    private suspend fun handleTableConfigurationComparison(
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
            } else {
                // Update existing table configuration
                val updatedEntity: TableConfiguration = updateTableConfiguration(scannedTable, existingTable)
                tableConfigurations[updatedEntity.id] = updatedEntity
            }
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
    private suspend fun createTableConfiguration(
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
            columns = tableColumns,
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
    private suspend fun updateTableConfiguration(
        scannedTable: DatabaseTable,
        existingTable: TableMonitoringConfigurationEntity
    ): TableConfiguration {
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

}