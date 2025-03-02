package veridius.discover.configuration.properties

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jmx.export.MBeanExporter
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter

@Configuration
class JmxConfiguration {

    @Bean
    fun mBeanExporter(): MBeanExporter {
        return AnnotationMBeanExporter()
    }
}