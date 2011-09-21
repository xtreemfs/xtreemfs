/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.util.concurrent.atomic.AtomicReference;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.common.stage.BabuDBComponent;
import org.xtreemfs.common.stage.BabuDBPostprocessing;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.BabuDBComponent.BabuDBRequest;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceGetByUUIDRequest;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class ServiceOfflineOperation extends DIROperation {

    private final BabuDBComponent component;
    private final Database database;
    
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
    public void startRequest(DIRRequest rq, RPCRequestCallback callback) throws Exception {

        final serviceGetByUUIDRequest request = (serviceGetByUUIDRequest) rq.getRequestMessage();
        final AtomicReference<byte[]> newData = new AtomicReference<byte[]>();
        
        component.lookup(database, callback, DIRRequestDispatcher.INDEX_ID_SERVREG, request.getName().getBytes(),
                rq.getMetadata(), new BabuDBPostprocessing<byte[]>() {
                    
            @Override
            public Message execute(byte[] result, BabuDBRequest rq) throws Exception {
                
                if (result != null) {
                    
                    ReusableBuffer buf = ReusableBuffer.wrap(result);
                    ServiceRecord dbData = new ServiceRecord(buf);
                    
                    dbData.setLast_updated_s(0);
                    dbData.setVersion(dbData.getVersion()+1);

                    newData.set(new byte[dbData.getSize()]);
                    dbData.serialize(ReusableBuffer.wrap(newData.get()));
                    
                    return null;
                } else {
                    return emptyResponse.getDefaultInstance();
                }
            }
            
            @Override
            public void requeue(BabuDBRequest rq) {
                
                component.singleInsert(DIRRequestDispatcher.INDEX_ID_SERVREG, request.getName().getBytes(), 
                        newData.get(), rq, new BabuDBPostprocessing<Object>() {
                    
                    @Override
                    public Message execute(Object result, BabuDBRequest request) throws Exception {
                        return emptyResponse.getDefaultInstance();
                    }
                });
            }
        });
    }
}
