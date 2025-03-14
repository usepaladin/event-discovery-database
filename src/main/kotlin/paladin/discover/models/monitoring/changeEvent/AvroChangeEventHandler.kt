package paladin.discover.models.monitoring.changeEvent

import io.debezium.engine.ChangeEvent
import paladin.discover.pojo.monitoring.ChangeEventFormatHandler
import paladin.discover.util.monitor.ChangeEventDecoder
import java.util.*

class AvroChangeEventHandler(
    override val connectorProperties: Properties,
    override val recordHandlerCallback: (ChangeEvent<ByteArray, ByteArray>) -> Unit
) : ChangeEventFormatHandler<ByteArray>(), ChangeEventDecoder<GenericRecor>