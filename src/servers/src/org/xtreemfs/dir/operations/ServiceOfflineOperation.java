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

import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.BabuDBException.ErrorCode;
import org.xtreemfs.babudb.lsmdb.BabuDBInsertGroup;
import org.xtreemfs.babudb.lsmdb.Database;
import org.xtreemfs.babudb.replication.ReplicationManager;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_offlineRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_offlineResponse;

/**
 * 
 * @author bjko
 */
public class ServiceOfflineOperation extends DIROperation {
    
    private final int      operationNumber;
    
    private final Database database;
    
    private final ReplicationManager dbsReplicationManager;
    
    public ServiceOfflineOperation(DIRRequestDispatcher master) {
        super(master);
        operationNumber = xtreemfs_service_offlineRequest.TAG;
        database = master.getDirDatabase();
        dbsReplicationManager = master.getDBSReplicationService();
    }
    
    @Override
    public int getProcedureId() {
        return operationNumber;
    }
    
    @Override
    public void startRequest(DIRRequest rq) {
        try {
            final xtreemfs_service_offlineRequest request = (xtreemfs_service_offlineRequest) rq
                    .getRequestMessage();
            
            byte[] data = database.directLookup(DIRRequestDispatcher.INDEX_ID_SERVREG, request.getUuid()
                    .getBytes());
            if (data != null) {
                ReusableBuffer buf = ReusableBuffer.wrap(data);
                ServiceRecord dbData = new ServiceRecord(buf);
                
                dbData.setLast_updated_s(0);
                dbData.setVersion(dbData.getVersion()+1);
                
                byte[] newData = new byte[dbData.getSize()];
                dbData.serialize(ReusableBuffer.wrap(newData));
                BabuDBInsertGroup ig = database.createInsertGroup();
                ig.addInsert(DIRRequestDispatcher.INDEX_ID_SERVREG, request.getUuid().getBytes(), newData);
                database.directInsert(ig);
            }
            
            xtreemfs_service_offlineResponse response = new xtreemfs_service_offlineResponse();
            rq.sendSuccess(response);
        } catch (BabuDBException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            if (ex.getErrorCode() == ErrorCode.NO_ACCESS && dbsReplicationManager != null)
                rq.sendRedirectException(dbsReplicationManager.getMaster());
            else
                rq.sendInternalServerError(ex);
        } catch (Throwable th) {
            Logging.logError(Logging.LEVEL_ERROR, this, th);
            rq.sendInternalServerError(th);
        }
    }
    
    @Override
    public boolean isAuthRequired() {
        return false;
    }
    
    @Override
    public void parseRPCMessage(DIRRequest rq) throws Exception {
        xtreemfs_service_offlineRequest amr = new xtreemfs_service_offlineRequest();
        rq.deserializeMessage(amr);
    }
    
}
