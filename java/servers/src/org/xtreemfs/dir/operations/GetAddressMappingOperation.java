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
import org.xtreemfs.common.stage.BabuDBComponent;
import org.xtreemfs.common.stage.BabuDBPostprocessing;
import org.xtreemfs.common.stage.BabuDBComponent.BabuDBRequest;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.AddressMappingRecords;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMappingSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.addressMappingGetRequest;

import com.google.protobuf.Message;

/**
 *
 * @author bjko
 */
public class GetAddressMappingOperation extends DIROperation {

    private final BabuDBComponent component;
    private final Database database;
    
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
    public void startRequest(DIRRequest rq, RPCRequestCallback callback) throws Exception {
        addressMappingGetRequest request = (addressMappingGetRequest) rq.getRequestMessage();

        if (request.getUuid().length() > 0) {
            
            //single mapping was requested
            component.lookup(database, callback, DIRRequestDispatcher.INDEX_ID_ADDRMAPS, request.getUuid().getBytes(), 
                    rq.getMetadata(), new BabuDBPostprocessing<byte[]>() {
                        
                        @Override
                        public Message execute(byte[] result, BabuDBRequest request) throws Exception {
                            if (result == null) {
                                return AddressMappingSet.getDefaultInstance();
                            } else {
                                return new AddressMappingRecords(ReusableBuffer.wrap(result)).getAddressMappingSet();
                            }
                        }
                    });
        } else {
            
            //full list requested
            component.prefixLookup(database, callback, DIRRequestDispatcher.INDEX_ID_ADDRMAPS, new byte[0], 
                    rq.getMetadata(), new BabuDBPostprocessing<ResultSet<byte[], byte[]>>() {
                        
                        @Override
                        public Message execute(ResultSet<byte[], byte[]> result, BabuDBRequest request) 
                                throws Exception {
                            
                            AddressMappingRecords list = new AddressMappingRecords();
                            while (result.hasNext()) {
                                Entry<byte[],byte[]> e = result.next();
                                AddressMappingRecords recs = new AddressMappingRecords(ReusableBuffer.wrap(e.getValue()));
                                list.add(recs);
                            }
                            return list.getAddressMappingSet();
                        }
                    });
        }
    }
}
