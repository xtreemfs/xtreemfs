/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xtreemfs.TestHelper;

// TODO
public class StorageStageTest {
    @Rule
    public final TestRule testLog = TestHelper.testLog;

    @Test
    public void testDummy() {

    }

//    /*
//     * needed for checking the results
//     */
//    private class TestRequestController implements RequestHandler, UDPCom {
//
//        private OSDRequest lastRequest = null;
//
//        public TestRequestController(OSDId me) throws IOException {
//        }
//
//        public OSDId getMe() {
//            return OSDID;
//        }
//
//        /**
//         * blocks untill a Request is received
//         *
//         * @return last received Request
//         */
//        public synchronized OSDRequest getLastRequest(long timeout) {
//            if (lastRequest == null) {
//                try {
//                    wait(timeout);
//                } catch (InterruptedException ex) {
//                    ex.printStackTrace();
//                }
//            }
//            OSDRequest ret = lastRequest;
//            lastRequest = null;
//            return ret;
//        }
//
//        public synchronized void stageCallback(OSDRequest request) {
//            lastRequest = request;
//            BufferPool.free(request.getData());
//            notify();
//        }
//
//        public void sendUDP(ReusableBuffer data, InetSocketAddress receiver) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        public void receiveUDP(ReusableBuffer data, InetSocketAddress sender) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        public void sendInternalEvent(OSDRequest event) {
//        }
//    }
//
//    final OSDId           OSDID = new OSDId("localhost", 32636,
//                                    OSDId.SCHEME_HTTP);
//
//    StorageStage          stage;
//
//    TestRequestController controller;
//
//    File                  dbDir;
//
//    OSDConfig             config;
//
//    StripingPolicy        sp;
//
//    Location              loc;
//
//    MultiSpeedy           speedy;
//
//    public StorageStageTest(String testName) throws IOException {
//        super(testName);
//        Logging.start(SetupUtils.DEBUG_LEVEL);
//
//        config = SetupUtils.createOSD1ConfigForceWithoutSSL();
//        controller = new TestRequestController(OSDID);
//        stage = null;
//
//        dbDir = new File(config.getObjDir());
//        FSTools.delTree(dbDir);
//
//        sp = new RAID0(1, 1);
//        List<OSDId> osd = new ArrayList<OSDId>();
//        osd.add(OSDID);
//        loc = new Location(sp, osd);
//    }
//
//    protected void setUp() throws Exception {
//        System.out.println("TEST: " + getClass().getSimpleName() + "."
//            + getName());
//        speedy = new MultiSpeedy();
//        stage = new StorageStage(controller, controller, config, speedy);
//        stage.start();
//    }
//
//    protected void tearDown() throws Exception {
//        FSTools.delTree(dbDir);
//        stage.shutdown();
//        speedy.shutdown();
//        stage.waitForShutdown();
//        speedy.waitForShutdown();
//        stage = null;
//    }
//
//    /*
//     * Tests
//     */
//
//    // TODO: write better assert
//    public void testFileWrite() {
//        for (int i = 0; i < 10; i++) {
//            System.out.println("TEST: " + getClass().getSimpleName() + "."
//                + getName() + ":" + i);
//
//            OSDRequest rq = createWriteRequest(1000);
//            stage.enqueueRequest(rq);
//            OSDRequest rqR = controller.getLastRequest(OSDClient.TIMEOUT);
//
//            assertEquals(OSDRequest.Status.PERSISTED, rqR.getStatus());
//        }
//    }
//
//    // TODO: write better assert
//    public void testFileRead() {
//        for (int i = 0; i < 10; i++) {
//            System.out.println("TEST: " + getClass().getSimpleName() + "."
//                + getName() + ":" + i);
//
//            OSDRequest rq1 = createWriteRequest(10);
//            stage.enqueueRequest(rq1);
//            OSDRequest rq1R = controller.getLastRequest(OSDClient.TIMEOUT);
//
//            OSDRequest rq2 = createReadRequest(rq1.getFileId(), rq1
//                    .getObjectNo());
//            stage.enqueueRequest(rq2);
//            OSDRequest rq2R = controller.getLastRequest(OSDClient.TIMEOUT);
//
//            assertEquals(OSDRequest.Status.PERSISTED, rq2R.getStatus());
//        }
//    }
//
//    // TODO: write better assert
//    public void testFileDelete() {
//        for (int i = 0; i < 10; i++) {
//            System.out.println("TEST: " + getClass().getSimpleName() + "."
//                + getName() + ":" + i);
//
//            OSDRequest rq1 = createWriteRequest(10);
//            stage.enqueueRequest(rq1);
//            OSDRequest rq1R = controller.getLastRequest(OSDClient.TIMEOUT);
//
//            OSDRequest rq2 = createDeleteRequest(rq1.getFileId(), rq1
//                    .getObjectNo());
//            stage.enqueueRequest(rq2);
//            OSDRequest rq2R = controller.getLastRequest(OSDClient.TIMEOUT);
//
//            assertEquals(OSDRequest.Status.PERSISTED, rq2R.getStatus());
//        }
//    }
//
//    /**
//     * setup a WriteRequest
//     */
//    private OSDRequest createWriteRequest(int dataLength)
//        throws IllegalArgumentException {
//        OSDRequest rq = new OSDRequest(new PinkyRequest());
//        // set the needed parameters
//        OSDOperation op = new OSDOperation(OperationType.WRITE,
//            OperationSubType.WHOLE);
//        rq.setOSDOperation(op);
//        String id = generateFileId();
//        rq.setFileId(id);
//        rq.setObjectNo(1);
//        rq.setPolicy(sp);
//        rq.setLocation(loc);
//        rq.setCapability(new Capability(id, "write", "IAmTheClient", 0));
//
//        byte[] bytes = generateRandomBytes(dataLength);
//        ReusableBuffer buf = ReusableBuffer.wrap(bytes);
//        rq.getRequest().requestBody = buf;
//        rq.getRequest().requestBdyLength = buf.capacity();
//        return rq;
//    }
//
//    /**
//     * setup a ReadRequest
//     */
//    private OSDRequest createReadRequest(String fileId, long objNo)
//        throws IllegalArgumentException {
//        OSDRequest rq = new OSDRequest(new PinkyRequest());
//        // set the needed parameters
//        OSDOperation op = new OSDOperation(OperationType.READ,
//            OperationSubType.WHOLE);
//        rq.setOSDOperation(op);
//
//        rq.setFileId(fileId);
//        rq.setObjectNo(objNo);
//        rq.setPolicy(sp);
//        rq.setLocation(loc);
//        rq.setCapability(new Capability(fileId, "read", "IAmTheClient", 0));
//
//        return rq;
//    }
//
//    /**
//     * setup a ReadRequest
//     */
//    private OSDRequest createDeleteRequest(String fileId, long objNo)
//        throws IllegalArgumentException {
//        OSDRequest rq = new OSDRequest(new PinkyRequest());
//
//        // set the needed parameters
//        OSDOperation op = new OSDOperation(OperationType.DELETE,
//            OperationSubType.WHOLE);
//        rq.setOSDOperation(op);
//
//        rq.setLocation(loc);
//        rq.setFileId(fileId);
//        rq.setObjectNo(objNo);
//        rq.setPolicy(sp);
//        rq.setCapability(new Capability(fileId, "delete", "IAmTheClient", 0));
//
//        return rq;
//    }
//
//    /**
//     * generates randomly filled byte-array
//     *
//     * @param length
//     *            length of the byte-array
//     */
//    private byte[] generateRandomBytes(int length) {
//        Random r = new Random();
//        byte[] bytes = new byte[length];
//
//        r.nextBytes(bytes);
//        return bytes;
//    }
//
//    /**
//     * generates randomly Filename
//     */
//    private String generateFileId() throws IllegalArgumentException {
//        Random r = new Random();
//        String id = new String(r.nextInt(10) + ":" + r.nextInt(1000000));
//
//        return id;
//    }
//
//    public static void main(String[] args) {
//        TestRunner.run(StorageStageTest.class);
//    }
//}
//
//
//class MultiThreadedStorageStageTest extends TestCase {
//
//    /*
//     * needed for checking the results
//     */
//    private class TestRequestController implements RequestHandler, UDPCom {
//        private OSDId me;
//        private OSDRequest lastRequest = null;
//        private int num_requests_processed;
//
//        public TestRequestController(OSDId me) throws IOException {
//            this.me = me;
//            num_requests_processed = 0;
//        }
//
//        public OSDId getMe() {
//            return OSDID;
//        }
//
//        /**
//         * blocks until a Request is received
//         * @return last received Request
//         */
//        public synchronized OSDRequest getLastRequest(long timeout) {
//            if(lastRequest == null) {
//                try {
//                    wait(timeout);
//                }
//                catch (InterruptedException ex) {
//                    ex.printStackTrace();
//                }
//            }
//            OSDRequest ret = lastRequest;
//            lastRequest = null;
//            return ret;
//        }
//
//        public synchronized void stageCallback(OSDRequest request) {
//            //lastRequest = request;
//                //System.out.println("++++" + num_requests_processed + ": stage callback: request " + request.getFileId());
//                num_requests_processed++;
//            BufferPool.free(request.getData());
//            //notify();
//        }
//
//        public void sendUDP(ReusableBuffer data, InetSocketAddress receiver) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        public void receiveUDP(ReusableBuffer data, InetSocketAddress sender) {
//            throw new UnsupportedOperationException("Not supported yet.");
//        }
//
//        public void sendInternalEvent(OSDRequest event) {
//        }
//    }
//
//    final OSDId OSDID = new OSDId("localhost",32636, OSDId.SCHEME_HTTP);
//
//    MultithreadedStorageStage stage;
//    TestRequestController controller;
//    File dbDir;
//    OSDConfig config;
//
//    StripingPolicy sp;
//    Location loc;
//
//    MultiSpeedy speedy;
//
//
//    public MultiThreadedStorageStageTest(String testName) throws IOException {
//        super(testName);
//        Logging.start(SetupUtils.DEBUG_LEVEL);
//
//        config = SetupUtils.createOSD1ConfigForceWithoutSSL();
//        controller = new TestRequestController(OSDID);
//        stage = null;
//
//        dbDir = new File(config.getObjDir());
//        FSTools.delTree(dbDir);
//
//        sp = new RAID0(1,1);
//        List<OSDId> osd = new ArrayList<OSDId>();
//        osd.add(OSDID);
//        loc = new Location(sp,osd);
//    }
//
//    protected void setUp() throws Exception {
//        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
//        speedy = new MultiSpeedy();
//        stage = new MultithreadedStorageStage(controller, controller, config, speedy,10);
//        stage.start();
//    }
//
//    protected void tearDown() throws Exception {
//        FSTools.delTree(dbDir);
//        stage.shutdown();
//        speedy.shutdown();
//        stage.waitForShutdown();
//        speedy.waitForShutdown();
//        stage = null;
//    }
//
//    /*
//     * Tests
//     */
//
//    // TODO: write better assert
//    public void testMultipleRequests() throws InterruptedException {
//        int numRequests = 10;
//        for(int i=0; i<numRequests; i++){
//                //System.out.println("TEST: " + getClass().getSimpleName() + "." + getName() + ":" + i);
//                OSDRequest rq = createWriteRequest(1000);
//                synchronized(this) {
//                        stage.enqueueRequest(rq);
//                        //System.out.println("--- " + i + ": sent request fileId=" + rq.getFileId());
//                }
//                //OSDRequest rqR = controller.getLastRequest(OSDClient.TIMEOUT);
//
//                //assertEquals(OSDRequest.Status.PERSISTED, rqR.getStatus());
//
//        }
//        Thread.sleep(1000);
//
//        assertTrue(controller.num_requests_processed == numRequests);
//
//    }
//
//
//    /**
//     * setup a WriteRequest
//     */
//    private OSDRequest createWriteRequest(int dataLength) throws IllegalArgumentException {
//        OSDRequest rq = new OSDRequest(new PinkyRequest());
//        // set the needed parameters
//        OSDOperation op = new OSDOperation(OperationType.WRITE, OperationSubType.WHOLE);
//        rq.setOSDOperation(op);
//        String id = generateFileId();
//        rq.setFileId(id);
//        rq.setObjectNo(1);
//        rq.setPolicy(sp);
//        rq.setLocation(loc);
//        rq.setCapability(new Capability(id, "write", "IAmTheClient", 0));
//
//        byte[] bytes = generateRandomBytes(dataLength);
//        rq.getRequest().requestBody = ReusableBuffer.wrap(bytes);
//        rq.getRequest().requestBdyLength = bytes.length;
//        return rq;
//    }
//
//    /**
//     * setup a ReadRequest
//     */
//    private OSDRequest createReadRequest(String fileId, long objNo) throws IllegalArgumentException {
//        OSDRequest rq = new OSDRequest(new PinkyRequest());
//        // set the needed parameters
//        OSDOperation op = new OSDOperation(OperationType.READ, OperationSubType.WHOLE);
//        rq.setOSDOperation(op);
//
//        rq.setFileId(fileId);
//        rq.setObjectNo(objNo);
//        rq.setPolicy(sp);
//        rq.setLocation(loc);
//        rq.setCapability(new Capability(fileId, "read", "IAmTheClient", 0));
//
//        return rq;
//    }
//
//    /**
//     * setup a ReadRequest
//     */
//    private OSDRequest createDeleteRequest(String fileId, long objNo) throws IllegalArgumentException {
//        OSDRequest rq = new OSDRequest(new PinkyRequest());
//
//        // set the needed parameters
//        OSDOperation op = new OSDOperation(OperationType.DELETE, OperationSubType.WHOLE);
//        rq.setOSDOperation(op);
//
//        rq.setLocation(loc);
//        rq.setFileId(fileId);
//        rq.setObjectNo(objNo);
//        rq.setPolicy(sp);
//        rq.setCapability(new Capability(fileId, "delete", "IAmTheClient", 0));
//
//        return rq;
//    }
//
//
//    /**
//     * generates randomly filled byte-array
//     * @param length length of the byte-array
//     */
//    private byte[] generateRandomBytes(int length){
//        Random r = new Random();
//        byte[] bytes = new byte[length];
//
//        r.nextBytes(bytes);
//        return bytes;
//    }
//
//    /**
//     * generates randomly Filename
//     */
//    private String generateFileId() throws IllegalArgumentException {
//        Random r = new Random();
//        String id = new String(r.nextInt(10) + ":" + r.nextInt(1000000));
//
//        return id;
//    }
//
//    public static void main(String[] args) {
//        TestRunner.run(MultiThreadedStorageStageTest.class);
//    }
}

