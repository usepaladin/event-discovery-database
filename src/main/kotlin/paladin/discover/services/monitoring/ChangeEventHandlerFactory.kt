package paladin.discover.services.monitoring

import io.github.oshai.kotlinlogging.KLogger
import org.springframework.stereotype.Service
import paladin.discover.enums.monitoring.ChangeEventHandlerType
import paladin.discover.models.monitoring.changeEvent.AvroChangeEventHandler
import paladin.discover.models.monitoring.changeEvent.JsonChangeEventHandler
import paladin.discover.models.monitoring.changeEvent.ProtobufChangeEventHandler
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.pojo.monitoring.ChangeEventFormatHandler
import paladin.discover.pojo.monitoring.DatabaseMonitoringConnector
import paladin.discover.services.producer.ProducerService
import java.util.*

@Service
class ChangeEventHandlerFactory(
    private val producerService: ProducerService,
    private val monitoringMetricsService: MonitoringMetricsService,
    private val logger: KLogger
) {
    fun createChangeEventHandler(connector: DatabaseMonitoringConnector): ChangeEventFormatHandler<*, *> {
        val config: Properties = connector.getConnectorProps()
        val type: ChangeEventHandlerType = connector.getDatabaseChangeEventHandler()
        val client: DatabaseClient = connector.client
        return when (type) {
            ChangeEventHandlerType.JSON -> JsonChangeEventHandler(
                config,
                client,
                producerService,
                monitoringMetricsService,
                logger
            )

            ChangeEventHandlerType.AVRO -> AvroChangeEventHandler(
                config,
                client,
                producerService,
                monitoringMetricsService,
                logger
            )

            ChangeEventHandlerType.PROTOBUF -> ProtobufChangeEventHandler(
                config,
                client,
                producerService,
                monitoringMetricsService,
                logger
            )
        }
    }
}