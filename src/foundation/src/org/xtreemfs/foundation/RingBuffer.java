/*
 * Copyright (c) 2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */
package org.xtreemfs.foundation;

import java.util.Iterator;

/**
 *
 * @author bjko
 */
public class RingBuffer<T> implements Iterable<T> {

    protected T[] items;
    protected int pointer;
    protected int start;
    
    @SuppressWarnings("unchecked")
    public RingBuffer(int capacity) {
        items = (T[]) new Object[capacity];
        pointer = 0;
        start = 0;
    }
    
    public RingBuffer(int capacity, T initialValue) {
        this(capacity);
        for (int i = 0; i < capacity; i++)
            items[i] = initialValue;
    }
    
    public void insert(T item) {
        final T tmp = items[pointer];
        if (tmp != null) {
            //overwriting
            start++;
            if (start == items.length)
                start = 0;
        }
        items[pointer++] = item;
        if (pointer == items.length)
            pointer = 0;
    }
    
    @SuppressWarnings("hiding")
    private class RingBufferIterator<T> implements Iterator<T> {

        private int position;
        
        public RingBufferIterator() {
            position = 0;
        }
        
        public boolean hasNext() {
            if (position >= items.length)
                return false;
            return items[ (position+start) % items.length] != null;
        }

        @SuppressWarnings("unchecked")
        public T next() {
            return (T) items[ ((position++)+start) % items.length];
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }

    public Iterator<T> iterator() {
        return new RingBufferIterator<T>();
    }
    
    public String toString() {
        StringBuilder contents = new StringBuilder();
        contents.append("[ ");
        for (int i = 0; i < items.length; i++) {
            T item = items[ (i+start) % items.length];
            if (item == null)
                break;
            contents.append(item);
            contents.append(", ");
        }
        contents.append("] ");
        return contents.toString();
    }

}
