/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.VersionManagement;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/**
 *
 * @author bjko
 */
public class StatusPage {

    private enum Vars {
            UUID("<!-- $UUID -->"),
            MAXMEM("<!-- $MAXMEM -->"),
            FREEMEM("<!-- $FREEMEM -->"),
            AVAILPROCS("<!-- $AVAILPROCS -->"),
            BPSTATS("<!-- $BPSTATS -->"),
            PORT("<!-- $PORT -->"),
            DIRURL("<!-- $DIRURL -->"),
            DEBUG("<!-- $DEBUG -->"),
            NUMCON("<!-- $NUMCON -->"),
            PINKYQ("<!-- $PINKYQ -->"),
            PARSERQ("<!-- $PARSERQ -->"),
            AUTHQ("<!-- $AUTHQ -->"),
            STORAGEQ("<!-- $STORAGEQ -->"),
            DELETIONQ("<!-- $DELETIONQ -->"),
            OPENFILES("<!-- $OPENFILES -->"),
            OBJWRITE("<!-- $OBJWRITE -->"),
            OBJREAD("<!-- $OBJREAD -->"),
            BYTETX("<!-- $BYTETX -->"),
            BYTERX("<!-- $BYTERX -->"),
            REPLOBJWRITE("<!-- $REPLOBJWRITE -->"),
            REPLBYTERX("<!-- $REPLBYTERX -->"),
            GMAXMSG("<!-- $GMAXMSG -->"),
            GMAXRPC("<!-- $GMAXRPC -->"),
            DELETES("<!-- $DELETES -->"),
            GLOBALTIME("<!-- $GLOBALTIME -->"),
            GLOBALRESYNC("<!-- $GLOBALRESYNC -->"),
            LOCALTIME("<!-- $LOCALTIME -->"),
            LOCALRESYNC("<!-- $LOCALRESYNC -->"),
            MEMSTAT("<!-- $MEMSTAT -->"),
            UUIDCACHE("<!-- $UUIDCACHE -->"),
            STATCOLLECT("<!-- $STATCOLLECT -->"),
            DISKFREE("<!-- $DISKFREE -->"),
            PROTOVERSION("<!-- $PROTOVERSION -->"),
            VERSION("<!-- $VERSION -->");

        private String template;

        Vars(String template) {
            this.template = template;
        }

        public String toString() {
            return template;
        }
    }

    private final static String statusPageTemplate;

    static {
        StringBuffer sb = null;
        try {
            InputStream is = StatusPage.class.getClassLoader().getResourceAsStream(
                    "org/xtreemfs/osd/templates/status.html");
            if (is == null) {
                is = StatusPage.class.getClass().getResourceAsStream("../templates/status.html");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            sb = new StringBuffer();
            String line = br.readLine();
            while (line != null) {
                sb.append(line + "\n");
                line = br.readLine();
            }
            br.close();
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, null, ex);
        }
        if (sb == null) {
            statusPageTemplate = "<H1>Template was not found, unable to show status page!</h1>";
        } else {
            statusPageTemplate = sb.toString();
        }
    }


    public static String getStatusPage(OSDRequestDispatcher master) {

        long globalTime = TimeSync.getGlobalTime();
        long localTime = TimeSync.getLocalSystemTime();

        String tmp = statusPageTemplate.replace(Vars.AVAILPROCS.toString(), Runtime.getRuntime()
                .availableProcessors()
            + " bytes");
        tmp = tmp.replace(Vars.FREEMEM.toString(), Runtime.getRuntime().freeMemory() + " bytes");
        tmp = tmp.replace(Vars.MAXMEM.toString(), Runtime.getRuntime().maxMemory() + " bytes");
        tmp = tmp.replace(Vars.BPSTATS.toString(), BufferPool.getStatus());
        tmp = tmp.replace(Vars.UUID.toString(), master.getConfig().getUUID().toString());
        tmp = tmp.replace(Vars.PORT.toString(), Integer.toString(master.getConfig().getPort()));
        tmp = tmp.replace(Vars.DIRURL.toString(), (master.getConfig().isUsingSSL() ? (master.getConfig()
                .isGRIDSSLmode() ? Schemes.SCHEME_PBRPCG : Schemes.SCHEME_PBRPCS) : Schemes.SCHEME_PBRPC)
            + "://"
            + master.getConfig().getDirectoryService().getHostName()
            + ":"
            + master.getConfig().getDirectoryService().getPort());
        tmp = tmp.replace(Vars.DEBUG.toString(), Integer.toString(master.getConfig()
                .getDebugLevel()));
        tmp = tmp.replace(Vars.NUMCON.toString(), Integer.toString(master.getNumClientConnections()));
        tmp = tmp.replace(Vars.PINKYQ.toString(), Long.toString(master.getPendingRequests()));
        tmp = tmp.replace(Vars.PARSERQ.toString(), Integer.toString(master.getPreprocStage().getQueueLength()));
        tmp = tmp.replace(Vars.STORAGEQ.toString(), Integer.toString(master.getStorageStage().getQueueLength()));
        tmp = tmp.replace(Vars.DELETIONQ.toString(), Integer.toString(master.getDeletionStage().getQueueLength()));
        tmp = tmp.replace(Vars.OPENFILES.toString(), Integer.toString(master.getPreprocStage().getNumOpenFiles()));
        tmp = tmp.replace(Vars.OBJWRITE.toString(), Long.toString(master.getObjectsReceived()));
        tmp = tmp.replace(Vars.OBJREAD.toString(), Long.toString(master.getObjectsSent()));
        tmp = tmp.replace(Vars.BYTETX.toString(), OutputUtils
                .formatBytes(master.getBytesSent()));
        tmp = tmp.replace(Vars.BYTERX.toString(), OutputUtils
                .formatBytes(master.getBytesReceived()));
        tmp = tmp.replace(Vars.REPLOBJWRITE.toString(), Long.toString(master.getReplicatedObjectsReceived()));
        tmp = tmp.replace(Vars.REPLBYTERX.toString(), OutputUtils
                .formatBytes(master.getReplicatedBytesReceived()));
        tmp = tmp
                .replace(Vars.DELETES.toString(), Long.toString(master.getDeletionStage().getNumFilesDeleted()));
        tmp = tmp.replace(Vars.GLOBALTIME.toString(), new Date(globalTime).toString() + " ("
            + globalTime + ")");
        tmp = tmp.replace(Vars.GLOBALRESYNC.toString(), Long.toString(TimeSync
                .getTimeSyncInterval()));
        tmp = tmp.replace(Vars.LOCALTIME.toString(), new Date(localTime).toString() + " ("
            + localTime + ")");
        tmp = tmp.replace(Vars.LOCALRESYNC.toString(), Long.toString(TimeSync
                .getLocalRenewInterval()));
        tmp = tmp.replace(Vars.UUIDCACHE.toString(), UUIDResolver.getCache());
        tmp = tmp.replace(Vars.VERSION.toString(), VersionManagement.RELEASE_VERSION);
        tmp = tmp.replace(Vars.PROTOVERSION.toString(), Integer.toString(OSDServiceConstants.INTERFACE_ID));
        /*tmp = tmp.replace(Vars.STATCOLLECT.toString(), "basic stats: "+(StatisticsStage.collect_statistics ? "enabled" : "disabled")+
                "<BR>per stage request details: "+(StatisticsStage.measure_request_times ? "enabled" : "disabled"));*/

        long freeMem = Runtime.getRuntime().freeMemory();
        String span = "<span>";
        if (freeMem < 1024 * 1024 * 32) {
            span = "<span class=\"levelWARN\">";
        } else if (freeMem < 1024 * 1024 * 2) {
            span = "<span class=\"levelERROR\">";
        }
        tmp = tmp.replace(Vars.MEMSTAT.toString(), span + OutputUtils.formatBytes(freeMem) + " / "
            + OutputUtils.formatBytes(Runtime.getRuntime().maxMemory()) + " / "
            + OutputUtils.formatBytes(Runtime.getRuntime().totalMemory()) + "</span>");

        long freeDisk = ((OSDRequestDispatcher) master).getFreeSpace();

        span = "<span>";
        if (freeDisk < 1024 * 1024 * 1024 * 2) {
            span = "<span class=\"levelWARN\">";
        } else if (freeDisk < 1024 * 1024 * 512) {
            span = "<span class=\"levelERROR\">";
        }
        tmp = tmp.replace(Vars.DISKFREE.toString(), span + OutputUtils.formatBytes(freeDisk)
            + "</span>");

        return tmp;

    }

}
