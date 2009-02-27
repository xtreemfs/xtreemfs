/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.Request;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.clients.osd.OSDClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.striping.Locations;
import org.xtreemfs.common.striping.RAID0;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.RequestController;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.foundation.speedy.SpeedyResponseListener;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.RequestDispatcher.Operations;
import org.xtreemfs.osd.RequestDispatcher.Stages;
import org.xtreemfs.osd.ops.FetchAndWriteReplica;
import org.xtreemfs.osd.ops.Operation;
import org.xtreemfs.osd.ops.ReadOperation;
import org.xtreemfs.osd.replication.SimpleStrategy;
import org.xtreemfs.osd.replication.TransferStrategy;
import org.xtreemfs.osd.stages.ReplicationStage;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.stages.StageCallbackInterface;
import org.xtreemfs.osd.stages.StageStatistics;
import org.xtreemfs.osd.stages.StorageThread;
import org.xtreemfs.osd.stages.Stage.StageResponseCode;
import org.xtreemfs.test.SetupUtils;

/**
 * 
 * 15.09.2008
 * 
 * @author clorenz
 */
public class ReplicationStageTest extends TestCase {
    RequestDispatcher dispatcher;
    RequestController dir;
    int requestID = 0;

    private Capability capability;
    private String file;
    private List<Location> locationList;

    // needed for dummy classes
    private int stripeSize;
    private ReusableBuffer data;

    public ReplicationStageTest() {
        super();
        // Auto-generated constructor stub
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        System.out.println("TEST: " + getClass().getSimpleName() + "." + getName());
        Logging.start(Logging.LEVEL_DEBUG);

        this.stripeSize = 128;
        this.data = SetupUtils.generateData(stripeSize * 1024);

        DIRConfig dirConfig = SetupUtils.createDIRConfig();
        dir = new RequestController(dirConfig);
        dir.startup();
        dispatcher = new TestRequestDispatcher(new InetSocketAddress(dirConfig.getAddress(), dirConfig
                .getPort()), this.data);

        file = "1:1";
        capability = new Capability(file, "read", 0, "IAmTheClient");
        locationList = new ArrayList<Location>();

        // UUIDResolver.addLocalMapping("localhost", 32640, false);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        dispatcher.shutdown();
        dir.shutdown();
        // UUIDResolver.shutdown();
    }

    /*
     * does not work properly at the moment
     */
    /**
     * this test has no assertions
     * 
     * @throws JSONException
     */
    public void testControlFlowOfFetchAndWriteReplicaOperation() throws JSONException {
        int objectNo = 2;

        OSDRequest request = createOSDRequest(objectNo);
        request.setOperation(dispatcher.getOperation(Operations.READ));
        OSDRequest request2 = createOSDRequest(objectNo + 1);
        request.setOperation(dispatcher.getOperation(Operations.READ));

        // enqueue
        request.getOperation().startRequest(request);
        request.getOperation().startRequest(request2);

        try {
            // wait, hopefully the request has finished
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
     * does not work properly at the moment
     */
    public void testStageMethodsThroughControlFlowOfFetchAndWriteReplicaOperation() throws JSONException {
        int objectNo = 2;
        OSDRequest originalRequest = createOSDRequest(objectNo);
        originalRequest.setOperation(dispatcher.getOperation(Operations.READ));
        OSDRequest request = createOSDRequest(objectNo);
        originalRequest.setCurrentCallback(new StageCallbackInterface() {
            public void methodExecutionCompleted(OSDRequest request, StageResponseCode result) {
                // assert data has been copied to original request
                assertEquals(data.getBuffer(), request.getData().getBuffer());
            }
        });
        request.setOriginalOsdRequest(originalRequest);
        request.setOperation(dispatcher.getOperation(Operations.FETCH_AND_WRITE_REPLICA));

        TransferStrategy strategy = new SimpleStrategy(request.getDetails());
        request.getDetails().setReplicationTransferStrategy(strategy);
        strategy.addPreferredObject(request.getDetails().getObjectNumber());

        // enqueue
        dispatcher.getStage(Stages.REPLICATION).enqueueOperation(request,
                ReplicationStage.STAGEOP_INTERNAL_FETCH_OBJECT, new StageCallbackInterface() {
                    public void methodExecutionCompleted(OSDRequest request, StageResponseCode result) {
                        // assert object was fetched
                        assertEquals(data.getBuffer(), request.getData().getBuffer());

                        dispatcher.getStage(Stages.STORAGE).enqueueOperation(request,
                                StorageThread.STAGEOP_WRITE_OBJECT, new StageCallbackInterface() {
                                    public void methodExecutionCompleted(OSDRequest request,
                                            StageResponseCode result) {
                                        // assert object has been written to
                                        // disk (could not be asserted
                                        // currently)

                                        // copy to original request
                                        OSDRequest originalRq = request.getOriginalOsdRequest();
                                        originalRq.setData(request.getData().createViewBuffer(), request
                                                .getDataType());
                                        // go on with the original request
                                        // operation-callback
                                        originalRq.getCurrentCallback().methodExecutionCompleted(originalRq,
                                                result);

                                        // initiate next steps for replication
                                        dispatcher.getStage(Stages.REPLICATION).enqueueOperation(request,
                                                ReplicationStage.STAGEOP_INTERNAL_TRIGGER_FURTHER_REQUESTS,
                                                new StageCallbackInterface() {
                                                    public void methodExecutionCompleted(OSDRequest request,
                                                            StageResponseCode result) {
                                                        // assert nothing
                                                        // currently (request
                                                        // has ended)
                                                    }
                                                });

                                    }
                                });

                    }
                });

        try {
            // wait, hopefully the request has finished
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // Auto-generated catch block
            e.printStackTrace();
        }

        // caution: maybe the assertions have never been executed
    }

    /**
     * @return
     */
    private OSDRequest createOSDRequest(long objectNo) {
        OSDRequest request = new OSDRequest(requestID++);

        // add available osds
        List<ServiceUUID> osds = new ArrayList<ServiceUUID>();
        osds.add(SetupUtils.getOSD1UUID());
        osds.add(SetupUtils.getOSD2UUID());

        List<ServiceUUID> osds2 = new ArrayList<ServiceUUID>();
        osds2.add(SetupUtils.getOSD3UUID());
        osds2.add(SetupUtils.getOSD4UUID());

        locationList.add(new Location(new RAID0(stripeSize, osds.size()), osds));
        locationList.add(new Location(new RAID0(stripeSize, osds2.size()), osds2));

        Locations locations = new Locations(locationList);

        // fill request
        request.getDetails().setLocationList(locations);
        request.getDetails().setCapability(capability);
        request.getDetails().setFileId(file);
        request.getDetails().setCurrentReplica(locations.getLocation(0));
        request.getDetails().setObjectNumber(objectNo);
        return request;
    }

    private class TestRequestDispatcher implements RequestDispatcher {
        MultiSpeedy speedy;
        ReplicationStage replication;
        TestStorageStage storage;
        DummyStage dummyStage;
        private OSDClient osdClient;
        private DIRClient dirClient;

        public TestRequestDispatcher(InetSocketAddress dirAddress, ReusableBuffer data) throws IOException {
            replication = new ReplicationStage(this);
            storage = new TestStorageStage(this);
            dummyStage = new DummyStage();
            speedy = new TestMultiSpeedy(data);
            osdClient = new OSDClient(speedy);
            // dirClient = new DIRClient(speedy,dirAddress);
            try {
                dirClient = SetupUtils.initTimeSync();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            replication.start();
            storage.start();
            dummyStage.start();

            // // register/update the current address mapping
            // try {
            // RPCResponse r3 = dirClient.registerAddressMapping("localhost",
            // NetUtils.getReachableEndpoints(32636, "http"), 1,
            // NullAuthProvider.createAuthString("localhost", "localhost"));
            // r3.waitForResponse();
            // } catch (JSONException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // } catch (InterruptedException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }
        }

        @Override
        public Operation getOperation(RequestDispatcher.Operations opCode) {
            switch (opCode) {
            case READ:
                return new ReadOperation(this);
            case FETCH_AND_WRITE_REPLICA:
                return new FetchAndWriteReplica(this);
            }
            return null;
        }

        @Override
        public Stage getStage(Stages stage) {
            switch (stage) {
            case REPLICATION:
                return this.replication;
            case STORAGE:
                return this.storage;
            default:
                return this.dummyStage;
            }
        }

        @Override
        public StageStatistics getStatistics() {
            return new StageStatistics();
        }

        @Override
        public boolean isHeadOSD(Location xloc) {
            return false;
        }

        @Override
        public void requestFinished(OSDRequest rq) {
            // TODO
        }

        @Override
        public void sendSpeedyRequest(Request originalRequest, SpeedyRequest speedyRq,
                InetSocketAddress server) throws IOException {
            speedyRq.setOriginalRequest(originalRequest);
            this.speedy.sendRequest(speedyRq, server);
        }

        @Override
        public void sendUDP(ReusableBuffer data, InetSocketAddress receiver) {
        }

        @Override
        public void shutdown() {
            speedy.shutdown();
            try {
                speedy.waitForShutdown();
            } catch (Exception e) {
                // Auto-generated catch block
                e.printStackTrace();
            }
            replication.shutdown();
            storage.shutdown();
            dummyStage.shutdown();
        }

        @Override
        public OSDConfig getConfig() {
            // Auto-generated method stub
            return null;
        }

        @Override
        public DIRClient getDIRClient() {
            return dirClient;
        }

        @Override
        public OSDClient getOSDClient() {
            // Auto-generated method stub
            return osdClient;
        }
    }

    private class TestMultiSpeedy extends MultiSpeedy {
        // SpeedyResponseListener listener;
        ReusableBuffer data;

        public TestMultiSpeedy(ReusableBuffer data) throws IOException {
            super();
            this.data = data;
        }

        @Override
        public void registerListener(SpeedyResponseListener rl, InetSocketAddress server) {
            // Auto-generated method stub
            // this.listener = rl;
        }

        @Override
        public void sendRequest(SpeedyRequest rq, InetSocketAddress server) throws IOException,
                IllegalStateException {
            // if(rq.getURI().equals("getAddressMapping")) {
            //		
            // List<Object> endpoints = new ArrayList(1);
            // Map<String,Object> m = RPCClient.generateMap("address",
            // "127.0.0.1",
            // "port", 32636, "protocol", "http",
            // "ttl", 3600, "match_network", "*");
            // endpoints.add(m);
            //		
            // Map<String, List<Object>> results = new HashMap<String,
            // List<Object>>();
            // List<Object> result = new ArrayList<Object>(3);
            // result.add(1); // version
            // result.add(endpoints);
            // results.put("bla", result);
            //                
            // try {
            // ((OSDRequest)
            // rq.getOriginalRequest()).setData(ReusableBuffer.wrap(JSONParser.writeJSON(results).getBytes()),
            // DATA_TYPE.JSON);
            // rq.listener.receiveRequest(rq);
            // } catch (JSONException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }
            // }
            //	    
            // else {
            rq.responseHeaders = new HTTPHeaders();
            rq.responseBody = this.data;
            rq.responseHeaders.addHeader(HTTPHeaders.HDR_CONTENT_TYPE, HTTPUtils.DATA_TYPE.BINARY.toString());
            rq.responseHeaders.addHeader(HTTPHeaders.HDR_XNEWFILESIZE, this.data.limit());
            rq.listener.receiveRequest(rq);
            // }
        }
    }

    private class DummyStage extends Stage {
        public DummyStage() {
            super("DummyStage");
        }

        @Override
        protected void processMethod(StageMethod method) {
            method.getCallback().methodExecutionCompleted(method.getRq(), StageResponseCode.OK);
        }
    }

    private class TestStorageStage extends StorageThread {
        ReusableBuffer data;

        /**
	 * 
	 */
        public TestStorageStage(RequestDispatcher dispatcher) {
            super(0, dispatcher, null, null, null);
            // Auto-generated constructor stub
        }

        @Override
        protected void processMethod(StageMethod method) {
            // Auto-generated method stub
            method.getRq().getDetails().setObjectNotExistsOnDisk(true);
            method.getCallback().methodExecutionCompleted(method.getRq(), StageResponseCode.OK);
        }
    }
}
