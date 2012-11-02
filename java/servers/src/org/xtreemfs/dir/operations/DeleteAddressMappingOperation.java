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
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.addressMappingGetRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class DeleteAddressMappingOperation extends DIROperation {
    
    private final Database database;
    
    public DeleteAddressMappingOperation(DIRRequestDispatcher master) throws BabuDBException {
        super(master);
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_ADDRESS_MAPPINGS_REMOVE;
    }
    
    @Override
    public void startRequest(DIRRequest rq) {
        addressMappingGetRequest request = (addressMappingGetRequest) rq.getRequestMessage();
        
        DatabaseInsertGroup ig = database.createInsertGroup();
        ig.addDelete(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, request.getUuid().getBytes());
        database.insert(ig, rq).registerListener(new DBRequestListener<Object, Object>(true) {
            
            @Override
            Object execute(Object result, DIRRequest rq) throws Exception {
                return result;
            }
        });
    }
    
    @Override
    public boolean isAuthRequired() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    protected Message getRequestMessagePrototype() {
        return addressMappingGetRequest.getDefaultInstance();
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
        rq.sendSuccess(emptyResponse.getDefaultInstance());
    }
}
