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

import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The class provides the ability to monitor data. The data must be added via push (put-method). For getting the
 * contained data poll (get-method) and push (via listeners) is supported.<br>
 * NOTE: This class is thread-safe. <br>
 * 22.07.2009
 */
public class Monitoring<V> {
    /**
     * contains the monitored data
     */
    protected ConcurrentHashMap<String, V>                           datasets;

    /**
     * contains the registered listeners
     */
    protected ConcurrentHashMap<String, List<MonitoringListener<V>>> listeners;

    /**
     * 
     */
    public Monitoring() {
        datasets = new ConcurrentHashMap<String, V>();
        listeners = new ConcurrentHashMap<String, List<MonitoringListener<V>>>();
    }

    /**
     * Adds the value to the given key and overwrites the old value. Notifies all associated listeners.
     * 
     * @param key
     * @param value
     * @return
     * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
     */
    public V put(String key, V value) {
        V oldValue = datasets.put(key, value);

        // check if listeners are registered
        if (listeners.containsKey(key)) {
            MonitoringEvent<V> event = new MonitoringEvent<V>(this, key, value);
            for (MonitoringListener<V> listener : listeners.get(key))
                listener.valueAddedOrChanged(event);
        }
        return oldValue;
    }

    /**
     * @param key
     * @return
     * @see java.util.HashMap#get(java.lang.Object)
     */
    public V get(String key) {
        return datasets.get(key);
    }

    /**
     * @param key
     * @return
     * @see java.util.HashMap#containsKey(java.lang.Object)
     */
    public boolean containsKey(String key) {
        return datasets.containsKey(key);
    }

    /**
     * @return
     * @see java.util.HashMap#entrySet()
     */
    public Set<Entry<String, V>> entrySet() {
        return datasets.entrySet();
    }

    /**
     * @param key
     * @return
     * @see java.util.HashMap#remove(java.lang.Object)
     */
    public V remove(String key) {
        return datasets.remove(key);
    }

    /**
     * @return
     * @see java.util.HashMap#size()
     */
    public int size() {
        return datasets.size();
    }

    /**
     * @return
     * @see java.util.AbstractMap#toString()
     */
    public String toString() {
        return datasets.toString();
    }

    /**
     * Registers a listener for the specified key. If the value of the key changes, the associated listeners
     * will be notified.
     * 
     * @param key
     * @param listener
     */
    public void registerListener(String key, MonitoringListener<V> listener) {
        List<MonitoringListener<V>> list = listeners.get(key);
        if (list == null)
            list = new CopyOnWriteArrayList<MonitoringListener<V>>();
        list.add(listener);
    }

    /**
     * Unregisters the given listener for the given key.
     * 
     * @param key
     * @param listener
     */
    public void unregisterListener(String key, MonitoringListener<V> listener) {
        List<MonitoringListener<V>> list = listeners.get(key);
        if (list != null) {
            int index = list.indexOf(listener);
            if (index != -1)
                list.remove(index);
        }
    }
    
    /*
     * system-wide stuff
     */
    private static boolean monitoringEnabled = false;

    /**
     * enables monitoring for the whole system
     */
    public static void enable() {
        monitoringEnabled = true;
    }

    /**
     * @return the monitoringEnabled
     */
    public static boolean isEnabled() {
        return monitoringEnabled;
    }
}
