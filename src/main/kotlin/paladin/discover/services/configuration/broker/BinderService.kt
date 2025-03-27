//package paladin.discover.services.configuration.broker
//
//import org.springframework.cloud.stream.binder.Binder
//import org.springframework.cloud.stream.binder.DefaultBinderFactory
//import org.springframework.context.ApplicationContext
//import org.springframework.context.ApplicationContextAware
//import org.springframework.stereotype.Service
//import java.util.concurrent.ConcurrentHashMap
//
///**
// * Service for managing, and creating new dynamic Binders for the application with
// * message broker configuration provided by the user throughout the life cycle of the application.
// */
//
//@Service
//class BinderService(
//    private val binderFactory: DefaultBinderFactory,
//) : ApplicationContextAware {
////    private lateinit var applicationContext: ApplicationContext
////    private val binders = ConcurrentHashMap<String, Binder<*, *, *>>()
////
////    /**
////     * Create a new Binder with the provided configuration.
////     *
////     * @param binderName Name of the binder to be created.
////     * @param binderConfiguration Configuration for the binder to be created.
////     */
////    fun createBinder(binderName: String, binderConfiguration: Map<String, Any>) {
////        (binderFactory as ConfigurableBinderFactory).createBinder(binderName, binderConfiguration)
////    }
////
////    override fun setApplicationContext(applicationContext: ApplicationContext) {
////        this.applicationContext = applicationContext
////    }
//}