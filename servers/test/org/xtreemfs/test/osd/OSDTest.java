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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import junit.textui.TestRunner;

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
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.test.SetupUtils;

/**
 * Class for testing the NewOSD It uses the old OSDTest tests. It checks if the
 * OSD works without replicas neither striping
 *
 * @author Jesus Malo (jmalo)
 */
public class OSDTest extends TestCase {

    private final ServiceUUID serverID;

    private final Locations   loc;

    private final String      file;

    private final long        objectNumber;

    private final long        stripeSize;

    private final DIRConfig   dirConfig;

    private final OSDConfig   osdConfig;

    private DIRClient         dirClient;

    private OSDClient         client;

    private RequestController dir;

    private OSD               osd;

    private Capability        cap;

    public OSDTest(String testName) throws Exception {
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

        cap = new Capability(file, "DebugCapability", 0, osdConfig.getCapabilitySecret());
    }

    protected void setUp() throws Exception {

        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());

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
     * It tests the get of a whole object
     */
    public void testRightGET() throws Exception {

        final String content = "Hello World";
        RPCResponse tmp = client.put(serverID.getAddress(), loc, cap, file, objectNumber,
            ReusableBuffer.wrap(content.getBytes()));
        tmp.waitForResponse();
        tmp.freeBuffers();

        RPCResponse answer = client.get(serverID.getAddress(), loc, cap, file, objectNumber);
        answer.waitForResponse();

        assertEquals(HTTPUtils.SC_OKAY, answer.getStatusCode());
        assertEquals(content, new String(answer.getBody().array()));

        answer.freeBuffers();

        // check object; proper size should be returned
        answer = client.checkObject(serverID.getAddress(), loc, cap, file, objectNumber);
        assertEquals(String.valueOf(content.length()), answer.get().toString());

        answer.freeBuffers();
    }

    /**
     * It tests the get of a range of bytes
     */
    public void testRangeGET() throws Exception {

        final String content = "Hello World";
        RPCResponse tmp = client.put(serverID.getAddress(), loc, cap, file, objectNumber,
            ReusableBuffer.wrap(content.getBytes()));
        tmp.waitForResponse();
        tmp.freeBuffers();

        final int firstByte = 2;
        final int lastByte = 9;

        RPCResponse answer = client.get(serverID.getAddress(), loc, cap, file, objectNumber,
            firstByte, lastByte);
        answer.waitForResponse();

        assertEquals(HTTPUtils.SC_OKAY, answer.getStatusCode());
        assertEquals(content.substring(firstByte, lastByte + 1), new String(answer.getBody()
                .array()));

        answer.freeBuffers();
    }

    /**
     * It tests the empty get
     */
    public void testEmptyGET() throws Exception {

        // create a new file
        RPCResponse answer = client.put(serverID.getAddress(), loc, cap, file, objectNumber,
            ReusableBuffer.wrap("Hello World".getBytes()));
        answer.waitForResponse();
        answer.freeBuffers();

        cap = new Capability(file, "", 1, osdConfig.getCapabilitySecret());

        // truncate the file to zero length
        answer = client.truncate(serverID.getAddress(), loc, cap, file, 0);
        answer.waitForResponse();
        answer.freeBuffers();

        // get the file content
        answer = client.get(serverID.getAddress(), loc, cap, file);
        answer.waitForResponse();

        assertEquals(HTTPUtils.SC_OKAY, answer.getStatusCode());
        assertNull(answer.getBody());

        answer.freeBuffers();
    }

    /**
     * It tests the put of a range
     */
    public void testRangePUT() throws Exception {

        final int tamData = 4;
        byte[] data = new byte[tamData];
        final int firstByte = 0;
        byte[] readByte;
        RPCResponse answer;
        HTTPHeaders headers;
        String newFileSize;

        // It tests the first put. The response will have a new file size header
        answer = client.put(serverID.getAddress(), loc, cap, file, objectNumber, firstByte,
            ReusableBuffer.wrap(data));
        answer.waitForResponse();

        headers = answer.getHeaders();
        newFileSize = headers.getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
        assertNotNull(newFileSize);
        newFileSize = newFileSize.substring(1, newFileSize.indexOf(','));
        assertEquals(tamData, Integer.parseInt(newFileSize));
        answer.freeBuffers();

        answer = client.get(serverID.getAddress(), loc, cap, file, objectNumber);
        answer.waitForResponse();
        readByte = answer.getSpeedyRequest().getResponseBody();
        assertEquals(tamData, readByte.length);
        answer.freeBuffers();

        // It tests a second put, cloned to the previous one. The response won't
        // have a new file size header
        answer = client.put(serverID.getAddress(), loc, cap, file, objectNumber, firstByte,
            ReusableBuffer.wrap(data));
        answer.waitForResponse();
        headers = answer.getHeaders();
        newFileSize = headers.getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
        assertNull(newFileSize);
        answer.freeBuffers();

        answer = client.get(serverID.getAddress(), loc, cap, file, objectNumber);
        answer.waitForResponse();
        readByte = answer.getSpeedyRequest().getResponseBody();
        assertNotNull(readByte);
        assertEquals(tamData, readByte.length);
        answer.freeBuffers();
    }

    /**
     * It test the put of a whole object.
     */
    public void testObjectPUT() throws Exception {

        final int tamData = 1;
        byte[] data = new byte[tamData];

        RPCResponse answer = client.put(serverID.getAddress(), loc, cap, file, objectNumber,
            ReusableBuffer.wrap(data));
        answer.waitForResponse();
        HTTPHeaders headers = answer.getHeaders();
        String newFileSize = headers.getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
        assertNotNull(newFileSize);
        newFileSize = newFileSize.substring(1, newFileSize.indexOf(','));
        assertNotNull(newFileSize);
        assertEquals(tamData, Integer.parseInt(newFileSize));
        answer.freeBuffers();

        answer = client.get(serverID.getAddress(), loc, cap, file, objectNumber);
        answer.waitForResponse();
        byte[] readByte = answer.getSpeedyRequest().getResponseBody();
        assertNotNull(readByte);
        assertEquals(tamData, readByte.length);
        answer.freeBuffers();
    }

    /**
     * It tests the deletion of a non-existent file
     */
    public void testWrongDELETE() throws Exception {

        // Test
        RPCResponse answer = client.delete(serverID.getAddress(), loc, cap, file);
        try {
            answer.waitForResponse();
            fail("got OK, should have got NOT FOUND");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
            answer.freeBuffers();
        }
    }

    /**
     * It tests the deletion of one existing file
     */
    public void testObjectDELETE() throws Exception {

        final String content = "Hello World";
        RPCResponse tmp = client.put(serverID.getAddress(), loc, cap, file, objectNumber,
            ReusableBuffer.wrap(content.getBytes()));
        tmp.waitForResponse();
        tmp.freeBuffers();

        RPCResponse answer = client.delete(serverID.getAddress(), loc, cap, file);
        answer.waitForResponse();
        answer.freeBuffers();

        // NOTE: Due to the POSIX-compliant delete-on-close semantics, it is not
        // possible to draw any conclusions whether the objects have been
        // deleted yet or not. A file will not be regarded as closed by an OSD
        // as long as it still knows of a capability for the file that has not
        // yet expired.

        // try {
        // // file should not exist anymore
        // answer = client.get(serverID.getAddress(), loc, cap, file);
        // answer.waitForResponse();
        // fail();
        // } catch (Exception ex) {
        // answer.freeBuffers();
        // }
    }

    /**
     * It tests the OSD when receives a x-Location where it isn't include
     */
    public void testNoOSDLocation() throws Exception {

        List<Location> locations = new ArrayList<Location>(1);
        StripingPolicy sp = new RAID0(1, 1);
        List<ServiceUUID> osd = new ArrayList<ServiceUUID>(1);
        osd.add(new ServiceUUID("www.google.com:80"));
        locations.add(new Location(sp, osd));
        Locations noOSDLoc = new Locations(locations);

        long objectNumber = 0;
        final int tamData = 1;
        byte[] data = new byte[tamData];

        // Test
        RPCResponse answerGET = null;
        try {
            answerGET = client.get(serverID.getAddress(), noOSDLoc, cap, file);
            answerGET.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
            answerGET.freeBuffers();
        }

        RPCResponse answerPUT = null;
        try {
            answerPUT = client.put(serverID.getAddress(), noOSDLoc, cap, file, objectNumber,
                ReusableBuffer.wrap(data));
            answerPUT.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
            answerPUT.freeBuffers();
        }

        RPCResponse answerDELETE = null;
        try {
            answerDELETE = client.delete(serverID.getAddress(), noOSDLoc, cap, file);
            answerDELETE.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
            answerDELETE.freeBuffers();
        }
    }

    /**
     * It tests the OSD when receives a request for an object which the OSD
     * isn't responsible for
     */
    public void testNoOSDResponsible() throws Exception {

        List<Location> locations = new ArrayList<Location>(1);
        StripingPolicy sp = new RAID0(1, 2);
        List<ServiceUUID> osd = new ArrayList<ServiceUUID>(1);
        osd.add(serverID);
        osd.add(new ServiceUUID("www.google.com"));
        locations.add(new Location(sp, osd));
        Locations noOSDLoc = new Locations(locations);

        long objectNumber = 1;

        // Test
        RPCResponse answerGET = null;
        try {
            answerGET = client.get(serverID.getAddress(), noOSDLoc, cap, file, objectNumber);
            answerGET.waitForResponse();
            fail("got 200, should have got 500");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
            answerGET.freeBuffers();
        }

    }

    /**
     * It tests if wrong GET requests with low level errors are correctly
     * processed
     */
    public void testLowLevelGetRequests() throws Exception {

        String token = "GET";
        HTTPHeaders headers = new HTTPHeaders();
        headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString().asString());
        headers.addHeader(HTTPHeaders.HDR_XOBJECTNUMBER, Long.toString(objectNumber));
        headers.addHeader(HTTPHeaders.HDR_CONTENT_LENGTH, Long.toString(0));

        RPCResponse answer = null;

        // Bad Content-Range tests
        // Empty string
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String(""));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }

        // Totally wrong string
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("qwerty1234567890"));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }

        // Incomplete string
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes"));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }

        // Incomplete string
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes 0"));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }

        // Incomplete string
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes 0-"));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }

        // Incomplete string
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes 0-0"));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }

        // Incomplete string
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes 0-0/"));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }

        // Wrong string at end
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes 0-0/1"));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }

        // Wrong string at end
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes 0-0/1*"));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }

        // Wrong string: there are no blank
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes0-0/*"));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }

        // Semantically wrong string
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes 1-0/*"));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }

        // Wrong string
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes A-/*"));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }

        // Wrong string
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes $-$/*"));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }

        // Wrong string
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes 1.1-4/*"));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }

        // Semantically wrong string with no limits values
        try {
            headers.setHeader(HTTPHeaders.HDR_CONTENT_RANGE, new String("bytes 4321-1234/*"));
            answer = client.sendRPC(serverID.getAddress(), token, null, file, headers);
            answer.waitForResponse();
            fail("got 200, should have got 420");
        } catch (HttpErrorException exc) {
            assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
        } finally {
            answer.freeBuffers();
        }
    }

    /**
     * It tests the truncate method
     */
    public void testTruncate() throws Exception {

        final byte[] data = new String("abcdefghij").getBytes();
        final Long testSize = new Long(data.length);

        final String badFile = "2:2";

        // Testing
        RPCResponse answer = null;

        // Bad location -> Inexistent OSD
        {
            List<Location> locations = new ArrayList<Location>(1);
            StripingPolicy sp = new RAID0(1, 1);
            ServiceUUID google = new ServiceUUID("www.google.com:80");
            List<ServiceUUID> osd2 = new ArrayList<ServiceUUID>(1);
            osd2.add(google);
            locations.add(new Location(sp, osd2));
            Locations noOSDLoc = new Locations(locations);

            try {
                answer = client.truncate(serverID.getAddress(), noOSDLoc, cap, badFile, testSize);
                answer.waitForResponse();
                fail("got 200, should have got 420");
            } catch (HttpErrorException exc) {
                assertEquals(HTTPUtils.SC_USER_EXCEPTION, exc.getStatusCode());
                answer.freeBuffers();
            }
        }

        // It generates a file of testSize bytes which will be truncated several
        // times
        Logging.logMessage(Logging.LEVEL_DEBUG, this, loc.toString());
        answer = client.put(serverID.getAddress(), loc, cap, file, objectNumber, ReusableBuffer
                .wrap(data));
        answer.waitForResponse();
        answer.freeBuffers();

        // Test sizes
        Long[] testSizes = { new Long(testSize + 1), new Long(testSize), new Long(2 * testSize),
            new Long(0) };
        int epoch = 0;
        for (Long size : testSizes) {
            epoch++;

            Capability newCap = new Capability(file, "DebugCapability", epoch, osdConfig
                    .getCapabilitySecret());
            answer = client.truncate(serverID.getAddress(), loc, newCap, file, size);
            answer.waitForResponse();
            String newFileSize = answer.getHeaders().getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
            assertNotNull(newFileSize);
            newFileSize = newFileSize.substring(1, newFileSize.indexOf(','));
            assertEquals(size, new Long(newFileSize));
            answer.freeBuffers();

            answer = client.get(serverID.getAddress(), loc, cap, file);
            answer.waitForResponse();
            answer.freeBuffers();
        }
    }

    /**
     * It tests truncate mixed with read and write operations.
     */
    public void testTruncateWithPutAndGet() throws Exception {

        final byte[] data = new String("abcdefghij").getBytes();
        final long kb = 1024 * stripeSize;

        final long[] testSizes = { 0, 1, 2, kb - 1, kb, kb + 1, 2 * kb - 1, 2 * kb, 2 * kb + 1 };

        int epoch = 0;
        Capability oldCap = cap;
        for (long i : testSizes) {
            epoch++;

            Capability newCap = new Capability(file, "DebugCapability", epoch, osdConfig
                    .getCapabilitySecret());

            // append data to the file
            RPCResponse answer = client.put(serverID.getAddress(), loc, oldCap, file, objectNumber,
                ReusableBuffer.wrap(data));
            answer.waitForResponse();

            HTTPHeaders headers = answer.getHeaders();
            String newFileSize = headers.getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
            assertNotNull(newFileSize);
            newFileSize = newFileSize.substring(1, newFileSize.indexOf(','));
            assertEquals(data.length, Integer.parseInt(newFileSize));
            answer.freeBuffers();

            // truncate the file
            RPCResponse answer2 = client.truncate(serverID.getAddress(), loc, newCap, file, i);
            answer2.waitForResponse();
            HTTPHeaders headers2 = answer2.getHeaders();
            newFileSize = headers2.getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
            assertNotNull(newFileSize);
            newFileSize = newFileSize.substring(1, newFileSize.indexOf(','));
            assertEquals(i, Long.parseLong(newFileSize));
            answer2.freeBuffers();

            // read the last byte
            if (i > 0) {
                RPCResponse answer3 = client.get(serverID.getAddress(), loc, cap, file, (i - 1)
                    / kb, (i - 1) % kb, (i - 1) % kb);
                answer3.waitForResponse();
                HTTPHeaders headers3 = answer3.getHeaders();
                String gotFileSize = headers3.getHeader(HTTPHeaders.HDR_CONTENT_LENGTH);
                assertNotNull(gotFileSize);
                assertEquals(1, Long.parseLong(gotFileSize));
                answer3.freeBuffers();
            }

            // truncate the file to zero length
            newCap = new Capability(file, "DebugCapability", ++epoch, osdConfig
                    .getCapabilitySecret());
            RPCResponse answer4 = client.truncate(serverID.getAddress(), loc, newCap, file, 0);
            answer4.waitForResponse();
            answer4.freeBuffers();

            oldCap = newCap;
        }
    }

    /**
     * It tests the operations over files with holes
     */
    public void testHoles() throws Exception {

        long epoch = 0;

        final byte[] data = new String("abcdefghij").getBytes();
        final long kb = 1024 * stripeSize;

        // Write object 1 and read object 0
        {
            RPCResponse answerW = client.put(serverID.getAddress(), loc, cap, file, 1,
                ReusableBuffer.wrap(data));
            answerW.waitForResponse();
            assertEquals(HTTPUtils.SC_OKAY, answerW.getStatusCode());
            HTTPHeaders headersW = answerW.getHeaders();
            String newFileSize = headersW.getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
            assertNotNull(newFileSize);
            newFileSize = newFileSize.substring(1, newFileSize.indexOf(','));
            assertEquals(kb + data.length, Integer.parseInt(newFileSize));
            answerW.freeBuffers();

            RPCResponse answerR = client.get(serverID.getAddress(), loc, cap, file, 0);
            answerR.waitForResponse();
            HTTPHeaders headersR = answerR.getHeaders();
            String dataSize = headersR.getHeader(HTTPHeaders.HDR_CONTENT_LENGTH);
            assertEquals(HTTPUtils.SC_OKAY, answerR.getStatusCode());
            assertNotNull(dataSize);
            assertEquals(kb, Long.parseLong(dataSize));
            answerR.freeBuffers();

            // @todo Check the data zeroed

            // truncate the file to zero length
            cap = new Capability(file, "DebugCapability", ++epoch, osdConfig.getCapabilitySecret());
            RPCResponse answerT = client.truncate(serverID.getAddress(), loc, cap, file, 0);
            answerT.waitForResponse();
            answerT.freeBuffers();
        }

        // Write objecs 0 and 2 and read object 1
        {
            RPCResponse answerW = client.put(serverID.getAddress(), loc, cap, file, 0,
                ReusableBuffer.wrap(data));
            answerW.waitForResponse();
            assertEquals(HTTPUtils.SC_OKAY, answerW.getStatusCode());
            HTTPHeaders headersW = answerW.getHeaders();
            String newFileSize = headersW.getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
            assertNotNull(newFileSize);
            newFileSize = newFileSize.substring(1, newFileSize.indexOf(','));
            assertEquals(data.length, Integer.parseInt(newFileSize));
            answerW.freeBuffers();

            RPCResponse answerW2 = client.put(serverID.getAddress(), loc, cap, file, 2,
                ReusableBuffer.wrap(data));
            answerW2.waitForResponse();
            assertEquals(HTTPUtils.SC_OKAY, answerW2.getStatusCode());
            HTTPHeaders headersW2 = answerW2.getHeaders();
            String newFileSize2 = headersW2.getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
            assertNotNull(newFileSize2);
            newFileSize2 = newFileSize2.substring(1, newFileSize2.indexOf(','));
            assertEquals(2 * kb + data.length, Integer.parseInt(newFileSize2));
            answerW2.freeBuffers();

            RPCResponse answerR = client.get(serverID.getAddress(), loc, cap, file, 1);
            answerR.waitForResponse();
            HTTPHeaders headersR = answerR.getHeaders();
            String dataSize = headersR.getHeader(HTTPHeaders.HDR_CONTENT_LENGTH);
            assertEquals(HTTPUtils.SC_OKAY, answerR.getStatusCode());
            assertNotNull(dataSize);
            assertEquals(kb, Long.parseLong(dataSize));
            answerR.freeBuffers();

            // @todo Check the data zeroed

            // truncate the file to zero length
            cap = new Capability(file, "DebugCapability", ++epoch, osdConfig.getCapabilitySecret());
            RPCResponse answerT = client.truncate(serverID.getAddress(), loc, cap, file, 0);
            answerT.waitForResponse();
            answerT.freeBuffers();
        }

        // Write objecs 0 and truncate extending to object 1 and read object 1.
        // It also tries to read object 2, but it should fail
        {
            RPCResponse answerW = client.put(serverID.getAddress(), loc, cap, file, 0,
                ReusableBuffer.wrap(data));
            answerW.waitForResponse();
            assertEquals(HTTPUtils.SC_OKAY, answerW.getStatusCode());
            HTTPHeaders headersW = answerW.getHeaders();
            String newFileSize = headersW.getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
            assertNotNull(newFileSize);
            newFileSize = newFileSize.substring(1, newFileSize.indexOf(','));
            assertEquals(data.length, Integer.parseInt(newFileSize));
            answerW.freeBuffers();

            cap = new Capability(file, "DebugCapability", ++epoch, osdConfig.getCapabilitySecret());
            RPCResponse answerT = client.truncate(serverID.getAddress(), loc, cap, file, 2 * kb);
            answerT.waitForResponse();
            assertEquals(HTTPUtils.SC_OKAY, answerT.getStatusCode());
            HTTPHeaders headersT = answerT.getHeaders();
            String newFileSizeT = headersT.getHeader(HTTPHeaders.HDR_XNEWFILESIZE);
            assertNotNull(newFileSizeT);
            newFileSizeT = newFileSizeT.substring(1, newFileSizeT.indexOf(','));
            assertEquals(2 * kb, Long.parseLong(newFileSizeT));
            answerT.freeBuffers();

            RPCResponse answerR = client.get(serverID.getAddress(), loc, cap, file, 1);
            answerR.waitForResponse();
            HTTPHeaders headersR = answerR.getHeaders();
            String dataSize = headersR.getHeader(HTTPHeaders.HDR_CONTENT_LENGTH);
            assertEquals(HTTPUtils.SC_OKAY, answerR.getStatusCode());
            assertNotNull(dataSize);
            assertEquals(kb, Long.parseLong(dataSize));
            answerR.freeBuffers();

            RPCResponse answerR2 = client.get(serverID.getAddress(), loc, cap, file, 2);
            answerR2.waitForResponse();
            HTTPHeaders headersR2 = answerR2.getHeaders();
            String dataSize2 = headersR2.getHeader(HTTPHeaders.HDR_CONTENT_LENGTH);
            assertEquals(HTTPUtils.SC_OKAY, answerR2.getStatusCode());
            assertNotNull(dataSize2);
            assertEquals(0, Long.parseLong(dataSize2));
            answerR2.freeBuffers();

            // @todo Check the data zeroed

            // truncate the file to zero length
            cap = new Capability(file, "DebugCapability", ++epoch, osdConfig.getCapabilitySecret());
            RPCResponse answerTr = client.truncate(serverID.getAddress(), loc, cap, file, 0);
            answerTr.waitForResponse();
            answerTr.freeBuffers();
        }
    }

    public static void main(String[] args) {
        TestRunner.run(OSDTest.class);
    }
}
