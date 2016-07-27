/**
 * Copyright (c) 2013 Johannes Dillmann, Zuse Institute Berlin 
 *
 * Licensed under the BSD License, see LICENSE file for details.
 * 
 */
package org.xtreemfs.dir;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.database.ResultSet;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.statusserver.StatusServerHelper;
import org.xtreemfs.common.statusserver.StatusServerModule;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.osd.vivaldi.VivaldiNode;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

import com.sun.net.httpserver.HttpExchange;

/**
 * Serves a HTML status page, which is using JavaScript to visualize vivaldi distances and states.
 */
public class VivaldiStatusPage extends StatusServerModule {

    private DIRRequestDispatcher master;

    private DIRConfig            config;

    public VivaldiStatusPage(DIRConfig config) {
        this.config = config;
    }

    @Override
    public String getDisplayName() {
        return "Vivaldi Status Summary";
    }

    @Override
    public String getUriPath() {
        return "/vivaldi";
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
        if (uriPath.equals("/vivaldi/data")) {
            try {
                String content = getVivaldiData();
                sendResponse(httpExchange, content);
            } catch (BabuDBException ex) {
                Logging.logError(Logging.LEVEL_WARN, (Object) null, ex);
                httpExchange.sendResponseHeaders(500, 0);
            } finally {
                httpExchange.close();
            }
        } else if (uriPath.equals("/vivaldi")) {
            StatusServerHelper.sendFile("org/xtreemfs/dir/templates/vivaldi.html", httpExchange);
        } else if (uriPath.equals("/vivaldi/d3.js")) {
            StatusServerHelper.sendFile("org/xtreemfs/dir/templates/d3.js", httpExchange);
        } else {
            httpExchange.sendResponseHeaders(404, -1);
            httpExchange.close();
        }

    }

    private String getVivaldiData() throws BabuDBException, IOException {
        // NOTE(jdillmann): Access to the database is not synchronized. This might result in reading stale data.
        final Database database = master.getDirDatabase();
        StringBuilder dump = new StringBuilder();
        ResultSet<byte[], byte[]> iter = database
                .prefixLookup(DIRRequestDispatcher.INDEX_ID_SERVREG, new byte[0], null).get();

        // create tab separated plain text table
        dump.append("uuid");
        dump.append("\t");
        dump.append("name");
        dump.append("\t");
        dump.append("type");
        dump.append("\t");
        dump.append("status");
        dump.append("\t");
        dump.append("vivaldi_x");
        dump.append("\t");
        dump.append("vivaldi_y");
        dump.append("\t");
        dump.append("vivaldi_err");

        while (iter.hasNext()) {
            Entry<byte[], byte[]> e = iter.next();
            final String uuid = new String(e.getKey());
            final ServiceRecord sreg = new ServiceRecord(ReusableBuffer.wrap(e.getValue()));

            final ServiceStatus status = ServiceStatus.valueOf(Integer.valueOf(sreg.getData().get(
                    HeartbeatThread.STATUS_ATTR)));
            String statusString = "unknown value";
            switch (status) {
            case SERVICE_STATUS_AVAIL:
                statusString = "online";
                break;
            case SERVICE_STATUS_TO_BE_REMOVED:
                statusString = "locked";
                break;
            case SERVICE_STATUS_REMOVED:
                statusString = "removed";
                break;
            }

            VivaldiCoordinates coords;
            // TODO: remove this condition if other services support coordinates too
            if (sreg.getType() == ServiceType.SERVICE_TYPE_OSD) {
                coords = VivaldiNode.stringToCoordinates(sreg.getData().get("vivaldi_coordinates"));
            } else {
                VivaldiCoordinates.Builder coordBuilder = VivaldiCoordinates.newBuilder();
                coordBuilder.setXCoordinate(0.0);
                coordBuilder.setYCoordinate(0.0);
                coordBuilder.setLocalError(0.0);
                coords = coordBuilder.build();
            }

            dump.append("\n");
            dump.append(uuid);
            dump.append("\t");
            dump.append(sreg.getName());
            dump.append("\t");
            dump.append(sreg.getType());
            dump.append("\t");
            dump.append(statusString);
            dump.append("\t");
            dump.append(coords.getXCoordinate());
            dump.append("\t");
            dump.append(coords.getYCoordinate());
            dump.append("\t");
            dump.append(coords.getLocalError());

        } // while

        iter.free();

        master.getVivaldiClientMap().filterTimeOuts();
        // append clients

        // for (Map.Entry<InetSocketAddress, VivaldiClientValue> entry:
        // master.getVivaldiClientMap().entrySet()) {
        for (Map.Entry<String, org.xtreemfs.dir.VivaldiClientMap.VivaldiClientValue> entry : master
                .getVivaldiClientMap().entrySet()) {
            dump.append("\n");
            dump.append(entry.getValue().getAddress().toString());
            dump.append("\t");
            dump.append(entry.getValue().getAddress().getHostName());
            dump.append("\t");
            dump.append("CLIENT");
            dump.append("\t");
            dump.append("online");
            dump.append("\t");
            dump.append(entry.getValue().getCoordinates().getXCoordinate());
            dump.append("\t");
            dump.append(entry.getValue().getCoordinates().getYCoordinate());
            dump.append("\t");
            dump.append(entry.getValue().getCoordinates().getLocalError());
        }

        return dump.toString();
    }

}
