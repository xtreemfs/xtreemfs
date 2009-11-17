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

import java.util.Iterator;
import java.util.Map.Entry;

import org.xtreemfs.babudb.lsmdb.Database;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_get_by_nameRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_get_by_nameResponse;

/**
 *
 * @author bjko
 */
public class GetServiceByNameOperation extends DIROperation {

    private final int operationNumber;

    private final Database database;

    public GetServiceByNameOperation(DIRRequestDispatcher master) {
        super(master);
        operationNumber = xtreemfs_service_get_by_nameRequest.TAG;
        database = master.getDirDatabase();
    }

    @Override
    public int getProcedureId() {
        return operationNumber;
    }

    @Override
    public void startRequest(DIRRequest rq) {
        final xtreemfs_service_get_by_nameRequest request = 
            (xtreemfs_service_get_by_nameRequest)rq.getRequestMessage();

        database.prefixLookup(DIRRequestDispatcher.INDEX_ID_SERVREG, 
                new byte[0],rq).registerListener(new DBRequestListener<Iterator<Entry<byte[],byte[]>>, ServiceSet>(true) {
                    
                    @Override
                    ServiceSet execute(Iterator<Entry<byte[], byte[]>> result, 
                            DIRRequest rq) throws Exception {
                        
                        ServiceSet services = new ServiceSet();
                        long now = System.currentTimeMillis()/1000l;

                        while (result.hasNext()) {
                            Entry<byte[],byte[]> e = result.next();
                            ServiceRecord servEntry = new ServiceRecord(
                                    ReusableBuffer.wrap(e.getValue()));
                            if (servEntry.getName().equals(request.getName()))
                                services.add(servEntry.getService());

                            long secondsSinceLastUpdate = now - servEntry.getLast_updated_s();
                            servEntry.getData().put("seconds_since_last_update",
                                    Long.toString(secondsSinceLastUpdate));
                        }
                        return services;
                    }
                });
        
    }

    @Override
    public boolean isAuthRequired() {
        return false;
    }

    @Override
    public void parseRPCMessage(DIRRequest rq) throws Exception {
        xtreemfs_service_get_by_nameRequest amr = 
            new xtreemfs_service_get_by_nameRequest();
        rq.deserializeMessage(amr);
    }

    /*
     * (non-Javadoc)
     * @see org.xtreemfs.dir.operations.DIROperation#requestFinished(java.lang.Object, org.xtreemfs.dir.DIRRequest)
     */
    @Override
    void requestFinished(Object result, DIRRequest rq) {
       rq.sendSuccess(new xtreemfs_service_get_by_nameResponse((ServiceSet) result));
    }

}
