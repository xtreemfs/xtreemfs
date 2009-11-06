/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.common.util;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.interfaces.Constants;
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
        ONCRPCServiceURL u = new ONCRPCServiceURL("oncrpcg://yagga:1254/",Constants.ONCRPC_SCHEME,12345);
    }

}