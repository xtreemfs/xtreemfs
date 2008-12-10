/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin,
    Barcelona Supercomputing Center - Centro Nacional de Supercomputacion and
    Consiglio Nazionale delle Ricerche.

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
 * AUTHORS: Jan Stender (ZIB), Jesús Malo (BSC), Björn Kolbeck (ZIB),
 *          Eugenio Cesario (CNR)
 */

package org.xtreemfs.test.common.striping;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.osd.LocationsCache;

/**
 * This class implements the tests for LocationsCache
 * 
 * @author jmalo
 */
public class LocationsCacheTest extends TestCase {
    private LocationsCache cache;
    private final int maximumSize = 3;

    /** Creates a new instance of LocationsCacheTest */
    public LocationsCacheTest(String testName) {
	super(testName);
    }

    protected void setUp() throws Exception {
	System.out.println("TEST: " + getClass().getSimpleName() + "."
		+ getName());
	cache = new LocationsCache(maximumSize);
    }

    protected void tearDown() throws Exception {
	cache = null;
    }

    /**
     * It tests the update method
     */
    public void testUpdate() throws Exception {

	Locations loc = new Locations(new JSONString("[[], 1]"));

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
    public void testGetVersion() throws Exception {

	Locations loc0 = new Locations(new JSONString("[[], 1]"));
	Locations loc1 = new Locations(new JSONString("[[], 2]"));
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
    public void testGetLocations() throws Exception {

	Locations loc = new Locations(new JSONString("[[], 1]"));

	// It fills the cache
	for (int i = 0; i < maximumSize; i++) {
	    cache.update("F" + i, loc);
	}

	// Checks the whole cache
	for (int i = 0; i < maximumSize; i++) {
	    Locations loc2 = cache.getLocations("F" + i);

	    assertNotNull(loc2);
	    assertEquals(loc, loc2);
	}

	// Removes an entry and adds a new one
	{
	    cache.update("F" + maximumSize, loc);

	    Locations loc2 = cache.getLocations("F" + 0);
	    assertNull(loc2);

	    for (int i = 1; i <= maximumSize; i++) {
		loc2 = cache.getLocations("F" + i);

		assertNotNull(loc2);
		assertEquals(loc, loc2);
	    }
	}
    }

    public static void main(String[] args) {
	TestRunner.run(LocationsCacheTest.class);
    }
}
