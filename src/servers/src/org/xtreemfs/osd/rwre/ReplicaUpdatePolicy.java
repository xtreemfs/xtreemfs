/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Queue;
import org.xtreemfs.common.buffer.ASCIIString;
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

    public static final long UNBOUND_RESET = -1;

    protected List<InetSocketAddress>       remoteOSDs;

    protected final ASCIIString             cellId;

    protected long localObjVersion;
    
    public ReplicaUpdatePolicy(List<InetSocketAddress> remoteOSDs, ASCIIString cellId, long maxObjVerOnDisk) {
        this.remoteOSDs = remoteOSDs;
        this.cellId = cellId;
        this.localObjVersion = maxObjVerOnDisk;
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
    public abstract void    executeReset(FileCredentials credentials, long updateObjVer, ExecuteResetCallback callback);

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

    public abstract long       onClientOperation(Operation operation, long objVersion, ReplicaState currentState, ASCIIString leaseOwner)
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
