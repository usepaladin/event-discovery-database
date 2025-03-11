package veridius.discover.util.monitor

import java.util.*

interface ConnectorStorageConfiguration {
    fun validateConfig()
    fun applyProperties(props: Properties)
}