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
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;

public class CapabilityTest extends TestCase {
    
    private static final String SECRET = "secret";
    
    private TestEnvironment     te;
    
    protected void setUp() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        
        te = new TestEnvironment(TestEnvironment.Services.TIME_SYNC);
        te.start();
    }
    
    protected void tearDown() throws Exception {
        te.shutdown();
    }
    
    public void testCapability() throws Exception {
        
        // create and test capability that is valid for an hour
        Capability cap = new Capability("1254" + ((HashStorageLayout.WIN) ? ":" : "_") + "AB", 1, 60, System
                .currentTimeMillis() / 1000 + 100, "", 1, false, SECRET);
        
        assertTrue(cap.isValid());
        assertEquals(cap.getFileId(), "1254" + ((HashStorageLayout.WIN) ? ":" : "_") + "AB");
        assertEquals(cap.getAccessMode(), 1);
        
        // assert that a capability is invalid if it has timed out
        Capability cap4 = new Capability("bla" + ((HashStorageLayout.WIN) ? ":" : "_") + "2", 1, 60, System
                .currentTimeMillis() / 1000 - 3600, "", 0, false, SECRET);
        assertFalse(cap4.isValid());
        
    }
    
    public static void main(String[] args) {
        TestRunner.run(CapabilityTest.class);
    }
    
}
