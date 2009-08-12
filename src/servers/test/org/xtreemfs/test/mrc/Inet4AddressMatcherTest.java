/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.mrc;

import java.net.Inet4Address;
import java.net.InetAddress;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xtreemfs.mrc.replication.dcmap.Inet4AddressMatcher;
import static org.junit.Assert.*;

/**
 *
 * @author bjko
 */
public class Inet4AddressMatcherTest {

    public Inet4AddressMatcherTest() {
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

    @Test
    public void testMatcher() throws Exception {

        Inet4Address ifa1 = (Inet4Address) InetAddress.getByAddress(new byte[]{(byte)192,(byte)168,(byte)1,(byte)125});
        Inet4Address ifa2 = (Inet4Address) InetAddress.getByAddress(new byte[]{(byte)192,(byte)168,(byte)1,(byte)126});
        Inet4Address ifa3 = (Inet4Address) InetAddress.getByAddress(new byte[]{(byte)192,(byte)168,(byte)1,(byte)254});
        Inet4Address ifa4 = (Inet4Address) InetAddress.getByAddress(new byte[]{(byte)192,(byte)168,(byte)10,(byte)125});
        Inet4Address ifa5 = (Inet4Address) InetAddress.getByAddress(new byte[]{(byte)10,(byte)0,(byte)1,(byte)125});

        Inet4AddressMatcher m = new Inet4AddressMatcher(ifa1);
        assertTrue(m.matches(ifa1));
        assertFalse(m.matches(ifa2));
        assertFalse(m.matches(ifa3));
        assertFalse(m.matches(ifa4));
        assertFalse(m.matches(ifa5));

        m = new Inet4AddressMatcher(ifa1,25);
        assertTrue(m.matches(ifa1));
        assertTrue(m.matches(ifa2));
        assertFalse(m.matches(ifa3));
        assertFalse(m.matches(ifa4));
        assertFalse(m.matches(ifa5));

        m = new Inet4AddressMatcher(ifa1,24);
        assertTrue(m.matches(ifa1));
        assertTrue(m.matches(ifa2));
        assertTrue(m.matches(ifa3));
        assertFalse(m.matches(ifa4));
        assertFalse(m.matches(ifa5));
        
        m = new Inet4AddressMatcher(ifa1,16);
        assertTrue(m.matches(ifa1));
        assertTrue(m.matches(ifa2));
        assertTrue(m.matches(ifa3));
        assertTrue(m.matches(ifa4));
        assertFalse(m.matches(ifa5));

        m = new Inet4AddressMatcher(ifa1,1);
        assertTrue(m.matches(ifa1));
        assertTrue(m.matches(ifa2));
        assertTrue(m.matches(ifa3));
        assertTrue(m.matches(ifa4));
        assertFalse(m.matches(ifa5));



    }

}