package paladin.discover.pojo.configuration.database

interface BaseColumn {
    val name: String?
    val type: String?
    val nullable: Boolean
    val autoIncrement: Boolean
    val defaultValue: String?
}