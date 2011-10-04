/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.database.DatabaseInsertGroup;
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.BabuDBComponent;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.addressMappingGetRequest;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class DeleteAddressMappingOperation extends DIROperation {
    
    private final BabuDBComponent<DIRRequest> component;
    private final Database                    database;
    
    
    public DeleteAddressMappingOperation(DIRRequestDispatcher master) {
        
        super(master);
        component = master.getBabuDBComponent();
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        
        return DIRServiceConstants.PROC_ID_XTREEMFS_ADDRESS_MAPPINGS_REMOVE;
    }
    
    @Override
    public void startRequest(DIRRequest rq, RPCRequestCallback callback) {
        
        addressMappingGetRequest request = (addressMappingGetRequest) rq.getRequestArgs();
        
        DatabaseInsertGroup ig = database.createInsertGroup();
        ig.addDelete(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, request.getUuid().getBytes());
        
        component.insert(database, new AbstractRPCRequestCallback(callback) {
            
            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                    throws ErrorResponseException {
                
                return success((Message) null);
            }
        }, ig, rq);
    }
}
