/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.util.ConcurrentModificationException;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceRegisterRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceRegisterResponse;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class RegisterServiceOperation extends DIROperation {
    
    private final Database database;
    
    public RegisterServiceOperation(DIRRequestDispatcher master) {
        super(master);
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_SERVICE_REGISTER;
    }
    
    @Override
    public void startRequest(DIRRequest rq) {
        final serviceRegisterRequest request = (serviceRegisterRequest) rq.getRequestMessage();
        
        final Service.Builder reg = request.getService().toBuilder();
        
        database.lookup(DIRRequestDispatcher.INDEX_ID_SERVREG, reg.getUuid().getBytes(), rq)
                .registerListener(new DBRequestListener<byte[], Long>(false) {
                    
                    @Override
                    Long execute(byte[] result, DIRRequest rq) throws Exception {
                        long currentVersion = 0;
                        if (result != null) {
                            ReusableBuffer buf = ReusableBuffer.wrap(result);
                            ServiceRecord dbData = new ServiceRecord(buf);
                            currentVersion = dbData.getVersion();
                        }
                        
                        if (reg.getVersion() != currentVersion) {
                            throw new ConcurrentModificationException("The requested version number ("
                                + reg.getVersion() + ") did not match the " + "expected version ("
                                + currentVersion + ")!");
                        }
                        
                        final long version = ++currentVersion;
                        
                        reg.setVersion(currentVersion);
                        reg.setLastUpdatedS(System.currentTimeMillis() / 1000l);
                        
                        ServiceRecord newRec = new ServiceRecord(reg.build());
                        byte[] newData = new byte[newRec.getSize()];
                        newRec.serialize(ReusableBuffer.wrap(newData));
                        
                        database.singleInsert(DIRRequestDispatcher.INDEX_ID_SERVREG,
                            newRec.getUuid().getBytes(), newData, rq).registerListener(
                            new DBRequestListener<Object, Long>(true) {
                                
                                @Override
                                Long execute(Object result, DIRRequest rq) throws Exception {
                                    
                                    return version;
                                }
                            });
                        return null;
                    }
                });
    }
    
    @Override
    public boolean isAuthRequired() {
        return false;
    }
    
    @Override
    protected Message getRequestMessagePrototype() {
        return serviceRegisterRequest.getDefaultInstance();
    }
    
    @Override
    void requestFinished(Object result, DIRRequest rq) {
        serviceRegisterResponse resp = serviceRegisterResponse.newBuilder().setNewVersion((Long) result)
                .build();
        rq.sendSuccess(resp);
    }
    
}
