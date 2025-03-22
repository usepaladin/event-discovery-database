package paladin.discover.models.monitoring.changeEvent

import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.format.Protobuf
import io.github.oshai.kotlinlogging.KLogger
import paladin.discover.enums.monitoring.ChangeEventOperation
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.pojo.monitoring.ChangeEventData
import paladin.discover.pojo.monitoring.ChangeEventFormatHandler
import paladin.discover.pojo.monitoring.DatabaseMonitoringConnector
import paladin.discover.services.monitoring.MonitoringMetricsService
import paladin.discover.services.producer.ProducerService

class ProtobufChangeEventHandler(
    override val connector: DatabaseMonitoringConnector,
    override val client: DatabaseClient,
    override val producerService: ProducerService,
    override val monitoringMetricsService: MonitoringMetricsService,
    override val logger: KLogger
) : ChangeEventFormatHandler<ByteArray, ByteArray>() {
    override fun createEngine(): DebeziumEngine<ChangeEvent<ByteArray, ByteArray>> {
        return DebeziumEngine.create(Protobuf::class.java)
            .using(connector.getConnectorProps())
            .notifying { event -> handleObservation(event) }
            .build()
    }

    override fun decodeValue(rawValue: ByteArray): ChangeEventData {
        TODO("Not yet implemented")
    }

    override fun decodeKey(rawKey: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun decodeValue(rawValue: ByteArray, operationType: ChangeEventOperation): ChangeEventData {
        TODO("Not yet implemented")
    }

    override fun handleObservation(event: ChangeEvent<ByteArray, ByteArray>) {
        TODO("Not yet implemented")
    }

    override fun handleMetadataEvent(value: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun handleRecordChangeEvent(operation: ChangeEventOperation, value: ByteArray) {
        TODO("Not yet implemented")
    }
}