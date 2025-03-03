package veridius.discover.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jmx.export.MBeanExporter
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter

@Configuration
class JMXConfig {

    @Bean
    fun mBeanExporter(): MBeanExporter {
        return AnnotationMBeanExporter()
    }
}