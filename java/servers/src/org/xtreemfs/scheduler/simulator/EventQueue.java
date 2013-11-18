/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler.simulator;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class EventQueue {
    private PriorityQueue<SchedulerEvent> q;

    public EventQueue(int capacity) {
        this.q = new PriorityQueue<SchedulerEvent>(capacity, new Comparator<SchedulerEvent>() {
            @Override
            public int compare(SchedulerEvent o1, SchedulerEvent o2) {
                return (int) (o1.getTimeStamp() - o2.getTimeStamp());
            }
        });
    }

    public SchedulerEvent getNextEvent() {
        return this.q.poll();
    }

    public void add(SchedulerEvent e) {
        this.q.add(e);
    }

    public int size() {
        return this.q.size();
    }
}
