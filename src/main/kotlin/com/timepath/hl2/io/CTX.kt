package com.timepath.hl2.io

import com.timepath.Logger
import com.timepath.crypto.IceKey
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.Arrays
import kotlin.platform.platformStatic

/**
 * Thin-ICE encrypted files. Often VDF
 */
public class CTX private constructor() {
    companion object {

        public val TF2: String = "E2NcUkG2"
        public val TF2_ITEMS: String = "A5fSXbf7"
        public val SOURCE_DEFAULT: String = "x9Ke0BY7"
        private val LOG = Logger()

        public platformStatic fun main(args: Array<String>) {
            var `is`: InputStream = FileInputStream(args[0])
            var key: String? = args[1]
            if (key == null) {
                key = TF2
            }
            var de = decrypt(key.toByteArray(), `is`)
            val is2 = encrypt(key.toByteArray(), de)
            `is` = FileInputStream(args[0])
            de = decrypt(key.toByteArray(), `is`)
            `is` = FileInputStream(args[0])
            val bs = 4096
            val debuf = ByteBuffer.allocate(de.available())
            val inde = ByteArray(bs)
            run {
                var read = 0
                while (read != -1) {
                    debuf.put(inde, 0, read)
                    read = de.read(inde, 0, bs)
                }
            }
            LOG.info { String(debuf.array(), "UTF-8") }
            val buf = ByteBuffer.allocate(`is`.available())
            val `in` = ByteArray(bs)
            run {
                var read = 0
                while (read != -1) {
                    buf.put(`in`, 0, read)
                    read = `is`.read(`in`, 0, bs)
                }
            }
            val buf2 = ByteBuffer.allocate(is2.available())
            val in2 = ByteArray(bs)
            run {
                var read = 0
                while (read != -1) {
                    buf2.put(in2, 0, read)
                    read = is2.read(in2, 0, bs)
                }
            }
            LOG.info { "Equal = ${Arrays.equals(buf.array(), buf2.array())}" }
        }

        throws(IOException::class)
        private fun encrypt(key: ByteArray, `is`: InputStream): InputStream {
            return ByteArrayInputStream(method(key, `is`, false))
        }

        throws(IOException::class)
        public fun decrypt(key: ByteArray, `is`: InputStream): InputStream {
            return ByteArrayInputStream(method(key, `is`, true))
        }

        throws(IOException::class)
        private fun method(key: ByteArray, `is`: InputStream, decrypt: Boolean): ByteArray {
            val buf = ByteBuffer.allocate(`is`.available())
            val ice = IceKey(0)
            ice.set(key)
            val bs = 8 // ice.blockSize();
            val `in` = ByteArray(bs)
            val out = ByteArray(bs)
            var prevRead = 0
            run {
                var read = 0
                while (read != -1) {
                    prevRead = read
                    //            if (read != bs) {
                    //                if (read == 0) {
                    ////                    System.err.println("read 0 bytes?");
                    //                } else {
                    //                    System.err.println("read: " + read);
                    //                }
                    //            }
                    if (decrypt) {
                        ice.decrypt(`in`, out)
                    } else {
                        ice.encrypt(`in`, out)
                    }
                    buf.put(out, 0, read)
                    read = `is`.read(`in`, 0, bs)
                }
            }
            // Last block is not encrypted if there are fewer bytes in it than the blocksize
            if (prevRead < bs) {
                buf.position(buf.limit() - prevRead)
                buf.put(`in`, 0, prevRead)
            }
            return buf.array()
        }
    }
}
