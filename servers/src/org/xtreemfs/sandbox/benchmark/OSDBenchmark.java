package org.xtreemfs.sandbox.benchmark;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.osd.OSDConfig;

/**
 *
 * @author clorenz
 */
public abstract class OSDBenchmark {
//	/*
//	 * needed for checking the results
//	 */
//	protected class TestRequestController implements RequestHandler, UDPCom {
//		private OSDRequest lastRequest;
//
//		/** dummy */
//		public OSDId getMe() {
//			return new OSDId("localhost", 32636, OSDId.SCHEME_HTTP);
//		}
//
//		/**
//		 * blocks until a Request is received
//		 *
//		 * @return last received Request
//		 */
//		public synchronized OSDRequest getLastRequest() {
//			if (lastRequest == null) {
//				try {
//					this.wait();
//				} catch (InterruptedException ex) {
//					ex.printStackTrace();
//				}
//			}
//			OSDRequest ret = lastRequest;
//			lastRequest = null;
//			return ret;
//		}
//
//		public synchronized void stageCallback(OSDRequest request) {
//			lastRequest = request;
//			BufferPool.free(request.getRequest().requestBody);
//			BufferPool.free(request.getRequest().responseBody);
//			BufferPool.free(request.getRequest().responseHeaders);
//			BufferPool.free(request.getData());
//			notify();
//		}
//
//
//                public void sendUDP(ReusableBuffer data, InetSocketAddress receiver) {
//                    throw new UnsupportedOperationException("Not supported yet.");
//                }
//
//                public void receiveUDP(ReusableBuffer data, InetSocketAddress sender) {
//                    throw new UnsupportedOperationException("Not supported yet.");
//                }
//
//        public void sendInternalEvent(OSDRequest event) {
//
//        }
//
//	}
//
//	private final OSDId OSDID = new OSDId("localhost", 32636, OSDId.SCHEME_HTTP);
//
//	/**
//	 * the controller which gets the result-requests from stages
//	 */
//	protected TestRequestController controller;
//
//	/**
//	 * the OSDConfig
//	 */
//	protected OSDConfig config;
//
//	protected StripingPolicy sp;
//
//	private Location loc;
//
//	/**
//	 * root-path for benchmarks
//	 */
//	protected String testDir;
//
//	/**
//	 * a Random for generating the same filenames every benchmark
//	 */
//	protected Random filenameRandom;
//
//	protected OSDBenchmark(String testDir, StripingPolicy sp) throws IOException {
//		Logging.start(Logging.LEVEL_WARN);
//
//		config = createOSDConfig(testDir);
//		controller = new TestRequestController();
//
//		this.testDir = testDir;
//		filenameRandom = new Random(54684651);
//
//		FSTools.delTree(new File(testDir));
//
//		this.sp = sp;
//		List<OSDId> osd = new ArrayList<OSDId>();
//		osd.add(OSDID);
//		loc = new Location(sp, osd);
//	}
//
//	/**
//	 * run before benchmark
//	 */
//	protected abstract void setUp();
//
//	/**
//	 * run after benchmark
//	 */
//	protected abstract void tearDown();
//
//	/**
//	 * setup a WriteRequest
//	 */
//	protected OSDRequest createWriteRequest(String fileId, int objNo,
//			int version, int dataLength) throws IllegalArgumentException {
//		OSDOperation op = new OSDOperation(OperationType.WRITE,
//				OperationSubType.WHOLE);
//
//		OSDRequest rq = new OSDRequest(new PinkyRequest());
//		// set the needed parameters
//		rq.setOSDOperation(op);
//		rq.setFileId(fileId);
//		rq.setObjectNo(objNo);
//		rq.setVersionNo(version);
//		rq.setPolicy(sp);
//		rq.setLocation(loc);
//		rq.setCapability(new Capability(fileId, "write", "IAmTheClient",0));
//
//		byte[] bytes = Common.generateRandomBytes(dataLength);
//		ReusableBuffer buf = BufferPool.allocate(dataLength);
//		buf.put(bytes);
//		rq.getRequest().requestBody = buf;
//		rq.getRequest().requestBdyLength = buf.capacity();
//		return rq;
//	}
//
//	/**
//	 * setup a ReadRequest
//	 */
//	protected OSDRequest createReadRequest(String fileId, long objNo,
//			int version) throws IllegalArgumentException {
//		OSDRequest rq = new OSDRequest(new PinkyRequest());
//		// set the needed parameters
//		OSDOperation op = new OSDOperation(OperationType.READ,
//				OperationSubType.WHOLE);
//		rq.setOSDOperation(op);
//
//		rq.setFileId(fileId);
//		rq.setObjectNo(objNo);
//		rq.setVersionNo(version);
//		rq.setPolicy(sp);
//		rq.setLocation(loc);
//		rq.setCapability(new Capability(fileId, "read", "IAmTheClient",0));
//
//		return rq;
//	}
//
//	/**
//	 * setup a DeleteRequest
//	 */
//	protected OSDRequest createDeleteRequest(String fileId, long objNo,
//			int version) throws IllegalArgumentException {
//		OSDRequest rq = new OSDRequest(new PinkyRequest());
//
//		// set the needed parameters
//		OSDOperation op = new OSDOperation(OperationType.DELETE,
//				OperationSubType.WHOLE);
//		rq.setOSDOperation(op);
//
//		rq.setFileId(fileId);
//		rq.setObjectNo(objNo);
//		rq.setVersionNo(version);
//		rq.setPolicy(sp);
//		rq.setCapability(new Capability(fileId, "delete", "IAmTheClient",0));
//
//		return rq;
//	}
//
//	/**
//	 * setup a OSDConfig
//	 *
//	 * @param testDir
//	 * @return
//	 */
//	private static OSDConfig createOSDConfig(String testDir) throws IOException {
//		Properties props = new Properties();
//		props.setProperty("dir_service.host", "localhost");
//		props.setProperty("dir_service.port", "32638");
//		props.setProperty("object_dir", testDir);
//		props.setProperty("debug_level", "" + Logging.LEVEL_WARN);
//		props.setProperty("listen_port", "32637");
//		props.setProperty("local_clock_renewal", "1");
//		props.setProperty("remote_time_sync", "1");
//
//		return new OSDConfig(props);
//	}

}
