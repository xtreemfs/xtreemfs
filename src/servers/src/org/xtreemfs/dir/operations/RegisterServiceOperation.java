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
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.DIRInterface.ConcurrentModificationException;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_registerRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_registerResponse;

/**
 * 
 * @author bjko
 */
public class RegisterServiceOperation extends DIROperation {
    
    private final int      operationNumber;
    
    private final Database database;
        
    public RegisterServiceOperation(DIRRequestDispatcher master) {
        super(master);
        operationNumber = xtreemfs_service_registerRequest.TAG;
        database = master.getDirDatabase();
    }
    
    @Override
    public int getProcedureId() {
        return operationNumber;
    }
    
    @Override
    public void startRequest(DIRRequest rq) {
        final xtreemfs_service_registerRequest request = 
            (xtreemfs_service_registerRequest) rq.getRequestMessage();
        
        final Service reg = request.getService();
        
        database.lookup(DIRRequestDispatcher.INDEX_ID_SERVREG, 
                reg.getUuid().getBytes(),rq).registerListener(
                        new DBRequestListener<byte[], Long>(false) {
                    
                    @Override
                    Long execute(byte[] result, DIRRequest rq) throws Exception {
                        long currentVersion = 0;
                        if (result != null) {
                            ReusableBuffer buf = ReusableBuffer.wrap(result);
                            ServiceRecord dbData = new ServiceRecord(buf);
                            currentVersion = dbData.getVersion();
                        }
                        
                        if (reg.getVersion() != currentVersion)
                            throw new ConcurrentModificationException();
                        
                        final long version = ++currentVersion;
                        
                        reg.setVersion(currentVersion);
                        reg.setLast_updated_s(System.currentTimeMillis() / 1000l);
                        
                        ServiceRecord newRec = new ServiceRecord(reg);
                        byte[] newData = new byte[newRec.getSize()];
                        newRec.serialize(ReusableBuffer.wrap(newData));
                        
                        database.singleInsert(DIRRequestDispatcher.INDEX_ID_SERVREG, 
                                reg.getUuid().getBytes(), newData, rq)
                                .registerListener(new DBRequestListener<Object, Long>(true) {
                                    
                                    @Override
                                    Long execute(Object result, DIRRequest rq) 
                                            throws Exception {
                                        
                                        return version;
                                    }
                                });
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
        xtreemfs_service_registerRequest amr = new xtreemfs_service_registerRequest();
        rq.deserializeMessage(amr);
    }

    @Override
    void requestFinished(Object result, DIRRequest rq) {
        rq.sendSuccess(new xtreemfs_service_registerResponse((Long) result));
    }
    
}
