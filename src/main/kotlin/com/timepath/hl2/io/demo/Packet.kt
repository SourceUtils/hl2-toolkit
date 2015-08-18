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
        data class Disconnect(val reason: String)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int) = Disconnect(
                reason = bb.getString()
        )
    }

    object net_File : PacketHandler {
        data class File(val transferId: Int, val name: String, val requested: Boolean)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int) = File(
                transferId = bb.getInt(),
                name = bb.getString(),
                requested = bb.getBoolean()
        )
    }

    object net_Tick : PacketHandler {
        data class Tick(val tick: Int, val `host frametime`: Short, val `host frametime stddev`: Short)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int) = Tick(
                tick = bb.getInt(),
                `host frametime` = bb.getShort(),
                `host frametime stddev` = bb.getShort()
        )
    }

    object net_StringCmd : PacketHandler {
        data class StringCmd(val command: String)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int) = StringCmd(
                command = bb.getString()
        )
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
        data class net_SignonState(val state: SignonState?, val spawnCount: Int)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int) = net_SignonState(
                state = SignonState[bb.getUByte()],
                spawnCount = bb.getInt()
        )
    }

    /** 16 in newer protocols */
    object svc_Prval : PacketHandler {
        data class Prval(val value: String)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int) = Prval(
                value = bb.getString()
        )
    }

    object svc_ServerInfo : PacketHandler {
        data class ServerInfo(
                val version: Int,
                val count: Long,
                val isSourceTV: Boolean,
                val isDedicated: Boolean,
                val serverClientCRC: String,
                val maxClasses: Int,
                val mapHash: String,
                val players: Int,
                val maxPlayers: Int,
                val tickInterval: Float,
                val platform: Char,
                val gameDir: String,
                val mapName: String,
                val sky: String,
                val host: String,
                val hasReplay: Boolean
        )

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int): ServerInfo {
            val version = bb.getUShort()
            val count = bb.getUInt()
            val sourceTV = bb.getBoolean()
            val dedicated = bb.getBoolean()
            val serverClientCRC = "0x${Integer.toHexString(bb.getInt())}"
            val maxClasses = bb.getUShort()
            val hash = if (version >= 18) {
                val md5 = ByteArray(16)
                bb.get(md5)
                "%0${md5.size() * 2}x".format(BigInteger(1, md5))
            } else {
                // CRC
                "0x${Integer.toHexString(bb.getInt())}"
            }
            val players = bb.getUByte()
            val maxplayers = bb.getUByte()
            val tickInterval = bb.getFloat()
            val platform = bb.getByte().toChar()
            val gamedir = bb.getString()
            val mapname = bb.getString()
            val sky = bb.getString()
            val host = bb.getString()
            val replay = bb.getBoolean()
            return ServerInfo(
                    version = version,
                    count = count,
                    isSourceTV = sourceTV,
                    isDedicated = dedicated,
                    serverClientCRC = serverClientCRC,
                    maxClasses = maxClasses,
                    mapHash = hash,
                    players = players,
                    maxPlayers = maxplayers,
                    tickInterval = tickInterval,
                    platform = platform,
                    gameDir = gamedir,
                    mapName = mapname,
                    sky = sky,
                    host = host,
                    hasReplay = replay
            )
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
        data class SetPause(val paused: Boolean)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int) = SetPause(
                paused = bb.getBoolean()
        )
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
        data class VoiceInit(val codec: String, val quality: Byte)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int) = VoiceInit(
                codec = bb.getString(),
                quality = bb.getByte()
        )
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
        data class Sounds(val reliable: Boolean,
                          val count: Int,
                          /** bits */
                          val length: Int)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int): Sounds {
            val reliable = bb.getBoolean()
            val count = if (reliable) 1 else bb.getUByte()
            val length = if (reliable) bb.getByte().toUnsigned() else bb.getShort().toUnsigned()
            bb.getBits(length) // Skip
            return Sounds(reliable, count, length)
        }
    }

    object svc_SetView : PacketHandler {
        data class SetView(val entityIndex: Long)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int) = SetView(
                bb.getBits(11)
        )
    }

    object svc_FixAngle : PacketHandler {
        data class FixAngle(val relative: Boolean, val vector: Vector3f)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int) = FixAngle(
                relative = bb.getBoolean(),
                vector = Vector3f(readBitAngle(bb, 16), readBitAngle(bb, 16), readBitAngle(bb, 16))
        )
    }

    object svc_CrosshairAngle : PacketHandler {
        data class CrosshairAngle(val vector: Vector3f)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int) = CrosshairAngle(
                Vector3f(readBitAngle(bb, 16), readBitAngle(bb, 16), readBitAngle(bb, 16))
        )
    }

    object svc_BSPDecal : PacketHandler {
        data class BSPDecal(
                val position: Vector3f,
                /** decal texture index */
                val index: Long,
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
        data class EntityMessage(val index: Long, val classId: Long, val length: Int)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int): EntityMessage {
            val index = bb.getBits(11)
            val classId = bb.getBits(9)
            val length = bb.getBits(11).toInt()
            bb.getBits(length) // Skip
            return EntityMessage(index, classId, length)
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
        data class TempEntities(val count: Long, val length: Long)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int): TempEntities {
            val numEntries = bb.getBits(HL2DEM.EVENT_INDEX_BITS)
            val length = bb.getBits(HL2DEM.NET_MAX_PALYLOAD_BITS)
            // FIXME: underflows, but is usually last
            bb.getBits(length.toInt()) // Skip
            return TempEntities(numEntries, length)
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
        data class Prefetch(val index: Long)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int): Prefetch {
            val bits = when {
                demo.header.networkProtocol >= 23 -> HL2DEM.MAX_SOUND_INDEX_BITS
                else -> 13
            }
            return Prefetch(bb.getBits(bits))
        }
    }

    /** TODO */
    object svc_Menu : PacketHandler {
        data class Menu(val type: Short, val length: Int)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int): Menu {
            val type = bb.getShort()
            val length = bb.getUShort()
            bb.getBits(length * 8) // Skip
            return Menu(type, length)
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
        data class GetCvarValue(val cookie: Int, val value: String)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int) = GetCvarValue(
                bb.getInt(),
                bb.getString()
        )
    }

    /** TODO */
    object svc_CmdKeyValues : PacketHandler {
        data class CmdKeyValues(val length: Int)

        override fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int): CmdKeyValues {
            val length = bb.getInt()
            bb.getBits(length) // Skip
            return CmdKeyValues(length)
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
