package paladin.discover.services.monitoring

import io.github.oshai.kotlinlogging.KLogger
import org.springframework.stereotype.Service
import paladin.discover.enums.monitoring.ChangeEventHandlerType
import paladin.discover.models.monitoring.changeEvent.AvroChangeEventHandler
import paladin.discover.models.monitoring.changeEvent.JsonChangeEventHandler
import paladin.discover.models.monitoring.changeEvent.ProtobufChangeEventHandler
import paladin.discover.pojo.monitoring.ChangeEventFormatHandler
import paladin.discover.services.producer.ProducerService
import java.util.*

@Service
class ChangeEventHandlerFactory(
    private val producerService: ProducerService,
    private val logger: KLogger
) {
    fun createChangeEventHandler(type: ChangeEventHandlerType, config: Properties): ChangeEventFormatHandler<*, *> {
        return when (type) {
            ChangeEventHandlerType.AVRO -> AvroChangeEventHandler(config, producerService, logger)
            ChangeEventHandlerType.JSON -> JsonChangeEventHandler(config, producerService, logger)
            ChangeEventHandlerType.PROTOBUF -> ProtobufChangeEventHandler(config, producerService, logger)
        }
    }
}