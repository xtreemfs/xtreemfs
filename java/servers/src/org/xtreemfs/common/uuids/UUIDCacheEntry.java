/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.uuids;


import org.xtreemfs.foundation.TimeSync;

/**
 * Cache entry for the UUIDResolver.
 * 
 * @author bjko
 */
class UUIDCacheEntry {
    
    private String    uuid;
    
    private Mapping[] mappings;
    
    private long      validUntil;
    
    private long      lastAccess;
    
    private boolean   sticky;
    
    public UUIDCacheEntry(String uuid, long validUntil, Mapping... mappings) {
        this.uuid = uuid;
        this.mappings = mappings;
        this.validUntil = validUntil;
        this.lastAccess = TimeSync.getLocalSystemTime();
    }
    
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public Mapping[] getMappings() {
        return mappings;
    }
    
    public void setMappings(Mapping... mappings) {
        this.mappings = mappings;
    }
    
    public void addMapping(Mapping mapping) {
        Mapping[] tmp = this.mappings;
        this.mappings = new Mapping[tmp.length + 1];
        System.arraycopy(tmp, 0, this.mappings, 0, tmp.length);
        this.mappings[this.mappings.length - 1] = mapping;
    }
    
    public long getValidUntil() {
        return validUntil;
    }
    
    public void setValidUntil(long validUntil) {
        this.validUntil = validUntil;
    }
    
    public long getLastAccess() {
        return lastAccess;
    }
    
    public void setLastAccess(long lastAccess) {
        this.lastAccess = lastAccess;
    }
    
    public boolean isSticky() {
        return sticky;
    }
    
    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Mapping mapping : mappings) {
            sb.append(mapping.protocol);
            sb.append("://");
            sb.append(mapping.resolvedAddr.toString());
            sb.append(",");
        }
        return sb.toString();
    }
}
