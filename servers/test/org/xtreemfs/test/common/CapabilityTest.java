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

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.test.SetupUtils;

public class CapabilityTest extends TestCase {
    
    private static final String SECRET = "secret";
    
    protected void setUp() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL);
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
    }
    
    protected void tearDown() throws Exception {
    }
    
    public void testCapability() throws Exception {
        
        // create and test capability that is valid for an hour
        Capability cap = new Capability("bla:1", "r", System.currentTimeMillis() / 1000 + 3600, 0,
            SECRET);
        
        assertTrue(cap.isValid());
        assertEquals(cap.getFileId(), "bla:1");
        assertEquals(cap.getAccessMode(), "r");
        
        // create and test a valid capability that is parsed from
        // a string representation
        String capAsString = cap.toString();
        
        Capability cap2 = new Capability(capAsString, SECRET);
        assertTrue(cap2.isValid());
        assertEquals(cap2.getFileId(), "bla:1");
        assertEquals(cap2.getAccessMode(), "r");
        
        // assert that a capability is invalid if the signature is invalid
        String fakedCap = capAsString.substring(0, capAsString.length() - 35)
            + "\"FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF\"]";
        Capability cap3 = new Capability(fakedCap, SECRET);
        assertFalse(cap3.isValid());
        
        // assert that a capability is invalid if it has timed out
        Capability cap4 = new Capability("bla:2", "w", System.currentTimeMillis() / 1000 - 3600, 0,
            SECRET);
        assertFalse(cap4.isValid());
        
    }
    
    public static void main(String[] args) {
        TestRunner.run(CapabilityTest.class);
    }
    
}
