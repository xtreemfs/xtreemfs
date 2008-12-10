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
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
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
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.foundation.speedy.SpeedyResponseListener;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.RequestDispatcher.Operations;
import org.xtreemfs.osd.RequestDispatcher.Stages;
import org.xtreemfs.osd.ops.Operation;
import org.xtreemfs.osd.ops.ReadOperation;
import org.xtreemfs.osd.ops.FetchAndWriteReplica;
import org.xtreemfs.osd.stages.AuthenticationStage;
import org.xtreemfs.osd.stages.ParserStage;
import org.xtreemfs.osd.stages.ReplicationStage;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.stages.StageCallbackInterface;
import org.xtreemfs.osd.stages.StageStatistics;
import org.xtreemfs.osd.stages.StorageStage;
import org.xtreemfs.osd.stages.Stage.StageResponseCode;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.osd.storage.Striping;
import org.xtreemfs.osd.storage.Striping.RPCMessage;
import org.xtreemfs.test.SetupUtils;
import org.xtreemfs.test.osd.ParserStageTest;

/**
 *
 * 15.09.2008
 * @author clorenz
 */
public class ReplicationStageTest extends TestCase {
    TestRequestDispatcher dispatcher;
	private Capability capability;
	private String file;
	private List<Location> locationList;

    public ReplicationStageTest() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		System.out.println("TEST: " + getClass().getSimpleName() + "."
                + getName());
        Logging.start(SetupUtils.DEBUG_LEVEL);

        this.dispatcher = new TestRequestDispatcher();

    	file = "1:1";
		capability = new Capability(file, "read", 0, "IAmTheClient");
    	locationList = new ArrayList<Location>();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		dispatcher.shutdown();
	}

    public void testControlFlow() throws JSONException {
    	OSDRequest request = new OSDRequest(0);

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

		locationList.add(new Location(new RAID0(stripeSize, osds.size()), osds));
    	locationList.add(new Location(new RAID0(stripeSize, osds2.size()), osds2));

    	Locations locations = new Locations(locationList);
		request.setPinkyRequest(generateGetRequest(locations, capability, file));

		// start
		request.setOperation(dispatcher.getOperation(Operations.READ));
		request.getOperation().startRequest(request);

    	try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private PinkyRequest generateGetRequest(Locations loc, Capability cap,
        String file) throws JSONException {
        HTTPHeaders headers = new HTTPHeaders();
        if (cap != null)
            headers.addHeader(HTTPHeaders.HDR_XCAPABILITY, cap.toString());
        if (loc != null)
            headers.addHeader(HTTPHeaders.HDR_XLOCATIONS, loc.asJSONString()
                    .asString());
        String uri = null;
        if (file != null)
            uri = file;

        return new PinkyRequest(HTTPUtils.GET_TOKEN, uri, null, headers);
    }

    private class TestRequestDispatcher implements RequestDispatcher {
    	MultiSpeedy speedy = new TestMultiSpeedy();
    	ReplicationStage stage;
    	DummyStage dummyStage;

        public TestRequestDispatcher() throws IOException {
            stage = new ReplicationStage(this);
            dummyStage = new DummyStage();
            stage.start();
            dummyStage.start();
        }
        @Override
        public Operation getOperation(RequestDispatcher.Operations opCode) {
            switch(opCode){
            case READ: return new ReadOperation(this);
            case FETCH_AND_WRITE_REPLICA: return new FetchAndWriteReplica(this);
            }
            return null;
        }
        @Override
        public Stage getStage(Stages stage) {
            switch(stage){
            case REPLICATION: return this.stage;
            default: return this.dummyStage;
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
        public void sendSpeedyRequest(Request originalRequest,
            SpeedyRequest speedyRq, InetSocketAddress server)
            throws IOException {
            speedyRq.setOriginalRequest(originalRequest);
            this.speedy.sendRequest(speedyRq, server);
        }
        @Override
        public void sendUDP(ReusableBuffer data, InetSocketAddress receiver) {
        }
        @Override
        public void shutdown() {
        }
		@Override
		public OSDConfig getConfig() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public DIRClient getDIRClient() {
			// TODO Auto-generated method stub
			return null;
		}
    }

    private class TestMultiSpeedy extends MultiSpeedy {
    	SpeedyResponseListener listener;

		public TestMultiSpeedy() throws IOException {
			super();
			// TODO Auto-generated constructor stub
		}
		@Override
    	public void registerListener(SpeedyResponseListener rl,
    			InetSocketAddress server) {
    		// TODO Auto-generated method stub
    		this.listener = rl;
    	}
    	@Override
    	public void sendRequest(SpeedyRequest rq, InetSocketAddress server)
    			throws IOException, IllegalStateException {
    		// TODO Auto-generated method stub
    		rq.listener.receiveRequest(rq);
    	}
    }

    private class DummyStage extends Stage {
		public DummyStage() { super("DummyStage"); }
    	@Override
		protected void processMethod(StageMethod method) {
    		method.getCallback().methodExecutionCompleted(method.getRq(), StageResponseCode.OK);
		}
    }
}
