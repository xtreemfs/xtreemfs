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
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.StorageStage.ReadObjectCallback;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.InternalGmax;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_check_objectRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class CheckObjectOperation extends OSDOperation {

    final String sharedSecret;

    final ServiceUUID localUUID;

    public CheckObjectOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_CHECK_OBJECT;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_check_objectRequest args = (xtreemfs_check_objectRequest) rq.getRequestArgs();

        if (args.getObjectNumber() < 0) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "object number must be >= 0");
            return;
        }

        master.getStorageStage().readObject(args.getFileId(), args.getObjectNumber(), rq.getLocationList().getLocalReplica().getStripingPolicy(),
                0, StorageLayout.FULL_OBJECT_LENGTH, rq.getCapability().getSnapConfig() == SnapConfig.SNAP_CONFIG_ACCESS_SNAP ? rq.getCapability()
                        .getSnapTimestamp() : 0, rq, new ReadObjectCallback() {

                    @Override
                    public void readComplete(ObjectInformation result, ErrorResponse error) {
                        step2(rq, args, result, error);
                    }
                });
    }

    public void step2(final OSDRequest rq, xtreemfs_check_objectRequest args, ObjectInformation result, ErrorResponse error) {
        if (error != null) {
            rq.sendError(error);
        } else {
            if (rq.getLocationList().getLocalReplica().getOSDs().size() == 1) {
                //non-striped case
                nonStripedCheckObject(rq, args, result);
            } else {
                //striped read
                stripedCheckObject(rq, args, result);
            }

        }

    }

    private void nonStripedCheckObject(OSDRequest rq, xtreemfs_check_objectRequest args, ObjectInformation result) {

        final boolean isLastObjectOrEOF = result.getLastLocalObjectNo() <= args.getObjectNumber();
        readFinish(rq, args, result, isLastObjectOrEOF);
    }

    private void stripedCheckObject(final OSDRequest rq, final xtreemfs_check_objectRequest args, final ObjectInformation result) {
        //ObjectData data;
        final long objNo = args.getObjectNumber();
        final long lastKnownObject = Math.max(result.getLastLocalObjectNo(), result.getGlobalLastObjectNo());
        final boolean isLastObjectLocallyKnown = lastKnownObject <= objNo;
        //check if GMAX must be fetched to determine EOF
        if ((objNo > lastKnownObject) ||
                (objNo == lastKnownObject) && (result.getData() != null) && (result.getData().remaining() < result.getStripeSize())) {
            try {
                final List<ServiceUUID> osds = rq.getLocationList().getLocalReplica().getOSDs();
                final RPCResponse[] gmaxRPCs = new RPCResponse[osds.size() - 1];
                int cnt = 0;
                for (ServiceUUID osd : osds) {
                    if (!osd.equals(localUUID)) {
                        gmaxRPCs[cnt++] = master.getOSDClient().xtreemfs_internal_get_gmax(osd.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, args.getFileCredentials(), args.getFileId());
                    }
                }
                this.waitForResponses(gmaxRPCs, new ResponsesListener() {

                    @Override
                    public void responsesAvailable() {
                        stripedCheckObjectAnalyzeGmax(rq, args, result, gmaxRPCs);
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

    private void stripedCheckObjectAnalyzeGmax(final OSDRequest rq, final xtreemfs_check_objectRequest args,
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
            //and update gmax locally
            master.getStorageStage().receivedGMAX_ASYNC(args.getFileId(), maxTruncate, maxObjNo);
        } catch (Exception ex) {
            rq.sendInternalServerError(ex);
        } finally {
            for (RPCResponse r : gmaxRPCs)
                r.freeBuffers();
        }

    }

    private void readFinish(OSDRequest rq, xtreemfs_check_objectRequest args, ObjectInformation result, boolean isLastObjectOrEOF) {

        InternalObjectData data;
        data = result.getObjectData(isLastObjectOrEOF, 0, result.getStripeSize());
        if (data.getData() != null) {
            data.setZero_padding(data.getZero_padding() + data.getData().remaining());
            BufferPool.free(data.getData());
            data.setData(null);
        }
        sendResponse(rq, data);
    }

    public void sendResponse(OSDRequest rq, InternalObjectData result) {
        rq.sendSuccess(result.getMetadata(), null);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_check_objectRequest rpcrq = (xtreemfs_check_objectRequest) rq.getRequestArgs();
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