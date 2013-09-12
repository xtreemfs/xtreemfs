/**
 * Copyright (c) 2013 Johannes Dillmann, Zuse Institute Berlin 
 *
 * Licensed under the BSD License, see LICENSE file for details.
 * 
 */

package org.xtreemfs.common.statusserver;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;

import com.sun.net.httpserver.HttpExchange;

/**
 * Serves a simple HTML status page with BabuDBs runtime state.
 */
public class BabuDBStatusPage extends StatusServerModule {

    private final BabuDBStatusProvider statusProvider;

    public BabuDBStatusPage(BabuDBStatusProvider statusProvider) {
        this.statusProvider = statusProvider;
    }

    @Override
    public String getDisplayName() {
        return "BabuDB Status Summary";
    }

    @Override
    public String getUriPath() {
        return "/babudb";
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<HTML><BODY><H1>BABUDB STATE</H1>");

        Map<String, Object> dbStatus = statusProvider.getStatus();
        if (dbStatus == null) {
            sb.append("BabuDB has not yet been initialized.");
        } else {
            sb.append("<TABLE>");
            Map<String, Object> status = new TreeMap<String, Object>(dbStatus);
            for (Entry<String, Object> entry : status.entrySet()) {
                sb.append("<TR><TD STYLE=\"text-align:right; font-style:italic\">");
                sb.append(entry.getKey());
                sb.append(":</TD><TD STYLE=\"font-weight:bold\">");
                sb.append(entry.getValue());
                sb.append("</TD></TR>");
            }
            sb.append("</TABLE>");
        }

        sb.append("</BODY></HTML>");

        sendResponse(httpExchange, sb.toString());
        httpExchange.close();
    }

    @Override
    public boolean isAvailableForService(ServiceType service) {
        return true;
    }

    @Override
    public void initialize(ServiceType service, Object serviceRequestDispatcher) {
    }

    @Override
    public void shutdown() {
    }

    /**
     * Simple interface which is used to provide the BabuDB status when the page is served.
     */
    public interface BabuDBStatusProvider {
        Map<String, Object> getStatus();
    }

}
