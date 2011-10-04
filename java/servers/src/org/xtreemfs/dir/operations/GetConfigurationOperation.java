/*
 * Copyright (c) 2011 by Paul Seiferth,
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
import org.xtreemfs.dir.data.ConfigurationRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.configurationGetRequest;

import com.google.protobuf.Message;

public class GetConfigurationOperation extends DIROperation {
    
    private final BabuDBComponent<DIRRequest> component;
    private final Database                    database;
    
    public GetConfigurationOperation(DIRRequestDispatcher master) {
        super(master);
        
        component = master.getBabuDBComponent();
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        
        return DIRServiceConstants.PROC_ID_XTREEMFS_CONFIGURATION_GET;
    }
    
    @Override
    public void startRequest(DIRRequest rq, RPCRequestCallback callback) {
        
        final configurationGetRequest request = (configurationGetRequest) rq.getRequestArgs();
        
        
        
        component.lookup(database, new AbstractRPCRequestCallback(callback) {
            
            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S request)
                    throws ErrorResponseException {

                if (result == null) {
                    
                    return success((Message) null);
                } else {
                    try {
                        
                        return success(new ConfigurationRecord(ReusableBuffer.wrap(
                                                        (byte[]) result)).getConfiguration());
                    } catch (IOException ex) {
                        
                        throw new ErrorResponseException(ex);
                    }
                }
            }
        }, DIRRequestDispatcher.INDEX_ID_CONFIGURATIONS, request.getUuid().getBytes(), rq);
    }
}
