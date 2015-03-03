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
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.StorageStage.ReadObjectCallback;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_rwr_fetchRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class InternalRWRFetchOperation extends OSDOperation {

    final String sharedSecret;
    final ServiceUUID localUUID;

    public InternalRWRFetchOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_RWR_FETCH;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_rwr_fetchRequest args = (xtreemfs_rwr_fetchRequest)rq.getRequestArgs();

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"RWR fetch request for file %s-%d",args.getFileId(),args.getObjectNumber());
        }

        fetchObject(rq,args);

    }
    
    public void fetchObject(final OSDRequest rq, final xtreemfs_rwr_fetchRequest args) {
        master.getStorageStage().readObject(rq.getFileId(), args.getObjectNumber(),
                rq.getLocationList().getLocalReplica().getStripingPolicy(), 0, -1, 0, rq, new ReadObjectCallback() {

            @Override
            public void readComplete(ObjectInformation result, ErrorResponse error) {
                if (error != null)
                    sendResult(rq, null, error);
                else {
                    InternalObjectData odata = new InternalObjectData(0, false, 0, result.getData());
                    sendResult(rq, odata, null);
                }
            }
        });
    }

    public void sendResult(final OSDRequest rq, InternalObjectData response, ErrorResponse error) {

        if (error != null) {
            rq.sendError(error);
        } else {
            //only locally
           rq.sendSuccess(response.getMetadata(),response.getData());
        }
    }


    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_rwr_fetchRequest rpcrq = (xtreemfs_rwr_fetchRequest)rq.getRequestArgs();
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
    public boolean bypassViewValidation() {
        // This operation has to be used while the replicas are invalidated and a reset triggered
        // by InternalRWRAuthStateInvalidatedOperation.
        return true;
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}