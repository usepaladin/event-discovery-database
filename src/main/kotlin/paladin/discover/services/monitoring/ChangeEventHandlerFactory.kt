package paladin.discover.services.monitoring

import com.fasterxml.jackson.databind.JsonNode
import io.github.oshai.kotlinlogging.KLogger
import org.springframework.stereotype.Service
import paladin.discover.models.monitoring.changeEvent.ChangeEventHandler
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.pojo.monitoring.ChangeEventFormatHandler
import paladin.discover.pojo.monitoring.DatabaseMonitoringConnector
import paladin.discover.services.metrics.MonitoringMetricsService
import paladin.discover.services.producer.ProducerService

@Service
class ChangeEventHandlerFactory(
    private val producerService: ProducerService,
    private val monitoringMetricsService: MonitoringMetricsService,
    private val logger: KLogger
) {
    fun createChangeEventHandler(connector: DatabaseMonitoringConnector): ChangeEventFormatHandler<String, JsonNode> {
        val client: DatabaseClient = connector.client
        return ChangeEventHandler(
            connector,
            client,
            producerService,
            monitoringMetricsService,
            logger
        )
    }
}