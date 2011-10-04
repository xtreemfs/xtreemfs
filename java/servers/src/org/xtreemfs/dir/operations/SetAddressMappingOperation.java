/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.common.olp.OLPStageRequest;
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.BabuDBComponent;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.AddressMappingRecord;
import org.xtreemfs.dir.data.AddressMappingRecords;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMappingSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.addressMappingSetResponse;

/**
 * 
 * @author bjko
 */
public class SetAddressMappingOperation extends DIROperation {

    private final BabuDBComponent<DIRRequest> component;
    private final Database                    database;
        
    public SetAddressMappingOperation(DIRRequestDispatcher master) {
        super(master);
        
        component = master.getBabuDBComponent();
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        
        return DIRServiceConstants.PROC_ID_XTREEMFS_ADDRESS_MAPPINGS_SET;
    }
    
    @Override
    public void startRequest(DIRRequest rq, final RPCRequestCallback callback) throws ErrorResponseException {

        final AddressMappingSet mappings = (AddressMappingSet) rq.getRequestMessage();
        String uuid = null;
        if (mappings.getMappingsCount() == 0) {
            throw new ErrorResponseException(ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, 
                    "non-empty address mapping set not allowed"));
        }
        for (AddressMapping am : mappings.getMappingsList()) {
            if (uuid == null)
                uuid = am.getUuid();
            if (!am.getUuid().equals(uuid)) {
                throw new ErrorResponseException(ErrorUtils.getErrorResponse(ErrorType.ERRNO, 
                        POSIXErrno.POSIX_ERROR_EINVAL, "all mappings must have the same UUID"));
            }
        }
        
        assert (uuid != null);
        assert (component != null);
        
        final String UUID = uuid;
        final AtomicLong version = new AtomicLong();
        final AtomicReference<byte[]> newBytes = new AtomicReference<byte[]>();
        
        component.lookup(database, new AbstractRPCRequestCallback(callback) {
            
            @SuppressWarnings("unchecked")
            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                    throws ErrorResponseException {
                
                try {
                    long currentVersion = 0;
                    if (result != null) {
                        ReusableBuffer buf = ReusableBuffer.wrap((byte[]) result);
                        AddressMappingRecords dbData = new AddressMappingRecords(buf);
                        if (dbData.size() > 0) {
                            currentVersion = dbData.getRecord(0).getVersion();
                        }
                    }
    
                    if (mappings.getMappings(0).getVersion() != currentVersion)
                        throw new ConcurrentModificationException();
                    
                    version.set(++currentVersion);
    
                    AddressMappingSet.Builder newSet = mappings.toBuilder();
                    for (int i = 0; i < mappings.getMappingsCount(); i++) {
                        newSet.setMappings(i, mappings.getMappings(i).toBuilder().setVersion(currentVersion));
                    }
                    
                    AddressMappingRecords newData = new AddressMappingRecords(newSet.build());
                    final int size = newData.getSize();
                    newBytes.set(new byte[size]);
                    ReusableBuffer buf = ReusableBuffer.wrap(newBytes.get());
                    newData.serialize(buf);
                    
                    // notify all listeners about the insert
                    for (AddressMappingRecord amr : newData.getRecords()) {
                        master.notifyAddressMappingAdded(amr.getUuid(), amr.getUri());
                    }
                    
                    component.singleInsert(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, UUID.getBytes(), newBytes.get(), 
                            (OLPStageRequest<DIRRequest>) stageRequest, new AbstractRPCRequestCallback(callback) {
                                
                                @Override
                                public <T extends StageRequest<?>> boolean success(Object result, T stageRequest)
                                        throws ErrorResponseException {
    
                                    return success(addressMappingSetResponse.newBuilder().setNewVersion(
                                            version.get()).build());
                                }
                            });
                    return false;
                } catch (IOException e) {
                    
                    throw new ErrorResponseException(e);
                }
            }
        }, DIRRequestDispatcher.INDEX_ID_ADDRMAPS, uuid.getBytes(), rq);
    }
}
