/*
 * Copyright (c) 2011 by Paul Seiferth,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.xtreemfs.common.stage.BabuDBComponent;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.BabuDBComponent.BabuDBDatabaseRequest;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.ConfigurationRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Configuration;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.configurationSetResponse;

import com.google.protobuf.Message;

public class SetConfigurationOperation extends DIROperation {

    private final BabuDBComponent database;

    public SetConfigurationOperation(DIRRequestDispatcher master) {
        super(master);
        database = master.getDatabase();

    }

    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_CONFIGURATION_SET;
    }

    @Override
    public void startRequest(DIRRequest rq, RPCRequestCallback callback) throws Exception {

        final Configuration conf = (Configuration) rq.getRequestMessage();

        String uuid = null;

        if (conf.getParameterCount() == 0) {
            throw new IllegalArgumentException("empty configuration set not allowed");
        }

        uuid = conf.getUuid();

        assert (uuid != null);
        assert (database != null);
        
        final String UUID = uuid;
        final AtomicLong version = new AtomicLong();
        final AtomicReference<byte[]> newBytes = new AtomicReference<byte[]>();

        database.lookup(callback, DIRRequestDispatcher.INDEX_ID_CONFIGURATIONS, uuid.getBytes(), rq.getMetadata(), 
                database.new BabuDBPostprocessing<byte[]>() {
            
            @Override
            public Message execute(byte[] result, BabuDBDatabaseRequest request) throws Exception {
                long currentVersion = 0;
                if (result != null) {
                    ReusableBuffer buf = ReusableBuffer.wrap(result);
                    ConfigurationRecord dbData = new ConfigurationRecord(buf);
                    currentVersion = dbData.getVersion();

                }
                
                if (conf.getVersion() != currentVersion) {
                    throw new ConcurrentModificationException();
                }
                
                version.set(++currentVersion);
                
                ConfigurationRecord newRec = new ConfigurationRecord(conf);
                newRec.setVersion(currentVersion);    
               
                final int size = newRec.getSize();
                newBytes.set(new byte[size]);
                ReusableBuffer buf = ReusableBuffer.wrap(newBytes.get());
                newRec.serialize(buf);
                
                return null;
            }
            
            @Override
            public void requeue(BabuDBDatabaseRequest rq) {
                database.singleInsert(DIRRequestDispatcher.INDEX_ID_CONFIGURATIONS, UUID.getBytes(), newBytes.get(), rq,
                        database.new BabuDBPostprocessing<Object>() {
                    
                    @Override
                    public Message execute(Object result, BabuDBDatabaseRequest request) throws Exception {
                        return configurationSetResponse.newBuilder().setNewVersion(version.get()).build();
                    }
                });
            }
        });
    }
}
