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

import org.xtreemfs.common.buffer.ASCIIString;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;

public abstract class BufferBackedFSObject extends BufferBackedMetadata {
    
    protected static final int         ID        = 0;
    
    protected static final int         ATIME     = 1;
    
    protected static final int         CTIME     = 2;
    
    protected static final int         MTIME     = 3;
    
    protected static final int         SIZE      = 4;
    
    protected static final int         LINKCOUNT = 5;
    
    protected static final int         EPOCH     = 6;
    
    protected static final int         ISSEPOCH  = 7;
    
    protected static final int         READONLY  = 8;
    
    protected static final int         OWNER     = 9;
    
    protected static final int         GROUP     = 10;
    
    protected static final int         ACL       = 11;
    
    protected static final int         XLOC      = 12;
    
    protected static final int         SP        = 13;
    
    protected static final int         LINKTRG   = 14;
    
    protected static final int         XATTRS    = 15;
    
    private ASCIIString                ownerIDString;
    
    private ASCIIString                owningGroupIDString;
    
    private ASCIIString                linkTargetString;
    
    private BufferBackedACL            acl;
    
    private BufferBackedXAttrs         xattrs;
    
    private BufferBackedStripingPolicy sp;
    
    protected BufferBackedFSObject(ReusableBuffer buffer, boolean copy, boolean freeOnDestroy) {
        super(buffer, copy, freeOnDestroy);
    }
    
    /**
     * Returns all internally allocated buffers.
     */
    public void destroy() {
        
        if (sp != null)
            sp.destroy();
        if (xattrs != null)
            xattrs.destroy();
        if (acl != null)
            acl.destroy();
        
        super.destroy();
    }
    
    public long getId() {
        buffer.position(getFixedBufferIndex(ID));
        return buffer.getLong();
    }
    
    public void setId(long id) {
        buffer.position(getFixedBufferIndex(ID));
        buffer.putLong(id);
    }
    
    public int getAtime() {
        buffer.position(getFixedBufferIndex(ATIME));
        return buffer.getInt();
    }
    
    public void setAtime(int atime) {
        buffer.position(getFixedBufferIndex(ATIME));
        buffer.putInt(atime);
    }
    
    public int getCtime() {
        buffer.position(getFixedBufferIndex(CTIME));
        return buffer.getInt();
    }
    
    public void setCtime(int ctime) {
        buffer.position(getFixedBufferIndex(CTIME));
        buffer.putInt(ctime);
    }
    
    public int getMtime() {
        buffer.position(getFixedBufferIndex(MTIME));
        return buffer.getInt();
    }
    
    public void setMtime(int mtime) {
        buffer.position(getFixedBufferIndex(MTIME));
        buffer.putInt(mtime);
    }
    
    public ASCIIString getOwnerId() {
        
        if (ownerIDString == null)
            ownerIDString = getAttrString(getDynamicIndex(OWNER));
        
        return ownerIDString;
    }
    
    public void setOwnerId(String ownerId) {
        
        assert (ownerId != null);
        
        int offset = getDynamicBufferIndex(getDynamicIndex(OWNER));
        
        ASCIIString currentOwner = getOwnerId();
        ReusableBuffer buf = BufferPool.allocate(ownerId.length() + 4);
        buf.putString(ownerId);
        
        delete(offset, currentOwner.toString().length() + 4);
        insert(offset, buf);
        
        ownerIDString = null;
        BufferPool.free(buf);
    }
    
    public ASCIIString getOwningGroupId() {
        
        if (owningGroupIDString == null)
            owningGroupIDString = getAttrString(getDynamicIndex(GROUP));
        
        return owningGroupIDString;
    }
    
    public void setOwningGroupId(String groupId) {
        
        assert (groupId != null);
        
        int offset = getDynamicBufferIndex(getDynamicIndex(GROUP));
        
        ASCIIString currentGroup = getOwningGroupId();
        ReusableBuffer buf = BufferPool.allocate(groupId.length() + 4);
        buf.putString(groupId);
        
        delete(offset, currentGroup.toString().length() + 4);
        insert(offset, buf);
        
        owningGroupIDString = null;
        BufferPool.free(buf);
    }
    
    public ASCIIString getLinkTarget() {
        
        if (linkTargetString == null)
            linkTargetString = getAttrString(getDynamicIndex(LINKTRG));
        
        return linkTargetString;
    }
    
    public void setLinkTarget(String linkTarget) {
        
        int offset = getDynamicBufferIndex(getDynamicIndex(LINKTRG));
        
        ASCIIString currentLinkTarget = getLinkTarget();
        ReusableBuffer buf = BufferPool
                .allocate((linkTarget == null ? 0 : linkTarget.length()) + 4);
        buf.putString(linkTarget);
        
        delete(offset, (currentLinkTarget == null ? 0 : currentLinkTarget.toString().length()) + 4);
        insert(offset, buf);
        
        linkTargetString = null;
        BufferPool.free(buf);
    }
    
    public BufferBackedACL getAcl() {
        
        if (acl == null) {
            ReusableBuffer buf = getAttrBuffer(getDynamicIndex(ACL));
            if (buf != null)
                acl = new BufferBackedACL(buf, false, true);
        }
        
        return acl;
    }
    
    public void setACL(ACL acl) {
        
        assert (acl == null || acl instanceof BufferBackedACL);
        
        BufferBackedACL newACL = (BufferBackedACL) acl;
        BufferBackedACL currentACL = getAcl();
        int offset = getDynamicBufferIndex(getDynamicIndex(ACL));
        
        // remove the current entity
        delete(offset + 4, currentACL == null ? 0 : currentACL.size());
        this.acl.destroy();
        this.acl = null;
        
        // update the entity size
        buffer.position(offset);
        buffer.putInt(newACL.size());
        
        // insert the new entity
        insert(offset + 4, newACL.getBuffer());
    }
    
    public BufferBackedStripingPolicy getStripingPolicy() {
        
        if (sp == null) {
            ReusableBuffer buf = getAttrBuffer(getDynamicIndex(SP));
            if (buf != null)
                sp = new BufferBackedStripingPolicy(buf, false, true);
        }
        
        return sp;
    }
    
    public void setStripingPolicy(StripingPolicy sp) {
        
        assert (sp == null || sp instanceof BufferBackedStripingPolicy);
        
        BufferBackedStripingPolicy newSP = (BufferBackedStripingPolicy) sp;
        BufferBackedStripingPolicy currentSP = getStripingPolicy();
        int offset = getDynamicBufferIndex(getDynamicIndex(SP));
        
        // remove the current entity
        delete(offset + 4, currentSP == null ? 0 : currentSP.size());
        this.sp.destroy();
        this.sp = null;
        
        // update the entity size
        buffer.position(offset);
        buffer.putInt(newSP.size());
        
        // insert the new entity
        insert(offset + 4, newSP.getBuffer());
    }
    
    public BufferBackedXAttrs getXAttrs() {
        
        if (xattrs == null) {
            ReusableBuffer buf = getAttrBuffer(getDynamicIndex(XATTRS));
            if (buf != null)
                xattrs = new BufferBackedXAttrs(buf, false, true);
        }
        
        return xattrs;
    }
    
    public void setXAttrs(XAttrs xattrs) {
        
        assert (xattrs == null || xattrs instanceof BufferBackedXAttrs);
        
        BufferBackedXAttrs newXAttrs = (BufferBackedXAttrs) xattrs;
        BufferBackedXAttrs currentXAttrs = getXAttrs();
        int offset = getDynamicBufferIndex(getDynamicIndex(XATTRS));
        
        // remove the current entity
        delete(offset + 4, currentXAttrs == null ? 0 : currentXAttrs.size());
        this.xattrs.destroy();
        this.xattrs = null;
        
        // update the entity size
        buffer.position(offset);
        buffer.putInt(newXAttrs.size());
        
        // insert the new entity
        insert(offset + 4, newXAttrs.getBuffer());
    }
    
    protected ASCIIString getAttrString(int attrIndex) {
        
        // find the correct index position in the buffer
        int index = getDynamicBufferIndex(attrIndex);
        buffer.position(index);
        
        // total length = string length + # length bytes
        int len = buffer.getInt() + 4;
        
        // if string length == -1, return null string
        if (len == 3)
            return null;
        
        // create the string from a view buffer
        ReusableBuffer buf = buffer.createViewBuffer();
        buf.range(index, len);
        buf.position(0);
        ASCIIString string = buf.getBufferBackedASCIIString();
        BufferPool.free(buf);
        
        return string;
    }
    
    protected ReusableBuffer getAttrBuffer(int attrIndex) {
        
        // find the correct index position in the buffer
        int index = getDynamicBufferIndex(attrIndex);
        buffer.position(index);
        
        int len = buffer.getInt();
        assert (len >= 0);
        
        if (len == 0)
            return null;
        
        // create the target object from a view buffer (skip the len bytes)
        ReusableBuffer buf = buffer.createViewBuffer();
        buf.range(index + 4, len);
        buf.position(0);
        
        return buf;
    }
    
    protected int getDynamicBufferIndex(int dynAttrIndex) {
        
        int index = getDynamicBufferStartIndex();
        for (int i = 0; i < dynAttrIndex; i++) {
            buffer.position(index);
            int len = buffer.getInt();
            assert (len >= -1);
            
            index += 4 + (len > -1 ? len : 0);
        }
        
        return index;
    }
    
    protected abstract int getFixedBufferIndex(int fixedAttrIndex);
    
    protected abstract int getDynamicBufferStartIndex();
    
    protected abstract int getDynamicIndex(int attr);
    
}
