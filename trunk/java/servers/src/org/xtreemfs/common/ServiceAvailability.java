/*
 * Copyright (c) 2009 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 * It manages the availability for all services. If a service could not accessed it is marked as not available
 * for a period.<br>
 * This class is thread-safe.<br>
 * 06.04.2009
 */
public class ServiceAvailability {
    public final static int DEFAULT_INITIAL_TIMEOUT = 1000 * 60; // 1 minute
    public final static int DEFAULT_CLEANUP_INTERVAL = 1000 * 60 * 60; // 60 minutes
    public final static int DEFAULT_MAX_LAST_ACCESS = 1000 * 60 * 60 * 24; // 1 day
    
    /**
     * This thread removes services from the list, which were not accessed since a long time.
     * 06.04.2009
     */
    private class ServiceRemover extends Thread {
        final int cleanupInterval;
        final private int maxLastAccessTime;
        private final ConcurrentHashMap<ServiceUUID, ServiceInfo> serviceAvailability;
        boolean quit;

        public ServiceRemover(ConcurrentHashMap<ServiceUUID, ServiceInfo> serviceAvailability, int maxLastAccessTime,
                int cleanupInterval) {
            super("ServiceAvailability Service-Remover");
            this.serviceAvailability = serviceAvailability;
            this.cleanupInterval = cleanupInterval;
            this.maxLastAccessTime = maxLastAccessTime;
            this.quit = false;
        }

        /**
         * Shuts the thread down.
         */
        public void quitThread() {
            this.quit = true;
            this.interrupt();
        }

        /**
         * Main loop.
         */
        public void run() {
            while (!quit) {
                Iterator<ServiceInfo> serviceIt = serviceAvailability.values().iterator();
                while (serviceIt.hasNext()) {
                    ServiceInfo service = serviceIt.next();
                    // osd was not accessed since a long time
                    if(System.currentTimeMillis() - service.lastAccessTime > maxLastAccessTime)
                        serviceIt.remove();
                }
                try {
                    Thread.sleep(cleanupInterval);
                } catch (InterruptedException ex) {
                }
            }
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.lifecycle, this, "shutdown complete");
        }
    }
    
    /**
     * encapsulates the necessary infos
     * 09.04.2009
     */
    private static class ServiceInfo {
        private long lastAccessTime = 0; // milliseconds
        private long lastFailedAccessTime = 0; // milliseconds
        private int currentTimeout; // milliseconds

        public ServiceInfo(int timeout) {
            currentTimeout = timeout;
        }

        public boolean isAvailable() {
            lastAccessTime = System.currentTimeMillis();
            return lastFailedAccessTime + currentTimeout <= System.currentTimeMillis();
        }
        
        public void lastAccessFailed() {
            lastFailedAccessTime = System.currentTimeMillis();
            currentTimeout = currentTimeout * 2;
        }
    }

     /**
     * makes sure, that obsolete entries in the map will be removed from time to time
     */
    private final ServiceRemover removerThread;
    
    /**
     * saves the service-timeouts
     * key: OSD-UUID
     */
    private ConcurrentHashMap<ServiceUUID, ServiceInfo> serviceAvailability;
    
    private final int initialTimeout;

    /**
     * uses default time intervals
     */
    public ServiceAvailability() {
        this.serviceAvailability = new ConcurrentHashMap<ServiceUUID, ServiceInfo>();
        
        initialTimeout = DEFAULT_INITIAL_TIMEOUT;
        
        this.removerThread = new ServiceRemover(this.serviceAvailability, DEFAULT_MAX_LAST_ACCESS, DEFAULT_CLEANUP_INTERVAL);
        this.removerThread.start();
    }

    /*
     * useful for tests
     */
    /**
     * all params in milliseconds
     */
    public ServiceAvailability(int initialTimeout, int maxLastAccessTime, int cleanupInterval) {
        this.serviceAvailability = new ConcurrentHashMap<ServiceUUID, ServiceInfo>();
        
        this.initialTimeout = initialTimeout;
        
        this.removerThread = new ServiceRemover(this.serviceAvailability, maxLastAccessTime, cleanupInterval);
        this.removerThread.start();
    }

    /**
     * shutdown of the internal thread
     */
    public void shutdown() {
        this.removerThread.quitThread();
    }

    /**
     * Checks if the service should be available for access.
     * @param service
     * @return
     */
    public boolean isServiceAvailable(ServiceUUID service) {
        if (!serviceAvailability.containsKey(service)) {
            serviceAvailability.put(service, new ServiceInfo(initialTimeout));
            return true;
        } else
            return serviceAvailability.get(service).isAvailable();

    }

    /**
     * If a service could not be accessed, you must run this method. So the system can know who is available and
     * can manage the timeouts.
     * @param service
     */
    public void setServiceWasNotAvailable(ServiceUUID service) {
        if(!serviceAvailability.containsKey(service))
            serviceAvailability.put(service, new ServiceInfo(initialTimeout));
        serviceAvailability.get(service).lastAccessFailed();
    }
}
