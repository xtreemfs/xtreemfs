/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.stages;

import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.queue.WeightedFairQueue;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.operations.ReadOperation;
import org.xtreemfs.osd.operations.WriteOperation;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class WFQStage extends Stage {

    public final static int                         WFQ_STAGE_OPERATION = 1;

    private OSDRequestDispatcher                    master;

    private WeightedFairQueue<String, StageRequest> wfqQueue;

    private Map<String, String>                     volumeNames;

    private final RPC.UserCredentials               uc;

    private static RPC.Auth                         authNone;

    static {
        authNone = RPC.Auth.newBuilder().setAuthType(RPC.AuthType.AUTH_NONE).build();
    }

    public WFQStage(OSDRequestDispatcher master, int queueCapacity) {
        super("WFQ Stage", queueCapacity);
        this.master = master;
        this.volumeNames = new HashMap<String, String>();
        this.uc = RPC.UserCredentials.newBuilder().setUsername("wfq-stage").addGroups("xtreemfs-services")
                .build();

        this.wfqQueue = new WeightedFairQueue<String, StageRequest>(queueCapacity,
                master.getConfig().getWfqResetPeriod(),
                new WeightedFairQueue.WFQElementInformationProvider<String, StageRequest>() {

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
            public String getQualityClass(StageRequest element) {
                return element.getRequest().getFileId().split(":")[0];
            }

            @Override
            public int getWeight(String qualityClass) {
                if (qualityClass.equals(getVolumeName("volume1"))) {
                    return 1;
                } else if (qualityClass.equals(getVolumeName("volume2"))) {
                    return 2;
                } else {
                    return 1;
                }
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

    private String getVolumeName(String uuid) {
        if(volumeNames.containsKey(uuid)) {
            return volumeNames.get(uuid);
        } else {
            DIRClient dirClient = master.getDIRClient();

            try {
                DIR.ServiceSet serviceSet = dirClient.xtreemfs_service_get_by_uuid(null, authNone, uc, uuid);

                if(serviceSet.getServicesCount() > 0) {
                    return serviceSet.getServices(0).getName();
                } else {
                    Logging.logMessage(Logging.LEVEL_ERROR, this, "Cannot resolve uuid " + uuid);
                }
            } catch(Exception e) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, "Cannot resolve uuid " + uuid + ", " + e);
            }

            return null;
        }
    }

    private double getQoS(String volumeName) {
        return 1.0;
    }
}
