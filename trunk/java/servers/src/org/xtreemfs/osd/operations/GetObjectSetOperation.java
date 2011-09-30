/*
 * Copyright (c) 2009 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import com.google.protobuf.ByteString;
import java.io.IOException;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.stages.StorageStage.GetObjectListCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectList;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_object_setRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/**
 *
 * <br>15.06.2009
 */
public class GetObjectSetOperation extends OSDOperation {

    final String sharedSecret;

    final ServiceUUID localUUID;

    public GetObjectSetOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_INTERNAL_GET_OBJECT_SET;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_internal_get_object_setRequest args = (xtreemfs_internal_get_object_setRequest) rq
                .getRequestArgs();

//        System.out.println("rq: " + args);

        master.getStorageStage().getObjectSet(args.getFileId(), rq.getLocationList().getLocalReplica().getStripingPolicy(), rq,
                new GetObjectListCallback() {
                    @Override
                    public void getObjectSetComplete(ObjectSet result, ErrorResponse error) {
                        postReadObjectList(rq, args, result, error);
                    }
                });
    }

    public void postReadObjectList(final OSDRequest rq, xtreemfs_internal_get_object_setRequest args,
            ObjectSet result, ErrorResponse error) {
        if (error != null) {
            rq.sendError(error);
        } else {
            // serialize objectSet
            byte[] serialized;
            try {
                serialized = result.getSerializedBitSet();

                ObjectList objList = ObjectList.newBuilder().setSet(ByteString.copyFrom(serialized)).setStripeWidth(result.getStripeWidth()).setFirst(result.getFirstObjectNo()).build();
                rq.sendSuccess(objList,null);
            } catch (IOException e) {
                rq.sendInternalServerError(e);
            }
        }
    }


    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_internal_get_object_setRequest rpcrq = (xtreemfs_internal_get_object_setRequest)rq.getRequestArgs();
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
