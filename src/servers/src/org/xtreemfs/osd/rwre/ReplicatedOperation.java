/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.rwre;

import java.net.InetSocketAddress;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.OSDInterface.RedirectException;
import org.xtreemfs.interfaces.ObjectData;

/**
 *
 * @author bjko
 */
public class ReplicatedOperation implements RPCResponseAvailableListener {


    private int                   responseCount;

    private boolean               errorSent;

    private final ReplicaUpdatePolicy   policy;

    private final RWReplicationStage.ReplicatedOperationCallback callback;


    public ReplicatedOperation(ReplicaUpdatePolicy policy,
            final RWReplicationStage.ReplicatedOperationCallback callback) {

        this.policy = policy;
        this.callback = callback;
        this.responseCount = 0;
        this.errorSent = false;
    }

    public void write(FileCredentials credentials, long objNo, long objVersion, ObjectData data) {

        policy.writeUpdate(credentials, objNo, objVersion, data, new ReplicaUpdatePolicy.ReplicatedOperationCallback() {

            @Override
            public void operationCompleted(RPCResponse[] responses, String redirectTo, Exception error) {
                if (error != null) {
                    System.out.println("write complete: "+error);
                    callback.writeCompleted(error);
                    return;
                }
                if (redirectTo != null) {
                    System.out.println("write complete: "+redirectTo);
                    callback.writeCompleted(new RedirectException(redirectTo));
                    return;
                }
                if (responses != null) {
                    for (RPCResponse r : responses) {
                        r.registerListener(ReplicatedOperation.this);
                    }
                } else {
                    System.out.println("write complete");
                    callback.writeCompleted(null);
                }
            }
        });
    }

    public void truncate(FileCredentials credentials, long newFileSize, long newObjectVersion) {

        policy.truncateFile(credentials, newFileSize, newObjectVersion, new ReplicaUpdatePolicy.ReplicatedOperationCallback() {

            @Override
            public void operationCompleted(RPCResponse[] responses, String redirectTo, Exception error) {
                if (error != null) {
                    System.out.println("write complete: "+error);
                    callback.writeCompleted(error);
                    return;
                }
                if (redirectTo != null) {
                    System.out.println("write complete: "+redirectTo);
                    callback.writeCompleted(new RedirectException(redirectTo));
                    return;
                }
                if (responses != null) {
                    for (RPCResponse r : responses) {
                        r.registerListener(ReplicatedOperation.this);
                    }
                } else {
                    System.out.println("write complete");
                    callback.writeCompleted(null);
                }
            }
        });
    }


    public void responseAvailable(RPCResponse r) {

        try {
            responseCount++;
            r.get();
            if (policy.hasFinished(responseCount)) {
                System.out.println("write complete");
                callback.writeCompleted(null);
            }
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_WARN, this,"error in replicated op: "+ex);
            if (!errorSent) {
                errorSent = true;
                callback.writeCompleted(ex);
            }
        } finally {
            r.freeBuffers();
        }

    }



}
