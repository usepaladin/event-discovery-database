package veridius.discover.services.configuration

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mu.KLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import veridius.discover.entities.configuration.TableMonitoringConfigurationEntity
import veridius.discover.models.configuration.Column
import veridius.discover.models.configuration.DatabaseTable
import veridius.discover.models.configuration.ForeignKey
import veridius.discover.models.configuration.PrimaryKey
import veridius.discover.pojo.client.DatabaseClient
import veridius.discover.repositories.configuration.TableConfigurationRepository
import veridius.discover.utils.TestDatabaseConfigurations
import java.util.*

@ExperimentalCoroutinesApi
class TableConfigurationServiceTest {
    private lateinit var tableConfigService: TableConfigurationService
    private lateinit var mockTableConfigRepository: TableConfigurationRepository
    private lateinit var mockClient: DatabaseClient
    private lateinit var logger: KLogger

    @BeforeEach
    fun setup() {
        mockTableConfigRepository = mockk<TableConfigurationRepository>()
        mockClient = mockk<DatabaseClient>()
        logger = mockk<KLogger>(relaxed = true)
        tableConfigService = TableConfigurationService(mockTableConfigRepository, logger)
    }

    @Test
    fun `test scan database table configuration`() = runTest {
        val config = TestDatabaseConfigurations.createPostgresConfig()
        val mockTables = listOf(
            DatabaseTable(
                tableName = "users",
                schema = "public",
                columns = listOf(
                    Column("id", "INTEGER", false, true, null),
                    Column("name", "VARCHAR", true, false, null)
                ),
                primaryKey = PrimaryKey("pk_users", listOf("id")),
                foreignKeys = listOf(
                    ForeignKey(
                        name = "fk_role",
                        column = "role_id",
                        foreignTable = "roles",
                        foreignColumn = "id"
                    )
                )
            )
        )

        every { mockClient.id } returns config.id
        every { mockClient.config } returns config
        every { mockClient.getDatabaseProperties() } returns mockTables
        every { mockTableConfigRepository.findAllByDatabaseConnectionId(any()) } returns emptyList()
        every { mockTableConfigRepository.save(any()) } returnsArgument 0

        tableConfigService.scanDatabaseTableConfiguration(mockClient)

        every { mockTableConfigRepository.save(any()) }
    }

    @Test
    fun `test update existing table configuration`() = runTest {
        val config = TestDatabaseConfigurations.createPostgresConfig()
        val existingTable = TableMonitoringConfigurationEntity(
            id = UUID.randomUUID(),
            databaseConnectionId = config.id,
            tableName = "users",
            namespace = "public",
            isEnabled = true,
            includeAllColumns = true
        )

        val scannedTable = DatabaseTable(
            tableName = "users",
            schema = "public",
            columns = listOf(
                Column("id", "INTEGER", false, true, null),
                Column("name", "VARCHAR", true, false, null),
                Column("email", "VARCHAR", true, false, null) // New column
            )
        )

        every { mockClient.id } returns config.id
        every { mockClient.config } returns config
        every { mockTableConfigRepository.findAllByDatabaseConnectionId(any()) } returns listOf(existingTable)
        every { mockTableConfigRepository.save(any()) } returnsArgument 0

        tableConfigService.handleTableConfigurationComparison(
            listOf(scannedTable),
            listOf(existingTable),
            mockClient
        )

        verify { mockTableConfigRepository.save(any()) }
    }

    @Test
    fun `test create new table configuration`() = runTest {
        val config = TestDatabaseConfigurations.createPostgresConfig()
        val newTable = DatabaseTable(
            tableName = "new_table",
            schema = "public",
            columns = listOf(
                Column("id", "INTEGER", false, true, null),
                Column("name", "VARCHAR", true, false, null)
            )
        )


        every { mockClient.id } returns config.id
        every { mockClient.config } returns config
        coEvery { mockTableConfigRepository.save(any()) } answers {
            // The repository is responsible for the generation of an Entity UUID, so we need to mock this
            val savedEntity = firstArg<TableMonitoringConfigurationEntity>().copy(
                id = UUID.randomUUID()
            )
            savedEntity
        }

        val result = tableConfigService.createTableConfiguration(mockClient, newTable)

        assertNotNull(result)
        assertEquals(newTable.tableName, result.tableName)
        assertEquals(newTable.schema, result.namespace)
        coVerify { mockTableConfigRepository.save(any()) }
    }

    /**todo: Further Implement Change recognition during Database scans
    // Currently, it still just overwrites current configuration, but would shit the bed when
    handling an event where the column name changes lol**/
//    @Test
//    fun `test handle table configuration comparison with no changes`() = runTest {
//        val config = TestDatabaseConfigurations.createPostgresConfig()
//        val existingTable = TableMonitoringConfigurationEntity(
//            databaseConnectionId = config.id,
//            tableName = "users",
//            namespace = "public",
//            isEnabled = true,
//            includeAllColumns = true
//        )
//
//        val scannedTable = DatabaseTable(
//            tableName = "users",
//            schema = "public",
//            columns = listOf(
//                Column("id", "INTEGER", false, true, null),
//                Column("name", "VARCHAR", true, false, null)
//            )
//        )
//
//        coEvery { mockClient.id } returns config.id
//        coEvery { mockClient.config } returns config
//
//        tableConfigService.handleTableConfigurationComparison(
//            listOf(scannedTable),
//            listOf(existingTable),
//            mockClient
//        )
//
//        // Verify no save operations were performed since there were no changes
//        coVerify(exactly = 0) { mockTableConfigRepository.save(any()) }
//    }
} 