/*
 * Copyright (c) 2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

/**
 *
 * Java donsn't have tupels.
 */
public class Tupel<T,V> {
    
    private T object1;
    private V object2;
    
    
    protected Tupel(T object1, V object2) {
        this.object1 = object1;
        this.object2 = object2;
    }
    
    protected T getFirst() {
        return object1;       
    }
    
    protected V getSecond() {
        return object2;
    }
    
}
