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

import org.xtreemfs.babudb.replication.ReplicationManager;
import org.xtreemfs.babudb.replication.SlavesStates.NotEnoughAvailableSlavesException;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.dir.DIRRequest;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.ErrorCodes;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.DIRInterface.replication_toMasterRequest;
import org.xtreemfs.interfaces.DIRInterface.replication_toMasterResponse;

/**
 *
 * @author flangner
 * @since 09/16/2009
 */
public class ReplicationToMasterOperation extends DIROperation {

    private final int operationNumber;
    
    private final ReplicationManager dbsReplicationManager;

    public ReplicationToMasterOperation(DIRRequestDispatcher master) {
        super(master);
        operationNumber = replication_toMasterRequest.TAG;
        dbsReplicationManager = master.getDBSReplicationService();
    }

    @Override
    public int getProcedureId() {
        return operationNumber;
    }

    @Override
    public void startRequest(DIRRequest rq) {
        try {
            
            // check password to ensure that user is authorized
            UserCredentials uc = rq.getRPCRequest().getUserCredentials();
            if ((uc == null) || (!uc.getPassword().equals(master.getConfig().getAdminPassword()))) {
                rq.sendDIRException(ErrorCodes.AUTH_FAILED, "this operation requires an admin password");
                return;
            }
            
            dbsReplicationManager.declareToMaster();
            replication_toMasterResponse response = new replication_toMasterResponse();
            rq.sendSuccess(response);
        } catch (NotEnoughAvailableSlavesException e) {
            rq.sendDIRException(ErrorCodes.NOT_ENOUGH_PARTICIPANTS, e.getMessage());
        } catch (Exception e) {
            Logging.logError(Logging.LEVEL_ERROR, this, e);
            rq.sendInternalServerError(e);
        } 
    }

    @Override
    public boolean isAuthRequired() {
        return false;
    }

    @Override
    public void parseRPCMessage(DIRRequest rq) throws Exception {
        replication_toMasterRequest amr = new replication_toMasterRequest();
        rq.deserializeMessage(amr);
    }

}
