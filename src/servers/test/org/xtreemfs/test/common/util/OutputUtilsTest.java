/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test.common.util;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.xtreemfs.common.util.OutputUtils;

/**
 *
 * @author bjko
 */
public class OutputUtilsTest extends TestCase {

    public OutputUtilsTest() {
        super();
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

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

    @Test
    public void testReadWriteHex() {
        final int objno = 129;
        final int objver = 459435;
        final int trepo = 1;
        final long checksum = 843349439598l;

        final StringBuffer sb = new StringBuffer(Integer.SIZE/8*3+2*Long.SIZE/8);
        OutputUtils.writeHexInt(sb,objno);
        OutputUtils.writeHexInt(sb,objver);
        OutputUtils.writeHexInt(sb,trepo);
        OutputUtils.writeHexInt(sb,(int) (checksum >> 32));
        OutputUtils.writeHexInt(sb,(int) (checksum & 0xFFFFFFFF));
        OutputUtils.writeHexLong(sb, checksum);
        final String result = sb.toString();
        System.out.println("result: "+result);

        int tmp = OutputUtils.readHexInt(result, 0);
        assertEquals(objno,tmp);
        tmp = OutputUtils.readHexInt(result, 8);
        assertEquals(objver,tmp);
        tmp = OutputUtils.readHexInt(result, 16);
        assertEquals(trepo,tmp);
        tmp = OutputUtils.readHexInt(result, 24);
        long tmp2 = ((long)tmp)<< 32;
        tmp = OutputUtils.readHexInt(result, 32);
        tmp2 += tmp;
        assertEquals(checksum,tmp2);
        tmp2 = OutputUtils.readHexLong(result, 40);
        assertEquals(checksum,tmp2);
    }

}