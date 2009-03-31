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
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_offlineRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_registerRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_service_registerResponse;

/**
 *
 * @author bjko
 */
public class ServiceOfflineOperation extends DIROperation {

    private final int operationNumber;

    private final BabuDB database;

    public ServiceOfflineOperation(DIRRequestDispatcher master) {
        super(master);
        xtreemfs_service_offlineRequest tmp = new xtreemfs_service_offlineRequest();
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
            final xtreemfs_service_offlineRequest request = (xtreemfs_service_offlineRequest)rq.getRequestMessage();

            byte[] data = database.directLookup(DIRRequestDispatcher.DB_NAME,
                    DIRRequestDispatcher.INDEX_ID_SERVREG, request.getUuid().getBytes());
            long currentVersion = 0;
            if (data != null) {
                final Service dbData = new Service();
                ReusableBuffer buf = ReusableBuffer.wrap(data);
                dbData.deserialize(buf);

                dbData.setLast_updated_s(0);

                final int dataSize = dbData.calculateSize();
                ONCRPCBufferWriter writer = new ONCRPCBufferWriter(dataSize);
                dbData.serialize(writer);
                writer.flip();
                assert(writer.getBuffers().size() == 1);
                byte[] newData = writer.getBuffers().get(0).array();
                writer.freeBuffers();
                BabuDBInsertGroup ig = database.createInsertGroup(DIRRequestDispatcher.DB_NAME);
                ig.addInsert(DIRRequestDispatcher.INDEX_ID_SERVREG, request.getUuid().getBytes(), newData);
                database.directInsert(ig);
            }
            
            xtreemfs_service_registerResponse response = new xtreemfs_service_registerResponse(currentVersion);
            rq.sendSuccess(response);
        } catch (BabuDBException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this,ex);
            rq.sendInternalServerError(ex);
        } catch (Throwable th) {
            Logging.logMessage(Logging.LEVEL_ERROR, this,th);
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
