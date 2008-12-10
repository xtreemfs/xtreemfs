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
package org.xtreemfs.osd.ops;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.RequestDispatcher.Stages;
import org.xtreemfs.osd.stages.ReplicationStage;
import org.xtreemfs.osd.stages.StageCallbackInterface;
import org.xtreemfs.osd.stages.StorageThread;
import org.xtreemfs.osd.stages.Stage.StageResponseCode;

/**
 * This operation is only for OSD internal processing. It generates no response.
 * 25.09.2008
 * 
 * @author clorenz
 */
public class FetchAndWriteReplica extends Operation {
	/**
	 * @param master
	 */
	public FetchAndWriteReplica(RequestDispatcher master) {
		super(master);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xtreemfs.osd.ops.Operation#startRequest(org.xtreemfs.osd.Request)
	 */
	@Override
	public void startRequest(OSDRequest rq) {
		// FIXME: testcode
		String str = rq.getRequestId()
				+ ": start WriteReplica request (fetch object)";
		if (rq.getOriginalOsdRequest() != null)
			str += " with original request "
					+ rq.getOriginalOsdRequest().getRequestId();
		System.out.println(str);
		master.getStage(Stages.REPLICATION).enqueueOperation(rq,
				ReplicationStage.STAGEOP_INTERNAL_SEND_FETCH_OBJECT_REQUEST,
				new StageCallbackInterface() {

					public void methodExecutionCompleted(OSDRequest request,
							StageResponseCode result) {
						postFetchObject(request, result);
					}
				});
	}

	private void postFetchObject(OSDRequest rq, StageResponseCode result) {
		if (result == StageResponseCode.OK) {
			System.out.println(rq.getRequestId()
					+ ": write object to StorageStage");

			// continue with original request
			OSDRequest originalRq = rq.getOriginalOsdRequest();
			if (originalRq != null) {
				System.out.println(rq.getRequestId()
						+ ": copy data to original request "
						+ originalRq.getRequestId());
				// copy fetched data to original request
				originalRq.setData(BufferPool.allocate(rq.getData().capacity())
						.put(rq.getData()), rq.getDataType());
				// go on with the original request operation-callback
				originalRq.getCurrentCallback().methodExecutionCompleted(
						originalRq, result);
				rq.setOriginalOsdRequest(null);
			}

			master.getStage(Stages.STORAGE).enqueueOperation(rq,
					StorageThread.STAGEOP_WRITE_OBJECT,
					new StageCallbackInterface() {

						public void methodExecutionCompleted(
								OSDRequest request, StageResponseCode result) {
							postWrite(request, result);
						}
					});
		} else {
			if (Logging.isDebug())
				Logging
						.logMessage(Logging.LEVEL_DEBUG, this,
								"fetching object "
										+ rq.getDetails().getFileId() + ":"
										+ rq.getDetails().getObjectNumber()
										+ " failed");
			master.requestFinished(rq);
		}
	}

	private void postWrite(OSDRequest rq, StageResponseCode result) {
		if (result == StageResponseCode.OK) {
			System.out.println(rq.getRequestId() + ": end WriteReplica");

			// initiate next steps for replication
			master
					.getStage(Stages.REPLICATION)
					.enqueueOperation(
							rq,
							ReplicationStage.STAGEOP_INTERNAL_REPLICATION_REQUEST_FINISHED,
							new StageCallbackInterface() {

								public void methodExecutionCompleted(
										OSDRequest request,
										StageResponseCode result) {
									postInitiatingNextReplicationSteps(request,
											result);
								}
							});
		} else {
			if (Logging.isDebug())
				Logging.logMessage(Logging.LEVEL_DEBUG, this, "writing object "
						+ rq.getDetails().getFileId() + ":"
						+ rq.getDetails().getObjectNumber() + " failed");
			master.requestFinished(rq);
			// TODO: handling for original request
		}
	}

	private void postInitiatingNextReplicationSteps(OSDRequest rq,
			StageResponseCode result) {
		// do nothing except cleanup
		System.out.println(rq.getRequestId() + ": cleanup");
		cleanUp(rq);
	}

	private void cleanUp(OSDRequest rq) {
		if (rq.getData() != null)
			BufferPool.free(rq.getData());
	}
}
