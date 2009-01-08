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
 * AUTHORS: Jan Stender (ZIB)
 */
package org.xtreemfs.new_mrc.metadata;

public interface FileMetadata {
    
    /** frequently-changed metadata */
    public static final byte             FC_METADATA   = 0;
    
    /** rarely-changed metadata */
    public static final byte             RC_METADATA   = 1;
    
    /** X-Locations list metadata */
    public static final byte             XLOC_METADATA = 2;
    
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
    
    public short getPerms();
    
    public void setPerms(short perms);
    
    public short getLinkCount();
    
    public void setLinkCount(short linkCount);
    
    public int getEpoch();
    
    public void setEpoch(int epoch);
    
    public int getIssuedEpoch();
    
    public void setIssuedEpoch(int issuedEpoch);
    
    public boolean isReadOnly();
    
    public void setReadOnly(boolean readOnly);
    
    public String getFileName();
    
    public String getOwnerId();
    
    public String getOwningGroupId();
    
    public boolean isDirectory();
    
}
