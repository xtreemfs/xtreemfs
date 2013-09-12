/**
 * Copyright (c) 2013 Johannes Dillmann, Zuse Institute Berlin 
 *
 * Licensed under the BSD License, see LICENSE file for details.
 * 
 */

package org.xtreemfs.dir;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.database.ResultSet;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.common.statusserver.StatusServerHelper;
import org.xtreemfs.common.statusserver.StatusServerModule;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;

import com.sun.net.httpserver.HttpExchange;

/**
 * Serves a HTML page, which is using JavaScript to monitor and present the states of the OSDs and open files.
 */
public class ReplicaStatusPage extends StatusServerModule {

    private DIRRequestDispatcher master;

    private final String         statusPageTemplate;

    private enum Vars {
        OSDSJSON("<!-- $OSDSJSON -->");

        private String template;

        Vars(String template) {
            this.template = template;
        }

        @Override
        public String toString() {
            return template;
        }
    };

    public ReplicaStatusPage() {
        StringBuffer sb = StatusServerHelper.readTemplate("org/xtreemfs/dir/templates/replica_status.html");
        if (sb == null) {
            statusPageTemplate = "<h1>Template was not found, unable to show status page!</h1>";
        } else {
            statusPageTemplate = sb.toString();
        }
    }

    @Override
    public String getDisplayName() {
        return "Replica Status Summary";
    }

    @Override
    public String getUriPath() {
        return "/replica_status";
    }

    @Override
    public boolean isAvailableForService(ServiceType service) {
        return service == ServiceType.SERVICE_TYPE_DIR;
    }

    @Override
    public void initialize(ServiceType service, Object serviceRequestDispatcher) {
        assert (service == ServiceType.SERVICE_TYPE_DIR);
        master = (DIRRequestDispatcher) serviceRequestDispatcher;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        String uriPath = httpExchange.getRequestURI().getPath();
        if (uriPath.equals("/replica_status/d3.v3.js")) {
            StatusServerHelper.sendFile("org/xtreemfs/dir/templates/d3.v3.js", httpExchange);
        } else if (uriPath.equals("/replica_status")) {
            handleStatusPage(httpExchange);
        } else {
            httpExchange.sendResponseHeaders(404, -1);
            httpExchange.close();
        }

    }

    private void handleStatusPage(HttpExchange httpExchange) throws IOException {
        // The OSDs will be saved as a map associating the OSDs uuid with a map consisting of the uuid, the
        // name and the status page url. This map will be served as JSON.
        // For example: { "osd-uuid-1": { "uuid": "osd-uuid-1", "name": "OSD Number 1", "status_page_url": "http://osd1.cluster.tld"} }
        HashMap<String, HashMap<String, String>> osds = new HashMap<String, HashMap<String, String>>();
        ResultSet<byte[], byte[]> iter = null;

        try {
            // NOTE(jdillmann): Access to the database is not synchronized. This might result in reading stale data.
            final Database database = master.getDirDatabase();

            iter = database.prefixLookup(DIRRequestDispatcher.INDEX_ID_SERVREG, new byte[0], null).get();
            while (iter.hasNext()) {
                Entry<byte[], byte[]> e = iter.next();
                final String uuid = new String(e.getKey());
                final ServiceRecord sreg = new ServiceRecord(ReusableBuffer.wrap(e.getValue()));

                if (sreg.getType() == ServiceType.SERVICE_TYPE_OSD) {
                    HashMap<String, String> data = new HashMap<String, String>();
                    data.put("uuid", sreg.getUuid());
                    data.put("name", sreg.getName());

                    for (Entry<String, String> dataEntry : sreg.getData().entrySet()) {
                        if (dataEntry.getKey().equals("status_page_url")) {
                            data.put("status_page_url", dataEntry.getValue());
                        }
                    }

                    // Add only OSDs with a status_page_url and are not shutdown.
                    if (data.containsKey("status_page_url") && sreg.getLast_updated_s() != 0) {
                        osds.put(sreg.getUuid(), data);
                    }
                }

            }

            String osdsJSON = JSONParser.writeJSON(osds);
            sendResponse(httpExchange, statusPageTemplate.replace(Vars.OSDSJSON.toString(), osdsJSON));

        } catch (BabuDBException ex) {
            Logging.logError(Logging.LEVEL_WARN, (Object) null, ex);
            httpExchange.sendResponseHeaders(500, 0);
        } catch (JSONException ex) {
            Logging.logError(Logging.LEVEL_WARN, (Object) null, ex);
            httpExchange.sendResponseHeaders(500, 0);
        } finally {
            httpExchange.close();
            if (iter != null) {
                iter.free();
            }
        }
    }

}
