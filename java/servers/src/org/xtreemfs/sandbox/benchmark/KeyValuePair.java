/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

/**
* @author jensvfischer
*/
class KeyValuePair<K, V> {
    K key;
    V value;

    KeyValuePair(){};

    KeyValuePair(K key, V value) {
        this.key = key;
        this.value = value;
    }
}
