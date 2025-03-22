package paladin.discover.pojo.monitoring

import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.github.oshai.kotlinlogging.KLogger
import paladin.discover.enums.monitoring.ChangeEventOperation
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.services.monitoring.MonitoringMetricsService
import paladin.discover.services.producer.ProducerService
import paladin.discover.util.monitor.ChangeEventDecoder

abstract class ChangeEventFormatHandler<T, V> : ChangeEventDecoder<T, V> {
    abstract val connector: DatabaseMonitoringConnector
    abstract val client: DatabaseClient

    // Pass through references to spring managed Dependencies through a Handler creation factory
    abstract val producerService: ProducerService
    abstract val monitoringMetricsService: MonitoringMetricsService
    abstract val logger: KLogger


    // Handle Debezium Engine format and observation handling
    abstract fun createEngine(): DebeziumEngine<ChangeEvent<T, T>>
    abstract fun handleObservation(event: ChangeEvent<T, T>)
    abstract fun handleRecordChangeEvent(operation: ChangeEventOperation, value: V)
    abstract fun handleMetadataEvent(value: V)

    fun isRecordChangeEvent(event: ChangeEventOperation): Boolean {
        return event == ChangeEventOperation.CREATE || event == ChangeEventOperation.UPDATE || event == ChangeEventOperation.DELETE
    }
}