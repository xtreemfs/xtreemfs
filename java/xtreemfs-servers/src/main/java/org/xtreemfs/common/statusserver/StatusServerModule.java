/*
 * Copyright (c) 2012 by Bjoern Kolbeck.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.statusserver;

import java.io.IOException;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Status Server module to display information about a service.
 */
public abstract class StatusServerModule implements HttpHandler {

    /**
     * Name which is shown to the user.
     */
    public abstract String getDisplayName();
    
    /**
     * Path for accessing this module.
     */
    public abstract String getUriPath();
    
    /**
     * Should return true, if the module is suiatble for a service type.
     */
    public abstract boolean isAvailableForService(ServiceType service);
    
    /**
     * Initialize instance of the module for a service.
     * @param service indicates the service type for which the module is initialized.
     * @param serviceRequestDispatcher request dispatcher of service, must be casted according
     *        to the service type running.
     */
    public abstract void initialize(ServiceType service, Object serviceRequestDispatcher);
    
    /**
     * Shuts down the module, should also end any tasks/threads started.
     */
    public abstract void shutdown();
    
    protected void sendResponse(HttpExchange httpExchange, String html) throws IOException {
        try {
            byte[] content = html.getBytes("UTF-8");
            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            httpExchange.sendResponseHeaders(200, content.length);
            httpExchange.getResponseBody().write(content);
            httpExchange.getResponseBody().close();
        } catch (Exception ex) {
            Logging.logMessage(
                Logging.LEVEL_ERROR,
                this,
                "Error while sending response for module %s: %s",
                this.getClass().getCanonicalName(),
                ex.toString());
            httpExchange.sendResponseHeaders(500, 0);
            httpExchange.getResponseBody().close();
        }
    }
    
}
