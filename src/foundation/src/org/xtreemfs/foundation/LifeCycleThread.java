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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.foundation;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 * A base class for threads representing a life cycle. It offers methods for
 * blocking other threads until a certain life cycle event has occured. It
 * currently supports two life cycle-related events: startup and shutdown.
 * 
 * @author stender
 * 
 */
public class LifeCycleThread extends Thread {
    
    private final Object      startLock;
    
    private final Object      stopLock;
    
    private boolean           started;
    
    private boolean           stopped;
    
    private Exception         exc;
    
    private LifeCycleListener listener;
    
    public LifeCycleThread(String name) {
        super(name);
        startLock = new Object();
        stopLock = new Object();
    }
    
    /**
     * This method should be invoked by subclasses when the startup procedure
     * has been completed.
     */
    protected void notifyStarted() {
        
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this, "Thread %s started", Thread
                    .currentThread().getName());
        
        synchronized (startLock) {
            started = true;
            startLock.notifyAll();
            if (listener != null)
                listener.startupPerformed();
        }
    }
    
    /**
     * This method should be invoked by subclasses when the shutdown procedure
     * has been completed.
     */
    protected void notifyStopped() {
        
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this, "Thread %s terminated", Thread
                    .currentThread().getName());
        
        synchronized (stopLock) {
            stopped = true;
            stopLock.notifyAll();
            if (listener != null)
                listener.shutdownPerformed();
        }
    }
    
    /**
     * This method should be invoked by subclasses when the thread has crashed.
     */
    protected void notifyCrashed(Exception exc) {
        
        Logging.logMessage(Logging.LEVEL_CRIT, this, "service ***CRASHED***, shutting down");
        Logging.logError(Logging.LEVEL_CRIT, this, exc);
        
        synchronized (startLock) {
            this.exc = exc;
            started = true;
            startLock.notifyAll();
        }
        
        synchronized (stopLock) {
            this.exc = exc;
            stopped = true;
            stopLock.notifyAll();
        }
        
        if (listener != null)
            listener.crashPerformed(exc);
    }
    
    /**
     * Synchronously waits for a notification indicating that the startup
     * procedure has been completed.
     * 
     * @throws Exception
     *             if an error occured during the startup procedure
     */
    public void waitForStartup() throws Exception {
        synchronized (startLock) {
            
            while (!started)
                startLock.wait();
            
            if (exc != null)
                throw exc;
        }
    }
    
    /**
     * Synchronously waits for a notification indicating that the shutdown
     * procedure has been completed.
     * 
     * @throws Exception
     *             if an error occured during the shutdown procedure
     */
    public void waitForShutdown() throws Exception {
        synchronized (stopLock) {
            
            if (!started)
                return;
            while (!stopped)
                stopLock.wait();
            
            if (exc != null)
                throw exc;
        }
    }
    
    /**
     * Sets a listener waiting for life cycle events.
     * 
     * @param listener
     *            the listener
     */
    public void setLifeCycleListener(LifeCycleListener listener) {
        this.listener = listener;
    }
    
}
