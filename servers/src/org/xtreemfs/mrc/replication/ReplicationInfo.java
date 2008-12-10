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

import java.io.Serializable;

/**
 *
 * @author bjko
 */
public class ReplicationInfo implements Serializable {
    
    public ReplicationMechanism mechanism;
    
    public int currentViewID;
    
    public int nextSequenceID;
    
    /** This is the sequence ID of the first
     *  (oldest) log entry available.
     */
    public int lastAvailSqID;
    
    /** Creates a new instance of ReplicationInfo */
    public ReplicationInfo() {
        currentViewID = 1;
        nextSequenceID = 1;
        lastAvailSqID = 1;
    }
    
    public int getNextSequenceID() {
        return this.nextSequenceID++;
    }
    
}
