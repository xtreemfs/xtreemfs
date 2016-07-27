/*
 * Copyright (c) 2012 by Bjoern Kolbeck.
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.common.statusserver.StatusServerHelper;
import org.xtreemfs.common.statusserver.StatusServerModule;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.VersionManagement;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

import com.sun.net.httpserver.HttpExchange;

/**
 * OSD summary status page.
 * @author bjko
 */
class StatusPage extends StatusServerModule {

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

    private final String statusPageTemplate;
    
    private OSDRequestDispatcher myDispatcher;

    public StatusPage() {
        StringBuffer sb = StatusServerHelper.readTemplate("org/xtreemfs/osd/templates/status.html");
        if (sb == null) {
            statusPageTemplate = "<h1>Template was not found, unable to show status page!</h1>";
        } else {
            statusPageTemplate = sb.toString();
        }
    }
    
    @Override
    public String getDisplayName() {
        return "OSD Status Summary";
    }

    @Override
    public String getUriPath() {
        return "/";
    }

    @Override
    public boolean isAvailableForService(ServiceType service) {
        return service == ServiceType.SERVICE_TYPE_OSD;
    }

    @Override
    public void initialize(ServiceType service, Object serviceRequestDispatcher) {
        assert(service == ServiceType.SERVICE_TYPE_OSD);
        myDispatcher = (OSDRequestDispatcher) serviceRequestDispatcher;
    }

    @Override
    public void shutdown() {
        // Noop.
    }
    
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        long globalTime = TimeSync.getGlobalTime();
        long localTime = TimeSync.getLocalSystemTime();
        
        Map<Vars, String> values = new HashMap<Vars, String>();
        
        values.put(
                Vars.AVAILPROCS,
                Runtime.getRuntime().availableProcessors() + " bytes");
        values.put(
                Vars.FREEMEM,
                Runtime.getRuntime().freeMemory() + " bytes");
        values.put(
                Vars.MAXMEM,
                Runtime.getRuntime().maxMemory() + " bytes");
        values.put(
                Vars.BPSTATS,
                BufferPool.getStatus());
        values.put(
                Vars.UUID,
                myDispatcher.getConfig().getUUID().toString());
        values.put(
                Vars.PORT,
                Integer.toString(myDispatcher.getConfig().getPort()));
        values.put(
                Vars.DEBUG,
                Integer.toString(myDispatcher.getConfig().getDebugLevel()));
        values.put(
                Vars.NUMCON,
                Integer.toString(myDispatcher.getNumClientConnections()));
        values.put(
                Vars.PINKYQ,
                Long.toString(myDispatcher.getPendingRequests()));
        values.put(
                Vars.PARSERQ,
                Integer.toString(myDispatcher.getPreprocStage().getQueueLength()));
        values.put(
                Vars.STORAGEQ,
                Integer.toString(myDispatcher.getStorageStage().getQueueLength()));
        values.put(
                Vars.DELETIONQ,
                Integer.toString(myDispatcher.getDeletionStage().getQueueLength()));
        values.put(
                Vars.OPENFILES,
                Integer.toString(myDispatcher.getPreprocStage().getNumOpenFiles()));
        values.put(
                Vars.OBJWRITE,
                Long.toString(myDispatcher.getObjectsReceived()));
        values.put(
                Vars.OBJREAD, 
                Long.toString(myDispatcher.getObjectsSent()));
        values.put(
                Vars.BYTETX,
                OutputUtils.formatBytes(myDispatcher.getBytesSent()));
        values.put(
                Vars.BYTERX,
                OutputUtils.formatBytes(myDispatcher.getBytesReceived()));
        values.put(
                Vars.REPLOBJWRITE,
                Long.toString(myDispatcher.getReplicatedObjectsReceived()));
        values.put(
                Vars.REPLBYTERX,
                OutputUtils.formatBytes(myDispatcher.getReplicatedBytesReceived()));
        values.put(
                Vars.DELETES,
                Long.toString(myDispatcher.getDeletionStage().getNumFilesDeleted()));
        values.put(
                Vars.GLOBALTIME,
                new Date(globalTime).toString() + " (" + globalTime + ")");
        values.put(
                Vars.GLOBALRESYNC,
                Long.toString(TimeSync.getTimeSyncInterval()));
        values.put(
                Vars.LOCALTIME,
                new Date(localTime).toString() + " (" + localTime + ")");
        values.put(
                Vars.LOCALRESYNC, 
                Long.toString(TimeSync.getLocalRenewInterval()));
        values.put(
                Vars.UUIDCACHE,
                UUIDResolver.getCache());
        values.put(
                Vars.VERSION,
                VersionManagement.RELEASE_VERSION);
        values.put(
                Vars.PROTOVERSION,
                Integer.toString(OSDServiceConstants.INTERFACE_ID));

        String schema = Schemes.SCHEME_PBRPC;
        if (myDispatcher.getConfig().isUsingSSL()) {
            if (myDispatcher.getConfig().isGRIDSSLmode()) {
                schema = Schemes.SCHEME_PBRPCG;
            } else {
                schema = Schemes.SCHEME_PBRPCS;
            }
        }
        
        values.put(
                Vars.DIRURL,
                schema
                + "://"
                + myDispatcher.getConfig().getDirectoryService().getHostName()
                + ":"
                + myDispatcher.getConfig().getDirectoryService().getPort());


        long freeMem = Runtime.getRuntime().freeMemory();
        String span = "<span>";
        if (freeMem < 1024 * 1024 * 32) {
            span = "<span class=\"levelWARN\">";
        } else if (freeMem < 1024 * 1024 * 2) {
            span = "<span class=\"levelERROR\">";
        }
        values.put(
                Vars.MEMSTAT,
                span
                + OutputUtils.formatBytes(freeMem)
                + " / "
                + OutputUtils.formatBytes(Runtime.getRuntime().maxMemory())
                + " / "
                + OutputUtils.formatBytes(Runtime.getRuntime().totalMemory())
                + "</span>");

        long freeDisk = myDispatcher.getFreeSpace();

        span = "<span>";
        if (freeDisk < 1024 * 1024 * 1024 * 2) {
            span = "<span class=\"levelWARN\">";
        } else if (freeDisk < 1024 * 1024 * 512) {
            span = "<span class=\"levelERROR\">";
        }
        values.put(
                Vars.DISKFREE,
                span
                + OutputUtils.formatBytes(freeDisk)
                + "</span>");
        
        String html = statusPageTemplate;
        for (Vars key : values.keySet()) {
            html = html.replace(key.toString(), values.get(key));
        }
        sendResponse(httpExchange, html);
    }

}
