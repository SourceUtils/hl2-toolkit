package com.timepath.hl2.io.demo

import com.timepath.Logger
import com.timepath.io.BitBuffer
import org.xiph.speex.SpeexDecoder
import java.io.FileOutputStream
import java.io.IOException
import java.io.StreamCorruptedException
import java.util.logging.Level
import javax.sound.sampled.*

/**
 * @see <a>http://hg.limetech.org/java/DemoReader/file/2771d28988dc/src/org/limetech/demoreader/Main.java#l127</a>
 * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/se2007/engine/voice_codecs/speex/VoiceEncoder_Speex.cpp</a>
 */
class VoiceDataHandler : PacketHandler {
    private var audioOut: SourceDataLine? = null
    private var speexDecoder: SpeexDecoder? = null

    init {
        try {
            // Disabled for now
            // speexDecoder = SpeexDecoder()
            val mode = 1 // Narrow band
            speexDecoder?.init(mode, 11025, 1, true)
            // Signed 16 bit LE mono
            val sourceVoiceFormat = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, VOICE_OUTPUT_SAMPLE_RATE.toFloat(), 16, 1, 2, VOICE_OUTPUT_SAMPLE_RATE.toFloat(), false)
            LOG.info({ "Voice: ${sourceVoiceFormat}" })
            val info = DataLine.Info(javaClass<SourceDataLine>(), sourceVoiceFormat)
            audioOut = AudioSystem.getLine(info) as SourceDataLine
            audioOut?.open(sourceVoiceFormat)
            audioOut?.start()
        } catch (ex: Exception) {
            when (ex) {
                is LineUnavailableException, is IllegalArgumentException -> {
                    LOG.log(Level.SEVERE, { null }, ex)
                }
                else -> throw ex
            }
        }
    }

    override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
        val client = bb.getByte().toInt() and 0xFF
        l.add(("Client" to client))
        l.add(("Proximity" to bb.getByte()))
        val length = bb.getShort().toInt() and 0xFFFF
        l.add(("Length in bits" to length))
        if (length < 0) {
            return false
        }
        if (length == 0) {
            return true
        }
        val data = ByteArray(bitsToBytes(length))
        bb.get(data)
        // speex(client, data);
        return true
    }

    fun speex(index: Int, vararg data: Byte) {
        var decoded = data
        try {
            speexDecoder!!.processData(data, 0, data.size())
            decoded = ByteArray(speexDecoder!!.getProcessedDataByteSize())
            speexDecoder!!.getProcessedData(decoded, 0)
        } catch (ex: StreamCorruptedException) {
            LOG.log(Level.SEVERE, { null }, ex)
        }

        // dump(index, decoded);
        pcm(index, *decoded)
    }

    fun pcm(index: Int, vararg data: Byte) {
        try {
            audioOut?.write(data, 0, data.size())
        } catch (ex: IllegalArgumentException) {
            LOG.log(Level.SEVERE, { ex.getMessage()!! })
        }

    }

    companion object {

        private val LOG = Logger()
        private val VOICE_OUTPUT_SAMPLE_RATE = 11025

        fun bitsToBytes(bits: Int) = (bits + 7) / 8

        fun dump(index: Int, vararg data: Byte) = try {
            FileOutputStream("target/vo_$index.pcm", true).use {
                it.write(data)
                it.flush()
            }
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, { null }, ex)
        }
    }
}
