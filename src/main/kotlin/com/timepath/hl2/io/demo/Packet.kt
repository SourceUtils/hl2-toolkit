package com.timepath.hl2.io.demo

import com.timepath.io.BitBuffer
import com.timepath.hl2.io.util.Vector3f
import java.util.LinkedList
import com.timepath
import java.text.MessageFormat
import java.math.BigInteger

/**
 * @author TimePath
 */
public class Packet(public val type: Packet.Type, public val offset: Int) {

    public val list: MutableList<Pair<Any, Any>> = LinkedList()

    override fun toString(): String {
        return MessageFormat.format("{0}, offset {1}", type, offset)
    }

    /**
     * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.h
     */
    public enum class Type(
            /**
             * The opcode for this packet
             */
            private val id: Int) : PacketHandler {
        net_NOP : Type(0) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                return true
            }
        }
        net_Disconnect : Type(1) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(Pair<Any, Any>("Reason", bb.getString()))
                return true
            }
        }
        net_File : Type(2) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(Pair<Any, Any>("Transfer ID", bb.getInt()))
                l.add(Pair<Any, Any>("Filename", bb.getString()))
                l.add(Pair<Any, Any>("Requested", bb.getBoolean()))
                return true
            }
        }
        net_Tick : Type(3) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(Pair<Any, Any>("Tick", bb.getInt()))
                l.add(Pair<Any, Any>("Host frametime", bb.getShort()))
                l.add(Pair<Any, Any>("Host frametime StdDev", bb.getShort()))
                return true
            }
        }
        net_StringCmd : Type(4) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(Pair<Any, Any>("Command", bb.getString()))
                return true
            }
        }
        net_SetConVar : Type(5) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val n = bb.getByte()
                n.toInt().times {
                    l.add(Pair<Any, Any>(bb.getString(), bb.getString()))
                }
                return true
            }
        }
        net_SignonState : Type(6) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val state = bb.getByte().toInt() and 0xFF
                val signon = SignonState.values()
                l.add(Pair<Any, Any>("Signon state", if (state < signon.size()) signon[state] else state))
                l.add(Pair<Any, Any>("Spawn count", bb.getBits(32)))
                return true
            }
        }
        /**
         * 16 in newer protocols
         */
        svc_Prval : Type(7) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(Pair<Any, Any>("Value", bb.getString()))
                return true
            }
        }
        svc_ServerInfo : Type(8) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val version = bb.getBits(16)
                l.add(Pair<Any, Any>("Version", version))
                l.add(Pair<Any, Any>("Server count", bb.getBits(32)))
                l.add(Pair<Any, Any>("SourceTV", bb.getBoolean()))
                l.add(Pair<Any, Any>("Dedicated", bb.getBoolean()))
                l.add(Pair<Any, Any>("Server client CRC", "0x${Integer.toHexString(bb.getInt())}"))
                l.add(Pair<Any, Any>("Max classes", bb.getBits(16)))
                if (version >= 18) {
                    val md5 = ByteArray(16)
                    bb.get(md5)
                    l.add(Pair<Any, Any>("Server map MD5",
                            java.lang.String.format("%0${md5.size() * 2}x",
                                    BigInteger(1, md5))))
                } else {
                    l.add(Pair<Any, Any>("Server map CRC", "0x${Integer.toHexString(bb.getInt())}"))
                }
                l.add(Pair<Any, Any>("Current player count", bb.getBits(8)))
                l.add(Pair<Any, Any>("Max player count", bb.getBits(8)))
                l.add(Pair<Any, Any>("Interval per tick", bb.getFloat()))
                l.add(Pair<Any, Any>("Platform", bb.getBits(8).toChar()))
                l.add(Pair<Any, Any>("Game directory", bb.getString()))
                l.add(Pair<Any, Any>("Map name", bb.getString()))
                l.add(Pair<Any, Any>("Skybox name", bb.getString()))
                l.add(Pair<Any, Any>("Hostname", bb.getString()))
                l.add(Pair<Any, Any>("Has replay", bb.getBoolean())) // ???: protocol version
                return true
            }
        }
        /**
         * TODO
         */
        svc_SendTable : Type(9) {
        }
        svc_ClassInfo : Type(10) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val n = bb.getShort().toInt()
                l.add(Pair<Any, Any>("Number of server classes", n))
                val cc = bb.getBoolean()
                l.add(Pair<Any, Any>("Create classes on client", cc))
                demo.serverClassBits = log2(n) + 1
                l.add(Pair<Any, Any>("serverClassBits", demo.serverClassBits))
                if (!cc) {
                    n.times {
                        l.add(Pair<Any, Any>("Class ID", bb.getBits(demo.serverClassBits)))
                        l.add(Pair<Any, Any>("Class name", bb.getString()))
                        l.add(Pair<Any, Any>("Datatable name", bb.getString()))
                    }
                }
                return true
            }
        }
        svc_SetPause : Type(11) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(Pair<Any, Any>("Paused", bb.getBoolean()))
                return true
            }
        }
        /**
         * TODO
         * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.cpp#L898
         */
        svc_CreateStringTable : Type(12) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val tableName = bb.getString()
                l.add(Pair<Any, Any>("Table name", tableName))
                val maxEntries = bb.getShort().toInt()
                l.add(Pair<Any, Any>("Max entries", maxEntries))
                val entryBits = log2(maxEntries)
                val numEntries = bb.getBits(entryBits + 1)
                l.add(Pair<Any, Any>("Number of entries", numEntries))
                val length = bb.getBits(HL2DEM.NET_MAX_PALYLOAD_BITS + 3)
                l.add(Pair<Any, Any>("Length in bits", length))
                val userDataFixedSize = bb.getBoolean()
                l.add(Pair<Any, Any>("Userdata fixed size", userDataFixedSize))
                var userDataSize = -1
                var userDataSizeBits = -1
                if (userDataFixedSize) {
                    userDataSize = bb.getBits(12).toInt()
                    l.add(Pair<Any, Any>("Userdata size", userDataSize))
                    userDataSizeBits = bb.getBits(4).toInt()
                    l.add(Pair<Any, Any>("Userdata bits", userDataSizeBits))
                }
                StringTable.create(tableName, maxEntries.toInt(), entryBits, userDataFixedSize, userDataSize, userDataSizeBits)
                //                .parse(bb, l)

                bb.getBits(length.toInt()) // Skip
                return true
            }
        }
        /**
         * TODO
         * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.cpp#L837
         */
        svc_UpdateStringTable : Type(13) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val tableID = bb.getBits(log2(StringTable.MAX_TABLES)) // 5 bits
                l.add(Pair<Any, Any>("Table ID", tableID))
                val changedEntries = if (bb.getBoolean()) bb.getBits(16) else 1
                l.add(Pair<Any, Any>("Changed entries", changedEntries))
                val length = bb.getBits(20)
                l.add(Pair<Any, Any>("Length in bits", length))
                StringTable[tableID.toInt()]
                //                .parse(bb, l)

                bb.getBits(length.toInt()) // Skip
                return true
            }
        }
        svc_VoiceInit : Type(14) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(Pair<Any, Any>("Codec", bb.getString()))
                l.add(Pair<Any, Any>("Quality", bb.getByte()))
                return true
            }
        }
        svc_VoiceData : Type(15) {
            val s = VoiceDataHandler()
            override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                return s.read(bb, l, demo, lengthBits)
            }
        }
        /**
         * TODO: One of these
         * svc_HLTV: HLTV control messages
         * svc_Print: split screen style message
         */
        svc_Unknown16 : Type(16) {
        }
        /**
         * TODO
         */
        svc_Sounds : Type(17) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val reliable = bb.getBoolean()
                l.add(Pair<Any, Any>("Reliable", reliable))
                val count = if (reliable) 1 else bb.getByte()
                l.add(Pair<Any, Any>("Number of sounds", count))
                val length = if (reliable) bb.getByte().toInt() else bb.getShort().toInt()
                l.add(Pair<Any, Any>("Length in bits", length))
                bb.getBits(length) // Skip
                return true
            }
        }
        svc_SetView : Type(18) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(Pair<Any, Any>("Entity index", bb.getBits(11)))
                return true
            }
        }
        svc_FixAngle : Type(19) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(Pair<Any, Any>("Relative", bb.getBoolean()))
                val v = Vector3f(readBitAngle(bb, 16), readBitAngle(bb, 16), readBitAngle(bb, 16))
                l.add(Pair<Any, Any>("Vector", v))
                return true
            }

            fun readBitAngle(bb: BitBuffer, numbits: Int): Float {
                return bb.getBits(numbits) * (360.0f / (1 shl numbits))
            }
        }
        svc_CrosshairAngle : Type(20) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val v = Vector3f(readBitAngle(bb, 16), readBitAngle(bb, 16), readBitAngle(bb, 16))
                l.add(Pair<Any, Any>("Vector", v))
                return true
            }

            fun readBitAngle(bb: BitBuffer, numbits: Int): Float {
                return bb.getBits(numbits) * (360.0f / (1 shl numbits))
            }
        }
        svc_BSPDecal : Type(21) {
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

            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(Pair<Any, Any>("Position", getVecCoord(bb)))
                l.add(Pair<Any, Any>("Decal texture index", bb.getBits(HL2DEM.MAX_DECAL_INDEX_BITS)))
                if (bb.getBoolean()) {
                    l.add(Pair<Any, Any>("Entity index", bb.getBits(HL2DEM.MAX_EDICT_BITS)))
                    var bits = HL2DEM.SP_MODEL_INDEX_BITS
                    if (demo.header.demoProtocol <= 21) {
                        bits--
                    }
                    l.add(Pair<Any, Any>("Model index", bb.getBits(bits)))
                }
                l.add(Pair<Any, Any>("Low priority", bb.getBoolean()))
                return true
            }
        }
        /**
         * TODO: One of these
         * svc_TerrainMod: modification to the terrain/displacement
         * svc_SplitScreen: split screen style message
         */
        svc_unknown2 : Type(22) {
        }
        /**
         * TODO
         */
        svc_UserMessage : Type(23) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                return UserMessage.read(bb, l, demo)
            }
        }
        /**
         * TODO
         */
        svc_EntityMessage : Type(24) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(Pair<Any, Any>("Entity index", bb.getBits(11)))
                l.add(Pair<Any, Any>("Class ID", bb.getBits(9)))
                val length = bb.getBits(11).toInt()
                l.add(Pair<Any, Any>("Length in bits", length))
                bb.getBits(length) // Skip
                return true
            }
        }
        svc_GameEvent : Type(25) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val length = bb.getBits(11).toInt()
                l.add(Pair<Any, Any>("Length in bits", length))
                val gameEventId = bb.getBits(9).toInt()
                val gameEvent = demo.gameEvents[gameEventId]
                if (gameEvent != null) {
                    l.add(Pair<Any, Any>(gameEvent.name, gameEvent.parse(bb).entrySet()))
                } else {
                    l.add(Pair<Any, Any>("Unknown event", gameEventId))
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
         * @see <a>https://code.google.com/p/coldemoplayer/source/browse/branches/2.0/compLexity+Demo+Player/CDP
         * .HalfLifeDemo/Messages/SvcPacketEntities.cs?r=59#43<a/>
         * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/packed_entity.h#L52<a/>
         * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/sv_ents_write.cpp#L862<a/>
         */
        svc_PacketEntities : Type(26) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val MAX_EDICT_BITS = 11
                val DELTASIZE_BITS = 20
                val maxEntries = bb.getBits(MAX_EDICT_BITS)
                l.add(Pair<Any, Any>("Max entries", maxEntries))
                val isDelta = bb.getBoolean()
                l.add(Pair<Any, Any>("Is delta", isDelta))
                if (isDelta) {
                    val deltaFrom = bb.getBits(32).toInt()
                    l.add(Pair<Any, Any>("Delta from", deltaFrom))
                }
                val baseline = bb.getBoolean()
                l.add(Pair<Any, Any>("Baseline", baseline))
                val updatedEntries = bb.getBits(MAX_EDICT_BITS)
                l.add(Pair<Any, Any>("Updated entries", updatedEntries))
                val length = bb.getBits(DELTASIZE_BITS)
                l.add(Pair<Any, Any>("Length in bits", length))
                val updateBaseline = bb.getBoolean()
                l.add(Pair<Any, Any>("Update baseline", updateBaseline))
                bb.getBits(length.toInt()) // Skip
                return true
            }
        }
        /**
         * TODO
         * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/servermsghandler.cpp#L738
         */
        svc_TempEntities : Type(27) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val numEntries = bb.getBits(HL2DEM.EVENT_INDEX_BITS)
                l.add(Pair<Any, Any>("Number of entries", numEntries))
                val length = bb.getBits(HL2DEM.NET_MAX_PALYLOAD_BITS)
                l.add(Pair<Any, Any>("Length in bits", length))
                // FIXME: underflows, but is usually last
                bb.getBits(length.toInt()) // Skip
                return true
                /*
            if(numEntries == 0) {
                val reliable = true
                numEntries = 1
            }
            val classID = -1
            l.add(Pair<Any, Any>("serverClassBits", demo.serverClassBits))
            for(val i = 0 i < numEntries i++) {
                float delay = 0
                if(bb.getBoolean()) {
                    delay = bb.getBits(8) / 100.0f
                }
                l.add(Pair<Any, Any>("ent[" + i + "].delay", delay))
                if(bb.getBoolean()) { // Full update
                    classID = (int) bb.getBits(demo.serverClassBits)
                }
                l.add(Pair<Any, Any>("ent[" + i + "].classID", classID))
            }
            return false
            */
            }
        }
        svc_Prefetch : Type(28) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val bits = if (demo.header.networkProtocol >= 23) HL2DEM.MAX_SOUND_INDEX_BITS else 13
                l.add(Pair<Any, Any>("Sound index", bb.getBits(bits)))
                return true
            }
        }
        /**
         * TODO
         */
        svc_Menu : Type(29) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(Pair<Any, Any>("Menu type", bb.getBits(16)))
                val length = bb.getBits(16).toInt()
                l.add(Pair<Any, Any>("Length in bytes", length))
                bb.getBits(length * 8) // Skip
                return true
            }
        }
        svc_GameEventList : Type(30) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val numGameEvents = bb.getBits(9).toInt()
                demo.gameEvents = arrayOfNulls<GameEvent>(HL2DEM.MAX_GAME_EVENTS)
                l.add(Pair<Any, Any>("Number of events", numGameEvents))
                val length = bb.getBits(20)
                l.add(Pair<Any, Any>("Length in bits", length))
                numGameEvents.times {
                    val id = bb.getBits(9).toInt()
                    val gameEvent = GameEvent(bb)
                    demo.gameEvents[id] = gameEvent
                    l.add(Pair<Any, Any>("gameEvents[$id] = ${gameEvent.name}",
                            gameEvent.declarations.entrySet()))
                }
                return true
            }
        }
        svc_GetCvarValue : Type(31) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                l.add(Pair<Any, Any>("Cookie", "0x${Integer.toHexString(bb.getInt())}"))
                l.add(Pair<Any, Any>("value", bb.getString()))
                return true
            }
        }
        /**
         * TODO
         */
        svc_CmdKeyValues : Type(32) {
            override
            fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
                val length = bb.getInt()
                l.add(Pair<Any, Any>("Length in bits: ", length))
                bb.getBits(length) // Skip
                return true
            }
        }

        /**
         * The handler associated with this packet
         */
        val handler: PacketHandler

        init {
            this.handler = this
        }

        companion object {

            public fun get(i: Int): Type? {
                for (t in Type.values()) {
                    if (t.id == i) {
                        return t
                    }
                }
                return null
            }

            private fun log2(i: Int): Int {
                return (Math.log(i.toDouble()) / Math.log(2.0)).toInt()
            }
        }
    }
}
