/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
package org.xtreemfs.foundation.oncrpc.server;

import java.net.SocketAddress;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.foundation.pinky.channels.ChannelIO;
import org.xtreemfs.interfaces.Exceptions.ProtocolException;
import org.xtreemfs.interfaces.Exceptions.errnoException;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.interfaces.utils.Serializable;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.mrc.ErrNo;

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

    public void sendGarbageArgs(String message) {
        assert (responseHeader == null) : "response already sent";
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_GARBAGE_ARGS);
        sendException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_GARBAGE_ARGS,ErrNo.EINVAL, message));
    }
    
    public void sendInternalServerError(Throwable rootCause) {
        assert (responseHeader == null) : "response already sent";
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_SYSTEM_ERR);
        final String strace = OutputUtils.stackTraceToString(rootCause);
        sendException(new errnoException(0, "internal server error caused by: "+rootCause, strace));
    }

    public void sendProtocolException(ProtocolException exception) {
        assert (responseHeader == null) : "response already sent";
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                exception.getAccept_stat());
        sendException(exception);
    }

    public void sendGenericException(ONCRPCException exception) {
        assert (responseHeader == null) : "response already sent";
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_SYSTEM_ERR);
        sendException(exception);
    }

    public void sendResponse(ReusableBuffer serializedResponse) {
        ONCRPCBufferWriter writer = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS);

        responseHeader.serialize(writer);
        writer.put(serializedResponse);
        writer.flip();
        record.setResponseBuffers(writer.getBuffers());
        record.sendResponse();
    }
    

    void sendException(Serializable exception) {
        ONCRPCBufferWriter writer = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        responseHeader.serialize(writer);
        if (exception != null) {
            final byte[] exName = exception.getTypeName().getBytes();
            writer.putInt(exName.length);
            writer.put(exName);
            if (exName.length % 4 > 0) {
                for (int i = 0; i < ( 4 - exName.length%4); i++)
                    writer.put((byte)0);
            }
            exception.serialize(writer);
        }
        //make ready for sending
        writer.flip();
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

    public UserCredentials getUserCredentials() {
        return requestHeader.getUser_credentials();
    }

    public String toString() {
        return this.requestHeader+"/"+this.responseHeader;
    }
    
    public SocketAddress getClientIdentity() {
        return record.getConnection().getClientAddress();
    }

    public ChannelIO getChannel() {
        return record.getConnection().getChannel();
    }
    
}
