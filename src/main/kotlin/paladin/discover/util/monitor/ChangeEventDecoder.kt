package paladin.discover.util.monitor

import paladin.discover.enums.monitoring.ChangeEventOperation
import paladin.discover.pojo.monitoring.ChangeEventData

interface ChangeEventDecoder<T, V> {
    fun decodeKey(rawKey: T): V
    fun decodeValue(rawValue: V, operationType: ChangeEventOperation): ChangeEventData
}