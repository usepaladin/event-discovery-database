//// Import a library like org.HdrHistogram.Histogram
//val histogramWindow = responseTimeEvents
//    .groupBy { _, _ -> "global" }
//    .windowedBy(TimeWindows.of(Duration.ofMinutes(1)))
//    .aggregate(
//        { Histogram(TimeUnit.MILLISECONDS.toNanos(1), TimeUnit.SECONDS.toNanos(10), 3) },
//        { _, event, histogram ->
//            val responseTime = event.get("response_time") as Long
//            histogram.recordValue(responseTime)
//            histogram
//        }
//    )
//    .mapValues { histogram ->
//        mapOf(
//            "p95" to histogram.getValueAtPercentile(95.0),
//            "p99" to histogram.getValueAtPercentile(99.0)
//        )
//    }
//    .toStream()
//    .to("percentile-metrics")