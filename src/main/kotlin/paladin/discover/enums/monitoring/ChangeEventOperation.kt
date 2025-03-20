package paladin.discover.enums.monitoring

/**
 * Enum class to represent the different types of change event operations, such as CREATE, UPDATE, DELETE, SNAPSHOT
 */
enum class ChangeEventOperation {
    /**
     * Represents a CREATE operation, such as creating a new record in a database
     */
    CREATE,

    /**
     * Represents an UPDATE operation, such as updating an existing record in a database
     */
    UPDATE,

    /**
     * Represents a DELETE operation, such as deleting an existing record in a database
     */
    DELETE,

    /**
     * Represents a SNAPSHOT operation, such as taking a snapshot of the current state of a database
     */
    SNAPSHOT,

    /**
     * Represents an operation that is not one of the above types, usually a sign to disregard the event
     */
    OTHER
}