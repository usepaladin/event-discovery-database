package paladin.discover.services.configuration

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import paladin.discover.entities.configuration.TableMonitoringConfigurationEntity
import paladin.discover.models.configuration.database.Column
import paladin.discover.models.configuration.database.DatabaseTable
import paladin.discover.models.configuration.database.ForeignKey
import paladin.discover.models.configuration.database.PrimaryKey
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.repositories.configuration.TableConfigurationRepository
import paladin.discover.services.configuration.database.TableConfigurationService
import paladin.discover.utils.TestDatabaseConfigurations
import paladin.discover.utils.TestLogAppender
import java.util.*

@ExperimentalCoroutinesApi
@ExtendWith(MockKExtension::class)
class TableConfigurationServiceTest {
    private lateinit var tableConfigService: TableConfigurationService

    @MockK
    private lateinit var tableConfigRepository: TableConfigurationRepository

    @MockK
    private lateinit var client: DatabaseClient

    private lateinit var testAppender: TestLogAppender
    private var logger: KLogger = KotlinLogging.logger {}
    private lateinit var logbackLogger: Logger

    @BeforeEach
    fun setup() {
        // Configure logger
        logbackLogger = LoggerFactory.getLogger(logger.name) as Logger
        testAppender = TestLogAppender.factory(logbackLogger, Level.DEBUG)
        tableConfigService = TableConfigurationService(tableConfigRepository, logger)
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

        every { client.id } returns config.id
        every { client.config } returns config
        every { client.getDatabaseProperties() } returns mockTables
        every { tableConfigRepository.findAllByDatabaseConnectionId(any()) } returns emptyList()
        every { tableConfigRepository.save(any()) } returnsArgument 0

        tableConfigService.scanDatabaseTableConfiguration(client)

        every { tableConfigRepository.save(any()) }
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

        every { client.id } returns config.id
        every { client.config } returns config
        every { tableConfigRepository.findAllByDatabaseConnectionId(any()) } returns listOf(existingTable)
        every { tableConfigRepository.save(any()) } returnsArgument 0

        tableConfigService.handleTableConfigurationComparison(
            listOf(scannedTable),
            listOf(existingTable),
            client
        )

        verify { tableConfigRepository.save(any()) }
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


        every { client.id } returns config.id
        every { client.config } returns config
        coEvery { tableConfigRepository.save(any()) } answers {
            // The repository is responsible for the generation of an Entity UUID, so we need to mock this
            val savedEntity = firstArg<TableMonitoringConfigurationEntity>().copy(
                id = UUID.randomUUID()
            )
            savedEntity
        }

        val result = tableConfigService.createTableConfiguration(client, newTable)

        assertNotNull(result)
        assertEquals(newTable.tableName, result.tableName)
        assertEquals(newTable.schema, result.namespace)
        coVerify { tableConfigRepository.save(any()) }
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