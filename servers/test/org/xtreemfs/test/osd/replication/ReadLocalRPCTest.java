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

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.HttpErrorException;
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
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.RequestController;
import org.xtreemfs.foundation.json.JSONInput;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.test.SetupUtils;

import junit.framework.TestCase;

/**
 * 
 * 12.01.2009
 * 
 * @author clorenz
 */
public class ReadLocalRPCTest extends TestCase {

    private final ServiceUUID serverID;

    private final Locations loc;

    private final String file;

    private final long objectNumber;

    private final long stripeSize;

    private final DIRConfig dirConfig;

    private final OSDConfig osdConfig;

    private DIRClient dirClient;

    private OSDClient client;

    private RequestController dir;

    private OSD osd;

    private Capability cap;

    public ReadLocalRPCTest(String testName) throws Exception {
	super(testName);

	Logging.start(Logging.LEVEL_DEBUG);

	dirConfig = SetupUtils.createDIRConfig();
	osdConfig = SetupUtils.createOSD1Config();

	stripeSize = 1;

	// It sets the loc attribute
	List<Location> locations = new ArrayList<Location>(1);
	StripingPolicy sp = new RAID0(stripeSize, 1);
	serverID = SetupUtils.getOSD1UUID();
	List<ServiceUUID> osd = new ArrayList<ServiceUUID>(1);
	osd.add(serverID);
	locations.add(new Location(sp, osd));
	loc = new Locations(locations);

	file = "1:1";
	objectNumber = 0;

	cap = new Capability(file, "DebugCapability", 0, osdConfig
		.getCapabilitySecret());
    }

    protected void setUp() throws Exception {

	System.out.println("TEST: " + getClass().getSimpleName() + "."
		+ getName());

	// cleanup
	File testDir = new File(SetupUtils.TEST_DIR);

	FSUtils.delTree(testDir);
	testDir.mkdirs();

	dir = new RequestController(dirConfig);
	dir.startup();

	dirClient = SetupUtils.initTimeSync();

	osd = new OSD(osdConfig);
	client = SetupUtils.createOSDClient(10000);
    }

    protected void tearDown() throws Exception {
	osd.shutdown();
	dir.shutdown();

	client.shutdown();
	client.waitForShutdown();

	if (dirClient != null) {
	    dirClient.shutdown();
	    dirClient.waitForShutdown();
	}
    }

    /**
     * 
     */
    public void testObjectLocallyAvailable() throws Exception {

	final String content = "Hello World";
	RPCResponse tmp = client.put(serverID.getAddress(), loc, cap, file,
		objectNumber, ReusableBuffer.wrap(content.getBytes()));
	tmp.waitForResponse();
	tmp.freeBuffers();

	RPCResponse answer = client.readLocalRPC(serverID.getAddress(), loc,
		cap, file, objectNumber);
	answer.waitForResponse();

	assertEquals(HTTPUtils.SC_OKAY, answer.getStatusCode());
	assertEquals(""+content.getBytes().length, answer.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
	assertEquals(content, new String(answer.getBody().array()));

	answer.freeBuffers();

	// check object; proper size should be returned
	answer = client.checkObject(serverID.getAddress(), loc, cap, file,
		objectNumber);
	assertEquals(String.valueOf(content.length()), answer.get().toString());

	answer.freeBuffers();
    }

    /**
     * 
     */
    public void testObjectLocallyNOTAvailable() throws Exception {
	// read object, when none have been written before
	RPCResponse answer = client.readLocalRPC(serverID.getAddress(), loc, cap, file,
		objectNumber);
	answer.waitForResponse();
	
	// TODO: enable filesize-check

	assertEquals(HTTPUtils.SC_OKAY, answer.getStatusCode());
	assertEquals(""+0, answer.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
	// get empty (JSON-)map, because object was not available
	assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), answer.getHeaders().getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	HashMap<String, Long> response = (HashMap<String, Long>) JSONParser.parseJSON(new JSONString(new String(answer.getBody().array())));
	assertEquals(0, response.size());

	answer.freeBuffers();

	// read higher object than have been written (EOF)
	final String content = "Hello World";
	answer = client.put(serverID.getAddress(), loc, cap, file,
		objectNumber, ReusableBuffer.wrap(content.getBytes()));
	answer.waitForResponse();
	answer.freeBuffers();
    
	answer = client.readLocalRPC(serverID.getAddress(), loc, cap, file,
		objectNumber+3);
	answer.waitForResponse();

	assertEquals(HTTPUtils.SC_OKAY, answer.getStatusCode());
	// get correct filesize
	assertEquals(""+content.getBytes().length, answer.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
	// get empty map, because object was not available
	assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), answer.getHeaders().getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	response = (HashMap<String, Long>) JSONParser.parseJSON(new JSONString(new String(answer.getBody().array())));
	assertEquals(0, response.size());

	// read object that haven't been written (hole)
	answer = client.put(serverID.getAddress(), loc, cap, file,
		1l, ReusableBuffer.wrap(content.getBytes()));
	answer.waitForResponse();
	answer.freeBuffers();
	answer = client.put(serverID.getAddress(), loc, cap, file,
		10l, ReusableBuffer.wrap(content.getBytes()));
	answer.waitForResponse();
	answer.freeBuffers();
    
	answer = client.readLocalRPC(serverID.getAddress(), loc, cap, file,
		5l);
	answer.waitForResponse();

	assertEquals(HTTPUtils.SC_OKAY, answer.getStatusCode());
	// get correct filesize
	assertEquals(""+(10l*stripeSize*1024+content.getBytes().length), answer.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
	// get empty map, because object was not available
	assertEquals(HTTPUtils.DATA_TYPE.JSON.toString(), answer.getHeaders().getHeader(HTTPHeaders.HDR_CONTENT_TYPE));
	response = (HashMap<String, Long>) JSONParser.parseJSON(new JSONString(new String(answer.getBody().array())));
	assertEquals(0, response.size());
    }

}
