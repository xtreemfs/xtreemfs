/*
 * Copyright (c) 2009-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.monitoring;

import java.util.EventObject;

/**
 * An event which is created, when a value has changed. It piggybacks the key and new value, too. <br>
 * 22.07.2009
 */
public class MonitoringEvent<V> extends EventObject {
    private static final long serialVersionUID = 1L;

    /**
     * the key of the changed value
     */
    String                    key;
    /**
     * the changed value
     */
    V                         newValue;

    /**
     * Creates a new instance of this class.
     * 
     * @param source
     * @param key
     * @param newValue
     */
    public MonitoringEvent(Object source, String key, V newValue) {
        super(source);
        this.key = key;
        this.newValue = newValue;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the value
     */
    public V getNewValue() {
        return newValue;
    }
}
