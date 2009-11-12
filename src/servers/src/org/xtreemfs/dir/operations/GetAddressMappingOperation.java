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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.BabuDBRequestListener;
import org.xtreemfs.babudb.BabuDBException.ErrorCode;
import org.xtreemfs.babudb.lsmdb.Database;
import org.xtreemfs.babudb.replication.ReplicationManager;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.AddressMappingRecords;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_getRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_getResponse;

/**
 *
 * @author bjko
 */
public class GetAddressMappingOperation extends DIROperation {

    private final int operationNumber;

    private final Database database;
    
    private final ReplicationManager dbsReplicationManager;

    public GetAddressMappingOperation(DIRRequestDispatcher master) {
        super(master);
        operationNumber = xtreemfs_address_mappings_getRequest.TAG;
        database = master.getDirDatabase();
        dbsReplicationManager = master.getDBSReplicationService();
    }

    @Override
    public int getProcedureId() {
        return operationNumber;
    }

    @Override
    public void startRequest(DIRRequest rq) {
        final xtreemfs_address_mappings_getRequest request = 
            (xtreemfs_address_mappings_getRequest)rq.getRequestMessage();

        if (request.getUuid().length() > 0) {
            //single mapping was requested
            database.lookup(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, 
                    request.getUuid().getBytes(),rq).registerListener(
                            new BabuDBRequestListener<byte[]>() {
                
                @Override
                public void finished(byte[] result, Object context) {
                    if (result == null) {
                        ((DIRRequest) context).sendSuccess(
                                request.createDefaultResponse());
                    } else {
                        try {
                            AddressMappingRecords set = new AddressMappingRecords(
                                    ReusableBuffer.wrap(result));
                            xtreemfs_address_mappings_getResponse response = 
                                new xtreemfs_address_mappings_getResponse(set
                                        .getAddressMappingSet());
                            
                            ((DIRRequest) context).sendSuccess(response);
                        } catch (IOException e) {
                            Logging.logError(Logging.LEVEL_ERROR, this, e);
                            ((DIRRequest) context).sendInternalServerError(e);
                        }
                    }
                }
                
                @Override
                public void failed(BabuDBException e, Object context) {
                    Logging.logError(Logging.LEVEL_ERROR, this, e);
                    if (e.getErrorCode() == ErrorCode.NO_ACCESS &&
                            dbsReplicationManager != null)
                        ((DIRRequest) context).sendRedirectException(
                                dbsReplicationManager.getMaster());
                    else
                        ((DIRRequest) context).sendInternalServerError(e);
                }
            });
        } else {
            //full list requested
            database.prefixLookup(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, 
                    new byte[0], rq).registerListener(
                            new BabuDBRequestListener<Iterator<Entry<byte[],byte[]>>>() {
                
                @Override
                public void finished(Iterator<Entry<byte[], byte[]>> result, Object context) {
                    try {
                        AddressMappingRecords list = new AddressMappingRecords();
                        while (result.hasNext()) {
                            Entry<byte[],byte[]> e = result.next();
                            AddressMappingRecords recs = new AddressMappingRecords(ReusableBuffer.wrap(e.getValue()));
                            list.add(recs);
                        }
                        xtreemfs_address_mappings_getResponse response = new xtreemfs_address_mappings_getResponse(list.getAddressMappingSet());
                        ((DIRRequest) context).sendSuccess(response);
                    } catch (IOException e) {
                        Logging.logError(Logging.LEVEL_ERROR, this, e);
                        ((DIRRequest) context).sendInternalServerError(e);
                    }
                }
                
                @Override
                public void failed(BabuDBException e, Object context) {
                    Logging.logError(Logging.LEVEL_ERROR, this, e);
                    if (e.getErrorCode() == ErrorCode.NO_ACCESS &&
                            dbsReplicationManager != null)
                        ((DIRRequest) context).sendRedirectException(
                                dbsReplicationManager.getMaster());
                    else
                        ((DIRRequest) context).sendInternalServerError(e);
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

}
