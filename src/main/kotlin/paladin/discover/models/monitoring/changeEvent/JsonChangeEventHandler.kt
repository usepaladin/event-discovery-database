package paladin.discover.models.monitoring.changeEvent

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.format.Json
import io.github.oshai.kotlinlogging.KLogger
import paladin.discover.enums.monitoring.ChangeEventOperation
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.pojo.monitoring.ChangeEventData
import paladin.discover.pojo.monitoring.ChangeEventDataKey
import paladin.discover.pojo.monitoring.ChangeEventFormatHandler
import paladin.discover.services.monitoring.MonitoringMetricsService
import paladin.discover.services.producer.ProducerService
import java.util.*

class JsonChangeEventHandler(
    override val connectorProperties: Properties,
    override val client: DatabaseClient,
    override val producerService: ProducerService,
    override val monitoringMetricsService: MonitoringMetricsService,
    override val logger: KLogger
) : ChangeEventFormatHandler<String, JsonNode>() {

    private val objectMapper = ObjectMapper()

    override fun createEngine(): DebeziumEngine<ChangeEvent<String, String>> {
        return DebeziumEngine.create(Json::class.java)
            .using(connectorProperties)
            .notifying { event -> handleObservation(event) }
            .build()
    }

    override fun decodeKey(rawKey: String): JsonNode {
        return generateJsonNode(rawKey)
    }

    override fun decodeValue(rawValue: String): ChangeEventData {
        TODO()
    }

    /**
     * */
    override fun decodeValue(rawValue: JsonNode, operationType: ChangeEventOperation): ChangeEventData {
        val payload = rawValue.get("payload") ?: throw IllegalArgumentException("Payload not found in JSON")
        // Safely parse 'before' field - it might be null (like in create events)
        val before: Map<String, Any?>? = payload.get("before")?.let {
            if (!it.isNull) objectMapper.convertValue(it, object : TypeReference<Map<String, Any?>>() {}) else null
        }

        // Safely parse 'after' field - it might be null (like in delete events)
        val after: Map<String, Any?>? = payload.get("after")?.let {
            if (!it.isNull) objectMapper.convertValue(it, object : TypeReference<Map<String, Any?>>() {}) else null
        }

        // Parse 'source' field - this should typically be present
        val source = payload.get("source")?.let {
            if (!it.isNull) objectMapper.convertValue(it, object : TypeReference<Map<String, Any?>>() {}) else null
        }

        // Create and return your ChangeEventData object with the parsed fields
        return ChangeEventData(
            operation = operationType,
            before = before,
            after = after,
            source = source,
            // Easy access fields from the payload
            timestamp = payload.get("ts_ms")?.asLong(),
            table = source?.get("table")?.toString(),
        )
    }

    private fun generateJsonNode(value: String): JsonNode {
        return objectMapper.readTree(value)
    }


    private fun filterMetadataEvents(event: JsonNode) {
        val eventName: String = event.get("schema")?.get("name")?.asText() ?: "Unknown"
        println(eventName)
    }

    private fun parseOperationType(value: JsonNode): ChangeEventOperation {
        val operation: String? = value.get("payload")?.get("op")?.asText()

        return when (operation) {
            "c" -> ChangeEventOperation.CREATE
            "u" -> ChangeEventOperation.UPDATE
            "d" -> ChangeEventOperation.DELETE
            "r" -> ChangeEventOperation.SNAPSHOT
            else -> ChangeEventOperation.OTHER
        }
    }

    override fun handleRecordChangeEvent(operation: ChangeEventOperation, value: JsonNode) {
        val changeEventData: ChangeEventData = decodeValue(value, operation)

        if (changeEventData.table.isNullOrEmpty()) {
            throw IllegalArgumentException("Table name not found in ChangeEventData")
        }

        val changeEventKey = ChangeEventDataKey(
            database = client.config.databaseType,
            namespace = client.config.database,
            table = changeEventData.table,
            operation = operation
        )

        // todo: Allow customisable operation topic names when sending to Client Producer
        // Current Strategy: <databaseType>.<schema>.<table>.<operation>
        val externalTopicName: String =
            "${changeEventKey.database}.${changeEventKey.namespace}.${changeEventKey.table}.${changeEventKey.operation}"

        producerService.sendMessage("database-monitoring-record-change-event-in-0", changeEventData)
    }

    /**
     * Parse the event to locate any generated events that contribute useful metadata. This would include:
     * Ensuring that we can route these to separate Brokers for Web client consumption
     *    - Heartbeat events
     *    - Transaction events (ie. BEGIN, COMMIT, ROLLBACK)
     *    - Manual Debezium Signal Events (https://debezium.io/documentation/reference/stable/configuration/signalling.html)
     * */
    override fun handleMetadataEvent(value: JsonNode) {
        filterMetadataEvents(value)
        producerService.sendMessage("database-monitoring-metadata-in-0", value)
    }

    override fun handleObservation(event: ChangeEvent<String, String>) {
        try {


            logger.info { "Monitoring Service => JSON Event Handler => Database Id: ${client.id} => Record Observed" }

            // Handle nullable events
            if (event.value() == null) {
                logger.info { "Monitoring Service => JSON Event Handler => Database Id: ${client.id} => Record Ignored/Not published => Event Value was null" }
                return
            }

            val valueNode: JsonNode = generateJsonNode(event.value())
            val operation: ChangeEventOperation = parseOperationType(valueNode)

            if (this.isRecordChangeEvent(operation)) {
                logger.info { "Monitoring Service => JSON Event Handler => Database Id: ${client.id} => Record Change Event Observed => Operation Type: $operation" }
                handleRecordChangeEvent(operation, valueNode)
                return
            }

            logger.info { "Monitoring Service => JSON Event Handler => Database Id: ${client.id} => Processing Engine Metadata" }
            handleMetadataEvent(valueNode)
        } catch (e: Exception) {
            logger.error(e) { "Monitoring Service => JSON Event Handler => Database Id: ${client.id} => Failed to process event" }
        }
    }
}