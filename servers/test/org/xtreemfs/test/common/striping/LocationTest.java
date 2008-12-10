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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.striping.RAID0;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.test.SetupUtils;

/**
 * It tests the Location class
 * 
 * @author Jesus Malo (jmalo)
 */
public class LocationTest extends TestCase {

    private List<ServiceUUID> osdList;
    private StripingPolicy stripingPolicy;

    /** Creates a new instance of LocationTest */
    public LocationTest(String testName) {
	super(testName);
	Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    protected void setUp() throws Exception {
	System.out.println("TEST: " + getClass().getSimpleName() + "."
		+ getName());

	final int numberOfOSDs = 3;
	this.stripingPolicy = new RAID0(1, numberOfOSDs);
	osdList = new ArrayList<ServiceUUID>(numberOfOSDs);
	osdList.add(new ServiceUUID("http://www.google.com:80"));
	osdList.add(new ServiceUUID("http://www.yahoo.com:80"));
	osdList.add(new ServiceUUID("http://www.ozu.com:80"));
    }

    protected void tearDown() throws Exception {
    }

    /**
     * It tests the creation of objects
     */
    public void testCreateLocation() throws Exception {
	List[] lo = { null, new ArrayList<ServiceUUID>(), osdList };
	StripingPolicy[] stripingPolicies = new StripingPolicy[] { null,
		this.stripingPolicy };

	// Wrong use cases
	try {
	    // Null sp and list of OSDs
	    Location tester = new Location(stripingPolicies[0], lo[0]);
	    fail();
	} catch (IllegalArgumentException e) {
	}

	try {
	    // Null sp
	    Location tester = new Location(stripingPolicies[0], lo[1]);
	    fail();
	} catch (IllegalArgumentException e) {
	}

	try {
	    // Null list of OSDs
	    Location tester = new Location(stripingPolicies[1], lo[0]);
	    fail();
	} catch (IllegalArgumentException e) {
	}

	try {
	    // Wrong matching of sp and list of OSDs
	    Location tester = new Location(stripingPolicies[1], lo[1]);
	    fail();
	} catch (IllegalArgumentException e) {
	}

	// Right use case
	Location tester = new Location(stripingPolicies[1], lo[2]);

	// Right use case
	List<Object> validList = tester.asList();
	Location tester2 = new Location(validList);

	// Preparing for the second constructor
	List<Object> invalidList2 = new ArrayList();
	List<Object> invalidList3 = new ArrayList();
	invalidList3.add(validList.get(1));
	invalidList3.add(null);
	List<Object> invalidList4 = new ArrayList();
	invalidList4.add(validList.get(0));
	invalidList4.add(new ArrayList());
	List<Object> invalidList5 = new ArrayList();
	List<Object> listOSDs = (List) validList.get(1);
	listOSDs.remove(0);
	invalidList5.add(validList.get(0));
	invalidList5.add(listOSDs);

	// Wrong use cases
	try {
	    // Wrong number of arguments
	    Location tester3 = new Location(invalidList2);
	    fail();
	} catch (IllegalArgumentException e) {
	}

	try {
	    // Wrong first argument
	    Location tester3 = new Location(invalidList3);
	    fail();
	} catch (IllegalArgumentException e) {
	} catch (ClassCastException e) {
	}

	try {
	    // Wrong second argument
	    Location tester3 = new Location(invalidList4);
	    fail();
	} catch (IllegalArgumentException e) {
	}

	try {
	    // Wrong matching
	    Location tester3 = new Location(invalidList5);
	    fail();
	} catch (IllegalArgumentException e) {
	}
    }

    public void testCreateLocationFromJSON() throws Exception {
	// Preparing for the JSON constructor
	JSONString JSONLocNull = new JSONString("n");

	JSONString JSONstripingPolicy = this.stripingPolicy.asJSONString();
	List<String> osds = new ArrayList<String>();
	String osdList = JSONParser.writeJSON(osds);

	JSONString JSONemptyOSDList = new JSONString("["
		+ JSONstripingPolicy.asString() + "," + osdList + "]");

	for (ServiceUUID osd : this.osdList) {
	    osds.add(osd.toString());
	}
	osdList = JSONParser.writeJSON(osds);
	JSONString JSONLocRight = new JSONString("["
		+ JSONstripingPolicy.asString() + "," + osdList + "]");

	try {
	    // Null JSON String
	    new Location(JSONLocNull);
	    fail();
	} catch (IllegalArgumentException e) {
	}

	try {
	    // Empty osdList
	    new Location(JSONemptyOSDList);
	    fail();
	} catch (IllegalArgumentException e) {
	}

	try {
	    // Right tests
	    Location fromJSONLoc = new Location(JSONLocRight);
	    new Location(new JSONString(JSONParser.writeJSON(fromJSONLoc
		    .asList())));
	} catch (Exception e) {
	    fail();
	}

	Location tested = new Location(
		new JSONString(
			"[{\"width\":1,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/127.0.0.2:32637\"]]"));

	StripingPolicy sp = tested.getStripingPolicy();
	List<ServiceUUID> osds2 = tested.getOSDs();

	assertEquals(new RAID0(1, 1), sp);
	assertEquals(osds2.get(0), new ServiceUUID("http://127.0.0.2:32637"));
	assertEquals(1, osds2.size());
    }

    /**
     * It tests the getStripingPolicy method
     */
    public void testGetStripingPolicy() throws Exception {
	// Preparing
	final int numberOfOSDs = 1;
	final StripingPolicy sp = new RAID0(1, numberOfOSDs);
	final StripingPolicy sp2 = new RAID0(1, numberOfOSDs);
	final List<ServiceUUID> osds = new ArrayList<ServiceUUID>(numberOfOSDs);
	osds.add(new ServiceUUID("http://www.google.com:80"));
	final Location loc = new Location(sp, osds);

	// Test
	StripingPolicy answer = loc.getStripingPolicy();

	// Checking
	assertEquals(sp, answer);
	assertEquals(sp2, answer);
    }

    /**
     * It tests the getOSDs method
     */
    public void testGetOSDs() throws Exception {

	// Preparing
	final int numberOfOSDs = 3;
	final StripingPolicy sp = new RAID0(1, numberOfOSDs);
	final List<ServiceUUID> osds = new ArrayList<ServiceUUID>(numberOfOSDs);
	osds.add(new ServiceUUID("http://www.google.com:80"));
	osds.add(new ServiceUUID("http://www.yahoo.com:80"));
	osds.add(new ServiceUUID("http://www.ozu.com:80"));
	final Location loc = new Location(sp, osds);

	final List<ServiceUUID> osds2 = new ArrayList<ServiceUUID>(numberOfOSDs);
	osds2.add(new ServiceUUID("http://www.google.com:80"));
	osds2.add(new ServiceUUID("http://www.yahoo.com:80"));
	osds2.add(new ServiceUUID("http://www.ozu.com:80"));

	// Test
	List<ServiceUUID> answer = loc.getOSDs();

	// Checking
	assertEquals(osds, answer);
	assertEquals(osds2, answer);
    }

    /**
     * It tests the asList method
     */
    public void testAsList() throws Exception {
	// Preparing
	final int numberOfOSDs = 3;
	final StripingPolicy sp = new RAID0(1, numberOfOSDs);
	final List<ServiceUUID> osds = new ArrayList<ServiceUUID>(numberOfOSDs);
	osds.add(new ServiceUUID("http://www.google.com:80"));
	osds.add(new ServiceUUID("http://www.yahoo.com:80"));
	osds.add(new ServiceUUID("http://www.ozu.com:80"));
	final Location loc = new Location(sp, osds);

	// Test
	List<Object> answer = loc.asList();

	// Checking
	Map<String, Object> mappedSP = (Map) answer.get(0);
	List<String> listedOSDs = (List) answer.get(1);

	assertEquals(sp, StripingPolicy.readFromJSON(mappedSP));
	assertEquals(numberOfOSDs, listedOSDs.size());
	for (int i = 0; i < osds.size(); i++) {
	    assertEquals(osds.get(i).toString(), listedOSDs.get(i));
	}
    }

    public void testGetOSDByX() throws Exception {
	Location loc = new Location(this.stripingPolicy, this.osdList);

	assertEquals(this.osdList.get(this.stripingPolicy.getOSDByObject(0)),
		loc.getOSDByObject(0));
	assertEquals(this.osdList.get(this.stripingPolicy.getOSDByObject(1)),
		loc.getOSDByObject(1));
	assertEquals(this.osdList.get(this.stripingPolicy.getOSDByObject(85)),
		loc.getOSDByObject(85));

	final long KB = 1024;
	assertEquals(this.osdList.get(this.stripingPolicy.getOSDByOffset(0)),
		loc.getOSDByOffset(0));
	assertEquals(this.osdList.get(this.stripingPolicy.getOSDByOffset(20)),
		loc.getOSDByOffset(20));
	assertEquals(this.osdList.get(this.stripingPolicy
		.getOSDByOffset(1 * KB)), loc.getOSDByOffset(1 * KB));
	assertEquals(this.osdList.get(this.stripingPolicy
		.getOSDByOffset(85 * KB)), loc.getOSDByOffset(85 * KB));

	// one OSD
	assertEquals(this.osdList.get(this.stripingPolicy.getOSDByOffset(20)),
		loc.getOSDByByteRange(20, 80));
	assertEquals(
		this.osdList.get(this.stripingPolicy.getOSDByOffset(1022)), loc
			.getOSDByByteRange(1022, 1023));
	assertEquals(
		this.osdList.get(this.stripingPolicy.getOSDByOffset(1024)), loc
			.getOSDByByteRange(1024, 2047));

	// multiple osds
	assertEquals(null, loc.getOSDByByteRange(1020, 1055));
    }

    public void testContainsOSD() throws Exception {
	Location loc = new Location(this.stripingPolicy, this.osdList);
	assertEquals(true, loc.containsOSD(this.osdList.get(1)));
	assertEquals(false, loc.containsOSD(new ServiceUUID("bla")));
    }

    public static void main(String[] args) {
	TestRunner.run(LocationTest.class);
    }
}
