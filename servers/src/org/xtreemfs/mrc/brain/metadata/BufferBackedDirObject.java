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

public class BufferBackedDirObject extends BufferBackedFSObject implements FSObject {
    
    private static final int DYNAMIC_PART_INDEX = 20;
    
    public BufferBackedDirObject(ReusableBuffer buffer, boolean copy, boolean freeOnDestroy) {
        super(buffer, copy, freeOnDestroy);
    }
    
    public BufferBackedDirObject(long id, int atime, int ctime, int mtime, String ownerID,
        String owningGroupId, BufferBackedACL acl, BufferBackedStripingPolicy defaultSP,
        String linkTarget, BufferBackedXAttrs xattrs) {
        
        super(null, false, true);
        
        // allocate a new buffer from the pool
        buffer = BufferPool.allocate(DYNAMIC_PART_INDEX + ownerID.getBytes().length + 4
            + owningGroupId.getBytes().length + 4
            + (linkTarget == null ? 4 : linkTarget.getBytes().length + 4)
            + (acl == null ? 4 : acl.size() + 4) + (defaultSP == null ? 4 : defaultSP.size() + 4)
            + (xattrs == null ? 4 : xattrs.size()) + 4);
        
        // fill the buffer with the given data
        buffer.position(0);
        buffer.putLong(id);
        buffer.putInt(atime);
        buffer.putInt(ctime);
        buffer.putInt(mtime);
        buffer.putString(ownerID);
        buffer.putString(owningGroupId);
        
        if (acl != null) {
            buffer.putInt(acl.size());
            acl.getBuffer().position(0);
            buffer.put(acl.getBuffer());
        } else {
            buffer.putInt(0);
        }
        
        if (defaultSP != null) {
            buffer.putInt(defaultSP.size());
            defaultSP.getBuffer().position(0);
            buffer.put(defaultSP.getBuffer());
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
        case SP:
            return 3;
        case LINKTRG:
            return 4;
        case XATTRS:
            return 5;
        default:
            throw new IllegalArgumentException("invalid attribute: " + attr);
        }
    }
}
