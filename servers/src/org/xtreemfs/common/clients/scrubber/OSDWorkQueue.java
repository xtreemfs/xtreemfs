package org.xtreemfs.common.clients.scrubber;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.RPCResponseListener;
import org.xtreemfs.common.clients.osd.OSDClient;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.foundation.speedy.MultiSpeedy;

/**
 * The work queue for an OSD.
 * 
 * Asynchronously sends off checkObject requests with one of its MultiSpeedies.
 * Dispatches "finished" callback to respective file.
 */
public class OSDWorkQueue implements RPCResponseListener {
    
    private AtomicLong        transferredBytes  = new AtomicLong();
    
    private final ServiceUUID id;
    
    private MultiSpeedy[]     connections;
    
    private OSDClient[]       clients;
    
    private boolean[]         isIdle;
                                                                    
    private AtomicInteger     noOfIdleConnections;
    
    private int               noOfConnections;
    
    public OSDWorkQueue(ServiceUUID id, int noConnections, SSLOptions ssl) throws IOException {
        this.id = id;
        connections = new MultiSpeedy[noConnections];
        clients = new OSDClient[noConnections];
        isIdle = new boolean[noConnections];
        noOfIdleConnections = new AtomicInteger(0);
        this.noOfConnections = noConnections;
        
        for (int i = 0; i < noConnections; i++) {
            MultiSpeedy speedy = null;
            if (ssl == null)
                speedy = new MultiSpeedy();
            else
                speedy = new MultiSpeedy(ssl);
            speedy.start();
            connections[i] = speedy;
            clients[i] = new OSDClient(speedy);
            isIdle[i] = true;
        }
        noOfIdleConnections.set(noConnections);
    }
    
    ServiceUUID getOSDId() {
        return id;
    }
    
    int getNumberOfIdleConnections() {
        return noOfIdleConnections.get();
    }
    
    int getTotalNumberOfConnections() {
        return noOfConnections;
    }
    
    /**
     * @return the total amount of bytes transferred by the OSD
     */
    public long getTransferredBytes() {
        return transferredBytes.get();
    }
    
    /**
     * Called by the Main thread Reads an object given by the parameters file
     * and objectNo asynchronously, if the file not marked as unreadable and an
     * idle connection exists.
     * 
     * @return returns false if there is no idle connection.
     */
    public boolean readObjectAsync(ScrubbedFile file, int objectNo) {
        for (int i = 0; i < clients.length; i++) {
            if (isIdle[i]) {
                // submitrequest
                isIdle[i] = false;
                noOfIdleConnections.decrementAndGet();
                file.markObjectAsInFlight(objectNo);
                file.readObjectAsync(clients[i], this, new ReadObjectContext(i, file, objectNo,
                    TimeSync.getLocalSystemTime()), objectNo);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Called by MultiSpeedy or Main thread.
     * 
     */
    public void responseAvailable(RPCResponse response) {
        ReadObjectContext context = (ReadObjectContext) response.getAttachment();
        
        // unsynchronized access to shared variable!
        // ok here as only connection might be unused for another round
        isIdle[context.connectionNo] = true;
        noOfIdleConnections.incrementAndGet(); // atomic!
        try {
            if (response.getStatusCode() == 200) {// no error occurred
                ReusableBuffer data = response.getBody();
                if (data != null) {// read was successful
                    data.flip();
                    String tmp = new String(data.array());
                    long bytesInObject = Long.valueOf(tmp);

                    transferredBytes.addAndGet(bytesInObject);
                    context.file.objectHasBeenRead(bytesInObject, context.objectNo);
                } else
                    context.file.objectHasBeenRead(0, context.objectNo);
                
                String header = response.getHeaders().getHeader(HTTPHeaders.HDR_XINVALIDCHECKSUM);
                if (header != null && header.equalsIgnoreCase("true"))
                    context.file.objectHasInvalidChecksum(context.objectNo);
                // throw new IOException("object " + context.objectNo +
                // " has an invalid checksum");
                // TODO: dont throw, but call method as in objectHasNotBeenRead
            } else {
                context.file.couldNotReadObject(context.objectNo);
            }
            
        } catch (Exception e) {
            context.file.couldNotReadObject(context.objectNo);
        } finally {
            response.freeBuffers();
        }
    }
    
    void shutDown() {
        for (int i = 0; i < connections.length; i++) {
            connections[i].shutdown();
        }
    }
}