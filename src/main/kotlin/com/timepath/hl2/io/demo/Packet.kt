package com.timepath.hl2.io.demo

import com.timepath.hl2.io.util.Vector3f
import com.timepath.io.BitBuffer
import com.timepath.log
import com.timepath.toUnsigned
import java.math.BigInteger

public class Packet(public val type: PacketHandler, public val offset: Int) {

    public val list: TupleMap<Any, Any> = TupleMap()

    override fun toString() = "${type}, offset ${offset}"

    // https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.h

    object net_NOP : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean = true
    }

    object net_Disconnect : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Reason"] = bb.getString()
            return true
        }
    }

    object net_File : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Transfer ID"] = bb.getInt()
            l["Filename"] = bb.getString()
            l["Requested"] = bb.getBoolean()
            return true
        }
    }

    object net_Tick : PacketHandler {
        data class Tick(val tick: Int, val `host frametime`: Short, val `host frametime stddev`: Short)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int) = Tick(
                bb.getInt(),
                bb.getShort(),
                bb.getShort()
        )
    }

    object net_StringCmd : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Command"] = bb.getString()
            return true
        }
    }

    object net_SetConVar : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            val n = bb.getUByte()
            repeat(n) {
                l[bb.getString()] = bb.getString()
            }
            return true
        }
    }

    object net_SignonState : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            val state = bb.getUByte()
            l["Signon state"] = (SignonState[state] ?: state)
            l["Spawn count"] = bb.getInt()
            return true
        }
    }

    /** 16 in newer protocols */
    object svc_Prval : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Value"] = bb.getString()
            return true
        }
    }

    object svc_ServerInfo : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
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
            return true
        }
    }

    /** TODO */
    object svc_SendTable : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean = false
    }

    object svc_ClassInfo : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
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
            return true
        }
    }

    object svc_SetPause : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Paused"] = bb.getBoolean()
            return true
        }
    }

    /**
     * TODO
     * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.cpp#L898
     */
    object svc_CreateStringTable : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
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
            // .parse(bb, l)

            bb.getBits(length.toInt()) // Skip
            return true
        }
    }

    /**
     * TODO
     * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.cpp#L837
     */
    object svc_UpdateStringTable : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            val tableID = bb.getBits(log(2, StringTable.MAX_TABLES)) // 5 bits
            l["Table ID"] = tableID
            val changedEntries = if (bb.getBoolean()) bb.getShort() else 1
            l["Changed entries"] = changedEntries
            val length = bb.getBits(20)
            l["Length in bits"] = length
            // StringTable[tableID.toInt()]?.parse(bb, l)
            bb.getBits(length.toInt()) // Skip
            return true
        }
    }

    object svc_VoiceInit : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Codec"] = bb.getString()
            l["Quality"] = bb.getByte()
            return true
        }
    }

    object svc_VoiceData : PacketHandler by VoiceDataHandler()

    /**
     * TODO: One of these
     * svc_HLTV: HLTV control messages
     * svc_Print: split screen style message
     */
    object svc_Unknown16 : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean = false
    }

    /** TODO */
    object svc_Sounds : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            val reliable = bb.getBoolean()
            l["Reliable"] = reliable
            val count = if (reliable) 1 else bb.getUByte()
            l["Number of sounds"] = count
            val length = if (reliable) bb.getByte().toUnsigned() else bb.getShort().toUnsigned()
            l["Length in bits"] = length
            bb.getBits(length) // Skip
            return true
        }
    }

    object svc_SetView : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Entity index"] = bb.getBits(11)
            return true
        }
    }

    object svc_FixAngle : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Relative"] = bb.getBoolean()
            val v = Vector3f(readBitAngle(bb, 16), readBitAngle(bb, 16), readBitAngle(bb, 16))
            l["Vector"] = v
            return true
        }
    }

    object svc_CrosshairAngle : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            val v = Vector3f(readBitAngle(bb, 16), readBitAngle(bb, 16), readBitAngle(bb, 16))
            l["Vector"] = v
            return true
        }
    }

    object svc_BSPDecal : PacketHandler {
        data class BSPDecal(
                val position: Vector3f,
                val `decal texture index`: Long,
                val entityIndex: Long?,
                val modelIndex: Long?,
                val lowPriority: Boolean
        )

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int): Any {
            val position = getVecCoord(bb)
            val index = bb.getBits(HL2DEM.MAX_DECAL_INDEX_BITS)
            val entityIndex: Long?
            val modelIndex: Long?
            if (bb.getBoolean()) {
                entityIndex = bb.getBits(HL2DEM.MAX_EDICT_BITS)
                var bits = HL2DEM.SP_MODEL_INDEX_BITS
                if (demo.header.demoProtocol <= 21) {
                    bits--
                }
                modelIndex = bb.getBits(bits)
            } else {
                entityIndex = null
                modelIndex = null
            }
            val lowPriority = bb.getBoolean()
            return BSPDecal(position, index, entityIndex, modelIndex, lowPriority)
        }
    }

    /**
     * TODO: One of these
     * svc_TerrainMod: modification to the terrain/displacement
     * svc_SplitScreen: split screen style message
     */
    object svc_unknown22 : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean = false
    }

    /** TODO */
    object svc_UserMessage : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            return UserMessage.read(bb, l, demo)
        }
    }

    /** TODO */
    object svc_EntityMessage : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Entity index"] = bb.getBits(11)
            l["Class ID"] = bb.getBits(9)
            val length = bb.getBits(11).toInt()
            l["Length in bits"] = length
            bb.getBits(length) // Skip
            return true
        }
    }

    object svc_GameEvent : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
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
            return true
        }
    }

    /**
     * Non-delta compressed entities.
     * TODO
     *
     * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/baseclientstate.cpp#L1245<a/>
     * @see <a>https://code.google.com/p/coldemoplayer/source/browse/branches/2.0/compLexity+Demo+Player/CDP.HalfLifeDemo/Messages/SvcPacketEntities.cs?r=59#43<a/>
     * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/packed_entity.h#L52<a/>
     * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/sv_ents_write.cpp#L862<a/>
     */
    object svc_PacketEntities : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
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
            return true
        }
    }

    /**
     * TODO
     * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/servermsghandler.cpp#L738
     */
    object svc_TempEntities : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            val numEntries = bb.getBits(HL2DEM.EVENT_INDEX_BITS)
            l["Number of entries"] = numEntries
            val length = bb.getBits(HL2DEM.NET_MAX_PALYLOAD_BITS)
            l["Length in bits"] = length
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
    }

    object svc_Prefetch : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            val bits = when {
                demo.header.networkProtocol >= 23 -> HL2DEM.MAX_SOUND_INDEX_BITS
                else -> 13
            }
            l["Sound index"] = bb.getBits(bits)
            return true
        }
    }

    /** TODO */
    object svc_Menu : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Menu type"] = bb.getShort()
            val length = bb.getUShort()
            l["Length in bytes"] = length
            bb.getBits(length * 8) // Skip
            return true
        }
    }

    object svc_GameEventList : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
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
            return true
        }
    }

    object svc_GetCvarValue : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Cookie"] = "0x${Integer.toHexString(bb.getInt())}"
            l["value"] = bb.getString()
            return true
        }
    }

    /** TODO */
    object svc_CmdKeyValues : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            val length = bb.getInt()
            l["Length in bits"] = length
            bb.getBits(length) // Skip
            return true
        }
    }

    companion object {

        public fun get(i: Int): PacketHandler? = values[i]

        protected fun readBitAngle(bb: BitBuffer, numbits: Int): Float {
            return bb.getBits(numbits) * (360f / (1 shl numbits))
        }

        protected fun getCoord(bb: BitBuffer): Float {
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

        protected fun getVecCoord(bb: BitBuffer): Vector3f {
            val x = bb.getBoolean()
            val y = bb.getBoolean()
            val z = bb.getBoolean()
            return Vector3f(
                    if (x) getCoord(bb) else 0f,
                    if (y) getCoord(bb) else 0f,
                    if (z) getCoord(bb) else 0f)
        }

        private val values = mapOf(
                0 to net_NOP,
                1 to net_Disconnect,
                2 to net_File,
                3 to net_Tick,
                4 to net_StringCmd,
                5 to net_SetConVar,
                6 to net_SignonState,
                7 to svc_Prval,
                8 to svc_ServerInfo,
                9 to svc_SendTable,
                10 to svc_ClassInfo,
                11 to svc_SetPause,
                12 to svc_CreateStringTable,
                13 to svc_UpdateStringTable,
                14 to svc_VoiceInit,
                15 to svc_VoiceData,
                16 to svc_Unknown16,
                17 to svc_Sounds,
                18 to svc_SetView,
                19 to svc_FixAngle,
                20 to svc_CrosshairAngle,
                21 to svc_BSPDecal,
                22 to svc_unknown22,
                23 to svc_UserMessage,
                24 to svc_EntityMessage,
                25 to svc_GameEvent,
                26 to svc_PacketEntities,
                27 to svc_TempEntities,
                28 to svc_Prefetch,
                29 to svc_Menu,
                30 to svc_GameEventList,
                31 to svc_GetCvarValue,
                32 to svc_CmdKeyValues
        )

    }
}
