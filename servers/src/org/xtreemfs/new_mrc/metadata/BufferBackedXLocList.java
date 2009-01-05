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

import java.nio.ByteBuffer;
import java.util.Iterator;

public class BufferBackedXLocList extends BufferBackedMetadata implements XLocList {
    
    private static final int VERSION_INDEX      = 0;
    
    private static final int REPLICA_LIST_INDEX = 4;
    
    private XLoc[]           replicaCache;
    
    public BufferBackedXLocList(byte[] buffer) {
        super(buffer, 0, buffer.length);
        replicaCache = new XLoc[buffer[REPLICA_LIST_INDEX]];
    }
    
    public BufferBackedXLocList(BufferBackedXLoc[] replicas, int version) {
        
        super(null, 0, 0);
        
        // determine required buffer size
        replicaCache = new XLoc[replicas.length];
        int bufSize = 8; // 1st & 2nd 4 bytes: version + # replicas
        for (BufferBackedXLoc repl : replicas)
            // size + 4 length bytes
            bufSize += repl.len + 4;
        
        // allocate a new buffer and fill it with the given data
        buffer = new byte[bufSize];
        ByteBuffer tmp = ByteBuffer.wrap(buffer);
        tmp.putInt(version);
        tmp.putInt(replicas.length);
        
        // first, insert all offsets
        int replOffset = 0;
        for (BufferBackedXLoc replica : replicas) {
            replOffset += replica.getLength();
            tmp.putInt(replOffset);
        }
        
        // second, insert all replicas
        for (BufferBackedXLoc replica : replicas)
            tmp.put(replica.getBuffer(), replica.getOffset(), replica.getLength());
        
        offset = 0;
        len = buffer.length;
    }
    
    public int getReplicaCount() {
        return replicaCache.length;
    }
    
    public XLoc getReplica(int replicaIndex) {
        
        if (replicaCache[replicaIndex] == null) {
            
            // find the correct index position in the buffer
            int index = getBufferIndex(replicaIndex);
            
            int nextIndex = replicaIndex == getReplicaCount() - 1 ? offset + len
                : getBufferIndex(replicaIndex + 1);
            
            ByteBuffer tmp = ByteBuffer.wrap(buffer);
            int length = tmp.getInt(index);
            assert (length >= 0);
            
            if (length == 0)
                return null;
            
            replicaCache[replicaIndex] = new BufferBackedXLoc(buffer, offset + index, offset
                + nextIndex);
        }
        
        return replicaCache[replicaIndex];
    }
    
    public int getVersion() {
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
        return tmp.getInt(VERSION_INDEX);
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
    
    private int getBufferIndex(int replicaIndex) {
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset, len);
        return tmp.getInt(REPLICA_LIST_INDEX + replicaIndex * Integer.SIZE / 8);
    }
    
}
