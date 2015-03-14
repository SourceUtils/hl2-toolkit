package com.timepath.hl2.io.demo

import com.timepath.Pair
import com.timepath.io.BitBuffer
import org.xiph.speex.SpeexDecoder

import javax.sound.sampled.*
import java.io.FileOutputStream
import java.io.IOException
import java.io.StreamCorruptedException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 * @see <a>http://hg.limetech.org/java/DemoReader/file/2771d28988dc/src/org/limetech/demoreader/Main.java#l127</a>
 * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/se2007/engine/voice_codecs/speex/VoiceEncoder_Speex.cpp</a>
 */
class VoiceDataHandler : PacketHandler {
    private var audioOut: SourceDataLine? = null
    private var speexDecoder: SpeexDecoder? = null

    {
        try {
            speexDecoder = SpeexDecoder()
            val mode = 1 // Narrow band
            speexDecoder!!.init(mode, 11025, 1, true)
            // Signed 16 bit LE mono
            val sourceVoiceFormat = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, VOICE_OUTPUT_SAMPLE_RATE.toFloat(), 16, 1, 2, VOICE_OUTPUT_SAMPLE_RATE.toFloat(), false)
            LOG.log(Level.INFO, "Voice: {0}", sourceVoiceFormat)
            val info = DataLine.Info(javaClass<SourceDataLine>(), sourceVoiceFormat)
            audioOut = AudioSystem.getLine(info) as SourceDataLine
            audioOut!!.open(sourceVoiceFormat)
            audioOut!!.start()
        } catch (ex: LineUnavailableException) {
            LOG.log(Level.SEVERE, null, ex)
        }
    }

    override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
        val client = bb.getByte().toInt() and 255
        l.add(Pair<Any, Any>("Client", client))
        l.add(Pair<Any, Any>("Proximity", bb.getByte()))
        val length = bb.getShort().toInt() and 65535
        l.add(Pair<Any, Any>("Length in bits", length))
        if (length < 0) {
            return false
        }
        if (length == 0) {
            return true
        }
        val data = ByteArray(bitsToBytes(length))
        bb.get(data)
        //        speex(client, data);
        return true
    }

    fun speex(index: Int, vararg data: Byte) {
        var decoded = data
        try {
            speexDecoder!!.processData(data, 0, data.size)
            decoded = ByteArray(speexDecoder!!.getProcessedDataByteSize())
            speexDecoder!!.getProcessedData(decoded, 0)
        } catch (ex: StreamCorruptedException) {
            LOG.log(Level.SEVERE, null, ex)
        }

        //        dump(index, decoded);
        pcm(index, *decoded)
    }

    fun pcm(index: Int, vararg data: Byte) {
        try {
            audioOut!!.write(data, 0, data.size)
        } catch (ex: IllegalArgumentException) {
            LOG.log(Level.SEVERE, null, ex.getMessage())
        }

    }

    class object {

        private val LOG = Logger.getLogger(javaClass<VoiceDataHandler>().getName())
        private val VOICE_OUTPUT_SAMPLE_RATE = 11025

        fun bitsToBytes(bits: Int): Int {
            return (bits + 7) / 8
        }

        fun dump(index: Int, vararg data: Byte) {
            try {
                FileOutputStream("target/vo_" + index + ".pcm", true).use { fos ->
                    fos.write(data)
                    fos.flush()
                }
            } catch (ex: IOException) {
                LOG.log(Level.SEVERE, null, ex)
            }

        }
    }
}