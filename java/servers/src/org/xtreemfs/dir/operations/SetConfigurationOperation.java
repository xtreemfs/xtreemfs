/*
 * Copyright (c) 2011 by Paul Seiferth,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.io.IOException;
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
import org.xtreemfs.dir.data.ConfigurationRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Configuration;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.configurationSetResponse;

public class SetConfigurationOperation extends DIROperation {

    private final BabuDBComponent<DIRRequest> component;
    private final Database                    database;
    
    public SetConfigurationOperation(DIRRequestDispatcher master) {
        super(master);
        
        component = master.getBabuDBComponent();
        database = master.getDirDatabase();
    }

    @Override
    public int getProcedureId() {
        return DIRServiceConstants.PROC_ID_XTREEMFS_CONFIGURATION_SET;
    }

    @Override
    public void startRequest(DIRRequest rq, final RPCRequestCallback callback) throws ErrorResponseException {

        final Configuration conf = (Configuration) rq.getRequestArgs();

        String uuid = null;

        if (conf.getParameterCount() == 0) {
            throw new ErrorResponseException(ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, 
                "empty configuration set not allowed"));
        }

        uuid = conf.getUuid();

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

                long currentVersion = 0;
                if (result != null) {
                    try {
                        
                        ReusableBuffer buf = ReusableBuffer.wrap((byte[]) result);
                        ConfigurationRecord dbData = new ConfigurationRecord(buf);
                        currentVersion = dbData.getVersion();
                    } catch (IOException e) {
                        
                        throw new ErrorResponseException(e);
                    }
                }
                
                if (conf.getVersion() != currentVersion) {
                    throw new ErrorResponseException(ErrorUtils.getErrorResponse(ErrorType.ERRNO, 
                            POSIXErrno.POSIX_ERROR_EAGAIN, "Concurrent modification."));
                }
                
                version.set(++currentVersion);
                
                ConfigurationRecord newRec = new ConfigurationRecord(conf);
                newRec.setVersion(currentVersion);    
               
                final int size = newRec.getSize();
                newBytes.set(new byte[size]);
                ReusableBuffer buf = ReusableBuffer.wrap(newBytes.get());
                newRec.serialize(buf);
                
                component.singleInsert(DIRRequestDispatcher.INDEX_ID_CONFIGURATIONS, UUID.getBytes(), newBytes.get(), 
                        (OLPStageRequest<DIRRequest>) stageRequest, new AbstractRPCRequestCallback(callback) {
                            
                    @Override
                    public <T extends StageRequest<?>> boolean success(Object result, T stageRequest)
                            throws ErrorResponseException {

                        return success(configurationSetResponse.newBuilder().setNewVersion(version.get()).build());
                    }
                });
                
                return false;
            }
        }, DIRRequestDispatcher.INDEX_ID_CONFIGURATIONS, uuid.getBytes(), rq);
    }
}
