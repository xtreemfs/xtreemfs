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
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.InternalGmax;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_file_sizeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_file_sizeResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class InternalGetFileSizeOperation extends OSDOperation {

    private final String sharedSecret;
    private final ServiceUUID localUUID;

    public InternalGetFileSizeOperation(OSDRequestDispatcher master) {
        super(master);
        
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        
        return OSDServiceConstants.PROC_ID_XTREEMFS_INTERNAL_GET_FILE_SIZE;
    }

    @Override
    public ErrorResponse startRequest(final OSDRequest rq, final RPCRequestCallback callback) {
        
        final xtreemfs_internal_get_file_sizeRequest args = 
            (xtreemfs_internal_get_file_sizeRequest) rq.getRequestArgs();

        StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

        master.getStorageStage().getFilesize(sp, rq.getCapability().getSnapTimestamp(), rq, 
                new AbstractRPCRequestCallback(callback) {
            
            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                    throws ErrorResponseException {
                
                return step2(rq, args, (Long) result, callback);
            }
        });
        
        return null;
    }

    private boolean step2(OSDRequest rq, xtreemfs_internal_get_file_sizeRequest args, long localFS, 
            RPCRequestCallback callback) throws ErrorResponseException {

        if (rq.getLocationList().getLocalReplica().isStriped()) {
            
            //striped read
            stripedGetFS(rq, args, localFS, callback);
            return true;
        } else {
            //non-striped case
            return sendResponse(localFS, callback);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void stripedGetFS(OSDRequest rq, xtreemfs_internal_get_file_sizeRequest args, final long localFS, 
            final RPCRequestCallback callback) throws ErrorResponseException {
        
        try {
            
            List<ServiceUUID> osds = rq.getLocationList().getLocalReplica().getOSDs();
            final RPCResponse[] gmaxRPCs = new RPCResponse[osds.size() - 1];
            int cnt = 0;
            for (ServiceUUID osd : osds) {
                if (!osd.equals(localUUID)) {
                    gmaxRPCs[cnt++] = master.getOSDClient().xtreemfs_internal_get_gmax(osd.getAddress(), 
                            RPCAuthentication.authNone, RPCAuthentication.userService, args.getFileCredentials(), 
                            args.getFileId());
                }
            }
            waitForResponses(gmaxRPCs, new ResponsesListener() {

                @Override
                public void responsesAvailable() {
                    stripedReadAnalyzeGmax(localFS, gmaxRPCs, callback);
                }
            });
            
        } catch (IOException ex) {
            
            throw new ErrorResponseException(ex);
        }
    }

    @SuppressWarnings("rawtypes")
    private void stripedReadAnalyzeGmax(long localFS, RPCResponse[] gmaxRPCs, RPCRequestCallback callback) {
        
        long maxFS = localFS;

        try {
            
            for (int i = 0; i < gmaxRPCs.length; i++) {
                InternalGmax gmax = (InternalGmax) gmaxRPCs[i].get();
                if (gmax.getFileSize() > maxFS) {
                    //found new max
                    maxFS = gmax.getFileSize();
                }
            }
            sendResponse(maxFS, callback);
        } catch (Exception e) {
            
            callback.failed(e);
        } finally {
            
            for (RPCResponse r : gmaxRPCs) {
                r.freeBuffers();
            }
        }
    }

    private boolean sendResponse(long fileSize, RPCRequestCallback callback) throws ErrorResponseException {
        
        return callback.success(xtreemfs_internal_get_file_sizeResponse.newBuilder().setFileSize(fileSize).build());
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        
        try {
            xtreemfs_internal_get_file_sizeRequest rpcrq = (xtreemfs_internal_get_file_sizeRequest)rq.getRequestArgs();
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