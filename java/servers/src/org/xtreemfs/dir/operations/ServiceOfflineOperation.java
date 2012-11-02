/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceGetByUUIDRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class ServiceOfflineOperation extends DIROperation {
    
    private final Database database;
    
    public ServiceOfflineOperation(DIRRequestDispatcher master) throws BabuDBException {
        super(master);
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_SERVICE_OFFLINE;
    }
    
    @Override
    public void startRequest(DIRRequest rq) {
        final serviceGetByUUIDRequest request = (serviceGetByUUIDRequest) rq.getRequestMessage();
        
        database.lookup(DIRRequestDispatcher.INDEX_ID_SERVREG, request.getName().getBytes(), rq).registerListener(
                new DBRequestListener<byte[], Object>(false) {
                    
                    @Override
                    Object execute(byte[] result, DIRRequest rq) throws Exception {
                        if (result != null) {
                            ReusableBuffer buf = ReusableBuffer.wrap(result);
                            ServiceRecord dbData = new ServiceRecord(buf);
                            
                            dbData.setLast_updated_s(0);
                            dbData.setVersion(dbData.getVersion() + 1);
                            
                            byte[] newData = new byte[dbData.getSize()];
                            dbData.serialize(ReusableBuffer.wrap(newData));
                            database.singleInsert(DIRRequestDispatcher.INDEX_ID_SERVREG, request.getName().getBytes(),
                                    newData, rq).registerListener(new DBRequestListener<Object, Object>(true) {
                                
                                @Override
                                Object execute(Object result, DIRRequest rq) throws Exception {
                                    return null;
                                }
                            });
                        } else
                            requestFinished(null, rq);
                        
                        return null;
                    }
                });
    }
    
    @Override
    public boolean isAuthRequired() {
        return false;
    }
    
    @Override
    protected Message getRequestMessagePrototype() {
        return serviceGetByUUIDRequest.getDefaultInstance();
    }
    
    @Override
    void requestFinished(Object result, DIRRequest rq) {
        rq.sendSuccess(emptyResponse.getDefaultInstance());
    }
}
