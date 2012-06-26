/*
 * Copyright (c) 2012 by Bjoern Kolbeck.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.statusserver;

import java.io.IOException;

import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;

import com.sun.net.httpserver.HttpExchange;

/**
 * Status page that shows the stack traces of all threads in the VM.
 *
 * @author bjko
 */
public class PrintStackTrace extends StatusServerModule {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        sendResponse(httpExchange, OutputUtils.getThreadDump());
    }

    @Override
    public String getDisplayName() {
        return "Thread Status Page";
    }

    @Override
    public String getUriPath() {
        return "/strace";
    }

    @Override
    public boolean isAvailableForService(ServiceType service) {
        return true;  // Available for all services.
    }

    @Override
    public void initialize(ServiceType service, Object serviceRequestDispatcher) {
        // Noop.
    }

    @Override
    public void shutdown() {
        // Noop.
    }

}
