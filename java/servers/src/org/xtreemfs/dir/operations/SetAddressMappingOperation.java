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
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.AddressMappingRecord;
import org.xtreemfs.dir.data.AddressMappingRecords;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMapping;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMappingSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.addressMappingSetResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;

import com.google.protobuf.Message;

/**
 * 
 * @author bjko
 */
public class SetAddressMappingOperation extends DIROperation {
    
    private final Database database;
    
    public SetAddressMappingOperation(DIRRequestDispatcher master) throws BabuDBException {
        super(master);
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_ADDRESS_MAPPINGS_SET;
    }
    
    @Override
    public void startRequest(DIRRequest rq) {
        
        final AddressMappingSet mappings = (AddressMappingSet) rq.getRequestMessage();
        String uuid = null;
        if (mappings.getMappingsCount() == 0) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "non-empty address mapping set not allowed");
            return;
        }
        for (AddressMapping am : mappings.getMappingsList()) {
            if (uuid == null)
                uuid = am.getUuid();
            if (!am.getUuid().equals(uuid)) {
                rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "all mappings must have the same UUID");
                return;
            }
        }
        
        assert (uuid != null);
        assert (database != null);
        
        final String UUID = uuid;
        
        database.lookup(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, uuid.getBytes(), rq).registerListener(
                new DBRequestListener<byte[], Long>(false) {
                    
                    @Override
                    Long execute(byte[] result, DIRRequest rq) throws Exception {
                        long currentVersion = 0;
                        if (result != null) {
                            ReusableBuffer buf = ReusableBuffer.wrap(result);
                            AddressMappingRecords dbData = new AddressMappingRecords(buf);
                            if (dbData.size() > 0) {
                                currentVersion = dbData.getRecord(0).getVersion();
                            }
                        }
                        
                        if (mappings.getMappings(0).getVersion() != currentVersion)
                            throw new ConcurrentModificationException();
                        
                        final long version = ++currentVersion;
                        
                        AddressMappingSet.Builder newSet = mappings.toBuilder();
                        for (int i = 0; i < mappings.getMappingsCount(); i++) {
                            newSet.setMappings(i, mappings.getMappings(i).toBuilder().setVersion(currentVersion));
                        }
                        
                        AddressMappingRecords newData = new AddressMappingRecords(newSet.build());
                        final int size = newData.getSize();
                        byte[] newBytes = new byte[size];
                        ReusableBuffer buf = ReusableBuffer.wrap(newBytes);
                        newData.serialize(buf);
                        database.singleInsert(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, UUID.getBytes(), newBytes, rq)
                                .registerListener(new DBRequestListener<Object, Long>(true) {
                                    
                                    @Override
                                    Long execute(Object result, DIRRequest rq) throws Exception {
                                        return version;
                                    }
                                });
                        
                        // notify all listeners about the insert
                        for (AddressMappingRecord amr : newData.getRecords()) {
                            master.notifyAddressMappingAdded(amr.getUuid(), amr.getUri());
                        }
                        
                        return null;
                    }
                });
    }
    
    @Override
    public boolean isAuthRequired() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    protected Message getRequestMessagePrototype() {
        return AddressMappingSet.getDefaultInstance();
    }
    
    @Override
    void requestFinished(Object result, DIRRequest rq) {
        addressMappingSetResponse resp = addressMappingSetResponse.newBuilder().setNewVersion((Long) result).build();
        rq.sendSuccess(resp);
    }
}
