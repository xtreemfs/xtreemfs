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
    OSDConfig[] configs;
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
	Logging.start(Logging.LEVEL_TRACE);

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
	
	osds = new OSD[12];
	configs = SetupUtils.createMultipleOSDConfigs(12);
	for(int i=0; i< osds.length; i++){
	    osds[i] = new OSD(configs[i]);
	}

	client = SetupUtils.createOSDClient(10000);

	file = "1:1";
	objectNo = 0;
	capability = new Capability(this.file, "read", 0, configs[0].getCapabilitySecret());

	locations = createLocations(4,3);
    }

    /**
     * 
     */
    private Locations createLocations(int numberOfReplicas, int numberOfStripedOSDs) {
	assert(numberOfReplicas*numberOfStripedOSDs <= osds.length);

	ArrayList<Location> locationList = new ArrayList<Location>();
	for(int replica=0; replica<numberOfReplicas; replica++) {
	    List<ServiceUUID> osds = new ArrayList<ServiceUUID>();
	    int startOSD = replica*numberOfStripedOSDs;
	    for(int stripe=0; stripe<numberOfStripedOSDs; stripe++) {
		// add available osds
		osds.add(configs[startOSD+stripe].getUUID());
	    }
	    locationList.add(new Location(new RAID0(stripeSize, osds.size()), osds));
	}
	return new Locations(locationList);
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

    public void testStriped() throws Exception {
	// write object to replica 3
	RPCResponse response = client.put(locations.getOSDsByObject(objectNo)
		.get(2).getAddress(), this.locations, this.capability,
		this.file, objectNo, this.data);
	response.waitForResponse();
	response.freeBuffers();

	// read object from replica 3 (object exists on this OSD) => normal read
	response = client.get(locations.getOSDsByObject(objectNo).get(2)
		.getAddress(), this.locations, this.capability, this.file,
		objectNo);
	response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	assertEquals(HTTPUtils.DATA_TYPE.BINARY.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	assertEquals(this.data.getBuffer(), response.getBody().getBuffer());
	response.freeBuffers();

	// read object from replica 2 (object not exists on this OSD)
	// => replication
	response = client.get(locations.getOSDsByObject(objectNo).get(1)
		.getAddress(), this.locations, this.capability, this.file,
		objectNo);
	response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	assertEquals(HTTPUtils.DATA_TYPE.BINARY.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	assertEquals(this.data.getBuffer(), response.getBody().getBuffer());
	response.freeBuffers();
	
	// read object from replica 4 (object not exists on this OSD)
	// => replication
	response = client.get(locations.getOSDsByObject(objectNo).get(3)
		.getAddress(), this.locations, this.capability, this.file,
		objectNo);
	response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	assertEquals(HTTPUtils.DATA_TYPE.BINARY.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	assertEquals(this.data.getBuffer(), response.getBody().getBuffer());
	response.freeBuffers();
    }
    
    public void testHoleAndEOF() throws Exception {
	// write object to replica 1 => full object
	RPCResponse response = client.put(locations.getOSDsByObject(objectNo)
		.get(0).getAddress(), this.locations, this.capability,
		this.file, objectNo, this.data);
	response.waitForResponse();
	response.freeBuffers();

	ReusableBuffer data2 = generateData(1024*this.stripeSize/2);
	// write half object to replica 1 at quarter position => half object, HOLE
	response = client.put(locations.getOSDsByObject(objectNo+2)
		.get(0).getAddress(), this.locations, this.capability,
		this.file, objectNo+2, 1024*this.stripeSize/4, data2);
	response.waitForResponse();
	response.freeBuffers();

	// write half object to replica 1 => half object, EOF
	response = client.put(locations.getOSDsByObject(objectNo+3)
		.get(0).getAddress(), this.locations, this.capability,
		this.file, objectNo+3, data2);
	response.waitForResponse();
	response.freeBuffers();

	// read object from replica 2
	response = client.get(locations.getOSDsByObject(objectNo).get(1)
		.getAddress(), this.locations, this.capability, this.file,
		objectNo);
	response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	assertEquals(HTTPUtils.DATA_TYPE.BINARY.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	assertEquals(this.data.getBuffer(), response.getBody().getBuffer());
	response.freeBuffers();

	// read hole from replica 2
	response = client.get(locations.getOSDsByObject(objectNo+1).get(1)
		.getAddress(), this.locations, this.capability, this.file,
		objectNo+1);
	response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	assertEquals(HTTPUtils.DATA_TYPE.BINARY.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	// correct length
        assertEquals(data.limit(), response.getBody().limit());
	// filled with zeros
        for(byte b : response.getBody().array()) {
            assertEquals(0, b);
        }
	response.freeBuffers();
	
	// read EOF from replica 2
	response = client.get(locations.getOSDsByObject(objectNo+4).get(1)
		.getAddress(), this.locations, this.capability, this.file,
		objectNo+4);
	response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	assertNull(response.getBody());
	response.freeBuffers();
	
	// read hole within an object from replica 2
	response = client.get(locations.getOSDsByObject(objectNo+2).get(1)
		.getAddress(), this.locations, this.capability, this.file,
		objectNo+2);
	response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	assertEquals(HTTPUtils.DATA_TYPE.BINARY.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
 	// correct length
        assertEquals(this.stripeSize*1024, response.getBody().limit());
        byte[] responseData = response.getBody().array();
	// first quarter filled with zeros
        for(int i=0; i<stripeSize*1024/4; i++) {
            assertEquals((byte)0, responseData[i]);
        }
        int j=0;
	// then there is the data
        byte[] data2bytes = data2.array();
        for(int i=(stripeSize*1024/4); i<(stripeSize*1024/4)*3; i++) {
            assertEquals(data2bytes[j++], responseData[i]);
        }
	// last quarter filled with zeros again
        for(int i=(stripeSize*1024/4)*3; i<stripeSize*1024/4; i++) {
            assertEquals((byte)0, responseData[i]);
        }
	response.freeBuffers();

	// read EOF within data from replica 2
	response = client.get(locations.getOSDsByObject(objectNo+3).get(1)
		.getAddress(), this.locations, this.capability, this.file,
		objectNo+3);
	response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	assertEquals(HTTPUtils.DATA_TYPE.BINARY.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	assertEquals(data2.getBuffer(), response.getBody().getBuffer());
	response.freeBuffers();
     }


    public void testCorrectFilesize() throws Exception {
	// write object to replica 1 : OSD 1
        ServiceUUID osd = locations.getOSDsByObject(0).get(0);
        RPCResponse response = client.put(osd.getAddress(), locations, capability, file,
        	0, data);
        response.waitForResponse();
        response.freeBuffers();
        // write object to replica 1 : OSD 2
        osd = locations.getOSDsByObject(1).get(0);
        response = client.put(osd.getAddress(), locations, capability, file,
        	1, data);
        response.waitForResponse();
        response.freeBuffers();

        String fileLength = (1 * stripeSize * 1024 + data.limit())+"";

        // get correct filesize from replica 1 : OSD 1
        osd = locations.getOSDsByObject(0).get(0);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 0);
        response.waitForResponse();
        assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get correct filesize
	assertEquals(fileLength, response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
        assertEquals(data.getBuffer(), response.getBody().getBuffer());
	response.freeBuffers();

        // get unknown filesize (0) from replica 2 : OSD 1
        osd = locations.getOSDsByObject(0).get(1);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 0);
        response.waitForResponse();
        assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get unknown filesize (replica cannot know the filesize, because
        // nothing has been replicated so far)
        assertEquals(0+"", response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
        // get empty map, because object was not available
        assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders()
        	.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	response.freeBuffers();

        // get unknown filesize (0) from replica 2 : OSD 2
        osd = locations.getOSDsByObject(1).get(1);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 0);
        response.waitForResponse();
        assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get unknown filesize (replica cannot know the filesize, because
        // nothing has been replicated so far)
        assertEquals(0+"", response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
        // get empty map, because object was not available
        assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders()
        	.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	response.freeBuffers();

        // read object from replica 2 : OSD 1 (object not exists on this OSD)
        // => replication
        response = client.get(locations.getOSDsByObject(0).get(1)
        	.getAddress(), this.locations, this.capability, this.file, 0);
        response.waitForResponse();
        assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        assertEquals(this.data.getBuffer(), response.getBody().getBuffer());
        response.freeBuffers();
        
        // get correct filesize from replica 2 : OSD 1 (just written)
        osd = locations.getOSDsByObject(0).get(1);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 0);
        response.waitForResponse();
        assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get correct filesize
        assertEquals(fileLength, response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
        assertEquals(data.getBuffer(), response.getBody().getBuffer());
	response.freeBuffers();

        // get correct filesize from replica 2 : OSD 2
        osd = locations.getOSDsByObject(1).get(1);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 1);
        response.waitForResponse();
        assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get correct filesize (at least one OSD of this replica knows the correct filesize)
        assertEquals(fileLength, response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
        // get empty map, because object was not available
        assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders()
        	.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	response.freeBuffers();

        // get unknown filesize (0) from replica 3 : OSD 2
        osd = locations.getOSDsByObject(1).get(2);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 0);
        response.waitForResponse();
        assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get unknown filesize (replica cannot know the filesize, because
        // nothing has been replicated so far)
        assertEquals(0+"", response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
        // get empty map, because object was not available
        assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders()
        	.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	response.freeBuffers();

        // read object from replica 3 (object not exists on this OSD)
        // => replication
        response = client.get(locations.getOSDsByObject(1).get(2)
        	.getAddress(), this.locations, this.capability, this.file, 1);
        response.waitForResponse();
        assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        assertEquals(this.data.getBuffer(), response.getBody().getBuffer());
        response.freeBuffers();
    
        // get correct filesize from replica 3 : OSD 1
        osd = locations.getOSDsByObject(0).get(2);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 0);
        response.waitForResponse();
        assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get correct filesize (at least one OSD of this replica knows the correct filesize)
        assertEquals(fileLength, response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
        // get empty map, because object was not available
        assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders()
        	.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	response.freeBuffers();
    }

    public void testCorrectFilesizeForHole() throws Exception {
        // write object to replica 4 : OSD 1
        ServiceUUID osd = locations.getOSDsByObject(0).get(3);
        RPCResponse response = client.put(osd.getAddress(), locations, capability, file,
        	0, data);
        response.waitForResponse();
        response.freeBuffers();
        // write object to replica 4 : OSD 3
        osd = locations.getOSDsByObject(2).get(3);
        response = client.put(osd.getAddress(), locations, capability, file,
        	2, data);
        response.waitForResponse();
        response.freeBuffers();

        String fileLength = (2 * stripeSize * 1024 + data.limit())+"";

        // get correct filesize from replica 4 : OSD 3
        osd = locations.getOSDsByObject(2).get(3);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 2);
        response.waitForResponse();
        assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get correct filesize
        assertEquals(fileLength, response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
        assertEquals(data.getBuffer(), response.getBody().getBuffer());
	response.freeBuffers();

        // get correct filesize from replica 4 : OSD 2
        osd = locations.getOSDsByObject(1).get(3);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 1);
        response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get correct filesize
	assertEquals(fileLength, response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
	assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
        response.freeBuffers();

        // read object from replica 2 : OSD 2 (object not exists on this OSD)
        // => replication, but it is a hole
        response = client.get(locations.getOSDsByObject(1).get(1)
        	.getAddress(), this.locations, this.capability, this.file, 1);
        response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	// correct length
        assertEquals(this.stripeSize*1024, response.getBody().limit());
	// filled with zeros
        for(byte b : response.getBody().array()) {
            assertEquals(0, b);
        }
        response.freeBuffers();

        // get correct filesize from replica 2 : OSD 2
        osd = locations.getOSDsByObject(1).get(1);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 1);
        response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get correct filesize
	assertEquals(fileLength, response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
	assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
        response.freeBuffers();

        // read object from replica 3 : OSD 3 (object not exists on this OSD)
        // => replication
        response = client.get(locations.getOSDsByObject(2).get(2)
        	.getAddress(), this.locations, this.capability, this.file, 2);
        response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        assertEquals(data.getBuffer(), response.getBody().getBuffer());
        response.freeBuffers();

        // get correct filesize from replica 3 : OSD 2
        osd = locations.getOSDsByObject(1).get(2);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 1);
        response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get correct filesize
	assertEquals(fileLength, response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
	assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
        response.freeBuffers();

        // get unknown filesize (0) from replica 1 : OSD 1
        osd = locations.getOSDsByObject(0).get(0);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 0);
        response.waitForResponse();
        assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get unknown filesize (replica cannot know the filesize, because
        // nothing has been replicated so far)
        assertEquals(0+"", response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
        // get empty map, because object was not available
        assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders()
        	.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
        response.freeBuffers();
    }

    public void testCorrectFilesizeForEOF() throws Exception {
        // write object to replica 3 : OSD 1
        ServiceUUID osd = locations.getOSDsByObject(0).get(2);
        RPCResponse response = client.put(osd.getAddress(), locations, capability, file,
        	0, data);
        response.waitForResponse();
        response.freeBuffers();

	ReusableBuffer data2 = generateData(1024*this.stripeSize/2);
        // write object to replica 3 : OSD 2
        osd = locations.getOSDsByObject(1).get(2);
        response = client.put(osd.getAddress(), locations, capability, file,
        	1, data2);
        response.waitForResponse();
        response.freeBuffers();

        String fileLength = (1 * stripeSize * 1024 + data2.limit())+"";

        // get correct filesize from replica 3 : OSD 3
        osd = locations.getOSDsByObject(2).get(2);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 2);
        response.waitForResponse();
        
        assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get correct filesize
        assertEquals(fileLength, response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
        // get empty map, because object was not available
        assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders()
        	.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));

        // read object from replica 2 : OSD 3 (object not exists on this OSD)
        // => replication, but EOF
        response = client.get(locations.getOSDsByObject(2).get(1)
        	.getAddress(), this.locations, this.capability, this.file, 2);
        response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	// correct length
        assertNull(response.getBody());
        response.freeBuffers();

        // get correct filesize from replica 2 : OSD 3
        osd = locations.getOSDsByObject(2).get(2);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 2);
        response.waitForResponse();
        assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get correct filesize
        assertEquals(fileLength, response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
        // get empty map, because object was not available
        assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders()
        	.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));

        // read object from replica 1 : OSD 2 (object not exists on this OSD)
        // => replication
        response = client.get(locations.getOSDsByObject(1).get(0)
        	.getAddress(), this.locations, this.capability, this.file, 1);
        response.waitForResponse();
	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	// correct length
        assertEquals(data2.getBuffer(), response.getBody().getBuffer());
        response.freeBuffers();

        // get correct filesize from replica 1 : OSD 2
        osd = locations.getOSDsByObject(1).get(0);
        response = client.readLocalRPC(osd.getAddress(), locations, capability, file, 1);
        response.waitForResponse();
        assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
        // get correct filesize
        assertEquals(fileLength, response.getHeaders().getHeader(
        	HTTPHeaders.HDR_XNEWFILESIZE));
        assertEquals(data2.getBuffer(), response.getBody().getBuffer());
    }

    /*
     * following tests are testing readLocal-RPC
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

	RPCResponse response = client.readLocalRPC(serverID.getAddress(),
		locations, capability, file, objectNo);
	response.waitForResponse();

	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	assertEquals("" + data.limit()+"", response.getHeaders().getHeader(
		HTTPHeaders.HDR_XNEWFILESIZE));
	assertEquals(HTTPUtils.DATA_TYPE.BINARY.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	assertEquals(data.getBuffer(), response.getBody().getBuffer());

	response.freeBuffers();

	// check object; proper size should be returned
	response = client.checkObject(serverID.getAddress(), locations,
		capability, file, objectNo);
	assertEquals(String.valueOf(data.limit()), response.get().toString());

	response.freeBuffers();
    }

    /**
     * striped case
     */
    public void testObjectLocalNOTAvailable() throws Exception {
	ServiceUUID serverID = locations.getOSDsByObject(objectNo).get(0);

	// read object, when none have been written before
	RPCResponse response = client.readLocalRPC(serverID.getAddress(),
		locations, capability, file, objectNo);
	response.waitForResponse();

	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	assertEquals("" + 0 +"", response.getHeaders().getHeader(
		HTTPHeaders.HDR_XNEWFILESIZE));
	// get empty (JSON-)map, because object was not available
	assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	HashMap<String, Long> json = (HashMap<String, Long>) JSONParser
		.parseJSON(new JSONString(new String(response.getBody().array())));
	assertEquals(0, json.size());

	response.freeBuffers();

	// read higher object than have been written (EOF)
	final String content = "Hello World";
	response = client.put(serverID.getAddress(), locations, capability, file,
		objectNo, ReusableBuffer.wrap(content.getBytes()));
	response.waitForResponse();
	response.freeBuffers();

	response = client.readLocalRPC(serverID.getAddress(), locations,
		capability, file, objectNo + 3);
	response.waitForResponse();

	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	// get correct filesize
	assertEquals(content.getBytes().length+"", response.getHeaders()
		.getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
	// get empty map, because object was not available
	assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	json = (HashMap<String, Long>) JSONParser.parseJSON(new JSONString(
		new String(response.getBody().array())));
	assertEquals(0, json.size());

	// read object that has not been written (hole)
	response = client.put(serverID.getAddress(), locations, capability, file,
		1l, ReusableBuffer.wrap(content.getBytes()));
	response.waitForResponse();
	response.freeBuffers();
	response = client.put(serverID.getAddress(), locations, capability, file,
		10l, ReusableBuffer.wrap(content.getBytes()));
	response.waitForResponse();
	response.freeBuffers();

	response = client.readLocalRPC(serverID.getAddress(), locations,
		capability, file, 5l);
	response.waitForResponse();

	assertEquals(HTTPUtils.SC_OKAY, response.getStatusCode());
	// get correct filesize
	assertEquals((10l * stripeSize * 1024 + content.getBytes().length)+"",
		response.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
	// get empty map, because object was not available
	assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), response.getHeaders()
		.getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	json = (HashMap<String, Long>) JSONParser.parseJSON(new JSONString(
		new String(response.getBody().array())));
	assertEquals(0, json.size());
    }

    public void testObjectLocalAvailableNONStriped() throws Exception {
	this.locations = createLocations(2, 1);
	// reuse test
	testObjectLocalAvailable();
    }

    public void testObjectLocalNOTAvailableNONStriped() throws Exception {
	this.locations = createLocations(2, 1);

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
	byte[] data = new byte[size];
	random.nextBytes(data);
	return ReusableBuffer.wrap(data);
    }
}
