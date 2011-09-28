/*
 * Copyright (c) 2011 by Paul Seiferth,
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
import org.xtreemfs.dir.data.ConfigurationRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Configuration;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.configurationGetRequest;

import com.google.protobuf.Message;

public class GetConfigurationOperation extends DIROperation {
    
    private final BabuDBComponent component;
    private final Database database;
    
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
    public void startRequest(DIRRequest rq, RPCRequestCallback callback) throws Exception {
        
        configurationGetRequest request = (configurationGetRequest) rq.getRequestMessage();
        
        component.lookup(database, callback, DIRRequestDispatcher.INDEX_ID_CONFIGURATIONS, request.getUuid().getBytes(), 
            rq.getMetadata(), new BabuDBPostprocessing<byte[]>() {
                    
            @Override
            public Message execute(byte[] result, BabuDBRequest request) throws Exception {
                if (result == null) {
                    return Configuration.getDefaultInstance();
                } else {
                    return new ConfigurationRecord(ReusableBuffer.wrap(result)).getConfiguration();
                }
            }
        });
    }
}
