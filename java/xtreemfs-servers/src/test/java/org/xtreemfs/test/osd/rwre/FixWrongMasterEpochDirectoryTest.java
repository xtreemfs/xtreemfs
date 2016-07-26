/*
 * Copyright (c) 2013 Michael Berlin, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.test.osd.rwre;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestHelper;

public class FixWrongMasterEpochDirectoryTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testAutomaticMoveToCorrectDirectory() throws IOException {
        final String globalFileId = "f32b0854-91eb-44d8-adf8-65bb8baf5f60:13193";
        final String correctedFileId = globalFileId;
        final String brokenFileId = "/" + correctedFileId;
        final HashStorageLayout hsl = new HashStorageLayout(SetupUtils.createOSD1Config(), new MetadataCache());
        
        // Cleanup previous runs.
        hsl.deleteFile(brokenFileId, true);
        hsl.deleteFile(correctedFileId, true);

        final File brokenFileDir = new File(hsl.generateAbsoluteFilePath(brokenFileId));
        final File correctedFileDir = new File(hsl.generateAbsoluteFilePath(correctedFileId));

        // Set masterepoch using the wrong id.
        assertFalse(brokenFileDir.isDirectory());
        assertFalse(correctedFileDir.isDirectory());
        hsl.setMasterEpoch(brokenFileId, 1);
        assertTrue(brokenFileDir.isDirectory());

        // Get the masterepoch with the correct id.
        assertEquals(1, hsl.getMasterEpoch(correctedFileId));
        assertFalse(brokenFileDir.isDirectory());
        assertTrue(correctedFileDir.isDirectory());

        // Get the masterepoch of a file which does not exist.
        assertEquals(0, hsl.getMasterEpoch("fileIdDoesNotExist"));
    }

}
