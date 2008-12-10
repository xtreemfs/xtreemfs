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
 * AUTHORS: Christian Lorenz (ZIB), Jan Stender (ZIB), Jesús Malo (BSC), Björn Kolbeck (ZIB),
 *          Eugenio Cesario (CNR)
 */

package org.xtreemfs.test.common.striping;

import java.util.List;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.RAID0;
import org.xtreemfs.common.striping.StripeInfo;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.test.SetupUtils;

/**
 * It tests the RAID0 class
 * 
 * @author clorenz
 */
public class RAID0Test extends TestCase {
    private static final long KILOBYTE = 1024L;

    /** Creates a new instance of RAID0Test */
    public RAID0Test(String testName) {
	super(testName);
	Logging.start(SetupUtils.DEBUG_LEVEL);
    }

    protected void setUp() throws Exception {
	System.out.println("TEST: " + getClass().getSimpleName() + "."
		+ getName());
    }

    protected void tearDown() throws Exception {

    }

    /**
     * It tests the creation of RAID0 objects
     */
    public void testRAID0ObjectCreation() throws Exception {

	RAID0 wrong;

	try {
	    // Bad size
	    wrong = new RAID0(0, 1);

	    fail();
	} catch (IllegalArgumentException e) {
	}

	try {
	    // Bad number of OSDs
	    wrong = new RAID0(1, 0);

	    fail();
	} catch (IllegalArgumentException e) {
	}

	try {
	    // Bad size and number of OSDs
	    wrong = new RAID0(-1, 0);

	    fail();
	} catch (IllegalArgumentException e) {
	}

	RAID0 right = new RAID0(1, 1);
    }

    public void testGetObjectsAndBytes() throws Exception {
	RAID0 policy = new RAID0(128, 3); // 128 kB per stripe and 3 OSDs
	long objectID, offset;

	objectID = policy.getObject(20);
	assertEquals(0, objectID);

	objectID = policy.getObject(20 * KILOBYTE);
	assertEquals(0, objectID);

	objectID = policy.getObject(255 * KILOBYTE);
	assertEquals(1, objectID);

	objectID = policy.getObject(256 * KILOBYTE);
	assertEquals(2, objectID);

	offset = policy.getFirstByte(5);
	assertEquals(640 * KILOBYTE, offset);

	offset = policy.getLastByte(5);
	assertEquals(768 * KILOBYTE - 1, offset);

	offset = policy.getFirstByte(6);
	assertEquals(768 * KILOBYTE, offset);
    }

    public void testGetOSDs() throws Exception {
	RAID0 policy = new RAID0(128, 8); // 128 kB per stripe and 8 OSDs

	int osd0 = policy.getOSDByObject(0);
	assertEquals(0, osd0);

	int osd1 = policy.getOSDByObject(1);
	assertEquals(1, osd1);

	int osd7 = policy.getOSDByObject(7);
	assertEquals(7, osd7);

	int osd8 = policy.getOSDByObject(8);
	assertEquals(0, osd8);

	int osd21 = policy.getOSDByObject(2125648682);
	assertEquals(2, osd21);

	int osd0b = policy.getOSDByOffset(20);
	assertEquals(osd0, osd0b);

	int osd0c = policy.getOSDByOffset(20 * KILOBYTE);
	assertEquals(0, osd0c);

	int osd7b = policy.getOSDByOffset(7 * 128 * KILOBYTE);
	assertEquals(osd7, osd7b);

	int osd8b = policy.getOSDByOffset(8 * 128 * KILOBYTE);
	assertEquals(osd8, osd8b);

	int osd21b = policy.getOSDByOffset(2125648682 * 128 * KILOBYTE);
	assertEquals(osd21, osd21b);
    }

    public void testGetObjectsByRange() throws Exception {
	RAID0 policy = new RAID0(10, 3); // 10 kB per stripe and 3 OSDs
	List<StripeInfo> result;
	StripeInfo expectedStart;
	StripeInfo expectedEnd;

	// one object, byte-range < stripe size
	result = policy.getObjects(0, 9 * KILOBYTE);
	expectedStart = new StripeInfo(0, 0, 0, 9 * KILOBYTE);
	// expected only one element in list
	assertEquals(1, result.size());
	assertEquals(expectedStart, result.remove(0));

	// more objects, byte-range < stripe size
	result = policy.getObjects(5 * KILOBYTE, 14 * KILOBYTE);
	expectedStart = new StripeInfo(0, 0, 5 * KILOBYTE, 10 * KILOBYTE - 1);
	expectedEnd = new StripeInfo(1, 1, 0, 4 * KILOBYTE);
	// expected two elements in list
	assertEquals(2, result.size());
	assertEquals(expectedStart, result.remove(0));
	assertEquals(expectedEnd, result.remove(0));

	// more objects, byte-range > stripe size (simple)
	result = policy.getObjects(0, 29 * KILOBYTE);
	expectedStart = new StripeInfo(0, 0, 0, 10 * KILOBYTE - 1);
	expectedEnd = new StripeInfo(2, 2, 0, 9 * KILOBYTE);
	assertEquals(expectedStart, result.remove(0));
	assertEquals(expectedEnd, result.remove(0));

	// more objects, byte-range > stripe size
	result = policy.getObjects(8 * KILOBYTE, 54 * KILOBYTE);
	expectedStart = new StripeInfo(0, 0, 8 * KILOBYTE, 10 * KILOBYTE - 1);
	expectedEnd = new StripeInfo(5, 2, 0, 4 * KILOBYTE);
	assertEquals(expectedStart, result.remove(0));
	assertEquals(expectedEnd, result.remove(0));
    }

    public void testGetStripeSize() throws Exception {
	RAID0 policy = new RAID0(256, 3); // 256 kB per stripe and 3 OSDs
	assertEquals(256 * KILOBYTE, policy.getStripeSize(5));
    }

    public void testCalculateLastObject() throws Exception {
	RAID0 policy = new RAID0(256, 3); // 256 kB per stripe and 3 OSDs
	assertEquals(41, policy.calculateLastObject(256L * KILOBYTE * 42)); // filesize
	// =
	// offset
	// +
	// 1
	assertEquals(42, policy
		.calculateLastObject(256L * KILOBYTE * 42 + 32000));
	assertEquals(42, policy.calculateLastObject(256L * KILOBYTE * 43 - 1));
    }

    /**
     * It tests the export method
     */
    public void testAsJSONString() throws Exception {
	RAID0 subject = new RAID0(7, 3); // 7 kB per stripe and 3 OSDs
	assertEquals(new JSONString(
		"{\"width\":3,\"policy\":\"RAID0\",\"stripe-size\":7}"),
		subject.asJSONString());
    }

    /**
     * It tests the parsing and unparsing of the striping policies
     */
    public void testJSONParsing() throws Exception {
	// RAID0 parsing and unparsing
	RAID0 unparsedRaid0 = new RAID0(1, 1);
	StripingPolicy parsedRaid0 = StripingPolicy.readFromJSON(unparsedRaid0
		.asJSONString());

	assertEquals(unparsedRaid0.asJSONString(), parsedRaid0.asJSONString());
    }
}
