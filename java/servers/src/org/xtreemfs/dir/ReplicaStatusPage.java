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
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;

import com.sun.net.httpserver.HttpExchange;

public class ReplicaStatusPage extends StatusServerModule {

    private DIRRequestDispatcher master;

    private final String         statusPageTemplate;

    private enum Vars {
        OSDSJSON("<!-- $OSDSJSON -->");

        private String template;

        Vars(String template) {
            this.template = template;
        }

        public String toString() {
            return template;
        }
    };

    public ReplicaStatusPage() {
        StringBuffer sb = StatusServerHelper.readTemplate("org/xtreemfs/dir/templates/replicated_status.html");
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
        return "/replicaStatus";
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
        if (uriPath.equals("/replicaStatus/d3.v3.js")) {
            StatusServerHelper.sendFile("org/xtreemfs/dir/templates/d3.v3.js", httpExchange);
        } else if (uriPath.equals("/replicaStatus")) {
            handleStatusPage(httpExchange);
        } else {
            httpExchange.sendResponseHeaders(404, -1);
            httpExchange.close();
        }

    }

    private void handleStatusPage(HttpExchange httpExchange) throws IOException {
        try {
            final Database database = master.getDirDatabase();
            ResultSet<byte[], byte[]> iter;

            HashMap<String, HashMap<String, String>> osds = new HashMap<String, HashMap<String, String>>();

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
                    // TODO (jdillmann): check if status_page_url was found
                    osds.put(sreg.getUuid(), data);
                }

            }
            iter.free();

            String osdsJSON = JSONParser.writeJSON(osds);
            sendResponse(httpExchange, statusPageTemplate.replace(Vars.OSDSJSON.toString(), osdsJSON));

        } catch (BabuDBException ex) {
            ex.printStackTrace();
            httpExchange.sendResponseHeaders(500, 0);
        } catch (JSONException ex) {
            ex.printStackTrace();
            httpExchange.sendResponseHeaders(500, 0);
        } finally {
            httpExchange.close();
        }
    }

}
