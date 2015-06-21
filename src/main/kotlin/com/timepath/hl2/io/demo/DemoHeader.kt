package com.timepath.hl2.io.demo

import com.timepath.DataUtils
import com.timepath.Logger
import com.timepath.io.struct.StructField
import java.nio.ByteBuffer
import java.util.logging.Level
import kotlin.properties.Delegates

public class DemoHeader {
    StructField(index = 0, limit = 8)
    public var head: String by Delegates.notNull()
    StructField(index = 1)
    public var demoProtocol: Int = 0
    StructField(index = 2)
    public var networkProtocol: Int = 0
    StructField(index = 3, limit = MAX_OSPATH)
    public var serverName: String by Delegates.notNull()
    StructField(index = 4, limit = MAX_OSPATH)
    public var clientName: String by Delegates.notNull()
    StructField(index = 5, limit = MAX_OSPATH)
    public var mapName: String by Delegates.notNull()
    StructField(index = 6, limit = MAX_OSPATH)
    public var gameDirectory: String by Delegates.notNull()
    StructField(index = 7)
    public var playbackTime: Float = 0f
    StructField(index = 8)
    public var ticks: Int = 0
    StructField(index = 9)
    public var frames: Int = 0
    StructField(index = 10)
    public var signonLength: Int = 0

    companion object {

        private val LOG = Logger()
        private val MAX_OSPATH = 260

        fun parse(slice: ByteBuffer): DemoHeader {
            val h = DemoHeader()
            h.head = DataUtils.getText(DataUtils.getSlice(slice, 8))
            if (HL2DEM.HEADER != h.head) {
                LOG.log(Level.WARNING, { "Unexpected header" })
            }
            h.demoProtocol = slice.getInt()
            if (h.demoProtocol != HL2DEM.DEMO_PROTOCOL) {
                LOG.log(Level.WARNING, { "Unknown demo version ${h.demoProtocol}" })
            }
            h.networkProtocol = slice.getInt()
            LOG.info({ "Network protocol: ${h.networkProtocol}" })
            val serverNameBuffer = DataUtils.getSlice(slice, MAX_OSPATH)
            h.serverName = DataUtils.getText(serverNameBuffer, true)
            LOG.info({ "Server: ${h.serverName}" })
            val clientNameBuffer = DataUtils.getSlice(slice, MAX_OSPATH)
            h.clientName = DataUtils.getText(clientNameBuffer, true)
            LOG.info({ "Client: ${h.clientName}" })
            val mapNameBuffer = DataUtils.getSlice(slice, MAX_OSPATH)
            h.mapName = DataUtils.getText(mapNameBuffer, true)
            LOG.info({ "Map: ${h.mapName}" })
            val gameDirectoryBuffer = DataUtils.getSlice(slice, MAX_OSPATH)
            h.gameDirectory = DataUtils.getText(gameDirectoryBuffer, true)
            LOG.info({ "Game: ${h.gameDirectory}" })
            h.playbackTime = slice.getFloat()
            LOG.info({ "Playback time: ${h.playbackTime}" })
            h.ticks = slice.getInt()
            LOG.info({ "Ticks: ${h.ticks}" })
            h.frames = slice.getInt()
            LOG.info({ "Frames: ${h.frames}" })
            h.signonLength = slice.getInt()
            LOG.info({ "Signon length: ${h.signonLength}" })
            return h
        }
    }
}
