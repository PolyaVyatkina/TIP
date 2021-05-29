package utils

fun <K, V> Map<K, V>.withDefaultValue(default: V) = MapWithDefault(this) { default }
fun <K, V> Map<K, V>.withDefault(default: (K) -> V) = MapWithDefault(this, default)

operator fun <K,V> MapWithDefault<K, V>.plus(pair: Pair<K, V>): MapWithDefault<K, V> =
    this.apply { put(pair.first, pair.second) }

class MapWithDefault<K, V>(map: Map<K, V>, val default: (K) -> V) : MutableMap<K, V>, AbstractMap<K, V>() {

    private val map = map.toMutableMap()

    override operator fun get(key: K): V = this.map[key] ?: default(key)

    operator fun plus(pair: Pair<K, V>): MapWithDefault<K, V> =
        this.apply { put(pair.first, pair.second) }

    operator fun plus(pairs: Iterable<Pair<K, V>>): MapWithDefault<K, V> =
        this.apply { putAll(pairs) }

    operator fun plus(map: Map<out K, V>): MapWithDefault<K, V> =
        this.apply { putAll(map) }

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

}