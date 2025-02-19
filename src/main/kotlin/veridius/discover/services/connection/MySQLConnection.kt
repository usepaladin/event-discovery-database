package veridius.discover.services.connection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.boot.jdbc.DataSourceBuilder
import veridius.discover.configuration.properties.DatabaseConfigurationProperties.DatabaseConnectionConfiguration
import javax.sql.DataSource

data class MySQLConnection(
    override val id: String, override val config: DatabaseConnectionConfiguration
) : DatabaseConnection() {
    private var datasource: DataSource? = null
    private val logger = KotlinLogging.logger {}

    override fun connect(): DataSource {
        try{
            _connectionState.value = ConnectionState.Connecting
            if(datasource == null){
                datasource = DataSourceBuilder.create()
                    .driverClassName("com.mysql.cj.jdbc.Driver")
                    .url(config.toConnectionURL())
                    .username(config.user)
                    .password(config.password)
                    .build()
            }
            _connectionState.value = ConnectionState.Connected
            return datasource!!
        } catch (e: Exception){
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) { "Error connecting to Postgres \n" +
                    "Stack trace: ${e.message}" }
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