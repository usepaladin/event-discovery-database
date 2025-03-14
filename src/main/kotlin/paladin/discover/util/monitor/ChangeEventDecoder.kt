package paladin.discover.util.monitor

import paladin.discover.pojo.monitoring.ChangeEventData

interface ChangeEventDecoder<T> {
    fun decodeKey(rawKey: String): T
    fun decodeValue(rawValue: String): ChangeEventData
}