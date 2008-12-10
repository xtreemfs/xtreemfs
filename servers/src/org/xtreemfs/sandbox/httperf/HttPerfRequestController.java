/*
 * RequestController.java
 *
 * Created on June 20, 2007, 12:36 PM
 *
 * @author Bjoern Kolbeck (bjoern@xtreemfs.com)
 * copyright 2006, 2007.
 *
 */

package org.xtreemfs.sandbox.httperf;


public class HttPerfRequestController {


///**
// * This class is the responsible of handling the request from clients. It acts
// * like a monitor : the different stages execute the methods in its threads when
// * methods are called
// */
//public class HttPerfRequestController implements PinkyRequestListener, RequestHandler, UDPCom {
//
//    // Stages of OSD
//    private final AuthenticationStage stAuth;
//
//    private final LeaseManagerStage   stLease;
//
//    private final StorageStage        stStorage;
//
//    private final ParserStage         stParser;
//
//    private final ReplicationStage    stReplication;
//
//    private final PipelinedPinky      stPinky;      // Stage with input requests
//
//    private final MultiSpeedy         stSpeedy;
//
//    private final OSDConfig           config;
//
//    private final OSDId               me;
//
//	private Location loc;
//
//	private RAID0 sp;
//
//	private OSDOperation op;
//
//	private Locations locs;
//
///*	public RequestControllerForHttPerf() throws IOException {
//		this(new OSDConfig("/"), new OSDId("localhost"));
//	}*/
//
//	/**
//     * Creates a new instance of RequestController
//     *
//     * @param config
//     *            OSD's setup
//     * @param me
//     *            Identifier of this OSD
//     */
//    public HttPerfRequestController(OSDConfig config, OSDId me) throws IOException {
//
//        this.me = me;
//        this.config = config;
//
//        // create stages an register this controller as event listener
//        stSpeedy = new MultiSpeedy();
//        OSDClient client = new OSDClient(stSpeedy);
//
//        stParser = new ParserStage(this, config);
//        stAuth = new AuthenticationStage(this, config);
//        stLease = new LeaseManagerStage(this, this, config, me.toString(), me);
//        stStorage = new StorageStage(this, this, config, stSpeedy);
//        stReplication = new ReplicationStage(this, client, config);
//
//        stPinky = new PipelinedPinky(config.getPort(), null, this);
//
//        // debugStuff = new DebugStuff(this, stPinky, stAuth, stLease);
//
//		generateOSDarrangementsForHttperf(config);
//    }
//
//    /**
//     * It starts the execution of the OSD
//     */
//    public void start() {
//        stSpeedy.start();
//        stParser.start();
//        stAuth.start();
//        stLease.start();
//        stStorage.start();
//        stReplication.start();
//        stPinky.start();
//
//        try {
//            stSpeedy.waitForStartup();
//            stParser.waitForStartup();
//            stAuth.waitForStartup();
//            stLease.waitForStartup();
//            stStorage.waitForStartup();
//            stReplication.waitForStartup();
//            stPinky.waitForStartup();
//        } catch (Exception exc) {
//            Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
//        }
//    }
//
//    /**
//     * It gets a request from Pinky and begins its processing. This is the entry
//     * point for Pinky
//     *
//     * @param theRequest
//     *            The request to proccess.
//     */
//    public void receiveRequest(PinkyRequest theRequest) {
//        // Logging.logMessage(Logging.LEVEL_DEBUG,this,"Received request : " +
//        // theRequest.toString());
//
//        OSDRequest rq = new OSDRequest(theRequest);
//        rq.tRecv = System.currentTimeMillis();
//
//        generateOSDRequestFromHttperfRequest(theRequest, rq);
//
//        stageCallback(rq);
//    }
//
//    /**
//     * shuts down all stages and the HTTP server
//     */
//    public void shutdown() {
//        stPinky.shutdown();
//        stParser.shutdown();
//        stAuth.shutdown();
//        stLease.shutdown();
//        stStorage.shutdown();
//        stReplication.shutdown();
//        stSpeedy.shutdown();
//
//        try {
//            stPinky.waitForShutdown();
//            stParser.waitForShutdown();
//            stAuth.waitForShutdown();
//            stLease.waitForShutdown();
//            stStorage.waitForShutdown();
//            stReplication.waitForShutdown();
//            stSpeedy.waitForStartup();
//        } catch (Exception exc) {
//            Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
//        }
//    }
//
//    public OSDId getMe() {
//        return me;
//    }
//
//    public void stageCallback(OSDRequest request) {
//
//        request.tParse = System.currentTimeMillis();
//
//        switch (request.getStatus()) {
//
//        case AUTHENTICATED:
//
//            // check where the request has to be enqueued, depending on the
//            // requested operation
//            switch (request.getOSDOperation().getOpType()) {
//
//            case READ:
//                // open operations and fully-synchronous read operations are
//                // directly enqueued at the storage stage, while any other
//                // read requests first have to be enqueued at the lease stage
///*                if (request.getOSDOperation().getOpSubType() == OperationSubType.OPEN
//                    || (request.getLocations().getRepUpdatePolicy().equals(
//                        Locations.RUP_SYNC) && request.getLocations()
//                            .getRepUpdatePolicySyncLevel() == request
//                            .getLocations().size()))*/
//                    stStorage.enqueueRequest(request);
///*                else
//                    stLease.enqueueRequest(request);*/
//                break;
//
//            default:
//                throw new RuntimeException("illegal request status: "
//                    + request.getStatus());
//            }
//            break;
//
//        case PERSISTED:
///*            if (request.getLocations() != null
//                && request.getLocations().size() > 1
//                && ((request.getOSDOperation().getOpType() == OperationType.WRITE && request
//                        .getLocations().getRepUpdatePolicy().equals(
//                            Locations.RUP_SYNC))
//                    || request.getOSDOperation().getOpType() == OperationType.DELETE || (request
//                        .getOSDOperation().getOpType() == OperationType.RPC && request
//                        .getOSDOperation().getOpSubType() == OperationSubType.TRUNCATE))) {
//                stReplication.enqueueRequest(request);
//            } else {*/
//                response(request);
////            }
//            break;
//
//        case NOTFOUND:
//        case FAILED:
//            response(request);
//            break;
//
//        default:
//            throw new RuntimeException("illegal request status: "
//                + request.getStatus());
//        }
//    }
//
//    /**
//     * Sends back the client response.
//     *
//     * @param rq
//     *            the request
//     */
//    private void response(OSDRequest rq) {
//        PinkyRequest answer = rq.getRequest();
//
//        // Logging.logMessage(Logging.LEVEL_INFO,this,"stage duration
//        // (auth/parse/lease/repl/store): "+
//        // (rq.tAuth-rq.tRecv)+"/"+
//        // (rq.tParse-rq.tRecv)+"/"+
//        // (rq.tLease-rq.tRecv)+"/"+
//        // (rq.tRepl-rq.tRecv)+"/"+
//        // (rq.tStore-rq.tRecv));
//
//        switch (rq.getStatus()) {
//
//        case NOTFOUND:
//            answer.setResponse(HTTPUtils.SC_NOT_FOUND, rq.getErrorMsg());
//            break;
//
//        case PERSISTED:
//            if (rq.getData() != null) {
//                answer.setResponse(HTTPUtils.SC_OKAY, rq.getData(), rq
//                        .getDataType());
//            } else {
//                String newFileSize = rq.getNewFileSize();
//                if (newFileSize != null) {
//                    HTTPHeaders newFileSizeHeader = new HTTPHeaders();
//                    newFileSizeHeader.addHeader(HTTPHeaders.HDR_XNEWFILESIZE,
//                        newFileSize.toString());
//
//                    answer.setResponse(HTTPUtils.SC_OKAY, null,
//                        HTTPUtils.DATA_TYPE.JSON, newFileSizeHeader);
//                } else {
//                    answer.setResponse(HTTPUtils.SC_OKAY);
//                }
//            }
//            break;
//
//        case FAILED:
//            answer.setResponse(HTTPUtils.SC_BAD_REQUEST, rq.getErrorMsg());
//            break;
//
//        default:
//            // This is a bug because the request isn't any of allowed ones
//            throw new RuntimeException(
//                "The status of the request was not a final one");
//        }
//
//        Logging.logMessage(Logging.LEVEL_DEBUG, this, "Processed request : "
//            + answer.toString());
//        stPinky.sendResponse(answer);
//    }
//
//    /**
//     * It gets the storageStage of the controller
//     *
//     * @return It returns the storage stage used by the controller
//     */
//    public StorageStage getStorage() {
//        return stStorage;
//    }
//
//	private void generateOSDarrangementsForHttperf(OSDConfig config) {
//		op = new OSDOperation(OperationType.READ,
//				OperationSubType.WHOLE);
//		List<OSDId> osd = new ArrayList<OSDId>();
//		osd.add(new OSDId("localhost", config.getPort(), OSDId.SCHEME_HTTP));
//		sp = new RAID0(1,1);
//		loc = new Location(sp, osd);
//		ArrayList<Location> list = new ArrayList<Location>(2);
//		list.add(loc);
//		locs = new Locations(list);
//	}
//
//	private void generateOSDRequestFromHttperfRequest(PinkyRequest theRequest, OSDRequest rq) {
//		String[] uri = theRequest.requestURI.split("&");
//        String fileId = uri[0];
//        int objNo = Integer.parseInt(uri[1]);
//        int version = Integer.parseInt(uri[2]);
//
//		rq.setFileId(fileId);
//		rq.setObjectNo(objNo);
//		rq.setVersionNo(version);
//
//		rq.setOSDOperation(op);
//		rq.setPolicy(sp);
//		rq.setLocation(loc);
//		rq.setLocations(locs);
//		rq.setCapability(new Capability(rq.getFileId(), "read", "IAmTheClient", 0));
//    	rq.setStatus(OSDRequest.Status.AUTHENTICATED);
//	}
//
//    public void sendUDP(ReusableBuffer data, InetSocketAddress receiver) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    public void receiveUDP(ReusableBuffer data, InetSocketAddress sender) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    public void sendInternalEvent(OSDRequest event) {
//    }
}
