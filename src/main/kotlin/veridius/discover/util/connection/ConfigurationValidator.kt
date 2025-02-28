package veridius.discover.util.connection

import veridius.discover.models.connection.DatabaseConnectionConfiguration

interface ConfigurationValidator {

    fun validateConfig(configuration: DatabaseConnectionConfiguration) {

        if (configuration.additionalProperties?.public == false && (configuration.user.isNullOrEmpty() || configuration.password.isNullOrEmpty())) {
            throw IllegalArgumentException("Private connections must have associated authentication credentials")
        }


    }

}