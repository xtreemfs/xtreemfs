/**
 * Copyright (c) 2013 Johannes Dillmann, Zuse Institute Berlin 
 *
 * Licensed under the BSD License, see LICENSE file for details.
 * 
 */

package org.xtreemfs.dir;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.xtreemfs.babudb.BabuDBFactory;
import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.database.ResultSet;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.config.ServiceConfig;
import org.xtreemfs.common.statusserver.StatusServerHelper;
import org.xtreemfs.common.statusserver.StatusServerModule;
import org.xtreemfs.dir.data.AddressMappingRecord;
import org.xtreemfs.dir.data.AddressMappingRecords;
import org.xtreemfs.dir.data.ConfigurationRecord;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.foundation.VersionManagement;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.osd.vivaldi.VivaldiNode;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.OSDHealthResult;

import com.sun.net.httpserver.HttpExchange;

/**
 * Serves a simple HTML status page with DIR stats.
 */
public class StatusPage extends StatusServerModule {

    private DIRRequestDispatcher master;

    private final DIRConfig      config;

    private final String         statusPageTemplate;

    /**
     * Time after a service, which has not send a heartbeat signal, will be displayed as not available
     * (default: 10 min).
     */
    private final static int     SERVICE_TIMEOUT = 600000;

    private enum Vars {

        MAXMEM("<!-- $MAXMEM -->"), 
        FREEMEM("<!-- $FREEMEM -->"), 
        AVAILPROCS("<!-- $AVAILPROCS -->"), 
        BPSTATS("<!-- $BPSTATS -->"), 
        PORT("<!-- $PORT -->"), 
        DEBUG("<!-- $DEBUG -->"), 
        NUMCON("<!-- $NUMCON -->"), 
        PINKYQ("<!-- $PINKYQ -->"), 
        NUMREQS("<!-- $NUMREQS -->"), 
        TIME("<!-- $TIME -->"), 
        TABLEDUMP("<!-- $TABLEDUMP -->"), 
        PROTOVERSION("<!-- $PROTOVERSION -->"), 
        VERSION("<!-- $VERSION -->"), 
        DBVERSION("<!-- $DBVERSION -->");

        private String template;

        Vars(String template) {
            this.template = template;
        }

        public String toString() {
            return template;
        }
    };

    public StatusPage(DIRConfig config) {
        StringBuffer sb = StatusServerHelper.readTemplate("org/xtreemfs/dir/templates/status.html");
        if (sb == null) {
            statusPageTemplate = "<h1>Template was not found, unable to show status page!</h1>";
        } else {
            statusPageTemplate = sb.toString();
        }

        this.config = config;
    }

    @Override
    public String getDisplayName() {
        return "DIR Status Summary";
    }

    @Override
    public String getUriPath() {
        return "/";
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

        ResultSet<byte[], byte[]> addrMapsIter = null;
        ResultSet<byte[], byte[]> servRegIter = null;
        ResultSet<byte[], byte[]> confIter = null;

        try {
            // NOTE(jdillmann): Access to the database is not synchronized. This might result in reading stale data.
            final Database database = master.getDirDatabase();

            assert (statusPageTemplate != null);

            long time = System.currentTimeMillis();

            addrMapsIter = database.prefixLookup(DIRRequestDispatcher.INDEX_ID_ADDRMAPS, new byte[0], null).get();
            StringBuilder dump = new StringBuilder();
            dump.append("<br><table width=\"100%\" frame=\"box\"><td colspan=\"2\" class=\"heading\">Address Mapping</td>");
            dump.append("<tr><td class=\"dumpTitle\">UUID</td><td class=\"dumpTitle\">mapping</td></tr>");
            while (addrMapsIter.hasNext()) {
                Entry<byte[], byte[]> e = addrMapsIter.next();
                AddressMappingRecords ams = new AddressMappingRecords(ReusableBuffer.wrap(e.getValue()));

                final String uuid = new String(e.getKey());

                dump.append("<tr><td class=\"uuid\">");
                dump.append(uuid);
                dump.append("</td><td class=\"dump\"><table width=\"100%\"><tr>");
                dump.append("<tr><td><table width=\"100%\">");
                long version = 0;
                for (AddressMappingRecord am : ams.getRecords()) {
                    dump.append("<tr><td class=\"mapping\">");
                    String endpoint = am.getUri() + " (" + am.getProtocol() + "," + am.getAddress() + ","
                            + am.getPort() + ")";
                    dump.append(endpoint);
                    dump.append("</td><td class=\"mapping\">");
                    dump.append(am.getMatch_network());
                    dump.append("</td><td class=\"mapping\">");
                    dump.append(am.getTtl_s());
                    dump.append("</td></tr>");
                    version = am.getVersion();
                }
                dump.append("</table></td></tr>");
                dump.append("<td class=\"version\">version: <b>");
                dump.append(version);
                dump.append("</b></td></tr></table>");
            }
            dump.append("</td></tr></table>");
            addrMapsIter.free();

            servRegIter = database.prefixLookup(DIRRequestDispatcher.INDEX_ID_SERVREG, new byte[0], null).get();

            dump.append("<br><table width=\"100%\" frame=\"box\"><td colspan=\"2\" class=\"heading\">Service Registry</td>");
            dump.append("<tr><td class=\"dumpTitle\">UUID</td><td class=\"dumpTitle\">mapping</td></tr>");
            while (servRegIter.hasNext()) {
                Entry<byte[], byte[]> e = servRegIter.next();
                final String uuid = new String(e.getKey());
                final ServiceRecord sreg = new ServiceRecord(ReusableBuffer.wrap(e.getValue()));

                dump.append("<tr><td class=\"uuid\">");
                dump.append(uuid);
                dump.append("</td><td class=\"dump\"><table width=\"100%\">");

                dump.append("<tr><td width=\"30%\">");
                dump.append("type");
                dump.append("</td><td><b>");
                dump.append(sreg.getType());
                dump.append("</b></td></tr>");

                dump.append("<tr><td width=\"30%\">");
                dump.append("name");
                dump.append("</td><td><b>");
                dump.append(sreg.getName());
                dump.append("</b></td></tr>");

                // sort the set of entries
                SortedMap<String, String> sMap = new TreeMap<String, String>();
                for (Entry<String, String> entry : sreg.getData().entrySet())
                    sMap.put(entry.getKey(), entry.getValue());
                
                //remove osd_health_check_output from map to display it together with osd_health_check_result
                String osdHealthCheckOutput = sMap.remove("osd_health_check_output");
                
                for (Entry<String, String> dataEntry : sMap.entrySet()) {
                    dump.append("<tr><td width=\"30%\">");
                    dump.append(dataEntry.getKey());
                    dump.append("</td><td><b>");

                    if (dataEntry.getKey().equals("status_page_url")) {
                        dump.append("<a href=\"");
                        dump.append(dataEntry.getValue());
                        dump.append("\">");
                    }

                    if (!(dataEntry.getKey().equals(HeartbeatThread.STATUS_ATTR) || dataEntry.getKey().equals(
                            "osd_health_check"))) {
                        dump.append(dataEntry.getValue());
                    }

                    if (dataEntry.getKey().equals("status_page_url")) {
                        dump.append("</a>");
                    } else if (dataEntry.getKey().equals("last_updated")) {

                    } else if (dataEntry.getKey().equals(HeartbeatThread.STATUS_ATTR)) {
                        ServiceStatus status = ServiceStatus.valueOf(Integer.valueOf(dataEntry.getValue()));
                        switch (status) {
                        case SERVICE_STATUS_AVAIL:
                            dump.append("online (new files will be assigned to it)");
                            break;
                        case SERVICE_STATUS_TO_BE_REMOVED:
                            dump.append("locked (new files will not be assigned to it)");
                            break;
                        case SERVICE_STATUS_REMOVED:
                            dump.append("removed (replicas assigned to this OSD will be replaced)");
                            break;
                        }
                    } else if (dataEntry.getKey().equals("free") || dataEntry.getKey().equals("total")
                            || dataEntry.getKey().endsWith("RAM") || dataEntry.getKey().equals("used")) {
                        dump.append(" bytes (");
                        dump.append(OutputUtils.formatBytes(Long.parseLong(dataEntry.getValue())));
                        dump.append(")");
                    } else if (dataEntry.getKey().equals("load")) {
                        dump.append("%");
                    } else if (dataEntry.getKey().equals("vivaldi_coordinates")) {
                        final VivaldiCoordinates coord = VivaldiNode.stringToCoordinates(dataEntry.getValue());
                        dump.append(" (");
                        dump.append(coord.getXCoordinate());
                        dump.append(",");
                        dump.append(coord.getYCoordinate());
                        dump.append(" err ");
                        dump.append(coord.getLocalError());
                        dump.append(")");
                    } else if (dataEntry.getKey().equals("osd_health_check")) {
                        switch (OSDHealthResult.valueOf(Integer.valueOf(dataEntry.getValue()))) {
                        case OSD_HEALTH_RESULT_PASSED:
                            dump.append("passed (" + osdHealthCheckOutput + ")");
                            break;
                        case OSD_HEALTH_RESULT_FAILED:
                            dump.append("<font color=#FF0000>failed (" + osdHealthCheckOutput + ")</font>");
                            break;
                        case OSD_HEALTH_RESULT_WARNING:
                            dump.append("<font color=#FFFF00>warning (" + osdHealthCheckOutput + ")</font>");
                            break;
                        case OSD_HEALTH_RESULT_NOT_AVAIL:
                            dump.append("Not available (" + dataEntry.getValue() + ")");
                        }
                    }
                    dump.append("</b></td></tr>");
                }

                dump.append("<tr><td width=\"30%\">");
                dump.append("last updated");
                dump.append("</td><td><b>");
                dump.append(sreg.getLast_updated_s());
                if (sreg.getLast_updated_s() == 0) {
                    dump.append(" (service was shutdown)");
                } else {
                    dump.append(" (");
                    Date lastUpdatedDate = new Date(sreg.getLast_updated_s() * 1000);
                    dump.append(lastUpdatedDate);
                    // check timeout only for MRCs and OSDs
                    if (sreg.getType() == ServiceType.SERVICE_TYPE_MRC
                            || sreg.getType() == ServiceType.SERVICE_TYPE_OSD) {
                        long lastUpdateDateTime = lastUpdatedDate.getTime();
                        if (lastUpdateDateTime < (System.currentTimeMillis() - SERVICE_TIMEOUT)) {
                            dump.append(", that's ");
                            dump.append(OutputUtils.SecondsToString((System.currentTimeMillis() - lastUpdateDateTime) / 1000));
                            dump.append(" ago. Please check connectivity of the server");
                        }
                    }
                    dump.append(")");
                    dump.append("</b></td></tr>");
                }

                dump.append("<td></td><td class=\"version\">version: <b>");
                dump.append(sreg.getVersion());
                dump.append("</b></td></table></td></tr>");
            }

            dump.append("</td></tr></table>");
            servRegIter.free();

            // Configuration part

            confIter = database.prefixLookup(DIRRequestDispatcher.INDEX_ID_CONFIGURATIONS, new byte[0], null).get();

            dump.append("<br><table width=\"100%\" frame=\"box\"><td colspan=\"2\" class=\"heading\">Configurations</td>");
            dump.append("<tr><td class=\"dumpTitle\">UUID</td><td class=\"dumpTitle\">Configuration Parameter</td></tr>");

            while (confIter.hasNext()) {
                Entry<byte[], byte[]> e = confIter.next();
                final String uuid = new String(e.getKey());
                final ConfigurationRecord conf = new ConfigurationRecord(ReusableBuffer.wrap(e.getValue()));

                dump.append("<tr><td class=\"uuid\">");
                dump.append(uuid);
                dump.append("</td><td class=\"dump\"><table width=\"100%\">");

                Collections.sort(conf.getData(), new Comparator<KeyValuePair>() {
                    public int compare(KeyValuePair o1, KeyValuePair o2) {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                });

                for (KeyValuePair kvp : conf.getData()) {
                    dump.append("<tr><td width=\"30%\">");
                    dump.append(kvp.getKey());
                    dump.append("</td><td><b>");

                    dump.append(kvp.getKey().equals(ServiceConfig.Parameter.ADMIN_PASSWORD.getPropertyString())
                            || kvp.getKey().equals(ServiceConfig.Parameter.CAPABILITY_SECRET.getPropertyString())
                            || kvp.getKey()
                                    .equals(ServiceConfig.Parameter.SERVICE_CREDS_PASSPHRASE.getPropertyString())
                            || kvp.getKey()
                                    .equals(ServiceConfig.Parameter.TRUSTED_CERTS_PASSPHRASE.getPropertyString()) ? "*******"
                            : kvp.getValue());
                    dump.append("</b></td></tr>");
                }

                dump.append("<td></td><td class=\"version\">version: <b>");
                dump.append(conf.getVersion());
                dump.append("</b></td></table></td></tr>");
            }
            confIter.free();

            dump.append("</b></td></table></td></tr>");
            dump.append("</table>");

            String tmp = null;
            try {
                tmp = statusPageTemplate.replace(Vars.AVAILPROCS.toString(), Runtime.getRuntime().availableProcessors()
                        + " bytes");
            } catch (Exception e) {
                tmp = statusPageTemplate;
            }
            tmp = tmp.replace(Vars.FREEMEM.toString(), Runtime.getRuntime().freeMemory() + " bytes");
            tmp = tmp.replace(Vars.MAXMEM.toString(), Runtime.getRuntime().maxMemory() + " bytes");
            tmp = tmp.replace(Vars.BPSTATS.toString(), BufferPool.getStatus());
            tmp = tmp.replace(Vars.PORT.toString(), Integer.toString(config.getPort()));
            tmp = tmp.replace(Vars.DEBUG.toString(), Integer.toString(config.getDebugLevel()));
            tmp = tmp.replace(Vars.NUMCON.toString(), Integer.toString(master.getNumConnections()));
            tmp = tmp.replace(Vars.NUMREQS.toString(), Long.toString(master.getNumRequests()));
            tmp = tmp.replace(Vars.TIME.toString(), new Date(time).toString() + " (" + time + ")");
            tmp = tmp.replace(Vars.TABLEDUMP.toString(), dump.toString());

            tmp = tmp.replace(Vars.VERSION.toString(), VersionManagement.RELEASE_VERSION);
            tmp = tmp.replace(Vars.PROTOVERSION.toString(), Integer.toString(DIRServiceConstants.INTERFACE_ID));
            tmp = tmp.replace(Vars.DBVERSION.toString(), BabuDBFactory.BABUDB_VERSION);

            sendResponse(httpExchange, tmp);

        } catch (BabuDBException ex) {
            Logging.logError(Logging.LEVEL_WARN, (Object) null, ex);
            httpExchange.sendResponseHeaders(500, 0);
        } finally {
            httpExchange.close();
            if (addrMapsIter != null) {
                addrMapsIter.free();
            }
            if (servRegIter != null) {
                servRegIter.free();
            }
            if (confIter != null) {
                confIter.free();
            }
        }
    }

}
