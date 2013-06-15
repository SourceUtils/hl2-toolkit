package com.timepath.hl2.io;

import com.timepath.crypto.IceKey;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Thin-ICE encrypted files. Often VDF
 * @author timepath
 */
public class CTX {

    public static final String key = "E2NcUkG2";
    public static final String key_items = "A5fSXbf7";
    public static final String key_default = "x9Ke0BY7";

    public static void main(String[] args) {
        try {
            InputStream is = new FileInputStream(args[0]);
            InputStream de = CTX.decrypt(is);
            InputStream is2 = CTX.encrypt(de);
            is = new FileInputStream(args[0]);
            de = CTX.decrypt(is);
            is = new FileInputStream(args[0]);
            
            int bs = 4096;
            
            ByteBuffer debuf = ByteBuffer.allocate(de.available());
            byte[] inde = new byte[bs];
            for (int read = 0; read != -1; read = de.read(inde, 0, bs)) {
                debuf.put(inde, 0, read);
            }
            
            System.out.println(new String(debuf.array()));
            
            ByteBuffer buf = ByteBuffer.allocate(is.available());
            byte[] in = new byte[bs];
            for (int read = 0; read != -1; read = is.read(in, 0, bs)) {
                buf.put(in, 0, read);
            }
            
            ByteBuffer buf2 = ByteBuffer.allocate(is2.available());
            byte[] in2 = new byte[bs];
            for (int read = 0; read != -1; read = is2.read(in2, 0, bs)) {
                buf2.put(in2, 0, read);
            }
            System.out.println("Equal = " + Arrays.equals(buf.array(), buf2.array()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static byte[] method(InputStream is, boolean decrypt) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(is.available());
        IceKey ice = new IceKey(0);
        
        ice.set(key.getBytes());

        final int bs = 8; // ice.blockSize();
        byte[] in = new byte[bs];
        byte[] out = new byte[bs];
        int prevRead = 0;
        for (int read = 0; read != -1; read = is.read(in, 0, bs)) {
            prevRead = read;
//            if (read != bs) {
//                if (read == 0) {
////                    System.err.println("read 0 bytes?");
//                } else {
//                    System.err.println("read: " + read);
//                }
//            }
            if (decrypt) {
                ice.decrypt(in, out);
            } else {
                ice.encrypt(in, out);
            }
            buf.put(out, 0, read);
        }
        // last block is not encrypted
        buf.position(buf.limit() - prevRead);
        buf.put(in, 0, prevRead);
        
        byte[] arr = buf.array();
        return arr;
    }

    public static InputStream encrypt(InputStream is) throws IOException {
        return new ByteArrayInputStream(method(is, false));
    }

    public static InputStream decrypt(InputStream is) throws IOException {
        return new ByteArrayInputStream(method(is, true));
    }
}