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

package org.xtreemfs.mrc.slices;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import org.xtreemfs.mrc.brain.storage.SliceID;
import org.xtreemfs.mrc.replication.NullReplicationMechanism;
import org.xtreemfs.mrc.replication.ReplicationMechanism;

/**
 *
 * @author bjko
 */
public class SliceInfo implements Serializable {

    public final SliceID sliceID;

    /** The current viewID
     */
    private volatile int currentViewID;

    /** sequence ID to assign to the next operation
     */
    private AtomicInteger nextSequenceID;

    /** This is the sequence ID of the first
     *  (oldest) log entry available.
     */
    private int lastAvailSqID;

    /** replication mechanism to use for this slice
     */
    private ReplicationMechanism replMech;

    /** slice status
     */
    public static enum SliceStatus { ONLINE, READONLY, OFFLINE };

    /** current status of the slice (as seen by the client)
     */
    private volatile SliceStatus status;

    /** true if replication is working and accepting remote operations
     */
    private volatile boolean replicationOperational;

    private boolean deleted;

    /** Creates a new instance of SliceInfo */
    public SliceInfo(SliceID id, ReplicationMechanism mecha) {
        this.sliceID = id;
        nextSequenceID = new AtomicInteger(0);
        currentViewID = 0;
        lastAvailSqID = 0;
        if (mecha == null) {
            replMech = new NullReplicationMechanism();
        } else {
            replMech = mecha;
        }
        status = SliceStatus.ONLINE;
        replicationOperational = false;
    }

    public int getNextSequenceID() {
        return this.nextSequenceID.getAndIncrement();
    }

    public int getCurrentSequenceID() {
        return this.nextSequenceID.get();
    }

    public int getLastAvailSqID() {
        return this.lastAvailSqID;
    }

    public int getCurrentViewID() {
        return currentViewID;
    }

    public synchronized void viewChange() {
        currentViewID++;
        lastAvailSqID = 0;
        nextSequenceID.set(0);
    }

    public ReplicationMechanism getReplicationMechanism() {
        return this.replMech;
    }

    public synchronized void changeReplicationMechanism(ReplicationMechanism rm) {
        currentViewID++;
        lastAvailSqID = 0;
        nextSequenceID.set(0);
        replMech = rm;
    }

    public SliceStatus getStatus() {
        return status;
    }

    public void setStatus(SliceStatus status) {
        this.status = status;
    }

    public Boolean isReplicating() {
        return this.replicationOperational;
    }

    public void setReplicating(boolean rStatus) {
        this.replicationOperational = rStatus;
    }

    public void setLastAvailSqID(int sqID) {
        this.lastAvailSqID = sqID;
    }

    public void setCurrentViewID(int viewID) {
        assert(viewID >= 0);
        this.currentViewID = viewID;
    }

    public void setNextSequenceID(int id) {
        this.nextSequenceID.set(id);
    }

    public void setDeleted() {
        this.deleted = true;
    }

    public boolean isDeleted() {
        return this.deleted;
    }

}
