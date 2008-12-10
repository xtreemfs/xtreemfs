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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.foundation;

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
            listener.crashPerformed();
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
