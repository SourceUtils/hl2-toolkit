package com.timepath.hl2.io.demo;

import com.timepath.Pair;
import com.timepath.io.BitBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
class StringTable {

    public static final int SUBSTRING_BITS = 5;
    public static final int MAX_USERDATA_BITS = 14;
    public static final int MAX_USERDATA_SIZE = 1 << MAX_USERDATA_BITS;
    public static final int MAX_TABLES = 32;
    public static final Map<Integer, StringTable> tables = new HashMap<>();
    private static final Logger LOG = Logger.getLogger(StringTable.class.getName());
    private String tableName;
    private int id;
    private int maxEntries;
    private int numEntries;
    private int userDataSize;
    private int userDataSizeBits;
    private boolean userDataFixedSize;
    private int entryBits;

    static StringTable get(int id) {
        return tables.get(id);
    }

    @NotNull
    static StringTable create(String tableName,
                              int maxEntries,
                              int entryBits,
                              boolean userDataFixedSize,
                              int userDataSize,
                              int userDataSizeBits) {
        @NotNull StringTable st = new StringTable();
        st.tableName = tableName;
        st.id = tables.size();
        st.maxEntries = maxEntries;
        st.entryBits = entryBits;
        st.userDataFixedSize = userDataFixedSize;
        st.userDataSize = userDataSize;
        st.userDataSizeBits = userDataSizeBits;
        return st;
    }

    /**
     * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/networkstringtable.cpp#L595
     */
    void parse(@NotNull BitBuffer bb, @NotNull List<Pair<Object, Object>> l) {
        long lastEntry = -1;
        @NotNull List<String> history = new ArrayList<>(32); // Fixed size window
        for (int i = 0; i < numEntries; i++) {
            long entryIndex = lastEntry + 1;
            if (!bb.getBoolean()) {
                entryIndex = bb.getBits(entryBits);
            }
            lastEntry = entryIndex;
            if (entryIndex < 0 || entryIndex >= maxEntries) {
                LOG.warning(String.format("Server sent bogus string index %d for table %s", entryIndex, tableName));
            }
            @NotNull String entry = "";
            if (bb.getBoolean()) {
                if (bb.getBoolean()) { // Substring check
                    int index = (int) bb.getBits(5);
                    int bytestocopy = (int) bb.getBits(SUBSTRING_BITS);
                    entry = history.get(index).substring(0, bytestocopy + 1);
                    @NotNull String substr = bb.getString(bytestocopy);
                    entry += substr;
                } else {
                    entry = bb.getString();
                }
                l.add(new Pair<Object, Object>("entry", entry));
            }
            // Read in the user data.
            if (bb.getBoolean()) {
                @NotNull byte[] tempbuf = new byte[MAX_USERDATA_SIZE];
                if (userDataFixedSize) {
                    assert userDataSize > 0;
                    // TODO: store in tempbuf
                    l.add(new Pair<Object, Object>("Userdata", bb.getBits(userDataSizeBits)));
                } else {
                    int nBytes = (int) bb.getBits(MAX_USERDATA_BITS);
                    assert nBytes <= MAX_USERDATA_SIZE : (String.format("message too large (%d bytes).", nBytes));
                    bb.get(tempbuf, 0, nBytes);
                }
            }
            if (entryIndex < numEntries) { // Updating
            } else { // Adding
            }
            if (history.size() > 31) {
                history.remove(0);
            }
            history.add(entry);
        }
    }
}
