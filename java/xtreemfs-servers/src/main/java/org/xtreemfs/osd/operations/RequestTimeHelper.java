/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import java.util.ArrayList;

/**
 *
 * @author bjko
 */
public class RequestTimeHelper {

    private final ArrayList<String> mps;

    private final ArrayList<Long>   mpNanos;

    public RequestTimeHelper() {
        mpNanos = new ArrayList(30);
        mps = new ArrayList(30);
        mps.add("Start");
        mpNanos.add(System.nanoTime());
    }

    public void addMP(String name) {
        final long now = System.nanoTime();
        
        
        mpNanos.add(now);
        mps.add(name);
    }

    public void print() {
        System.out.println("rq stats--------------------------------");
        long total = 0;
        for (int i = 1; i < mps.size(); i++) {
            final long lastMp = mpNanos.get(i-1);
            final long dur = mpNanos.get(i)-lastMp;
            total += dur;
            System.out.format(" %10.4f ms  %s\n",(((double)dur)/1e6),mps.get(i));
        }
        System.out.format(" %10.4f ms  total\n",(((double)total)/1e6));
    }

}
