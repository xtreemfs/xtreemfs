/*  Copyright (c) 2008 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.test.osd.replication;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.common.striping.RAID0;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.osd.RequestDetails;
import org.xtreemfs.osd.replication.RandomStrategy;
import org.xtreemfs.osd.replication.SimpleStrategy;
import org.xtreemfs.osd.replication.TransferStrategy;
import org.xtreemfs.osd.replication.TransferStrategy.NextRequest;

import com.sun.xml.internal.ws.api.pipe.NextAction;

/**
 * 
 * 18.12.2008
 * 
 * @author clorenz
 */
public class TransferStrategiesTest extends TestCase {
    private RequestDetails details;
    private TransferStrategy strategy;

    /**
     * 
     */
    public TransferStrategiesTest() {
	String file = "1:1";
	Capability capability = new Capability(file, "read", 0, "IAmTheClient");
	List<Location> locationList = new ArrayList<Location>();

	int stripeSize = 128;

	// add available osds
	List<ServiceUUID> osds = new ArrayList<ServiceUUID>();
	osds.add(new ServiceUUID("UUID:localhost:33637"));
	osds.add(new ServiceUUID("UUID:localhost:33638"));
	osds.add(new ServiceUUID("UUID:localhost:33639"));
	List<ServiceUUID> osds2 = new ArrayList<ServiceUUID>();
	osds2.add(new ServiceUUID("UUID:localhost:33640"));
	osds2.add(new ServiceUUID("UUID:localhost:33641"));
	osds2.add(new ServiceUUID("UUID:localhost:33642"));
	List<ServiceUUID> osds3 = new ArrayList<ServiceUUID>();
	osds3.add(new ServiceUUID("UUID:localhost:33643"));
	osds3.add(new ServiceUUID("UUID:localhost:33644"));
	osds3.add(new ServiceUUID("UUID:localhost:33645"));
	List<ServiceUUID> osds4 = new ArrayList<ServiceUUID>();
	osds4.add(new ServiceUUID("UUID:localhost:33646"));
	osds4.add(new ServiceUUID("UUID:localhost:33647"));
	osds4.add(new ServiceUUID("UUID:localhost:33648"));

	locationList.add(new Location(new RAID0(stripeSize, osds.size()), osds));
	locationList.add(new Location(new RAID0(stripeSize, osds2.size()), osds2));
	locationList.add(new Location(new RAID0(stripeSize, osds3.size()), osds3));
	locationList.add(new Location(new RAID0(stripeSize, osds4.size()), osds4));

	Locations locations = new Locations(locationList);

	this.details = new RequestDetails();
	this.details.setFileId(file);
	this.details.setCapability(capability);
	this.details.setLocationList(locations);
	this.details.setObjectNumber(2);
	
	// set the first replica as current replica
	this.details.setCurrentReplica(locations.getLocation(0));
    }

    @Before
    public void setUp() throws Exception {
	this.strategy = new SimpleStrategy(this.details);
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link org.xtreemfs.osd.replication.TransferStrategy#addRequiredObject(long)}
     * and
     * {@link org.xtreemfs.osd.replication.TransferStrategy#removeRequiredObject(long)}
     * .
     */
    @Test
    public void testAddAndRemoveRequiredObject() {
	this.strategy.addRequiredObject(this.details.getObjectNumber());
	assertTrue(this.strategy.removeRequiredObject(this.details
		.getObjectNumber()));
    }

    /**
     * Test method for
     * {@link org.xtreemfs.osd.replication.TransferStrategy#addPreferredObject(long)}
     * and
     * {@link org.xtreemfs.osd.replication.TransferStrategy#removePreferredObject(long)}
     * .
     */
    @Test
    public void testAddAndRemovePreferredObject() {
	this.strategy.addPreferredObject(this.details.getObjectNumber());
	assertTrue(this.strategy.removePreferredObject(this.details
		.getObjectNumber()));
    }

    /**
     * Test method for
     * {@link org.xtreemfs.osd.replication.TransferStrategy#getRequiredObjectsCount()}
     * and
     * {@link org.xtreemfs.osd.replication.TransferStrategy#getPreferredObjectsCount()}
     * .
     */
    @Test
    public void testGetXXXObjectsCount() {
	this.strategy.addRequiredObject(1);
	this.strategy.addRequiredObject(2);
	this.strategy.addRequiredObject(3);
	this.strategy.addRequiredObject(4);
	this.strategy.addPreferredObject(3);

	assertEquals(4, this.strategy.getRequiredObjectsCount());
	assertEquals(1, this.strategy.getPreferredObjectsCount());
    }

    @Test
    public void testCurrentReplicaNotInReplicaList() {
	this.strategy = new SimpleStrategy(details);
	for(int i=0; i<20; i++) {
	    this.strategy.addRequiredObject(i);
	}
	while(true) {
	   this.strategy.selectNext();
	   NextRequest next = this.strategy.getNext();
	   if(next!=null) {
		assertNotSame(this.details.getCurrentReplica(), next.osd);
	   } else
	       break;
	}

    }

    /**
     * Test method for
     * {@link org.xtreemfs.osd.replication.SimpleStrategy#selectNext()}.
     */
    @Test
    public void testSelectNextForSimpleTransfer() {
	this.strategy = new SimpleStrategy(details);
	this.strategy.addRequiredObject(1);
	this.strategy.addRequiredObject(2);
	this.strategy.addRequiredObject(3);
	this.strategy.addRequiredObject(4);
	this.strategy.addPreferredObject(2);

	int replica = 1;
	
	// first request
	this.strategy.selectNext();
	NextRequest next = this.strategy.getNext();
	assertEquals(2, next.objectID);
	List<ServiceUUID> osds = this.details.getLocationList()
		.getOSDsByObject(next.objectID);
	assertEquals(osds.get(replica++), next.osd);
	assertFalse(next.requestObjectList);

	// second request
	this.strategy.selectNext();
	next = this.strategy.getNext();
	assertEquals(1, next.objectID);
	osds = this.details.getLocationList().getOSDsByObject(next.objectID);
	assertEquals(osds.get(replica++ % osds.size()), next.osd);
	assertFalse(next.requestObjectList);

	// third request
	this.strategy.selectNext();
	next = this.strategy.getNext();
	assertEquals(3, next.objectID);
	osds = this.details.getLocationList().getOSDsByObject(next.objectID);
	assertEquals(osds.get(replica++ % osds.size()), next.osd);
	assertFalse(next.requestObjectList);

	// fourth request
	this.strategy.selectNext();
	next = this.strategy.getNext();
	assertEquals(4, next.objectID);
	osds = this.details.getLocationList().getOSDsByObject(next.objectID);
	assertEquals(osds.get((replica++ % osds.size())+1), next.osd);
	assertFalse(next.requestObjectList);

	// no more requests possible
	this.strategy.selectNext();
	next = this.strategy.getNext();
	assertNull(next);
    }

    /**
     * Test method for
     * {@link org.xtreemfs.osd.replication.RandomStrategy#selectNext()}.
     */
    @Test
    public void testSelectNextForRandomTransfer() {
	this.strategy = new RandomStrategy(details);

	ArrayList<Long> objectsToRequest = new ArrayList<Long>();
	objectsToRequest.add(Long.valueOf(1));
	objectsToRequest.add(Long.valueOf(2));
	objectsToRequest.add(Long.valueOf(3));
	objectsToRequest.add(Long.valueOf(4));

	for (int i = 0; i < objectsToRequest.size(); i++) {
	    this.strategy.addRequiredObject(objectsToRequest.get(i));
	}
	this.strategy.addPreferredObject(2);

	ArrayList<Long> requestedObjects = new ArrayList<Long>();

	NextRequest next;
	for (int i = 0; i < objectsToRequest.size(); i++) {
	    this.strategy.selectNext();
	    next = this.strategy.getNext();
	    requestedObjects.add(Long.valueOf(next.objectID));
	    assertNotNull(this.details.getLocationList().getLocation(next.osd));
	    assertFalse(next.requestObjectList);
	}

	for (int i = 0; i < objectsToRequest.size(); i++)
	    assertTrue(requestedObjects.contains(objectsToRequest.get(i)));

	// no more requests possible
	this.strategy.selectNext();
	next = this.strategy.getNext();
	assertNull(next);
    }

}
