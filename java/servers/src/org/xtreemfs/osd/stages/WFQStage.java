/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
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

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class WFQStage extends Stage {

    public final static int                         WFQ_STAGE_OPERATION = 1;

    private OSDRequestDispatcher                    master;

    private WeightedFairQueue<String, StageRequest> wfqQueue;

    public WFQStage(OSDRequestDispatcher master, int queueCapacity) {
        super("WFQ Stage", queueCapacity);
        this.master = master;

        this.wfqQueue = new WeightedFairQueue<String, StageRequest>(queueCapacity,
                master.getConfig().getWfqResetPeriod(),
                new WeightedFairQueue.WFQElementInformationProvider<String, StageRequest>() {

            @Override
            public int getRequestCost(StageRequest element) {
                if(element.getRequest().getOperation() instanceof ReadOperation ||
                        element.getRequest().getOperation() instanceof WriteOperation) {
                    // TODO(ckleineweber): Determine request cost
                    return 1;
                } else {
                    return 1;
                }
            }

            @Override
            public String getQualityClass(StageRequest element) {
                return element.getRequest().getFileId().split(":")[0];
            }

            @Override
            public int getWeight(String qualityClass) {
                return 1;
            }
        });
    }

    public void addOperation(OSDRequest request, Object[] args, Object callback) {
        enqueueOperation(WFQ_STAGE_OPERATION, args, request, callback);
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
            if (wfqQueue.size() < this.getQueueLength()) {
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
            final OSDRequest request = (OSDRequest) method.getArgs()[0];
            request.getOperation().startRequest(request);
        }
    }
}
