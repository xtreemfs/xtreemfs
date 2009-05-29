/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

package org.xtreemfs.common.uuids;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Arrays;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.ASCIIString;

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
        updateMe();
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
        if (nonSingleton == null) {
            cacheEntry = UUIDResolver.resolve(this.uuid);
        } else {
            cacheEntry = UUIDResolver.resolve(this.uuid, nonSingleton);
        }
        
        this.validUntil = cacheEntry.getValidUntil();
    }
    
    @Override
    public int compareTo(Object o) {
        return uuid.compareTo((String) o);
    }
}
