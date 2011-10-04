/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.common.olp.OLPStageRequest;
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.BabuDBComponent;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceGetByUUIDRequest;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class ServiceOfflineOperation extends DIROperation {

    private final BabuDBComponent<DIRRequest> component;
    private final Database                    database;
    
    public ServiceOfflineOperation(DIRRequestDispatcher master) {
        super(master);
        
        component = master.getBabuDBComponent();
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_SERVICE_OFFLINE;
    }
    
    @Override
    public void startRequest(DIRRequest rq, final RPCRequestCallback callback) {

        final serviceGetByUUIDRequest request = (serviceGetByUUIDRequest) rq.getRequestMessage();
        final AtomicReference<byte[]> newData = new AtomicReference<byte[]>();
        
        component.lookup(database, new AbstractRPCRequestCallback(callback) {
            
            @SuppressWarnings("unchecked")
            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                    throws ErrorResponseException {
                
                if (result == null) {
                    
                    return success((Message) null);
                } else {
                    
                    try {
                        
                        ReusableBuffer buf = ReusableBuffer.wrap((byte[]) result);
                        ServiceRecord dbData = new ServiceRecord(buf);
                        
                        dbData.setLast_updated_s(0);
                        dbData.setVersion(dbData.getVersion()+1);
    
                        newData.set(new byte[dbData.getSize()]);
                        dbData.serialize(ReusableBuffer.wrap(newData.get()));
                        
                        component.singleInsert(DIRRequestDispatcher.INDEX_ID_SERVREG, request.getName().getBytes(), 
                                newData.get(), (OLPStageRequest<DIRRequest>) stageRequest, callback);
                        
                        return false;
                    } catch(IOException e) {
                        
                        throw new ErrorResponseException(e);
                    }
                }
            }
        }, DIRRequestDispatcher.INDEX_ID_SERVREG, request.getName().getBytes(), rq);
    }
}
