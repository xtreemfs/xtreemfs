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

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.BabuDBInsertGroup;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.interfaces.Exceptions.ConcurrentModificationException;
import org.xtreemfs.interfaces.ServiceRegistry;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.interfaces.DIRInterface.service_registerRequest;
import org.xtreemfs.interfaces.DIRInterface.service_registerResponse;
import org.xtreemfs.interfaces.KeyValuePair;

/**
 *
 * @author bjko
 */
public class RegisterServiceOperation extends DIROperation {

    private final int operationNumber;

    private final BabuDB database;

    public RegisterServiceOperation(DIRRequestDispatcher master) {
        super(master);
        service_registerRequest tmp = new service_registerRequest();
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
            final service_registerRequest request = (service_registerRequest)rq.getRequestMessage();

            final ServiceRegistry reg = request.getService();

            byte[] data = database.directLookup(DIRRequestDispatcher.DB_NAME,
                    DIRRequestDispatcher.INDEX_ID_SERVREG, reg.getUuid().getBytes());
            long currentVersion = 0;
            if (data != null) {
                ServiceRegistry dbData = new ServiceRegistry();
                ReusableBuffer buf = ReusableBuffer.wrap(data);
                dbData.deserialize(buf);
                currentVersion = dbData.getVersion();
            }

            if (reg.getVersion() != currentVersion) {
                rq.sendException(new ConcurrentModificationException());
                return;
            }

            currentVersion++;

            reg.setVersion(currentVersion);
            reg.getData().add(new KeyValuePair("lastUpdated",Long.toString(System.currentTimeMillis()/1000l)));

            ONCRPCBufferWriter writer = new ONCRPCBufferWriter(reg.calculateSize());
            reg.serialize(writer);
            byte[] newData = writer.getBuffers().get(0).array();
            BabuDBInsertGroup ig = database.createInsertGroup(DIRRequestDispatcher.DB_NAME);
            ig.addInsert(DIRRequestDispatcher.INDEX_ID_SERVREG, reg.getUuid().getBytes(), newData);
            database.directInsert(ig);
            
            service_registerResponse response = new service_registerResponse(currentVersion);
            rq.sendSuccess(response);
        } catch (BabuDBException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this,ex);
            rq.sendInternalServerError();
        } catch (Throwable th) {
            Logging.logMessage(Logging.LEVEL_ERROR, this,th);
            rq.sendInternalServerError();
        }
    }

    @Override
    public boolean isAuthRequired() {
        return false;
    }

    @Override
    public void parseRPCMessage(DIRRequest rq) throws Exception {
        service_registerRequest amr = new service_registerRequest();
        rq.deserializeMessage(amr);
    }

}
