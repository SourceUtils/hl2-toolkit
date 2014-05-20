package com.timepath.hl2.io.captions;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
        InputStream is = getClass().getResourceAsStream("/test.dat");
        byte[] src = new byte[is.available()];
        //noinspection StatementWithEmptyBody
        for(int offset = 0; ( offset = is.read(src, offset, src.length - offset) ) != -1; ) ;
        is.close();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(src.length);
        com.timepath.hl2.io.captions.VCCD.save(com.timepath.hl2.io.captions.VCCD.load(new ByteArrayInputStream(src)), baos);
        assertTrue("content matches", Arrays.equals(src, baos.toByteArray()));
    }
}
