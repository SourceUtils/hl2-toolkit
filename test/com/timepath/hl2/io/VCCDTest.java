package com.timepath.hl2.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import org.junit.*;

import static org.junit.Assert.assertTrue;

/**
 *
 * @author TimePath
 */
public class VCCDTest {

    private static final Logger LOG = Logger.getLogger(VCCDTest.class.getName());
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
        public VCCDTest() {
        }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Tests whether loading and saving produces the same output
     * @throws java.lang.Exception
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