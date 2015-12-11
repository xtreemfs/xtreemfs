/*
 * Copyright (c) 2015 by Jan Fajerski,
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
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.FileOperationCallback;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.RedundancyStage;
import org.xtreemfs.osd.ec.ECStage;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_ec_diffs;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/**
 * @author Jan Fajerski
 */
public class InternalECDiffsOperation extends OSDOperation{

    final String sharedSecret;
    final ServiceUUID localUUID;

    public InternalECDiffsOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_EC_DIFF_DISTRIBUTE;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_ec_diffs args = (xtreemfs_ec_diffs) rq.getRequestArgs();

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.ec, this,
            "received diff update for file %s to object %s version",
            args.getFileId(), args.getObjectNumber(), args.getObjectVersion());
        }

        // is this necessary here? diff distribution should work regardless of leases
        prepareRequest(rq, args);
    }

    void prepareRequest(final OSDRequest rq, final xtreemfs_ec_diffs args) {
        master.getECStage().prepareOperation(args.getFileCredentials(), rq.getLocationList(),
                args.getObjectNumber(), args.getObjectVersion(), ECStage.Operation.WRITE, rq,
                new FileOperationCallback() {
                    @Override
                    public void success(long newObjectVersion) {
                        executeRequest(rq, args);
                    }

                    @Override
                    public void redirect(String redirectTo) {

                    }

                    @Override
                    public void failed(RPC.RPCHeader.ErrorResponse er) {
                        rq.sendError(er);
                    }
                });

    }

    void executeRequest(final OSDRequest rq, final xtreemfs_ec_diffs args) {
        master.getECStage().addDiff(args.getObjectNumber(), args.getOffset(), args.getObjectVersion(), rq,
                new FileOperationCallback() {
                    @Override
                    public void success(long newObjectVersion) {
                        rq.sendSuccess(null, null);
                    }

                    @Override
                    public void redirect(String redirectTo) {

                    }

                    @Override
                    public void failed(RPC.RPCHeader.ErrorResponse er) {
                        rq.sendError(er);
                    }
                });
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RPC.RPCHeader.ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_ec_diffs rpc_request = (xtreemfs_ec_diffs)rq.getRequestArgs();
            rq.setFileId(rpc_request.getFileId());
            rq.setCapability(new Capability(rpc_request.getFileCredentials().getXcap(), sharedSecret));
            rq.setLocationList(new XLocations(rpc_request.getFileCredentials().getXlocs(), localUUID));

            return null;
        } catch (InvalidXLocationsException ex) {
            return ErrorUtils.getErrorResponse(RPC.ErrorType.ERRNO, RPC.POSIXErrno.POSIX_ERROR_EINVAL, ex.toString());
        } catch (Throwable ex) {
            return ErrorUtils.getInternalServerError(ex);
        }
    }

    @Override
    public boolean requiresCapability() {
        return true;
    }
}
