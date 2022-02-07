package me.bytebeats.tools.ts

/**
 * Created by bytebeats on 2022/2/7 : 19:33
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

/**
 * Hash list map
 * Data structure: HashMap<K, MutableList<V>?>
 * @param K key
 * @param V value
 * @constructor Create empty Hash list map
 */
class HashListMap<K, V> : MutableMap<K, V> {
    private val mInnerMap = mutableMapOf<K, MutableList<V>>()
    private var mSize = 0

    override val size: Int
        get() = mSize

    override fun containsKey(key: K): Boolean = mInnerMap.containsKey(key)

    override fun containsValue(value: V): Boolean =
        mInnerMap.any { entry -> entry.value.contains(value) }

    fun contains(key: K, value: V): Boolean = mInnerMap[key]?.contains(value) == true

    override fun get(key: K): V? {
        throw UnsupportedOperationException(
            UNSUPPORTED_FORMATTER.format(
                "get(key: K): V?",
                "getValue(key: K): List<V>?"
            )
        )
    }

    fun getValue(key: K): List<V?>? = mInnerMap[key]

    override fun isEmpty(): Boolean = mSize <= 0

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = throw UnsupportedOperationException(
            UNSUPPORTED_FORMATTER.format(
                "entries: MutableSet<MutableMap.MutableEntry<K, V>>",
                "listEntries: MutableSet<MutableMap.MutableEntry<K, List<V>>>"
            )
        )

    val listEntries: MutableSet<MutableMap.MutableEntry<K, MutableList<V>>>
        get() = mInnerMap.entries

    override val keys: MutableSet<K>
        get() = mInnerMap.keys

    override val values: MutableCollection<V>
        get() = throw UnsupportedOperationException(
            UNSUPPORTED_FORMATTER.format(
                "values: MutableCollection<V>",
                "listValues: MutableCollection<List<V>>"
            )
        )

    val listValues: MutableCollection<MutableList<V>>
        get() = mInnerMap.values

    override fun clear() {
        mInnerMap.clear()
        mSize = 0
    }

    override fun put(key: K, value: V): V? {
        throw UnsupportedOperationException(
            UNSUPPORTED_FORMATTER.format(
                "put(key: K, value: V): V?",
                "putValue: MutableCollection<List<V>>"
            )
        )
    }

    fun putValue(key: K, value: V): MutableList<V>? {
        if (mInnerMap.containsKey(key)) {
            mInnerMap[key]!!.add(value)
        } else {
            mInnerMap[key] = mutableListOf(value)
        }
        mSize += 1
        return mInnerMap[key]
    }

    override fun putAll(from: Map<out K, V>) {
        from.entries.forEach { entry -> putValue(entry.key, entry.value) }
    }

    override fun remove(key: K): V? {
        throw UnsupportedOperationException(
            UNSUPPORTED_FORMATTER.format(
                "remove(key: K): V?",
                "removeList(key: K): MutableList<V?>?"
            )
        )
    }

    fun removeList(key: K): MutableList<V>? {
        val list = mInnerMap.remove(key)
        list?.let {
            mSize -= it.size
        }
        return list
    }

    fun removeValue(value: V?): V? {
        var contains = false
        for (entry in mInnerMap.entries) {
            if (value != null && entry.value.remove(value)) {
                mSize -= 1
                contains = true
            }
        }
        return if (contains) value else null
    }

    fun remove(key: K, value: V): V? {
        var v: V? = null
        if (mInnerMap[key]?.remove(value) == true) {
            v = value
            mSize -= 1
        }
        return v
    }

    override fun toString(): String {
        return mInnerMap.toString()
    }

    companion object {
        private const val UNSUPPORTED_FORMATTER = "%s is unsupported, try %s instead"
    }
}