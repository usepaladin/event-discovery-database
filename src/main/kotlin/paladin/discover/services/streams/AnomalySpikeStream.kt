//package paladin.discover.services.streams
//
//class Test {
//}


//val rateStream = enrichedEvents
//    .groupBy { _, _ -> "global" }
//    .windowedBy(TimeWindows.of(Duration.ofSeconds(10)).advanceBy(Duration.ofSeconds(1)))
//    .count()
//    .toStream()
//
//// Create a stream that calculates the moving average
//val movingAverageStream = rateStream
//    .mapValues { count -> RateRecord(count, System.currentTimeMillis()) }
//    .groupBy { key, _ -> key }
//    .aggregate(
//        { MovingAverage(mutableListOf(), 30) }, // Keep last 30 values
//        { _, record, ma ->
//            ma.values.add(record.count)
//            if (ma.values.size > ma.windowSize) {
//                ma.values.removeAt(0)
//            }
//            ma
//        }
//    )
//    .mapValues { ma ->
//        val avg = ma.values.average()
//        val stdDev = calculateStdDev(ma.values, avg)
//        MovingAverageStats(avg, stdDev)
//    }
//
//// Detect anomalies (3 sigma rule)
//rateStream.join(movingAverageStream) { currentRate, stats ->
//    val zScore = (currentRate - stats.avg) / stats.stdDev
//    val isAnomaly = abs(zScore) > 3.0
//    AnomalyRecord(currentRate, stats.avg, zScore, isAnomaly)
//}
//.filter { _, anomaly -> anomaly.isAnomaly }
//.to("anomaly-metrics")
//
//// Helper data classes
//data class RateRecord(val count: Long, val timestamp: Long)
//data class MovingAverage(val values: MutableList<Long>, val windowSize: Int)
//data class MovingAverageStats(val avg: Double, val stdDev: Double)
//data class AnomalyRecord(val currentRate: Long, val avgRate: Double, val zScore: Double, val isAnomaly: Boolean)
//
//// Standard deviation calculation
//fun calculateStdDev(values: List<Long>, mean: Double): Double {
//    return sqrt(values.map { (it - mean).pow(2) }.average())
//}