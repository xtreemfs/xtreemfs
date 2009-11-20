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
 * AUTHORS: Felix Langner (ZIB)
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replication_to_masterResponse;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.ReplicationManager;

/**
 *
 * @author flangner
 * @since 10/09/2009
 */
public class ReplicationToMasterOperation extends MRCOperation {
    
    private final ReplicationManager dbsReplicationManager;

    public ReplicationToMasterOperation(MRCRequestDispatcher master) {
        super(master);
        dbsReplicationManager = master.getDBSReplicationService();
    }

    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        try {
            
            // check password to ensure that user is authorized
            if (master.getConfig().getAdminPassword() != null
                && !master.getConfig().getAdminPassword().equals(
                rq.getDetails().password))
                    throw new UserException(ErrNo.EPERM, "invalid password");
            
            dbsReplicationManager.declareToMaster();
            rq.setResponse(new xtreemfs_replication_to_masterResponse());
            finishRequest(rq);
        } catch (DatabaseException e) {
            finishRequest(rq, new ErrorRecord(ErrorClass.REPLICATION, 
                    e.getMessage()));
        } 
    }
}
