/*
 * Copyright (c) 2012 by Bjoern Kolbeck.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.statusserver;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

/**
 * HTTP Server displaying status and debug information.
 * 
 * @author bjko
 */
public class StatusServer {
    /**
     * Block server up to this time if the StatusServer cannot bind the port due to "Address already in use".
     * Necessary because the Sun webserver does not support SO_REUSEADDR.
     */
    private static final double            MAX_TIME_CHECK_IF_ADDRESS_ALREADY_IN_USE_S = 180.0;
    /** Interval between two attempts to start the webserver. */
    private static final double            INTERVAL_CHECK_IF_ADDRESS_ALREADY_IN_USE_S = 0.1;

    private final int statusHttpPort;
    private final List<StatusServerModule> modules;
    private final ServiceType serviceType;
    private final Object serviceRequestDispatcher;
    private final Map<String, String> authorizedUsers;

    private HttpServer httpServ;
    private BasicAuthenticator passwordProtection;
    private boolean running;

    public StatusServer(
            ServiceType serviceType,
            Object serviceRequestDispatcher,
            int statusHttpPort) {
        this.serviceType = serviceType;
        this.serviceRequestDispatcher = serviceRequestDispatcher;
        this.statusHttpPort = statusHttpPort;
        authorizedUsers = new HashMap<String, String>();
        modules = new ArrayList<StatusServerModule>();
        running = false;
    }
    
    /**
     * Adds a user to the list of authorized users that can access status pages.
     */
    public void addAuthorizedUser(String userName, String password) {
        if (running) {
            throw new IllegalStateException("Cannot add users after startup.");
        }
        // TODO(bjko): Don't store plaintext passwords.
        authorizedUsers.put(userName, password);
    }

    public void start() throws IOException {
        if (!authorizedUsers.isEmpty()) {
            passwordProtection = new BasicAuthenticator("XtreemFS " + serviceType) {
                
                @Override
                public boolean checkCredentials(String userName, String password) {
                    String storedPassword = authorizedUsers.get(userName);
                    return storedPassword != null && storedPassword.equals(password);
                }
            };
        } else {
            passwordProtection = null;
        }
        
        // Try to start the webserver and bind the port. Retry if "address already in use" because the Sun
        // webserver does not support SO_REUSEADDR.
        for (double elapsedSeconds = 0.0;
             elapsedSeconds < MAX_TIME_CHECK_IF_ADDRESS_ALREADY_IN_USE_S;
             elapsedSeconds += INTERVAL_CHECK_IF_ADDRESS_ALREADY_IN_USE_S) {
            try {
                httpServ = HttpServer.create(new InetSocketAddress(statusHttpPort), 0);
                // Port successfully bound, do not retry.
                break;
            } catch (BindException e) {
                if (e.getMessage().contains("Address already in use")) {
                    if ((elapsedSeconds + INTERVAL_CHECK_IF_ADDRESS_ALREADY_IN_USE_S) >= MAX_TIME_CHECK_IF_ADDRESS_ALREADY_IN_USE_S) {
                        // Retries exceeded. Throw error.
                        throw new BindException(e.getMessage() + ". Port number: " + statusHttpPort);
                    } else {
                        // Wait between two attempts.
                        try {
                            Thread.sleep((long) (INTERVAL_CHECK_IF_ADDRESS_ALREADY_IN_USE_S * 1000));
                        } catch (InterruptedException e1) {
                        }
                    }
                } else {
                    throw e;
                }
            }
        }
        running = true;
        for (StatusServerModule module : modules) {
            String modulePath = module.getUriPath();
            if (modulePath.isEmpty()) {
                Logging.logMessage(
                    Logging.LEVEL_ERROR,
                    this,
                    "Invalid path or path for module %s",
                    module.getClass().getCanonicalName());
                   
            }
            HttpContext context = httpServ.createContext(module.getUriPath(), module);
            if (passwordProtection != null) {
                context.setAuthenticator(passwordProtection);
            }
            module.initialize(serviceType, serviceRequestDispatcher);
        }
        httpServ.start();
    }

    public void shutdown() {
        if (running) {
            httpServ.stop(0);
            for (StatusServerModule module : modules) {
                module.shutdown();
            }
        }
    }

    public void registerModule(StatusServerModule module) {
        if (running) {
            throw new IllegalStateException("Cannot add modules after calling start()");
        }
        if (module.isAvailableForService(serviceType)) {
            modules.add(module);
        } else {
            Logging.logMessage(Logging.LEVEL_INFO,
                this,
                "Module %s not suitable for service %s, skipping module",
                module.getClass().getCanonicalName(),
                serviceType);
        }
    }

}
