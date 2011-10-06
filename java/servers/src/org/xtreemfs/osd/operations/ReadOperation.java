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
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
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
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.ObjectInformation.ObjectStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.InternalGmax;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.readRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class ReadOperation extends OSDOperation {

    private final String sharedSecret;
    private final ServiceUUID localUUID;

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
    public ErrorResponse startRequest(final OSDRequest rq, final RPCRequestCallback callback) {
        
        final readRequest args = (readRequest) rq.getRequestArgs();

        if (args.getObjectNumber() < 0) {

            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, 
                    "object number must be >= 0");
        }

        if (args.getOffset() < 0) {
            
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "offset must be >= 0");
        }

        if (args.getLength() < 0) {
            
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "length must be >= 0");
        }

        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

        if (args.getLength()+args.getOffset() > sp.getStripeSizeForObject(0)) {
            
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, 
                    "length + ofset must be <= " + sp.getStripeSizeForObject(0)+" (stripe size)");
        }

        if ( (rq.getLocationList().getReplicaUpdatePolicy().length() == 0)
            || (rq.getLocationList().getNumReplicas() == 1)
            || (rq.getLocationList().getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY))){

           final long snapVerTS = rq.getCapability().getSnapConfig() == SnapConfig.SNAP_CONFIG_ACCESS_SNAP ? 
                    rq.getCapability().getSnapTimestamp(): 0;

            master.getStorageStage().readObject(args.getObjectNumber(), sp, args.getOffset(),args.getLength(), 
                    snapVerTS, rq, new AbstractRPCRequestCallback(callback) {
                        
                        @Override
                        public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                                throws ErrorResponseException {
                            
                            return postRead(rq, args, (ObjectInformation) result, callback);
                        }
                    });
        } else {
            rwReplicatedRead(rq, args, callback);
        }
        
        return null;
    }

    private void rwReplicatedRead(final OSDRequest rq, final readRequest args, final RPCRequestCallback callback) {
        
        master.getRWReplicationStage().prepareOperation(args.getFileCredentials(), rq.getLocationList(), 
                args.getObjectNumber(), args.getObjectVersion(), RWReplicationStage.Operation.READ, 
                new AbstractRPCRequestCallback(callback) {
                
            @Override
            public <S extends StageRequest<?>> boolean success(Object newObjectVersion, S stageRequest)
                    throws ErrorResponseException {

                final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

                final long snapVerTS = rq.getCapability().getSnapConfig() == SnapConfig.SNAP_CONFIG_ACCESS_SNAP ? 
                        rq.getCapability().getSnapTimestamp() : 0;

                master.getStorageStage().readObject(args.getObjectNumber(), sp,
                    args.getOffset(), args.getLength(), snapVerTS, rq, new AbstractRPCRequestCallback(callback) {
                    
                    @Override
                    public <T extends StageRequest<?>> boolean success(Object result, T stageRequest)
                            throws ErrorResponseException {
                        
                        return postRead(rq, args, (ObjectInformation) result, callback);
                    }
                });

                return true;
            }
        }, rq);
    }

    private boolean postRead(OSDRequest rq, readRequest args, ObjectInformation result, RPCRequestCallback callback) 
            throws ErrorResponseException {

        if (result.getStatus() == ObjectInformation.ObjectStatus.DOES_NOT_EXIST
                && rq.getLocationList().getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY)
                && rq.getLocationList().getNumReplicas() > 1
                && !rq.getLocationList().getLocalReplica().isComplete()) {
            
            // read only replication!
            return readReplica(rq, args, callback);
        } else {
            
            if (rq.getLocationList().getLocalReplica().isStriped()) {
                
                // striped read
                return stripedRead(rq, args, result, callback);
            } else {
                
                // non-striped case
                return nonStripedRead(args, result, callback);
            }
        }
    }

    private boolean nonStripedRead(readRequest args, ObjectInformation result, RPCRequestCallback callback) 
            throws ErrorResponseException {

        boolean isLastObjectOrEOF = result.getLastLocalObjectNo() <= args.getObjectNumber();
        return readFinish(args, result, isLastObjectOrEOF, callback);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private boolean stripedRead(OSDRequest rq, final readRequest args, final ObjectInformation result, 
            final RPCRequestCallback callback) throws ErrorResponseException {
        
        long objNo = args.getObjectNumber();
        long lastKnownObject = Math.max(result.getLastLocalObjectNo(), result.getGlobalLastObjectNo());
        boolean isLastObjectLocallyKnown = lastKnownObject <= objNo;
        
        //check if GMAX must be fetched to determin EOF
        if ((objNo > lastKnownObject) ||
            (objNo == lastKnownObject) && 
            (result.getData() != null) && 
            (result.getData().remaining() < result.getStripeSize())) {
            
            try {
                List<ServiceUUID> osds = rq.getLocationList().getLocalReplica().getOSDs();
                final RPCResponse[] gmaxRPCs = new RPCResponse[osds.size() - 1];
                int cnt = 0;
                for (ServiceUUID osd : osds) {
                    if (!osd.equals(localUUID)) {
                        gmaxRPCs[cnt++] = master.getOSDClient().xtreemfs_internal_get_gmax(osd.getAddress(), RPCAuthentication.authNone,RPCAuthentication.userService,args.getFileCredentials(),args.getFileId());
                    }
                }
                waitForResponses(gmaxRPCs, new ResponsesListener() {

                    @Override
                    public void responsesAvailable() {
                        stripedReadAnalyzeGmax(args, result, gmaxRPCs, callback);
                    }
                });
                
                return true;
            } catch (IOException ex) {
                
                throw new ErrorResponseException(ex);
            }
        } else {
            return readFinish(args, result, isLastObjectLocallyKnown, callback);
        }
    }

    @SuppressWarnings("rawtypes")
    private void stripedReadAnalyzeGmax(final readRequest args, final ObjectInformation result, RPCResponse[] gmaxRPCs, 
            RPCRequestCallback callback) {
        
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
            boolean isLastObjectLocallyKnown = maxObjNo <= args.getObjectNumber();
            readFinish(args, result, isLastObjectLocallyKnown, callback);
            
            if (args.getFileCredentials().getXcap().getSnapConfig() == SnapConfig.SNAP_CONFIG_ACCESS_SNAP)
                return;
            
            //and update gmax locally
            master.getStorageStage().receivedGMAX_ASYNC(args.getFileId(), maxTruncate, maxObjNo);
            
        } catch (Exception ex) {
            
            callback.failed(ex);
        } finally {
            
            for (RPCResponse r : gmaxRPCs) {
                if (r != null) r.freeBuffers();
            }
        }
    }

    private boolean readFinish(readRequest args, ObjectInformation result, boolean isLastObjectOrEOF, 
            RPCRequestCallback callback) throws ErrorResponseException {
        
        final InternalObjectData data = result.getObjectData(isLastObjectOrEOF, args.getOffset(), args.getLength());

        //must deliver enough data!
        int datasize = 0;
        if (data.getData() != null)
            datasize = data.getData().remaining();
        datasize += data.getZero_padding();
        assert((isLastObjectOrEOF && datasize <= args.getLength()) ||
                (!isLastObjectOrEOF && datasize == args.getLength()));
        if (Logging.isDebug() && (datasize == 0)) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "zero data response (EOF), file %s",
                    args.getFileId());
        }
        master.objectSent();
        if (data.getData() != null)
            master.dataSent(data.getData().capacity());

        return callback.success(data.getMetadata(), data.getData());
    }
    
    private boolean readReplica(final OSDRequest rq, final readRequest args, final RPCRequestCallback callback) 
            throws ErrorResponseException {
        
        final XLocations xLoc = rq.getLocationList();
        final StripingPolicyImpl sp = xLoc.getLocalReplica().getStripingPolicy();
        
        // check if it is a EOF
        if (args.getObjectNumber() > sp.getObjectNoForOffset(xLoc.getXLocSet().getReadOnlyFileSize() - 1)) {
            
            ObjectInformation objectInfo = new ObjectInformation(ObjectStatus.DOES_NOT_EXIST, null, sp
                    .getStripeSizeForObject(args.getObjectNumber()));
            objectInfo.setGlobalLastObjectNo(xLoc.getXLocSet().getReadOnlyFileSize());

            return readFinish(args, objectInfo, true, callback);
        } else {
            
            master.getReplicationStage().fetchObject(args.getFileId(), args.getObjectNumber(), xLoc,
                    rq.getCapability(), rq.getCowPolicy(), rq, new AbstractRPCRequestCallback(callback) {
                        
                // executed by the replication stage
                @Override
                public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                        throws ErrorResponseException {
                   
                    return postReadReplica(rq, args, (ObjectInformation) result, callback);
                }
            });
            
            return true;
        }
    }

    private boolean postReadReplica(OSDRequest rq, readRequest args, ObjectInformation result, 
            RPCRequestCallback callback) throws ErrorResponseException {
        
        final XLocations xLoc = rq.getLocationList();
        final StripingPolicyImpl sp = xLoc.getLocalReplica().getStripingPolicy();

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

        if (args.getObjectNumber() == sp.getObjectNoForOffset(xLoc.getXLocSet().getReadOnlyFileSize() - 1)) {
            
            // last object
            return readFinish(args, result, true, callback);
        } else {
            
            return readFinish(args, result, false, callback);
        }
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        
        try {
            readRequest rpcrq = (readRequest)rq.getRequestArgs();
            rq.setFileId(rpcrq.getFileId());
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