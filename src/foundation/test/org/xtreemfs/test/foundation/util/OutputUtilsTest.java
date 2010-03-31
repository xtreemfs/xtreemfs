/*
 * Copyright (c) 2008-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: Jan Stender (ZIB), Bjoern Kolbeck (ZIB)
 */

package org.xtreemfs.test.foundation.util;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.junit.Test;
import org.xtreemfs.foundation.util.OutputUtils;

public class OutputUtilsTest extends TestCase {
    
    protected void setUp() throws Exception {
    }
    
    protected void tearDown() throws Exception {
    }
    
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
    
    public static void main(String[] args) {
        TestRunner.run(OutputUtilsTest.class);
    }
    
}
