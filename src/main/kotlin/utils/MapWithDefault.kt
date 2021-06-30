package utils

fun <K, V> Map<K, V>.withDefaultValue(default: V) = MapWithDefault(this) { default }
fun <K, V> Map<K, V>.withDefault(default: (K) -> V) = MapWithDefault(this, default)

class MapWithDefault<K, V>(map: Map<K, V>, val default: (K) -> V) : MutableMap<K, V>, AbstractMap<K, V>() {

    private val map = map.toMutableMap()

    override operator fun get(key: K): V = this.map[key] ?: default(key)

    operator fun plus(pair: Pair<K, V>): MapWithDefault<K, V> {
        val m = this.map.toMutableMap()
        m.apply { put(pair.first, pair.second) }
        return m.withDefault(default)
    }
    operator fun plus(map: Map<out K, V>): MapWithDefault<K, V> {
        val m = this.map.toMutableMap()
        m.apply { putAll(map) }
        return m.withDefault(default)
    }

    operator fun plusAssign(pair: Pair<K, V>) {
        this.map[pair.first] = pair.second
    }

    override val size: Int
        get() = this.map.size

    override fun containsKey(key: K): Boolean = this.map.containsKey(key)

    override fun containsValue(value: V): Boolean = this.map.containsValue(value)

    override fun isEmpty(): Boolean = this.map.isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get()  = this.map.entries

    override val keys: MutableSet<K>
        get()  = this.map.keys

    override val values: MutableCollection<V>
        get()  = this.map.values

    override fun clear() = this.map.clear()

    override fun put(key: K, value: V): V? = this.map.put(key, value)

    override fun putAll(from: Map<out K, V>) = this.map.putAll(from)

    override fun remove(key: K): V? = this.map.remove(key)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Map<*, *>) return false
        if (size != other.size) return false
        if (!keys.containsAll(other.keys)) return false
        for (entry in entries)
            if (other[entry.key] != entry.value)
                return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + default.hashCode()
        result = 31 * result + map.hashCode()
        return result
    }
}