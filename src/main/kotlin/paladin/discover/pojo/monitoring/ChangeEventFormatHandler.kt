package paladin.discover.pojo.monitoring

import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.github.oshai.kotlinlogging.KLogger
import paladin.discover.services.producer.ProducerService
import paladin.discover.util.monitor.ChangeEventDecoder
import java.util.*

abstract class ChangeEventFormatHandler<T, V> : ChangeEventDecoder<V> {
    abstract val connectorProperties: Properties
    // Pass through references to spring managed Dependencies through a Handler creation factory
    abstract val producerService: ProducerService
    abstract val logger: KLogger
    abstract fun createEngine(): DebeziumEngine<ChangeEvent<T, T>>
    abstract fun deserializeEvent(event: ChangeEvent<T, T>): Map<String, Any>
    abstract fun handleObservation(event: ChangeEvent<T, T>)
}