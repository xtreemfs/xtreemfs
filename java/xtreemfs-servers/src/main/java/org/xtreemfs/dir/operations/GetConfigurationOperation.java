/*
 * Copyright (c) 2011 by Paul Seiferth,
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
import org.xtreemfs.dir.data.ConfigurationRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Configuration;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.configurationGetRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;

import com.google.protobuf.Message;

public class GetConfigurationOperation extends DIROperation {
    
    private final Database database;
    
    public GetConfigurationOperation(DIRRequestDispatcher master) throws BabuDBException {
        super(master);
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_CONFIGURATION_GET;
    }
    
    @Override
    protected Message getRequestMessagePrototype() {
        
        return configurationGetRequest.getDefaultInstance();
    }
    
    @Override
    public boolean isAuthRequired() {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    void requestFinished(Object result, DIRRequest rq) {
        rq.sendSuccess((Configuration) result);
        
    }
    
    @Override
    public void startRequest(DIRRequest rq) {
        configurationGetRequest request = (configurationGetRequest) rq.getRequestMessage();
        
        database.lookup(DIRRequestDispatcher.INDEX_ID_CONFIGURATIONS, request.getUuid().getBytes(), rq)
                .registerListener(new DBRequestListener<byte[], Configuration>(true) {
                    
                    @Override
                    Configuration execute(byte[] result, DIRRequest rq) throws Exception {
                        
                        if (result == null) {
                            return Configuration.getDefaultInstance();
                        } else {
                            return new ConfigurationRecord(ReusableBuffer.wrap(result)).getConfiguration();
                            
                        }
                    }
                });
    }
    
}
