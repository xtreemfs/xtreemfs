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
import org.xtreemfs.dir.data.AddressMappingRecords;
import org.xtreemfs.interfaces.AddressMappingSet;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_getRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_getResponse;

/**
 *
 * @author bjko
 */
public class GetAddressMappingOperation extends DIROperation {

    private final int operationNumber;

    private final Database database;

    public GetAddressMappingOperation(DIRRequestDispatcher master) {
        super(master);
        operationNumber = xtreemfs_address_mappings_getRequest.TAG;
        database = master.getDirDatabase();
    }

    @Override
    public int getProcedureId() {
        return operationNumber;
    }

    @Override
    public void startRequest(DIRRequest rq) {
        xtreemfs_address_mappings_getRequest request = 
            (xtreemfs_address_mappings_getRequest)rq.getRequestMessage();

        if (request.getUuid().length() > 0) {
            //single mapping was requested
            database.lookup(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, 
                    request.getUuid().getBytes(),rq).registerListener(
                            new DBRequestListener<byte[],AddressMappingSet>(true) {
                        
                        @Override
                        AddressMappingSet execute(byte[] result, DIRRequest rq) 
                            throws Exception {
                            
                            if (result == null)
                                return new AddressMappingSet();
                            else
                                return new AddressMappingRecords(ReusableBuffer
                                        .wrap(result)).getAddressMappingSet();
                        }
                    });
        } else {
            //full list requested
            database.prefixLookup(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, 
                    new byte[0], rq).registerListener(
                            new DBRequestListener<Iterator<Entry<byte[],byte[]>>,AddressMappingSet>(true) {
                        
                        @Override
                        AddressMappingSet execute(Iterator<Entry<byte[], byte[]>> result, 
                                DIRRequest rq) throws Exception {
                            
                            AddressMappingRecords list = new AddressMappingRecords();
                            while (result.hasNext()) {
                                Entry<byte[],byte[]> e = result.next();
                                AddressMappingRecords recs = 
                                    new AddressMappingRecords(
                                            ReusableBuffer.wrap(e.getValue()));
                                list.add(recs);
                            }
                            return list.getAddressMappingSet();
                        }
                    });
        }
    }

    @Override
    public boolean isAuthRequired() {
        return false;
    }

    @Override
    public void parseRPCMessage(DIRRequest rq) throws Exception {
        xtreemfs_address_mappings_getRequest amr = new xtreemfs_address_mappings_getRequest();
        rq.deserializeMessage(amr);
    }

    /*
     * (non-Javadoc)
     * @see org.xtreemfs.dir.operations.DIROperation#requestFinished(java.lang.Object, org.xtreemfs.dir.DIRRequest)
     */
    @Override
    void requestFinished(Object result, DIRRequest rq) {
        rq.sendSuccess(new xtreemfs_address_mappings_getResponse((AddressMappingSet) result));
    }
}
