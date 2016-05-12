/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import java.io.IOException;
import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.osd.stages.ReplicationStage.FetchObjectCallback;
import org.xtreemfs.osd.stages.StorageStage.ReadObjectCallback;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.ObjectInformation.ObjectStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.InternalGmax;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.readRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class ReadOperation extends OSDOperation {

    final String sharedSecret;

    final ServiceUUID localUUID;

    public ReadOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_READ;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final readRequest args = (readRequest) rq.getRequestArgs();

        if (args.getObjectNumber() < 0) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "object number must be >= 0");
            return;
        }

        if (args.getOffset() < 0) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "offset must be >= 0");
            return;
        }

        if (args.getLength() < 0) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "length must be >= 0");
            return;
        }

        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

        if (args.getLength()+args.getOffset() > sp.getStripeSizeForObject(0)) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "length + ofset must be <= "+sp.getStripeSizeForObject(0)+" (stripe size)");
            return;
        }

        int numReplicas = rq.getLocationList().getNumReplicas();
        String replicaUpdatePolicy = rq.getLocationList().getReplicaUpdatePolicy();

        if (numReplicas > 1 && ReplicaUpdatePolicies.isRW(replicaUpdatePolicy)) {
            rwReplicatedRead(rq, args);
        } else if (numReplicas == 1 || ReplicaUpdatePolicies.isRO(replicaUpdatePolicy)
                || ReplicaUpdatePolicies.isNONE(replicaUpdatePolicy)) {
            final long snapVerTS = rq.getCapability().getSnapConfig() == SnapConfig.SNAP_CONFIG_ACCESS_SNAP
                    ? rq.getCapability().getSnapTimestamp() : 0;

            master.getStorageStage().readObject(args.getFileId(), args.getObjectNumber(), sp, args.getOffset(),
                    args.getLength(), snapVerTS, rq, new ReadObjectCallback() {

                        @Override
                        public void readComplete(ObjectInformation result, ErrorResponse error) {
                            postRead(rq, args, result, error);
                        }
                    });
        } else if (ReplicaUpdatePolicies.isEC(replicaUpdatePolicy)) {
            // FIXME (jdillmann): do!
        } else {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL,
                    "Invalid ReplicaUpdatePolicy: " + replicaUpdatePolicy);
        }
    }

    public void rwReplicatedRead(final OSDRequest rq, final readRequest args) {
        master.getRWReplicationStage().prepareOperation(args.getFileCredentials(), rq.getLocationList(), args.getObjectNumber(), args.getObjectVersion(),
                RWReplicationStage.Operation.READ, new RWReplicationStage.RWReplicationCallback() {

            @Override
            public void success(long newObjectVersion) {
                final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

                final long snapVerTS = rq.getCapability().getSnapConfig() == SnapConfig.SNAP_CONFIG_ACCESS_SNAP? rq.getCapability().getSnapTimestamp(): 0;

                //FIXME: ignore canExecOperation for now...
                master.getStorageStage().readObject(args.getFileId(), args.getObjectNumber(), sp,
                    args.getOffset(),args.getLength(), snapVerTS, rq, new ReadObjectCallback() {

                    @Override
                    public void readComplete(ObjectInformation result, ErrorResponse error) {
                        postRead(rq, args, result, error);
                    }
                });
            }

            @Override
            public void redirect(String redirectTo) {
                rq.getRPCRequest().sendRedirect(redirectTo);
            }

            @Override
            public void failed(ErrorResponse err) {
                rq.sendError(err);
            }
        }, rq);
    }

    public void postRead(final OSDRequest rq, readRequest args, ObjectInformation result, ErrorResponse error) {
        if (error != null) {
            rq.sendError(error);
        } else {
            if (result.getStatus() == ObjectInformation.ObjectStatus.DOES_NOT_EXIST
                    && ReplicaUpdatePolicies.isRO(rq.getLocationList().getReplicaUpdatePolicy())
                    && rq.getLocationList().getNumReplicas() > 1
                    && !rq.getLocationList().getLocalReplica().isComplete()) {
                // read only replication!
                readReplica(rq, args);
            } else {
                if (rq.getLocationList().getLocalReplica().isStriped()) {
                    // striped read
                    stripedRead(rq, args, result);
                } else {
                    // non-striped case
                    nonStripedRead(rq, args, result);
                }
            }
        }

    }

    private void nonStripedRead(OSDRequest rq, readRequest args, ObjectInformation result) {

        final boolean isLastObjectOrEOF = result.getLastLocalObjectNo() <= args.getObjectNumber();
        readFinish(rq, args, result, isLastObjectOrEOF);
    }

    private void stripedRead(final OSDRequest rq, final readRequest args, final ObjectInformation result) {
        InternalObjectData data;
        final long objNo = args.getObjectNumber();
        final long lastKnownObject = Math.max(result.getLastLocalObjectNo(), result.getGlobalLastObjectNo());
        final boolean isLastObjectLocallyKnown = lastKnownObject <= objNo;
        //check if GMAX must be fetched to determin EOF
        if ((objNo > lastKnownObject) ||
                (objNo == lastKnownObject) && (result.getData() != null) && (result.getData().remaining() < result.getStripeSize())) {
            try {
                final List<ServiceUUID> osds = rq.getLocationList().getLocalReplica().getOSDs();
                final RPCResponse[] gmaxRPCs = new RPCResponse[osds.size() - 1];
                int cnt = 0;
                for (ServiceUUID osd : osds) {
                    if (!osd.equals(localUUID)) {
                        gmaxRPCs[cnt++] = master.getOSDClient().xtreemfs_internal_get_gmax(osd.getAddress(), RPCAuthentication.authNone,RPCAuthentication.userService,args.getFileCredentials(),args.getFileId());
                    }
                }
                this.waitForResponses(gmaxRPCs, new ResponsesListener() {

                    @Override
                    public void responsesAvailable() {
                        stripedReadAnalyzeGmax(rq, args, result, gmaxRPCs);
                    }
                });
            } catch (IOException ex) {
                rq.sendInternalServerError(ex);
                return;
            }
        } else {
            readFinish(rq, args, result, isLastObjectLocallyKnown);
        }
    }

    private void stripedReadAnalyzeGmax(final OSDRequest rq, final readRequest args,
            final ObjectInformation result, RPCResponse[] gmaxRPCs) {
        long maxObjNo = -1;
        long maxTruncate = -1;

        try {
            for (int i = 0; i < gmaxRPCs.length; i++) {
                InternalGmax gmax = (InternalGmax) gmaxRPCs[i].get();
                if ((gmax.getLastObjectId() > maxObjNo) && (gmax.getEpoch() >= maxTruncate)) {
                    //found new max
                    maxObjNo = gmax.getLastObjectId();
                    maxTruncate = gmax.getEpoch();
                }
            }
            final boolean isLastObjectLocallyKnown = maxObjNo <= args.getObjectNumber();
            readFinish(rq, args, result, isLastObjectLocallyKnown);
            
            if (args.getFileCredentials().getXcap().getSnapConfig() == SnapConfig.SNAP_CONFIG_ACCESS_SNAP)
                return;
            
            //and update gmax locally
            master.getStorageStage().receivedGMAX_ASYNC(args.getFileId(), maxTruncate, maxObjNo);
            
        } catch (Exception ex) {
            rq.sendInternalServerError(ex);
        } finally {
            for (RPCResponse r : gmaxRPCs)
                r.freeBuffers();
        }

    }

    private void readFinish(OSDRequest rq, readRequest args, ObjectInformation result, boolean isLastObjectOrEOF) {
        //final boolean isRangeRequested = (args.getOffset() > 0) || (args.getLength() < result.getStripeSize());
        InternalObjectData data;
        data = result.getObjectData(isLastObjectOrEOF, args.getOffset(), args.getLength());

        //must deliver enough data!
        int datasize = 0;
        if (data.getData() != null)
            datasize = data.getData().remaining();
        datasize += data.getZero_padding();
        assert((isLastObjectOrEOF && datasize <= args.getLength()) ||
                (!isLastObjectOrEOF && datasize == args.getLength()));
        if (Logging.isDebug() && (datasize == 0)) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "zero data response (EOF), file %s",args.getFileId());
        }
        master.objectSent();
        if (data.getData() != null)
            master.dataSent(data.getData().capacity());

        sendResponse(rq, data);
    }
    
    private void readReplica(final OSDRequest rq, final readRequest args) {
        XLocations xLoc = rq.getLocationList();
        StripingPolicyImpl sp = xLoc.getLocalReplica().getStripingPolicy();
        
        // check if it is a EOF
        if (args.getObjectNumber() > sp.getObjectNoForOffset(xLoc.getXLocSet().getReadOnlyFileSize() - 1)) {
            ObjectInformation objectInfo = new ObjectInformation(ObjectStatus.DOES_NOT_EXIST, null, sp
                    .getStripeSizeForObject(args.getObjectNumber()));
            objectInfo.setGlobalLastObjectNo(xLoc.getXLocSet().getReadOnlyFileSize());

            readFinish(rq, args, objectInfo, true);
        } else {
            master.getReplicationStage().fetchObject(args.getFileId(), args.getObjectNumber(), xLoc,
                    rq.getCapability(), rq.getCowPolicy(), rq, new FetchObjectCallback() {
                        @Override
                        public void fetchComplete(ObjectInformation objectInfo, ErrorResponse error) {
                            postReadReplica(rq, args, objectInfo, error);
                        }
                    });
        }
    }

    public void postReadReplica(final OSDRequest rq, readRequest args, ObjectInformation result, ErrorResponse error) {
        XLocations xLoc = rq.getLocationList();
        StripingPolicyImpl sp = xLoc.getLocalReplica().getStripingPolicy();

        if (error != null) {
            rq.sendError(error);
        } else {
            try {
                // replication always delivers full objects => cut data
                if (args.getOffset() > 0 || args.getLength() < result.getStripeSize()) {
                    if (result.getStatus() == ObjectStatus.EXISTS) {
                        // cut range from object data
                        final int availData = result.getData().remaining();
                        if (availData - args.getOffset() <= 0) {
                            // offset is beyond available data
                            BufferPool.free(result.getData());
                            result.setData(BufferPool.allocate(0));
                        } else {
                            if (availData - args.getOffset() >= args.getLength()) {
                                result.getData().range(args.getOffset(), args.getLength());
                            } else {
                                // less data than requested
                                result.getData().range(args.getOffset(), availData - args.getOffset());
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                rq.sendInternalServerError(ex);
                return;
            }

            if (args.getObjectNumber() == sp.getObjectNoForOffset(xLoc.getXLocSet().getReadOnlyFileSize() - 1))
                // last object
                readFinish(rq, args, result, true);
            else
                readFinish(rq, args, result, false);
        }
    }

    public void sendResponse(OSDRequest rq, InternalObjectData result) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, result.toString());
        }
        rq.sendSuccess(result.getMetadata(),result.getData());
    }


    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            readRequest rpcrq = (readRequest)rq.getRequestArgs();
            rq.setFileId(rpcrq.getFileCredentials().getXcap().getFileId());
            rq.setCapability(new Capability(rpcrq.getFileCredentials().getXcap(), sharedSecret));
            rq.setLocationList(new XLocations(rpcrq.getFileCredentials().getXlocs(), localUUID));

            return null;
        } catch (InvalidXLocationsException ex) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, ex.toString());
        } catch (Throwable ex) {
            return ErrorUtils.getInternalServerError(ex);
        }
    }

    @Override
    public boolean requiresCapability() {
        return true;
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
