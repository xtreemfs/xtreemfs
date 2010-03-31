/*
 * Copyright (c) 2008-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.foundation;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author bjko
 */
public final class DualQueue {
    
    private final ConcurrentLinkedQueue<Object> highPriority;
    private final ConcurrentLinkedQueue<Object> lowPriority;
    
    private final AtomicInteger          totalQueueLength;
    
    private final ReentrantLock          waitLock;
    
    private final Condition              notEmpty;
    
    public DualQueue() {
        highPriority = new ConcurrentLinkedQueue<Object>();
        lowPriority = new ConcurrentLinkedQueue<Object>();
        totalQueueLength = new AtomicInteger(0);
        waitLock = new ReentrantLock();
        notEmpty = waitLock.newCondition();
    }
    
    public void putHighPriority(Object item) {
        highPriority.add(item);
        incrementAndWakeup();
    }
    
    public void putLowPriority(Object item) {
        lowPriority.add(item);
        incrementAndWakeup();
    }
    
    private void incrementAndWakeup()  {
        if (totalQueueLength.incrementAndGet() == 1) {
            try {
                waitLock.lock();
                notEmpty.signalAll();
            } finally {
                waitLock.unlock();
            }
        }
    }

    public void poll(Collection<?> container, int maxItems) throws InterruptedException {
        if (totalQueueLength.get() == 0) {
            try {
                waitLock.lockInterruptibly();
                notEmpty.await();
            } finally {
                waitLock.unlock();
            }
        }

        int numItems = 0;
        do {
            
        } while (numItems < maxItems);
    }
    
    public Object poll() throws InterruptedException {
        
        if (totalQueueLength.get() == 0) {
            try {
                waitLock.lockInterruptibly();
                notEmpty.await();
            } finally {
                waitLock.unlock();
            }
        }
        
        Object item = highPriority.poll();
        if (item != null) {
            totalQueueLength.decrementAndGet();
            return item;
        }
        item = lowPriority.poll();
        if (item != null) {
            totalQueueLength.decrementAndGet();
            return item;
        }
            
        throw new RuntimeException("totalQueueCount is incorrect (> 0) while all queues are empty!");
    }
    
    public Object poll(long waitTimeInMs) throws InterruptedException {
        
        while (totalQueueLength.get() == 0) {
            try {
                waitLock.lockInterruptibly();
                notEmpty.await(waitTimeInMs,TimeUnit.MILLISECONDS);
            } finally {
                waitLock.unlock();
            }
        }
        
        if (totalQueueLength.get() == 0)
            return null;
        
        Object item = highPriority.poll();
        if (item != null) {
            totalQueueLength.decrementAndGet();
            return item;
        }
        item = lowPriority.poll();
        if (item != null) {
            totalQueueLength.decrementAndGet();
            return item;
        }
            
        throw new RuntimeException("totalQueueCount is incorrect (> 0) while all queues are empty!");
    }
   

}
