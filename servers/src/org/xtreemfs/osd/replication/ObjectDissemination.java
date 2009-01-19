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

import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.foundation.speedy.SpeedyResponseListener;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDetails;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.RequestDispatcher.Operations;
import org.xtreemfs.osd.replication.TransferStrategy.NextRequest;
import org.xtreemfs.osd.stages.ReplicationStage;

/**
 * 
 * 15.09.2008
 * 
 * @author clorenz
 */
public class ObjectDissemination {
    private ReplicationStage stage;

    private RequestDispatcher master;

    int id = 100; // FIXME: testcode

    /**
     * files which will be downloaded, maybe in background (without a current
     * request)
     * key: fileID
     */
    private HashMap<String, TransferStrategy> filesInProgress;

    /**
     * key: fileID:objectID
     */
    private HashMap<String, OSDRequest> waitingOSDRequests;

    public ObjectDissemination(ReplicationStage stage, RequestDispatcher master) {
	this.stage = stage;
	this.master = master;

	this.filesInProgress = new HashMap<String, TransferStrategy>();
	this.waitingOSDRequests = new HashMap<String, OSDRequest>();
    }

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
	rqDetails.setReplicationTransfer(strategy);

	rqDetails.getReplicationTransfer().addPreferredObject(
		rqDetails.getObjectNumber());

	// generate new WriteReplica Request
	OSDRequest newRq = generateWriteReplicaRequest(rqDetails);
	waitingOSDRequests.put(rqDetails.getFileId() + ":"
		+ rqDetails.getObjectNumber(), rq);
	// prepareRequest(newRq);

	// start new request
	newRq.getOperation().startRequest(newRq);
    }

    private OSDRequest generateWriteReplicaRequest(RequestDetails details) {
	OSDRequest newRq = new OSDRequest(id++);
	newRq.setOperation(master
		.getOperation(Operations.FETCH_AND_WRITE_REPLICA));
	// TODO: set options for request
	newRq.getDetails().setFileId(details.getFileId());
	newRq.getDetails().setReplicationTransfer(
		details.getReplicationTransfer());
	return newRq;
    }

    public void sendFetchObjectRequest(OSDRequest rq) {
	// TODO: set needed options from infos in RequestDetails
	SpeedyRequest speedyRq = new SpeedyRequest("read", "testhost", "", "");
	speedyRq.registerListener(new SpeedyResponseListener() {
	    public void receiveRequest(SpeedyRequest theRequest) {
		// TODO
		OSDRequest originalRq = (OSDRequest) theRequest
			.getOriginalRequest();
		stage.enqueueOperation(originalRq,
			ReplicationStage.STAGEOP_INTERNAL_OBJECT_FETCHED,
			originalRq.getCurrentCallback());
	    }
	});
	try {
	    master.sendSpeedyRequest(rq, speedyRq, new InetSocketAddress(0));
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    private void prepareRequest(OSDRequest rq) {
	NextRequest next = rq.getDetails().getReplicationTransfer()
		.selectNext();
	if (next != null) {
	    rq.getDetails().setObjectNumber(next.objectID);

	    // if a original OSDRequest is waiting for this object, attach it to
	    // the request, which should fetch it
	    if (this.waitingOSDRequests.containsKey(rq.getDetails().getFileId()
		    + ":" + rq.getDetails().getObjectNumber())) {
		rq.setOriginalOsdRequest(this.waitingOSDRequests.remove(rq
			.getDetails().getFileId()
			+ ":" + rq.getDetails().getObjectNumber()));
	    }

	    // TODO: set options for request
	}
    }

    public void prepareRequests(OSDRequest rq) {
	// maybe start new requests
	/*
	 * for (int i = 0; i < 4 && this.id < 110; i++) { OSDRequest newRq =
	 * generateWriteReplicaRequest(rq.getDetails()); prepareRequest(newRq);
	 * // start new request newRq.getOperation().startRequest(newRq); }
	 */
    }
}
