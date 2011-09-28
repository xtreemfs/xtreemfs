/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.common.stage.BabuDBComponent;
import org.xtreemfs.common.stage.BabuDBPostprocessing;
import org.xtreemfs.common.stage.BabuDBComponent.BabuDBRequest;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceGetByUUIDRequest;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class GetServiceByUuidOperation extends DIROperation {
    
    
    private final BabuDBComponent component;
    private final Database database;
        
    public GetServiceByUuidOperation(DIRRequestDispatcher master) {
        super(master);
        component = master.getBabuDBComponent();
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_SERVICE_GET_BY_UUID;
    }
    
    @Override
    public void startRequest(DIRRequest rq, RPCRequestCallback callback) throws Exception {

        serviceGetByUUIDRequest request = (serviceGetByUUIDRequest) rq.getRequestMessage();

        component.lookup(database, callback, DIRRequestDispatcher.INDEX_ID_SERVREG, request.getName().getBytes(), 
                rq.getMetadata(), new BabuDBPostprocessing<byte[]>() {
            
            @Override
            public Message execute(byte[] result, BabuDBRequest request) throws Exception {
                
                ServiceSet.Builder services = ServiceSet.newBuilder();
                if (result != null) {
                    ServiceRecord dbData = new ServiceRecord(ReusableBuffer.wrap(result));
                    services.addServices(dbData.getService());
                }
                return services.build();
            }
        });
    }
}
