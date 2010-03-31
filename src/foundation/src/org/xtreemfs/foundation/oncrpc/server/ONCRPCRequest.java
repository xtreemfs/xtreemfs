/*
 * Copyright (c) 2009-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: Bjoern Kolbeck (ZIB)
 */
package org.xtreemfs.foundation.oncrpc.server;

import java.net.SocketAddress;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.oncrpc.channels.ChannelIO;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.interfaces.utils.ONCRPCRecordFragmentHeader;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;

/**
 *
 * @author bjko
 */
public class ONCRPCRequest {

    private final ONCRPCRecord record;

    private final ONCRPCRequestHeader requestHeader;

    private ONCRPCResponseHeader      responseHeader;

    public ONCRPCRequest(ONCRPCRecord record, yidl.runtime.Object user_credentials) {
        this.record = record;

        requestHeader = new ONCRPCRequestHeader(user_credentials);

        final ReusableBuffer firstFragment = record.getRequestFragments().get(0);
        firstFragment.position(0);
        requestHeader.unmarshal(new XDRUnmarshaller(firstFragment));
    }

    public ReusableBuffer getRequestFragment() {
        return record.getRequestFragments().get(0);
    }

    public void sendResponse(yidl.runtime.Object response) {
        assert (responseHeader == null) : "response already sent";
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "response %s sent to client %s",
                response.getTypeName(), this.record.getConnection().getClientAddress().toString());
        }
        serializeAndSendRespondse(response);
    }

    /*public void sendInternalServerError(Throwable rootCause, org.xtreemfs.interfaces.OSDInterface.errnoException ex) {
        final String strace = OutputUtils.stackTraceToString(rootCause);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                "errnoException: SYSTEM ERR/Internal Server Error sent to client %s", this.record
                        .getConnection().getClientAddress().toString());
        }
        ex.setError_code(ErrNo.EIO);
        ex.setError_message("internal server error caused by: "+rootCause);
        ex.setStack_trace(strace);
        wrapAndSendException(ex);
    }

    public void sendInternalServerError(Throwable rootCause, org.xtreemfs.interfaces.MRCInterface.errnoException ex) {
        final String strace = OutputUtils.stackTraceToString(rootCause);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this,
                "errnoException: SYSTEM ERR/Internal Server Error sent to client %s", this.record
                        .getConnection().getClientAddress().toString());
        }
        ex.setError_code(ErrNo.EIO);
        ex.setError_message("internal server error caused by: "+rootCause);
        ex.setStack_trace(strace);
        wrapAndSendException(ex);
    }*/

    public void sendException(ONCRPCException exception) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "%s sent to client %s", exception
                    .toString(), this.record.getConnection().getClientAddress().toString());
        }
        wrapAndSendException(exception);
    }

    public void sendResponse(ReusableBuffer serializedResponse) {
        ONCRPCBufferWriter writer = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
                ONCRPCResponseHeader.ACCEPT_STAT_SUCCESS);

        final int fragmentSize = serializedResponse.capacity()+responseHeader.getXDRSize();
        assert (fragmentSize >= 0) : "fragment has invalid size: "+fragmentSize;
        final boolean isLastFragment = true;
        final int fragHdr = ONCRPCRecordFragmentHeader.getFragmentHeader(fragmentSize, isLastFragment);
        writer.writeInt32(null,fragHdr);
        responseHeader.marshal(writer);
        writer.put(serializedResponse);
        writer.flip();
        record.setResponseBuffers(writer.getBuffers());
        record.sendResponse();
    }

    public void sendProcUnavail() {
        sendErrorCode(ONCRPCResponseHeader.ACCEPT_STAT_PROC_UNAVAIL);
    }

    public void sendProgMismatch() {
        sendErrorCode(ONCRPCResponseHeader.ACCEPT_STAT_PROG_MISMATCH);
    }

    public void sendErrorCode(int errorCode) {
        ONCRPCBufferWriter writer = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);

        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
            errorCode);

        final int fragmentSize =  responseHeader.getXDRSize();
        assert (fragmentSize >= 0) : "fragment has invalid size: "+fragmentSize;
        final boolean isLastFragment = true;
        final int fragHdr = ONCRPCRecordFragmentHeader.getFragmentHeader(fragmentSize, isLastFragment);
        writer.writeInt32(null,fragHdr);
        responseHeader.marshal(writer);;
        //make ready for sending
        writer.flip();
        record.setResponseBuffers(writer.getBuffers());

        record.sendResponse();
    }
    

    void wrapAndSendException(yidl.runtime.Object exception) {
        assert(exception != null);
        assert (responseHeader == null) : "response already sent";
        ONCRPCBufferWriter writer = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        
        responseHeader = new ONCRPCResponseHeader(requestHeader.getXID(), ONCRPCResponseHeader.REPLY_STAT_MSG_ACCEPTED,
            exception.getTag());

        final int fragmentSize =  responseHeader.getXDRSize()+exception.getXDRSize();
        assert (fragmentSize >= 0) : "fragment has invalid size: "+fragmentSize;
        final boolean isLastFragment = true;
        final int fragHdr = ONCRPCRecordFragmentHeader.getFragmentHeader(fragmentSize, isLastFragment);
        writer.writeInt32(null,fragHdr);
        responseHeader.marshal(writer);
        exception.marshal(writer);
        //make ready for sending
        writer.flip();
        record.setResponseBuffers(writer.getBuffers());

        record.sendResponse();
    }

    void serializeAndSendRespondse(yidl.runtime.Object response) {
        final int fragmentSize = response.getXDRSize()+responseHeader.getXDRSize();
        final boolean isLastFragment = true;
        assert (fragmentSize >= 0) : "fragment has invalid size: "+fragmentSize;
        final int fragHdr = ONCRPCRecordFragmentHeader.getFragmentHeader(fragmentSize, isLastFragment);
        ONCRPCBufferWriter writer = new ONCRPCBufferWriter(ONCRPCBufferWriter.BUFF_SIZE);
        writer.writeInt32(null,fragHdr);
        responseHeader.marshal(writer);
        if (response != null)
            response.marshal(writer);
        //make ready for sending
        writer.flip();
        record.setResponseBuffers(writer.getBuffers());
        assert (record.getResponseSize() == fragmentSize+4) : "wrong fragSize: "+record.getResponseSize() +" vs. "+ fragmentSize+", response is: "+response;
        record.sendResponse();
    }


    public ONCRPCRequestHeader getRequestHeader() {
        return this.requestHeader;
    }

    public yidl.runtime.Object getUserCredentials() {
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

    public void sendGarbageArgs() {
        sendErrorCode(ONCRPCResponseHeader.ACCEPT_STAT_GARBAGE_ARGS);
    }
    
}
