package com.timepath.hl2.io.demo

import com.timepath.hl2.io.util.Vector3f
import com.timepath.io.BitBuffer
import com.timepath.log
import com.timepath.toUnsigned
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
            open val handler: PacketHandler = object : PacketHandler {}) {
        net_NOP(0) {
            override val handler = PacketHandler { bb, l, demo, lengthBits -> true }
        },
        net_Disconnect(1) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l.add("Reason" to bb.getString())
                true
            }
        },
        net_File(2) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l.add("Transfer ID" to bb.getInt())
                l.add("Filename" to bb.getString())
                l.add("Requested" to bb.getBoolean())
                true
            }
        },
        net_Tick(3) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l.add("Tick" to bb.getInt())
                l.add("Host frametime" to bb.getShort())
                l.add("Host frametime StdDev" to bb.getShort())
                true
            }
        },
        net_StringCmd(4) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l.add("Command" to bb.getString())
                true
            }
        },
        net_SetConVar(5) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val n = bb.getUByte()
                repeat(n) {
                    l.add(bb.getString() to bb.getString())
                }
                true
            }
        },
        net_SignonState(6) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val state = bb.getUByte()
                l.add("Signon state" to (SignonState[state] ?: state))
                l.add("Spawn count" to bb.getInt())
                true
            }
        },
        /** 16 in newer protocols */
        svc_Prval(7) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l.add("Value" to bb.getString())
                true
            }
        },
        svc_ServerInfo(8) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val version = bb.getUShort()
                l.add("Version" to version)
                l.add("Server count" to bb.getUInt())
                l.add("SourceTV" to bb.getBoolean())
                l.add("Dedicated" to bb.getBoolean())
                l.add("Server client CRC" to "0x${Integer.toHexString(bb.getInt())}")
                l.add("Max classes" to bb.getUShort())
                if (version >= 18) {
                    val md5 = ByteArray(16)
                    bb.get(md5)
                    l.add("Server map MD5" to "%0${md5.size() * 2}x".format(BigInteger(1, md5)))
                } else {
                    l.add("Server map CRC" to "0x${Integer.toHexString(bb.getInt())}")
                }
                l.add("Current player count" to bb.getUByte())
                l.add("Max player count" to bb.getUByte())
                l.add("Interval per tick" to bb.getFloat())
                l.add("Platform" to bb.getByte().toChar())
                l.add("Game directory" to bb.getString())
                l.add("Map name" to bb.getString())
                l.add("Skybox name" to bb.getString())
                l.add("Hostname" to bb.getString())
                l.add("Has replay" to bb.getBoolean()) // ???: protocol version
                true
            }
        },
        /** TODO */
        svc_SendTable(9),
        svc_ClassInfo(10) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val n = bb.getShort().toInt()
                l.add("Number of server classes" to n)
                val cc = bb.getBoolean()
                l.add("Create classes on client" to cc)
                demo.serverClassBits = log(2, n) + 1
                l.add("serverClassBits" to demo.serverClassBits)
                if (!cc) {
                    repeat(n) {
                        l.add("Class ID" to bb.getBits(demo.serverClassBits))
                        l.add("Class name" to bb.getString())
                        l.add("Datatable name" to bb.getString())
                    }
                }
                true
            }
        },
        svc_SetPause(11) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l.add("Paused" to bb.getBoolean())
                true
            }
        },
        /**
         * TODO
         * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.cpp#L898
         */
        svc_CreateStringTable(12) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val tableName = bb.getString()
                l.add("Table name" to tableName)
                val maxEntries = bb.getShort().toInt()
                l.add("Max entries" to maxEntries)
                val entryBits = log(2, maxEntries)
                val numEntries = bb.getBits(entryBits + 1)
                l.add("Number of entries" to numEntries)
                val length = bb.getBits(HL2DEM.NET_MAX_PALYLOAD_BITS + 3)
                l.add("Length in bits" to length)
                val userDataFixedSize = bb.getBoolean()
                l.add("Userdata fixed size" to userDataFixedSize)
                var userDataSize = -1
                var userDataSizeBits = -1
                if (userDataFixedSize) {
                    userDataSize = bb.getBits(12).toInt()
                    l.add("Userdata size" to userDataSize)
                    userDataSizeBits = bb.getBits(4).toInt()
                    l.add("Userdata bits" to userDataSizeBits)
                }
                StringTable.create(tableName, maxEntries.toInt(), entryBits, userDataFixedSize, userDataSize, userDataSizeBits)
                //                .parse(bb, l)

                bb.getBits(length.toInt()) // Skip
                true
            }
        },
        /**
         * TODO
         * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.cpp#L837
         */
        svc_UpdateStringTable(13) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val tableID = bb.getBits(log(2, StringTable.MAX_TABLES)) // 5 bits
                l.add("Table ID" to tableID)
                val changedEntries = if (bb.getBoolean()) bb.getShort() else 1
                l.add("Changed entries" to changedEntries)
                val length = bb.getBits(20)
                l.add("Length in bits" to length)
                // StringTable[tableID.toInt()]?.parse(bb, l)
                bb.getBits(length.toInt()) // Skip
                true
            }
        },
        svc_VoiceInit(14) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l.add("Codec" to bb.getString())
                l.add("Quality" to bb.getByte())
                true
            }
        },
        svc_VoiceData(15) {
            val vdh = VoiceDataHandler()
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                vdh.read(bb, l, demo, lengthBits)
            }
        },
        /**
         * TODO: One of these
         * svc_HLTV: HLTV control messages
         * svc_Print: split screen style message
         */
        svc_Unknown16(16),
        /** TODO */
        svc_Sounds(17) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val reliable = bb.getBoolean()
                l.add("Reliable" to reliable)
                val count = if (reliable) 1 else bb.getUByte()
                l.add("Number of sounds" to count)
                val length = if (reliable) bb.getByte().toUnsigned() else bb.getShort().toUnsigned()
                l.add("Length in bits" to length)
                bb.getBits(length) // Skip
                true
            }
        },
        svc_SetView(18) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l.add("Entity index" to bb.getBits(11))
                true
            }
        },
        svc_FixAngle(19) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l.add("Relative" to bb.getBoolean())
                val v = Vector3f(readBitAngle(bb, 16), readBitAngle(bb, 16), readBitAngle(bb, 16))
                l.add("Vector" to v)
                true
            }
        },
        svc_CrosshairAngle(20) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val v = Vector3f(readBitAngle(bb, 16), readBitAngle(bb, 16), readBitAngle(bb, 16))
                l.add("Vector" to v)
                true
            }
        },
        svc_BSPDecal(21) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l.add("Position" to getVecCoord(bb))
                l.add("Decal texture index" to bb.getBits(HL2DEM.MAX_DECAL_INDEX_BITS))
                if (bb.getBoolean()) {
                    l.add(("Entity index" to bb.getBits(HL2DEM.MAX_EDICT_BITS)))
                    var bits = HL2DEM.SP_MODEL_INDEX_BITS
                    if (demo.header.demoProtocol <= 21) {
                        bits--
                    }
                    l.add("Model index" to bb.getBits(bits))
                }
                l.add("Low priority" to bb.getBoolean())
                true
            }
        },
        /**
         * TODO: One of these
         * svc_TerrainMod: modification to the terrain/displacement
         * svc_SplitScreen: split screen style message
         */
        svc_unknown2(22),
        /** TODO */
        svc_UserMessage(23) {
            override val handler = PacketHandler { bb, l, demo, lengthBits -> UserMessage.read(bb, l, demo) }
        }
        ,
        /** TODO */
        svc_EntityMessage(24) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l.add("Entity index" to bb.getBits(11))
                l.add("Class ID" to bb.getBits(9))
                val length = bb.getBits(11).toInt()
                l.add("Length in bits" to length)
                bb.getBits(length) // Skip
                true
            }
        },
        svc_GameEvent(25) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val length = bb.getBits(11).toInt()
                l.add("Length in bits" to length)
                val gameEventId = bb.getBits(9).toInt()
                val gameEvent = demo.gameEvents[gameEventId]
                if (gameEvent != null) {
                    l.add(gameEvent.name to gameEvent.parse(bb).entrySet())
                } else {
                    l.add("Unknown event" to gameEventId)
                    bb.getBits(length - 9) // Skip
                }
                true
            }
        },
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
        svc_PacketEntities(26) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val MAX_EDICT_BITS = 11
                val DELTASIZE_BITS = 20
                val maxEntries = bb.getBits(MAX_EDICT_BITS)
                l.add("Max entries" to maxEntries)
                val isDelta = bb.getBoolean()
                l.add("Is delta" to isDelta)
                if (isDelta) {
                    val deltaFrom = bb.getInt()
                    l.add("Delta from" to deltaFrom)
                }
                val baseline = bb.getBoolean()
                l.add("Baseline" to baseline)
                val updatedEntries = bb.getBits(MAX_EDICT_BITS)
                l.add("Updated entries" to updatedEntries)
                val length = bb.getBits(DELTASIZE_BITS)
                l.add("Length in bits" to length)
                val updateBaseline = bb.getBoolean()
                l.add("Update baseline" to updateBaseline)
                bb.getBits(length.toInt()) // Skip
                true
            }
        },
        /**
         * TODO
         * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/servermsghandler.cpp#L738
         */
        svc_TempEntities(27) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val numEntries = bb.getBits(HL2DEM.EVENT_INDEX_BITS)
                l.add("Number of entries" to numEntries)
                val length = bb.getBits(HL2DEM.NET_MAX_PALYLOAD_BITS)
                l.add("Length in bits" to length)
                // FIXME: underflows, but is usually last
                bb.getBits(length.toInt()) // Skip
                true
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
        },
        svc_Prefetch(28) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val bits = if (demo.header.networkProtocol >= 23) HL2DEM.MAX_SOUND_INDEX_BITS else 13
                l.add("Sound index" to bb.getBits(bits))
                true
            }
        },
        /** TODO */
        svc_Menu(29) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l.add("Menu type" to bb.getShort())
                val length = bb.getUShort()
                l.add("Length in bytes" to length)
                bb.getBits(length * 8) // Skip
                true
            }
        },
        svc_GameEventList(30) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val numGameEvents = bb.getBits(9).toInt()
                l.add("Number of events" to numGameEvents)
                val length = bb.getBits(20)
                l.add("Length in bits" to length)
                repeat(numGameEvents) {
                    val id = bb.getBits(9).toInt()
                    val gameEvent = GameEvent(bb)
                    demo.gameEvents[id] = gameEvent
                    l.add("gameEvents[$id] = ${gameEvent.name}" to
                            gameEvent.declarations.entrySet())
                }
                true
            }
        },
        svc_GetCvarValue(31) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l.add("Cookie" to "0x${Integer.toHexString(bb.getInt())}")
                l.add("value" to bb.getString())
                true
            }
        },
        /** TODO */
        svc_CmdKeyValues(32) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val length = bb.getInt()
                l.add("Length in bits: " to length)
                bb.getBits(length) // Skip
                true
            }
        };

        public fun PacketHandler(f: (BitBuffer, MutableList<Pair<Any, Any>>, HL2DEM, Int) -> Boolean): PacketHandler {
            return object : PacketHandler {
                override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int)
                        = f(bb, l, demo, lengthBits)
            }
        }

        companion object {

            fun get(i: Int) = values().firstOrNull { it.id == i }

            fun readBitAngle(bb: BitBuffer, numbits: Int): Float {
                return bb.getBits(numbits) * (360f / (1 shl numbits))
            }

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
        }
    }
}
