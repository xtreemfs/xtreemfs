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
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceGetByUUIDRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class GetServiceByUuidOperation extends DIROperation {
    
    private final Database database;
    
    public GetServiceByUuidOperation(DIRRequestDispatcher master) throws BabuDBException {
        super(master);
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_SERVICE_GET_BY_UUID;
    }
    
    @Override
    public void startRequest(DIRRequest rq) {
        serviceGetByUUIDRequest request = (serviceGetByUUIDRequest) rq.getRequestMessage();
        
        database.lookup(DIRRequestDispatcher.INDEX_ID_SERVREG, request.getName().getBytes(), rq).registerListener(
                new DBRequestListener<byte[], ServiceSet>(true) {
                    
                    @Override
                    ServiceSet execute(byte[] result, DIRRequest rq) throws Exception {
                        
                        ServiceSet.Builder services = ServiceSet.newBuilder();
                        if (result != null) {
                            ServiceRecord dbData = new ServiceRecord(ReusableBuffer.wrap(result));
                            services.addServices(dbData.getService());
                        }
                        return services.build();
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
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.xtreemfs.dir.operations.DIROperation#requestFinished(java.lang.Object
     * , org.xtreemfs.dir.DIRRequest)
     */
    @Override
    void requestFinished(Object result, DIRRequest rq) {
        rq.sendSuccess((ServiceSet) result);
    }
    
}
