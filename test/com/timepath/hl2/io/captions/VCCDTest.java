package com.timepath.hl2.io.captions;

import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;

/**
 * @author TimePath
 */
public class VCCDTest {

    private static final Logger LOG = Logger.getLogger(VCCDTest.class.getName());

    public VCCDTest() {}

    /**
     * Tests whether loading and saving produce the same output
     */
    @Test
    public void testLoadSave() throws IOException {
        File in = new File("testdata/in.dat");
        FileInputStream is = new FileInputStream(in);
        byte[] src = new byte[(int) in.length()];
        is.read(src);
        is.close();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(src.length);
        VCCD.save(VCCD.load(new ByteArrayInputStream(src)), baos);
        assertTrue("content matches", Arrays.equals(src, baos.toByteArray()));
    }
}
