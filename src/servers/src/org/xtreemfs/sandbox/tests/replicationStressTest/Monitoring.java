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
package org.xtreemfs.sandbox.tests.replicationStressTest;

import java.util.HashMap;

/**
 * 
 * <br>
 * 23.06.2009
 */
public class Monitoring {
    protected static Monitoring     instance;

    /**
     * value: B/ms
     */
    protected HashMap<String, Long> entries;

    /**
     * Creates a new instance of Monitoring
     */
    private Monitoring() {
        instance = this;
        HashMap<String, Long> entries = new HashMap<String, Long>();
    }

    public synchronized static void start() {
        if (instance == null) {
            instance = new Monitoring();
        }
    }

    public static int getNumberOfEntries() {
        return instance.entries.size();
    }

    public static void resetEntry(String key) {
        instance.entries.remove(key);
    }

    public static void resetAllEntries() {
        for(String key : instance.entries.keySet())
            resetEntry(key);
    }

    /*
     * throughput
     */
    /**
     * @param requiredTime in ms
     */
    public static void monitorThroughput(String key, long bytes, long requiredTime) {
        if (instance.entries.containsKey(key)) {
            Long lastValue = instance.entries.get(key);
            instance.entries.put(key, (lastValue + (bytes / requiredTime)) / 2);
        } else
            instance.entries.put(key, bytes / requiredTime);
    }

    public static void printThroughput(String key) {
        Long value = instance.entries.get(key);
        if (value != null) {
            double normalized = value / 1024 * 1000; // KB/s
            String output = Math.round((normalized * 1000) / 1000) + "KB/s";
            System.out.println("average throughput:\t" + key + ":\t" + output);
        }
    }

    public static void printAllThroughput() {
        for(String key : instance.entries.keySet())
            printThroughput(key);
    }

    /*
     * time to replicate all objects of the file
     */
    /**
     * @param startTime in ms
     */
    public static boolean monitorStartTime(String key, long startTime) {
        if (!instance.entries.containsKey(key)) {
            instance.entries.put(key, startTime);
            return true;
        } else
            return false;
    }

    /**
     * @param endTime in ms
     */
    public static boolean monitorEndTime(String key, long endTime) {
        if (instance.entries.containsKey(key)) {
            Long startTime = instance.entries.get(key);
            instance.entries.put(key, endTime - startTime);
            return true;
        } else
            return false;
    }

    public static void printRequiredTime(String key) {
        Long value = instance.entries.get(key);
        if (value != null) {
            String output = value + "ms";
            System.out.println("required time:\t" + key + ":\t" + output);
        }
    }

    public static void printAllRequiredTime() {
        for(String key : instance.entries.keySet())
            printThroughput(key);
    }
}
