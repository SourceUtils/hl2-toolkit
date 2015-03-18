package com.timepath.hl2.io.demo

import com.timepath.io.BitBuffer

import java.util.ArrayList
import java.util.HashMap
import java.util.logging.Logger

/**
 * @author TimePath
 */
class StringTable {
    private var tableName: String? = null
    private var id: Int = 0
    private var maxEntries: Int = 0
    private val numEntries: Int = 0
    private var userDataSize: Int = 0
    private var userDataSizeBits: Int = 0
    private var userDataFixedSize: Boolean = false
    private var entryBits: Int = 0

    /**
     * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/networkstringtable.cpp#L595
     */
    fun parse(bb: BitBuffer, l: MutableList<Pair<Any, Any>>) {
        var lastEntry = (-1).toLong()
        val history = ArrayList<String>(32) // Fixed size window
        for (i in numEntries.indices) {
            var entryIndex = lastEntry + 1
            if (!bb.getBoolean()) {
                entryIndex = bb.getBits(entryBits)
            }
            lastEntry = entryIndex
            if (entryIndex < 0 || entryIndex >= maxEntries) {
                LOG.warning(java.lang.String.format("Server sent bogus string index %d for table %s", entryIndex, tableName))
            }
            var entry = ""
            if (bb.getBoolean()) {
                if (bb.getBoolean()) {
                    // Substring check
                    val index = bb.getBits(5).toInt()
                    val bytestocopy = bb.getBits(SUBSTRING_BITS).toInt()
                    entry = history[index].substring(0, bytestocopy + 1)
                    val substr = bb.getString(bytestocopy)
                    entry += substr
                } else {
                    entry = bb.getString()
                }
                l.add("entry" to entry)
            }
            // Read in the user data.
            if (bb.getBoolean()) {
                val tempbuf = ByteArray(MAX_USERDATA_SIZE)
                if (userDataFixedSize) {
                    assert(userDataSize > 0)
                    // TODO: store in tempbuf
                    l.add("Userdata" to bb.getBits(userDataSizeBits))
                } else {
                    val nBytes = bb.getBits(MAX_USERDATA_BITS).toInt()
                    assert(nBytes <= MAX_USERDATA_SIZE) { (java.lang.String.format("message too large (%d bytes).", nBytes)) }
                    bb.get(tempbuf, 0, nBytes)
                }
            }
            if (entryIndex < numEntries) {
                // Updating
            } else {
                // Adding
            }
            if (history.size() > 31) {
                history.remove(0)
            }
            history.add(entry)
        }
    }

    class object {

        public val SUBSTRING_BITS: Int = 5
        public val MAX_USERDATA_BITS: Int = 14
        public val MAX_USERDATA_SIZE: Int = 1 shl MAX_USERDATA_BITS
        public val MAX_TABLES: Int = 32
        public val tables: Map<Int, StringTable> = HashMap()
        private val LOG = Logger.getLogger(javaClass<StringTable>().getName())

        fun get(id: Int) = tables[id]!!

        fun create(tableName: String, maxEntries: Int, entryBits: Int, userDataFixedSize: Boolean, userDataSize: Int, userDataSizeBits: Int): StringTable {
            val st = StringTable()
            st.tableName = tableName
            st.id = tables.size()
            st.maxEntries = maxEntries
            st.entryBits = entryBits
            st.userDataFixedSize = userDataFixedSize
            st.userDataSize = userDataSize
            st.userDataSizeBits = userDataSizeBits
            return st
        }
    }
}
