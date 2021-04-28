package utils

fun <K, V> Map<K, V>.withDefaultValue(default: V) = MapWithDefaultValue(this, default)
fun <K, V> Map<K, V>.withDefault(default: (K) -> V) = MapWithDefault(this, default)

open class MapWithDefaultValue<K, V>(map: Map<K, V>, private val default: V) : MutableMap<K, V> {

    private val map = map.toMutableMap()

    override operator fun get(key: K) = this.map[key] ?: default

    override val size: Int
        get() = this.map.size

    override fun containsKey(key: K): Boolean = this.map.containsKey(key)

    override fun containsValue(value: V): Boolean = this.map.containsValue(value)

    override fun isEmpty(): Boolean = this.map.isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> = this.map.entries

    override val keys: MutableSet<K> = this.map.keys

    override val values: MutableCollection<V> = this.map.values

    override fun clear() = this.map.clear()

    override fun put(key: K, value: V): V? = this.map.put(key, value)

    override fun putAll(from: Map<out K, V>) = this.map.putAll(from)

    override fun remove(key: K): V? = this.map.remove(key)

}

class MapWithDefault<K, V>(map: Map<K, V>, private val default: (K) -> V) : MutableMap<K, V> {

    private val map = map.toMutableMap()

    override operator fun get(key: K) = this.map[key] ?: default(key)

    override val size: Int
        get() = this.map.size

    override fun containsKey(key: K): Boolean = this.map.containsKey(key)

    override fun containsValue(value: V): Boolean = this.map.containsValue(value)

    override fun isEmpty(): Boolean = this.map.isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> = this.map.entries

    override val keys: MutableSet<K> = this.map.keys

    override val values: MutableCollection<V> = this.map.values

    override fun clear() = this.map.clear()

    override fun put(key: K, value: V): V? = this.map.put(key, value)

    override fun putAll(from: Map<out K, V>) = this.map.putAll(from)

    override fun remove(key: K): V? = this.map.remove(key)

}