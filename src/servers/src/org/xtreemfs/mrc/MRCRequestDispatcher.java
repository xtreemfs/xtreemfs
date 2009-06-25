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

package org.xtreemfs.mrc;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.HeartbeatThread.ServiceDataGenerator;
import org.xtreemfs.common.auth.AuthenticationProvider;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.dir.discovery.DiscoveryUtils;
import org.xtreemfs.foundation.CrashReporter;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.oncrpc.server.RPCServerRequestListener;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.DirService;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceDataMap;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.interfaces.MRCInterface.MRCException;
import org.xtreemfs.interfaces.MRCInterface.MRCInterface;
import org.xtreemfs.interfaces.MRCInterface.ProtocolException;
import org.xtreemfs.interfaces.MRCInterface.errnoException;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.DBAccessResultListener;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.operations.StatusPageOperation;
import org.xtreemfs.mrc.operations.StatusPageOperation.Vars;
import org.xtreemfs.mrc.osdselection.OSDStatusManager;
import org.xtreemfs.mrc.stages.ProcessingStage;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.volumes.BabuDBVolumeManager;
import org.xtreemfs.mrc.volumes.VolumeManager;
import org.xtreemfs.mrc.volumes.metadata.VolumeInfo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * 
 * @author bjko
 */
public class MRCRequestDispatcher implements RPCServerRequestListener, LifeCycleListener,
    DBAccessResultListener {
    
    public final static String           VERSION            = "1.0.0 (v1.0 RC2)";
    
    private static final int             RPC_TIMEOUT        = 10000;
    
    private static final int             CONNECTION_TIMEOUT = 5 * 60 * 1000;
    
    private final RPCNIOSocketServer     serverStage;
    
    private final RPCNIOSocketClient     clientStage;
    
    private final DIRClient              dirClient;
    
    private final ProcessingStage        procStage;
    
    private final OSDStatusManager       osdMonitor;
    
    private final PolicyContainer        policyContainer;
    
    private final AuthenticationProvider authProvider;
    
    private final MRCConfig              config;
    
    private final HeartbeatThread        heartbeatThread;
    
    private final VolumeManager          volumeManager;
    
    private final FileAccessManager      fileAccessManager;
    
    private final HttpServer             httpServ;
    
    public MRCRequestDispatcher(final MRCConfig config) throws IOException, ClassNotFoundException,
        IllegalAccessException, InstantiationException, DatabaseException {
        
        this.config = config;
        
        if (this.config.getDirectoryService().getHostName().equals(DiscoveryUtils.AUTODISCOVER_HOSTNAME)) {
            Logging.logMessage(Logging.LEVEL_INFO, Category.net, this,
                "trying to discover local XtreemFS DIR service...");
            DirService dir = DiscoveryUtils.discoverDir(10);
            if (dir == null) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.net, this,
                    "CANNOT FIND XtreemFS DIR service via discovery broadcasts... no response");
                throw new IOException("no DIR service found via discovery broadcast");
            }
            Logging.logMessage(Logging.LEVEL_INFO, Category.net, this, "found XtreemFS DIR service at "
                + dir.getAddress() + ":" + dir.getPort());
            config.setDirectoryService(new InetSocketAddress(dir.getAddress(), dir.getPort()));
        }
        
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, this, "use SSL=%b", config.isUsingSSL());
        
        SSLOptions sslOptions = config.isUsingSSL() ? new SSLOptions(new FileInputStream(config
                .getServiceCredsFile()), config.getServiceCredsPassphrase(), config
                .getServiceCredsContainer(), new FileInputStream(config.getTrustedCertsFile()), config
                .getTrustedCertsPassphrase(), config.getTrustedCertsContainer(), false) : null;
        
        clientStage = new RPCNIOSocketClient(sslOptions, RPC_TIMEOUT, CONNECTION_TIMEOUT);
        clientStage.setLifeCycleListener(this);
        
        serverStage = new RPCNIOSocketServer(config.getPort(), config.getAddress(), this, sslOptions);
        serverStage.setLifeCycleListener(this);
        
        dirClient = new DIRClient(clientStage, config.getDirectoryService());
        
        policyContainer = new PolicyContainer(config);
        authProvider = policyContainer.getAuthenticationProvider();
        authProvider.initialize(config.isUsingSSL());
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, this, "using authentication provider '%s'",
                authProvider.getClass().getName());
        
        osdMonitor = new OSDStatusManager(config, dirClient, policyContainer);
        osdMonitor.setLifeCycleListener(this);
        
        procStage = new ProcessingStage(this);
        
        volumeManager = new BabuDBVolumeManager(this);
        fileAccessManager = new FileAccessManager(volumeManager, policyContainer);
        
        ServiceDataGenerator gen = new ServiceDataGenerator() {
            public ServiceSet getServiceData() {
                
                String uuid = config.getUUID().toString();
                
                OperatingSystemMXBean osb = ManagementFactory.getOperatingSystemMXBean();
                String load = String.valueOf((int) (osb.getSystemLoadAverage() * 100 / osb
                        .getAvailableProcessors()));
                
                long totalRAM = Runtime.getRuntime().maxMemory();
                long usedRAM = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                
                // get service data
                ServiceSet sregs = new ServiceSet();
                ServiceDataMap dmap = new ServiceDataMap();
                dmap.put("load", load);
                dmap.put("proto_version", Integer.toString(MRCInterface.getVersion()));
                dmap.put("totalRAM", Long.toString(totalRAM));
                dmap.put("usedRAM", Long.toString(usedRAM));
                dmap.put("geoCoordinates", config.getGeoCoordinates());
                try {
                    dmap.put("status_page_url", "http://"
                        + config.getUUID().getAddress().getAddress().getHostAddress() + ":"
                        + config.getHttpPort());
                } catch (UnknownUUIDException ex) {
                    // should never happen
                }
                
                Service mrcReg = new Service(ServiceType.SERVICE_TYPE_MRC, uuid, 0, "MRC @ " + uuid, 0, dmap);
                sregs.add(mrcReg);
                
                try {
                    for (VolumeInfo vol : volumeManager.getVolumes()) {
                        Service dsVolumeInfo = MRCHelper.createDSVolumeInfo(vol, osdMonitor, uuid);
                        sregs.add(dsVolumeInfo);
                    }
                } catch (DatabaseException exc) {
                    Logging.logError(Logging.LEVEL_ERROR, this, exc);
                }
                
                return sregs;
            }
            
        };
        
        final StatusPageOperation status = new StatusPageOperation(this);
        
        httpServ = HttpServer.create(new InetSocketAddress(config.getHttpPort()), 0);
        httpServ.createContext("/", new HttpHandler() {
            public void handle(HttpExchange httpExchange) throws IOException {
                byte[] content;
                try {
                    
                    content = status.getStatusPage().getBytes("ascii");
                    httpExchange.sendResponseHeaders(200, content.length);
                    httpExchange.getResponseBody().write(content);
                    httpExchange.getResponseBody().close();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    httpExchange.sendResponseHeaders(500, 0);
                }
                
            }
        });
        httpServ.start();
        
        heartbeatThread = new HeartbeatThread("MRC Heartbeat Thread", dirClient, config.getUUID(), gen,
            config, false);
    }
    
    public void asyncShutdown() {
        heartbeatThread.shutdown();
        
        serverStage.shutdown();
        
        clientStage.shutdown();
        
        osdMonitor.shutdown();
        
        procStage.shutdown();
        
        UUIDResolver.shutdown();
        
        volumeManager.shutdown();
        
        httpServ.stop(0);
        
        TimeSync.getInstance().shutdown();
    }
    
    public void startup() {
        
        try {
            TimeSync.initializeLocal(config.getRemoteTimeSync(), config.getLocalClockRenew());
            
            clientStage.start();
            clientStage.waitForStartup();
            
            UUIDResolver.start(dirClient, 10 * 1000, 600 * 1000);
            UUIDResolver.addLocalMapping(config.getUUID(), config.getPort(),
                config.isUsingSSL() ? Constants.ONCRPCS_SCHEME : Constants.ONCRPC_SCHEME);
            
            heartbeatThread.initialize();
            heartbeatThread.start();
            
            // TimeSync.getInstance().enableRemoteSynchronization(dirClient);
            // XXX
            osdMonitor.start();
            osdMonitor.waitForStartup();
            
            procStage.start();
            procStage.waitForStartup();
            
            volumeManager.init();
            volumeManager.addVolumeChangeListener(osdMonitor);
            
            serverStage.start();
            serverStage.waitForStartup();
            
            if (Logging.isInfo())
                Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this,
                    "MRC operational, listening on port %d", config.getPort());
            
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "STARTUP FAILED!");
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            System.exit(1);
        }
    }
    
    public void shutdown() throws Exception {
        
        heartbeatThread.shutdown();
        heartbeatThread.waitForShutdown();
        
        serverStage.shutdown();
        serverStage.waitForShutdown();
        
        clientStage.shutdown();
        clientStage.waitForShutdown();
        
        osdMonitor.shutdown();
        osdMonitor.waitForShutdown();
        
        procStage.shutdown();
        procStage.waitForShutdown();
        
        UUIDResolver.shutdown();
        
        volumeManager.shutdown();
        
        httpServ.stop(0);
        
        TimeSync.getInstance().shutdown();
        
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this, "MRC shutdown complete");
    }
    
    public void requestFinished(MRCRequest request) {
        // send response back to client, if a pinky request is present
        assert (request != null);
        
        final ONCRPCRequest rpcRequest = request.getRPCRequest();
        assert (rpcRequest != null);
        
        if (request.getError() != null) {
            
            final ErrorRecord error = request.getError();
            switch (error.getErrorClass()) {
            
            case INTERNAL_SERVER_ERROR: {
                Logging.logMessage(Logging.LEVEL_ERROR, this, "%s / request: %s", error.getErrorMessage(),
                    request.toString());
                Logging.logError(Logging.LEVEL_ERROR, this, error.getThrowable());
                rpcRequest.sendInternalServerError(error.getThrowable(), new errnoException());
                break;
            }
            case USER_EXCEPTION: {
                MRCException exc = new MRCException(error.getErrorCode(), error.getErrorMessage(), "");
                rpcRequest.sendException(exc);
                if (Logging.isDebug()) {
                    Logging.logUserError(Logging.LEVEL_DEBUG, Category.proc, this, exc);
                }
                break;
            }
            case INVALID_ARGS: {
                rpcRequest.sendGarbageArgs(error.getErrorMessage(), new ProtocolException());
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "invalid request arguments");
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, error.getErrorMessage());
                }
                break;
            }
            case UNKNOWN_OPERATION: {
                rpcRequest.sendException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_PROC_UNAVAIL,
                    ErrNo.EINVAL, error.getErrorMessage() + " " + error.getStackTrace()));
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "unknown operation: %d",
                        request.getRPCRequest().getRequestHeader().getTag());
                break;
            }
                
            default: {
                Logging.logMessage(Logging.LEVEL_ERROR, this, "some unexpected exception occurred");
                Logging.logError(Logging.LEVEL_ERROR, this, error.getThrowable());
                rpcRequest.sendInternalServerError(error.getThrowable(), new errnoException());
                break;
            }
            }
        }

        else {
            assert (request.getResponse() != null);
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                    "sending response for request %d", request.getRPCRequest().getRequestHeader().getXID());
                Logging
                        .logMessage(Logging.LEVEL_DEBUG, Category.proc, this, request.getResponse()
                                .toString());
            }
            rpcRequest.sendResponse(request.getResponse());
        }
        
    }
    
    public Map<StatusPageOperation.Vars, String> getStatusInformation() {
        HashMap<StatusPageOperation.Vars, String> data = new HashMap<StatusPageOperation.Vars, String>();
        
        data.put(Vars.AVAILPROCS, String.valueOf(Runtime.getRuntime().availableProcessors()));
        data.put(Vars.BPSTATS, BufferPool.getStatus());
        data.put(Vars.DEBUG, Integer.toString(config.getDebugLevel()));
        data.put(Vars.DIRURL, "oncrpc://" + config.getDirectoryService().getHostName() + ":"
            + config.getDirectoryService().getPort());
        data.put(Vars.GLOBALRESYNC, Long.toString(TimeSync.getTimeSyncInterval()));
        
        final long globalTime = TimeSync.getGlobalTime();
        final long localTime = TimeSync.getLocalSystemTime();
        data.put(Vars.GLOBALTIME, new Date(globalTime).toString() + " (" + globalTime + ")");
        data.put(Vars.LOCALTIME, new Date(localTime).toString() + " (" + localTime + ")");
        data.put(Vars.LOCALRESYNC, Long.toString(TimeSync.getLocalRenewInterval()));
        
        data.put(Vars.PORT, Integer.toString(config.getPort()));
        
        data.put(Vars.UUID, config.getUUID().toString());
        data.put(Vars.UUIDCACHE, UUIDResolver.getCache());
        data.put(Vars.PROTOVERSION, Integer.toString(MRCInterface.getVersion()));
        data.put(Vars.VERSION, VERSION);
        
        data.put(Vars.PINKYQ, Long.toString(this.serverStage.getPendingRequests()));
        data.put(Vars.NUMCON, Integer.toString(this.serverStage.getNumConnections()));
        
        long freeMem = Runtime.getRuntime().freeMemory();
        String span = "<span>";
        if (freeMem < 1024 * 1024 * 32) {
            span = "<span class=\"levelWARN\">";
        } else if (freeMem < 1024 * 1024 * 2) {
            span = "<span class=\"levelERROR\">";
        }
        data.put(Vars.MEMSTAT, span + OutputUtils.formatBytes(freeMem) + " / "
            + OutputUtils.formatBytes(Runtime.getRuntime().maxMemory()) + " / "
            + OutputUtils.formatBytes(Runtime.getRuntime().totalMemory()) + "</span>");
        
        StringBuffer rqTableBuf = new StringBuffer();
        long totalRequests = 0;
        for (Entry<Integer, Integer> entry : procStage.get_opCountMap().entrySet()) {
            
            long count = entry.getValue();
            totalRequests += count;
            
            if (count != 0) {
                
                try {
                    String req = MRCInterface.createRequest(new ONCRPCRequestHeader(0, 0, 0, entry.getKey()))
                            .getTypeName();
                    req = req.substring(req.lastIndexOf("::") + 2, req.lastIndexOf("Request"));
                    
                    rqTableBuf.append("<tr><td align=\"left\">'");
                    rqTableBuf.append(req);
                    rqTableBuf.append("'</td><td>");
                    rqTableBuf.append(count);
                    rqTableBuf.append("</td></tr>");
                    
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        
        data.put(Vars.TOTALNUMRQ, totalRequests + "");
        data.put(Vars.RQSTATS, rqTableBuf.toString());
        
        // add volume statistics
        try {
            StringBuffer volTableBuf = new StringBuffer();
            Collection<VolumeInfo> vols = volumeManager.getVolumes();
            for (VolumeInfo v : vols) {
                
                ServiceSet osdList = osdMonitor.getUsableOSDs(v.getId());
                StorageManager sMan = volumeManager.getStorageManager(v.getId());
                
                volTableBuf.append("<tr><td align=\"left\">");
                volTableBuf.append(v.getName());
                volTableBuf
                        .append("</td><td><table border=\"0\" cellpadding=\"0\"><tr><td class=\"subtitle\">selectable OSDs</td><td align=\"right\">");
                Iterator<Service> it = osdList.iterator();
                while (it.hasNext()) {
                    Service osd = it.next();
                    final ServiceUUID osdUUID = new ServiceUUID(osd.getUuid());
                    volTableBuf.append(osdUUID);
                    if (it.hasNext())
                        volTableBuf.append(", ");
                }
                
                StripingPolicy defaultSP = volumeManager.getStorageManager(v.getId())
                        .getDefaultStripingPolicy(1);
                
                volTableBuf.append("</td></tr><tr><td class=\"subtitle\">striping policy</td><td>");
                volTableBuf.append(Converter.stripingPolicyToString(defaultSP));
                volTableBuf.append("</td></tr><tr><td class=\"subtitle\">access policy</td><td>");
                volTableBuf.append(v.getAcPolicyId());
                volTableBuf.append("</td></tr><tr><td class=\"subtitle\">osd policy</td><td>");
                volTableBuf.append(v.getOsdPolicyId());
                volTableBuf.append("</td></tr><tr><td class=\"subtitle\">replica policy</td><td>");
                volTableBuf.append(v.getReplicaPolicyId());
                volTableBuf.append("</td></tr><tr><td class=\"subtitle\">#files</td><td>");
                volTableBuf.append(sMan.getNumFiles());
                volTableBuf.append("</td></tr><tr></tr><tr><td class=\"subtitle\">#directories</td><td>");
                volTableBuf.append(sMan.getNumDirs());
                volTableBuf.append("</td></tr><tr><td class=\"subtitle\">free disk space:</td><td>");
                volTableBuf.append(OutputUtils.formatBytes(osdMonitor.getFreeSpace(v.getId())));
                volTableBuf.append("</td></tr><tr><td class=\"subtitle\">occupied disk space:</td><td>");
                volTableBuf.append(OutputUtils.formatBytes(sMan.getVolumeSize()));
                volTableBuf.append("</td></tr></table></td></tr>");
            }
            
            data.put(Vars.VOLUMES, volTableBuf.toString());
        } catch (Exception exc) {
            data.put(Vars.VOLUMES,
                "<tr><td align=\"left\">could not retrieve volume info due to an server internal error: "
                    + exc + "</td></tr>");
        }
        
        return data;
    }
    
    public VolumeManager getVolumeManager() {
        return volumeManager;
    }
    
    public FileAccessManager getFileAccessManager() {
        return fileAccessManager;
    }
    
    public AuthenticationProvider getAuthProvider() {
        return authProvider;
    }
    
    public OSDStatusManager getOSDStatusManager() {
        return osdMonitor;
    }
    
    public PolicyContainer getPolicyContainer() {
        return policyContainer;
    }
    
    public DIRClient getDirClient() {
        return dirClient;
    }
    
    public MRCConfig getConfig() {
        return config;
    }
    
    public void startupPerformed() {
    }
    
    public void shutdownPerformed() {
    }
    
    public void crashPerformed(Throwable cause) {
        CrashReporter.reportXtreemFSCrash("MRC", this.VERSION, cause);
        try {
            shutdown();
        } catch (Exception e) {
            Logging.logError(Logging.LEVEL_ERROR, this, e);
            System.exit(1);
        }
    }
    
    @Override
    public void insertFinished(Object context) {
        requestFinished((MRCRequest) context);
    }
    
    @Override
    public void lookupFinished(Object context, byte[] value) {
        
    }
    
    @Override
    public void prefixLookupFinished(Object context, Iterator<Entry<byte[], byte[]>> iterator) {
        
    }
    
    @Override
    public void userDefinedLookupFinished(Object context, Object result) {
        
    }
    
    @Override
    public void requestFailed(Object context, Throwable error) {
        
    }
    
    @Override
    public void receiveRecord(ONCRPCRequest rq) {
        
        final ONCRPCRequestHeader hdr = rq.getRequestHeader();
        
        if (hdr.getInterfaceVersion() != MRCInterface.getVersion()) {
            rq.sendException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_PROG_MISMATCH,
                ErrNo.EINVAL, "invalid version requested"));
            
            return;
        }
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "enqueueing request: %s", rq
                    .toString());
        
        // no callback, special stage which executes the operatios
        procStage.enqueueOperation(new MRCRequest(rq), ProcessingStage.STAGEOP_PARSE_AND_EXECUTE, null);
    }
    
}
