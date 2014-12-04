/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Eugenio Cesario,
 *               Zuse Institute Berlin, Consiglio Nazionale delle Ricerche
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test.common.striping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.osd.LocationsCache;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.test.TestHelper;

/**
 * This class implements the tests for LocationsCache
 * 
 * @author jmalo
 */
public class LocationsCacheTest {
    @Rule
    public final TestRule  testLog     = TestHelper.testLog;

    private LocationsCache cache;

    private final int      maximumSize = 3;

    @Before
    public void setUp() throws Exception {
        cache = new LocationsCache(maximumSize);
    }

    @After
    public void tearDown() throws Exception {
        cache = null;
    }

    /**
     * It tests the update method
     */
    @Test
    public void testUpdate() throws Exception {

        XLocSet xlocSet = XLocSet.newBuilder().setReadOnlyFileSize(0).setReplicaUpdatePolicy("").setVersion(1).build();
        XLocations loc = new XLocations(xlocSet, null);

        for (int i = 0; i < 3 * maximumSize; i++) {
            cache.update("F" + i, loc);
        }

        for (int i = 0; i < 2 * maximumSize; i++) {
            assertNull(cache.getLocations("F" + i));
            assertEquals(0, cache.getVersion("F" + i));
        }

        for (int i = 2 * maximumSize; i < 3 * maximumSize; i++) {
            assertNotNull(cache.getLocations("F" + i));
            assertEquals(loc.getVersion(), cache.getVersion("F" + i));
        }
    }

    /**
     * It tests the getVersion method
     */
    @Test
    public void testGetVersion() throws Exception {

        XLocSet xlocSet0 = XLocSet.newBuilder().setReadOnlyFileSize(0).setReplicaUpdatePolicy("").setVersion(1).build();
        XLocSet xlocSet1 = XLocSet.newBuilder().setReadOnlyFileSize(0).setReplicaUpdatePolicy("").setVersion(2).build();

        XLocations loc0 = new XLocations(xlocSet0, null);
        XLocations loc1 = new XLocations(xlocSet1, null);
        String fileId = "F0";

        // It asks the version number of an inexistent entry
        assertEquals(0, cache.getVersion(fileId));

        // It asks the version number of a new added entry
        cache.update(fileId, loc0);
        assertEquals(loc0.getVersion(), cache.getVersion(fileId));

        // It asks the version number of an updated entry
        cache.update(fileId, loc1);
        assertEquals(loc1.getVersion(), cache.getVersion(fileId));
    }

    /**
     * It tests the getLocations method
     */
    @Test
    public void testGetLocations() throws Exception {

        XLocSet xlocSet = XLocSet.newBuilder().setReadOnlyFileSize(0).setReplicaUpdatePolicy("").setVersion(1).build();
        XLocations loc = new XLocations(xlocSet, null);

        // It fills the cache
        for (int i = 0; i < maximumSize; i++) {
            cache.update("F" + i, loc);
        }

        // Checks the whole cache
        for (int i = 0; i < maximumSize; i++) {
            XLocations loc2 = cache.getLocations("F" + i);

            assertNotNull(loc2);
            assertEquals(loc, loc2);
        }

        // Removes an entry and adds a new one
        {
            cache.update("F" + maximumSize, loc);

            XLocations loc2 = cache.getLocations("F" + 0);
            assertNull(loc2);

            for (int i = 1; i <= maximumSize; i++) {
                loc2 = cache.getLocations("F" + i);

                assertNotNull(loc2);
                assertEquals(loc, loc2);
            }
        }
    }
}
