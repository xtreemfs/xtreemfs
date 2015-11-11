/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.FileOperationCallback;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.osd.stages.StorageStage.TruncateCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_rwr_truncateRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class InternalRWRTruncateOperation extends OSDOperation {

    final String sharedSecret;
    final ServiceUUID localUUID;

    public InternalRWRTruncateOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_RWR_TRUNCATE;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_rwr_truncateRequest args = (xtreemfs_rwr_truncateRequest)rq.getRequestArgs();

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"RWR truncate for file %s objVer %d",args.getFileId(),
                    args.getObjectVersion());
        }

       prepareLocalTruncate(rq,args);
    }
    
    public void localTruncate(final OSDRequest rq, final xtreemfs_rwr_truncateRequest args) {
         if (args.getNewFileSize() < 0) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "new_file_size for truncate must be >= 0");
            return;
        }

        master.getStorageStage().truncate(args.getFileId(), args.getNewFileSize(),
            rq.getLocationList().getLocalReplica().getStripingPolicy(),
            rq.getLocationList().getLocalReplica(), rq.getCapability().getEpochNo(), rq.getCowPolicy(),
            args.getObjectVersion(), true, rq,
            new TruncateCallback() {

            @Override
            public void truncateComplete(OSDWriteResponse result, ErrorResponse error) {
                sendResult(rq, error);
            }
        });
    }

    public void prepareLocalTruncate(final OSDRequest rq, final xtreemfs_rwr_truncateRequest args) {
        master.getRWReplicationStage().prepareOperation(args.getFileCredentials(), rq.getLocationList(), 0, args.getObjectVersion(),
                RWReplicationStage.Operation.INTERNAL_TRUNCATE, rq, new FileOperationCallback() {

            @Override
            public void success(long newObjectVersion) {
                localTruncate(rq, args);
            }

            @Override
            public void redirect(String redirectTo) {
                rq.getRPCRequest().sendRedirect(redirectTo);
            }

            @Override
            public void failed(ErrorResponse err) {
                rq.sendError(err);
            }
        });
    }


    public void sendResult(final OSDRequest rq, ErrorResponse error) {

        if (error != null) {
            rq.sendError(error);
        } else {
            //only locally
            sendResponse(rq);
        }
    }

    
    public void sendResponse(OSDRequest rq) {
        rq.sendSuccess(null,null);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_rwr_truncateRequest rpcrq = (xtreemfs_rwr_truncateRequest)rq.getRequestArgs();
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