package com.timepath.hl2.io.demo;

import com.timepath.Pair;
import com.timepath.io.BitBuffer;
import org.xiph.speex.SpeexDecoder;

import javax.sound.sampled.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * http://hg.limetech.org/java/DemoReader/file/2771d28988dc/src/org/limetech/demoreader/Main.java#l127
 *
 * @author TimePath
 */
class VoiceDataHandler extends PacketHandler {

    private static final Logger LOG = Logger.getLogger(VoiceDataHandler.class.getName());
    private SourceDataLine audioOut;
    private SpeexDecoder   speexDecoder;

    VoiceDataHandler() {
        try {
            speexDecoder = new SpeexDecoder();
            int mode = 1; // Narrow band
            speexDecoder.init(mode, 11025, 1, true);
            // Signed 16 bit LE mono
            AudioFormat sourceVoiceFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 11025, 16, 1, 2, 11025, false);
            LOG.log(Level.INFO, "Voice: {0}", sourceVoiceFormat);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, sourceVoiceFormat);
            audioOut = (SourceDataLine) AudioSystem.getLine(info);
            audioOut.open(sourceVoiceFormat);
            audioOut.start();
        } catch(LineUnavailableException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
        int client = bb.getByte() & 0xFF;
        l.add(new Pair<Object, Object>("Client", client));
        l.add(new Pair<Object, Object>("Proximity", bb.getByte()));
        int length = bb.getShort() & 0xFFFF;
        l.add(new Pair<Object, Object>("Length in bits", length));
        if(length < 0) {
            return false;
        }
        if(length == 0) {
            return true;
        }
        byte[] data = new byte[bitsToBytes(length)];
        bb.get(data);
        //        speex(client, data);
        return true;
    }

    static int bitsToBytes(int bits) {
        return ( bits + 7 ) / 8;
    }

    void speex(int index, byte... data) {
        byte[] decoded = data;
        try {
            speexDecoder.processData(data, 0, data.length);
            decoded = new byte[speexDecoder.getProcessedDataByteSize()];
            speexDecoder.getProcessedData(decoded, 0);
        } catch(StreamCorruptedException ex) {
            LOG.log(Level.SEVERE, null, ex.getMessage());
        }
        dump(index, decoded);
        pcm(index, decoded);
    }

    static void dump(int index, byte... data) {
        try(FileOutputStream fos = new FileOutputStream("target/vo_" + index + ".pcm", true)) {
            fos.write(data);
            fos.flush();
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    void pcm(int index, byte... data) {
        try {
            audioOut.write(data, 0, data.length);
        } catch(IllegalArgumentException ex) {
            LOG.log(Level.SEVERE, null, ex.getMessage());
        }
    }
}
