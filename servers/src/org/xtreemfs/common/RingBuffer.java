/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.common;

import java.util.Iterator;

/**
 *
 * @author bjko
 */
public class RingBuffer<T> implements Iterable<T> {

    protected T[] items;
    protected int pointer;
    protected int start;
    
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
