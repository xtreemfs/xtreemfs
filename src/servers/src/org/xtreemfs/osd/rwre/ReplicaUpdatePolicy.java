/*  Copyright (c) 2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Queue;
import org.xtreemfs.common.buffer.ASCIIString;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.interfaces.OSDInterface.RedirectException;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.osd.rwre.RWReplicationStage.Operation;
import org.xtreemfs.osd.rwre.ReplicatedFileState.ReplicaState;

/**
 *
 * @author bjko
 */
public abstract class ReplicaUpdatePolicy {

    public static final long UNLIMITED_RESET = -1;

    protected List<InetSocketAddress>       remoteOSDs;

    protected final ASCIIString             cellId;

    protected long localObjVersion;
    
    public ReplicaUpdatePolicy(List<InetSocketAddress> remoteOSDs, ASCIIString cellId) {
        this.remoteOSDs = remoteOSDs;
        this.cellId = cellId;
        localObjVersion = -1;
    }

    public List<InetSocketAddress> getRemoteOSDs() {
        return remoteOSDs;
    }

    public ASCIIString getCellId() {
        return cellId;
    }

    public void objectFetched(long objVersion) {
        if (objVersion > localObjVersion)
            localObjVersion = objVersion;
    }

    public void setLocalObjectVersion(long localMaxObjVer) {
        localObjVersion = localMaxObjVer;
    }

    /**
     *
     * @return true, if this is primary/backup, false if all replicas are primary
     */
    public abstract boolean requiresLease();

    /**
     * called to execute a reset
     * @param credentials
     * @param maxLocalOV
     */
    public abstract void    executeReset(FileCredentials credentials, long updateObjVer,
            long localFileSize, long localTruncateEpoch, ExecuteResetCallback callback);

    public static interface ExecuteResetCallback {
        public void finished(Queue<ObjectFetchRecord> objectsToFetch);
        public void failed(Exception error);
    }

    /**
     * called to execute a client write operation
     */
    public abstract void    executeWrite(FileCredentials credentials, long objNo,
            long objVersion, ObjectData data,
            ClientOperationCallback callback);

    public abstract void    executeTruncate(FileCredentials credentials,
            long newFileSize, long newObjectVersion,
            ClientOperationCallback callback);

    public static interface ClientOperationCallback {
        public void finsihed();
        public void failed(Exception error);
    }

    public abstract long       onClientOperation(Operation operation, long objVersion, ReplicaState currentState, Flease lease)
            throws RedirectException, OSDException, IOException;

    public abstract boolean    onRemoteUpdate(long objVersion, ReplicaState currentState) throws IOException;

    /**
     *
     * @return true, if the policy needs to reset the replica
     * @throws IOException
     */
    public abstract boolean    onPrimary() throws IOException;

    /**
     *
     * @return true, if the policy needs to reset the replica
     * @throws IOException
     */
    public abstract boolean    onBackup() throws IOException;

    public abstract void       onFailed() throws IOException;

    /*public static interface PrepareOperationCallback {
        public void canExecute(long newObjectVersion);
        public void changeState(ReplicatedFileState.ReplicaState newState);
        public void failed(Exception error);
    }*/

    public abstract void    closeFile();

}
