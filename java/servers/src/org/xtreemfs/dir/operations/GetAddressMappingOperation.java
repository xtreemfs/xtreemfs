/*
 * Copyright (c) 2011 by Paul Seiferth,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.io.IOException;
import java.util.Map.Entry;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.database.ResultSet;
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.BabuDBComponent;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.AddressMappingRecords;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.addressMappingGetRequest;

import com.google.protobuf.Message;

/**
 *
 * @author bjko
 */
public class GetAddressMappingOperation extends DIROperation {

    private final BabuDBComponent<DIRRequest> component;
    private final Database                    database;
    
    public GetAddressMappingOperation(DIRRequestDispatcher master) {
        super(master);
        
        component = master.getBabuDBComponent();
        database = master.getDirDatabase();
    }

    @Override
    public int getProcedureId() {
        
        return DIRServiceConstants.PROC_ID_XTREEMFS_ADDRESS_MAPPINGS_GET;
    }

    @Override
    public void startRequest(DIRRequest rq, RPCRequestCallback callback) {
        
        final addressMappingGetRequest req = (addressMappingGetRequest) rq.getRequestArgs();

        if (req.getUuid().length() > 0) {
            
            //single mapping was requested
            component.lookup(database, new AbstractRPCRequestCallback(callback) {
                
                @Override
                public <S extends StageRequest<?>> boolean success(Object result, S request)
                        throws ErrorResponseException {

                    if (result == null) {
                        return success((Message) null);
                    } else {
                        try {
                            
                            return success(new AddressMappingRecords(ReusableBuffer.wrap((byte[]) result))
                                    .getAddressMappingSet());
                        } catch (IOException ex) {
                            
                            throw new ErrorResponseException(ex);
                        }
                    }
                }
            }, DIRRequestDispatcher.INDEX_ID_ADDRMAPS, req.getUuid().getBytes(), rq);
        } else {
            
            //full list requested
            component.prefixLookup(database, new AbstractRPCRequestCallback(callback) {
                
                @SuppressWarnings("unchecked")
                @Override
                public <S extends StageRequest<?>> boolean success(Object r, S request)
                        throws ErrorResponseException {

                    try {
                        ResultSet<byte[], byte[]> result = (ResultSet<byte[], byte[]>) r;
                        AddressMappingRecords list = new AddressMappingRecords();
                        while (result.hasNext()) {
                            Entry<byte[],byte[]> e = result.next();
                            AddressMappingRecords recs = new AddressMappingRecords(ReusableBuffer.wrap(e.getValue()));
                            list.add(recs);
                        }
                        return success(list.getAddressMappingSet());
                        
                    } catch (IOException ex) {
                        
                        throw new ErrorResponseException(ex);
                    }
                }
            }, DIRRequestDispatcher.INDEX_ID_ADDRMAPS, new byte[0], rq);
        }
    }
}