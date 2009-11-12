/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.dir.operations;

import java.io.IOException;

import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.BabuDBRequestListener;
import org.xtreemfs.babudb.BabuDBException.ErrorCode;
import org.xtreemfs.babudb.lsmdb.Database;
import org.xtreemfs.babudb.replication.ReplicationManager;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_get_by_uuidRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_get_by_uuidResponse;

/**
 * 
 * @author bjko
 */
public class GetServiceByUuidOperation extends DIROperation {
    
    private final int      operationNumber;
    
    private final Database database;
    
    private final ReplicationManager dbsReplicationManager;
    
    public GetServiceByUuidOperation(DIRRequestDispatcher master) {
        super(master);
        operationNumber = xtreemfs_service_get_by_uuidRequest.TAG;
        database = master.getDirDatabase();
        dbsReplicationManager = master.getDBSReplicationService();
    }
    
    @Override
    public int getProcedureId() {
        return operationNumber;
    }
    
    @Override
    public void startRequest(DIRRequest rq) {
        final xtreemfs_service_get_by_uuidRequest request = 
            (xtreemfs_service_get_by_uuidRequest) rq.getRequestMessage();
            
        database.lookup(DIRRequestDispatcher.INDEX_ID_SERVREG, request.getUuid()
                .getBytes(),rq).registerListener(
                        new BabuDBRequestListener<byte[]>() {
                    
            @Override
            public void finished(byte[] data, Object context) {
                try {
                    ServiceSet services = new ServiceSet();
                    if (data != null) {
                        ServiceRecord dbData = new ServiceRecord(
                                ReusableBuffer.wrap(data));
                        services.add(dbData.getService());
                    }
                    
                    xtreemfs_service_get_by_uuidResponse response = 
                        new xtreemfs_service_get_by_uuidResponse(services);
                    ((DIRRequest) context).sendSuccess(response);
                } catch (IOException e) {
                    Logging.logError(Logging.LEVEL_ERROR, this, e);
                    ((DIRRequest) context).sendInternalServerError(e);
                }
            }
            
            @Override
            public void failed(BabuDBException e, Object context) {
                Logging.logError(Logging.LEVEL_ERROR, this, e);
                if (e.getErrorCode() == ErrorCode.NO_ACCESS && 
                        dbsReplicationManager != null)
                    ((DIRRequest) context).sendRedirectException(
                            dbsReplicationManager.getMaster());
                else
                    ((DIRRequest) context).sendInternalServerError(e);
            }
        });
    }
    
    @Override
    public boolean isAuthRequired() {
        return false;
    }
    
    @Override
    public void parseRPCMessage(DIRRequest rq) throws Exception {
        xtreemfs_service_get_by_uuidRequest amr = new xtreemfs_service_get_by_uuidRequest();
        rq.deserializeMessage(amr);
    }
    
}
