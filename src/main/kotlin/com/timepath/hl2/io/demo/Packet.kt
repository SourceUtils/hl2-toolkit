package com.timepath.hl2.io.demo

import com.timepath.hl2.io.util.Vector3f
import com.timepath.io.BitBuffer
import com.timepath.log
import com.timepath.toUnsigned
import java.math.BigInteger

public class Packet(public val type: Packet.Type, public val offset: Int) {

    public val list: TupleMap<Any, Any> = TupleMap()

    override fun toString() = "${type}, offset ${offset}"

    /**
     * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.h
     */
    public enum class Type(
            /** The opcode for this packet */
            private val id: Int,
            /** The handler associated with this packet */
            open val handler: PacketHandler = PacketHandler { bb, l, demo, lengthBits -> false }) {
        net_NOP(0) {
            override val handler = PacketHandler { bb, l, demo, lengthBits -> true }
        },
        net_Disconnect(1) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l["Reason"] = bb.getString()
                true
            }
        },
        net_File(2) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l["Transfer ID"] = bb.getInt()
                l["Filename"] = bb.getString()
                l["Requested"] = bb.getBoolean()
                true
            }
        },
        net_Tick(3) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l["Tick"] = bb.getInt()
                l["Host frametime"] = bb.getShort()
                l["Host frametime StdDev"] = bb.getShort()
                true
            }
        },
        net_StringCmd(4) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l["Command"] = bb.getString()
                true
            }
        },
        net_SetConVar(5) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val n = bb.getUByte()
                repeat(n) {
                    l[bb.getString()] = bb.getString()
                }
                true
            }
        },
        net_SignonState(6) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val state = bb.getUByte()
                l["Signon state"] = (SignonState[state] ?: state)
                l["Spawn count"] = bb.getInt()
                true
            }
        },
        /** 16 in newer protocols */
        svc_Prval(7) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l["Value"] = bb.getString()
                true
            }
        },
        svc_ServerInfo(8) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val version = bb.getUShort()
                l["Version"] = version
                l["Server count"] = bb.getUInt()
                l["SourceTV"] = bb.getBoolean()
                l["Dedicated"] = bb.getBoolean()
                l["Server client CRC"] = "0x${Integer.toHexString(bb.getInt())}"
                l["Max classes"] = bb.getUShort()
                if (version >= 18) {
                    val md5 = ByteArray(16)
                    bb.get(md5)
                    l["Server map MD5"] = "%0${md5.size() * 2}x".format(BigInteger(1, md5))
                } else {
                    l["Server map CRC"] = "0x${Integer.toHexString(bb.getInt())}"
                }
                l["Current player count"] = bb.getUByte()
                l["Max player count"] = bb.getUByte()
                l["Interval per tick"] = bb.getFloat()
                l["Platform"] = bb.getByte().toChar()
                l["Game directory"] = bb.getString()
                l["Map name"] = bb.getString()
                l["Skybox name"] = bb.getString()
                l["Hostname"] = bb.getString()
                l["Has replay"] = bb.getBoolean() // ???: protocol version
                true
            }
        },
        /** TODO */
        svc_SendTable(9),
        svc_ClassInfo(10) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val n = bb.getShort().toInt()
                l["Number of server classes"] = n
                val cc = bb.getBoolean()
                l["Create classes on client"] = cc
                demo.serverClassBits = log(2, n) + 1
                l["serverClassBits"] = demo.serverClassBits
                if (!cc) {
                    repeat(n) {
                        l["Class ID"] = bb.getBits(demo.serverClassBits)
                        l["Class name"] = bb.getString()
                        l["Datatable name"] = bb.getString()
                    }
                }
                true
            }
        },
        svc_SetPause(11) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l["Paused"] = bb.getBoolean()
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
                l["Table name"] = tableName
                val maxEntries = bb.getShort().toInt()
                l["Max entries"] = maxEntries
                val entryBits = log(2, maxEntries)
                val numEntries = bb.getBits(entryBits + 1)
                l["Number of entries"] = numEntries
                val length = bb.getBits(HL2DEM.NET_MAX_PALYLOAD_BITS + 3)
                l["Length in bits"] = length
                val userDataFixedSize = bb.getBoolean()
                l["Userdata fixed size"] = userDataFixedSize
                var userDataSize = -1
                var userDataSizeBits = -1
                if (userDataFixedSize) {
                    userDataSize = bb.getBits(12).toInt()
                    l["Userdata size"] = userDataSize
                    userDataSizeBits = bb.getBits(4).toInt()
                    l["Userdata bits"] = userDataSizeBits
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
                l["Table ID"] = tableID
                val changedEntries = if (bb.getBoolean()) bb.getShort() else 1
                l["Changed entries"] = changedEntries
                val length = bb.getBits(20)
                l["Length in bits"] = length
                // StringTable[tableID.toInt()]?.parse(bb, l)
                bb.getBits(length.toInt()) // Skip
                true
            }
        },
        svc_VoiceInit(14) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l["Codec"] = bb.getString()
                l["Quality"] = bb.getByte()
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
                l["Reliable"] = reliable
                val count = if (reliable) 1 else bb.getUByte()
                l["Number of sounds"] = count
                val length = if (reliable) bb.getByte().toUnsigned() else bb.getShort().toUnsigned()
                l["Length in bits"] = length
                bb.getBits(length) // Skip
                true
            }
        },
        svc_SetView(18) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l["Entity index"] = bb.getBits(11)
                true
            }
        },
        svc_FixAngle(19) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l["Relative"] = bb.getBoolean()
                val v = Vector3f(readBitAngle(bb, 16), readBitAngle(bb, 16), readBitAngle(bb, 16))
                l["Vector"] = v
                true
            }
        },
        svc_CrosshairAngle(20) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val v = Vector3f(readBitAngle(bb, 16), readBitAngle(bb, 16), readBitAngle(bb, 16))
                l["Vector"] = v
                true
            }
        },
        svc_BSPDecal(21) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l["Position"] = getVecCoord(bb)
                l["Decal texture index"] = bb.getBits(HL2DEM.MAX_DECAL_INDEX_BITS)
                if (bb.getBoolean()) {
                    l["Entity index"] = bb.getBits(HL2DEM.MAX_EDICT_BITS)
                    var bits = HL2DEM.SP_MODEL_INDEX_BITS
                    if (demo.header.demoProtocol <= 21) {
                        bits--
                    }
                    l["Model index"] = bb.getBits(bits)
                }
                l["Low priority"] = bb.getBoolean()
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
                l["Entity index"] = bb.getBits(11)
                l["Class ID"] = bb.getBits(9)
                val length = bb.getBits(11).toInt()
                l["Length in bits"] = length
                bb.getBits(length) // Skip
                true
            }
        },
        svc_GameEvent(25) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val length = bb.getBits(11).toInt()
                l["Length in bits"] = length
                val gameEventId = bb.getBits(9).toInt()
                val gameEvent = demo.gameEvents[gameEventId]
                if (gameEvent != null) {
                    l[gameEvent.name] = gameEvent.parse(bb).entrySet()
                } else {
                    l["Unknown event"] = gameEventId
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
                l["Max entries"] = maxEntries
                val isDelta = bb.getBoolean()
                l["Is delta"] = isDelta
                if (isDelta) {
                    val deltaFrom = bb.getInt()
                    l["Delta from"] = deltaFrom
                }
                val baseline = bb.getBoolean()
                l["Baseline"] = baseline
                val updatedEntries = bb.getBits(MAX_EDICT_BITS)
                l["Updated entries"] = updatedEntries
                val length = bb.getBits(DELTASIZE_BITS)
                l["Length in bits"] = length
                val updateBaseline = bb.getBoolean()
                l["Update baseline"] = updateBaseline
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
                l["Number of entries"] = numEntries
                val length = bb.getBits(HL2DEM.NET_MAX_PALYLOAD_BITS)
                l["Length in bits"] = length
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
                val bits = when {
                    demo.header.networkProtocol >= 23 -> HL2DEM.MAX_SOUND_INDEX_BITS
                    else -> 13
                }
                l["Sound index"] = bb.getBits(bits)
                true
            }
        },
        /** TODO */
        svc_Menu(29) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l["Menu type"] = bb.getShort()
                val length = bb.getUShort()
                l["Length in bytes"] = length
                bb.getBits(length * 8) // Skip
                true
            }
        },
        svc_GameEventList(30) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val numGameEvents = bb.getBits(9).toInt()
                l["Number of events"] = numGameEvents
                val length = bb.getBits(20)
                l["Length in bits"] = length
                repeat(numGameEvents) {
                    val id = bb.getBits(9).toInt()
                    val gameEvent = GameEvent(bb)
                    demo.gameEvents[id] = gameEvent
                    l["gameEvents[$id] = ${gameEvent.name}"] = gameEvent.declarations.entrySet()
                }
                true
            }
        },
        svc_GetCvarValue(31) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                l["Cookie"] = "0x${Integer.toHexString(bb.getInt())}"
                l["value"] = bb.getString()
                true
            }
        },
        /** TODO */
        svc_CmdKeyValues(32) {
            override val handler = PacketHandler { bb, l, demo, lengthBits ->
                val length = bb.getInt()
                l["Length in bits"] = length
                bb.getBits(length) // Skip
                true
            }
        };

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
                    if (hasint) value += bb.getBits(14) + 1
                    if (hasfract) value += bb.getBits(5) * (1 / 32f)
                    if (sign) value = -value
                }
                return value
            }

            fun getVecCoord(bb: BitBuffer): Vector3f {
                val x = bb.getBoolean()
                val y = bb.getBoolean()
                val z = bb.getBoolean()
                return Vector3f(
                        if (x) getCoord(bb) else 0f,
                        if (y) getCoord(bb) else 0f,
                        if (z) getCoord(bb) else 0f)
            }
        }
    }
}
