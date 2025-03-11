package veridius.discover.pojo.monitoring

import veridius.discover.util.monitor.ConnectorStorageConfiguration
import java.util.*

sealed class StorageBackend : ConnectorStorageConfiguration {
    data object Kafka : StorageBackend() {
        override fun validateConfig() {
            TODO("Not yet implemented")
        }

        override fun applyProperties(props: Properties) {
            TODO("Not yet implemented")
        }
    }

    data object File : StorageBackend() {
        override fun validateConfig() {
            TODO("Not yet implemented")
        }

        override fun applyProperties(props: Properties) {
            TODO("Not yet implemented")
        }
    }

    data object Database : StorageBackend() {
        override fun applyProperties(props: Properties) {
            TODO("Not yet implemented")
        }

        override fun validateConfig() {
            TODO("Not yet implemented")
        }
    }
}