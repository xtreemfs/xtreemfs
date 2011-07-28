/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck, Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */


package org.xtreemfs.test.foundation.util;

import org.xtreemfs.foundation.pbrpc.Schemes;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.foundation.util.ONCRPCServiceURL;

import static org.junit.Assert.*;

/**
 *
 * @author bjko
 */
public class ONCRPCServiceURLTest {

    public ONCRPCServiceURLTest() {
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
        ONCRPCServiceURL u = new ONCRPCServiceURL("pbrpcg://"+host+":"+port+"/",
                Schemes.SCHEME_PBRPC,12345);
        assertEquals(host, u.getHost());
        assertEquals(port, u.getPort());
    }

}