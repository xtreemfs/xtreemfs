/*
 * Copyright (c) 2011 by Paul Seiferth,
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
import org.xtreemfs.dir.data.ConfigurationRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Configuration;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.configurationSetResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;

import com.google.protobuf.Message;

public class SetConfigurationOperation extends DIROperation {
    
    private final Database database;
    
    public SetConfigurationOperation(DIRRequestDispatcher master) throws BabuDBException {
        super(master);
        database = master.getDirDatabase();
        
    }
    
    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_CONFIGURATION_SET;
    }
    
    @Override
    protected Message getRequestMessagePrototype() {
        
        return Configuration.getDefaultInstance();
    }
    
    @Override
    public boolean isAuthRequired() {
        throw new UnsupportedOperationException("Not supported yet.");
        
    }
    
    @Override
    void requestFinished(Object result, DIRRequest rq) {
        
        configurationSetResponse response = configurationSetResponse.newBuilder().setNewVersion((Long) result).build();
        rq.sendSuccess(response);
    }
    
    @Override
    public void startRequest(DIRRequest rq) {
        
        final Configuration conf = (Configuration) rq.getRequestMessage();
        
        String uuid = null;
        
        if (conf.getParameterCount() == 0) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "empty configuration set not allowed");
            return;
        }
        
        uuid = conf.getUuid();
        
        assert (uuid != null);
        assert (database != null);
        
        final String UUID = uuid;
        
        database.lookup(DIRRequestDispatcher.INDEX_ID_CONFIGURATIONS, uuid.getBytes(), rq).registerListener(
                new DBRequestListener<byte[], Long>(false) {
                    @Override
                    Long execute(byte[] result, DIRRequest rq) throws Exception {
                        
                        long currentVersion = 0;
                        if (result != null) {
                            ReusableBuffer buf = ReusableBuffer.wrap(result);
                            ConfigurationRecord dbData = new ConfigurationRecord(buf);
                            currentVersion = dbData.getVersion();
                            
                        }
                        
                        if (conf.getVersion() != currentVersion) {
                            throw new ConcurrentModificationException();
                        }
                        
                        final long version = ++currentVersion;
                        
                        ConfigurationRecord newRec = new ConfigurationRecord(conf);
                        newRec.setVersion(version);
                        
                        final int size = newRec.getSize();
                        byte[] newBytes = new byte[size];
                        ReusableBuffer buf = ReusableBuffer.wrap(newBytes);
                        newRec.serialize(buf);
                        
                        database.singleInsert(DIRRequestDispatcher.INDEX_ID_CONFIGURATIONS, UUID.getBytes(), newBytes,
                                rq).registerListener(new DBRequestListener<Object, Long>(true) {
                            
                            @Override
                            Long execute(Object result, DIRRequest rq) throws Exception {
                                return version;
                            }
                        });
                        
                        return null;
                        
                    }
                    
                });
        
        //
        // database.lookup(DIRRequestDispatcher.INDEX_ID_CONFIGURATIONS,
        // uuid.getBytes(), rq).registerListener(
        // new DBRequestListener<byte[], Configuration>(true) {
        //
        // @Override
        // Configuration execute(byte[] result, DIRRequest rq) throws Exception
        // {
        //
        // if (result == null) {
        // return Configuration.getDefaultInstance();
        // } else
        // return new
        // ConfigurationRecord(ReusableBuffer.wrap(result)).getConfiguration();
        // }
        // });
        
    }
    
}
