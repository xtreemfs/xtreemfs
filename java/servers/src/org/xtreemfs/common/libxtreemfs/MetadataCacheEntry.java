/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrResponse;


/**
 *
 * Represents an entry in the metadatacache.
 */
public class MetadataCacheEntry {

    private long statTimeoutS;
    
    private long dirEntriesTimeoutS;
    
    private long xattrTimeoutS;

    private long timeoutS;
    
    private Stat stat;
    
    private DirectoryEntries dirEntries;
    
    private String path;
    
    private listxattrResponse xattrs;
    
    
    protected Stat getStat() {
        return stat;
    }
    
    protected long getStatTimeoutS() {
        return statTimeoutS;
    }
    
    protected void setStatTimeoutS(long timeout) {
            this.statTimeoutS = timeout;
    }
    
    protected long getTimeoutS() {
        return timeoutS;
    }
    
    protected void setTimeoutS(long timeout) {
        this.timeoutS = timeout;
    }
    
    protected void setStat(Stat stat) {
        this.stat = stat;
    }
    
    protected DirectoryEntries getDirectoryEntries() {
        return dirEntries;
    }
    
    protected void setDirectoryEntries(DirectoryEntries dirEntries) {
        this.dirEntries = dirEntries;
    }
    
    protected long getDirectoryEntriesTimeoutS() {
        return dirEntriesTimeoutS;
    }
    
    protected void setDirectoryEntriesTimeoutS(long timeout) {
        this.dirEntriesTimeoutS = timeout;
    }
    
    protected void setPath(String path) {
        this.path = path;
    }
    
    protected String getPath() {
        return path;
    }
    
    protected listxattrResponse getXattrs() {
        return xattrs;
    }
    
    protected void setXattrs(listxattrResponse xattrs) {
        this.xattrs = xattrs;
    }
    
    protected long getXattrTimeoutS() {
        return xattrTimeoutS;
    }
    
    protected void setXattrTimeoutS(long timeout) {
        this.xattrTimeoutS = timeout;
    }
}
