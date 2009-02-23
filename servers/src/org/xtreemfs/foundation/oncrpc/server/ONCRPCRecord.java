/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.foundation.oncrpc.server;

import java.util.ArrayList;
import java.util.List;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.ONCRPCResponseHeader;

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
    private final ArrayList<ReusableBuffer>  responseBuffers;

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
        this.requestFragments = new ArrayList(RPCNIOSocketServer.MAX_FRAGMENTS);
        this.responseBuffers = new ArrayList(RPCNIOSocketServer.MAX_FRAGMENTS);
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

    int getResponseSize() {
        int size = 0;
        for (ReusableBuffer buf : responseBuffers) {
            size += buf.capacity();
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
        for (ReusableBuffer fragment : responseBuffers) {
            BufferPool.free(fragment);
        }
        responseBuffers.clear();
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

}
