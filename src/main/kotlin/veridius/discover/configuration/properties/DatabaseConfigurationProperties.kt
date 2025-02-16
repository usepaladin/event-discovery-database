package veridius.discover.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(prefix = "discover")
data class DatabaseConfigurationProperties(
    @NestedConfigurationProperty val databases: List<DatabaseConnectionConfiguration>
)
{
    data class DatabaseConnectionConfiguration(
        val id: String,
        val type: DatabaseType,
        val host: String,
        val port: Number,
        val user: String?,
        val password: String?
    ){
        fun toConnectionURL(){
            when(type){
                DatabaseType.POSTGRES -> "jdbc:postgresql://$host:$port"
                DatabaseType.CASSANDRA -> "jdbc:cassandra://$host:$port"
                DatabaseType.MYSQL -> "jdbc:mysql://$host:$port"
                DatabaseType.MONGODB -> "mongodb://$host:$port"
            }
        }
    }

    enum class DatabaseType {
        POSTGRES,
        CASSANDRA,
        MYSQL,
        MONGODB
    }
}