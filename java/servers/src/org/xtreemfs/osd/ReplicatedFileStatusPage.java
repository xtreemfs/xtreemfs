/*
 * Copyright (c) 2012 by Bjoern Kolbeck.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import org.xtreemfs.common.statusserver.StatusServerModule;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;

import com.sun.net.httpserver.HttpExchange;

/**
 * Status page showing the list of files currently open in replication mode.
 * @author bjko
 */
class ReplicatedFileStatusPage extends StatusServerModule {

    private OSDRequestDispatcher myDispatcher;

    @Override
    public String getDisplayName() {
        return "OSD Replicated File Table";
    }

    @Override
    public String getUriPath() {
        return "/rft";
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
            final StringBuffer sb = new StringBuffer();
            final AtomicReference<Map<String, Map<String, String>>> result
                = new AtomicReference<Map<String, Map<String, String>>>();
            sb.append("<HTML><HEAD><TITLE>Replicated File Status List</TITLE>");
            sb.append("<STYLE type=\"text/css\">body,table,tr,td,h1 ");
            sb.append("{font-family:Arial,Helvetica,sans-serif;}</STYLE></HEAD><BODY>");
            sb.append("<H1>List of Open Replicated Files</H1>");
            sb.append("<TABLE border=\"1\">");
            sb.append("<TR><TD><B>File ID</B></TD><TD><B>Status</B></TD></TR>");
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

            for (String fileId : status.keySet()) {
                sb.append("<TR><TD>");
                sb.append(fileId);
                final String role = status.get(fileId).get("role");
                String bgcolor = "#FFFFFF";
                if (role != null && role.equals("primary")) {
                    bgcolor = "#A3FFA3";
                } else if (role != null && role.startsWith("backup")) {
                    bgcolor = "#FFFF66";
                }
                sb.append("</TD><TD style=\"background-color:");
                sb.append(bgcolor);
                sb.append("\"><TABLE border=\"0\">");
                for (Entry<String, String> e : status.get(fileId).entrySet()) {
                    sb.append("<TR><TD>");
                    sb.append(e.getKey());
                    sb.append("</TD><TD>");
                    sb.append(e.getValue());
                    sb.append("</TD></TR>\n");
                }
                sb.append("</TABLE></TD></TR>\n");
            }
            sendResponse(httpExchange, sb.toString());
        } catch (Throwable ex) {
            ex.printStackTrace();
            httpExchange.sendResponseHeaders(500, 0);
        }
    }
}
