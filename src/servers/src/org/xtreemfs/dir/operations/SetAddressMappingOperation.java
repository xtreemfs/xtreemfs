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

import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.BabuDBException.ErrorCode;
import org.xtreemfs.babudb.lsmdb.BabuDBInsertGroup;
import org.xtreemfs.babudb.lsmdb.Database;
import org.xtreemfs.babudb.replication.ReplicationManager;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.data.AddressMappingRecords;
import org.xtreemfs.interfaces.AddressMapping;
import org.xtreemfs.interfaces.AddressMappingSet;
import org.xtreemfs.interfaces.DIRInterface.ConcurrentModificationException;
import org.xtreemfs.interfaces.DIRInterface.InvalidArgumentException;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_setRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_setResponse;

/**
 * 
 * @author bjko
 */
public class SetAddressMappingOperation extends DIROperation {
    
    private final int      operationNumber;
    
    private final Database database;
    
    private final ReplicationManager dbsReplicationManager;
    
    public SetAddressMappingOperation(DIRRequestDispatcher master) {
        super(master);
        operationNumber = xtreemfs_address_mappings_setRequest.TAG;
        database = master.getDirDatabase();
        dbsReplicationManager = master.getDBSReplicationService();
    }
    
    @Override
    public int getProcedureId() {
        return operationNumber;
    }
    
    @Override
    public void startRequest(DIRRequest rq) {
        try {
            final xtreemfs_address_mappings_setRequest request = (xtreemfs_address_mappings_setRequest) rq
                    .getRequestMessage();
            
            final AddressMappingSet mappings = request.getAddress_mappings();
            String uuid = null;
            if (mappings.size() == 0) {
                rq.sendException(new InvalidArgumentException("must send at least one mapping"));
                return;
            }
            for (AddressMapping am : mappings) {
                if (uuid == null)
                    uuid = am.getUuid();
                if (!am.getUuid().equals(uuid)) {
                    rq.sendException(new InvalidArgumentException("all mappings must have the same UUID"));
                    return;
                }
            }
            
            assert (uuid != null);
            assert (database != null);
            
            /*AddressMappingSet dbData = new AddressMappingSet();
            byte[] data = database.directLookup(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, uuid.getBytes());
            long currentVersion = 0;
            if (data != null) {
                ReusableBuffer buf = ReusableBuffer.wrap(data);
                dbData.deserialize(buf);
                if (dbData.size() > 0) {
                    currentVersion = dbData.get(0).getVersion();
                }
            }
            
            if (mappings.get(0).getVersion() != currentVersion) {
                rq.sendException(new ConcurrentModificationException());
                return;
            }*/
            byte[] data = database.directLookup(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, uuid.getBytes());
            long currentVersion = 0;
            if (data != null) {
                ReusableBuffer buf = ReusableBuffer.wrap(data);
                AddressMappingRecords dbData = new AddressMappingRecords(buf);
                if (dbData.size() > 0) {
                    currentVersion = dbData.getRecord(0).getVersion();
                }
            }

            if (mappings.get(0).getVersion() != currentVersion) {
                rq.sendException(new ConcurrentModificationException());
                return;
            }
            
            currentVersion++;

            for (int i = 0; i < mappings.size(); i++)
                mappings.get(i).setVersion(currentVersion);


            AddressMappingRecords newData = new AddressMappingRecords(mappings);
            final int size = newData.getSize();
            byte[] newBytes = new byte[size];
            ReusableBuffer buf = ReusableBuffer.wrap(newBytes);
            newData.serialize(buf);
            BabuDBInsertGroup ig = database.createInsertGroup();
            ig.addInsert(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, uuid.getBytes(), newBytes);
            database.directInsert(ig, master, true);
            
            xtreemfs_address_mappings_setResponse response = new xtreemfs_address_mappings_setResponse(
                currentVersion);
            rq.sendSuccess(response);
        } catch (IOException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            rq.sendInternalServerError(ex);
        } catch (BabuDBException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            if (ex.getErrorCode() == ErrorCode.NO_ACCESS && dbsReplicationManager != null)
                rq.sendRedirectException(dbsReplicationManager.getMaster());
            else
                rq.sendInternalServerError(ex);
        }
    }
    
    @Override
    public boolean isAuthRequired() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void parseRPCMessage(DIRRequest rq) throws Exception {
        xtreemfs_address_mappings_setRequest amr = new xtreemfs_address_mappings_setRequest();
        rq.deserializeMessage(amr);
    }
    
}
