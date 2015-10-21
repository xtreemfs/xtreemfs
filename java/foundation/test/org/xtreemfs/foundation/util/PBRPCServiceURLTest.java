/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck, Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */


package org.xtreemfs.foundation.util;

import org.junit.*;
import org.xtreemfs.foundation.pbrpc.Schemes;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author bjko
 */
public class PBRPCServiceURLTest {

    public PBRPCServiceURLTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getProtocol method, of class ONCRPCServiceURL.
     */
    @Test
    public void testURLParse() throws Exception {
        String host = "yagga";
        int port = 1254;
        PBRPCServiceURL u = new PBRPCServiceURL("pbrpcg://"+host+":"+port+"/",
                Schemes.SCHEME_PBRPC,12345);
        assertEquals(host, u.getHost());
        assertEquals(port, u.getPort());
    }

}