/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.common;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.Capability;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
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
                .currentTimeMillis() / 1000 + 100, "", 1, false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, SECRET);
        
        assertTrue(cap.isValid());
        assertEquals(cap.getFileId(), "1254" + ((HashStorageLayout.WIN) ? ":" : "_") + "AB");
        assertEquals(cap.getAccessMode(), 1);
        
        // assert that a capability is invalid if it has timed out
        Capability cap4 = new Capability("bla" + ((HashStorageLayout.WIN) ? ":" : "_") + "2", 1, 60, System
                .currentTimeMillis() / 1000 - 3600, "", 0, false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, SECRET);
        assertFalse(cap4.isValid());
        
    }
    
    public static void main(String[] args) {
        TestRunner.run(CapabilityTest.class);
    }
    
}
