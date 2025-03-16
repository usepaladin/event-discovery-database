package paladin.discover.services.monitoring

import io.github.oshai.kotlinlogging.KLogger
import org.springframework.stereotype.Service

/**
 * Service used to handle metrics generated during Debezium monitoring events, this may include:
 *  - Heartbeat monitoring to monitor the health of the Debezium engine
 *  - Snapshot monitoring to monitor the progress of the database snapshot
 *  - Operation metrics to determine the type of operations observed + associated details to formulate data and graphs
 *  - Performance metrics
 */
@Service
class MonitoringMetricsService(
    private val logger: KLogger
)