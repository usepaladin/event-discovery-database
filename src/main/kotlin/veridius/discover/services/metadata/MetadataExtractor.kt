package veridius.discover.services.metadata

import veridius.discover.entities.TableMetadata

interface MetadataExtractor {
    fun extractMetadata(): List<TableMetadata>
    fun validateConfig(): Boolean
}