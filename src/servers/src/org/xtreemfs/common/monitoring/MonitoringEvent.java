/*  Copyright (c) 2009 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.common.monitoring;

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
