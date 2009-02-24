/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.foundation.oncrpc.server;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.interfaces.Exceptions.ProtocolException;
import org.xtreemfs.interfaces.Exceptions.errnoException;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.interfaces.utils.Serializable;

/**
 *
 * @author bjko
 */
public class ONCRPCRequest {

    private final ONCRPCRecord record;

    private final ONCRPCRequestHeader requestHeader;

    private ONCRPCResponseHeader      responseHeader;

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

    public void sendGarbageArgs(errnoException exception) {
        assert (responseHeader == null) : "response already sent";
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_GARBAGE_ARGS);
        serializeAndSendRespondse(exception);
    }
    
    public void sendInternalServerError(errnoException exception) {
        assert (responseHeader == null) : "response already sent";
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_SYSTEM_ERR);
        serializeAndSendRespondse(exception);
    }

    public void sendProtocolException(ProtocolException exception) {
        assert (responseHeader == null) : "response already sent";
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                exception.getAccept_stat());
        serializeAndSendRespondse(exception);
    }

    public void sendResponse(ReusableBuffer serializedResponse) {
        ONCRPCBufferWriter writer = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS);

        responseHeader.serialize(writer);
        writer.put(serializedResponse);
        writer.flip();
        System.out.println(writer);
        record.setResponseBuffers(writer.getBuffers());
        record.sendResponse();
    }
    

    void serializeAndSendRespondse(Serializable response) {
        ONCRPCBufferWriter writer = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        responseHeader.serialize(writer);
        if (response != null)
            response.serialize(writer);
        //make ready for sending
        writer.flip();
        record.setResponseBuffers(writer.getBuffers());
        record.sendResponse();
    }

    public ONCRPCRequestHeader getRequestHeader() {
        return this.requestHeader;
    }


}
