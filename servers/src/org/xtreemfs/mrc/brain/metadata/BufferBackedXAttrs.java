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

public class BufferBackedXAttrs extends BufferBackedMetadata implements XAttrs {
    
    public static class Entry implements XAttrs.Entry {
        
        private String key;
        
        private String value;
        
        private String uid;
        
        public Entry(String key, String uid, String value) {
            this.key = key;
            this.value = value;
            this.uid = uid;
        }
        
        public String getKey() {
            return key;
        }
        
        public String getUID() {
            return uid;
        }
        
        public String getValue() {
            return value;
        }
        
        public String toString() {
            return "(" + key + ", " + value + ", " + uid + ")";
        }
        
    }
    
    public BufferBackedXAttrs(ReusableBuffer buffer, boolean copy, boolean freeOnDestroy) {
        super(buffer, copy, freeOnDestroy);
    }
    
    public BufferBackedXAttrs(String[] keys, String[] values, String[] userIDs) {
        
        super(null, false, true);
        
        assert (keys.length == values.length && keys.length == userIDs.length);
        
        // create a sorted list of entries
        List<Entry> list = new ArrayList<Entry>(keys.length);
        for (int i = 0; i < keys.length; i++)
            list.add(new Entry(keys[i], userIDs[i], values[i]));
        Collections.sort(list, new Comparator<Entry>() {
            public int compare(Entry o1, Entry o2) {
                int tmp = o1.key.compareTo(o2.key);
                return tmp == 0 ? o1.uid.compareTo(o2.uid) : tmp;
            }
        });
        
        // determine required buffer size
        int bufSize = 4; // number of entries stored in first 4 bytes
        for (int i = 0; i < keys.length; i++)
            // length + 4 len bytes for each part
            bufSize += keys[i].length() + userIDs[i].length() + values[i].length() + 12;
        
        // allocate a new buffer
        buffer = BufferPool.allocate(bufSize);
        buffer.putInt(keys.length);
        
        // fill the buffer with the sorted list
        for (Entry entry : list) {
            buffer.putString(entry.key);
            buffer.putString(entry.uid);
            buffer.putString(entry.value);
        }
    }
    
    public int getEntryCount() {
        buffer.position(0);
        return buffer.getInt();
    }
    
    public Iterator<XAttrs.Entry> iterator() {
        
        return new Iterator<XAttrs.Entry>() {
            
            private int count = 0;
            
            private int index = 4;
            
            public boolean hasNext() {
                return count < getEntryCount();
            }
            
            public Entry next() {
                
                buffer.position(index);
                Entry entry = new Entry(buffer.getString(), buffer.getString(), buffer.getString());
                index = buffer.position();
                count++;
                
                return entry;
            }
            
            public void remove() {
                throw new UnsupportedOperationException("remove not implemented");
            }
        };
    }
    
    public ASCIIString getValue(String key, String uid) {
        
        int index = getIndexPosition(key, uid, false);
        if (index == -1)
            return null;
        
        // skip key and uid
        for (int i = 0; i < 2; i++) {
            buffer.position(index);
            index = index + buffer.getInt() + 4;
        }
        
        buffer.position(index);
        return buffer.getBufferBackedASCIIString();
    }
    
    public void deleteEntry(String key, String uid) {
        
        int index = getIndexPosition(key, uid, false);
        if (index == -1)
            return;
        
        // determine the entry size
        buffer.position(index);
        final int count = getEntrySize(index);
        
        // delete the entry
        delete(index, count);
        
        // update the entry count
        int entryCount = getEntryCount();
        buffer.position(0);
        buffer.putInt(entryCount - 1);
        
    }
    
    public void editEntry(String key, String value, String uid) {
        
        // first, delete the former entry if necessary
        deleteEntry(key, uid);
        
        // create a buffer containing the new entry and insert it
        
        // determine the size for the entry buffer
        final int size = key.length() + value.length() + uid.length() + 12;
        
        // create and fill the buffer
        ReusableBuffer tmp = BufferPool.allocate(size);
        tmp.putString(key);
        tmp.putString(uid);
        tmp.putString(value);
        
        // insert the buffer
        final int index = getIndexPosition(key, uid, true);
        insert(index, tmp);
        
        // update the entry count
        int entryCount = getEntryCount();
        buffer.position(0);
        buffer.putInt(entryCount + 1);
    }
    
    private int getEntrySize(int index) {
        
        int size = 0;
        
        for (int i = 0; i < 3; i++) {
            buffer.position(index + size);
            int len = buffer.getInt();
            assert (len >= 0);
            size += len + 4;
        }
        
        return size;
    }
    
    private int getIndexPosition(String key, String uid, boolean insert) {
        
        int index = 4;
        for (;;) {
            
            assert (index <= buffer.limit());
            
            if (index == buffer.limit())
                return insert ? index : -1;
            
            buffer.position(index);
            
            String k = buffer.getBufferBackedASCIIString().toString();
            int cmp = k.compareTo(key);
            if (cmp == 0) {
                String u = buffer.getBufferBackedASCIIString().toString();
                cmp = u.compareTo(uid);
            }
            
            if (cmp < 0)
                index += getEntrySize(index);
            else if (cmp == 0)
                return index;
            else
                return insert ? index : -1;
        }
    }
    
}
