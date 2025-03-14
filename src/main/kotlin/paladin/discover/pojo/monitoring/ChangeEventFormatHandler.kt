package paladin.discover.pojo.monitoring

import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import java.util.*

abstract class ChangeEventFormatHandler<T> {
    abstract val connectorProperties: Properties
    abstract val recordHandlerCallback: (ChangeEvent<T, T>) -> Unit
    abstract fun createEngine(): DebeziumEngine<ChangeEvent<T, T>>
    abstract fun deserializeEvent(event: ChangeEvent<T, T>): Map<String, Any>
}