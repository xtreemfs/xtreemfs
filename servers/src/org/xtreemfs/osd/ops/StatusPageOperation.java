/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.osd.ops;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.foundation.pinky.HTTPUtils.DATA_TYPE;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.stages.AuthenticationStage;
import org.xtreemfs.osd.stages.StatisticsStage;

public final class StatusPageOperation extends Operation {

    protected final String statusPageTemplate;

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
            DISKFREE("<!-- $DISKFREE -->");

        private String template;

        Vars(String template) {
            this.template = template;
        }

        public String toString() {
            return template;
        }
    }

    public StatusPageOperation(OSDRequestDispatcher master) {

        super(master);

        StringBuffer sb = null;
        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(
                "/org/xtreemfs/osd/templates/status.html");
            if (is == null)
                is = this.getClass().getClassLoader().getResourceAsStream(
                    "org/xtreemfs/osd/templates/status.html");
            if (is == null)
                is = this.getClass().getResourceAsStream("../templates/status.html");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            sb = new StringBuffer();
            String line = br.readLine();
            while (line != null) {
                sb.append(line + "\n");
                line = br.readLine();
            }
            br.close();
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, ex);
        }
        if (sb == null) {
            statusPageTemplate = "<H1>Template was not found, unable to show status page!</h1>";
        } else {
            statusPageTemplate = sb.toString();
        }
    }

    public void startRequest(OSDRequest rq) {
        PinkyRequest pr = rq.getPinkyRequest();
        //if (pr.requestAuthentication("admin","yagga")) {
            final String html = printStatusPage();
            rq.setData(ReusableBuffer.wrap(html.getBytes()), DATA_TYPE.HTML);
        //}
        finishRequest(rq);
    }

    public String printStatusPage() {

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
        tmp = tmp.replace(Vars.DIRURL.toString(), "http://"
            + master.getConfig().getDirectoryService().getHostName() + ":"
            + master.getConfig().getDirectoryService().getPort());
        tmp = tmp.replace(Vars.DEBUG.toString(), Integer.toString(master.getConfig()
                .getDebugLevel()));
        tmp = tmp.replace(Vars.NUMCON.toString(), Integer.toString(((OSDRequestDispatcher) master)
                .getPinkyCons()));
        tmp = tmp.replace(Vars.PINKYQ.toString(), Integer.toString(((OSDRequestDispatcher) master)
                .getPinkyQueueLength()));
        tmp = tmp.replace(Vars.PARSERQ.toString(), Integer.toString(master.getStage(
            RequestDispatcher.Stages.PARSER).getQueueLength()));
        tmp = tmp.replace(Vars.AUTHQ.toString(), Integer.toString(master.getStage(
            RequestDispatcher.Stages.AUTH).getQueueLength()));
        tmp = tmp.replace(Vars.STORAGEQ.toString(), Integer.toString(master.getStage(
            RequestDispatcher.Stages.STORAGE).getQueueLength()));
        tmp = tmp.replace(Vars.DELETIONQ.toString(), Integer.toString(master.getStage(
            RequestDispatcher.Stages.DELETION).getQueueLength()));
        tmp = tmp.replace(Vars.OPENFILES.toString(), Integer.toString(((AuthenticationStage) master
                .getStage(RequestDispatcher.Stages.AUTH)).getNumOpenFiles()));
        tmp = tmp.replace(Vars.OBJWRITE.toString(), master.getStatistics().numWrites.toString());
        tmp = tmp.replace(Vars.OBJREAD.toString(), master.getStatistics().numReads.toString());
        tmp = tmp.replace(Vars.BYTETX.toString(), OutputUtils
                .formatBytes(master.getStatistics().bytesTX.get()));
        tmp = tmp.replace(Vars.BYTERX.toString(), OutputUtils
                .formatBytes(master.getStatistics().bytesRX.get()));
        tmp = tmp.replace(Vars.GMAXMSG.toString(), master.getStatistics().numGmaxReceived
                .toString());
        tmp = tmp.replace(Vars.GMAXRPC.toString(), master.getStatistics().numGmaxRPCs.toString());
        tmp = tmp
                .replace(Vars.DELETES.toString(), Long.toString(master.getStatistics().numDeletes));
        tmp = tmp.replace(Vars.GLOBALTIME.toString(), new Date(globalTime).toString() + " ("
            + globalTime + ")");
        tmp = tmp.replace(Vars.GLOBALRESYNC.toString(), Long.toString(TimeSync
                .getTimeSyncInterval()));
        tmp = tmp.replace(Vars.LOCALTIME.toString(), new Date(localTime).toString() + " ("
            + localTime + ")");
        tmp = tmp.replace(Vars.LOCALRESYNC.toString(), Long.toString(TimeSync
                .getLocalRenewInterval()));
        tmp = tmp.replace(Vars.UUIDCACHE.toString(), UUIDResolver.getCache());
        tmp = tmp.replace(Vars.STATCOLLECT.toString(), "basic stats: "+(StatisticsStage.collect_statistics ? "enabled" : "disabled")+
                "<BR>per stage request details: "+(StatisticsStage.measure_request_times ? "enabled" : "disabled"));
        
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
