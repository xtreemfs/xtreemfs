/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.io.IOException;

import org.xtreemfs.babudb.api.database.Database;
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
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceGetByUUIDRequest;

/**
 * 
 * @author bjko
 */
public class GetServiceByUuidOperation extends DIROperation {
    
    
    private final BabuDBComponent<DIRRequest> component;
    private final Database                    database;
        
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
    public void startRequest(DIRRequest rq, RPCRequestCallback callback) {

        serviceGetByUUIDRequest request = (serviceGetByUUIDRequest) rq.getRequestMessage();

        component.lookup(database, new AbstractRPCRequestCallback(callback) {
            
            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S request)
                    throws ErrorResponseException {

                ServiceSet.Builder services = ServiceSet.newBuilder();
                if (result != null) {
                    try {
                        ServiceRecord dbData = new ServiceRecord(ReusableBuffer.wrap((byte[]) result));
                        services.addServices(dbData.getService());
                    } catch (IOException ex) {
                        throw new ErrorResponseException(ex);
                    }
                }
                return success(services.build());
            }
        }, DIRRequestDispatcher.INDEX_ID_SERVREG, request.getName().getBytes(), rq);
    }
}
