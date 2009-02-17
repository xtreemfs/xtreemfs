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
package org.xtreemfs.osd.stages;

import java.io.IOException;
import java.util.HashMap;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.replication.ObjectDissemination;
import org.xtreemfs.osd.stages.Stage.StageMethod;

/**
 * 
 * 09.09.2008
 * 
 * @author clorenz
 */
public class ReplicationStage extends Stage {
    /**
     * fetching an object from another replica
     */
    public static final int STAGEOP_FETCH_OBJECT = 1;

    /**
     * INTERNAL STAGE-OP: fetching an object from another replica
     */
    public static final int STAGEOP_INTERNAL_FETCH_OBJECT = 2;

    /**
     * INTERNAL STAGE-OP: object (maybe) fetched
     */
    public static final int STAGEOP_INTERNAL_OBJECT_FETCHED = 3;

    /**
     * INTERNAL STAGE-OP: object fetched and saved => trigger further replication
     */
    public static final int STAGEOP_INTERNAL_TRIGGER_FURTHER_REQUESTS = 4;

    private RequestDispatcher master;

    private ObjectDissemination disseminationLayer;

    public ReplicationStage(RequestDispatcher dispatcher) {
	super("OSD Replication Stage");

	this.master = dispatcher;
	this.disseminationLayer = new ObjectDissemination(this, master);
    }

    @Override
    protected void processMethod(StageMethod method) {
	try {
	    switch (method.getStageMethod()) {
	    case STAGEOP_FETCH_OBJECT: {
		if(!processFetchObject(method))
		    methodExecutionSuccess(method, StageResponseCode.FAILED);
		break;
	    }
	    case STAGEOP_INTERNAL_FETCH_OBJECT: {
		if (!processInternalFetchObject(method))
		    methodExecutionSuccess(method, StageResponseCode.FINISH);
		break;
	    }
	    case STAGEOP_INTERNAL_OBJECT_FETCHED: {
		if(processInternalObjectFetched(method))
		    methodExecutionSuccess(method, StageResponseCode.OK);
		break;
	    }
	    case STAGEOP_INTERNAL_TRIGGER_FURTHER_REQUESTS: {
		processInternalTriggerFurtherRequests(method);
		methodExecutionSuccess(method, StageResponseCode.OK);
		break;
	    }
	    default: {
		// TODO: error handling
	    }
	    }
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (JSONException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    /**
     * fetch an object
     * @param method
     * @return true, if replication is possible, otherwise false
     * @throws IOException
     * @throws JSONException
     */
    private boolean processFetchObject(StageMethod method) throws IOException,
            JSONException {
        // if replica exist
        if(method.getRq().getDetails().getLocationList().getNumberOfReplicas()>1) {
            disseminationLayer.fetchObject(method.getRq());
            return true;
        } else
            // no replica available => object cannot be fetched
            return false;
    }

    /**
     * prepares the request and sends it
     * @param method
     * @return true, if something can be fetched, otherwise false
     */
    private boolean processInternalFetchObject(StageMethod method) {
        if(disseminationLayer.prepareRequest(method.getRq())) {
            disseminationLayer.sendFetchObjectRequest(method.getRq());
            return true;
        } else
            // nothing to fetch
            return false;
    }

    /**
     * Checks the fetched object.
     * Tries another replica, if object could not be fetched.
     * @param method
     * @return true, if the object could be fetched, otherwise false
     */
    private boolean processInternalObjectFetched(StageMethod method) {
	if (disseminationLayer.objectFetched(method.getRq()))
	    // object fetched
	    return true;
	else {
	    // object not fetched => new try
	    this.enqueueOperation(method.getRq(), STAGEOP_INTERNAL_FETCH_OBJECT,
		    method.getRq().getCurrentCallback());
	    return false;
	}
    }

    /**
     * Decides to follow up replication or not.
     * @param method
     */
    private void processInternalTriggerFurtherRequests(StageMethod method) {
	disseminationLayer.triggerNewRequests(method.getRq());
    }

    @Override
    public void enqueueOperation(OSDRequest rq, int method,
	    StageCallbackInterface callback) {
	// save callback
	rq.setCurrentCallback(callback);
	super.enqueueOperation(rq, method, callback);
    }

}
