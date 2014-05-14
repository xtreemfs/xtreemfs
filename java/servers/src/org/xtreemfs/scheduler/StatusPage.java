/*
 * Copyright (c) 2014 by Lukas Kairies.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.scheduler;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.babudb.BabuDBFactory;
import org.xtreemfs.common.statusserver.StatusServerHelper;
import org.xtreemfs.common.statusserver.StatusServerModule;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.VersionManagement;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceConstants;
import org.xtreemfs.scheduler.data.OSDDescription;
import org.xtreemfs.scheduler.data.OSDPerformanceDescription;
import org.xtreemfs.scheduler.data.Reservation;
import org.xtreemfs.scheduler.data.Reservation.ReservationType;
import org.xtreemfs.scheduler.data.ResourceSet;

import com.sun.net.httpserver.HttpExchange;

public class StatusPage extends StatusServerModule {

    private enum Vars {
        PORT("<!-- $PORT -->"), DIRURL("<!-- $DIRURL -->"), DEBUG("<!-- $DEBUG -->"), NUMCON(
                "<!-- $NUMCON -->"), PINKYQ("<!-- $PINKYQ -->"), GLOBALTIME("<!-- $GLOBALTIME -->"), GLOBALRESYNC(
                "<!-- $GLOBALRESYNC -->"), LOCALTIME("<!-- $LOCALTIME -->"), MEMSTAT("<!-- $MEMSTAT -->"), BPSTATS(
                "<!-- $BPSTATS -->"), PROTOVERSION("<!-- $PROTOVERSION -->"), VERSION("<!-- $VERSION -->"), DBVERSION(
                "<!-- $DBVERSION -->"), LOCALRESYNC("<!-- $LOCALRESYNC -->"), AVAILABLEOSDS("<!-- $AVAILABLEOSDS -->"), RESERVATIONS(
                "<!-- $RESERVATIONS -->"), MAXPOSSIBLERESERVATION("<!-- $MAXPOSSIBLERESERVATION -->"), MPRSEQTHROUGHPUT(
                "<!-- $MPRSEQTHROUGHPUT -->"), MPRIOPS("<!-- $MPRIOPS -->"), MPRCAPACITY("<!-- $MPRCAPACITY -->");

        private String template;

        Vars(String template) {
            this.template = template;
        }

        public String toString() {
            return template;
        }
    }

    private String                     statusPageTemplate;

    private SchedulerRequestDispatcher master;

    private SchedulerConfig            config;

    public StatusPage(SchedulerConfig config) {
        StringBuffer sb = StatusServerHelper.readTemplate("org/xtreemfs/scheduler/templates/status.html");
        if (sb == null) {
            statusPageTemplate = "<h1>Template was not found, unable to show status page!</h1>";
        } else {
            statusPageTemplate = sb.toString();
        }
        this.config = config;
    }

    @Override
    public void initialize(ServiceType service, Object serviceRequestDispatcher) {
        assert (service == ServiceType.SERVICE_TYPE_SCHEDULER);
        master = (SchedulerRequestDispatcher) serviceRequestDispatcher;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        Map<Vars, String> values = new HashMap<Vars, String>();

        values.put(Vars.PORT, Integer.toString(config.getPort()));
        values.put(Vars.DIRURL, config.isUsingSSL() ? (config.isGRIDSSLmode() ? Schemes.SCHEME_PBRPCG
                : Schemes.SCHEME_PBRPCS) : Schemes.SCHEME_PBRPC + "://" + config.getDirectoryService().getHostName()
                + ":" + config.getDirectoryService().getPort());

        values.put(Vars.DEBUG, Integer.toString(config.getDebugLevel()));
        values.put(Vars.VERSION, VersionManagement.RELEASE_VERSION);
        values.put(Vars.PROTOVERSION, Integer.toString(SchedulerServiceConstants.INTERFACE_ID));
        values.put(Vars.DBVERSION, BabuDBFactory.BABUDB_VERSION);
        values.put(Vars.PINKYQ, Long.toString(master.getPendingRequests()));
        values.put(Vars.NUMCON, Integer.toString(master.getNumConnections()));

        long globalTime = TimeSync.getGlobalTime();
        long localTime = TimeSync.getLocalSystemTime();
        values.put(Vars.GLOBALTIME, new Date(globalTime).toString() + " (" + globalTime + ")");
        values.put(Vars.GLOBALRESYNC, Long.toString(TimeSync.getTimeSyncInterval()));
        values.put(Vars.LOCALTIME, new Date(localTime).toString() + " (" + localTime + ")");
        values.put(Vars.LOCALRESYNC, Long.toString(TimeSync.getLocalRenewInterval()));

        values.put(Vars.BPSTATS, BufferPool.getStatus());
        long freeMem = Runtime.getRuntime().freeMemory();
        String span = "<span>";
        if (freeMem < 1024 * 1024 * 32) {
            span = "<span class=\"levelWARN\">";
        } else if (freeMem < 1024 * 1024 * 2) {
            span = "<span class=\"levelERROR\">";
        }
        values.put(
                Vars.MEMSTAT,
                span + OutputUtils.formatBytes(freeMem) + " / "
                        + OutputUtils.formatBytes(Runtime.getRuntime().maxMemory()) + " / "
                        + OutputUtils.formatBytes(Runtime.getRuntime().totalMemory()) + "</span>");

        // Available OSDs
        StringBuffer OsdTableBuf = new StringBuffer();
        OsdTableBuf.append("<table width=\"100%\" frame=\"box\">");
        OsdTableBuf
                .append("<tr><td class=\"availableOsdsTitle\">Identifier</td><td class=\"availableOsdsTitle\">capabilites</td></tr>");
        for (OSDDescription osdDescription : master.getOsds()) {
            OSDPerformanceDescription capabilities = osdDescription.getCapabilities();

            OsdTableBuf.append("<tr><td class=\"identifier\">");
            OsdTableBuf.append(osdDescription.getIdentifier());
            OsdTableBuf.append("</td><td class=\"capabilities\"><table width=\"100%\">");

            OsdTableBuf.append("<tr><td width=\"30%\">");
            OsdTableBuf.append("capacity");
            OsdTableBuf.append("</td><td><b>");
            OsdTableBuf.append(capabilities.getCapacity() + " MiB");
            OsdTableBuf.append("</b></td></tr>");

            OsdTableBuf.append("<tr><td width=\"30%\">");
            OsdTableBuf.append("IOPS");
            OsdTableBuf.append("</td><td><b>");
            OsdTableBuf.append(capabilities.getIops());
            OsdTableBuf.append("</b></td></tr>");

            OsdTableBuf.append("<tr><td width=\"30%\">");
            OsdTableBuf.append("sequential throughput");
            OsdTableBuf.append("</td><td><b>");
            Map<Integer, Double> streamingPerformance = capabilities.getStreamingPerformance();
            for (int i = 1; i < streamingPerformance.size(); i++) {
                String streams = i + " Streams: ";
                if (i == 1) {
                    streams = i + " Stream: ";
                }

                OsdTableBuf.append(streams + capabilities.getStreamingPerformance().get(i) + " MiB/s");
                OsdTableBuf.append("<br>");
            }
            OsdTableBuf.append("</b></td></tr>");

            OsdTableBuf.append("</table></td></tr>");
        }
        OsdTableBuf.append("</td></tr></table>");
        values.put(Vars.AVAILABLEOSDS, OsdTableBuf.toString());

        // Reservations
        StringBuffer reservationTableBuf = new StringBuffer();
        reservationTableBuf.append("<table width=\"100%\" frame=\"box\">");
        reservationTableBuf
                .append("<tr><td class=\"availableOsdsTitle\">Volume Identifier</td><td class=\"availableOsdsTitle\">capabilites</td></tr>");
        for (Reservation reservation : master.getStore().getReservations()) {

            reservationTableBuf.append("<tr><td class=\"identifier\">");
            reservationTableBuf.append(reservation.getVolumeIdentifier());
            reservationTableBuf.append("</td><td class=\"capabilities\"><table width=\"100%\">");

            reservationTableBuf.append("<tr><td width=\"30%\">");
            reservationTableBuf.append("reservation type");
            reservationTableBuf.append("</td><td><b>");
            reservationTableBuf.append(reservation.getType().toString());
            reservationTableBuf.append("</b></td></tr>");

            reservationTableBuf.append("<tr><td width=\"30%\">");
            reservationTableBuf.append("capacity");
            reservationTableBuf.append("</td><td><b>");
            reservationTableBuf.append(reservation.getCapacity() + " MiB");
            reservationTableBuf.append("</b></td></tr>");
            
            if (reservation.getType() == ReservationType.STREAMING_RESERVATION) {
                reservationTableBuf.append("<tr><td width=\"30%\">");
                reservationTableBuf.append("streaming throughput");
                reservationTableBuf.append("</td><td><b>");
                reservationTableBuf.append(reservation.getStreamingThroughput() + " MiB/s");
                reservationTableBuf.append("</b></td></tr>");
            } else {
                reservationTableBuf.append("<tr><td width=\"30%\">");
                reservationTableBuf.append("random throughput");
                reservationTableBuf.append("</td><td><b>");
                reservationTableBuf.append(reservation.getRamdomThroughput() + " MiB/s");
                reservationTableBuf.append("</b></td></tr>");
            }
            
            reservationTableBuf.append("</table></td></tr>");
        }
        reservationTableBuf.append("</td></tr></table>");
        values.put(Vars.RESERVATIONS, reservationTableBuf.toString());

        // Max possible reservations
        ResourceSet freeResources = master.getReservationScheduler().getFreeResources();
        values.put(Vars.MPRCAPACITY, Double.toString(freeResources.getCapacity()) + " MiB");
        values.put(Vars.MPRIOPS, Double.toString(freeResources.getIops()) + " MiB/s");
        values.put(Vars.MPRSEQTHROUGHPUT, Double.toString(freeResources.getSeqTP()) + " MiB/s");

        String html = statusPageTemplate;
        for (Vars key : values.keySet()) {
            html = html.replace(key.toString(), values.get(key));
        }
        sendResponse(httpExchange, html);
    }

    @Override
    public String getDisplayName() {
        return "Scheduler Status Summary";
    }

    @Override
    public String getUriPath() {
        return "/";
    }

    @Override
    public boolean isAvailableForService(ServiceType service) {
        return service == ServiceType.SERVICE_TYPE_SCHEDULER;
    }

    @Override
    public void shutdown() {
    }
}
