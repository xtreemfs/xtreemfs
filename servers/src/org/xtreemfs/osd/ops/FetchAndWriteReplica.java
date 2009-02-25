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
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.StripingPolicy;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.HTTPUtils.DATA_TYPE;
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

    @Override
    public void startRequest(OSDRequest rq) {
        if (rq.getOriginalOsdRequest() != null)
            Logging.logMessage(Logging.LEVEL_TRACE, this, "start FetchAndWriteReplica request (requestID: "
                    + rq.getRequestId() + ") with original request piggyback (requestID: "
                    + rq.getOriginalOsdRequest().getRequestId() + ") for object: "
                    + rq.getDetails().getFileId() + "-" + rq.getDetails().getObjectNumber() + ".");
        master.getStage(Stages.REPLICATION).enqueueOperation(rq,
                ReplicationStage.STAGEOP_INTERNAL_FETCH_OBJECT, new StageCallbackInterface() {
                    public void methodExecutionCompleted(OSDRequest request, StageResponseCode result) {
                        postFetchObject(request, result);
                    }
                });
    }

    private void postFetchObject(OSDRequest rq, StageResponseCode result) {
        OSDRequest originalRq = rq.getOriginalOsdRequest();
        if (result == StageResponseCode.OK) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, "fetched filesize for object : "
                    + rq.getDetails().getFileId() + "-" + rq.getDetails().getObjectNumber() + " is "
                    + rq.getDetails().getKnownFilesize() + ".");

            switch (rq.getDetails().replicationFetchingStatus) {
            case FETCHED: {
                Logging.logMessage(Logging.LEVEL_TRACE, this, "object found for: "
                        + rq.getDetails().getFileId() + "-" + rq.getDetails().getObjectNumber() + ".");

                // handle original request
                if (originalRq != null) {
                    // data could be fetched from replica
                    Logging.logMessage(Logging.LEVEL_TRACE, this, "copy fetched data to original request: "
                            + rq.getDetails().getFileId() + "-" + rq.getDetails().getObjectNumber() + ".");
                    // "copy" fetched data to original request
                    originalRq.setData(rq.getData().createViewBuffer(), rq.getDataType());

                    // go on with the original request operation-callback
                    originalRq.getCurrentCallback().methodExecutionCompleted(originalRq, result);
                    rq.setOriginalOsdRequest(null);
                }

                // this also writes the known filesize to disk
                master.getStage(Stages.STORAGE).enqueueOperation(rq, StorageThread.STAGEOP_WRITE_OBJECT,
                        new StageCallbackInterface() {
                            public void methodExecutionCompleted(OSDRequest request, StageResponseCode result) {
                                postWrite(request, result);
                            }
                        });
                break;
            }
            case HOLE_WITH_DATA: {
                Logging.logMessage(Logging.LEVEL_TRACE, this, "data with hole found for: "
                        + rq.getDetails().getFileId() + "-" + rq.getDetails().getObjectNumber() + ".");

                // handle original request
                if (originalRq != null) {
                    // data could be fetched from replica
                    Logging.logMessage(Logging.LEVEL_TRACE, this,
                            "copy fetched and padded data to original request: "
                                    + rq.getDetails().getFileId() + "-" + rq.getDetails().getObjectNumber()
                                    + ".");

                    // padding zeros
                    StripingPolicy sp = rq.getDetails().getCurrentReplica().getStripingPolicy();
                    long currentObject = rq.getDetails().getObjectNumber();
                    ReusableBuffer data = padWithZeros(rq.getData(), (int) sp.getStripeSize(currentObject));

                    // "copy" padded data to original request
                    originalRq.setData(data, DATA_TYPE.BINARY);

                    // go on with the original request operation-callback
                    originalRq.getCurrentCallback().methodExecutionCompleted(originalRq, result);
                    rq.setOriginalOsdRequest(null);
                }

                // this also writes the known filesize to disk
                master.getStage(Stages.STORAGE).enqueueOperation(rq, StorageThread.STAGEOP_WRITE_OBJECT,
                        new StageCallbackInterface() {
                            public void methodExecutionCompleted(OSDRequest request, StageResponseCode result) {
                                postWrite(request, result);
                            }
                        });
                break;
            }
            case EOF: {
                Logging.logMessage(Logging.LEVEL_TRACE, this, "EOF found for: " + rq.getDetails().getFileId()
                        + "-" + rq.getDetails().getObjectNumber() + ".");

                // handle original request
                if (originalRq != null) {
                    // data could be fetched from replica
                    Logging.logMessage(Logging.LEVEL_TRACE, this, "copy empty buffer to original request: "
                            + rq.getDetails().getFileId() + "-" + rq.getDetails().getObjectNumber() + ".");

                    // empty buffer
                    ReusableBuffer data = BufferPool.allocate(0);
                    data.position(0);

                    // "copy" padded data to original request
                    originalRq.setData(data, DATA_TYPE.BINARY);

                    // go on with the original request operation-callback
                    originalRq.getCurrentCallback().methodExecutionCompleted(originalRq, result);
                    rq.setOriginalOsdRequest(null);
                }
                // write filesize to disk
                master.getStage(Stages.STORAGE).enqueueOperation(rq, StorageThread.STAGEOP_WRITE_FILESIZE,
                        new StageCallbackInterface() {
                            public void methodExecutionCompleted(OSDRequest request, StageResponseCode result) {
                                postWrite(request, result);
                            }
                        });
                break;
            }
            case HOLE: {
                Logging.logMessage(Logging.LEVEL_TRACE, this, "hole found for: "
                        + rq.getDetails().getFileId() + "-" + rq.getDetails().getObjectNumber() + ".");

                // handle original request
                if (originalRq != null) {
                    // data could be fetched from replica
                    Logging.logMessage(Logging.LEVEL_TRACE, this, "copy padded data to original request: "
                            + rq.getDetails().getFileId() + "-" + rq.getDetails().getObjectNumber() + ".");

                    // padding zeros
                    ReusableBuffer data = BufferPool.allocate(0);
                    data.position(0);
                    StripingPolicy sp = rq.getDetails().getCurrentReplica().getStripingPolicy();
                    long currentObject = rq.getDetails().getObjectNumber();
                    data = padWithZeros(data, (int) sp.getStripeSize(currentObject));

                    // "copy" padded data to original request
                    originalRq.setData(data, DATA_TYPE.BINARY);

                    // go on with the original request operation-callback
                    originalRq.getCurrentCallback().methodExecutionCompleted(originalRq, result);
                    rq.setOriginalOsdRequest(null);
                }
                // write filesize to disk
                master.getStage(Stages.STORAGE).enqueueOperation(rq, StorageThread.STAGEOP_WRITE_FILESIZE,
                        new StageCallbackInterface() {
                            public void methodExecutionCompleted(OSDRequest request, StageResponseCode result) {
                                postWrite(request, result);
                            }
                        });
                break;
            }
            case FAILED: {
                // TODO: handle it
                break;
            }
            default: {
                // TODO: not allowed
                break;
            }
            }
        } else {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "an error occured while fetching object "
                        + rq.getDetails().getFileId() + ":" + rq.getDetails().getObjectNumber());
            // go on with the original request operation-callback
            originalRq.getCurrentCallback().methodExecutionCompleted(originalRq, result);
            // TODO: what should the request respond?
            master.requestFinished(rq);
        }
    }

    private void postWrite(OSDRequest rq, StageResponseCode result) {
        if (result == StageResponseCode.OK) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "SUCCESFULLY REPLICATED OBJECT: "
                    + rq.getDetails().getFileId() + "-" + rq.getDetails().getObjectNumber() + ".");

            // initiate next steps for replication
            master.getStage(Stages.REPLICATION).enqueueOperation(rq,
                    ReplicationStage.STAGEOP_INTERNAL_TRIGGER_FURTHER_REQUESTS, new StageCallbackInterface() {
                        public void methodExecutionCompleted(OSDRequest request, StageResponseCode result) {
                            postTriggerNextReplicationSteps(request, result);
                        }
                    });
        } else {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "writing object " + rq.getDetails().getFileId()
                        + ":" + rq.getDetails().getObjectNumber() + " failed");
            master.requestFinished(rq);
            // TODO: handling for original request
        }
    }

    private void postTriggerNextReplicationSteps(OSDRequest rq, StageResponseCode result) {
        // end request
        cleanUp(rq);
    }

    private void cleanUp(OSDRequest rq) {
        if (rq.getData() != null)
            BufferPool.free(rq.getData());
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
