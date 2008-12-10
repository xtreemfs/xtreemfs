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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.xtreemfs.common.buffer.ASCIIString;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;

public class BufferBackedACL extends BufferBackedMetadata implements ACL {
    
    public static class Entry implements ACL.Entry {
        
        private String entity;
        
        private int    rights;
        
        public Entry(String entity, int rights) {
            this.entity = entity;
            this.rights = rights;
        }
        
        public String getEntity() {
            return entity;
        }
        
        public int getRights() {
            return rights;
        }
        
        public String toString() {
            return "(" + entity + "=" + rights + ")";
        }
    }
    
    public BufferBackedACL(ReusableBuffer buffer, boolean copy, boolean freeOnDestroy) {
        super(buffer, copy, freeOnDestroy);
    }
    
    public BufferBackedACL(String[] entities, int[] rights) {
        
        super(null, false, true);
        
        // create a sorted list of entries
        List<Entry> list = new ArrayList<Entry>(entities.length);
        for (int i = 0; i < entities.length; i++)
            list.add(new Entry(entities[i], rights[i]));
        Collections.sort(list, new Comparator<Entry>() {
            public int compare(Entry o1, Entry o2) {
                return o1.entity.compareTo(o2.entity);
            }
        });
        
        // determine required buffer size
        int bufSize = 4; // number of entries stored in first 4 bytes
        for (String entity : entities)
            // length + 4 len bytes + 4 rights bytes
            bufSize += entity.length() + 8;
        
        // allocate a new buffer
        buffer = BufferPool.allocate(bufSize);
        buffer.putInt(entities.length);
        
        // fill the buffer with the sorted list
        for (Entry entry : list) {
            buffer.putString(entry.entity);
            buffer.putInt(entry.rights);
        }
    }
    
    public Iterator<ACL.Entry> iterator() {
        
        return new Iterator<ACL.Entry>() {
            
            private int count = 0;
            
            private int index = 4;
            
            public boolean hasNext() {
                return count < getEntryCount();
            }
            
            public Entry next() {
                
                buffer.position(index);
                Entry entry = new Entry(buffer.getString(), buffer.getInt());
                index = buffer.position();
                count++;
                
                return entry;
            }
            
            public void remove() {
                throw new UnsupportedOperationException("remove not implemented");
            }
        };
    }
    
    public int getEntryCount() {
        buffer.position(0);
        return buffer.getInt();
    }
    
    public Integer getRights(String entity) {
        
        int index = getIndexPosition(entity);
        if (index == -1)
            return null;
        
        buffer.position(index);
        buffer.position(index + buffer.getInt() + 4);
        return buffer.getInt();
    }
    
    public void editEntry(String entity, int rights) {
        
        // first, find the position and check whether an insert operation is
        // necessary
        int index = 4;
        boolean insert = false;
        for (;;) {
            assert (index <= buffer.limit());
            
            if (index == buffer.limit()) {
                insert = true;
                break;
            }
            
            buffer.position(index);
            ASCIIString ent = buffer.getBufferBackedASCIIString();
            
            if (ent.toString().compareTo(entity) < 0)
                index += getEntrySize(index);
            else if (ent.toString().compareTo(entity) == 0) {
                insert = false;
                break;
            } else {
                insert = true;
                break;
            }
        }
        
        // if no insert operation is necessary, simply replace the rights string
        if (!insert)
            buffer.putInt(rights);
        
        // otherwise, create a buffer containing the new entry and insert it
        else {
            // determine the size for the entry buffer
            final int size = entity.length() + 8;
            
            // create and fill the buffer
            ReusableBuffer tmp = BufferPool.allocate(size);
            tmp.putString(entity);
            tmp.putInt(rights);
            
            // insert the buffer
            insert(index, tmp);
            
            // update the entry count
            int entryCount = getEntryCount();
            buffer.position(0);
            buffer.putInt(entryCount + 1);
        }
        
    }
    
    public void deleteEntry(String entity) {
        
        int index = getIndexPosition(entity);
        if (index == -1)
            return;
        
        // determine the entry size
        buffer.position(index);
        final int count = buffer.getInt() + 8;
        
        // delete the entry
        delete(index, count);
        
        // update the entry count
        int entryCount = getEntryCount();
        buffer.position(0);
        buffer.putInt(entryCount - 1);
    }
    
    private int getEntrySize(int index) {
        
        buffer.position(index);
        int len = buffer.getInt();
        assert (len > 0);
        
        return len + 8;
    }
    
    private int getIndexPosition(String entity) {
        
        int index = 4;
        for (;;) {
            assert (index <= buffer.limit());
            
            if (index == buffer.limit())
                return -1;
            
            buffer.position(index);
            ASCIIString ent = buffer.getBufferBackedASCIIString();
            
            if (ent.toString().compareTo(entity) < 0)
                index += getEntrySize(index);
            else if (ent.toString().compareTo(entity) == 0)
                return index;
            else
                return -1;
        }
    }
    
}
