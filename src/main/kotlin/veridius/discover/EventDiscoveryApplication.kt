package veridius.discover

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class EventDiscoveryApplication

fun main(args: Array<String>) {
	runApplication<EventDiscoveryApplication>(*args)
}
