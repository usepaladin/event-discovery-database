package paladin.discover.models.monitoring.changeEvent

import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.format.Protobuf
import io.github.oshai.kotlinlogging.KLogger
import paladin.discover.pojo.monitoring.ChangeEventData
import paladin.discover.pojo.monitoring.ChangeEventFormatHandler
import paladin.discover.services.producer.ProducerService
import java.util.*

//todo: Implement Protobuf Support for Change Events + Database Monitoring
class ProtobufChangeEventHandler(
    override val connectorProperties: Properties,
    override val producerService: ProducerService,
    override val logger: KLogger
) : ChangeEventFormatHandler<ByteArray, ByteArray>() {
    override fun createEngine(): DebeziumEngine<ChangeEvent<ByteArray, ByteArray>> {
        return DebeziumEngine.create(Protobuf::class.java)
            .using(connectorProperties)
            .notifying { event -> handleObservation(event) }
            .build()
    }

    override fun deserializeEvent(event: ChangeEvent<ByteArray, ByteArray>): Map<String, Any> {
        TODO("Not yet implemented")
    }

    override fun decodeKey(rawKey: String): ByteArray {
        TODO("Not yet implemented")
    }

    override fun decodeValue(rawValue: String): ChangeEventData {
        TODO("Not yet implemented")
    }

    override fun handleObservation(event: ChangeEvent<ByteArray, ByteArray>) {
        TODO("Not yet implemented")
    }
}