package paladin.discover.enums.monitoring

/**
 * Enum class to represent the different types of change event handlers, and the format in which the
 * engine will handle data received from any Change Event, such as JSON, Avro, or Protobuf
 * This is to help ensure that data can then be processed and received by the Broker in the correct format
 */
enum class ChangeEventHandlerType {
    JSON,
    AVRO,
    PROTOBUF
}