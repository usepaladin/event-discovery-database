package veridius.discover.exceptions

class DatabaseConnectionNotFound(message: String) : RuntimeException(message)
class ConnectionJobNotFound(message: String) : RuntimeException(message)
class NoActiveConnectionFound(message: String) : RuntimeException(message)