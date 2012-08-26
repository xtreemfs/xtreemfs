/*
 * Copyright (c) 2012 by Matthias Noack,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.net.InetSocketAddress;

import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.VivaldiClientMap;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

import com.google.protobuf.Message;

public class UpdateVivaldiClientOperation extends DIROperation {
    
    
    //private HashMap<InetSocketAddress, VivaldiClientValue> vivaldiClientMap;
    private VivaldiClientMap vivaldiClientMap;
    
    public UpdateVivaldiClientOperation(DIRRequestDispatcher master) {
        super(master);
        vivaldiClientMap = master.getVivaldiClientMap();
    }
    
    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_VIVALDI_CLIENT_UPDATE;
    }
    
    @Override
    public void startRequest(DIRRequest rq) {
        VivaldiCoordinates coords = (VivaldiCoordinates) rq.getRequestMessage();
        InetSocketAddress clientAddress = (InetSocketAddress)rq.getRPCRequest().getSenderAddress(); 
        vivaldiClientMap.put(clientAddress, coords);
        //vivaldiClientMap.filterTimeOuts();
        // send response
        rq.sendSuccess(emptyResponse.getDefaultInstance());
    }
    
    @Override
    public boolean isAuthRequired() {
        return false;
    }
    
    @Override
    protected Message getRequestMessagePrototype() {
        return VivaldiCoordinates.getDefaultInstance();
    }
    
    @Override
    void requestFinished(Object result, DIRRequest rq) {
        // NOTE(mno): this method is not called, a response is sent directly in startRequest
        //rq.sendSuccess(emptyResponse.getDefaultInstance());
    }
}
