/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.olp;

import java.util.ArrayList;

/**
 * Simple implementation of a scalable ring-buffer for Double.
 * 
 * @author fx.langner
 * @version 1.00, 01/05/2012
 */
class RingBuffer extends ArrayList<Double> {

    private static final long serialVersionUID = 7066402284552005835L;

    private final int initialCapacity;
    private int capacity;
    private int position = 0;
    
    RingBuffer(int initialCapacity) {
        super(initialCapacity);
        
        this.initialCapacity = initialCapacity;
        this.capacity = initialCapacity;
    }
    
    final void put(Double value) {
        
        position %= capacity;
        
        if (position >= size()) add(value);
        else add(position, value);
        
        position++;
    }
    
    final boolean isCharged() {
        
        return capacity <= size();
    }
    
    final double avg() {
        
        assert (isCharged());
        
        double result = 0.0;
        
        for (int i = 0; i < capacity; i++) {
            result += get(i);
        }
                
        return result / capacity;
    }
    
    final void enlargeCapacity() {
        
        capacity++;
    }
    
    final void shrinkCapacity() {
        
        if (capacity > 1) {
            capacity--;
        }
    }
    
    final void doubleCapacity() {
        
        // prevent the capacity from becoming double-doubled
        if (size() >= capacity) {
            capacity *= 2;
        }
    }
    
    final void halveCapacity() {
        
        if (capacity > 1) {
            capacity /= 2;
        }
    }
    
    final boolean resetCapacity() {
        
        if (capacity < initialCapacity) {
            capacity = initialCapacity;
            return true;
        }
        
        return false;
    }
    
    /* (non-Javadoc)
     * @see java.util.AbstractCollection#toString()
     */
    @Override
    public String toString() {
        
        return capacity + " / " + size();
    }
}
