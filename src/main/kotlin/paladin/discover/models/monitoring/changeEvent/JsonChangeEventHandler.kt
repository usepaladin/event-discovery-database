package paladin.discover.models.monitoring.changeEvent

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.format.Json
import io.github.oshai.kotlinlogging.KLogger
import paladin.discover.enums.monitoring.ChangeEventOperation
import paladin.discover.pojo.monitoring.ChangeEventData
import paladin.discover.pojo.monitoring.ChangeEventFormatHandler
import paladin.discover.services.producer.ProducerService
import java.util.*

class JsonChangeEventHandler(
    override val connectorProperties: Properties,
    override val clientId: UUID,
    override val producerService: ProducerService,
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

    /**
     * Determine the type of event being observed
     * This can contain relevant information that needs to be logged/observed externally, such as:
     *    - Heartbeat events
     * */
    private fun parseEventType(key: JsonNode) {}

    private fun parseOperationType(value: JsonNode): ChangeEventOperation {
        val operation: String? = value.get("payload")?.get("op")?.asText()

        return when (operation) {
            "c" -> ChangeEventOperation.CREATE
            "u" -> ChangeEventOperation.UPDATE
            "d" -> ChangeEventOperation.DELETE
            "r" -> ChangeEventOperation.SNAPSHOT
            else -> ChangeEventOperation.UNKNOWN
        }
    }

    override fun handleObservation(event: ChangeEvent<String, String>): Unit {
        logger.info { "Monitoring Service => JSON Event Handler => Database Id: $clientId => Record Observed" }
        // Filter out unwanted events  (ie. Unknown/Snapshot events)
        val valueNode: JsonNode = generateJsonNode(event.value())
        val operation: ChangeEventOperation = parseOperationType(valueNode)
        if (operation == ChangeEventOperation.SNAPSHOT || operation == ChangeEventOperation.UNKNOWN) {
            logger.info { "Monitoring Service => JSON Event Handler => Database Id: $clientId => Ignoring Snapshot/Unknown Event" }
            return
        }

        val changeEvent: ChangeEventData = decodeValue(valueNode, operation)
        println(changeEvent)
    }
}