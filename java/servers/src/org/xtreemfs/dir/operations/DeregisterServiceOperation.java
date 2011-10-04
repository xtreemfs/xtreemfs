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
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceDeregisterRequest;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class DeregisterServiceOperation extends DIROperation {
    
    private final BabuDBComponent<DIRRequest> component;
    private final Database                    database;
    
    public DeregisterServiceOperation(DIRRequestDispatcher master) {
        super(master);
        
        component = master.getBabuDBComponent();
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        
        return DIRServiceConstants.PROC_ID_XTREEMFS_SERVICE_DEREGISTER;
    }
    
    @Override
    public void startRequest(DIRRequest rq, RPCRequestCallback callback) {
        
        final serviceDeregisterRequest req = (serviceDeregisterRequest) rq.getRequestMessage();
        
        DatabaseInsertGroup ig = database.createInsertGroup();
        ig.addDelete(DIRRequestDispatcher.INDEX_ID_SERVREG, req.getUuid().getBytes());
        component.insert(database, new AbstractRPCRequestCallback(callback) {
            
            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S request)
                    throws ErrorResponseException {
                
                master.notifyServiceDeregistred(req.getUuid());
                return success((Message) null);
            }
        }, ig, rq);
    }
}