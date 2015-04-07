package com.timepath.hl2.io.demo

import com.timepath.hl2.io.util.Vector3f
import com.timepath.io.BitBuffer
import java.math.BigInteger
import java.util.LinkedList

public class Packet(public val type: Packet.Type, public val offset: Int) {

    public val list: MutableList<Pair<Any, Any>> = LinkedList()

    override fun toString() = "${type}, offset ${offset}"

    /**
     * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.h
     */
    public enum class Type(
            /** The opcode for this packet */
            private val id: Int,
            /** The handler associated with this packet */
            val handler: PacketHandler = object : PacketHandler {}) {
        net_NOP : Type(0, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                return true
            }
        })
        net_Disconnect : Type(1, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(("Reason" to bb.getString()))
                return true
            }
        })
        net_File : Type(2, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(("Transfer ID" to bb.getInt()))
                l.add(("Filename" to bb.getString()))
                l.add(("Requested" to bb.getBoolean()))
                return true
            }
        })
        net_Tick : Type(3, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(("Tick" to bb.getInt()))
                l.add(("Host frametime" to bb.getShort()))
                l.add(("Host frametime StdDev" to bb.getShort()))
                return true
            }
        })
        net_StringCmd : Type(4, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(("Command" to bb.getString()))
                return true
            }
        })
        net_SetConVar : Type(5, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val n = bb.getByte()
                n.toInt().times {
                    l.add((bb.getString() to bb.getString()))
                }
                return true
            }
        })
        net_SignonState : Type(6, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val state = bb.getByte().toInt() and 0xFF
                val signon = SignonState.values()
                l.add(("Signon state" to if (state < signon.size()) signon[state] else state))
                l.add(("Spawn count" to bb.getBits(32)))
                return true
            }
        })
        /** 16 in newer protocols */
        svc_Prval : Type(7, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(("Value" to bb.getString()))
                return true
            }
        })
        svc_ServerInfo : Type(8, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val version = bb.getBits(16)
                l.add(("Version" to version))
                l.add(("Server count" to bb.getBits(32)))
                l.add(("SourceTV" to bb.getBoolean()))
                l.add(("Dedicated" to bb.getBoolean()))
                l.add(("Server client CRC" to "0x${Integer.toHexString(bb.getInt())}"))
                l.add(("Max classes" to bb.getBits(16)))
                if (version >= 18) {
                    val md5 = ByteArray(16)
                    bb.get(md5)
                    l.add(("Server map MD5" to
                            java.lang.String.format("%0${md5.size() * 2}x",
                                    BigInteger(1, md5))))
                } else {
                    l.add(("Server map CRC" to "0x${Integer.toHexString(bb.getInt())}"))
                }
                l.add(("Current player count" to bb.getBits(8)))
                l.add(("Max player count" to bb.getBits(8)))
                l.add(("Interval per tick" to bb.getFloat()))
                l.add(("Platform" to bb.getBits(8).toChar()))
                l.add(("Game directory" to bb.getString()))
                l.add(("Map name" to bb.getString()))
                l.add(("Skybox name" to bb.getString()))
                l.add(("Hostname" to bb.getString()))
                l.add(("Has replay" to bb.getBoolean())) // ???: protocol version
                return true
            }
        })
        /** TODO */
        svc_SendTable : Type(9)
        svc_ClassInfo : Type(10, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val n = bb.getShort().toInt()
                l.add(("Number of server classes" to n))
                val cc = bb.getBoolean()
                l.add(("Create classes on client" to cc))
                demo.serverClassBits = log2(n) + 1
                l.add(("serverClassBits" to demo.serverClassBits))
                if (!cc) {
                    n.times {
                        l.add(("Class ID" to bb.getBits(demo.serverClassBits)))
                        l.add(("Class name" to bb.getString()))
                        l.add(("Datatable name" to bb.getString()))
                    }
                }
                return true
            }
        })
        svc_SetPause : Type(11, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(("Paused" to bb.getBoolean()))
                return true
            }
        })
        /**
         * TODO
         * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.cpp#L898
         */
        svc_CreateStringTable : Type(12, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val tableName = bb.getString()
                l.add(("Table name" to tableName))
                val maxEntries = bb.getShort().toInt()
                l.add(("Max entries" to maxEntries))
                val entryBits = log2(maxEntries)
                val numEntries = bb.getBits(entryBits + 1)
                l.add(("Number of entries" to numEntries))
                val length = bb.getBits(HL2DEM.NET_MAX_PALYLOAD_BITS + 3)
                l.add(("Length in bits" to length))
                val userDataFixedSize = bb.getBoolean()
                l.add(("Userdata fixed size" to userDataFixedSize))
                var userDataSize = -1
                var userDataSizeBits = -1
                if (userDataFixedSize) {
                    userDataSize = bb.getBits(12).toInt()
                    l.add(("Userdata size" to userDataSize))
                    userDataSizeBits = bb.getBits(4).toInt()
                    l.add(("Userdata bits" to userDataSizeBits))
                }
                StringTable.create(tableName, maxEntries.toInt(), entryBits, userDataFixedSize, userDataSize, userDataSizeBits)
                //                .parse(bb, l)

                bb.getBits(length.toInt()) // Skip
                return true
            }
        })
        /**
         * TODO
         * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.cpp#L837
         */
        svc_UpdateStringTable : Type(13, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val tableID = bb.getBits(log2(StringTable.MAX_TABLES)) // 5 bits
                l.add(("Table ID" to tableID))
                val changedEntries = if (bb.getBoolean()) bb.getBits(16) else 1
                l.add(("Changed entries" to changedEntries))
                val length = bb.getBits(20)
                l.add(("Length in bits" to length))
                StringTable[tableID.toInt()]
                //                .parse(bb, l)

                bb.getBits(length.toInt()) // Skip
                return true
            }
        })
        svc_VoiceInit : Type(14, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(("Codec" to bb.getString()))
                l.add(("Quality" to bb.getByte()))
                return true
            }
        })
        svc_VoiceData : Type(15, object : PacketHandler {
            val s = VoiceDataHandler()
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                return s.read(bb, l, demo, lengthBits)
            }
        })
        /**
         * TODO: One of these
         * svc_HLTV: HLTV control messages
         * svc_Print: split screen style message
         */
        svc_Unknown16 : Type(16)
        /** TODO */
        svc_Sounds : Type(17, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val reliable = bb.getBoolean()
                l.add(("Reliable" to reliable))
                val count = if (reliable) 1 else bb.getByte()
                l.add(("Number of sounds" to count))
                val length = if (reliable) bb.getByte().toInt() else bb.getShort().toInt()
                l.add(("Length in bits" to length))
                bb.getBits(length) // Skip
                return true
            }
        })
        svc_SetView : Type(18, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(("Entity index" to bb.getBits(11)))
                return true
            }
        })
        svc_FixAngle : Type(19, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(("Relative" to bb.getBoolean()))
                val v = Vector3f(readBitAngle(bb, 16), readBitAngle(bb, 16), readBitAngle(bb, 16))
                l.add(("Vector" to v))
                return true
            }

            fun readBitAngle(bb: BitBuffer, numbits: Int): Float {
                return bb.getBits(numbits) * (360.0f / (1 shl numbits))
            }
        })
        svc_CrosshairAngle : Type(20, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val v = Vector3f(readBitAngle(bb, 16), readBitAngle(bb, 16), readBitAngle(bb, 16))
                l.add(("Vector" to v))
                return true
            }

            fun readBitAngle(bb: BitBuffer, numbits: Int): Float {
                return bb.getBits(numbits) * (360.0f / (1 shl numbits))
            }
        })
        svc_BSPDecal : Type(21, object : PacketHandler {
            fun getCoord(bb: BitBuffer): Float {
                val hasint = bb.getBoolean()
                val hasfract = bb.getBoolean()
                var value = 0f
                if (hasint || hasfract) {
                    val sign = bb.getBoolean()
                    if (hasint) {
                        value += bb.getBits(14) + 1
                    }
                    if (hasfract) {
                        value += bb.getBits(5) * (1 / 32f)
                    }
                    if (sign) {
                        value = -value
                    }
                }
                return value
            }

            fun getVecCoord(bb: BitBuffer): Vector3f {
                val hasx = bb.getBoolean()
                val hasy = bb.getBoolean()
                val hasz = bb.getBoolean()
                return Vector3f(if (hasx) getCoord(bb) else 0f, if (hasy) getCoord(bb) else 0f, if (hasz) getCoord(bb) else 0f)
            }

            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(("Position" to getVecCoord(bb)))
                l.add(("Decal texture index" to bb.getBits(HL2DEM.MAX_DECAL_INDEX_BITS)))
                if (bb.getBoolean()) {
                    l.add(("Entity index" to bb.getBits(HL2DEM.MAX_EDICT_BITS)))
                    var bits = HL2DEM.SP_MODEL_INDEX_BITS
                    if (demo.header.demoProtocol <= 21) {
                        bits--
                    }
                    l.add(("Model index" to bb.getBits(bits)))
                }
                l.add(("Low priority" to bb.getBoolean()))
                return true
            }
        })
        /**
         * TODO: One of these
         * svc_TerrainMod: modification to the terrain/displacement
         * svc_SplitScreen: split screen style message
         */
        svc_unknown2 : Type(22)
        /** TODO */
        svc_UserMessage : Type(23, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                return UserMessage.read(bb, l, demo)
            }
        })
        /** TODO */
        svc_EntityMessage : Type(24, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(("Entity index" to bb.getBits(11)))
                l.add(("Class ID" to bb.getBits(9)))
                val length = bb.getBits(11).toInt()
                l.add(("Length in bits" to length))
                bb.getBits(length) // Skip
                return true
            }
        })
        svc_GameEvent : Type(25, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val length = bb.getBits(11).toInt()
                l.add(("Length in bits" to length))
                val gameEventId = bb.getBits(9).toInt()
                val gameEvent = demo.gameEvents[gameEventId]
                if (gameEvent != null) {
                    l.add((gameEvent.name to gameEvent.parse(bb).entrySet()))
                } else {
                    l.add(("Unknown event" to gameEventId))
                    bb.getBits(length - 9) // Skip
                }
                return true
            }
        })
        /**
         * Non-delta compressed entities.
         * TODO
         *
         * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/baseclientstate.cpp#L1245<a/>
         * @see <a>https://code.google.com/p/coldemoplayer/source/browse/branches/2.0/compLexity+Demo+Player/CDP
         * .HalfLifeDemo/Messages/SvcPacketEntities.cs?r=59#43<a/>
         * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/packed_entity.h#L52<a/>
         * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/sv_ents_write.cpp#L862<a/>
         */
        svc_PacketEntities : Type(26, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val MAX_EDICT_BITS = 11
                val DELTASIZE_BITS = 20
                val maxEntries = bb.getBits(MAX_EDICT_BITS)
                l.add(("Max entries" to maxEntries))
                val isDelta = bb.getBoolean()
                l.add(("Is delta" to isDelta))
                if (isDelta) {
                    val deltaFrom = bb.getBits(32).toInt()
                    l.add(("Delta from" to deltaFrom))
                }
                val baseline = bb.getBoolean()
                l.add(("Baseline" to baseline))
                val updatedEntries = bb.getBits(MAX_EDICT_BITS)
                l.add(("Updated entries" to updatedEntries))
                val length = bb.getBits(DELTASIZE_BITS)
                l.add(("Length in bits" to length))
                val updateBaseline = bb.getBoolean()
                l.add(("Update baseline" to updateBaseline))
                bb.getBits(length.toInt()) // Skip
                return true
            }
        })
        /**
         * TODO
         * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/servermsghandler.cpp#L738
         */
        svc_TempEntities : Type(27, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val numEntries = bb.getBits(HL2DEM.EVENT_INDEX_BITS)
                l.add(("Number of entries" to numEntries))
                val length = bb.getBits(HL2DEM.NET_MAX_PALYLOAD_BITS)
                l.add(("Length in bits" to length))
                // FIXME: underflows, but is usually last
                bb.getBits(length.toInt()) // Skip
                return true
                /*
            if(numEntries == 0) {
                val reliable = true
                numEntries = 1
            }
            val classID = -1
            l.add(("serverClassBits" to demo.serverClassBits))
            for(val i = 0 i < numEntries i++) {
                float delay = 0
                if(bb.getBoolean()) {
                    delay = bb.getBits(8) / 100.0f
                }
                l.add(("ent[" + i + "].delay" to delay))
                if(bb.getBoolean()) { // Full update
                    classID = (int) bb.getBits(demo.serverClassBits)
                }
                l.add(("ent[" + i + "].classID" to classID))
            }
            return false
            */
            }
        })
        svc_Prefetch : Type(28, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val bits = if (demo.header.networkProtocol >= 23) HL2DEM.MAX_SOUND_INDEX_BITS else 13
                l.add(("Sound index" to bb.getBits(bits)))
                return true
            }
        })
        /** TODO */
        svc_Menu : Type(29, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(("Menu type" to bb.getBits(16)))
                val length = bb.getBits(16).toInt()
                l.add(("Length in bytes" to length))
                bb.getBits(length * 8) // Skip
                return true
            }
        })
        svc_GameEventList : Type(30, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val numGameEvents = bb.getBits(9).toInt()
                demo.gameEvents = arrayOfNulls<GameEvent>(HL2DEM.MAX_GAME_EVENTS)
                l.add(("Number of events" to numGameEvents))
                val length = bb.getBits(20)
                l.add(("Length in bits" to length))
                numGameEvents.times {
                    val id = bb.getBits(9).toInt()
                    val gameEvent = GameEvent(bb)
                    demo.gameEvents[id] = gameEvent
                    l.add(("gameEvents[$id] = ${gameEvent.name}" to
                            gameEvent.declarations.entrySet()))
                }
                return true
            }
        })
        svc_GetCvarValue : Type(31, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(("Cookie" to "0x${Integer.toHexString(bb.getInt())}"))
                l.add(("value" to bb.getString()))
                return true
            }
        })
        /** TODO */
        svc_CmdKeyValues : Type(32, object : PacketHandler {
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val length = bb.getInt()
                l.add(("Length in bits: " to length))
                bb.getBits(length) // Skip
                return true
            }
        })

        companion object {

            public fun get(i: Int): Type? = Type.values().firstOrNull { it.id == i }

            private fun log2(i: Int) = (Math.log(i.toDouble()) / Math.log(2.0)).toInt()
        }
    }
}
