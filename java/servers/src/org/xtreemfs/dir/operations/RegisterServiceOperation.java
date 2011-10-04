/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.operations;

import java.io.IOException;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.olp.OLPStageRequest;
import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.BabuDBComponent;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.stage.StageRequest;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceRegisterResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceRegisterRequest;

/**
 * 
 * @author bjko
 */
public class RegisterServiceOperation extends DIROperation {
    
    private final BabuDBComponent<DIRRequest> component;
    private final Database                    database;
    
    public RegisterServiceOperation(DIRRequestDispatcher master) {
        super(master);
        
        component = master.getBabuDBComponent();
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        
        return DIRServiceConstants.PROC_ID_XTREEMFS_SERVICE_REGISTER;
    }
    
    @Override
    public void startRequest(DIRRequest rq, RPCRequestCallback callback) {
        
        final serviceRegisterRequest request = (serviceRegisterRequest) rq.getRequestMessage();
        
        final Service.Builder reg = request.getService().toBuilder();
                
        component.lookup(database, new AbstractRPCRequestCallback(callback) {

            @SuppressWarnings("unchecked")
            @Override
            public <S extends StageRequest<?>> boolean success(Object result, S stageRequest)
                    throws ErrorResponseException {

                long currentVersion = 0;
                if (result != null) {
                    
                    try {
                        
                        ReusableBuffer buf = ReusableBuffer.wrap((byte[]) result);
                        ServiceRecord dbData = new ServiceRecord(buf);
                        currentVersion = dbData.getVersion();
                    } catch (IOException e) {
                        
                        throw new ErrorResponseException(e);
                    }
                } else {
                    
                    // The registered service wasn't registered before. 
                    // Collect data from the request and inform all listeners about this registration
                    String uuid, name, type, pageUrl, geoCoordinates;
                    long totalRam, usedRam, lastUpdated;
                    int status, load, protoVersion;
                    
                    ServiceRecord sRec = new ServiceRecord(request.getService().toBuilder().build());
                    
                    uuid = sRec.getUuid();
                    name = sRec.getName();
                    type = sRec.getType().toString();
                    
                    
                    pageUrl = sRec.getData().get("status_page_url") == null ? "": sRec.getData().get("status_page_url");
                    geoCoordinates = sRec.getData().get("vivaldi_coordinates") == null ? "" :
                        sRec.getData().get("vivaldi_coordinates");
                    try {
                        totalRam = Long.parseLong(sRec.getData().get("totalRAM"));
                    } catch (NumberFormatException nfe) {
                        totalRam = -1;
                    }
                    try {
                        usedRam = Long.parseLong(sRec.getData().get("usedRAM"));
                    } catch (NumberFormatException nfe) {
                        usedRam = -1;
                    }
                    lastUpdated = System.currentTimeMillis() / 1000l;
                    try {
                        status = Integer.parseInt(sRec.getData().get(HeartbeatThread.STATUS_ATTR));
                    } catch (NumberFormatException nfe) {
                        status = -1;
                    }
                    try {
                        load = Integer.parseInt(sRec.getData().get("load"));
                    } catch (NumberFormatException nfe) {
                        load = -1;
                    }
                    try {
                        protoVersion = Integer.parseInt(sRec.getData().get("proto_version"));
                    } catch (NumberFormatException nfe) {
                        protoVersion = -1;
                    }
                    
                    master.notifyServiceRegistred(uuid, name, type, pageUrl, geoCoordinates, totalRam, usedRam, 
                            lastUpdated, status, load, protoVersion);
                }
                
                if (reg.getVersion() != currentVersion) {
                    
                    throw new ErrorResponseException(ErrorUtils.getErrorResponse(ErrorType.ERRNO, 
                            POSIXErrno.POSIX_ERROR_EAGAIN, "The requested version number (" + reg.getVersion() + 
                            ") did not match the expected version (" + currentVersion + ")!"));
                }
                                
                reg.setVersion(++currentVersion);
                reg.setLastUpdatedS(System.currentTimeMillis() / 1000l);

                
                ServiceRecord newRec = new ServiceRecord(reg.build());
                byte[] newData = new byte[newRec.getSize()];
                newRec.serialize(ReusableBuffer.wrap(newData));
                
                component.singleInsert(DIRRequestDispatcher.INDEX_ID_SERVREG, newRec.getUuid().getBytes(), newData, 
                        (OLPStageRequest<DIRRequest>) stageRequest, new AbstractRPCRequestCallback(this) {

                            @Override
                            public <T extends StageRequest<?>> boolean success(Object result, T stageRequest)
                                    throws ErrorResponseException {

                                return success(serviceRegisterResponse.newBuilder().setNewVersion(
                                        reg.getVersion()).build());
                            }
                        });
                
                return false;
            }
        }, DIRRequestDispatcher.INDEX_ID_SERVREG, reg.getUuid().getBytes(), rq);
    }
}