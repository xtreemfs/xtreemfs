/*
 * Copyright (c) 2008-2015 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.queue.WeightedFairQueue;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.operations.ReadOperation;
import org.xtreemfs.osd.operations.WriteOperation;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class ReqSchedStage extends Stage {

    public final static int REQSCHED_STAGE_OPERATION = 1;

    private OSDRequestDispatcher                    master;

    private WeightedFairQueue<QualityClass, StageRequest> wfqQueue;

    private class QualityClass {
        private int weight;
        private String volumeUuid;

        public QualityClass(String volumeUuid, int weight) {
            this.volumeUuid = volumeUuid;
            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }
    }

    public ReqSchedStage(OSDRequestDispatcher master, int queueCapacity) {
        super("ReqSched Stage", queueCapacity);
        this.master = master;

        this.wfqQueue = new WeightedFairQueue<QualityClass, StageRequest>(queueCapacity, 10,
                new WeightedFairQueue.WFQElementInformationProvider<QualityClass, StageRequest>() {

            @Override
            public int getRequestCost(StageRequest element) {
                if(element.getRequest().getOperation() instanceof ReadOperation) {
                    return ((OSD.readRequest) element.getRequest().getRequestArgs())
                            .getLength();
                } else if(element.getRequest().getOperation() instanceof WriteOperation) {
                    return ((OSD.writeRequest) element.getRequest().getRequestArgs())
                            .getObjectData().toByteArray().length;
                } else {
                    return 1;
                }
            }

            @Override
            public QualityClass getQualityClass(StageRequest element) {
                int weight = 0;
                String volumeUuid = "";
                if(element.getRequest().getCapability() != null) {
                    weight = element.getRequest().getCapability().getPriority();
                }
                if(element.getRequest().getFileId() != null) {
                    volumeUuid = element.getRequest().getFileId().split(":")[0];
                }
                return new QualityClass(volumeUuid, weight);
            }

            @Override
            public int getWeight(QualityClass qualityClass) {
                return qualityClass.getWeight();
            }
        });
    }

    public void addOperation(OSDRequest request, Object[] args, Object callback) {
        enqueueOperation(REQSCHED_STAGE_OPERATION, args, request, callback);
    }

    @Override
    protected void enqueueOperation(int stageOp, Object[] args, OSDRequest request, ReusableBuffer createdViewBuffer,
                                    Object callback) {
        if (request == null) {
            try {
                wfqQueue.put(new StageRequest(stageOp, args, request, callback));
            } catch (InterruptedException e) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.stage, this,
                        "Failed to queue internal request due to InterruptedException:");
                Logging.logError(Logging.LEVEL_DEBUG, this, e);
            }
        } else {
            if (wfqQueue.remainingCapacity() > 0) {
                try {
                    wfqQueue.put(new StageRequest(stageOp, args, request, callback));
                } catch (InterruptedException e) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.stage, this,
                            "Failed to queue external request due to InterruptedException:");
                    Logging.logError(Logging.LEVEL_DEBUG, this, e);
                }
            } else {
                // Make sure that the data buffer is returned to the pool if
                // necessary, as some operations create view buffers on the
                // data. Otherwise, a 'finalized but not freed before' warning
                // may occur.
                if (createdViewBuffer != null) {
                    assert (createdViewBuffer.getRefCount() >= 2);
                    BufferPool.free(createdViewBuffer);
                }
                Logging.logMessage(Logging.LEVEL_WARN, this, "stage is overloaded, request %d for %s dropped",
                        request.getRequestId(), request.getFileId());
                request.sendInternalServerError(new IllegalStateException("server overloaded, request dropped"));
            }
        }
    }

    @Override
    public int getQueueLength() {
        return wfqQueue.size();
    }

    @Override
    public void run() {

        notifyStarted();

        while (!quit) {
            try {
                final StageRequest op = wfqQueue.take();

                processMethod(op);

            } catch (InterruptedException ex) {
                break;
            } catch (Throwable ex) {
                this.notifyCrashed(ex);
                break;
            }
        }

        notifyStopped();
    }

    @Override
    protected void processMethod(StageRequest method) {
        if(method != null && method.getArgs().length > 0) {
            final OSDRequest request = (OSDRequest) method.getRequest();
            request.getOperation().startRequest(request);
        }
    }
}
