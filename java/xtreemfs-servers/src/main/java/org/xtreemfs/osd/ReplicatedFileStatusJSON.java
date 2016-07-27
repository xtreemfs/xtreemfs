/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.xtreemfs.common.statusserver.StatusServerModule;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

/**
 * JSON interface returning the the list of files currently open in replication mode.
 */
public class ReplicatedFileStatusJSON extends StatusServerModule {

    private OSDRequestDispatcher myDispatcher;

    @Override
    public String getDisplayName() {
        return "OSD Replicated File Table JSON Interface";
    }

    @Override
    public String getUriPath() {
        return "/rft.json";
    }

    @Override
    public boolean isAvailableForService(ServiceType service) {
        return service == ServiceType.SERVICE_TYPE_OSD;
    }

    @Override
    public void initialize(ServiceType service, Object serviceRequestDispatcher) {
        assert (service == ServiceType.SERVICE_TYPE_OSD);
        myDispatcher = (OSDRequestDispatcher) serviceRequestDispatcher;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            final AtomicReference<Map<String, Map<String, String>>> result = new AtomicReference<Map<String, Map<String, String>>>();

            myDispatcher.getRWReplicationStage().getStatus(new RWReplicationStage.StatusCallback() {

                @Override
                public void statusComplete(Map<String, Map<String, String>> status) {
                    synchronized (result) {
                        result.set(status);
                        result.notifyAll();
                    }
                }

                @Override
                public void failed(ErrorResponse ex) {
                    result.set(null);
                    result.notifyAll();
                }
            });
            synchronized (result) {
                if (result.get() == null)
                    result.wait();
            }

            Map<String, Map<String, String>> status = result.get();

            if (status == null) {
                throw new Throwable("Error on getting RWReplication status.");
            }

            String statusJSON = JSONParser.writeJSON(status);

            // set headers
            Headers headers = httpExchange.getResponseHeaders();
            headers.set("Access-Control-Allow-Origin", "*");
            headers.set("Access-Control-Allow-Methods", "GET");
            headers.set("Content-Type", "application/json");

            sendResponse(httpExchange, statusJSON);

        } catch (Throwable ex) {
            Logging.logError(Logging.LEVEL_WARN, (Object) null, ex);
            httpExchange.sendResponseHeaders(500, 0);
        }
    }

}
