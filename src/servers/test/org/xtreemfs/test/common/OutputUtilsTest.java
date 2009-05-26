/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.test.common;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.util.OutputUtils;

public class OutputUtilsTest extends TestCase {
    
    protected void setUp() throws Exception {
    }
    
    protected void tearDown() throws Exception {
    }
    
    public void testLongHexEncoding() throws Exception {
        
        final long[] values = { 805306368000L, Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE,
            Long.MAX_VALUE, 1, 0, -1 };
        
        for (long value : values) {
            StringBuffer sb = new StringBuffer();
            OutputUtils.writeHexLong(sb, value);
            
            assertEquals(value, OutputUtils.readHexLong(sb.toString(), 0));
        }
    }
    
    public static void main(String[] args) {
        TestRunner.run(OutputUtilsTest.class);
    }
    
}
