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
package org.xtreemfs.osd.replication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

import org.xtreemfs.babudb.lsmdb.LSMDBWorker.RequestOperation;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.HttpErrorException;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.RPCResponseListener;
import org.xtreemfs.common.clients.osd.OSDClient;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.HTTPUtils.DATA_TYPE;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.foundation.speedy.SpeedyResponseListener;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDetails;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.RequestDispatcher.Operations;
import org.xtreemfs.osd.replication.TransferStrategy.NextRequest;
import org.xtreemfs.osd.stages.ReplicationStage;
import org.xtreemfs.osd.storage.CowPolicy;

/**
 * Handles the fetching of replicas.
 * 15.09.2008
 * 
 * @author clorenz
 */
public class ObjectDissemination {
    private ReplicationStage stage;
    private RequestDispatcher master;

    // use negative requestIDs for simple differ of FetchAndWriteRequests
    private static long requestID = -10;

    /**
     * files which will be downloaded, maybe in background (without a current
     * request)
     * key: <code>fileID</code>
     */
    private HashMap<String, TransferStrategy> filesInProgress;

    /**
     * A OSDRequest maybe want to fetch an object, that is already in progress,
     * so it must wait until the object is fetched.
     * key: <code>fileID:objectID</code>
     */
//    private HashMap<String, OSDRequest> waitingOSDRequests;

    public ObjectDissemination(ReplicationStage stage, RequestDispatcher master) {
	this.stage = stage;
	this.master = master;

	this.filesInProgress = new HashMap<String, TransferStrategy>();
//	this.waitingOSDRequests = new HashMap<String, OSDRequest>();
    }

    /**
     * starts a new FetchAndWriteReplica request to start replicating the object
     * @param rq
     */
    public void fetchObject(OSDRequest rq) {
	RequestDetails rqDetails = rq.getDetails();
	TransferStrategy strategy = this.filesInProgress.get(rqDetails
		.getFileId());
	if (strategy == null) {
	    // file not in progress, so create a new strategy
	    strategy = new SimpleStrategy(rqDetails);
	    // keep strategy in mind
	    this.filesInProgress.put(rqDetails.getFileId(), strategy);
	}
	rqDetails.setReplicationTransferStrategy(strategy);

	strategy.addPreferredObject(rqDetails.getObjectNumber());

	// generate new WriteReplica Request
	OSDRequest newRq = generateFetchAndWriteReplicaRequest(rqDetails);
	newRq.setOriginalOsdRequest(rq);
//	waitingOSDRequests.put(rqDetails.getFileId() + ":"
//		+ rqDetails.getObjectNumber(), rq);

	// start new request
	newRq.getOperation().startRequest(newRq);
    }

    /**
     * Creates a new FetchAndWriteReplica request and sets up all necessary infos.
     * @param details
     * @return
     */
    private OSDRequest generateFetchAndWriteReplicaRequest(RequestDetails details) {
	OSDRequest newRq = new OSDRequest(requestID--);
	newRq.setOperation(master.getOperation(Operations.FETCH_AND_WRITE_REPLICA));
	// TODO: set options for request
	newRq.getDetails().setFileId(details.getFileId());
	newRq.getDetails().setReplicationTransferStrategy(
		details.getReplicationTransferStrategy());
	newRq.getDetails().setCapability(details.getCapability());
	newRq.getDetails().setCurrentReplica(details.getCurrentReplica());
	newRq.getDetails().setLocationList(details.getLocationList());
	newRq.getDetails().setObjectVersionNumber(details.getObjectVersionNumber());
	newRq.getDetails().setCowPolicy(details.getCowPolicy());
	newRq.getDetails().setRangeRequested(details.isRangeRequested());
	
	// FIXME: set objectID from original OSDRequest
	newRq.getDetails().setObjectNumber(details.getObjectNumber());
	return newRq;
    }

    /**
     * Prepares the request by using the associated TransferStrategy.
     * @param rq
     * @return
     */
    public boolean prepareRequest(OSDRequest rq) {
    	TransferStrategy strategy = rq.getDetails().getReplicationTransferStrategy();
    	strategy.selectNextOSD(rq.getDetails().getObjectNumber());
    	NextRequest next = strategy.getNext();
    
    	if (next != null) { // there is something to fetch
    	    rq.getDetails().nextReplicationStep = next;
//    	    rq.getDetails().setObjectNumber(next.objectID);
    
    //	    // if a original OSDRequest is waiting for this object, attach it to
    //	    // the request, which should fetch it
    //	    if (this.waitingOSDRequests.containsKey(rq.getDetails().getFileId()
    //		    + ":" + rq.getDetails().getObjectNumber())) {
    //		rq.setOriginalOsdRequest(this.waitingOSDRequests.remove(rq
    //			.getDetails().getFileId()
    //			+ ":" + rq.getDetails().getObjectNumber()));
    //	    }
    
    	    // TODO: set options for request
    	    return true;
    	} else {
    	    // no OSDs available for object anymore
    	    // check if it is a hole
	    StripingPolicy sp = rq.getDetails().getCurrentReplica().getStripingPolicy();
	    long currentObject = rq.getDetails().getObjectNumber();
	    long lastKnownObject = rq.getDetails().getCurrentReplica()
		    .getStripingPolicy().calculateLastObject(strategy.getKnownFilesize());
	    if (currentObject < lastKnownObject) { // => hole
		// padding zeros
		ReusableBuffer data = BufferPool.allocate(0);
		data.position(0);
		data = padWithZeros(data, (int) sp.getStripeSize(currentObject));
		rq.setData(data, DATA_TYPE.BINARY);
	    }
	    // else: no OSD of any replica has data of this object
	    return false;
    	}
    }

    /**
     * Sends a RPC for reading the object on another OSD.
     * @param rq
     */
    public void sendFetchObjectRequest(OSDRequest rq) {
	RequestDetails details = rq.getDetails();
	NextRequest next = details.nextReplicationStep;
	
	OSDClient client = master.getOSDClient();
	try {
	    RPCResponse response = client.readLocalRPC(next.osd.getAddress(),
		    details.getLocationList(), details.getCapability(), details
			    .getFileId(), next.objectID);
	    response.setAttachment(rq);
	    response.setResponseListener(new RPCResponseListener() {
		@Override
		public void responseAvailable(RPCResponse response) {
		    OSDRequest originalRq = (OSDRequest) response
			    .getAttachment();
		    try {
			// set data
			originalRq.setData(response.getBody(),
				HTTPUtils.DATA_TYPE.toDataType(response
					.getHeaders().getHeader(
						HTTPHeaders.HDR_CONTENT_TYPE)));
			// set filesize
			long filesize = Long.parseLong(response.getHeaders()
				.getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
			originalRq.getDetails()
				.getReplicationTransferStrategy()
				.setKnownFilesize(filesize);
		    } catch (HttpErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    }
		    stage.enqueueOperation(originalRq,
			    ReplicationStage.STAGEOP_INTERNAL_OBJECT_FETCHED,
			    originalRq.getCurrentCallback());
		}
	    });
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (JSONException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	
/*	SpeedyRequest speedyRq = new SpeedyRequest("read", "testhost", "", "");
	speedyRq.registerListener(new SpeedyResponseListener() {
	    public void receiveRequest(SpeedyRequest response) {
		OSDRequest originalRq = (OSDRequest) response.getOriginalRequest();
		// TODO: set attributes
		originalRq.setData(response.responseBody,
			HTTPUtils.DATA_TYPE.toDataType(response.responseHeaders
				.getHeader(HTTPHeaders.HDR_CONTENT_TYPE)));
		// set filesize
		long filesize = Long.parseLong(response.responseHeaders
			.getHeader(HTTPHeaders.HDR_XNEWFILESIZE));
		originalRq.getDetails().getReplicationTransferStrategy()
			.setKnownFilesize(filesize);
		stage.enqueueOperation(originalRq,
			ReplicationStage.STAGEOP_INTERNAL_OBJECT_FETCHED,
			originalRq.getCurrentCallback());
	    }
	});
	try {
	    master.sendSpeedyRequest(rq, speedyRq, next.osd.getAddress());
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}*/
    }

    /**
     * Checks if the object could be fetched.
     * If not, retry.
     * @param rq
     */
    public boolean objectFetched(OSDRequest rq) {
	TransferStrategy strategy = rq.getDetails().getReplicationTransferStrategy();
	NextRequest next = rq.getDetails().nextReplicationStep;

	// TODO:
        if(rq.getDataType() == HTTPUtils.DATA_TYPE.BINARY) {
            // object could be fetched
            strategy.removeOSDListForObject(next.objectID);
            strategy.removePreferredObject(next.objectID);
            return true;
        } else if (rq.getDataType() == HTTPUtils.DATA_TYPE.JSON) {
    	    // check if it is a EOF
	    if(strategy.getKnownFilesize()>0) { // at least one time a correct filesize was fetched
		StripingPolicy sp = rq.getDetails().getCurrentReplica().getStripingPolicy();
		long currentObject = rq.getDetails().getObjectNumber();
		long lastKnownObject = rq.getDetails().getCurrentReplica()
			.getStripingPolicy().calculateLastObject(strategy.getKnownFilesize());
		if (currentObject > lastKnownObject) { // => EOF
		    // padding zeros
		    ReusableBuffer data = BufferPool.allocate(0);
		    data.position(0);
		    rq.setData(data, DATA_TYPE.BINARY);
		    return true;
		}
	    }
	    // object could not be fetched => try another replica

            // save already tested OSDs => otherwise we could not
	    // determine when we have asked all replicas
            ServiceUUID lastOsd = next.osd;
            strategy.removeOSDForObject(next.objectID, lastOsd);
            
            // re-enqueue the objectNo
/*            if(rq.getOriginalOsdRequest() != null) // => urgent
        	strategy.addPreferredObject(next.objectID);
            else
        	strategy.addRequiredObject(next.objectID);
*/    
            return false; // begin "new"
        }
        return false;
    }

    /**
     * Decides what to do next, if an object has been successfully replicated.
     * @param rq
     */
    public void triggerNewRequests(OSDRequest rq) {
	// maybe start new requests
/*	for (int i = 0; i < 4 && this.requestID > -20; i++) {
	    OSDRequest newRq = generateWriteReplicaRequest(rq.getDetails());
	    prepareRequest(newRq);
	    // start new request
	    newRq.getOperation().startRequest(newRq);
	}
*/	 

	// TODO: error cases
    }

    /*
     * copied from StorageThread
     */
    private ReusableBuffer padWithZeros(ReusableBuffer data, int stripeSize) {
        int oldSize = data.capacity();
        if (!data.enlarge(stripeSize)) {
            ReusableBuffer tmp = BufferPool.allocate(stripeSize);
            data.position(0);
            tmp.put(data);
            while (tmp.hasRemaining())
                tmp.put((byte) 0);
            BufferPool.free(data);
            return tmp;
        } else {
            data.position(oldSize);
            while (data.hasRemaining())
                data.put((byte) 0);
            return data;
        }
    }

}
