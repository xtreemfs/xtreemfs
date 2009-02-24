/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.new_dir;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.interfaces.utils.Serializable;

/**
 *
 * @author bjko
 */
public class DIRRequest {

    private final ONCRPCRequest rpcRequest;

    private Serializable        requestMessage;

    public DIRRequest(ONCRPCRequest rpcRequest) {
        this.rpcRequest = rpcRequest;
    }

    public void deserializeMessage(Serializable message) {
        final ReusableBuffer payload = rpcRequest.getRequestFragment();
        message.deserialize(payload);
        requestMessage = message;
    }

    public Serializable getRequestMessage() {
        return requestMessage;
    }

    public void sendSuccess(Serializable response) {
        rpcRequest.sendResponse(response);
    }

    public void sendInternalServerError() {
    }

    public void sendException(Serializable exception) {
        rpcRequest.sendGenericException(exception);
    }


}
