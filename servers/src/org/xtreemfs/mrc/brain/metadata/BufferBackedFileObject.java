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

package org.xtreemfs.mrc.brain.metadata;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;

public class BufferBackedFileObject extends BufferBackedFSObject implements FileObject {
    
    private static final int     DYNAMIC_PART_INDEX = 39;
    
    private BufferBackedXLocList xLocList;
    
    public BufferBackedFileObject(ReusableBuffer buffer, boolean copy, boolean freeOnDestroy) {
        super(buffer, copy, freeOnDestroy);
    }
    
    public BufferBackedFileObject(long id, int atime, int ctime, int mtime, long size,
        short linkcount, int epoch, int issuedEpoch, boolean readonly, String ownerID,
        String owningGroupId, BufferBackedACL acl, BufferBackedXLocList xlocList,
        BufferBackedStripingPolicy sp, String linkTarget, BufferBackedXAttrs xattrs) {
        
        super(null, false, true);
        
        // allocate a new buffer from the pool
        buffer = BufferPool.allocate(DYNAMIC_PART_INDEX + ownerID.getBytes().length + 4
            + owningGroupId.getBytes().length + 4 + (xlocList == null ? 4 : xlocList.size() + 4)
            + (linkTarget == null ? 4 : linkTarget.getBytes().length + 4)
            + (acl == null ? 4 : acl.size() + 4) + (sp == null ? 4 : sp.size() + 4)
            + (xattrs == null ? 4 : xattrs.size()) + 4);
        
        // fill the buffer with the given data
        buffer.position(0);
        buffer.putLong(id);
        buffer.putInt(atime);
        buffer.putInt(ctime);
        buffer.putInt(mtime);
        buffer.putLong(size);
        buffer.putShort(linkcount);
        buffer.putInt(epoch);
        buffer.putInt(issuedEpoch);
        buffer.putBoolean(readonly);
        buffer.putString(ownerID);
        buffer.putString(owningGroupId);
        
        if (acl != null) {
            buffer.putInt(acl.size());
            acl.getBuffer().position(0);
            buffer.put(acl.getBuffer());
        } else {
            buffer.putInt(0);
        }
        
        if (xlocList != null) {
            buffer.putInt(xlocList.size());
            xlocList.getBuffer().position(0);
            buffer.put(xlocList.getBuffer());
        } else {
            buffer.putInt(0);
        }
        
        if (sp != null) {
            buffer.putInt(sp.size());
            sp.getBuffer().position(0);
            buffer.put(sp.getBuffer());
        } else {
            buffer.putInt(0);
        }
        
        buffer.putString(linkTarget);
        
        if (xattrs != null) {
            buffer.putInt(xattrs.size());
            xattrs.getBuffer().position(0);
            buffer.put(xattrs.getBuffer());
        } else {
            buffer.putInt(0);
        }
    }
    
    /**
     * Returns all internally allocated buffers.
     */
    public void destroy() {
        
        if (xLocList != null)
            xLocList.destroy();
        
        super.destroy();
    }
    
    public long getSize() {
        buffer.position(getFixedBufferIndex(SIZE));
        return buffer.getLong();
    }
    
    public void setSize(long size) {
        buffer.position(getFixedBufferIndex(SIZE));
        buffer.putLong(size);
    }
    
    public short getLinkCount() {
        buffer.position(getFixedBufferIndex(LINKCOUNT));
        return buffer.getShort();
    }
    
    public void setLinkCount(short count) {
        buffer.position(getFixedBufferIndex(LINKCOUNT));
        buffer.putShort(count);
    }
    
    public int getEpoch() {
        buffer.position(getFixedBufferIndex(EPOCH));
        return buffer.getInt();
    }
    
    public void setEpoch(int epoch) {
        buffer.position(getFixedBufferIndex(EPOCH));
        buffer.putInt(epoch);
    }
    
    public int getIssuedEpoch() {
        buffer.position(getFixedBufferIndex(ISSEPOCH));
        return buffer.getInt();
    }
    
    public void setIssuedEpoch(int epoch) {
        buffer.position(getFixedBufferIndex(ISSEPOCH));
        buffer.putInt(epoch);
    }
    
    public boolean isReadOnly() {
        buffer.position(getFixedBufferIndex(READONLY));
        return buffer.getBoolean();
    }
    
    public void setReadOnly(boolean readOnly) {
        buffer.position(getFixedBufferIndex(READONLY));
        buffer.putBoolean(readOnly);
    }
    
    public BufferBackedXLocList getXLocList() {
        
        if (xLocList == null) {
            ReusableBuffer buf = getAttrBuffer(getDynamicIndex(XLOC));
            if (buf != null)
                xLocList = new BufferBackedXLocList(buf, false, true);
        }
        
        return xLocList;
    }
    
    public void setXLocList(XLocList xlocList) {
        
        assert (xlocList instanceof BufferBackedXLocList);
        
        BufferBackedXLocList newXLoc = (BufferBackedXLocList) xlocList;
        BufferBackedXLocList currentXLoc = getXLocList();
        int offset = getDynamicBufferIndex(getDynamicIndex(XLOC));
        
        // remove the current entity
        delete(offset + 4, currentXLoc == null ? 0 : currentXLoc.size());
        this.xLocList.destroy();
        this.xLocList = null;
        
        // update the entity size
        buffer.position(offset);
        buffer.putInt(newXLoc.size());
        
        // insert the new entity
        insert(offset + 4, newXLoc.getBuffer());
    }
    
    protected int getFixedBufferIndex(int fixedAttrIndex) {
        
        switch (fixedAttrIndex) {
        case ID:
            return 0;
        case ATIME:
            return 8;
        case CTIME:
            return 12;
        case MTIME:
            return 16;
        case SIZE:
            return 20;
        case LINKCOUNT:
            return 28;
        case EPOCH:
            return 30;
        case ISSEPOCH:
            return 34;
        case READONLY:
            return 38;
        default:
            throw new IllegalArgumentException("invalid index: " + fixedAttrIndex);
        }
        
    }
    
    protected int getDynamicBufferStartIndex() {
        return DYNAMIC_PART_INDEX;
    }
    
    protected int getDynamicIndex(int attr) {
        switch (attr) {
        case OWNER:
            return 0;
        case GROUP:
            return 1;
        case ACL:
            return 2;
        case XLOC:
            return 3;
        case SP:
            return 4;
        case LINKTRG:
            return 5;
        case XATTRS:
            return 6;
        default:
            throw new IllegalArgumentException("invalid attribute: " + attr);
        }
    }
    
}
