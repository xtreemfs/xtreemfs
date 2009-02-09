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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.clients.osd.OSDClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.common.striping.RAID0;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.dir.RequestController;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.test.SetupUtils;

/**
 * 
 * 29.01.2009
 * 
 * @author clorenz
 */
public class ReplicationTest extends TestCase {
    RequestController dir;
    DIRClient dirClient;
    OSD[] osds;
    OSDClient client;

    private Capability capability;
    private String file;
    private Locations locations;

    // needed for dummy classes
    private int stripeSize;
    private ReusableBuffer data;

    private long objectNo;

    public ReplicationTest() {
	super();
	// Auto-generated constructor stub
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
	System.out.println("TEST: " + getClass().getSimpleName() + "."
		+ getName());
	Logging.start(SetupUtils.DEBUG_LEVEL);

	this.stripeSize = 128;
	this.data = generateData(stripeSize * 1024);

	System.out.println("TEST: " + getClass().getSimpleName() + "."
		+ getName());

	// cleanup
	File testDir = new File(SetupUtils.TEST_DIR);

	FSUtils.delTree(testDir);
	testDir.mkdirs();

	dir = new RequestController(SetupUtils.createDIRConfig());
	dir.startup();

	dirClient = SetupUtils.initTimeSync();

	osds = new OSD[4];
	osds[0] = new OSD(SetupUtils.createOSD1Config());
	osds[1] = new OSD(SetupUtils.createOSD2Config());
	osds[2] = new OSD(SetupUtils.createOSD3Config());
	osds[3] = new OSD(SetupUtils.createOSD4Config());
	client = SetupUtils.createOSDClient(1000000);

	file = "1:1";
	objectNo = 0;
	capability = new Capability(this.file, "read", 0, SetupUtils
		.createOSD1Config().getCapabilitySecret());

	ArrayList<Location> locationList = new ArrayList<Location>();
	// add available osds
	List<ServiceUUID> osds = new ArrayList<ServiceUUID>();
	osds.add(SetupUtils.getOSD1UUID());
	osds.add(SetupUtils.getOSD2UUID());
	List<ServiceUUID> osds2 = new ArrayList<ServiceUUID>();
	osds2.add(SetupUtils.getOSD3UUID());
	osds2.add(SetupUtils.getOSD4UUID());

	locationList
		.add(new Location(new RAID0(stripeSize, osds.size()), osds));
	locationList.add(new Location(new RAID0(stripeSize, osds2.size()),
		osds2));
	this.locations = new Locations(locationList);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
	for (OSD osd : this.osds)
	    osd.shutdown();
	dir.shutdown();

	client.shutdown();
	client.waitForShutdown();

	if (dirClient != null) {
	    dirClient.shutdown();
	    dirClient.waitForShutdown();
	}
    }

    public void testStripedReplication() throws Exception {
	// write object to replica 1
	RPCResponse response = client.put(locations.getOSDsByObject(objectNo)
		.get(0).getAddress(), this.locations, this.capability,
		this.file, objectNo, this.data);
	response.waitForResponse();
	response.freeBuffers();

	// read object from replica 1 (object exists on this OSD) => normal read
	response = client.get(locations.getOSDsByObject(objectNo).get(0)
		.getAddress(), this.locations, this.capability, this.file,
		objectNo);
	response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	assertEquals(HTTPUtils.DATA_TYPE.BINARY.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	assertEquals(this.data.getBuffer(), response.getBody().getBuffer());
	response.freeBuffers();

	// read object from replica 2 (object not exists on this OSD) =>
	// replication
	response = client.get(locations.getOSDsByObject(objectNo).get(1)
		.getAddress(), this.locations, this.capability, this.file,
		objectNo);
	response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	assertEquals(this.data.getBuffer(), response.getBody().getBuffer());
	response.freeBuffers();
    }

    /*
     * test RPC
     */
    /**
     * striped case
     */
    public void testObjectLocalAvailable() throws Exception {
	ServiceUUID serverID = locations.getOSDsByObject(objectNo).get(0);

	RPCResponse tmp = client.put(serverID.getAddress(), locations,
		capability, file, objectNo, data);
	tmp.waitForResponse();
	tmp.freeBuffers();

	RPCResponse answer = client.readLocalRPC(serverID.getAddress(),
		locations, capability, file, objectNo);
	answer.waitForResponse();

	assertEquals(HTTPUtils.SC_OKAY, answer.getStatusCode());
	assertEquals("" + data.limit(), answer.getHeaders().getHeader(
		HTTPHeaders.HDR_XNEWFILESIZE));
	assertEquals(HTTPUtils.DATA_TYPE.BINARY.toString(), answer.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	assertEquals(data.getBuffer(), answer.getBody().getBuffer());

	answer.freeBuffers();

	// check object; proper size should be returned
	answer = client.checkObject(serverID.getAddress(), locations,
		capability, file, objectNo);
	assertEquals(String.valueOf(data.limit()), answer.get().toString());

	answer.freeBuffers();
    }

    /**
     * striped case
     */
    public void testObjectLocalNOTAvailable() throws Exception {
	ServiceUUID serverID = locations.getOSDsByObject(objectNo).get(0);

	// read object, when none have been written before
	RPCResponse answer = client.readLocalRPC(serverID.getAddress(),
		locations, capability, file, objectNo);
	answer.waitForResponse();

	assertEquals(HTTPUtils.SC_OKAY, answer.getStatusCode());
	assertEquals("" + 0, answer.getHeaders().getHeader(
		HTTPHeaders.HDR_XNEWFILESIZE));
	// get empty (JSON-)map, because object was not available
	assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), answer.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	HashMap<String, Long> response = (HashMap<String, Long>) JSONParser
		.parseJSON(new JSONString(new String(answer.getBody().array())));
	assertEquals(0, response.size());

	answer.freeBuffers();

	// read higher object than have been written (EOF)
	final String content = "Hello World";
	answer = client.put(serverID.getAddress(), locations, capability, file,
		objectNo, ReusableBuffer.wrap(content.getBytes()));
	answer.waitForResponse();
	answer.freeBuffers();

	answer = client.readLocalRPC(serverID.getAddress(), locations,
		capability, file, objectNo + 3);
	answer.waitForResponse();

	assertEquals(HTTPUtils.SC_OKAY, answer.getStatusCode());
	// get correct filesize
	assertEquals("" + content.getBytes().length, answer.getHeaders()
		.getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
	// get empty map, because object was not available
	assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), answer.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	response = (HashMap<String, Long>) JSONParser.parseJSON(new JSONString(
		new String(answer.getBody().array())));
	assertEquals(0, response.size());

	// read object that has not been written (hole)
	answer = client.put(serverID.getAddress(), locations, capability, file,
		1l, ReusableBuffer.wrap(content.getBytes()));
	answer.waitForResponse();
	answer.freeBuffers();
	answer = client.put(serverID.getAddress(), locations, capability, file,
		10l, ReusableBuffer.wrap(content.getBytes()));
	answer.waitForResponse();
	answer.freeBuffers();

	answer = client.readLocalRPC(serverID.getAddress(), locations,
		capability, file, 5l);
	answer.waitForResponse();

	assertEquals(HTTPUtils.SC_OKAY, answer.getStatusCode());
	// get correct filesize
	assertEquals(
		"" + (10l * stripeSize * 1024 + content.getBytes().length),
		answer.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
	// get empty map, because object was not available
	assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), answer.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	response = (HashMap<String, Long>) JSONParser.parseJSON(new JSONString(
		new String(answer.getBody().array())));
	assertEquals(0, response.size());
    }

    public void testCorrectFilesizeUpdate() throws Exception {
	final String content = "Hello World";

	// write object to replica 1 : OSD 1
	RPCResponse answer = client.put(locations.getOSDsByObject(0).get(0).getAddress(), locations, capability, file,
		0, data);
	answer.waitForResponse();
	answer.freeBuffers();
	// write object to replica 1 : OSD 2
	answer = client.put(locations.getOSDsByObject(1).get(0).getAddress(), locations, capability, file,
		1, data);
	answer.waitForResponse();
	answer.freeBuffers();

	// read object (filesize) from replica 1 : OSD 1
	answer = client.readLocalRPC(locations.getOSDsByObject(0).get(0)
		.getAddress(), locations, capability, file, 0);
	answer.waitForResponse();

	assertEquals(HTTPUtils.SC_OKAY, answer.getStatusCode());
	// get correct filesize
	assertEquals("" + (1 * stripeSize * 1024 + data.limit()), answer.getHeaders().getHeader(
		HTTPHeaders.HDR_XNEWFILESIZE));
	assertEquals(data.getBuffer(), answer.getBody().getBuffer());
	
	// read object (filesize) from replica 2 : OSD 1
	answer = client.readLocalRPC(locations.getOSDsByObject(0).get(1)
		.getAddress(), locations, capability, file, 0);
	answer.waitForResponse();

	assertEquals(HTTPUtils.SC_OKAY, answer.getStatusCode());
	// get unknown filesize (replica cannot know the filesize, because nothing has been replicated so far)
	assertEquals("" + 0, answer.getHeaders().getHeader(
		HTTPHeaders.HDR_XNEWFILESIZE));
	// get empty map, because object was not available
	assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), answer.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	HashMap<String, Long> response = (HashMap<String, Long>) JSONParser.parseJSON(new JSONString(
		new String(answer.getBody().array())));
	assertEquals(0, response.size());

	// TODO: normal read to replica 2 : OSD 1 (so the object will be replicated),
	// then readRPC to replica 2 : OSD 2 => filesize must be correct
    }
    
    public void testObjectLocalAvailableNOTStriped() throws Exception {
	ArrayList<Location> locationList = new ArrayList<Location>();
	// add available osds
	List<ServiceUUID> osds = new ArrayList<ServiceUUID>();
	osds.add(SetupUtils.getOSD1UUID());
	List<ServiceUUID> osds2 = new ArrayList<ServiceUUID>();
	osds2.add(SetupUtils.getOSD3UUID());

	locationList
		.add(new Location(new RAID0(stripeSize, osds.size()), osds));
	locationList.add(new Location(new RAID0(stripeSize, osds2.size()),
		osds2));
	this.locations = new Locations(locationList);

	// reuse test
	testObjectLocalAvailable();
    }

    public void testObjectLocalNOTAvailableNOTStriped() throws Exception {
	ArrayList<Location> locationList = new ArrayList<Location>();
	// add available osds
	List<ServiceUUID> osds = new ArrayList<ServiceUUID>();
	osds.add(SetupUtils.getOSD1UUID());
	List<ServiceUUID> osds2 = new ArrayList<ServiceUUID>();
	osds2.add(SetupUtils.getOSD3UUID());

	locationList
		.add(new Location(new RAID0(stripeSize, osds.size()), osds));
	locationList.add(new Location(new RAID0(stripeSize, osds2.size()),
		osds2));
	this.locations = new Locations(locationList);

	// reuse test
	testObjectLocalNOTAvailable();
    }

    /**
     * @param size
     *            in byte
     * @return
     */
    private ReusableBuffer generateData(int size) {
	Random random = new Random();
	ReusableBuffer data = BufferPool.allocate(size);
	random.nextBytes(data.getData());
	return data;
    }

}
