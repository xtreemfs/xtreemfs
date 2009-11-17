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

import org.xtreemfs.babudb.lsmdb.Database;
import org.xtreemfs.common.buffer.ReusableBuffer;
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
    
    public ServiceOfflineOperation(DIRRequestDispatcher master) {
        super(master);
        operationNumber = xtreemfs_service_offlineRequest.TAG;
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        return operationNumber;
    }
    
    @Override
    public void startRequest(DIRRequest rq) {
        final xtreemfs_service_offlineRequest request = 
            (xtreemfs_service_offlineRequest) rq.getRequestMessage();
  
        database.lookup(DIRRequestDispatcher.INDEX_ID_SERVREG, 
                request.getUuid().getBytes(),rq).registerListener(
                        new DBRequestListener<byte[], Object>(false) {
                    
                    @Override
                    Object execute(byte[] result, DIRRequest rq) 
                            throws Exception {
                        if (result != null) {
                            ReusableBuffer buf = ReusableBuffer.wrap(result);
                            ServiceRecord dbData = new ServiceRecord(buf);
                            
                            dbData.setLast_updated_s(0);
                            dbData.setVersion(dbData.getVersion()+1);
                            
                            byte[] newData = new byte[dbData.getSize()];
                            dbData.serialize(ReusableBuffer.wrap(newData));
                            database.singleInsert(DIRRequestDispatcher.INDEX_ID_SERVREG, 
                                    request.getUuid().getBytes(), newData, rq)
                                    .registerListener(
                                            new DBRequestListener<Object, Object>(true) {
                                        
                                        @Override
                                        Object execute(Object result, DIRRequest rq) 
                                                throws Exception {
                                            return null;
                                        }
                                    });
                        } else 
                            requestFinished(null, rq);
                        
                        return null;
                    }
                });
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

    /*
     * (non-Javadoc)
     * @see org.xtreemfs.dir.operations.DIROperation#requestFinished(java.lang.Object, org.xtreemfs.dir.DIRRequest)
     */
    @Override
    void requestFinished(Object result, DIRRequest rq) {
        rq.sendSuccess(new xtreemfs_service_offlineResponse());
    }
}
