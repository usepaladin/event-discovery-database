package veridius.discover.pojo.settings

data class ErrorSettings(
    // The level of logging to use for errors
    var loggingLevel: ErrorLoggingLevel = ErrorLoggingLevel.ERROR,
    // How many times to retry a database operation before failing
    var databaseRetryCount: Int = 3,
    // How long to wait between retries
    var databaseRetryDelay: Int = 1000,
    // How long to wait for a database operation to complete before failing
    var databaseTimeout: Int = 10000,
) {

    enum class ErrorLoggingLevel {
        NONE,
        ERROR,
        VERBOSE,
    }

}