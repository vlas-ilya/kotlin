// TARGET_BACKEND: JVM
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION: KT-61370
// WITH_STDLIB

// binary representation of fractional part of phi = (sqrt(5) - 1) / 2
private const val MAGIC: Int = 0x9E3779B9L.toInt() // ((sqrt(5.0) - 1) / 2 * pow(2.0, 32.0)).toLong().toString(16)
private const val MAX_SHIFT = 27
private const val THRESHOLD = ((1L shl 31) - 1).toInt() // 50% fill factor for speed
private val EMPTY_ARRAY = arrayOf<Any?>()


// For more details see for Knuth's multiplicative hash with golden ratio
// Shortly, we're trying to keep distribution of it uniform independently of input
// It's necessary because we use very simple linear probing
@Suppress("NOTHING_TO_INLINE")
private inline fun Any.computeHash(shift: Int) = ((hashCode() * MAGIC) ushr shift) shl 1


internal class OpenAddressLinearProbingHashTable<K : Any, V : Any> : AbstractMutableMap<K, V>() {
    // fields be initialized later in `clear()`

    // capacity = 1 << (32 - shift)
    private var shift = 0
    // keys are stored in even elements, values are in odd ones
    private var array = EMPTY_ARRAY
    private var size_ = 0

    init {
        clear()
    }

    override val size
        get() = size_

    override fun get(key: K): V? {
        var i = key.computeHash(shift)
        var k = array[i]

        while (true) {
            if (k === null) return null
            @Suppress("UNCHECKED_CAST")
            if (k == key) return array[i + 1] as V
            if (i == 0) {
                i = array.size
            }
            i -= 2
            k = array[i]
        }
    }

    /**
     * Never returns previous values
     */
    override fun put(key: K, value: V): V? {
        if (put(array, shift, key, value)) {
            if (++size_ >= (THRESHOLD ushr shift)) {
                rehash()
            }
        }

        return null
    }

    private fun rehash() {
        val newShift = maxOf(shift - 3, 0)
        val newArraySize = 1 shl (33 - newShift)
        val newArray = arrayOfNulls<Any>(newArraySize)

        var i = 0
        val arraySize = array.size
        while (i < arraySize) {
            val key = array[i]
            if (key != null) {
                put(newArray, newShift, key, array[i + 1])
            }
            i += 2
        }

        shift = newShift
        array = newArray
    }

    override fun clear() {
        shift = MAX_SHIFT
        array = arrayOfNulls(1 shl (33 - shift))

        size_ = 0
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {

            throw IllegalStateException("OpenAddressLinearProbingHashTable::entries is not supported and hardly will be")
        }

    private class Entry<K, V>(override val key: K, override val value: V) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V = throw UnsupportedOperationException("This Entry is not mutable.")
    }

    companion object {
        // Change to "true" to be able to see the contents of the map in debugger views
        private const val DEBUG = false
    }
}

private fun put(array: Array<Any?>, aShift: Int, key: Any, value: Any?): Boolean {
    var i = key.computeHash(aShift)

    while (true) {
        val k = array[i]
        if (k == null) {
            array[i] = key
            array[i + 1] = value
            return true
        }
        if (k == key) break
        if (i == 0) {
            i = array.size
        }
        i -= 2
    }

    array[i + 1] = value

    return false
}

fun box(): String {
    val map = OpenAddressLinearProbingHashTable<String, String>()
    map.put("O", "K")
    return "O" + map["O"]
}
