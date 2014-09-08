/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Christian Lorenz, Eugenio Cesario,
 *               Zuse Institute Berlin, Consiglio Nazionale delle Ricerche
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.common.striping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.TestHelper;

/**
 * It tests the RAID0 class
 * 
 * @author clorenz
 */
public class RAID0Test {
    @Rule
    public final TestRule     testLog  = TestHelper.testLog;

    private static final long KILOBYTE = 1024L;

    @BeforeClass
    public static void initializeTest() throws Exception {
        Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetObjectsAndBytes() throws Exception {

        Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(3, 128)).setReplicationFlags(0)
                .build();

        StripingPolicyImpl policy = StripingPolicyImpl.getPolicy(r, 0);

        long objectID, offset;

        objectID = policy.getObjectNoForOffset(20);
        assertEquals(0, objectID);

        objectID = policy.getObjectNoForOffset(20 * KILOBYTE);
        assertEquals(0, objectID);

        objectID = policy.getObjectNoForOffset(255 * KILOBYTE);
        assertEquals(1, objectID);

        objectID = policy.getObjectNoForOffset(256 * KILOBYTE);
        assertEquals(2, objectID);

        offset = policy.getObjectStartOffset(5);
        assertEquals(640 * KILOBYTE, offset);

        offset = policy.getObjectEndOffset(5);
        assertEquals(768 * KILOBYTE - 1, offset);

        offset = policy.getObjectStartOffset(6);
        assertEquals(768 * KILOBYTE, offset);
    }

    @Test
    public void testGetOSDs() throws Exception {
        Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(8, 128)).setReplicationFlags(0)
                .build();
        StripingPolicyImpl policy = StripingPolicyImpl.getPolicy(r, 0);

        int osd0 = policy.getOSDforObject(0);
        assertEquals(0, osd0);

        int osd1 = policy.getOSDforObject(1);
        assertEquals(1, osd1);

        int osd7 = policy.getOSDforObject(7);
        assertEquals(7, osd7);

        int osd8 = policy.getOSDforObject(8);
        assertEquals(0, osd8);

        int osd21 = policy.getOSDforObject(2125648682);
        assertEquals(2, osd21);

        int osd0b = policy.getOSDforOffset(20);
        assertEquals(osd0, osd0b);

        int osd0c = policy.getOSDforOffset(20 * KILOBYTE);
        assertEquals(0, osd0c);

        int osd7b = policy.getOSDforOffset(7 * 128 * KILOBYTE);
        assertEquals(osd7, osd7b);

        int osd8b = policy.getOSDforOffset(8 * 128 * KILOBYTE);
        assertEquals(osd8, osd8b);

        int osd21b = policy.getOSDforOffset(2125648682 * 128 * KILOBYTE);
        assertEquals(osd21, osd21b);
    }

    @Test
    public void testGetStripeSize() throws Exception {
        Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(3, 256)).setReplicationFlags(0)
                .build();
        StripingPolicyImpl policy = StripingPolicyImpl.getPolicy(r, 0);
        assertEquals(256 * KILOBYTE, policy.getStripeSizeForObject(5));
    }

    @Test
    public void testCalculateLastObject() throws Exception {
        Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(3, 256)).setReplicationFlags(0)
                .build();
        StripingPolicyImpl policy = StripingPolicyImpl.getPolicy(r, 0);
        assertEquals(41, policy.getObjectNoForOffset(256L * KILOBYTE * 42 - 1)); // filesize
        // =
        // offset
        // +
        // 1
        assertEquals(42, policy.getObjectNoForOffset(256L * KILOBYTE * 42 + 32000 - 1));
        assertEquals(42, policy.getObjectNoForOffset(256L * KILOBYTE * 43 - 1 - 1));
    }

    @Test
    public void testGetObjectsOfOSDiterator() throws Exception {
        Replica r = Replica.newBuilder().setStripingPolicy(SetupUtils.getStripingPolicy(3, 128)).setReplicationFlags(0)
                .build();
        StripingPolicyImpl policy = StripingPolicyImpl.getPolicy(r, 0);

        long startObject = 0, endObject = 12;
        Iterator<Long> objectsIt = policy.getObjectsOfOSD(0, startObject, endObject);
        long objectNo = startObject;
        while (objectsIt.hasNext()) {
            assertEquals(objectNo, objectsIt.next().longValue());
            assertTrue(objectNo <= endObject);
            objectNo += policy.getWidth();
        }

        startObject = 2;
        endObject = 25;
        objectsIt = policy.getObjectsOfOSD(2, startObject, endObject);
        objectNo = startObject;
        while (objectsIt.hasNext()) {
            assertEquals(objectNo, objectsIt.next().longValue());
            assertTrue(objectNo <= endObject);
            objectNo += policy.getWidth();
        }

        startObject = 0;
        endObject = 5;
        objectsIt = policy.getObjectsOfOSD(0, startObject, endObject);
        objectNo = startObject;
        while (objectsIt.hasNext()) {
            assertEquals(objectNo, objectsIt.next().longValue());
            assertTrue(objectNo <= endObject);
            objectNo += policy.getWidth();
        }

        startObject = 2;
        endObject = 4;
        objectsIt = policy.getObjectsOfOSD(1, startObject, endObject);
        objectNo = 1;
        while (objectsIt.hasNext()) {
            assertEquals(objectNo, objectsIt.next().longValue());
            assertTrue(objectNo <= endObject);
            objectNo += policy.getWidth();
        }

        startObject = 2;
        endObject = 1;
        objectsIt = policy.getObjectsOfOSD(2, startObject, endObject);
        objectNo = startObject;
        assertFalse(objectsIt.hasNext());

        startObject = 32215;
        endObject = 32435;
        objectsIt = policy.getObjectsOfOSD(0, startObject, endObject);
        objectNo = 32214;
        while (objectsIt.hasNext()) {
            assertEquals(objectNo, objectsIt.next().longValue());
            assertTrue(objectNo <= endObject);
            objectNo += policy.getWidth();
        }
    }
}
