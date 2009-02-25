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

package org.xtreemfs.new_dir.operations;

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.interfaces.DIRInterface.getServiceByUuidRequest;
import org.xtreemfs.interfaces.DIRInterface.getServiceByUuidResponse;
import org.xtreemfs.interfaces.ServiceRegistry;
import org.xtreemfs.interfaces.ServiceRegistrySet;
import org.xtreemfs.new_dir.DIRRequest;
import org.xtreemfs.new_dir.DIRRequestDispatcher;

/**
 *
 * @author bjko
 */
public class GetServiceByUuidOperation extends DIROperation {

    private final int operationNumber;

    private final BabuDB database;

    public GetServiceByUuidOperation(DIRRequestDispatcher master) {
        super(master);
        getServiceByUuidRequest tmp = new getServiceByUuidRequest();
        operationNumber = tmp.getOperationNumber();
        database = master.getDatabase();
    }

    @Override
    public int getProcedureId() {
        return operationNumber;
    }

    @Override
    public void startRequest(DIRRequest rq) {
        try {
            final getServiceByUuidRequest request = (getServiceByUuidRequest)rq.getRequestMessage();

            
            byte[] data = database.directLookup(DIRRequestDispatcher.DB_NAME,
                    DIRRequestDispatcher.INDEX_ID_SERVREG, request.getUuid().getBytes());

            ServiceRegistrySet services = new ServiceRegistrySet();
            if (data != null) {
                ServiceRegistry dbData = new ServiceRegistry();
                ReusableBuffer buf = ReusableBuffer.wrap(data);
                dbData.deserialize(buf);
                services.add(dbData);
            }
            
            getServiceByUuidResponse response = new getServiceByUuidResponse(services);
            rq.sendSuccess(response);
        } catch (BabuDBException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this,ex);
            rq.sendInternalServerError();
        }
    }

    @Override
    public boolean isAuthRequired() {
        return false;
    }

    @Override
    public void parseRPCMessage(DIRRequest rq) throws Exception {
        getServiceByUuidRequest amr = new getServiceByUuidRequest();
        rq.deserializeMessage(amr);
    }

}
