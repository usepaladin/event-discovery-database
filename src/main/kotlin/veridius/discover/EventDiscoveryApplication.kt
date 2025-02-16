package veridius.discover

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EventDiscoveryApplication

fun main(args: Array<String>) {
	runApplication<EventDiscoveryApplication>(*args)
}
