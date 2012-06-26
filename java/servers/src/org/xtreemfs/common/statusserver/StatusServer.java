/*
 * Copyright (c) 2012 by Bjoern Kolbeck.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.statusserver;

import java.io.IOException;
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
        
        httpServ = HttpServer.create(new InetSocketAddress(statusHttpPort), 0);
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
