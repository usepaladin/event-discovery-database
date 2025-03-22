package paladin.discover.configuration.properties

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import paladin.discover.pojo.monitoring.StorageBackend

@Component
class StorageBackendConvertor : Converter<String, StorageBackend> {
    override fun convert(source: String): StorageBackend {
        return when (source.lowercase()) {
            "file" -> StorageBackend.File
            "kafka" -> StorageBackend.Kafka
            else -> throw IllegalArgumentException("Invalid storage backend: $source")
        }
    }
}