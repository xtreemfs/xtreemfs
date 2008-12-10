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

package org.xtreemfs.test.osd;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.Request;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.common.striping.RAID0;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.ErrorRecord.ErrorClass;
import org.xtreemfs.osd.ops.Operation;
import org.xtreemfs.osd.stages.ParserStage;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.stages.StageStatistics;
import org.xtreemfs.test.SetupUtils;

/**
 * This class implements the tests for the ParserStage
 *
 * @author jmalo
 */
public class ParserStageTest extends TestCase {

    ServiceUUID           osdId;

    ServiceUUID           wrongOSDId, unavailOSD;

    private final long    stripeSize;

    private Locations     loc;

    private Locations     wrongLoc;

    private Capability    cap;

    private String        file;

    ParserStage           stage;

    TestRequestDispatcher master;

    boolean               finished;

    /** Creates a new instance of ParserStageTest */
    public ParserStageTest(String testName) throws Exception {
        super(testName);

        Logging.start(Logging.LEVEL_DEBUG);

        OSDConfig config = SetupUtils.createOSD1Config();
        master = new TestRequestDispatcher(config);

        osdId = config.getUUID();

        stripeSize = 1;

        wrongOSDId = new ServiceUUID("www.google.es");

        unavailOSD = new ServiceUUID("osdX-uuid");

        stage = new ParserStage(master);

        // It sets the always required objects
        file = new String("1:1");

        List<Location> locations = new ArrayList<Location>(1);
        List<Location> wrongLocations = new ArrayList<Location>(1);
        StripingPolicy sp1 = new RAID0(stripeSize, 1);
        StripingPolicy sp2 = new RAID0(stripeSize, 2);
        List<ServiceUUID> osd = new ArrayList<ServiceUUID>(1);
        List<ServiceUUID> wrongOsd = new ArrayList<ServiceUUID>(1);
        osd.add(osdId);
        osd.add(unavailOSD);
        wrongOsd.add(wrongOSDId);
        locations.add(new Location(sp2, osd));
        wrongLocations.add(new Location(sp1, wrongOsd));
        loc = new Locations(locations);
        wrongLoc = new Locations(wrongLocations);

        cap = new Capability(file, "read", 0, config.getCapabilitySecret());
    }

    protected void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        stage.start();

        SetupUtils.setupLocalResolver();
        UUIDResolver.addLocalMapping("osdX-uuid", 45454, SetupUtils.SSL_ON);
    }

    protected void tearDown() throws Exception {
        stage.shutdown();
        stage.waitForShutdown();
    }

    public void testParseGetRequest() throws Exception {

    	OSDRequest rq = new OSDRequest(0);
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, "[[], 1,\"lazy\"]");
        String uri = null;
        if (file != null)
            uri = file;
        rq.setPinkyRequest(new PinkyRequest(HTTPUtils.GET_TOKEN, uri, null, headers));

        finished = false;
        stage.enqueueOperation(rq, ParserStage.STAGEOP_PARSE, null);
        synchronized (this) {
            while (!finished)
                wait();
        }

        // expected: X-Loc cache miss
        assertEquals(ErrorClass.USER_EXCEPTION, rq.getError().getErrorClass());
        assertEquals(ErrorCodes.NEED_FULL_XLOC, rq.getError().getErrorCode());

        rq = new OSDRequest(0);
        rq.setPinkyRequest(generateGetRequest(loc, cap, file));

        finished = false;
        stage.enqueueOperation(rq, ParserStage.STAGEOP_PARSE, null);
        synchronized (this) {
            while (!finished)
                wait();
        }

        // expected: parsing successful
        System.out.println(rq.getError());
        assertNull(rq.getError());
        assertEquals(OSDRequest.Type.READ, rq.getType());
        assertEquals(file, rq.getDetails().getFileId());
        assertEquals(loc, rq.getDetails().getLocationList());
        assertEquals(cap.toString(), rq.getDetails().getCapability().toString());
        assertEquals(loc.getLocation(osdId), rq.getDetails().getCurrentReplica());
        assertEquals(loc.getLocation(osdId).getStripingPolicy(), rq.getDetails()
                .getCurrentReplica().getStripingPolicy());

        // Wrong use cases
        List<PinkyRequest> erroneousRequests = new ArrayList<PinkyRequest>();
        erroneousRequests.add(generateGetRequest(loc, cap, null));
        erroneousRequests.add(generateGetRequest(wrongLoc, cap, file + "4"));

        checkFailureCases(erroneousRequests);
    }

    // /**
    // * It tests the parsing of ReadWhole requests
    // */
    // public void testReadWhole() throws Exception {
    //
    // long objectNumber = 0;
    //
    // // Right use case
    // {
    // Request rq = new Request(generateReadWholeRequest(loc
    // .getSummarized(), cap, file, objectNumber));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.X_LOC_CACHEMISS, rq.getStatus());
    //
    // rq = new Request(generateReadWholeRequest(loc, cap, file,
    // objectNumber));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.PARSED, rq.getStatus());
    // assertTrue(rq.getOperation() instanceof ReadWhole);
    // assertEquals(OperationType.READ, rq.getOSDOperation().getOpType());
    // assertEquals(OperationSubType.WHOLE, rq.getOSDOperation()
    // .getOpSubType());
    // assertEquals(file, rq.getFileId());
    // assertEquals(loc, rq.getLocations());
    // assertEquals(cap.toString(), rq.getCapability().toString());
    // assertEquals(loc.getLocation(osdId), rq.getLocation());
    // assertEquals(loc.getLocation(osdId).getStripingPolicy(), rq
    // .getPolicy());
    // assertEquals(objectNumber, rq.getObjectNo());
    // }
    //
    // // Wrong use cases
    // List<PinkyRequest> wrongOnes = new ArrayList<PinkyRequest>();
    // wrongOnes.add(generateReadWholeRequest(loc, cap, null, objectNumber));
    // wrongOnes.add(generateReadWholeRequest(loc, null, file + "2",
    // objectNumber));
    // wrongOnes.add(generateReadWholeRequest(null, cap, file + "3",
    // objectNumber));
    // wrongOnes.add(generateReadWholeRequest(wrongLoc, cap, file + "4",
    // objectNumber));
    // wrongOnes.add(generateReadWholeRequest(loc, cap, file + "5",
    // objectNumber + 1));
    //
    // checkWrongCases(wrongOnes);
    // }
    //
    // /**
    // * It tests the parsing of ReadRange requests
    // */
    // public void testReadRange() throws Exception {
    //
    // long objectNumber = 0;
    // long[] range = { 0, 1 };
    //
    // // Right use case
    // {
    // Request rq = new Request(generateReadRangeRequest(loc
    // .getSummarized(), cap, file, objectNumber, range));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.X_LOC_CACHEMISS, rq.getStatus());
    //
    // rq = new Request(generateReadRangeRequest(loc, cap, file,
    // objectNumber, range));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.PARSED, rq.getStatus());
    // assertTrue(rq.getOperation() instanceof ReadRange);
    // assertEquals(OperationType.READ, rq.getOSDOperation().getOpType());
    // assertEquals(OperationSubType.RANGE, rq.getOSDOperation()
    // .getOpSubType());
    // assertEquals(file, rq.getFileId());
    // assertEquals(loc, rq.getLocations());
    // assertEquals(cap.toString(), rq.getCapability().toString());
    // assertEquals(loc.getLocation(osdId), rq.getLocation());
    // assertEquals(loc.getLocation(osdId).getStripingPolicy(), rq
    // .getPolicy());
    // assertEquals(objectNumber, rq.getObjectNo());
    // assertEquals(range[0], rq.getByteRange()[0]);
    // assertEquals(range[1], rq.getByteRange()[1]);
    // }
    //
    // // Wrong use cases
    // List<PinkyRequest> wrongOnes = new ArrayList<PinkyRequest>();
    // wrongOnes.add(generateReadRangeRequest(loc, cap, null, objectNumber,
    // range));
    // wrongOnes.add(generateReadRangeRequest(loc, null, file + "2",
    // objectNumber, range));
    // wrongOnes.add(generateReadRangeRequest(null, cap, file + "3",
    // objectNumber, range));
    // wrongOnes.add(generateReadRangeRequest(wrongLoc, cap, file + "4",
    // objectNumber, range));
    // wrongOnes.add(generateReadRangeRequest(loc, cap, file + "5",
    // objectNumber + 1, range));
    // long[][] wrongRanges = { { 1, 0 }, { -1, 0 }, { 0, -1 }, { -1, -1 },
    // { 0, 1 + Integer.MAX_VALUE } };
    // for (long[] w : wrongRanges) {
    // wrongOnes.add(generateReadRangeRequest(loc, cap, file + "6",
    // objectNumber, w));
    // }
    //
    // checkWrongCases(wrongOnes);
    // }
    //
    // /**
    // * It tests the parsing of WriteWhole requests
    // */
    // public void testWriteWhole() throws Exception {
    //
    // long objectNumber = 0;
    // byte[] data = new byte[2];
    // data[0] = 'A';
    // data[1] = 'Z';
    //
    // // Right use case
    // {
    // Request rq = new Request(generateWriteWholeRequest(loc
    // .getSummarized(), cap, file, objectNumber, data));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.X_LOC_CACHEMISS, rq.getStatus());
    //
    // rq = new Request(generateWriteWholeRequest(loc, cap, file,
    // objectNumber, data));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.PARSED, rq.getStatus());
    // assertTrue(rq.getOperation() instanceof WriteWhole);
    // assertEquals(OperationType.WRITE, rq.getOSDOperation().getOpType());
    // assertEquals(OperationSubType.WHOLE, rq.getOSDOperation()
    // .getOpSubType());
    // assertEquals(file, rq.getFileId());
    // assertEquals(loc, rq.getLocations());
    // assertEquals(cap.toString(), rq.getCapability().toString());
    // assertEquals(loc.getLocation(osdId), rq.getLocation());
    // assertEquals(loc.getLocation(osdId).getStripingPolicy(), rq
    // .getPolicy());
    // assertEquals(objectNumber, rq.getObjectNo());
    // }
    //
    // // Wrong use cases
    // List<PinkyRequest> wrongOnes = new ArrayList<PinkyRequest>();
    // wrongOnes.add(generateWriteWholeRequest(loc, cap, null, objectNumber,
    // data));
    // wrongOnes.add(generateWriteWholeRequest(loc, null, file + "2",
    // objectNumber, data));
    // wrongOnes.add(generateWriteWholeRequest(null, cap, file + "3",
    // objectNumber, data));
    // wrongOnes.add(generateWriteWholeRequest(loc, cap, file + "4", null,
    // data));
    // wrongOnes.add(generateWriteWholeRequest(loc, cap, file + "5",
    // objectNumber, null));
    // wrongOnes.add(generateWriteWholeRequest(wrongLoc, cap, file + "6",
    // objectNumber, data));
    // wrongOnes.add(generateWriteWholeRequest(loc, cap, file + "7",
    // objectNumber + 1, data));
    //
    // checkWrongCases(wrongOnes);
    // }
    //
    // /**
    // * It tests the parsing of WriteRange requests
    // */
    // public void testWriteRange() throws Exception {
    //
    // long objectNumber = 0;
    // long[] range = { 0, 1 };
    // byte[] data = new byte[2];
    // data[0] = 'A';
    // data[1] = 'Z';
    //
    // // Right use case
    // {
    // Request rq = new Request(generateWriteRangeRequest(loc
    // .getSummarized(), cap, file, objectNumber, range, data));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.X_LOC_CACHEMISS, rq.getStatus());
    //
    // rq = new Request(generateWriteRangeRequest(loc, cap, file,
    // objectNumber, range, data));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.PARSED, rq.getStatus());
    // assertTrue(rq.getOperation() instanceof WriteRange);
    // assertEquals(OperationType.WRITE, rq.getOSDOperation().getOpType());
    // assertEquals(OperationSubType.RANGE, rq.getOSDOperation()
    // .getOpSubType());
    // assertEquals(file, rq.getFileId());
    // assertEquals(loc, rq.getLocations());
    // assertEquals(cap.toString(), rq.getCapability().toString());
    // assertEquals(loc.getLocation(osdId), rq.getLocation());
    // assertEquals(loc.getLocation(osdId).getStripingPolicy(), rq
    // .getPolicy());
    // assertEquals(objectNumber, rq.getObjectNo());
    // assertEquals(range[0], rq.getByteRange()[0]);
    // assertEquals(range[1], rq.getByteRange()[1]);
    // }
    //
    // // Wrong use cases
    // List<PinkyRequest> wrongOnes = new ArrayList<PinkyRequest>();
    // wrongOnes.add(generateWriteRangeRequest(loc, cap, null, objectNumber,
    // range, data));
    // wrongOnes.add(generateWriteRangeRequest(loc, null, file + "2",
    // objectNumber, range, data));
    // wrongOnes.add(generateWriteRangeRequest(null, cap, file + "3",
    // objectNumber, range, data));
    // wrongOnes.add(generateWriteRangeRequest(loc, cap, file + "4", null,
    // range, data));
    // wrongOnes.add(generateWriteRangeRequest(loc, cap, file + "5",
    // objectNumber, range, null));
    // wrongOnes.add(generateWriteRangeRequest(wrongLoc, cap, file + "6",
    // objectNumber, range, data));
    // wrongOnes.add(generateWriteRangeRequest(loc, cap, file + "7",
    // objectNumber + 1, range, data));
    // long[][] wrongRanges = { { 1, 0 }, { -1, 0 }, { 0, -1 }, { -1, -1 },
    // { 0, 1 + Integer.MAX_VALUE } };
    // for (long[] w : wrongRanges) {
    // wrongOnes.add(generateWriteRangeRequest(loc, cap, file + "8",
    // objectNumber, w, data));
    // }
    //
    // checkWrongCases(wrongOnes);
    // }
    //
    // /**
    // * It tests the parsing of DeleteWhole requests
    // */
    // public void testDeleteWhole() throws Exception {
    //
    // // Right use case
    // {
    // Request rq = new Request(generateDeleteWholeRequest(loc
    // .getSummarized(), cap, file));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.X_LOC_CACHEMISS, rq.getStatus());
    //
    // rq = new Request(generateDeleteWholeRequest(loc, cap, file));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.PARSED, rq.getStatus());
    // assertTrue(rq.getOperation() instanceof DeleteWhole);
    // assertEquals(OperationType.DELETE, rq.getOSDOperation().getOpType());
    // assertEquals(OperationSubType.WHOLE, rq.getOSDOperation()
    // .getOpSubType());
    // assertEquals(file, rq.getFileId());
    // assertEquals(loc, rq.getLocations());
    // assertEquals(cap.toString(), rq.getCapability().toString());
    // assertEquals(loc.getLocation(osdId), rq.getLocation());
    // assertEquals(loc.getLocation(osdId).getStripingPolicy(), rq
    // .getPolicy());
    // }
    //
    // // Wrong use cases
    // List<PinkyRequest> wrongOnes = new ArrayList<PinkyRequest>();
    // wrongOnes.add(generateDeleteWholeRequest(loc, cap, null));
    // wrongOnes.add(generateDeleteWholeRequest(loc, null, file + "2"));
    // wrongOnes.add(generateDeleteWholeRequest(null, cap, file + "3"));
    // wrongOnes.add(generateDeleteWholeRequest(wrongLoc, cap, file + "4"));
    //
    // checkWrongCases(wrongOnes);
    // }
    //
    // /**
    // * It tests the parsing of RPCFileSize requests
    // */
    // public void testFetchGlobalMax() throws Exception {
    //
    // // Right use case
    // {
    // Request rq = new Request(generateRPCGlobalMaxRequest(loc
    // .getSummarized(), cap, file));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.X_LOC_CACHEMISS, rq.getStatus());
    //
    // rq = new Request(generateRPCGlobalMaxRequest(loc, cap, file));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.PARSED, rq.getStatus());
    // assertTrue(rq.getOperation() instanceof RPCFileSize);
    // assertEquals(OperationType.RPC, rq.getOSDOperation().getOpType());
    // assertEquals(OperationSubType.FETCH_GLOBAL_MAX, rq
    // .getOSDOperation().getOpSubType());
    // assertEquals(cap.toString(), rq.getCapability().toString());
    // }
    //
    // // Wrong use cases
    // List<PinkyRequest> wrongOnes = new ArrayList<PinkyRequest>();
    // wrongOnes.add(generateRPCGlobalMaxRequest(null, cap, file + "2"));
    // wrongOnes.add(generateRPCGlobalMaxRequest(loc, null, file + "3"));
    // wrongOnes.add(generateRPCGlobalMaxRequest(loc, cap, null));
    // wrongOnes.add(generateRPCGlobalMaxRequest(wrongLoc, cap, file + "4"));
    //
    // checkWrongCases(wrongOnes);
    // }
    //
    // /**
    // * It tests the parsing of RPCTruncate requests
    // */
    // public void testTruncate() throws Exception {
    //
    // long size = 1;
    // // Right use case
    // {
    // Request rq = new Request(generateRPCTruncateRequest(loc
    // .getSummarized(), cap, file, size));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.X_LOC_CACHEMISS, rq.getStatus());
    //
    // rq = new Request(generateRPCTruncateRequest(loc, cap, file, size));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.PARSED, rq.getStatus());
    // assertTrue(rq.getOperation() instanceof RPCTruncate);
    // assertEquals(OperationType.RPC, rq.getOSDOperation().getOpType());
    // assertEquals(OperationSubType.TRUNCATE, rq.getOSDOperation()
    // .getOpSubType());
    // assertEquals(file, rq.getFileId());
    // assertEquals(loc, rq.getLocations());
    // assertEquals(cap.toString(), rq.getCapability().toString());
    // assertEquals(loc.getLocation(osdId), rq.getLocation());
    // assertEquals(loc.getLocation(osdId).getStripingPolicy(), rq
    // .getPolicy());
    // }
    //
    // // Wrong use cases
    // List<PinkyRequest> wrongOnes = new ArrayList<PinkyRequest>();
    // wrongOnes.add(generateRPCTruncateRequest(null, cap, file + "2", size));
    // wrongOnes.add(generateRPCTruncateRequest(loc, null, file + "3", size));
    // wrongOnes.add(generateRPCTruncateRequest(loc, cap, null, size));
    // wrongOnes.add(generateRPCTruncateRequest(loc, cap, file + "4", null));
    // wrongOnes.add(generateRPCTruncateRequest(loc, cap, null, null));
    // wrongOnes.add(generateRPCTruncateRequest(loc, cap, file + "5", -size));
    // wrongOnes.add(generateRPCTruncateRequest(wrongLoc, cap, file + "6",
    // size));
    //
    // checkWrongCases(wrongOnes);
    // }
    //
    // /**
    // * It tests the parsing of RPCDeleteReplica requests
    // */
    // public void testDeleteReplica() throws Exception {
    //
    // // Right use case
    // {
    // Request rq = new Request(generateRPCDeleteReplicaRequest(cap, file));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.PARSED, rq.getStatus());
    // assertTrue(rq.getOperation() instanceof RPCDeleteReplica);
    // assertEquals(OperationType.RPC, rq.getOSDOperation().getOpType());
    // assertEquals(OperationSubType.DELETE_REPLICA, rq.getOSDOperation()
    // .getOpSubType());
    // assertEquals(file, rq.getFileId());
    // assertEquals(cap.toString(), rq.getCapability().toString());
    // }
    //
    // // Wrong use cases
    // List<PinkyRequest> wrongOnes = new ArrayList<PinkyRequest>();
    // wrongOnes.add(generateRPCDeleteReplicaRequest(null, file + "2"));
    // wrongOnes.add(generateRPCDeleteReplicaRequest(cap, null));
    //
    // checkWrongCases(wrongOnes);
    // }
    //
    // /**
    // * It tests the parsing of RPCDeleteReplica requests
    // */
    // public void testTruncateReplica() throws Exception {
    //
    // // Right use case
    // long size = 1;
    // {
    // Request rq = new Request(generateRPCTruncateReplicaRequest(loc
    // .getSummarized(), cap, file, size));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.X_LOC_CACHEMISS, rq.getStatus());
    //
    // rq = new Request(generateRPCTruncateReplicaRequest(loc, cap, file,
    // size));
    //
    // stage.enqueueRequest(rq);
    // waitParsedRequest();
    //
    // assertEquals(Request.Status.PARSED, rq.getStatus());
    // assertTrue(rq.getOperation() instanceof RPCTruncateReplica);
    // assertEquals(OperationType.RPC, rq.getOSDOperation().getOpType());
    // assertEquals(OperationSubType.TRUNCATE_REPLICA, rq
    // .getOSDOperation().getOpSubType());
    // assertEquals(file, rq.getFileId());
    // assertEquals(cap.toString(), rq.getCapability().toString());
    // assertEquals(loc, rq.getLocations());
    // assertEquals(loc.getLocation(osdId), rq.getLocation());
    // assertEquals(loc.getLocation(osdId).getStripingPolicy(), rq
    // .getPolicy());
    // }
    //
    // // Wrong use cases
    // List<PinkyRequest> wrongOnes = new ArrayList<PinkyRequest>();
    // wrongOnes.add(generateRPCTruncateReplicaRequest(null, cap, file + "2",
    // size));
    // wrongOnes.add(generateRPCTruncateReplicaRequest(loc, null, file + "3",
    // size));
    // wrongOnes.add(generateRPCTruncateReplicaRequest(loc, cap, null, size));
    // wrongOnes.add(generateRPCTruncateReplicaRequest(loc, cap, file + "4",
    // null));
    // wrongOnes.add(generateRPCTruncateReplicaRequest(loc, cap, null, null));
    // wrongOnes.add(generateRPCTruncateReplicaRequest(loc, cap, file + "5",
    // -size));
    // wrongOnes.add(generateRPCTruncateReplicaRequest(wrongLoc, cap, file
    // + "6", size));
    //
    // checkWrongCases(wrongOnes);
    // }
    //

    private PinkyRequest generateGetRequest(Locations loc, Capability cap, String file)
        throws JSONException {
        HTTPHeaders headers = new HTTPHeaders();
        if (cap != null)
            headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        if (loc != null)
            headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());
        String uri = null;
        if (file != null)
            uri = file;

        return new PinkyRequest(HTTPUtils.GET_TOKEN, uri, null, headers);
    }

    //
    // private PinkyRequest generateReadRangeRequest(Locations loc,
    // Capability cap, String file, long objectNumber, long[] range)
    // throws JSONException {
    // HTTPHeaders headers = new HTTPHeaders();
    // if (cap != null)
    // headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
    // if (loc != null)
    // headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString()
    // .asString());
    // headers.addHeader(HTTPHeaders.HDR_XOBJECTNUMBER, Long
    // .toString(objectNumber));
    // headers.addHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes "
    // + range[0] + "-" + range[1] + "/*"));
    // String uri = null;
    // if (file != null)
    // uri = file;
    //
    // return new PinkyRequest(HTTPUtils.GET_TOKEN, uri, null, headers);
    // }
    //
    // private PinkyRequest generateReadWholeRequest(Locations loc,
    // Capability cap, String file, long objectNumber) throws JSONException {
    // HTTPHeaders headers = new HTTPHeaders();
    // if (cap != null)
    // headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
    // if (loc != null)
    // headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString()
    // .asString());
    // headers.addHeader(HTTPHeaders.HDR_XOBJECTNUMBER, Long
    // .toString(objectNumber));
    // String uri = null;
    // if (file != null)
    // uri = file;
    //
    // return new PinkyRequest(HTTPUtils.GET_TOKEN, uri, null, headers);
    // }
    //
    // private PinkyRequest generateWriteRangeRequest(Locations loc,
    // Capability cap, String file, Long objectNumber, long[] range,
    // byte[] data) throws JSONException {
    // HTTPHeaders headers = new HTTPHeaders();
    // if (cap != null)
    // headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
    // if (loc != null)
    // headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString()
    // .asString());
    // if (objectNumber != null)
    // headers.addHeader(HTTPHeaders.HDR_XOBJECTNUMBER, objectNumber
    // .toString());
    // headers.addHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes "
    // + range[0] + "-" + range[1] + "/*"));
    //
    // ReusableBuffer rb = null;
    // if (data != null) {
    // headers.addHeader(HTTPHeaders.HDR_CONTENT_LENGTH, Long
    // .toString(data.length));
    // rb = ReusableBuffer.wrap(data);
    // }
    //
    // String uri = null;
    // if (file != null)
    // uri = file;
    //
    // return new PinkyRequest(HTTPUtils.PUT_TOKEN, uri, null, headers, rb);
    // }
    //
    // private PinkyRequest generateWriteWholeRequest(Locations loc,
    // Capability cap, String file, Long objectNumber, byte[] data)
    // throws JSONException {
    // HTTPHeaders headers = new HTTPHeaders();
    // if (cap != null)
    // headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
    // if (loc != null)
    // headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString()
    // .asString());
    // if (objectNumber != null)
    // headers.addHeader(HTTPHeaders.HDR_XOBJECTNUMBER, objectNumber
    // .toString());
    //
    // ReusableBuffer rb = null;
    // if (data != null) {
    // headers.addHeader(HTTPHeaders.HDR_CONTENT_LENGTH, Long
    // .toString(data.length));
    // rb = ReusableBuffer.wrap(data);
    // }
    //
    // String uri = null;
    // if (file != null)
    // uri = file;
    //
    // return new PinkyRequest(HTTPUtils.PUT_TOKEN, uri, null, headers, rb);
    // }
    //
    // private PinkyRequest generateDeleteWholeRequest(Locations loc,
    // Capability cap, String file) throws JSONException {
    // HTTPHeaders headers = new HTTPHeaders();
    // if (cap != null)
    // headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
    // if (loc != null)
    // headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString()
    // .asString());
    //
    // String uri = null;
    // if (file != null)
    // uri = file;
    //
    // return new PinkyRequest(HTTPUtils.DELETE_TOKEN, uri, null, headers);
    // }
    //
    // private PinkyRequest generateRPCDeleteReplicaRequest(Capability cap,
    // String file) {
    // HTTPHeaders headers = new HTTPHeaders();
    // if (cap != null)
    // headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
    // if (file != null)
    // headers.addHeader(HTTPHeaders.HDR_XFILEID, file);
    //
    // return new PinkyRequest(HTTPUtils.POST_TOKEN,
    // RPCTokens.deleteLocalTOKEN, null, headers);
    // }
    //
    // private PinkyRequest generateRPCGlobalMaxRequest(Locations loc,
    // Capability cap, String file) throws JSONException {
    // HTTPHeaders headers = new HTTPHeaders();
    // if (cap != null)
    // headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
    // if (loc != null)
    // headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString()
    // .asString());
    // if (file != null)
    // headers.addHeader(HTTPHeaders.HDR_XFILEID, file);
    //
    // return new PinkyRequest(HTTPUtils.POST_TOKEN,
    // RPCTokens.fetchGlobalMaxToken, null, headers, null);
    // }
    //
    // private PinkyRequest generateRPCTruncateRequest(Locations loc,
    // Capability cap, String file, Long finalSize) throws JSONException {
    // HTTPHeaders headers = new HTTPHeaders();
    // if (cap != null)
    // headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
    // if (loc != null)
    // headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString()
    // .asString());
    //
    // byte[] data = null;
    // if ((file != null) && (finalSize != null)) {
    // data = JSONParser.toJSON(file, finalSize).getBytes(
    // HTTPUtils.ENC_UTF8);
    // } else if (file != null) {
    // data = JSONParser.toJSON(file).getBytes(HTTPUtils.ENC_UTF8);
    // } else if (finalSize != null) {
    // data = JSONParser.toJSON(finalSize).getBytes(HTTPUtils.ENC_UTF8);
    // }
    //
    // ReusableBuffer rb = null;
    // if (data != null) {
    // rb = ReusableBuffer.wrap(data);
    // headers.addHeader(HTTPHeaders.HDR_CONTENT_LENGTH, Long
    // .toString(data.length));
    // }
    //
    // return new PinkyRequest(HTTPUtils.POST_TOKEN, RPCTokens.truncateTOKEN,
    // null, headers, rb);
    // }
    //
    // private PinkyRequest generateRPCTruncateReplicaRequest(Locations loc,
    // Capability cap, String file, Long finalSize) throws JSONException {
    // HTTPHeaders headers = new HTTPHeaders();
    // if (cap != null)
    // headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
    // if (loc != null)
    // headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString()
    // .asString());
    //
    // byte[] data = null;
    // if ((file != null) && (finalSize != null)) {
    // data = JSONParser.toJSON(file, finalSize).getBytes(
    // HTTPUtils.ENC_UTF8);
    // } else if (file != null) {
    // data = JSONParser.toJSON(file).getBytes(HTTPUtils.ENC_UTF8);
    // } else if (finalSize != null) {
    // data = JSONParser.toJSON(finalSize).getBytes(HTTPUtils.ENC_UTF8);
    // }
    //
    // ReusableBuffer rb = null;
    // if (data != null) {
    // rb = ReusableBuffer.wrap(data);
    // headers.addHeader(HTTPHeaders.HDR_CONTENT_LENGTH, Long
    // .toString(data.length));
    // }
    //
    // return new PinkyRequest(HTTPUtils.POST_TOKEN,
    // RPCTokens.truncateLocalTOKEN, null, headers, rb);
    // }
    //

    private void checkFailureCases(List<PinkyRequest> cases) throws Exception {

        for (PinkyRequest pr : cases) {

            OSDRequest rq = new OSDRequest(0);
            rq.setPinkyRequest(pr);

            finished = false;
            stage.enqueueOperation(rq, ParserStage.STAGEOP_PARSE, null);
            synchronized (this) {
                while (!finished)
                    wait();
            }

            assertNotNull(rq.getError());
        }
    }

    public static void main(String[] args) {
        TestRunner.run(ParserStageTest.class);
    }

    private class TestRequestDispatcher implements RequestDispatcher {

        private class TestOp extends Operation {

            public TestOp(RequestDispatcher master) {
                super(master);
            }

            @Override
            public void startRequest(OSDRequest rq) {
                master.requestFinished(rq);
            }

        }

        private OSDConfig config;

        public TestRequestDispatcher(OSDConfig config) throws IOException {
            this.config = config;
        }

        public OSDConfig getConfig() {
            return config;
        }

        public Operation getOperation(RequestDispatcher.Operations opCode) {
            return new TestOp(this);
        }

        public Stage getStage(Stages stage) {
            return null;
        }

        public StageStatistics getStatistics() {
            return new StageStatistics();
        }

        public boolean isHeadOSD(Location xloc) {
            return false;
        }

        public void requestFinished(OSDRequest rq) {
            synchronized (ParserStageTest.this) {
                finished = true;
                ParserStageTest.this.notify();
            }
        }

        @Override
		public void sendSpeedyRequest(Request originalRequest,
				SpeedyRequest speedyRq, InetSocketAddress server)
				throws IOException {
			// TODO Auto-generated method stub

		}

		public void sendUDP(ReusableBuffer data, InetSocketAddress receiver) {
        }

        public void shutdown() {
        }

        @Override
        public DIRClient getDIRClient() {
            // TODO Auto-generated method stub
            return null;
        }
    }

}
