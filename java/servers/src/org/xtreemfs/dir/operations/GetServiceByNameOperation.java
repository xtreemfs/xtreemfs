/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.io.IOException;
import java.util.Map.Entry;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.database.ResultSet;
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
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceGetByNameRequest;

/**
 * 
 * @author bjko
 */
public class GetServiceByNameOperation extends DIROperation {
    
    private final BabuDBComponent<DIRRequest> component;
    private final Database                    database;
    
    public GetServiceByNameOperation(DIRRequestDispatcher master) {
        super(master);
        
        component = master.getBabuDBComponent();
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        
        return DIRServiceConstants.PROC_ID_XTREEMFS_SERVICE_GET_BY_NAME;
    }
    
    @Override
    public void startRequest(DIRRequest rq, RPCRequestCallback callback) {
        
        final serviceGetByNameRequest req = (serviceGetByNameRequest) rq.getRequestMessage();
        
        component.prefixLookup(database, new AbstractRPCRequestCallback(callback) {
            
            @SuppressWarnings("unchecked")
            @Override
            public <S extends StageRequest<?>> boolean success(Object r, S request)
                    throws ErrorResponseException {

                final ResultSet<byte[], byte[]> result = (ResultSet<byte[], byte[]>) r;
                
                final ServiceSet.Builder services = ServiceSet.newBuilder();
                final long now = System.currentTimeMillis() / 1000l;
                
                try {
                    
                    while (result.hasNext()) {
                        Entry<byte[], byte[]> e = result.next();
                        ServiceRecord servEntry = new ServiceRecord(ReusableBuffer.wrap(e.getValue()));
                        if (servEntry.getName().equals(req.getName()))
                            services.addServices(servEntry.getService());
                        
                        long secondsSinceLastUpdate = now - servEntry.getLast_updated_s();
                        servEntry.getData().put("seconds_since_last_update",
                            Long.toString(secondsSinceLastUpdate));
                    }
                    return success(services.build());
                } catch (IOException ex) {
                    
                    throw new ErrorResponseException(ex);
                }
            }
        }, DIRRequestDispatcher.INDEX_ID_SERVREG, new byte[0], rq);
    }
}
