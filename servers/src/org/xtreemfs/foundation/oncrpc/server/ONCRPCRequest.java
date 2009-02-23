/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.foundation.oncrpc.server;

import java.util.List;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.ONCRPCResponseHeader;
import org.xtreemfs.interfaces.Serializable;

/**
 *
 * @author bjko
 */
public class ONCRPCRequest {

    private final ONCRPCRecord record;

    private final ONCRPCRequestHeader requestHeader;

    private ONCRPCResponseHeader      responseHeader;

    public static enum RPCResultCode {
        SUCCESS,
        INTERNAL_SERVER_ERROR,
        SYSTEM_ERROR
    };

    public ONCRPCRequest(ONCRPCRecord record) {
        this.record = record;

        requestHeader = new ONCRPCRequestHeader();

        final ReusableBuffer firstFragment = record.getRequestFragments().get(0);
        firstFragment.position(0);
        requestHeader.deserialize(firstFragment);
    }

    public ReusableBuffer getRequestFragment() {
        return record.getRequestFragments().get(0);
    }

    public void sendResponse(Serializable response) {
        assert (responseHeader == null) : "response already sent";
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS);
        serializeAndSendRespondse(response);
    }

    public void sendError(Serializable response) {
        assert (responseHeader == null) : "response already sent";
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_SYSTEM_ERR);
        serializeAndSendRespondse(response);
    }

    public void sendGarbageArgsError() {
        assert (responseHeader == null) : "response already sent";
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_GARBAGE_ARGS);
        serializeAndSendRespondse(null);
    }

    public void sendResponse(ReusableBuffer serializedResponse) {
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS);

        responseHeader.serialize(record.getResponseBuffers());
        record.addResponseBuffer(serializedResponse);
        record.sendResponse();
    }
    

    void serializeAndSendRespondse(Serializable response) {
        responseHeader.serialize(record.getResponseBuffers());
        if (response != null)
            response.serialize(record.getResponseBuffers());
        record.sendResponse();
    }


}
