/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.common;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author bjko
 */
public final class DualQueue {
    
    private final LinkedBlockingQueue highPriority;
    private final LinkedBlockingQueue lowPriority;
    
    private final AtomicInteger          totalQueueLength;
    
    private final ReentrantLock          waitLock;
    
    private final Condition              notEmpty;
    
    public DualQueue() {
        highPriority = new LinkedBlockingQueue();
        lowPriority = new LinkedBlockingQueue();
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
        
        if (totalQueueLength.get() == 0) {
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
