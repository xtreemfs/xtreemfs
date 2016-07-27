/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.uuids;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Arrays;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ASCIIString;

/**
 * Encapsules the UUID and InetSocketAddress for a service.
 * 
 * @author bjko
 */
public final class ServiceUUID implements Serializable, Comparable {
    
    private final String       uuid;
    
    private long               validUntil;
    
    private UUIDCacheEntry     cacheEntry;
    
    private final UUIDResolver nonSingleton;
    
    /**
     * Creates a new ServiceUUID.
     * 
     * @param uuid
     *            the uuid string
     */
    public ServiceUUID(String uuid) {
        this.uuid = uuid;
        this.validUntil = 0;
        this.nonSingleton = null;
    }
    
    /**
     * Creates a new ServiceUUID with an individual UUIDresolver (rather than
     * the global instance)
     * 
     * @param uuid
     *            the uuid string
     */
    public ServiceUUID(String uuid, UUIDResolver nonSingleton) {
        this.uuid = uuid;
        this.validUntil = 0;
        this.nonSingleton = nonSingleton;
    }
    
    /**
     * Creates a new ServiceUUID.
     * 
     * @param uuid
     *            the uuid string.
     */
    public ServiceUUID(ASCIIString uuid) {
        this(uuid.toString());
        
    }
    
    /**
     * Resolves the uuid to a InetSocketAddress and protocol.
     * 
     * @throws org.xtreemfs.common.uuids.UnknownUUIDException
     *             if the uuid cannot be resolved (not local, no mapping on
     *             DIR).
     */
    public void resolve() throws UnknownUUIDException {
        resolve(null);
    }

    public void resolve(String protocol) throws UnknownUUIDException {
        updateMe(protocol);
    }
    
    /**
     * Retrieves the InetSocketAddress for the service.
     * 
     * @return the InetSocketAddress of the service
     * @throws org.xtreemfs.common.uuids.UnknownUUIDException
     *             if the UUID cannot be resolved
     */
    public Mapping[] getMappings() throws UnknownUUIDException {
        if (validUntil > TimeSync.getLocalSystemTime()) {
            cacheEntry.setLastAccess(TimeSync.getLocalSystemTime());
        } else {
            updateMe();
        }
        
        return cacheEntry.getMappings();
    }
    
    /**
     * Returns the first socket address from the list of mappings.
     * 
     * @return the socket address associated with the first list entry
     * @throws org.xtreemfs.common.uuids.UnknownUUIDException
     *             if the UUID cannot be resolved
     */
    public InetSocketAddress getAddress() throws UnknownUUIDException {
        return getMappings()[0].resolvedAddr;
    }
    
    /**
     * Returns the address as stored on the DIR.
     * 
     * @return String of the form "host:port".
     * @throws org.xtreemfs.common.uuids.UnknownUUIDException
     *             if the UUID cannot be resolved
     */
    public String getAddressString() throws UnknownUUIDException {
        return getMappings()[0].address;
    }
    
    /**
     * Returns the full URl of the service.
     * 
     * @return the URL of the service
     * @throws org.xtreemfs.common.uuids.UnknownUUIDException
     *             if the UUID cannot be resolved
     */
    public String toURL() throws UnknownUUIDException {
        if (validUntil > TimeSync.getLocalSystemTime()) {
            cacheEntry.setLastAccess(TimeSync.getLocalSystemTime());
        } else {
            updateMe();
        }
        return cacheEntry.toString();
    }
    
    /**
     * Get a details of the UUID mapping.
     * 
     * @return details of the UUID mapping.
     */
    public String debugString() {
        String mappingsStr = cacheEntry == null ? "" : Arrays.toString(cacheEntry.getMappings());
        return this.uuid + " -> " + mappingsStr + " (still valid for "
            + ((validUntil - TimeSync.getLocalSystemTime()) / 1000) + "s)";
    }
    
    /**
     * return the UUID string
     * 
     * @return UUID string
     */
    public String toString() {
        return this.uuid;
    }
    
    @Override
    public boolean equals(Object other) {
        try {
            final ServiceUUID o = (ServiceUUID) other;
            return this.uuid.equals(o.uuid);
        } catch (ClassCastException ex) {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
    
    /**
     * updates the UUID mapping via UUIDResolver
     * 
     * @throws org.xtreemfs.common.uuids.UnknownUUIDException
     */
    private void updateMe() throws UnknownUUIDException {
        updateMe(null);
    }

    private void updateMe(String protocol) throws UnknownUUIDException {
        if (nonSingleton == null) {
            cacheEntry = UUIDResolver.resolve(this.uuid, protocol);
        } else {
            cacheEntry = UUIDResolver.resolve(this.uuid, protocol, nonSingleton);
        }
        
        this.validUntil = cacheEntry.getValidUntil();
    }
    
    @Override
    public int compareTo(Object o) {
        return uuid.compareTo((String) o);
    }
}
