/*
 * Copyright (c) 2012-2013 by Johannes Dillmann,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.mrc.stages.XLocSetCoordinator;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.PreprocStage.InvalidateXLocSetCallback;
import org.xtreemfs.osd.stages.StorageStage.InternalGetReplicaStateCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.LeaseState;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ReplicaStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_xloc_set_invalidateRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_xloc_set_invalidateResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/**
 * Invalidates the XLocSet (view) on a certain replica.<br>
 * Invalidated replicas won't respond to operations until a newer XLocSet is installed. The installation will be
 * executed implicitly when a operation with a newer XLocSet is requested.<br>
 * This operation is intended to be called from the MRCs {@link XLocSetCoordinator}.
 */
public class InvalidateXLocSetOperation extends OSDOperation {
    final String      sharedSecret;
    final ServiceUUID localUUID;

    public InvalidateXLocSetOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();

    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_XLOC_SET_INVALIDATE;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        xtreemfs_xloc_set_invalidateRequest rpcrq = (xtreemfs_xloc_set_invalidateRequest) rq.getRequestArgs();

        master.getPreprocStage().invalidateXLocSet(rq, rpcrq.getFileCredentials(), true,
                new InvalidateXLocSetCallback() {

                    @Override
                    public void invalidateComplete(LeaseState leaseState, ErrorResponse error) {
                        if (error != null) {
                            rq.sendError(error);
                        } else {
                            postInvalidation(rq, leaseState);
                        }
                    }
                });
    }

    private void postInvalidation(final OSDRequest rq, final LeaseState leaseState) {
        if (ReplicaUpdatePolicies.isRW(rq.getLocationList().getReplicaUpdatePolicy())) {
            master.getStorageStage().internalGetReplicaState(rq.getFileId(),
                    rq.getLocationList().getLocalReplica().getStripingPolicy(), 0,
                    new InternalGetReplicaStateCallback() {

                        @Override
                        public void getReplicaStateComplete(ReplicaStatus localState, ErrorResponse error) {
                            if (error != null) {
                                rq.sendError(error);
                            } else {
                                invalidationFinished(rq, leaseState, localState);
                            }
                        }
                    });
            // FIXME (jdillmann): Handle EC Policy?
        } else {

            invalidationFinished(rq, leaseState, null);
        }
    }

    private void invalidationFinished(OSDRequest rq, LeaseState leaseState, ReplicaStatus localState) {
        xtreemfs_xloc_set_invalidateResponse.Builder response = xtreemfs_xloc_set_invalidateResponse.newBuilder();
        response.setLeaseState(leaseState);

        if (localState != null) {
            response.setReplicaStatus(localState);
        }
        
        rq.sendSuccess(response.build(), null);
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_xloc_set_invalidateRequest rpcrq = (xtreemfs_xloc_set_invalidateRequest) rq.getRequestArgs();
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
        // View validation will be handled at {@link PreprocStage#invalidateXLocSet()}.
        return true;
    }
}
