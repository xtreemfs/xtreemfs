/*
 * Copyright (c) 2009 Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import com.google.protobuf.ByteString;
import java.io.IOException;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.ObjectInformation.ObjectStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.InternalReadLocalResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectList;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_read_localRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class LocalReadOperation extends OSDOperation {
    
    private final String      sharedSecret;
    private final ServiceUUID localUUID;
    
    public LocalReadOperation(OSDRequestDispatcher master) {
        super(master);
        
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }
    
    @Override
    public int getProcedureId() {
        
        return OSDServiceConstants.PROC_ID_XTREEMFS_INTERNAL_READ_LOCAL;
    }
    
    @Override
    public ErrorResponse startRequest(final OSDRequest rq, final RPCRequestCallback callback) {
        
        final xtreemfs_internal_read_localRequest args = (xtreemfs_internal_read_localRequest) rq.getRequestArgs();
        
        if (args.getObjectNumber() < 0) {

            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, 
                    "object number must be >= 0");
        }
        
        StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();
        
        master.getStorageStage().readObject(args.getObjectNumber(), sp,
            (int) args.getOffset(), (int) args.getLength(),
            rq.getCapability().getSnapConfig() == SnapConfig.SNAP_CONFIG_ACCESS_SNAP ? rq.getCapability()
                    .getSnapTimestamp() : 0, rq, new AbstractRPCRequestCallback(callback) {
                        
                        @Override
                        public boolean success(Object result) {
                            
                            return postRead(rq, args, (ObjectInformation) result, callback);
                        }
                    });
        
        return null;
    }
    
    private boolean postRead(final OSDRequest rq, final xtreemfs_internal_read_localRequest args,
        final ObjectInformation result, final RPCRequestCallback callback) {

        // object list is requested
        if (args.getAttachObjectList()) { 
            
            master.getStorageStage().getObjectSet(rq.getLocationList().getLocalReplica().getStripingPolicy(),  rq, 
                    new AbstractRPCRequestCallback(callback) {
                        
                        @Override
                        public boolean success(Object result2) {
                            return postReadObjectSet(args, result, (ObjectSet) result2, callback);
                        }
            });
            
            return true;
        } else {
            
            return readFinish(args, result, null, callback);
        }
    }
    
    private boolean postReadObjectSet(xtreemfs_internal_read_localRequest args, ObjectInformation data, 
            ObjectSet result, RPCRequestCallback callback) {

        // serialize objectSet
        byte[] serialized;
        try {
            
            serialized = result.getSerializedBitSet();
            ObjectList objList = ObjectList.newBuilder().setSet(ByteString.copyFrom(serialized)).setFirst(
                    result.getFirstObjectNo()).setStripeWidth(result.getStripeWidth()).build();
            return readFinish(args, data, objList, callback);
        } catch (IOException e) {
            
            callback.failed(e);
            return false;
        }
    }
    
    private boolean readFinish(xtreemfs_internal_read_localRequest args, ObjectInformation result, 
            ObjectList objectList, RPCRequestCallback callback) {
        
        InternalObjectData data = null;
        // send raw data
        if (result.getStatus() == ObjectStatus.EXISTS) {
            final boolean isRangeRequested = (args.getOffset() > 0)
                || (args.getLength() < result.getStripeSize());
            if (isRangeRequested) {
                data = result.getObjectData(true, (int) args.getOffset(), (int) args.getLength());
            } else {
                data = new InternalObjectData(0, result.isChecksumInvalidOnOSD(), 0, result.getData());
            }
        }

        else if (result.getStatus() == ObjectStatus.PADDING_OBJECT) {
            final boolean isRangeRequested = (args.getOffset() > 0)
                || (args.getLength() < result.getStripeSize());
            if (isRangeRequested) {
                data = result.getObjectData(true, (int) args.getOffset(), (int) args.getLength());
            } else {
                data = result.getObjectData(false, 0, (int) args.getLength());
            }
        } else {
            data = new InternalObjectData(0, result.isChecksumInvalidOnOSD(), 0, null);
        }
        
        master.objectSent();
        if (data.getData() != null) {
            master.dataSent(data.getData().capacity());
        }
        
        InternalReadLocalResponse.Builder readLocalResponse = 
            InternalReadLocalResponse.newBuilder().setData(data.getMetadata());
        if (objectList != null)
            readLocalResponse.addObjectSet(objectList);
        
        return callback.success(readLocalResponse.build(), data.getData());
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        
        try {
            xtreemfs_internal_read_localRequest rpcrq = (xtreemfs_internal_read_localRequest)rq.getRequestArgs();
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