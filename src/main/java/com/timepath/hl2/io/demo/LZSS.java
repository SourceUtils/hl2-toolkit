package com.timepath.hl2.io.demo;

import org.apache.commons.io.EndianUtils;

public class LZSS {

    public static final String ID = "LZSS";
    private static final int LZSS_LOOKSHIFT = 4;

    public static byte[] inflate(byte[] input) throws LZSSException {
        int pInput = 8, pOutput = 0; // Pointers
        // Header
        String id = new String(input, 0, 4);
        int actualSize = EndianUtils.readSwappedInteger(input, 4);
        if (!ID.equals(id) || actualSize == 0) {
            throw new LZSSException("Unrecognized header");
        }
        // Payload
        byte[] output = new byte[actualSize];
        int totalBytes = 0, cmdByte = 0, getCmdByte = 0;
        for (; ; ) {
            if (getCmdByte == 0) {
                cmdByte = input[pInput++];
            }
            getCmdByte = (getCmdByte + 1) & 0x07;
            if ((cmdByte & 0x01) == 0x01) {
                int position = (input[pInput++] << LZSS_LOOKSHIFT) | (input[pInput] >> LZSS_LOOKSHIFT);
                int count = (input[pInput++] & 0x0F) + 1;
                if (count == 1) {
                    break;
                }
                int pSource = pOutput - position - 1;
                for (int i = 0; i < count; i++) {
                    output[pOutput++] = output[pSource++];
                }
                totalBytes += count;
            } else {
                output[pOutput++] = input[pInput++];
                totalBytes++;
            }
            cmdByte >>= 1;
        }
        // Verify
        if (totalBytes != actualSize) {
            throw new LZSSException("Unexpected failure: bytes read do not match expected size");
        }
        return output;
    }

    static class LZSSException extends Exception {

        public LZSSException(String message) {
            super(message);
        }
    }
}
