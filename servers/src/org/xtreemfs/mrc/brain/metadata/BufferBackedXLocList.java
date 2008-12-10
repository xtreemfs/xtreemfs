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

import java.util.Iterator;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;

public class BufferBackedXLocList extends BufferBackedMetadata implements XLocList {
    
    private static final int VERSION_INDEX      = 0;
    
    private static final int REPLICA_LIST_INDEX = 4;
    
    private XLoc[]           replicaCache;
    
    public BufferBackedXLocList(ReusableBuffer buffer, boolean copy, boolean freeOnDestroy) {
        
        super(buffer, copy, freeOnDestroy);
        
        buffer.position(REPLICA_LIST_INDEX);
        replicaCache = new XLoc[buffer.getInt()];
    }
    
    public BufferBackedXLocList(BufferBackedXLoc[] replicas, int version) {
        
        super(null, false, true);
        
        // determine required buffer size
        replicaCache = new XLoc[replicas.length];
        int bufSize = 8; // 1st & 2nd 4 bytes: version + # replicas
        for (BufferBackedXLoc repl : replicas)
            // size + 4 length bytes
            bufSize += repl.size() + 4;
        
        // allocate a new buffer and fill it with the given data
        buffer = BufferPool.allocate(bufSize);
        buffer.putInt(version);
        buffer.putInt(replicas.length);
        for (BufferBackedXLoc replica : replicas) {
            buffer.putInt(replica.size());
            replica.getBuffer().position(0);
            buffer.put(replica.getBuffer());
        }
        
    }
    
    public void destroy() {
        
        for (XLoc replica : replicaCache)
            if (replica != null)
                replica.destroy();
        
        super.destroy();
    }
    
    public int getReplicaCount() {
        buffer.position(REPLICA_LIST_INDEX);
        return buffer.getInt();
    }
    
    public XLoc getReplica(int replicaIndex) {
        
        if (replicaCache[replicaIndex] == null) {
            
            // find the correct index position in the buffer
            int index = getBufferIndex(replicaIndex);
            buffer.position(index);
            
            int len = buffer.getInt();
            assert (len >= 0);
            
            if (len == 0)
                return null;
            
            // create the target object from a view buffer (skip the len bytes)
            ReusableBuffer buf = buffer.createViewBuffer();
            buf.range(index + 4, len);
            buf.position(0);
            replicaCache[replicaIndex] = new BufferBackedXLoc(buf, false, true);
        }
        
        return replicaCache[replicaIndex];
    }
    
    public int getVersion() {
        buffer.position(VERSION_INDEX);
        return buffer.getInt();
    }
    
    public Iterator<XLoc> iterator() {
        
        return new Iterator<XLoc>() {
            
            private int index = 0;
            
            public boolean hasNext() {
                return index < getReplicaCount();
            }
            
            public XLoc next() {
                return getReplica(index++);
            }
            
            public void remove() {
                throw new UnsupportedOperationException("remove not implemented");
            }
        };
    }
    
    public void addReplica(XLoc replica, boolean incVersion) {
        
        assert (replica instanceof BufferBackedXLoc);
        BufferBackedXLoc repl = (BufferBackedXLoc) replica;
        
        // we assume that the buffer's limit marks the end of the last replica
        int offset = buffer.limit();
        
        // allocate a temporary buffer containing the replica's buffer + 4 bytes
        // for the size
        ReusableBuffer tmp = BufferPool.allocate(repl.getBuffer().limit() + 4);
        tmp.putInt(repl.getBuffer().limit());
        repl.getBuffer().position(0);
        tmp.put(repl.getBuffer());
        
        // insert the temp buffer at the end offset
        insert(offset, tmp);
        BufferPool.free(tmp);
        
        // if version incrementation is necessary, increment the version number
        // of the list
        if (incVersion) {
            int oldVer = getVersion();
            buffer.position(VERSION_INDEX);
            buffer.putInt(oldVer + 1);
        }
        
        // increment the replica count
        int count = getReplicaCount() + 1;
        buffer.position(REPLICA_LIST_INDEX);
        buffer.putInt(count);
        
        // empty the replica cache
        for (XLoc r : replicaCache)
            if (r != null)
                r.destroy();
        replicaCache = new XLoc[count];
        
    }
    
    public void removeReplica(int replicaIndex, boolean incVersion) {
        
        // determine buffer index and size of the replica to delete
        int index = getBufferIndex(replicaIndex);
        int size = getReplicaSize(index);
        
        // delete the replica
        delete(index, size + 4);
        
        // if version incrementation is necessary, increment the version number
        // of the list
        if (incVersion) {
            int oldVer = getVersion();
            buffer.position(VERSION_INDEX);
            buffer.putInt(oldVer + 1);
        }
        
        // decrement the replica count
        int count = getReplicaCount() - 1;
        buffer.position(REPLICA_LIST_INDEX);
        buffer.putInt(count);
        
        // empty the replica cache
        for (XLoc r : replicaCache)
            if (r != null)
                r.destroy();
        replicaCache = new XLoc[count];
    }
    
    private int getBufferIndex(int replicaIndex) {
        
        int index = 8;
        for (int i = 0; i < replicaIndex; i++) {
            buffer.position(index);
            int len = buffer.getInt();
            assert (len > 0);
            
            index += len + 4;
        }
        
        return index;
    }
    
    private int getReplicaSize(int index) {
        buffer.position(index);
        return buffer.getInt();
    }
    
}
