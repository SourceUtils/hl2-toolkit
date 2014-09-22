package com.timepath.hl2.io.demo;

import com.timepath.io.BitBuffer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

class GameEvent {

    private static final Logger LOG = Logger.getLogger(GameEvent.class.getName());
    public final Map<String, GameEventMessageType> declarations;
    public final String name;

    GameEvent(BitBuffer bb) {
        name = bb.getString();
        Map<String, GameEventMessageType> decl = new LinkedHashMap<>(0);
        while (true) {
            int entryType = (int) bb.getBits(3);
            if (entryType == 0) { // End of event description
                break;
            }
            String entryName = bb.getString();
            decl.put(entryName, GameEventMessageType.get(entryType));
        }
        declarations = Collections.unmodifiableMap(decl);
    }

    public Map<String, Object> parse(BitBuffer bb) {
        Map<String, Object> values = new LinkedHashMap<String, Object>(declarations);
        for (Map.Entry<String, GameEventMessageType> entry : declarations.entrySet()) {
            values.put(entry.getKey(), entry.getValue().parse(bb));
        }
        return values;
    }

    @Override
    public String toString() {
        return name + ": " + declarations;
    }
}
