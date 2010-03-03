/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import org.xtreemfs.common.buffer.ASCIIString;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.ObjectData;

/**
 *
 * @author bjko
 */
public abstract class ReplicaUpdatePolicy {

    protected List<InetSocketAddress> remoteOSDs;

    protected final ASCIIString             cellId;
    
    public ReplicaUpdatePolicy(List<InetSocketAddress> remoteOSDs, ASCIIString cellId) {
        this.remoteOSDs = remoteOSDs;
        this.cellId = cellId;
    }

    public List<InetSocketAddress> getRemoteOSDs() {
        return remoteOSDs;
    }

    public abstract void closeFile();

    /*public abstract void canExecuteWrite(FileCredentials credentials, long objNo, long objVersion, CanExecuteWriteCallback callback);

    public static interface CanExecuteWriteCallback {
        public void canExecuteWriteCompleted(String redirectTo, Exception error);
    }*/

    public abstract void openFile(long maxObjNoOnDisk, OpenFileCallback callback);

    /**
     * @return the cellId
     */
    public ASCIIString getCellId() {
        return cellId;
    }

    public static interface OpenFileCallback {
        public void fileOpened(boolean locallyReadable, boolean locallyWritable, Exception error);
    }

    public abstract void prepareOperation(long objNo, long objVersion, RWReplicationStage.Operation operation, PrepareOperationCallback callback);

    public static interface PrepareOperationCallback {
        public void prepareOperationComplete(boolean canExecOperation, String redirectToUUID, long newObjVersion, Exception error);
    }

    /**
     * create and execute the update RPCs to be sent to the other OSDs
     * @param credentials file credentials to contact OSDs, not used for XLoc!
     * @param objNo object number
     * @param objVersion new object version to write
     * @param data the object data
     * @return a list of RPCResponses, can be null if no update was create (e.g. cached locally)
     */
    public abstract void writeUpdate(FileCredentials credentials, long objNo, long objVersion, ObjectData data,
            ReplicatedOperationCallback callback);

    public static interface ReplicatedOperationCallback {
        public void operationCompleted(RPCResponse[] responses, String redirectToUUID, Exception error);
    }

    /**
     * create and execute the update RPCs to be sent to the other OSDs
     * @param credentials file credentials to contact OSDs, not used for XLoc!
     * @param objNo object number
     * @param objVersion new object version to write
     * @param data the object data
     * @return a list of RPCResponses, can be null if no update was create (e.g. cached locally)
     */
    public abstract void truncateFile(FileCredentials credentials, long newFileSize, long newObjectVersion,
            ReplicatedOperationCallback callback);

    /**
     * returns true if enough write update RPCs have been successful
     * @param responses the RPC responses returned by writeUpdate
     * @throws IOException if one or more requests failed and the update in total failed
     * @return true, if the write was successully disseminated to the other OSDs
     */
    public abstract boolean       hasFinished(int numResponses) throws IOException;

}
