package veridius.discover.pojo.configuration

interface BaseColumn {
    val name: String?
    val type: String?
    val nullable: Boolean
    val autoIncrement: Boolean
    val defaultValue: String?
}