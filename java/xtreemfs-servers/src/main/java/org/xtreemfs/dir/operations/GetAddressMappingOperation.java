/*
 * Copyright (c) 2011 by Paul Seiferth,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.util.Map.Entry;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.database.ResultSet;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.AddressMappingRecords;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMappingSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.addressMappingGetRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;

import com.google.protobuf.Message;

/**
 *
 * @author bjko
 */
public class GetAddressMappingOperation extends DIROperation {

    private final Database database;

    public GetAddressMappingOperation(DIRRequestDispatcher master) throws BabuDBException {
        super(master);
        database = master.getDirDatabase();
    }

    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_ADDRESS_MAPPINGS_GET;
    }

    @Override
    public void startRequest(DIRRequest rq) {
        addressMappingGetRequest request =
            (addressMappingGetRequest)rq.getRequestMessage();

        if (request.getUuid().length() > 0) {
            //single mapping was requested
            database.lookup(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, 
                    request.getUuid().getBytes(),rq).registerListener(
                            new DBRequestListener<byte[],AddressMappingSet>(true) {
                        
                        @Override
                        AddressMappingSet execute(byte[] result, DIRRequest rq) 
                            throws Exception {
                            
                            if (result == null)
                                return AddressMappingSet.getDefaultInstance();
                            else
                                return new AddressMappingRecords(ReusableBuffer
                                        .wrap(result)).getAddressMappingSet();
                        }
                    });
        } else {
            //full list requested
            database.prefixLookup(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, 
                    new byte[0], rq).registerListener(
                            new DBRequestListener<ResultSet<byte[],byte[]>,AddressMappingSet>(true) {
                        
                        @Override
                        AddressMappingSet execute(ResultSet<byte[], byte[]> result, 
                                DIRRequest rq) throws Exception {
                            
                            AddressMappingRecords list = new AddressMappingRecords();
                            while (result.hasNext()) {
                                Entry<byte[],byte[]> e = result.next();
                                AddressMappingRecords recs = 
                                    new AddressMappingRecords(
                                            ReusableBuffer.wrap(e.getValue()));
                                list.add(recs);
                            }
                            return list.getAddressMappingSet();
                        }
                    });
        }
    }

    @Override
    public boolean isAuthRequired() {
        return false;
    }

    @Override
    protected Message getRequestMessagePrototype() {
        return addressMappingGetRequest.getDefaultInstance();
    }

    /*
     * (non-Javadoc)
     * @see org.xtreemfs.dir.operations.DIROperation#requestFinished(java.lang.Object, org.xtreemfs.dir.DIRRequest)
     */
    @Override
    void requestFinished(Object result, DIRRequest rq) {
        rq.sendSuccess((AddressMappingSet) result);
    }
}
