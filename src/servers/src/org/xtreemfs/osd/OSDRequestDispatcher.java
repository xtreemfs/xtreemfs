/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.osd;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.ServiceAvailability;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.common.HeartbeatThread.ServiceDataGenerator;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.checksums.ChecksumFactory;
import org.xtreemfs.common.checksums.provider.JavaChecksumProvider;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.dir.discovery.DiscoveryUtils;
import org.xtreemfs.foundation.CrashReporter;
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
import org.xtreemfs.interfaces.VivaldiCoordinates;
import org.xtreemfs.interfaces.OSDInterface.OSDInterface;
import org.xtreemfs.interfaces.OSDInterface.errnoException;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_broadcast_gmaxRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_pingRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_pingResponse;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.operations.CheckObjectOperation;
import org.xtreemfs.osd.operations.CleanupGetResultsOperation;
import org.xtreemfs.osd.operations.CleanupGetStatusOperation;
import org.xtreemfs.osd.operations.CleanupIsRunningOperation;
import org.xtreemfs.osd.operations.CleanupStartOperation;
import org.xtreemfs.osd.operations.CleanupStopOperation;
import org.xtreemfs.osd.operations.DeleteOperation;
import org.xtreemfs.osd.operations.EventCloseFile;
import org.xtreemfs.osd.operations.EventWriteObject;
import org.xtreemfs.osd.operations.GetObjectSetOperation;
import org.xtreemfs.osd.operations.InternalGetFileSizeOperation;
import org.xtreemfs.osd.operations.InternalGetGmaxOperation;
import org.xtreemfs.osd.operations.InternalTruncateOperation;
import org.xtreemfs.osd.operations.LocalReadOperation;
import org.xtreemfs.osd.operations.LockAcquireOperation;
import org.xtreemfs.osd.operations.LockCheckOperation;
import org.xtreemfs.osd.operations.LockReleaseOperation;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.operations.ReadOperation;
import org.xtreemfs.osd.operations.ShutdownOperation;
import org.xtreemfs.osd.operations.TruncateOperation;
import org.xtreemfs.osd.operations.WriteOperation;
import org.xtreemfs.osd.stages.DeletionStage;
import org.xtreemfs.osd.stages.PreprocStage;
import org.xtreemfs.osd.stages.ReplicationStage;
import org.xtreemfs.osd.stages.StorageStage;
import org.xtreemfs.osd.stages.VivaldiStage;
import org.xtreemfs.osd.vivaldi.VivaldiNode;
import org.xtreemfs.osd.storage.CleanupThread;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.osd.striping.UDPCommunicator;
import org.xtreemfs.osd.striping.UDPMessage;
import org.xtreemfs.osd.striping.UDPReceiverInterface;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.osd.operations.VivaldiPingOperation;

public class OSDRequestDispatcher implements RPCServerRequestListener, LifeCycleListener,
    UDPReceiverInterface {
    
    protected final Map<Integer, OSDOperation>          operations;
    
    protected final Map<Class, OSDOperation>            internalEvents;
    
    protected final HeartbeatThread                     heartbeatThread;
    
    protected final OSDConfig                           config;

    protected final DIRClient                           dirClient;

    protected final MRCClient                           mrcClient;

    protected final OSDClient                           osdClient;

    protected final OSDClient                           osdClientForReplication;

    protected final RPCNIOSocketClient                  rpcClientForReplication;

    protected final RPCNIOSocketClient                  rpcClient;

    protected final RPCNIOSocketServer                  rpcServer;

    protected long                                      requestId;

    protected String                                    authString;

    protected final PreprocStage                        preprocStage;

    protected final StorageStage                        stStage;
    
    protected final DeletionStage                       delStage;
    
    protected final ReplicationStage                    replStage;
    
    protected final UDPCommunicator                     udpCom;
    
    protected final HttpServer                          httpServ;
    
    protected final long                                startupTime;
    
    protected final AtomicLong                          numBytesTX, numBytesRX, numObjsTX, numObjsRX,
            numReplBytesRX, numReplObjsRX;
    
    protected final VivaldiStage                        vStage;
    
    protected final AtomicReference<VivaldiCoordinates> myCoordinates;
    
    protected final CleanupThread                       cThread;
    
    /**
     * reachability of services
     */
    private final ServiceAvailability                   serviceAvailability;
    
    public OSDRequestDispatcher(final OSDConfig config) throws Exception {
        
        Logging.logMessage(Logging.LEVEL_INFO, this, "XtreemFS OSD version "
            + VersionManagement.RELEASE_VERSION);
        
        this.config = config;
        assert (config.getUUID() != null);
        
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
        
        numBytesTX = new AtomicLong();
        numBytesRX = new AtomicLong();
        numObjsTX = new AtomicLong();
        numObjsRX = new AtomicLong();
        numReplBytesRX = new AtomicLong();
        numReplObjsRX = new AtomicLong();
        
        // initialize the checksum factory
        ChecksumFactory.getInstance().addProvider(new JavaChecksumProvider());
        
        // ---------------------
        // initialize operations
        // ---------------------
        
        // IMPORTANT: the order of operations defined in
        // 'RequestDispatcher.Operations' has to be preserved!
        operations = new HashMap();
        internalEvents = new HashMap<Class, OSDOperation>();
        initializeOperations();
        
        // -------------------------------
        // initialize communication stages
        // -------------------------------
        
        SSLOptions serverSSLopts = config.isUsingSSL() ? new SSLOptions(new FileInputStream(config
                .getServiceCredsFile()), config.getServiceCredsPassphrase(), config
                .getServiceCredsContainer(), new FileInputStream(config.getTrustedCertsFile()), config
                .getTrustedCertsPassphrase(), config.getTrustedCertsContainer(), false, config.isGRIDSSLmode()) : null;
        
        rpcServer = new RPCNIOSocketServer(config.getPort(), config.getAddress(), this, serverSSLopts);
        rpcServer.setLifeCycleListener(this);
        
        final SSLOptions clientSSLopts = config.isUsingSSL() ? new SSLOptions(new FileInputStream(config
                .getServiceCredsFile()), config.getServiceCredsPassphrase(), config
                .getServiceCredsContainer(), new FileInputStream(config.getTrustedCertsFile()), config
                .getTrustedCertsPassphrase(), config.getTrustedCertsContainer(), false, config.isGRIDSSLmode()) : null;
        
        rpcClient = new RPCNIOSocketClient(clientSSLopts, 5000, 5 * 60 * 1000);
        rpcClient.setLifeCycleListener(this);

        // replication uses its own RPCClient with a much higher timeout
        rpcClientForReplication = new RPCNIOSocketClient(clientSSLopts, 30000,
                5 * 60 * 1000);
        rpcClientForReplication.setLifeCycleListener(this);

        // initialize ServiceAvailability
        this.serviceAvailability = new ServiceAvailability();
        
        // --------------------------
        // initialize internal stages
        // --------------------------
        
        MetadataCache metadataCache = new MetadataCache();
        StorageLayout storageLayout = null;
        if (config.getStorageLayout().equalsIgnoreCase(HashStorageLayout.class.getSimpleName())) {
            storageLayout = new HashStorageLayout(config, metadataCache);
        /*} else if (config.getStorageLayout().equalsIgnoreCase(SingleFileStorageLayout.class.getSimpleName())) {
            storageLayout = new SingleFileStorageLayout(config, metadataCache);*/
        } else {
            throw new RuntimeException("unknown storage layout in config file: "+config.getStorageLayout());
        }
        
        udpCom = new UDPCommunicator(config.getPort(), this);
        udpCom.setLifeCycleListener(this);
        
        preprocStage = new PreprocStage(this);
        preprocStage.setLifeCycleListener(this);
        
        stStage = new StorageStage(this, metadataCache, storageLayout, 1);
        stStage.setLifeCycleListener(this);
        
        delStage = new DeletionStage(this, metadataCache, storageLayout);
        delStage.setLifeCycleListener(this);
        
        replStage = new ReplicationStage(this);
        replStage.setLifeCycleListener(this);
        
        // ----------------------------------------
        // initialize TimeSync and Heartbeat thread
        // ----------------------------------------
        
        dirClient = new DIRClient(rpcClient, config.getDirectoryService());
        mrcClient = new MRCClient(rpcClient, null);
        osdClient = new OSDClient(rpcClient);
        osdClientForReplication = new OSDClient(rpcClientForReplication);

        HeartbeatThread.waitForDIR(config.getDirectoryService(),config.getWaitForDIR());

        TimeSync.initialize(dirClient, config.getRemoteTimeSync(), config.getLocalClockRenew());
        UUIDResolver.start(dirClient, 10 * 1000, 600 * 1000);
        UUIDResolver.addLocalMapping(config.getUUID(), config.getPort(),
            config.isUsingSSL() ? Constants.ONCRPCS_SCHEME : Constants.ONCRPC_SCHEME);
        UUIDResolver.addLocalMapping(config.getUUID(), config.getPort(),
            config.isUsingSSL() ? Constants.ONCRPCU_SCHEME : Constants.ONCRPC_SCHEME);
        
        myCoordinates = new AtomicReference<VivaldiCoordinates>();
        
        ServiceDataGenerator gen = new ServiceDataGenerator() {
            public ServiceSet getServiceData() {
                
                OSDConfig config = OSDRequestDispatcher.this.config;
                String freeSpace = "0";
                
                if (config.isReportFreeSpace()) {
                    freeSpace = String.valueOf(FSUtils.getFreeSpace(config.getObjDir()));
                }
                
                String totalSpace = "-1";
                
                try {
                    File f = new File(config.getObjDir());
                    totalSpace = String.valueOf(f.getTotalSpace());
                } catch (Exception ex) {
                }
                
                OperatingSystemMXBean osb = ManagementFactory.getOperatingSystemMXBean();
                String load = String.valueOf((int) (osb.getSystemLoadAverage() * 100 / osb
                        .getAvailableProcessors()));
                
                long totalRAM = Runtime.getRuntime().maxMemory();
                long usedRAM = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                
                ServiceSet data = new ServiceSet();
                
                ServiceDataMap dmap = new ServiceDataMap();
                dmap.put("load", load);
                dmap.put("total", totalSpace);
                dmap.put("free", freeSpace);
                dmap.put("totalRAM", Long.toString(totalRAM));
                dmap.put("usedRAM", Long.toString(usedRAM));
                dmap.put("geoCoordinates", config.getGeoCoordinates());
                dmap.put("proto_version", Integer.toString(OSDInterface.getVersion()));
                VivaldiCoordinates coord = myCoordinates.get();
                if (coord != null) {
                    dmap.put("vivaldi_coordinates", VivaldiNode.coordinatesToString(coord));
                }
                dmap.putAll(config.getCustomParams());

                
                try {
                    final String address = "".equals(config.getHostName()) ? config.getAddress() == null ? config
                            .getUUID().getMappings()[0].resolvedAddr.getAddress().getHostAddress()
                        : config.getAddress().getHostAddress()
                        : config.getHostName();
                    dmap.put("status_page_url", "http://" + address + ":" + config.getHttpPort());
                } catch (UnknownUUIDException ex) {
                    // should never happen
                }
                Service me = new Service(ServiceType.SERVICE_TYPE_OSD, config.getUUID().toString(), 0,
                    "OSD @ " + config.getUUID(), 0, dmap);
                data.add(me);
                
                return data;
            }
        };
        heartbeatThread = new HeartbeatThread("OSD HB Thr", dirClient, config.getUUID(), gen, config, true);
        
        httpServ = HttpServer.create(new InetSocketAddress(config.getHttpPort()), 0);
        final HttpContext ctx = httpServ.createContext("/", new HttpHandler() {
            public void handle(HttpExchange httpExchange) throws IOException {
                byte[] content;
                try {
                    if (httpExchange.getRequestURI().getPath().contains("strace")) {
                        content = OutputUtils.getThreadDump().getBytes("ascii");
                    } else {
                        content = StatusPage.getStatusPage(OSDRequestDispatcher.this).getBytes("ascii");
                    }
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
            ctx.setAuthenticator(new BasicAuthenticator("XtreemFS OSD "+config.getUUID()) {
                @Override
                public boolean checkCredentials(String arg0, String arg1) {
                    return (arg0.equals("admin")&& arg1.equals(config.getAdminPassword()));
                }
            });
        }

        httpServ.start();
        
        startupTime = System.currentTimeMillis();
        
        vStage = new VivaldiStage(this);
        vStage.setLifeCycleListener(this);
        
        cThread = new CleanupThread(this, storageLayout);
        cThread.setLifeCycleListener(this);
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.lifecycle, this, "OSD at %s ready", this
                    .getConfig().getUUID().toString());
    }
    
    public CleanupThread getCleanupThread() {
        return cThread;
    }
    
    public void start() {
        
        try {
            
            rpcServer.start();
            rpcClient.start();
            rpcClientForReplication.start();
            
            rpcServer.waitForStartup();
            rpcClient.waitForStartup();
            
            udpCom.start();
            preprocStage.start();
            delStage.start();
            stStage.start();
            replStage.start();
            vStage.start();
            cThread.start();
            
            udpCom.waitForStartup();
            preprocStage.waitForStartup();
            delStage.waitForStartup();
            stStage.waitForStartup();
            vStage.waitForStartup();
            cThread.waitForStartup();
            
            heartbeatThread.initialize();
            heartbeatThread.start();
            heartbeatThread.waitForStartup();
            
            if (Logging.isInfo())
                Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this,
                    "OSD RequestController and all services operational");
            
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "STARTUP FAILED!");
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            System.exit(1);
        }
        
    }
    
    public void shutdown() {
        
        try {
            
            heartbeatThread.shutdown();
            heartbeatThread.waitForShutdown();
            
            UUIDResolver.shutdown();
            
            rpcServer.shutdown();
            rpcClient.shutdown();
            rpcClientForReplication.shutdown();
            
            rpcServer.waitForShutdown();
            rpcClient.waitForShutdown();
            rpcClientForReplication.waitForShutdown();
            
            serviceAvailability.shutdown();
            
            udpCom.shutdown();
            preprocStage.shutdown();
            delStage.shutdown();
            stStage.shutdown();
            replStage.shutdown();
            vStage.shutdown();
            cThread.shutdown();
            
            udpCom.waitForShutdown();
            preprocStage.waitForShutdown();
            delStage.waitForShutdown();
            stStage.waitForShutdown();
            replStage.waitForShutdown();
            vStage.waitForShutdown();
            cThread.waitForShutdown();
            
            httpServ.stop(0);
            
            if (Logging.isInfo())
                Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this,
                    "OSD and all stages terminated");
            
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "shutdown failed");
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }
    
    public void asyncShutdown() {
        try {
            
            heartbeatThread.shutdown();
            
            UUIDResolver.shutdown();
            
            rpcServer.shutdown();
            rpcClient.shutdown();
            rpcClientForReplication.shutdown();
            
            udpCom.shutdown();
            preprocStage.shutdown();
            delStage.shutdown();
            stStage.shutdown();
            vStage.shutdown();
            cThread.cleanupStop();
            
            httpServ.stop(0);
            
            if (Logging.isInfo())
                Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this,
                    "OSD and all stages terminated");
            
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "shutdown failed");
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }
    
    public OSDOperation getOperation(int procId) {
        return operations.get(procId);
    }
    
    public OSDOperation getInternalEvent(Class clazz) {
        return internalEvents.get(clazz);
    }
    
    public OSDConfig getConfig() {
        return config;
    }
    
    public DIRClient getDIRClient() {
        return dirClient;
    }
    
    public MRCClient getMRCClient() {
        return mrcClient;
    }
    
    public OSDClient getOSDClient() {
        return osdClient;
    }
    
    public OSDClient getOSDClientForReplication() {
        return osdClientForReplication;
    }
    
    public RPCNIOSocketClient getRPCClient() {
        return rpcClient;
    }
    
    public void startupPerformed() {
        
    }
    
    public void shutdownPerformed() {
        
    }
    
    public void crashPerformed(Throwable cause) {
        final String report = CrashReporter.createCrashReport("OSD", VersionManagement.RELEASE_VERSION, cause);
        System.out.println(report);
        CrashReporter.reportXtreemFSCrash(report);
        this.shutdown();
    }
    
    /**
     * Checks if the local OSD is the head OSD in one of the given X-Locations
     * list.
     * 
     * @param xloc
     *            the X-Locations list
     * @return <texttt>true</texttt>, if the local OSD is the head OSD of the
     *         given X-Locations list; <texttt>false</texttt>, otherwise
     */
    public boolean isHeadOSD(XLocations xloc) {
        final ServiceUUID headOSD = xloc.getLocalReplica().getOSDs().get(0);
        return config.getUUID().equals(headOSD);
    }
    
    public long getFreeSpace() {
        return FSUtils.getFreeSpace(config.getObjDir());
    }
    
    public long getTotalSpace() {
        File f = new File(config.getObjDir());
        long s = f.getTotalSpace();
        return s;
    }
    
    @Override
    public void receiveRecord(ONCRPCRequest rq) {
        try {
            OSDRequest request = new OSDRequest(rq);
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "received new request: %s", rq
                        .toString());
            preprocStage.prepareRequest(request, new PreprocStage.ParseCompleteCallback() {
                
                @Override
                public void parseComplete(OSDRequest result, Exception error) {
                    if (error == null) {
                        result.getOperation().startRequest(result);
                    } else {
                        if (error instanceof ONCRPCException)
                            result.sendException((ONCRPCException) error);
                        else
                            result.sendInternalServerError(error);
                    }
                }
            });
        } catch (Exception ex) {
            rq.sendInternalServerError(ex, new errnoException());
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }
    
    int getNumClientConnections() {
        return rpcServer.getNumConnections();
    }
    
    long getPendingRequests() {
        return rpcServer.getPendingRequests();
    }
    
    private void initializeOperations() {
        // register all ops
        OSDOperation op = new ReadOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new WriteOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new DeleteOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new TruncateOperation(this);
        operations.put(op.getProcedureId(), op);
        
        /*
         * op = new KeepFileOpenOperation(this);
         * operations.put(op.getProcedureId(),op);
         */

        op = new InternalGetGmaxOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new InternalTruncateOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new CheckObjectOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new InternalGetFileSizeOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new ShutdownOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new LocalReadOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new CleanupStartOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new CleanupIsRunningOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new CleanupStopOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new CleanupGetStatusOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new CleanupGetResultsOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new GetObjectSetOperation(this);
        operations.put(op.getProcedureId(), op);

        op = new LockAcquireOperation(this);
        operations.put(op.getProcedureId(), op);

        op = new LockCheckOperation(this);
        operations.put(op.getProcedureId(), op);
        
        op = new LockReleaseOperation(this);
        operations.put(op.getProcedureId(), op);

        op = new VivaldiPingOperation(this);
        operations.put(op.getProcedureId(), op);
        
        // --internal events here--
        
        op = new EventCloseFile(this);
        internalEvents.put(EventCloseFile.class, op);
        
        op = new EventWriteObject(this);
        internalEvents.put(EventWriteObject.class, op);
    }
    
    public StorageStage getStorageStage() {
        return this.stStage;
    }
    
    public DeletionStage getDeletionStage() {
        return delStage;
    }
    
    public PreprocStage getPreprocStage() {
        return preprocStage;
    }
    
    public ReplicationStage getReplicationStage() {
        return replStage;
    }
    
    public UDPCommunicator getUdpComStage() {
        return udpCom;
    }

    public VivaldiStage getVivaldiStage() {
        return this.vStage;
    }
    
    @Override
    public void receiveUDP(UDPMessage msg) {
        assert (msg.isRequest() || msg.isResponse());

        try {
        
            if (msg.isRequest()) {
                if (msg.getRequestData() instanceof xtreemfs_broadcast_gmaxRequest) {
                    xtreemfs_broadcast_gmaxRequest rq = (xtreemfs_broadcast_gmaxRequest) msg.getRequestData();
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this,
                            "received GMAX packet for: %s from %s", rq.getFile_id(), msg.getAddress());

                    BufferPool.free(msg.getPayload());
                    stStage.receivedGMAX_ASYNC(rq.getFile_id(), rq.getTruncate_epoch(), rq.getLast_object());
                } else if (msg.getRequestData() instanceof xtreemfs_pingRequest) {
                    xtreemfs_pingRequest rq = (xtreemfs_pingRequest) msg.getRequestData();
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this,
                            "received ping request from: %s", msg.getAddress());

                    vStage.receiveVivaldiMessage(msg);
                }
            } else {
                if (msg.getResponseData() instanceof xtreemfs_pingResponse) {
                    xtreemfs_pingResponse resp = (xtreemfs_pingResponse) msg.getResponseData();
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this,
                            "received ping response from: %s", msg.getAddress());

                    vStage.receiveVivaldiMessage(msg);
                }
            }
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_DEBUG, this,ex);
        }
    }
    
    /**
     * @return the serviceAvailability
     */
    public ServiceAvailability getServiceAvailability() {
        return serviceAvailability;
    }
    
    public void objectReceived() {
        numObjsRX.incrementAndGet();
    }
    
    public void objectReplicated() {
        numReplObjsRX.incrementAndGet();
    }
    
    public void objectSent() {
        numObjsTX.incrementAndGet();
    }
    
    public void replicatedDataReceived(int numBytes) {
        numReplBytesRX.addAndGet(numBytes);
    }
    
    public void dataReceived(int numBytes) {
        numBytesRX.addAndGet(numBytes);
    }
    
    public void dataSent(int numBytes) {
        numBytesTX.addAndGet(numBytes);
    }
    
    public long getObjectsReceived() {
        return numObjsRX.get();
    }
    
    public long getObjectsSent() {
        return numObjsTX.get();
    }
    
    public long getBytesReceived() {
        return numBytesRX.get();
    }
    
    public long getBytesSent() {
        return numBytesTX.get();
    }
    
    public long getReplicatedObjectsReceived() {
        return numReplObjsRX.get();
    }
    
    public long getReplicatedBytesReceived() {
        return numReplBytesRX.get();
    }
    
    public void updateVivaldiCoordinates(VivaldiCoordinates newVC) {
        myCoordinates.set(newVC);
    }
    
}
