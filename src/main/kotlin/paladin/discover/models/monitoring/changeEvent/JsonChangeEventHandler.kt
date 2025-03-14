package paladin.discover.models.monitoring.changeEvent

import com.fasterxml.jackson.databind.JsonNode
import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.format.Json
import paladin.discover.pojo.monitoring.ChangeEventData
import paladin.discover.pojo.monitoring.ChangeEventFormatHandler
import paladin.discover.util.monitor.ChangeEventDecoder
import java.util.*

class JsonChangeEventHandler(
    override val connectorProperties: Properties,
    override val recordHandlerCallback: (ChangeEvent<String, String>) -> Unit
) : ChangeEventFormatHandler<String>(), ChangeEventDecoder<JsonNode> {

    override fun createEngine(): DebeziumEngine<ChangeEvent<String, String>> {
        return DebeziumEngine.create(Json::class.java)
            .using(connectorProperties)
            .notifying(recordHandlerCallback)
            .build()
    }

    //todo: Work out what data to return, how to make it customisable
    override fun deserializeEvent(event: ChangeEvent<String, String>): Nothing {

    }

    override fun decodeKey(rawKey: String): JsonNode {
        TODO("Not yet implemented")
    }

    override fun decodeValue(rawValue: String): ChangeEventData {
        TODO("Not yet implemented")
    }
}