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

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.test.SetupUtils;

/**
 * This class implements the tests for Locations
 * 
 * @author jmalo
 */
public class LocationsTest extends TestCase {
    List<ServiceUUID> osds = new ArrayList<ServiceUUID>();

    /**
     * Creates a new instance of LocationsTest
     */
    public LocationsTest(String testName) {
	super(testName);
	Logging.start(SetupUtils.DEBUG_LEVEL);

	osds.add(new ServiceUUID("http://127.0.0.1:65535"));
	osds.add(new ServiceUUID("http://192.168.0.1:65535"));
	osds.add(new ServiceUUID("http://172.16.0.1:65535"));
	osds.add(new ServiceUUID("http://10.0.0.1:65535"));
    }

    protected void setUp() throws Exception {
	System.out.println("TEST: " + getClass().getSimpleName() + "."
		+ getName());
    }

    protected void tearDown() throws Exception {
    }

    /**
     * It tests the constructor from strings
     */
    public void testFromString() throws Exception {
	Locations loc;

	// Right use cases
	{
	    loc = new Locations(
		    new JSONString(
			    "[[[{\"width\":1,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/127.0.0.1:65535\"]]],1,\"sync\"]"));
	    assertNotNull(loc.getLocation(osds.get(0)));
	    assertNull(loc.getLocation(osds.get(1)));

	    assertEquals(1, loc.getNumberOfReplicas());
	    assertEquals(1, loc.getVersion());
	    assertEquals(Locations.REPLICA_UPDATE_POLICY_SYNC, loc
		    .getReplicaUpdatePolicy());
	}

	{
	    loc = new Locations(
		    new JSONString(
			    "[[[{\"width\":2,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/127.0.0.1:65535\", \"http:\\/\\/192.168.0.1:65535\"]]], 1,\"lazy\"]"));
	    assertNotNull(loc.getLocation(osds.get(0)));
	    assertNotNull(loc.getLocation(osds.get(1)));
	    assertNull(loc.getLocation(osds.get(2)));
	    assertEquals(loc.getLocation(osds.get(0)), loc.getLocation(osds
		    .get(1)));

	    assertEquals(1, loc.getNumberOfReplicas());
	    assertEquals(1, loc.getVersion());
	    assertEquals(Locations.REPLICA_UPDATE_POLICY_ONDEMAND, loc
		    .getReplicaUpdatePolicy());
	}

	{
	    loc = new Locations(
		    new JSONString(
			    "[[[{\"width\":1,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/127.0.0.1:65535\"]], [{\"width\":1,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/172.16.0.1:65535\"]]], 1]"));
	    assertNotNull(loc.getLocation(osds.get(0)));
	    assertNull(loc.getLocation(osds.get(1)));
	    assertNotNull(loc.getLocation(osds.get(2)));
	    assertNull(loc.getLocation(osds.get(3)));
	    assertFalse(loc.getLocation(osds.get(0)).equals(
		    loc.getLocation(osds.get(2))));

	    assertEquals(2, loc.getNumberOfReplicas());
	    assertEquals(1, loc.getVersion());
	    assertEquals(Locations.REPLICA_UPDATE_POLICY_ONDEMAND, loc
		    .getReplicaUpdatePolicy());
	}

	{
	    loc = new Locations(
		    new JSONString(
			    "[[[{\"width\":2,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/127.0.0.1:65535\", \"http:\\/\\/192.168.0.1:65535\"]], [{\"width\":2,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/172.16.0.1:65535\", \"http:\\/\\/10.0.0.1:65535\"]]], 1,\"lazy\"]"));
	    assertNotNull(loc.getLocation(osds.get(0)));
	    assertNotNull(loc.getLocation(osds.get(1)));
	    assertNotNull(loc.getLocation(osds.get(2)));
	    assertNotNull(loc.getLocation(osds.get(3)));
	    assertEquals(loc.getLocation(osds.get(0)), loc.getLocation(osds
		    .get(1)));
	    assertEquals(loc.getLocation(osds.get(2)), loc.getLocation(osds
		    .get(3)));
	    assertFalse(loc.getLocation(osds.get(0)).equals(
		    loc.getLocation(osds.get(2))));

	    assertEquals(2, loc.getNumberOfReplicas());
	    assertEquals(1, loc.getVersion());
	    assertEquals(Locations.REPLICA_UPDATE_POLICY_ONDEMAND, loc
		    .getReplicaUpdatePolicy());
	}

	{
	    loc = new Locations(new JSONString("[[], 1]"));

	    assertEquals(0, loc.getNumberOfReplicas());
	    assertEquals(1, loc.getVersion());
	}

	{
	    loc = new Locations(
		    new JSONString(
			    "[[[{\"width\":1,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/127.0.0.1:65535\"]], [{\"width\":1,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/172.16.0.1:65535\"]]], 1,\"sync:1\"]"));
	    assertEquals(1, loc.getReplicaSyncLevel());
	}

	{
	    loc = new Locations(
		    new JSONString(
			    "[[[{\"width\":1,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/127.0.0.1:65535\"]]],1,\"sync:2\"]"));
	    assertEquals(Locations.REPLICA_UPDATE_POLICY_SYNC, loc
		    .getReplicaUpdatePolicy());
	    assertEquals(1, loc.getReplicaSyncLevel());
	}

	{
	    loc = new Locations(
		    new JSONString(
			    "[[[{\"width\":1,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/127.0.0.1:65535\"]]],1,\"sync:0\"]"));
	    assertEquals(Locations.REPLICA_UPDATE_POLICY_SYNC, loc
		    .getReplicaUpdatePolicy());
	    assertEquals(0, loc.getReplicaSyncLevel());
	}

	// Wrong use cases
	try {
	    new Locations(new JSONString(""));
	    fail();
	} catch (JSONException ex) {
	}

	try {
	    new Locations(new JSONString("[]"));
	    fail();
	} catch (Exception ex) {
	}

	try {
	    new Locations(new JSONString("[[]]"));
	    fail();
	} catch (Exception ex) {
	}

	try {
	    new Locations(new JSONString("[[],]"));
	    fail();
	} catch (Exception ex) {
	}

	try {
	    new Locations(new JSONString("[0]"));
	    fail();
	} catch (Exception ex) {
	}

	try {
	    new Locations(new JSONString("[, 0]"));
	    fail();
	} catch (Exception ex) {
	}

	try {
	    new Locations(new JSONString("[0, 0]"));
	    fail();
	} catch (Exception ex) {
	}

	try {
	    new Locations(new JSONString("[0, []]"));
	    fail();
	} catch (Exception ex) {
	}

	// tests, if we don't allow more than specified
	/*
	 * try { new Locations(newJSONString(
	 * "[[[{\"width\":1,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/127.0.0.1:65535\"]], [{\"width\":1,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/172.16.0.1:65535\"]]], 1,\"lazy:1\"]"
	 * )); fail(); } catch(Exception ex) { }
	 */
    }

    public void testAsJSONString() throws Exception {
	JSONString expected = new JSONString(
		"[[[{\"width\":2,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/127.0.0.1:65535\",\"http:\\/\\/192.168.0.1:65535\"]],[{\"width\":2,\"policy\":\"RAID0\",\"stripe-size\":1},[\"http:\\/\\/172.16.0.1:65535\",\"http:\\/\\/10.0.0.1:65535\"]]],1,\"sync:1\"]");
	assertEquals(expected.asString(), new Locations(expected)
		.asJSONString().asString());
    }

    public static void main(String[] args) {
	TestRunner.run(LocationsTest.class);
    }
}
