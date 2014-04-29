package com.timepath.hl2.io.captions;

import com.timepath.hl2.io.captions.VCCD;
import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;
import org.junit.*;

import static org.junit.Assert.assertTrue;

/**
 *
 * @author TimePath
 */
public class VCCDTest {

    private static final Logger LOG = Logger.getLogger(VCCDTest.class.getName());

    public VCCDTest() {
    }

    /**
     * Tests whether loading and saving produce the same output
     * <p/>
     * @throws java.lang.Exception
     */
    @Test
    public void testLoadSave() throws Exception {
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
