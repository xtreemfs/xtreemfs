/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */


package org.xtreemfs.foundation.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OutputUtilsTest {

    @Test
    public void testLongHexEncoding() throws Exception {
        
        final long[] values = { 805306368000L, Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE,
            Long.MAX_VALUE, 1, 0, -1 };
        
        for (long value : values) {
            StringBuffer sb = new StringBuffer();
            OutputUtils.writeHexLong(sb, value);
            
            assertEquals(value, OutputUtils.readHexLong(sb.toString(), 0));
        }
    }
    
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
        // System.out.println("result: "+result);

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
