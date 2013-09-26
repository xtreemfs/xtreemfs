/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.queue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
abstract public class AbstractWeightedFairQueue<E> {

    private Map<String, Queue<E>>  queues;

    private Map<String, Integer>    qualityClassWeights;

    private int                     capacity;

    private Iterator<Queue<E>>      queueIterator = null;

    public AbstractWeightedFairQueue(int capacity) {
        this.queues = new HashMap<String, Queue<E>>();
        this.qualityClassWeights = new HashMap<String, Integer>();
        this.capacity = capacity;
    }

    public synchronized void add(String qualityClass, E element) {
        if(queues.containsKey(qualityClass)) {
            queues.get(qualityClass).add(element);
        } else {
            throw new IllegalArgumentException("Unknown quality class");
        }
    }

    public synchronized E take() throws InterruptedException {
        E element;

        while((element = this.getNextQueue().poll()) == null) {
            Thread.sleep(10);
        }
        return element;
    }

    public synchronized boolean isEmpty() {
        for(Queue q: queues.values()) {
            if(!q.isEmpty())
                return false;
        }
        return true;
    }

    public synchronized void addQualityClass(String qualityClass, int weight) {
        this.qualityClassWeights.put(qualityClass, weight);
        Queue<E> queue = new LinkedBlockingDeque<E>(capacity);
        this.queues.put(qualityClass, queue);
    }

    public synchronized void removeQualityClass(int qualityClass) {
        this.qualityClassWeights.remove(qualityClass);
        this.queues.remove(qualityClass);
    }

    public synchronized int getQualityClassWeight(String qualityClass) {
        if(this.qualityClassWeights.containsKey(qualityClass))
            return this.qualityClassWeights.get(qualityClass);
        else
            throw new IllegalArgumentException("Unknown quality class");
    }

    public int size() {
        int size = 0;
        for(Queue<E> q: this.queues.values()) {
            size += q.size();
        }
        return size;
    }

    public abstract int getElementSize(E element);

    private Queue<E> getNextQueue() {
        // TODO(ckleineweber): consider quality class weights
        if(this.queueIterator == null || !this.queueIterator.hasNext()) {
            this.queueIterator = queues.values().iterator();
        }

        return this.queueIterator.next();
    }
}
