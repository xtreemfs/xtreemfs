/*  Copyright (c) 2009 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
package org.xtreemfs.mrc.database.babudb;

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.mrc.database.ReplicationManager;

/**
 * 
 * @author flangner
 * @since 10/19/2009
 */
public class BabuDBReplicationManger implements ReplicationManager {

    private final org.xtreemfs.babudb.replication.ReplicationManager replMan;
    
    public BabuDBReplicationManger(BabuDB dbs) {
        this.replMan = dbs.getReplicationManager();
    }
    
    /* (non-Javadoc)
     * @see org.xtreemfs.mrc.database.ReplicationManager#manualFailover()
     */
    @Override
    public void manualFailover() {
        replMan.manualFailover();
    }
}
