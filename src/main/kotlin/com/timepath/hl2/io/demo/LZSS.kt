package com.timepath.hl2.io.demo

import org.apache.commons.io.EndianUtils

public class LZSS {

    class LZSSException(message: String) : Exception(message)

    class object {

        public val ID: String = "LZSS"
        private val LZSS_LOOKSHIFT = 4

        throws(javaClass<LZSSException>())
        public fun inflate(input: ByteArray): ByteArray {
            var pInput = 8
            var pOutput = 0 // Pointers
            // Header
            val id = String(input, 0, 4)
            val actualSize = EndianUtils.readSwappedInteger(input, 4)
            if (ID != id || actualSize == 0) {
                throw LZSSException("Unrecognized header")
            }
            // Payload
            val output = ByteArray(actualSize)
            var totalBytes = 0
            var cmdByte = 0
            var getCmdByte = 0
            while (true) {
                if (getCmdByte == 0) {
                    cmdByte = input[pInput++].toInt()
                }
                getCmdByte = (getCmdByte + 1) and 7
                if ((cmdByte and 1) == 1) {
                    val position = (input[pInput++].toInt() shl LZSS_LOOKSHIFT) or (input[pInput].toInt() shr LZSS_LOOKSHIFT)
                    val count = (input[pInput++].toInt() and 15) + 1
                    if (count == 1) {
                        break
                    }
                    var pSource = pOutput - position - 1
                    for (i in 0..count - 1) {
                        output[pOutput++] = output[pSource++]
                    }
                    totalBytes += count
                } else {
                    output[pOutput++] = input[pInput++]
                    totalBytes++
                }
                cmdByte = cmdByte shr 1
            }
            // Verify
            if (totalBytes != actualSize) {
                throw LZSSException("Unexpected failure: bytes read do not match expected size")
            }
            return output
        }
    }
}
