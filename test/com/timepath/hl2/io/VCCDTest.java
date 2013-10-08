package com.timepath.hl2.io;

import java.io.*;
import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author TimePath
 */
public class VCCDTest {
    
    public VCCDTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Tests whether loading and saving produces the same output
     */
    @Test
    public void testLoadSave() throws Exception {
        File in = new File("testdata/in.dat");
        FileInputStream is = new FileInputStream(in);
        byte[] array = new byte[is.available()];
        is.read(array);
        is.close();
        ByteBuffer buf = ByteBuffer.wrap(array);
        ByteBuffer buf2 = VCCD.save(VCCD.load(buf.slice()));
        assertTrue("content matches", buf.equals(buf2));
    }
    
}