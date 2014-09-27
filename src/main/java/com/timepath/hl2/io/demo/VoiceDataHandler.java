package com.timepath.hl2.io.demo;

import com.timepath.Pair;
import com.timepath.io.BitBuffer;
import org.jetbrains.annotations.NotNull;
import org.xiph.speex.SpeexDecoder;

import javax.sound.sampled.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 * @see <a>http://hg.limetech.org/java/DemoReader/file/2771d28988dc/src/org/limetech/demoreader/Main.java#l127</a>
 * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/se2007/engine/voice_codecs/speex/VoiceEncoder_Speex.cpp</a>
 */
class VoiceDataHandler extends PacketHandler {

    private static final Logger LOG = Logger.getLogger(VoiceDataHandler.class.getName());
    private static final int VOICE_OUTPUT_SAMPLE_RATE = 11025;
    private SourceDataLine audioOut;
    private SpeexDecoder speexDecoder;

    VoiceDataHandler() {
        try {
            speexDecoder = new SpeexDecoder();
            int mode = 1; // Narrow band
            speexDecoder.init(mode, 11025, 1, true);
            // Signed 16 bit LE mono
            @NotNull AudioFormat sourceVoiceFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    VOICE_OUTPUT_SAMPLE_RATE,
                    16,
                    1,
                    2,
                    VOICE_OUTPUT_SAMPLE_RATE,
                    false);
            LOG.log(Level.INFO, "Voice: {0}", sourceVoiceFormat);
            @NotNull DataLine.Info info = new DataLine.Info(SourceDataLine.class, sourceVoiceFormat);
            audioOut = (SourceDataLine) AudioSystem.getLine(info);
            audioOut.open(sourceVoiceFormat);
            audioOut.start();
        } catch (LineUnavailableException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    static int bitsToBytes(int bits) {
        return (bits + 7) / 8;
    }

    static void dump(int index, @NotNull byte... data) {
        try (@NotNull FileOutputStream fos = new FileOutputStream("target/vo_" + index + ".pcm", true)) {
            fos.write(data);
            fos.flush();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    boolean read(@NotNull BitBuffer bb, @NotNull List<Pair<Object, Object>> l, HL2DEM demo) {
        int client = bb.getByte() & 0xFF;
        l.add(new Pair<Object, Object>("Client", client));
        l.add(new Pair<Object, Object>("Proximity", bb.getByte()));
        int length = bb.getShort() & 0xFFFF;
        l.add(new Pair<Object, Object>("Length in bits", length));
        if (length < 0) {
            return false;
        }
        if (length == 0) {
            return true;
        }
        @NotNull byte[] data = new byte[bitsToBytes(length)];
        bb.get(data);
//        speex(client, data);
        return true;
    }

    void speex(int index, @NotNull byte... data) {
        @NotNull byte[] decoded = data;
        try {
            speexDecoder.processData(data, 0, data.length);
            decoded = new byte[speexDecoder.getProcessedDataByteSize()];
            speexDecoder.getProcessedData(decoded, 0);
        } catch (StreamCorruptedException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
//        dump(index, decoded);
        pcm(index, decoded);
    }

    void pcm(int index, @NotNull byte... data) {
        try {
            audioOut.write(data, 0, data.length);
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.SEVERE, null, ex.getMessage());
        }
    }
}
