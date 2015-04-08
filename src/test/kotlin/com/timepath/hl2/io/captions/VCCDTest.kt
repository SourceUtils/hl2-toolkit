package com.timepath.hl2.io.captions

import org.junit.Assert
import org.junit.Test as test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

public class VCCDTest {

    /**
     * Tests whether loading and saving produce the same output
     */
    test fun testLoadSave() {
        val src = ByteArrayOutputStream().let {
            javaClass.getResourceAsStream("/test.dat").copyTo(it)
            it.toByteArray()
        }
        val dst = ByteArrayOutputStream(src.size()).let {
            VCCD.save(VCCD.load(ByteArrayInputStream(src))!!, it)
            it.toByteArray()
        }
        Assert.assertArrayEquals("content matches", src, dst)
    }
}
