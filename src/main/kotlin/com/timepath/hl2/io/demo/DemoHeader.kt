package com.timepath.hl2.io.demo

import com.timepath.DataUtils
import com.timepath.io.struct.StructField

import java.nio.ByteBuffer
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.properties.Delegates

/**
 * @author TimePath
 */
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

    class object {

        private val LOG = Logger.getLogger(javaClass<DemoHeader>().getName())
        private val MAX_OSPATH = 260

        fun parse(slice: ByteBuffer): DemoHeader {
            val h = DemoHeader()
            h.head = DataUtils.getText(DataUtils.getSlice(slice, 8))
            if (HL2DEM.HEADER != h.head) {
                LOG.log(Level.WARNING, "Unexpected header")
            }
            h.demoProtocol = slice.getInt()
            if (h.demoProtocol != HL2DEM.DEMO_PROTOCOL) {
                LOG.log(Level.WARNING, "Unknown demo version {0}", h.demoProtocol)
            }
            h.networkProtocol = slice.getInt()
            LOG.log(Level.INFO, "Network protocol: {0}", h.networkProtocol)
            val serverNameBuffer = DataUtils.getSlice(slice, MAX_OSPATH)
            h.serverName = DataUtils.getText(serverNameBuffer, true)
            LOG.log(Level.INFO, "Server: {0}", h.serverName)
            val clientNameBuffer = DataUtils.getSlice(slice, MAX_OSPATH)
            h.clientName = DataUtils.getText(clientNameBuffer, true)
            LOG.log(Level.INFO, "Client: {0}", h.clientName)
            val mapNameBuffer = DataUtils.getSlice(slice, MAX_OSPATH)
            h.mapName = DataUtils.getText(mapNameBuffer, true)
            LOG.log(Level.INFO, "Map: {0}", h.mapName)
            val gameDirectoryBuffer = DataUtils.getSlice(slice, MAX_OSPATH)
            h.gameDirectory = DataUtils.getText(gameDirectoryBuffer, true)
            LOG.log(Level.INFO, "Game: {0}", h.gameDirectory)
            h.playbackTime = slice.getFloat()
            LOG.log(Level.INFO, "Playback time: {0}", h.playbackTime)
            h.ticks = slice.getInt()
            LOG.log(Level.INFO, "Ticks: {0}", h.ticks)
            h.frames = slice.getInt()
            LOG.log(Level.INFO, "Frames: {0}", h.frames)
            h.signonLength = slice.getInt()
            LOG.log(Level.INFO, "Signon length: {0}", h.signonLength)
            return h
        }
    }
}
