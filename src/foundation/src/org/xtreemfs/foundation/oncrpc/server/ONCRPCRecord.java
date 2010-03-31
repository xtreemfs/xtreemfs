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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;

/**
 *
 * @author bjko
 */
public class ONCRPCRecord {

    /**
     * The client which sent the request
     */
    private final ClientConnection connection;

    /**
     * Server the request was received by
     */
    private final RPCNIOSocketServer server;

    /**
     * request fragments
     */
    private final ArrayList<ReusableBuffer>  requestFragments;

    /**
     * set to true after the last fragment was received and added
     * to requestFragments
     */
    private boolean allFragmentsReceived;

    /**
     * list of buffer that make up the response
     */
    private List<ReusableBuffer>  responseBuffers;

    private ByteBuffer[]          responseSendBuffers;

    /**
     * fragment which is currently sent
     */
    private int bufferSendCount;


    /**
     * Creates a new record
     * @param server the server which received the request
     * @param connection the client which sent the request
     */
    public ONCRPCRecord(RPCNIOSocketServer server, ClientConnection connection) {
        this.server = server;
        this.connection = connection;
        this.requestFragments = new ArrayList<ReusableBuffer>(RPCNIOSocketServer.MAX_FRAGMENTS);
    }

    /**
     * add a new request fragment
     * @param fragment
     */
    void addNewRequestFragment(ReusableBuffer fragment) {
        requestFragments.add(fragment);
    }

    /**
     * @return the last request fragment
     */
    ReusableBuffer getLastRequestFragment() {
        return requestFragments.get(requestFragments.size()-1);
    }

    /**
     *
     * @return a list with all request fragments sent by the client
     */
    public List<ReusableBuffer> getRequestFragments() {
        return this.requestFragments;
    }

    /**
     * Adds a new buffer which has to be sent to the client as a response
     * @param fragment the fragment to add
     */
    public void addResponseBuffer(ReusableBuffer buffer) {
        responseBuffers.add(buffer);
    }

    /**
     *
     * @return the current response fragment
     */
    ReusableBuffer getCurrentResponseBuffer() {
        return responseBuffers.get(bufferSendCount);
    }

    ByteBuffer[] getResponseSendBuffers() {
        if (this.responseSendBuffers == null) {
            final int numBuffers = responseBuffers.size();
            responseSendBuffers = new ByteBuffer[numBuffers];
            for (int i = 0; i < numBuffers; i++) {
                responseSendBuffers[i] = responseBuffers.get(i).getBuffer();
            }
        }
        return responseSendBuffers;
    }

    boolean responseComplete() {
        return !responseSendBuffers[responseSendBuffers.length-1].hasRemaining();
    }

    int getResponseSize() {
        int size = 0;
        for (ReusableBuffer buf : responseBuffers) {
            size += buf.remaining();
        }
        return size;
    }

    /**
     *
     * @return true, if the current is also the last response fragment
     */
    boolean isLastResoponseBuffer() {
        return bufferSendCount == responseBuffers.size()-1;
    }

    /**
     * Iterates to the next response fragment, if the current one if not
     * the last one
     */
    void nextResponseBuffer() {
        assert(bufferSendCount < responseBuffers.size());
        bufferSendCount++;
    }


    /**
     * Send the response fragments back to the client.
     */
    public void sendResponse() {
        server.sendResponse(this);
    }

    public String toString() {
        return "ONC RPC Record "+
                ", # request fragments: "+requestFragments.size()+", "+
                ", # response fragments: "+requestFragments.size();
    }

    ClientConnection getConnection() {
        return this.connection;
    }

    /**
     * Free all request buffers.
     */
    public void freeRequestBuffers() {
        for (ReusableBuffer fragment : requestFragments) {
            BufferPool.free(fragment);
        }
        requestFragments.clear();
    }

    /**
     * Free all response fragment buffers
     */
    public void freeResponseBuffers() {
        if (responseBuffers != null) {
            for (ReusableBuffer fragment : responseBuffers) {
                BufferPool.free(fragment);
            }
            responseBuffers.clear();
        }
    }

    /**
     * Free all buffers. Called by the RPCServer after sending out the last
     * response fragment.
     */
    public void freeBuffers() {
        freeRequestBuffers();
        freeResponseBuffers();
    }

    /**
     * @return the allFragmentsReceived
     */
    boolean isAllFragmentsReceived() {
        return allFragmentsReceived;
    }

    /**
     * @param allFragmentsReceived the allFragmentsReceived to set
     */
    void setAllFragmentsReceived(boolean allFragmentsReceived) {
        this.allFragmentsReceived = allFragmentsReceived;
    }

    /**
     * @return the fragmentSendCount
     */
    int getResponseBufferSendCount() {
        return bufferSendCount;
    }

    /**
     * @param fragmentSendCount the fragmentSendCount to set
     */
    void setResponseBufferSendCount(int bufferSendCount) {
        this.bufferSendCount = bufferSendCount;
    }

    List<ReusableBuffer> getResponseBuffers() {
        return this.responseBuffers;
    }

    void setResponseBuffers(List<ReusableBuffer> responseBuffers) {
        this.responseBuffers = responseBuffers;
    }

}
