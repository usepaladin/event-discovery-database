package veridius.discover.services.connection.internal

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import veridius.discover.configuration.properties.CoreConfigurationProperties.DatabaseConnectionConfiguration
import javax.sql.DataSource

data class PostgresConnection(
    override val id: String, override val config: DatabaseConnectionConfiguration
) : DatabaseConnection() {
    private var datasource: DataSource? = null

    private val logger = KotlinLogging.logger {}

    private val hikariConfig = HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = config.toConnectionURL()
        username = config.user
        password = config.password
        minimumIdle = 2
        maximumPoolSize = 10
        connectionTimeout = 3000
    }

    override fun connect(): DataSource {
        if(_connectionState.value == ConnectionState.Connected && datasource != null){
            return datasource!!
        }

        try{
            _connectionState.value = ConnectionState.Connecting
            datasource = HikariDataSource(hikariConfig)
            _connectionState.value = ConnectionState.Connected
            return datasource!!
        } catch (e: Exception){
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) { "Error connecting to Postgres \n" +
                    "Stack trace: ${e.localizedMessage}" }
            throw e
        }
    }

    override fun disconnect() {
        try{
            datasource?.connection?.close()
            datasource = null
            _connectionState.value = ConnectionState.Disconnected
        } catch (e: Exception){
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) { "Error disconnecting from Postgres \n" +
                    "Stack trace: ${e.message}" }
        }
    }

    override fun isConnected(): Boolean {
        try{
            return datasource?.connection?.isValid(1000) ?: false
        } catch (e: Exception){
            logger.error(e) { "Error checking connection to Postgres \n" +
                    "Stack trace: ${e.message}" }
            return false
        }
    }

    override fun validateConfig() {
        TODO("Not yet implemented")
    }

    override fun configure() {
        TODO("Not yet implemented")
    }
}