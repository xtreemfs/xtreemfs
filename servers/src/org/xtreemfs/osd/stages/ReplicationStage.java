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

import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.replication.ObjectDissemination;

/**
 * 
 * 09.09.2008
 * 
 * @author clorenz
 */
public class ReplicationStage extends Stage {
	public static final int STAGEOP_FETCH_OBJECT = 1;

	public static final int STAGEOP_INTERNAL_SEND_FETCH_OBJECT_REQUEST = 2;

	public static final int STAGEOP_INTERNAL_OBJECT_FETCHED = 3;

	public static final int STAGEOP_INTERNAL_REPLICATION_REQUEST_FINISHED = 4;

	private RequestDispatcher master;

	private ObjectDissemination disseminationLayer;

	public ReplicationStage(RequestDispatcher dispatcher) {
		super("OSD Replication Stage");

		this.master = dispatcher;
		this.disseminationLayer = new ObjectDissemination(this, master);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.osd.stages.Stage#processMethod(org.xtreemfs.osd.stages.Stage.StageMethod)
	 */
	@Override
	protected void processMethod(StageMethod method) {
		try {
			switch (method.getStageMethod()) {
			case STAGEOP_FETCH_OBJECT: {
				processFetchObject(method);
				break;
			}
			case STAGEOP_INTERNAL_OBJECT_FETCHED: {
				System.out.println(method.getRq().getRequestId()
						+ ": object fetched"); // FIXME: testcode
				methodExecutionSuccess(method, StageResponseCode.OK);
				break;
			}
			case STAGEOP_INTERNAL_SEND_FETCH_OBJECT_REQUEST: {
				processInternalSendFetchObjectRequest(method);
				break;
			}
			case STAGEOP_INTERNAL_REPLICATION_REQUEST_FINISHED: {
				processInternalReplicationRequestFinished(method);
				methodExecutionSuccess(method, StageResponseCode.OK);
				break;
			}
			default: {
				System.out.println(method.getRq().getRequestId()
						+ ": not supported method"); // FIXME: testcode
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

	private void processInternalReplicationRequestFinished(StageMethod method) {
		System.out.println(method.getRq().getRequestId()
				+ ": initiating next steps"); // FIXME: testcode
		disseminationLayer.prepareRequests(method.getRq());

	}

	private boolean processFetchObject(StageMethod method) throws IOException,
			JSONException {
		System.out.println(method.getRq().getRequestId()
				+ ": want to fetch object"); // FIXME: testcode
		disseminationLayer.fetchObject(method.getRq());
		return true;
	}

	private void processInternalSendFetchObjectRequest(StageMethod method) {
		// TODO Auto-generated method stub
		disseminationLayer.sendFetchObjectRequest(method.getRq());
		System.out.println(method.getRq().getRequestId() + ": fetch object"); // FIXME:
		// testcode
	}

	@Override
	public void enqueueOperation(OSDRequest rq, int method,
			StageCallbackInterface callback) {
		// save callback
		rq.setCurrentCallback(callback);
		super.enqueueOperation(rq, method, callback);
	}

}
