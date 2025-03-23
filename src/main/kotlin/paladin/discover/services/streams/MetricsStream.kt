////package paladin.discover.services.streams
////
////class MetricsStream {
////}
//
//fun buildStreamsTopology(): Topology {
//    val builder = StreamsBuilder()
//
//    // Read raw events from Debezium
//    val rawEvents = builder.stream<String, ChangeEvent<String, String>>("debezium-events")
//
//    // Add dispatch timestamp
//    val enrichedEvents = rawEvents.mapValues { event ->
//        val dispatchTime = System.currentTimeMillis()
//        event.with("dispatch_time", dispatchTime)
//    }
//
//    // Calculate response time (dispatch - detection)
//    val responseTimeEvents = enrichedEvents.mapValues { event ->
//        val detectionTime = event.get("detection_time") as Long
//        val dispatchTime = event.get("dispatch_time") as Long
//        val responseTime = dispatchTime - detectionTime
//        event.with("response_time", responseTime)
//    }
//
//    // Calculate metrics (using tumbling windows)
//    // 1. Response Time Metrics
//    val responseTimeWindowed = responseTimeEvents
//        .groupBy { _, _ -> "global" }
//        .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
//
//    // Average response time
//    responseTimeWindowed
//        .aggregate(
//            { ResponseTimeAggregator(0L, 0L) },
//            { _, event, aggregator ->
//                val responseTime = event.get("response_time") as Long
//                aggregator.sum += responseTime
//                aggregator.count++
//                aggregator
//            }
//        )
//        .mapValues { aggregator -> aggregator.sum.toDouble() / aggregator.count }
//        .toStream()
//        .to("response-time-metrics")
//
//    // P95 and P99 Response Times (using percentile approximation)
//    // This would typically use a custom aggregator that maintains a sorted list or sketch
//
//    // 2. Event Count Metrics
//    // Count events per table and operation
//    enrichedEvents
//        .groupBy { _, event ->
//            val table = event.source.get("table") as String
//            val op = event.op
//            "$table:$op"
//        }
//        .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
//        .count()
//        .toStream()
//        .to("event-count-metrics")
//
//    // 3. Payload Size Metrics
//    enrichedEvents
//        .mapValues { event ->
//            val payloadSize = event.toString().length
//            PayloadSizeEvent(event.source.get("table") as String, payloadSize)
//        }
//        .groupBy { _, payload -> payload.table }
//        .windowedBy(TimeWindows.of(Duration.ofMinutes(5)))
//        .aggregate(
//            { PayloadSizeAggregator(0L, 0L) },
//            { _, payload, aggregator ->
//                aggregator.sum += payload.size
//                aggregator.count++
//                aggregator
//            }
//        )
//        .mapValues { aggregator -> aggregator.sum.toDouble() / aggregator.count }
//        .toStream()
//        .to("payload-size-metrics")
//
//    // 4. Error Detection
//    enrichedEvents
//        .filter { _, event -> event.get("error") != null }
//        .to("error-metrics")
//
//    // 5. Rate Calculation (for peak events and unusual spikes)
//    enrichedEvents
//        .groupBy { _, _ -> "global" }
//        .windowedBy(TimeWindows.of(Duration.ofSeconds(1)).advanceBy(Duration.ofSeconds(1)))
//        .count()
//        .toStream()
//        .to("rate-metrics")
//
//    return builder.build()
//}
//
//// Data classes for aggregations
//data class ResponseTimeAggregator(var sum: Long, var count: Long)
//data class PayloadSizeEvent(val table: String, val size: Int)
//data class PayloadSizeAggregator(var sum: Long, var count: Long)
//
//// Start Kafka Streams
//val props = Properties().apply {
//    put(StreamsConfig.APPLICATION_ID_CONFIG, "debezium-metrics-app")
//    put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
//    // Add other configs
//}
//
//val topology = buildStreamsTopology()
//val streams = KafkaStreams(topology, props)
//streams.start()