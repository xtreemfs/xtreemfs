/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.Capability;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestEnvironment;
import org.xtreemfs.test.TestHelper;

public class CapabilityTest {
    @Rule
    public final TestRule       testLog = TestHelper.testLog;

    private static final String SECRET = "secret";

    private TestEnvironment     te;

    @Before
    public void setUp() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL, SetupUtils.DEBUG_CATEGORIES);


        te = new TestEnvironment(TestEnvironment.Services.TIME_SYNC);
        te.start();
    }

    @After
    public void tearDown() throws Exception {
        te.shutdown();
    }

    @Test
    public void testCapability() throws Exception {

        // create and test capability that is valid for an hour
        Capability cap = new Capability("1254" + ((HashStorageLayout.WIN) ? ":" : "_") + "AB", 1, 60,
                System.currentTimeMillis() / 1000 + 100, "", 1, false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0, SECRET, 0);

        assertTrue(cap.isValid());
        assertEquals(cap.getFileId(), "1254" + ((HashStorageLayout.WIN) ? ":" : "_") + "AB");
        assertEquals(cap.getAccessMode(), 1);

        // assert that a capability is invalid if it has timed out
        Capability cap4 = new Capability("bla" + ((HashStorageLayout.WIN) ? ":" : "_") + "2", 1, 60,
                System.currentTimeMillis() / 1000 - 3600, "", 0, false, SnapConfig.SNAP_CONFIG_SNAPS_DISABLED, 0,
                SECRET, 0);
        assertFalse(cap4.isValid());

    }
}
