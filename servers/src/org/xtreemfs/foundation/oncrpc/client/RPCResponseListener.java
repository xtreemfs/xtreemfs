/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.foundation.oncrpc.client;


/**
 *
 * @author bjko
 */
public interface RPCResponseListener {

    public static enum Errors {
        SERVER_NOT_REACHABLE,
        CONNECTION_CLOSED,
        INVALID_DATA,
        CONNECTION_UNUSED,
        TIMEOUT,
        REMOTE_EXCEPTION
    };

    public void responseAvailable(RPCRequest request);

    public void requestFailed(RPCRequest request, Errors reason);

}
