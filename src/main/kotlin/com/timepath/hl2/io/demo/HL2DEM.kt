package com.timepath.hl2.io.demo

import com.timepath.DataUtils
import com.timepath.Logger
import java.io.File
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.logging.Level
import kotlin.properties.Delegates

/**
 * Format of a demo:
 * HL2DEM {
 * DemoHeader,
 * Message {
 * Packet
 * ...
 * }
 * ...
 * }
 *
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/common/proto_version.h</a>
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/demofile/demoformat.h</a>
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/networkstringtabledefs.h</a>
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/keyvaluescompiler.h</a>
 * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.cpp</a>
 * @see <a>https://github.com/jpcy/coldemoplayer</a>
 * @see <a>https://github.com/stgn/netdecode</a>
 * @see <a>https://github.com/tritao/netdecode</a>
 * @see <a>https://github.com/jpcy/coldemoplayer/blob/master/compLexity%20Demo%20Player/demo/SourceDemo.cs</a>
 * @see <a>https://github.com/jpcy/coldemoplayer/blob/master/compLexity%20Demo%20Player/demo%20parser/SourceDemoParser.cs</a>
 * @see <a>https://github.com/tritao/netdecode/blob/master/DemoFile.cs</a>
 * @see <a>https://github.com/tritao/netdecode/blob/master/Packet.cs</a>
 * @see <a>https://forums.alliedmods.net/showthread.php?t=232925</a>
 * @see <a>http://demos.geit.co.uk/</a>
 */
public class HL2DEM private constructor(buffer: ByteBuffer, eager: Boolean) {
    val frames = LinkedList<Message>()
    val gameEvents = arrayOfNulls<GameEvent>(HL2DEM.MAX_GAME_EVENTS)
    public val header: DemoHeader
    var serverClassBits: Int = 0

    init {
        header = DemoHeader.parse(DataUtils.getSlice(buffer, /* Struct.sizeof(DemoHeader) */ 1072))
        while (true) {
            val frame: Message = try {
                Message.parse(this, buffer)
            } catch (e: BufferUnderflowException) {
                LOG.log(Level.WARNING, { "Unexpected end of demo" })
                break
            }

            frames.add(frame)
            if (frame.type == MessageType.Stop) break
            if (frame.size == 0) continue
            val dst = ByteArray(frame.size)
            try {
                buffer.get(dst)
            } catch (e: BufferUnderflowException) {
                LOG.log(Level.SEVERE, { "Unexpected end of message" }, e)
                break
            }

            frame.data = ByteBuffer.wrap(dst).let {
                it.order(ByteOrder.LITTLE_ENDIAN)
                it
            }
            if (eager) frame.parse()
        }
    }

    companion object {

        public val DEMO_PROTOCOL: Int = 3
        public val EVENT_INDEX_BITS: Int = 8
        public val HEADER: String = "HL2DEMO${0.toChar()}"
        public val MAX_DECAL_INDEX_BITS: Int = 9
        public val MAX_EDICT_BITS: Int = 11
        public val MAX_GAME_EVENTS: Int = 1 shl 9
        public val MAX_SOUND_INDEX_BITS: Int = 14
        public val NET_MAX_PALYLOAD_BITS: Int = 17
        public val SP_MODEL_INDEX_BITS: Int = 12
        /** TF2 specific, need enough space for OBJ_LAST items from tf_shareddefs.h */
        public val WEAPON_SUBTYPE_BITS: Int = 6
        private val LOG = Logger()

        public fun load(f: File, eager: Boolean = true): HL2DEM {
            LOG.info({ "Parsing ${f}" })
            val buffer = DataUtils.mapFile(f)
            return HL2DEM(buffer, eager)
        }
    }
}
