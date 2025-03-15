package paladin.discover.models.monitoring.changeEvent

import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.format.Avro
import io.github.oshai.kotlinlogging.KLogger
import org.apache.avro.generic.GenericRecord
import paladin.discover.enums.monitoring.ChangeEventOperation
import paladin.discover.pojo.monitoring.ChangeEventData
import paladin.discover.pojo.monitoring.ChangeEventFormatHandler
import paladin.discover.services.producer.ProducerService
import java.util.*

class AvroChangeEventHandler(
    override val connectorProperties: Properties,
    override val clientId: UUID,
    override val producerService: ProducerService,
    override val logger: KLogger
) : ChangeEventFormatHandler<ByteArray, GenericRecord>() {
    override fun createEngine(): DebeziumEngine<ChangeEvent<ByteArray, ByteArray>> {
        return DebeziumEngine.create(Avro::class.java)
            .using(connectorProperties)
            .notifying { event -> handleObservation(event) }
            .build()
    }

    override fun decodeKey(rawKey: ByteArray): GenericRecord {
        TODO("Not yet implemented")
    }

    override fun decodeValue(rawValue: ByteArray): ChangeEventData {
        TODO("Not yet implemented")
    }

    override fun decodeValue(rawValue: GenericRecord, operationType: ChangeEventOperation): ChangeEventData {
        TODO("Not yet implemented")
    }

    override fun handleObservation(event: ChangeEvent<ByteArray, ByteArray>) {
        TODO("Not yet implemented")

    }
}