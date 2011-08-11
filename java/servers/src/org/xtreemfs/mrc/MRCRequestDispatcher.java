/*
 * Copyright (c) 2008 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.HeartbeatThread.ServiceDataGenerator;
import org.xtreemfs.common.auth.AuthenticationProvider;
import org.xtreemfs.common.monitoring.StatusMonitor;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.dir.discovery.DiscoveryUtils;
import org.xtreemfs.foundation.CrashReporter;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeServerClient;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.VersionManagement;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.MessageType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;
import org.xtreemfs.foundation.pbrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequestListener;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.DBAccessResultListener;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.database.babudb.BabuDBVolumeManager;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.operations.StatusPageOperation;
import org.xtreemfs.mrc.operations.StatusPageOperation.Vars;
import org.xtreemfs.mrc.osdselection.OSDStatusManager;
import org.xtreemfs.mrc.stages.OnCloseReplicationThread;
import org.xtreemfs.mrc.stages.ProcessingStage;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.DirService;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceDataMap;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.globalTimeSGetResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * 
 * @author bjko
 */
public class MRCRequestDispatcher implements RPCServerRequestListener, LifeCycleListener,
    DBAccessResultListener<Object> {
    
    private static final int               RPC_TIMEOUT        = 10000;
    
    private static final int               CONNECTION_TIMEOUT = 5 * 60 * 1000;
    
    private final RPCNIOSocketServer       serverStage;
    
    private final RPCNIOSocketClient       clientStage;
    
    private final DIRServiceClient         dirClient;
    
    private final ProcessingStage          procStage;
    
    private final OSDStatusManager         osdMonitor;
    
    private final MRCPolicyContainer       policyContainer;
    
    private final AuthenticationProvider   authProvider;
    
    private final MRCConfig                config;
    
    private final HeartbeatThread          heartbeatThread;
    
    private final OnCloseReplicationThread onCloseReplicationThread;
    
    private final VolumeManager            volumeManager;
    
    private final FileAccessManager        fileAccessManager;
    
    private final HttpServer               httpServ;
    
    private final OSDServiceClient         osdClient;

    private List<MRCStatusListener>        statusListener;

    public MRCRequestDispatcher(final MRCConfig config, BabuDBConfig dbConfig) throws IOException,
        ClassNotFoundException, IllegalAccessException, InstantiationException, DatabaseException {
        
        Logging.logMessage(Logging.LEVEL_INFO, this, "XtreemFS Metadata Service version "
            + VersionManagement.RELEASE_VERSION);
        
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
        
        policyContainer = new MRCPolicyContainer(config);
        
        if (Logging.isInfo() && config.isUsingSSL() && policyContainer.getTrustManager() != null)
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, this, "using custom trust manager '%s'",
                policyContainer.getTrustManager().getClass().getName());
        
        SSLOptions sslOptions = config.isUsingSSL() ? new SSLOptions(new FileInputStream(config
                .getServiceCredsFile()), config.getServiceCredsPassphrase(), config
                .getServiceCredsContainer(), new FileInputStream(config.getTrustedCertsFile()), config
                .getTrustedCertsPassphrase(), config.getTrustedCertsContainer(), false, config
                .isGRIDSSLmode(), policyContainer.getTrustManager()) : null;
        
        clientStage = new RPCNIOSocketClient(sslOptions, RPC_TIMEOUT, CONNECTION_TIMEOUT);
        clientStage.setLifeCycleListener(this);
        
        serverStage = new RPCNIOSocketServer(config.getPort(), config.getAddress(), this, sslOptions);
        serverStage.setLifeCycleListener(this);
        
        dirClient = new DIRServiceClient(clientStage, config.getDirectoryService());
        osdClient = new OSDServiceClient(clientStage, null);
        
        authProvider = policyContainer.getAuthenticationProvider();
        authProvider.initialize(config.isUsingSSL());
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, this, "using authentication provider '%s'",
                authProvider.getClass().getName());
        
        osdMonitor = new OSDStatusManager(this);
        osdMonitor.setLifeCycleListener(this);
        
        procStage = new ProcessingStage(this);
        
        volumeManager = new BabuDBVolumeManager(this, dbConfig);
        fileAccessManager = new FileAccessManager(volumeManager, policyContainer);
        
        statusListener = new ArrayList<MRCStatusListener>();
        if (config.isUsingSnmp()) {
            statusListener.add(new StatusMonitor(
                    this, config.getSnmpAddress(), config.getSnmpPort(), config.getSnmpACLFile()));
            notifyConfigurationChange();
        }
        
        
        ServiceDataGenerator gen = new ServiceDataGenerator() {
            public ServiceSet getServiceData() {
                
                String uuid = config.getUUID().toString();
                
                OperatingSystemMXBean osb = ManagementFactory.getOperatingSystemMXBean();
                String load = String.valueOf((int) (osb.getSystemLoadAverage() * 100 / osb
                        .getAvailableProcessors()));
                
                long totalRAM = Runtime.getRuntime().maxMemory();
                long usedRAM = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                
                // get service data
                ServiceDataMap.Builder dmap = ServiceDataMap.newBuilder();
                dmap.addData(KeyValuePair.newBuilder().setKey("load").setValue(load).build());
                dmap.addData(KeyValuePair.newBuilder().setKey("proto_version").setValue(
                    Integer.toString(MRCServiceConstants.INTERFACE_ID)).build());
                dmap.addData(KeyValuePair.newBuilder().setKey("totalRAM").setValue(Long.toString(totalRAM))
                        .build());
                dmap.addData(KeyValuePair.newBuilder().setKey("usedRAM").setValue(Long.toString(usedRAM))
                        .build());
                dmap.addData(KeyValuePair.newBuilder().setKey("geoCoordinates").setValue(
                    config.getGeoCoordinates()).build());
                
                try {
                    final String address = "".equals(config.getHostName()) ? config.getAddress() == null ? config
                            .getUUID().getMappings()[0].resolvedAddr.getAddress().getHostAddress()
                        : config.getAddress().getHostAddress()
                        : config.getHostName();
                    dmap.addData(KeyValuePair.newBuilder().setKey("status_page_url").setValue(
                        "http://" + address + ":" + config.getHttpPort()));
                } catch (UnknownUUIDException ex) {
                    // should never happen
                    Logging.logError(Logging.LEVEL_ERROR, this, ex);
                }
                
                Service mrcReg = Service.newBuilder().setType(ServiceType.SERVICE_TYPE_MRC).setUuid(uuid)
                        .setData(dmap).setVersion(0).setLastUpdatedS(0).setName("MRC @ " + uuid).build();
                // (ServiceType.SERVICE_TYPE_MRC, uuid, 0, "MRC @ " + uuid, 0,
                // dmap);
                ServiceSet.Builder sregs = ServiceSet.newBuilder().addServices(mrcReg);
                
                for (StorageManager sMan : volumeManager.getStorageManagers()) {
                    VolumeInfo vol = sMan.getVolumeInfo();
                    Service dsVolumeInfo = MRCHelper.createDSVolumeInfo(vol, osdMonitor, sMan, uuid);
                    sregs.addServices(dsVolumeInfo);
                }
                
                return sregs.build();
            }
            
        };
        
        final StatusPageOperation status = new StatusPageOperation(this);
        
        httpServ = HttpServer.create(new InetSocketAddress(config.getHttpPort()), 0);
        final HttpContext ctx = httpServ.createContext("/", new HttpHandler() {
            public void handle(HttpExchange httpExchange) throws IOException {
                byte[] content;
                try {
                    if (httpExchange.getRequestURI().getPath().contains("strace")) {
                        content = OutputUtils.getThreadDump().getBytes("ascii");
                    } else {
                        content = status.getStatusPage().getBytes("ascii");
                    }
                    httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                    httpExchange.sendResponseHeaders(200, content.length);
                    httpExchange.getResponseBody().write(content);
                    httpExchange.getResponseBody().close();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    httpExchange.sendResponseHeaders(500, 0);
                }
                
            }
        });
        
        if (config.getAdminPassword().length() > 0) {
            ctx.setAuthenticator(new BasicAuthenticator("XtreemFS MRC " + config.getUUID()) {
                @Override
                public boolean checkCredentials(String arg0, String arg1) {
                    return (arg0.equals("admin") && arg1.equals(config.getAdminPassword()));
                }
            });
        }
        
        httpServ.start();
        
        heartbeatThread = new HeartbeatThread("MRC Heartbeat Thread", dirClient, config.getUUID(), gen,
            config, false);
        
        onCloseReplicationThread = new OnCloseReplicationThread(this);
    }
    
    public void asyncShutdown() {
        
        onCloseReplicationThread.shutdown();
        
        heartbeatThread.shutdown();
        
        serverStage.shutdown();
        
        clientStage.shutdown();
        
        osdMonitor.shutdown();
        
        procStage.shutdown();
        
        UUIDResolver.shutdown();
        
        volumeManager.shutdown();
        
        httpServ.stop(0);
        
        // TimeSync.getInstance().shutdown();
    }
    
    public void startup() {
        
        try {
            
            TimeSync.initialize(new TimeServerClient() {
                
                @Override
                public long xtreemfs_global_time_get(InetSocketAddress server) {
                    
                    RPCResponse<globalTimeSGetResponse> resp = null;
                    try {
                        resp = dirClient.xtreemfs_global_time_s_get(server, RPCAuthentication.authNone,
                            RPCAuthentication.userService);
                        return resp.get().getTimeInSeconds();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return -1;
                    } finally {
                        if (resp != null)
                            resp.freeBuffers();
                    }
                }
            }, config.getRemoteTimeSync(), config.getLocalClockRenew());
            
            clientStage.start();
            clientStage.waitForStartup();
            
            UUIDResolver.start(dirClient, 10 * 1000, 600 * 1000);
            UUIDResolver.addLocalMapping(config.getUUID(), config.getPort(), Schemes.getScheme(config
                    .isUsingSSL(), config.isGRIDSSLmode()));
            UUIDResolver.addLocalMapping(config.getUUID(), config.getPort(), Schemes.SCHEME_PBRPCU);
            
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
            
            onCloseReplicationThread.start();
            onCloseReplicationThread.waitForStartup();
            
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
        
        onCloseReplicationThread.shutdown();
        onCloseReplicationThread.waitForShutdown();
        
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
        
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this, "MRC shutdown complete");
    }
    
    public void requestFinished(MRCRequest request) {
        // send response back to client, if a pinky request is present
        assert (request != null);
        
        final RPCServerRequest rpcRequest = request.getRPCRequest();
        assert (rpcRequest != null);
        
        if (request.getError() != null) {
            
            final ErrorRecord error = request.getError();
            final String errorMessage = error.getErrorMessage() == null ? "" : error.getErrorMessage();
            
            switch (error.getErrorType()) {
            
            case INTERNAL_SERVER_ERROR: {
                Logging.logMessage(Logging.LEVEL_ERROR, this, "%s / request: %s", errorMessage, request
                        .toString());
                Logging.logError(Logging.LEVEL_ERROR, this, error.getThrowable());
                rpcRequest.sendError(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                    errorMessage, error.getStackTrace());
                break;
            }
            case ERRNO: {
                if (Logging.isDebug()) {
                    Logging.logUserError(Logging.LEVEL_DEBUG, Category.proc, this, error.getThrowable());
                }
                rpcRequest.sendError(ErrorType.ERRNO, error.getErrorCode(), errorMessage, "");
                break;
            }
            case AUTH_FAILED: {
                if (Logging.isDebug()) {
                    Logging.logUserError(Logging.LEVEL_DEBUG, Category.proc, this, error.getThrowable());
                }
                rpcRequest.sendError(ErrorType.AUTH_FAILED, error.getErrorCode(), errorMessage, "");
                break;
            }
            case GARBAGE_ARGS: {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "invalid request arguments");
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, errorMessage);
                }
                rpcRequest.sendError(ErrorType.GARBAGE_ARGS, POSIXErrno.POSIX_ERROR_EINVAL, errorMessage,
                    error.getStackTrace());
                break;
            }
            case INVALID_INTERFACE_ID: {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "invalid interface: %d",
                        request.getRPCRequest().getHeader().getRequestHeader().getInterfaceId());
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, errorMessage);
                }
                rpcRequest.sendError(ErrorType.INVALID_INTERFACE_ID, POSIXErrno.POSIX_ERROR_EINVAL,
                    errorMessage, error.getStackTrace());
                break;
            }
                
            case INVALID_PROC_ID: {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "unknown operation: %d",
                        request.getRPCRequest().getHeader().getRequestHeader().getProcId());
                }
                rpcRequest.sendError(ErrorType.INVALID_PROC_ID, POSIXErrno.POSIX_ERROR_EINVAL, errorMessage,
                    error.getStackTrace());
                break;
            }
                
            case REDIRECT: {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "redirect to: %s",
                        errorMessage);
                }
                rpcRequest.sendRedirect(errorMessage);
                break;
            }
                
            default: {
                Logging.logMessage(Logging.LEVEL_ERROR, this, "some unexpected exception occurred");
                Logging.logError(Logging.LEVEL_ERROR, this, error.getThrowable());
                rpcRequest.sendError(ErrorType.IO_ERROR, POSIXErrno.POSIX_ERROR_EIO, errorMessage, error
                        .getStackTrace());
                break;
            }
            }
        }

        else {
            assert (request.getResponse() != null);
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                    "sending response for request %d", request.getRPCRequest().getHeader().getCallId());
                Logging
                        .logMessage(Logging.LEVEL_DEBUG, Category.proc, this, request.getResponse()
                                .toString());
            }
            
            try {
                rpcRequest.sendResponse(request.getResponse(), null);
            } catch (IOException e) {
                Logging.logError(Logging.LEVEL_ERROR, this, e);
            }
        }

    }

    public int getNumConnections() {
        return this.serverStage.getNumConnections();
    }
    
    public long getNumRequests() {
        return this.serverStage.getPendingRequests();
    }
    
    
    public Map<StatusPageOperation.Vars, String> getStatusInformation() {
        HashMap<StatusPageOperation.Vars, String> data = new HashMap<StatusPageOperation.Vars, String>();
        
        data.put(Vars.AVAILPROCS, String.valueOf(Runtime.getRuntime().availableProcessors()));
        data.put(Vars.BPSTATS, BufferPool.getStatus());
        data.put(Vars.DEBUG, Integer.toString(config.getDebugLevel()));
        data.put(Vars.DIRURL, (config.isUsingSSL() ? (config.isGRIDSSLmode() ? Schemes.SCHEME_PBRPCG
            : Schemes.SCHEME_PBRPCS) : Schemes.SCHEME_PBRPC)
            + "://"
            + config.getDirectoryService().getHostName()
            + ":"
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
        data.put(Vars.PROTOVERSION, Integer.toString(MRCServiceConstants.INTERFACE_ID));
        data.put(Vars.VERSION, VersionManagement.RELEASE_VERSION);
        data.put(Vars.DBVERSION, volumeManager.getDBVersion());
        
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
                    String req = StatusPageOperation.getOpName(entry.getKey());
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
            Collection<StorageManager> sMans = volumeManager.getStorageManagers();
            
            List<VolumeInfo> volumes = new ArrayList<VolumeInfo>(sMans.size());
            for (StorageManager sMan : sMans)
                volumes.add(sMan.getVolumeInfo());
            
            Collections.sort(volumes, new Comparator<VolumeInfo>() {
                public int compare(VolumeInfo o1, VolumeInfo o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            
            boolean first = true;
            for (VolumeInfo v : volumes) {
                
                ServiceSet osdList = osdMonitor.getUsableOSDs(v.getId()).build();
                
                if (!first)
                    volTableBuf.append("<tr><td colspan=\"2\"><hr style=\"height:1px\"/></td></tr>");
                
                volTableBuf.append("<tr><td align=\"left\">");
                volTableBuf.append(v.getName());
                volTableBuf
                        .append("</td><td><table border=\"0\" cellpadding=\"0\"><tr><td class=\"subtitle\">selectable OSDs</td><td align=\"right\">");
                Iterator<Service> it = osdList.getServicesList().iterator();
                while (it.hasNext()) {
                    Service osd = it.next();
                    final ServiceUUID osdUUID = new ServiceUUID(osd.getUuid());
                    volTableBuf.append(osdUUID);
                    if (it.hasNext())
                        volTableBuf.append(", ");
                }
                
                StripingPolicy defaultSP = volumeManager.getStorageManager(v.getId())
                        .getDefaultStripingPolicy(1);
                
                AccessControlPolicyType policy = AccessControlPolicyType.valueOf(v.getAcPolicyId());
                
                volTableBuf.append("</td></tr><tr><td class=\"subtitle\">striping policy</td><td>");
                volTableBuf.append(Converter.stripingPolicyToString(defaultSP));
                volTableBuf.append("</td></tr><tr><td class=\"subtitle\">access policy</td><td>");
                volTableBuf.append(policy != null ? policy.name() : v.getAcPolicyId());
                volTableBuf.append("</td></tr><tr><td class=\"subtitle\">osd policy</td><td>");
                volTableBuf.append(Converter.shortArrayToString(v.getOsdPolicy()));
                volTableBuf.append("</td></tr><tr><td class=\"subtitle\">replica policy</td><td>");
                volTableBuf.append(Converter.shortArrayToString(v.getReplicaPolicy()));
                volTableBuf.append("</td></tr><tr><td class=\"subtitle\">#files</td><td>");
                volTableBuf.append(v.getNumFiles());
                volTableBuf.append("</td></tr><tr></tr><tr><td class=\"subtitle\">#directories</td><td>");
                volTableBuf.append(v.getNumDirs());
                volTableBuf.append("</td></tr><tr><td class=\"subtitle\">free disk space:</td><td>");
                volTableBuf.append(OutputUtils.formatBytes(osdMonitor.getFreeSpace(v.getId())));
                volTableBuf.append("</td></tr><tr><td class=\"subtitle\">occupied disk space:</td><td>");
                volTableBuf.append(OutputUtils.formatBytes(v.getVolumeSize()));
                volTableBuf.append("</td></tr></table></td></tr>");
                
                first = false;
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
    
    public OnCloseReplicationThread getOnCloseReplicationThread() {
        return onCloseReplicationThread;
    }
    
    public MRCPolicyContainer getPolicyContainer() {
        return policyContainer;
    }
    
    public DIRServiceClient getDirClient() {
        return dirClient;
    }
    
    public OSDServiceClient getOSDClient() {
        return osdClient;
    }
    
    public MRCConfig getConfig() {
        return config;
    }
    
    public void startupPerformed() {
    }
    
    public void shutdownPerformed() {
    }
    
    public void crashPerformed(Throwable cause) {
        final String report = CrashReporter
                .createCrashReport("MRC", VersionManagement.RELEASE_VERSION, cause);
        System.out.println(report);
        CrashReporter.reportXtreemFSCrash(report);
        try {
            shutdown();
        } catch (Exception e) {
            Logging.logError(Logging.LEVEL_ERROR, this, e);
            System.exit(1);
        }
    }
    
    @Override
    public void receiveRecord(RPCServerRequest rq) {
        
        // final ONCRPCRequestHeader hdr = rq.getRequestHeader();
        RPCHeader hdr = rq.getHeader();
        
        if (hdr.getMessageType() != MessageType.RPC_REQUEST) {
            rq.sendError(ErrorType.GARBAGE_ARGS, POSIXErrno.POSIX_ERROR_EIO,
                "expected RPC request message type but got " + hdr.getMessageType());
            return;
        }
        
        final RPCHeader.RequestHeader rqHdr = hdr.getRequestHeader();
        
        if (rqHdr.getInterfaceId() != MRCServiceConstants.INTERFACE_ID) {
            rq.sendError(ErrorType.INVALID_INTERFACE_ID, POSIXErrno.POSIX_ERROR_EIO,
                "invalid interface id. Maybe wrong service address/port configured?");
            return;
        }
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "enqueueing request: %s", rq
                    .toString());
        
        // no callback, special stage which executes the operatios
        procStage.enqueueOperation(new MRCRequest(rq), ProcessingStage.STAGEOP_PARSE_AND_EXECUTE, null);
    }
    
    @Override
    public void failed(Throwable error, Object context) {
        MRCRequest request = (MRCRequest) context;
        assert (request != null);
        
        final RPCServerRequest rpcRequest = request.getRPCRequest();
        assert (rpcRequest != null);
        
        if (request.getError() == null)
            request.setError(ErrorType.INTERNAL_SERVER_ERROR, error.getMessage());
        requestFinished(request);
    }
    
    @Override
    public void finished(Object result, Object context) {
        requestFinished((MRCRequest) context);
    }
    
    public void addStatusListener(MRCStatusListener listener) {
        this.statusListener.add(listener);
    }

    public void removeStatusListener(MRCStatusListener listener) {
        this.statusListener.remove(listener);
    }
        
    /**
     * Tells all listeners when the configuration has changed.
     */
    public void notifyConfigurationChange() {
        for (MRCStatusListener listener : statusListener ) {
            listener.MRCConfigChanged(this.config);
        } 
    }
    
    /**
     * Tells all listeners when a Volume was created.
     */
    public void notifyVolumeCreated() {
        for (MRCStatusListener listener : statusListener) {
            listener.volumeCreated();
        }
    }
    
    public void notifyVolumeDeleted() {
        for (MRCStatusListener listener : statusListener) {
            listener.volumeDeleted();
        }
    }
    
    
    /**
     * Getter for a timestamp when the heartbeatthread sent his last heartbeat
     * @return long - timestamp as returned by System.currentTimeMillis()
     */
    public long getLastHeartbeat() {
        return heartbeatThread.getLastHeartbeat();
    }
    
}
