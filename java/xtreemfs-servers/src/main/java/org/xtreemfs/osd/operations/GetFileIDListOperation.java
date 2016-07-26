/*
 * Copyright (c) 2009-2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.stages.StorageStage.GetFileIDListCallback;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.truncateRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_internal_get_fileid_listResponse;


public class GetFileIDListOperation extends OSDOperation {
    
    final String sharedSecret;

    final ServiceUUID localUUID;

    public GetFileIDListOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_INTERNAL_GET_FILEID_LIST;
    }
    
    @Override
    public void startRequest(final OSDRequest rq) {

        master.getStorageStage().getFileIDList(rq, new GetFileIDListCallback() {
            
            @Override
            public void createGetFileIDListComplete(ArrayList<String> fileIDList, ErrorResponse error) {
                
                postGetFileIDList(rq, fileIDList, error);
            }
        });
    }

    private void postGetFileIDList(final OSDRequest rq, ArrayList<String> fileIDList, ErrorResponse error) {
        
        if (error != null) {
            rq.sendError(error);
        } else {
            try {
                xtreemfs_internal_get_fileid_listResponse.Builder responseBuilder = xtreemfs_internal_get_fileid_listResponse.newBuilder();
                for (String fileID : fileIDList) {
                    responseBuilder.addFileIds(fileID);
                }
                rq.sendSuccess(responseBuilder.build(), null);
                
            } catch (Exception e) {
                rq.sendError(ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, e.toString()));
            }
        }
        
    }
    
    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean requiresCapability() {
        return false;
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}
