/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.foundation.intervals.AVLTreeIntervalVector;
import org.xtreemfs.foundation.intervals.Interval;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.foundation.intervals.ListIntervalVector;
import org.xtreemfs.foundation.intervals.ObjectInterval;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestHelper;

import com.google.protobuf.InvalidProtocolBufferException;

public class VectorStorageTest {
    @Rule
    public final TestRule    testLog = TestHelper.testLog;

    static OSDConfig         config;
    static HashStorageLayout layout;

    final String             fileId  = "ABCDEFG:0001";

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL);
        config = SetupUtils.createOSD1Config();
    }

    @Before
    public void setUp() throws Exception {
        FSUtils.delTree(new File(config.getObjDir()));
        layout = new HashStorageLayout(config, new MetadataCache());
    }

    @Test
    public void testStoreLoadVector() throws Exception {
        IntervalVector vecIn, vecOut;
        List<Interval> intervals = new LinkedList<Interval>();

        // Store and load a single interval
        intervals.add(new ObjectInterval(0, 2048, 1, 0));
        vecIn = new ListIntervalVector(intervals);
        layout.setECIntervalVector(fileId, vecIn, false, true);

        vecOut = new AVLTreeIntervalVector();
        layout.getECIntervalVector(fileId, false, vecOut);
        assertEquals(vecIn, vecOut);

        // Store and load a second, non overlapping interval
        intervals.add(new ObjectInterval(2048, 4096, 2, 0));
        vecIn = new ListIntervalVector(intervals);
        layout.setECIntervalVector(fileId, vecIn, false, true);

        vecOut = new AVLTreeIntervalVector();
        layout.getECIntervalVector(fileId, false, vecOut);
        assertEquals(vecIn, vecOut);

        // Add an overlapping interval
        vecOut.insert(new ObjectInterval(1024, 3096, 3, 0));
        vecIn = new ListIntervalVector(vecOut);
        layout.setECIntervalVector(fileId, vecIn, false, true);

        vecOut = new AVLTreeIntervalVector();
        layout.getECIntervalVector(fileId, false, vecOut);
        assertEquals(vecIn, vecOut);

        // Overwrite the interval
        intervals.clear();
        intervals.add(new ObjectInterval(0, 512, 4, 0));
        vecIn = new ListIntervalVector(intervals);
        layout.setECIntervalVector(fileId, vecIn, false, false);

        vecOut = new AVLTreeIntervalVector();
        layout.getECIntervalVector(fileId, false, vecOut);
        assertEquals(vecIn, vecOut);
    }

    @Test(expected = InvalidProtocolBufferException.class)
    public void testErrors() throws Exception {
        IntervalVector vecIn, vecOut;
        List<Interval> intervals = new LinkedList<Interval>();

        // Read from an inexistent file
        vecOut = new AVLTreeIntervalVector();
        boolean result = layout.getECIntervalVector(fileId, false, vecOut);
        assertFalse(result);
        
        // Read from corrupted vector file
        intervals.add(new ObjectInterval(0, 2048, 1, 0));
        vecIn = new ListIntervalVector(intervals);
        layout.setECIntervalVector(fileId, vecIn, false, false);

        // Truncate 1 byte from the end
        File fileDir = new File(layout.generateAbsoluteFilePath(fileId));
        File vectorFile = new File(fileDir, HashStorageLayout.EC_VERSIONS_CUR);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(vectorFile, true);
            // fw.write(666);
            out.getChannel().truncate(out.getChannel().size() - 1L);
        } finally {
            if (out != null) {
                out.close();
            }
        }

        // Should throw, since the message is corrupt
        vecOut = new AVLTreeIntervalVector();
        layout.getECIntervalVector(fileId, false, vecOut);
    }
}
