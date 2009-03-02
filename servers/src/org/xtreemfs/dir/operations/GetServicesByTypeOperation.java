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
import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.interfaces.ServiceRegistry;
import org.xtreemfs.interfaces.ServiceRegistrySet;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.interfaces.DIRInterface.service_get_by_typeRequest;
import org.xtreemfs.interfaces.DIRInterface.service_get_by_typeResponse;

/**
 *
 * @author bjko
 */
public class GetServicesByTypeOperation extends DIROperation {

    private final int operationNumber;

    private final BabuDB database;

    public GetServicesByTypeOperation(DIRRequestDispatcher master) {
        super(master);
        service_get_by_typeRequest tmp = new service_get_by_typeRequest();
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
            final service_get_by_typeRequest request = (service_get_by_typeRequest)rq.getRequestMessage();


            Iterator<Entry<byte[],byte[]>> iter = database.directPrefixLookup(DIRRequestDispatcher.DB_NAME, DIRRequestDispatcher.INDEX_ID_SERVREG, new byte[0]);

            ServiceRegistrySet services = new ServiceRegistrySet();

            while (iter.hasNext()) {
                final Entry<byte[],byte[]> e = iter.next();
                final ServiceRegistry servEntry = new ServiceRegistry();
                ReusableBuffer buf = ReusableBuffer.wrap(e.getValue());
                servEntry.deserialize(buf);
                if (servEntry.getService_type() == request.getType())
                    services.add(servEntry);
            }

            service_get_by_typeResponse response = new service_get_by_typeResponse(services);
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
        service_get_by_typeRequest amr = new service_get_by_typeRequest();
        rq.deserializeMessage(amr);
    }

}
