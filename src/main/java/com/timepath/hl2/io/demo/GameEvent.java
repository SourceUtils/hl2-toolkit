package com.timepath.hl2.io.demo;

import com.timepath.io.BitBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

class GameEvent {

    private static final Logger LOG = Logger.getLogger(GameEvent.class.getName());
    public final Map<String, GameEventMessageType> declarations;
    @NotNull
    public final String name;

    GameEvent(@NotNull BitBuffer bb) {
        name = bb.getString();
        @NotNull Map<String, GameEventMessageType> decl = new LinkedHashMap<>(0);
        while (true) {
            int entryType = (int) bb.getBits(3);
            if (entryType == 0) { // End of event description
                break;
            }
            @NotNull String entryName = bb.getString();
            decl.put(entryName, GameEventMessageType.get(entryType));
        }
        declarations = Collections.unmodifiableMap(decl);
    }

    @NotNull
    public Map<String, Object> parse(BitBuffer bb) {
        @NotNull Map<String, Object> values = new LinkedHashMap<String, Object>(declarations);
        for (@NotNull Map.Entry<String, GameEventMessageType> entry : declarations.entrySet()) {
            values.put(entry.getKey(), entry.getValue().parse(bb));
        }
        return values;
    }

    @NotNull
    @Override
    public String toString() {
        return name + ": " + declarations;
    }
}
