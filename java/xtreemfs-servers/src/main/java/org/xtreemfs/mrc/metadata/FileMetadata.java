/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.metadata;

public interface FileMetadata {
    
    /** frequently-changed metadata */
    public static final byte FC_METADATA   = 0;
    
    /** rarely-changed metadata */
    public static final byte RC_METADATA   = 1;
    
    public long getId();
    
    public void setId(long id);
    
    public int getAtime();
    
    public void setAtime(int atime);
    
    public int getCtime();
    
    public void setCtime(int ctime);
    
    public int getMtime();
    
    public void setMtime(int mtime);
    
    public long getSize();
    
    public void setSize(long size);
    
    public int getPerms();
    
    public void setPerms(int perms);
    
    public short getLinkCount();
    
    public void setLinkCount(short linkCount);
    
    public int getEpoch();
    
    public void setEpoch(int epoch);
    
    public int getIssuedEpoch();
    
    public void setIssuedEpoch(int issuedEpoch);
    
    public boolean isReadOnly();
    
    public void setReadOnly(boolean readOnly);
    
    public XLocList getXLocList();
    
    public void setXLocList(XLocList xLocList);
    
    public String getFileName();
    
    public String getOwnerId();
    
    public String getOwningGroupId();
    
    public void setOwnerAndGroup(String owner, String group);
    
    public void setW32Attrs(long w32Attrs);
    
    public long getW32Attrs();
    
    public boolean isDirectory();
    
}
