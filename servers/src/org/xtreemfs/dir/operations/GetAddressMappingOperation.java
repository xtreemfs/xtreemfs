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
import org.xtreemfs.interfaces.AddressMappingSet;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.interfaces.AddressMapping;
import org.xtreemfs.interfaces.DIRInterface.address_mappings_getRequest;
import org.xtreemfs.interfaces.DIRInterface.address_mappings_getResponse;

/**
 *
 * @author bjko
 */
public class GetAddressMappingOperation extends DIROperation {

    private final int operationNumber;

    private final BabuDB database;

    public GetAddressMappingOperation(DIRRequestDispatcher master) {
        super(master);
        address_mappings_getRequest tmp = new address_mappings_getRequest();
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
            final address_mappings_getRequest request = (address_mappings_getRequest)rq.getRequestMessage();

            if (request.getUuid().length() > 0) {
                //single mapping was requested
                byte[] result = database.directLookup(DIRRequestDispatcher.DB_NAME, DIRRequestDispatcher.INDEX_ID_ADDRMAPS, request.getUuid().getBytes());
                if (result == null) {
                    address_mappings_getResponse response = new address_mappings_getResponse();
                    rq.sendSuccess(response);
                } else {
                    AddressMappingSet set = new AddressMappingSet();
                    set.deserialize(ReusableBuffer.wrap(result));
                    address_mappings_getResponse response = new address_mappings_getResponse(set);
                    rq.sendSuccess(response);
                }
            } else {
                //full list requested
                AddressMappingSet list = new AddressMappingSet();
                Iterator<Entry<byte[],byte[]>> iter = database.directPrefixLookup(DIRRequestDispatcher.DB_NAME, DIRRequestDispatcher.INDEX_ID_ADDRMAPS, new byte[0]);
                while (iter.hasNext()) {
                    Entry<byte[],byte[]> e = iter.next();
                    AddressMappingSet set = new AddressMappingSet();
                    set.deserialize(ReusableBuffer.wrap(e.getValue()));
                    for (AddressMapping m : set)
                        list.add(m);
                }
                address_mappings_getResponse response = new address_mappings_getResponse(list);
                rq.sendSuccess(response);

            }
        } catch (BabuDBException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this,ex);
            rq.sendInternalServerError(ex);
        }
    }

    @Override
    public boolean isAuthRequired() {
        return false;
    }

    @Override
    public void parseRPCMessage(DIRRequest rq) throws Exception {
        address_mappings_getRequest amr = new address_mappings_getRequest();
        rq.deserializeMessage(amr);
    }

}
