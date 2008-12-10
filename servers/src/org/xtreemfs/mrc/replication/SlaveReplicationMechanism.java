/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.replication;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.brain.storage.LogEntry;
import org.xtreemfs.mrc.brain.storage.SliceID;
import org.xtreemfs.mrc.slices.SliceInfo;

/**
 *
 * @author bjko
 */
public class SlaveReplicationMechanism implements ReplicationMechanism {
    
    public InetSocketAddress master;
    
    private SliceInfo slice;
    
    transient private List<MRCRequest>  pendingRequests;
    
    
    /** Creates a new instance of SlaveReplicationMechanism */
    public SlaveReplicationMechanism(InetSocketAddress master) {
        this.master = master;
        this.pendingRequests = new LinkedList();
    }

    public boolean sendResponseAfterReplication() {
        return false;
    }

    public void registerSlice(SliceInfo slice) {
        this.slice = slice;
        this.slice.setReplicating(true);
    }
    
    public SliceInfo getSlice() {
        return this.slice;
    }



    
}
