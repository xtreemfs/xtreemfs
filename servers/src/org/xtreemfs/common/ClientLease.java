/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 *
 * @author bjko
 */
public final class ClientLease implements Cloneable {

    /**
     * Default time span for the client lease validity.
     * Must be smaller than a intra-OSD lease, if replication is
     * active!
     */
    public static final long LEASE_VALIDITY = 15000;
    
    /**
     * Indicates that a lease spans to EOF "append lease".
     * a lease from 0 to -1 spans the whole file, even if data is appended.
     */
    public static final long TO_EOF = -1;
    
    /**
     * timestamp when the lease expires
     */
    private long firstObject;
    /**
     * last object the lease is valid for
     */
    private long lastObject;

     /**
     * UUID of the client owning the lease
     */
    private String clientId;

    /**
     * timestamp when the lease expires (in seconds since 01/01/70)
     * must be XtreemFS global time!
     */
    private long expires;

    
    /**
     * fileId this lease was issued for
     */
    private final String fileId;
    
    /**
     * sequenceNo, used to generate unique leaseId = fileId+"/"+sequenceNo
     */
    private long  sequenceNo;
    
    /**
     * lease type/operation
     */
    private String operation;
    
    public static final String EXCLUSIVE_LEASE = "w";
    
    
    public ClientLease(final String fileId) {
        this.fileId = fileId;
    }
    
    
    /**
     * Checks if two leases have conflicting (i.e. overlapping ranges)
     * @param other other lease for the same file
     * @return true, if there is an overlap in the ranges
     */
    public boolean isConflicting(ClientLease other) {
        //checks
        if ( ((this.lastObject < other.firstObject) && (this.lastObject != TO_EOF)) ||
             ((other.lastObject < this.firstObject) && (other.lastObject != TO_EOF)) ) {
            return false;
        } else {
            return true;
        }
    }
    
    @Override
    public ClientLease clone() {
        ClientLease l = new ClientLease(this.fileId);
        l.clientId = this.clientId;
        l.expires = this.expires;
        l.firstObject = this.firstObject;
        l.lastObject = this.lastObject;
        l.operation = this.operation;
        l.sequenceNo = this.sequenceNo;
        return l;
    }
    
    public long getFirstObject() {
        return firstObject;
    }

    public void setFirstObject(long firstObject) {
        this.firstObject = firstObject;
    }

    public long getLastObject() {
        return lastObject;
    }

    public void setLastObject(long lastObject) {
        this.lastObject = lastObject;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getExpires() {
        return expires;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    public String getFileId() {
        return fileId;
    }

    public long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
    
       
}
