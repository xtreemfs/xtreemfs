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

import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.BabuDBException.ErrorCode;
import org.xtreemfs.babudb.lsmdb.BabuDBInsertGroup;
import org.xtreemfs.babudb.lsmdb.Database;
import org.xtreemfs.babudb.replication.ReplicationManager;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_removeRequest;
import org.xtreemfs.interfaces.DIRInterface.xtreemfs_address_mappings_removeResponse;

/**
 *
 * @author bjko
 */
public class DeleteAddressMappingOperation extends DIROperation {

    private final int operationNumber;

    private final Database database;
    
    private final ReplicationManager dbsReplicationManager;

    public DeleteAddressMappingOperation(DIRRequestDispatcher master) {
        super(master);
        operationNumber = xtreemfs_address_mappings_removeRequest.TAG;
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
            final xtreemfs_address_mappings_removeRequest request = (xtreemfs_address_mappings_removeRequest)rq.getRequestMessage();
            
            BabuDBInsertGroup ig = database.createInsertGroup();
            ig.addDelete(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, request.getUuid().getBytes());
            database.directInsert(ig, master, true);
            
            xtreemfs_address_mappings_removeResponse response = new xtreemfs_address_mappings_removeResponse();
            rq.sendSuccess(response);
            
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
        xtreemfs_address_mappings_removeRequest amr = new xtreemfs_address_mappings_removeRequest();
        rq.deserializeMessage(amr);
    }

}
