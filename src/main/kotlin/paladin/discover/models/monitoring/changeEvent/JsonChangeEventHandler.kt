package paladin.discover.models.monitoring.changeEvent

import com.fasterxml.jackson.databind.JsonNode
import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.format.Json
import io.github.oshai.kotlinlogging.KLogger
import paladin.discover.pojo.monitoring.ChangeEventData
import paladin.discover.pojo.monitoring.ChangeEventFormatHandler
import paladin.discover.services.producer.ProducerService
import java.util.*

class JsonChangeEventHandler(
    override val connectorProperties: Properties,
    override val producerService: ProducerService,
    override val logger: KLogger
) : ChangeEventFormatHandler<String, JsonNode>() {

    override fun createEngine(): DebeziumEngine<ChangeEvent<String, String>> {
        return DebeziumEngine.create(Json::class.java)
            .using(connectorProperties)
            .notifying { event -> handleObservation(event) }
            .build()
    }

    //todo: Work out what data to return, how to make it customisable
    override fun deserializeEvent(event: ChangeEvent<String, String>): Map<String, Any> {
        TODO()
    }

    override fun decodeKey(rawKey: String): JsonNode {
        TODO("Not yet implemented")
    }

    override fun decodeValue(rawValue: String): ChangeEventData {
        TODO("Not yet implemented")
    }

    override fun handleObservation(event: ChangeEvent<String, String>) {
        TODO("Not yet implemented")
    }
}