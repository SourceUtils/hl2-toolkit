package com.timepath.hl2.io.demo

import com.timepath.DataUtils
import com.timepath.Logger
import com.timepath.io.struct.StructField
import com.timepath.with
import java.nio.ByteBuffer

private val MAX_OSPATH = 260

public class DemoHeader(
        StructField(0, limit = 8) var head: String,
        StructField(1) var demoProtocol: Int,
        StructField(2) var networkProtocol: Int,
        StructField(3, limit = MAX_OSPATH) var serverName: String,
        StructField(4, limit = MAX_OSPATH) var clientName: String,
        StructField(5, limit = MAX_OSPATH) var mapName: String,
        StructField(6, limit = MAX_OSPATH) var gameDirectory: String,
        StructField(7) var playbackTime: Float,
        StructField(8) var ticks: Int,
        StructField(9) var frames: Int,
        StructField(10) var signonLength: Int
) {
    companion object {

        private val LOG = Logger()

        fun parse(slice: ByteBuffer): DemoHeader {
            val head = DataUtils.getText(DataUtils.getSlice(slice, 8))
            if (HL2DEM.HEADER != head) {
                LOG.warning { "Unexpected header" }
            }
            val demoProtocol = slice.getInt()
            if (demoProtocol != HL2DEM.DEMO_PROTOCOL) {
                LOG.warning { "Unknown demo version ${demoProtocol}" }
            }
            val networkProtocol = slice.getInt()
            val serverName = DataUtils.getText(DataUtils.getSlice(slice, MAX_OSPATH), true)
            val clientName = DataUtils.getText(DataUtils.getSlice(slice, MAX_OSPATH), true)
            val mapName = DataUtils.getText(DataUtils.getSlice(slice, MAX_OSPATH), true)
            val gameDirectory = DataUtils.getText(DataUtils.getSlice(slice, MAX_OSPATH), true)
            val playbackTime = slice.getFloat()
            val ticks = slice.getInt()
            val frames = slice.getInt()
            val signonLength = slice.getInt()
            LOG.info { "Network protocol: ${networkProtocol}" }
            LOG.info { "Server: ${serverName}" }
            LOG.info { "Client: ${clientName}" }
            LOG.info { "Map: ${mapName}" }
            LOG.info { "Game: ${gameDirectory}" }
            LOG.info { "Playback time: ${playbackTime}" }
            LOG.info { "Ticks: ${ticks}" }
            LOG.info { "Frames: ${frames}" }
            LOG.info { "Signon length: ${signonLength}" }
            return DemoHeader(
                    head = head,
                    demoProtocol = demoProtocol,
                    networkProtocol = networkProtocol,
                    serverName = serverName,
                    clientName = clientName,
                    mapName = mapName,
                    gameDirectory = gameDirectory,
                    playbackTime = playbackTime,
                    ticks = ticks,
                    frames = frames,
                    signonLength = signonLength
            )
        }
    }
}
