/*
 * Copyright (c) 2009-2012 by Bjoern Kolbeck, Matthias Noack,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.xtreemfs.babudb.BabuDBFactory;
import org.xtreemfs.babudb.api.BabuDB;
import org.xtreemfs.babudb.api.DatabaseManager;
import org.xtreemfs.babudb.api.SnapshotManager;
import org.xtreemfs.babudb.api.StaticInitialization;
import org.xtreemfs.babudb.api.database.Database;
import org.xtreemfs.babudb.api.exception.BabuDBException;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.common.config.PolicyContainer;
import org.xtreemfs.common.monitoring.StatusMonitor;
import org.xtreemfs.common.statusserver.BabuDBStatusPage;
import org.xtreemfs.common.statusserver.PrintStackTrace;
import org.xtreemfs.common.statusserver.StatusServer;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.dir.data.ServiceRecords;
import org.xtreemfs.dir.discovery.DiscoveryMsgThread;
import org.xtreemfs.dir.operations.DIROperation;
import org.xtreemfs.dir.operations.DeleteAddressMappingOperation;
import org.xtreemfs.dir.operations.DeregisterServiceOperation;
import org.xtreemfs.dir.operations.GetAddressMappingOperation;
import org.xtreemfs.dir.operations.GetConfigurationOperation;
import org.xtreemfs.dir.operations.GetGlobalTimeOperation;
import org.xtreemfs.dir.operations.GetServiceByNameOperation;
import org.xtreemfs.dir.operations.GetServiceByUuidOperation;
import org.xtreemfs.dir.operations.GetServicesByTypeOperation;
import org.xtreemfs.dir.operations.RegisterServiceOperation;
import org.xtreemfs.dir.operations.ServiceOfflineOperation;
import org.xtreemfs.dir.operations.SetAddressMappingOperation;
import org.xtreemfs.dir.operations.SetConfigurationOperation;
import org.xtreemfs.dir.operations.UpdateVivaldiClientOperation;
import org.xtreemfs.foundation.CrashReporter;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.SSLOptions.TrustManager;
import org.xtreemfs.foundation.VersionManagement;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.MessageType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;
import org.xtreemfs.foundation.pbrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequestListener;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;

/**
 * 
 * @author bjko
 */
public class DIRRequestDispatcher extends LifeCycleThread implements RPCServerRequestListener,
    LifeCycleListener {
    
    /**
     * index for address mappings, stores uuid -> AddressMappingSet
     */
    public static final int                       INDEX_ID_ADDRMAPS       = 0;
    
    /**
     * index for service registries, stores uuid -> ServiceRegistry
     */
    public static final int                       INDEX_ID_SERVREG        = 1;
    
    /**
     * index for configuration of services, stores uuid -> Configuration
     */
    public static final int                       INDEX_ID_CONFIGURATIONS = 2;
    
    public static final int                       DB_VERSION              = 2010111010;
    
    protected final StatusServer                  statusServer;
    
    private int                                   numRequests;
    
    private final Map<Integer, DIROperation>      registry;
    
    private final RPCNIOSocketServer              server;
    
    private final BlockingQueue<RPCServerRequest> queue;
    
    private volatile boolean                      quit;
    
    private final BabuDB                          database;
    
    private final DiscoveryMsgThread              discoveryThr;
    
    private final MonitoringThread                monThr;
    
    private final DIRConfig                       config;
    
    public static final String                    DB_NAME                 = "dirdb";

    private List<DIRStatusListener>               statusListener;
    
    private VivaldiClientMap vivaldiClientMap;
    
    public DIRRequestDispatcher(final DIRConfig config, final BabuDBConfig dbsConfig) throws IOException,
        BabuDBException {
        super("DIR RqDisp");
        this.config = config;
        
        Logging.logMessage(Logging.LEVEL_INFO, this, "XtreemFS Direcory Service version "
            + VersionManagement.RELEASE_VERSION);
        
        registry = new HashMap<Integer, DIROperation>();

        vivaldiClientMap = new VivaldiClientMap(config.getVivaldiMaxClients(), config.getVivaldiClientTimeout());
        
        // start up babudb
        database = BabuDBFactory.createBabuDB(dbsConfig, new StaticInitialization() {
            @Override
            public void initialize(DatabaseManager dbMan, SnapshotManager sMan) {
                initDB(dbMan, sMan);
            }
        });
        
        registerOperations();
        
        // start the server
        
        SSLOptions sslOptions = null;
        if (config.isUsingSSL()) {
            
            PolicyContainer policyContainer = new PolicyContainer(config);
            TrustManager tm = null;
            try {
                tm = policyContainer.getTrustManager();
            } catch (Exception e) {
                throw new IOException(e);
            }
            
            sslOptions = new SSLOptions(config.getServiceCredsFile(), config.getServiceCredsPassphrase(),
                    config.getServiceCredsContainer(), config.getTrustedCertsFile(),
                    config.getTrustedCertsPassphrase(), config.getTrustedCertsContainer(), false,
                    config.isGRIDSSLmode(), config.getSSLProtocolString(), tm);
            
            if (Logging.isInfo() && tm != null)
                Logging.logMessage(Logging.LEVEL_INFO, Category.misc, this,
                    "using custom trust manager '%s'", tm.getClass().getName());
        }
        
        queue = new LinkedBlockingQueue<RPCServerRequest>();
        quit = false;
        
        server = new RPCNIOSocketServer(config.getPort(), config.getAddress(), this, sslOptions);
        server.setLifeCycleListener(this);
        
        if (config.isAutodiscoverEnabled()) {
            
            String scheme = Schemes.SCHEME_PBRPC;
            if (config.isGRIDSSLmode())
                scheme = Schemes.SCHEME_PBRPCG;
            else if (config.isUsingSSL())
                scheme = Schemes.SCHEME_PBRPCS;
            
            discoveryThr = new DiscoveryMsgThread(InetAddress.getLocalHost().getCanonicalHostName(), config
                    .getPort(), scheme);
            discoveryThr.setLifeCycleListener(this);
        } else {
            discoveryThr = null;
        }
        
        if (config.getHttpPort() == -1) {
            // Webinterface is explicitly disabled.
            statusServer = null;
        } else {
            statusServer = new StatusServer(ServiceType.SERVICE_TYPE_DIR, this, config.getHttpPort());
            statusServer.registerModule(new PrintStackTrace());
            statusServer.registerModule(new StatusPage(config));
            statusServer.registerModule(new ReplicaStatusPage());
            statusServer.registerModule(new VivaldiStatusPage(config));
            statusServer.registerModule(new BabuDBStatusPage(new BabuDBStatusPage.BabuDBStatusProvider() {
                @Override
                public Map<String, Object> getStatus() {
                    // NOTE(jdillmann): Access to the database is not synchronized. This might result in
                    // reading stale data.
                    return database.getRuntimeState();
                }
            }));

            if (config.getAdminPassword().length() > 0) {
                statusServer.addAuthorizedUser("admin", config.getAdminPassword());
            }

            statusServer.start();
        }
        
        numRequests = 0;
        
        if (config.isMonitoringEnabled()) {
            monThr = new MonitoringThread(config, this);
            monThr.setLifeCycleListener(this);
        } else {
            monThr = null;
        }

        statusListener = new ArrayList<DIRStatusListener>();
        if (config.isUsingSnmp()) {
            statusListener.add(new StatusMonitor(
                    this, config.getSnmpAddress(), config.getSnmpPort(), config.getSnmpACLFile()));

            // tell the StatusMonitor about the new (initial) configuration
            notifyConfigurationChange();
        }
        
        
        //notify listener about further ServiceRecords which are already in the database on initialization
        try {
            for (ServiceRecord sRec : this.getServices().getList()) {
                this.notifyServiceRegistred(sRec.getUuid(),sRec.getName() ,sRec.getType().toString(), "", "", 0, 0,
                        sRec.getLast_updated_s(), 0, 0, 0);
            }
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, ": %s",
            ex.getMessage());
        }
        
        
    }
    
    @Override
    public void run() {
        try {
            notifyStarted();
            while (!quit) {
                final RPCServerRequest rq = queue.take();
                synchronized (database) {
                    processRequest(rq);
                }
            }
        } catch (InterruptedException ex) {
            quit = true;
        } catch (Throwable ex) {
            final String report = CrashReporter.createCrashReport("DIR", VersionManagement.RELEASE_VERSION,
                ex);
            System.out.println(report);
            CrashReporter.reportXtreemFSCrash(report);
            notifyCrashed(ex);
            System.exit(2);
        }
        notifyStopped();
    }
    
    public ServiceRecords getServices() throws Exception {
        
        synchronized (database) {
            Database db = getDirDatabase();
            Iterator<Entry<byte[], byte[]>> iter = db.prefixLookup(DIRRequestDispatcher.INDEX_ID_SERVREG,
                new byte[0], null).get();
            
            ServiceRecords services = new ServiceRecords();
            
            while (iter.hasNext()) {
                final Entry<byte[], byte[]> e = iter.next();
                final ServiceRecord servEntry = new ServiceRecord(ReusableBuffer.wrap(e.getValue()));
                services.add(servEntry);
            }
            return services;
        }
        
    }
    
    public void startup() throws Exception {
        this.start();
        
        server.start();
        server.waitForStartup();
        
        if (discoveryThr != null) {
            discoveryThr.start();
            discoveryThr.waitForStartup();
        }
        
        if (monThr != null) {
            monThr.start();
            monThr.waitForStartup();
        }
    }
    
    @Override
    public void shutdown() throws Exception {
    	
        for (DIRStatusListener listener : statusListener) {
            listener.shuttingDown();
        }
    	
        if (statusServer != null) {
            statusServer.shutdown();
        }
        server.shutdown();
        server.waitForShutdown();
        database.shutdown();
        
        if (discoveryThr != null) {
            discoveryThr.shutdown();
            discoveryThr.waitForShutdown();
        }
        
        if (monThr != null) {
            monThr.shutdown();
            monThr.waitForShutdown();
        }
        
        this.quit = true;
        this.interrupt();
        this.waitForShutdown();
    }
    
    private void registerOperations() throws BabuDBException {
        
        DIROperation op;
        op = new GetGlobalTimeOperation(this);
        registry.put(op.getProcedureId(), op);
        
        op = new GetAddressMappingOperation(this);
        registry.put(op.getProcedureId(), op);
        
        op = new SetAddressMappingOperation(this);
        registry.put(op.getProcedureId(), op);
        
        op = new DeleteAddressMappingOperation(this);
        registry.put(op.getProcedureId(), op);
        
        op = new RegisterServiceOperation(this);
        registry.put(op.getProcedureId(), op);
        
        op = new DeregisterServiceOperation(this);
        registry.put(op.getProcedureId(), op);
        
        op = new GetServiceByUuidOperation(this);
        registry.put(op.getProcedureId(), op);
        
        op = new GetServicesByTypeOperation(this);
        registry.put(op.getProcedureId(), op);
        
        op = new GetServiceByNameOperation(this);
        registry.put(op.getProcedureId(), op);
        
        op = new ServiceOfflineOperation(this);
        registry.put(op.getProcedureId(), op);
        
        op = new SetConfigurationOperation(this);
        registry.put(op.getProcedureId(), op);
        
        op = new GetConfigurationOperation(this);
        registry.put(op.getProcedureId(), op);
        
        op = new UpdateVivaldiClientOperation(this);
        registry.put(op.getProcedureId(), op);
    }
    
    public Database getDirDatabase() throws BabuDBException {
        return database.getDatabaseManager().getDatabase(DB_NAME);
    }
    
    @Override
    public void receiveRecord(RPCServerRequest rq) {
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "received new request: %s", rq
                    .toString());
        this.queue.add(rq);
    }
    
    public void processRequest(RPCServerRequest rq) {
        RPCHeader hdr = rq.getHeader();
        
        if (hdr.getMessageType() != MessageType.RPC_REQUEST) {
            rq.sendError(ErrorType.GARBAGE_ARGS, POSIXErrno.POSIX_ERROR_EIO,
                "expected RPC request message type but got " + hdr.getMessageType());
            return;
        }
        
        RPCHeader.RequestHeader rqHdr = hdr.getRequestHeader();
        
        /*
         * if (rqHdr.hasInterfaceId() == NettestInterface.getVersion()) {
         * Nettest.handleNettest(hdr,rq); return; }
         */

        if (rqHdr.getInterfaceId() != DIRServiceConstants.INTERFACE_ID) {
            rq.sendError(
                    ErrorType.INVALID_INTERFACE_ID,
                    POSIXErrno.POSIX_ERROR_EIO,
                    "Invalid interface id. This is a DIR service. You probably wanted to contact another service. Check the used address and port.");
            return;
        }
        
        // everything ok, find the right operation
        DIROperation op = registry.get(rqHdr.getProcId());
        if (op == null) {
            rq.sendError(ErrorType.INVALID_PROC_ID, POSIXErrno.POSIX_ERROR_EIO,
                "unknown procedure id requested");
            return;
        }
        
        DIRRequest dirRq = new DIRRequest(rq);
        try {
            op.parseRPCMessage(dirRq);
            numRequests++;
            op.startRequest(dirRq);
        } catch (Throwable ex) {
            ex.printStackTrace();
            rq.sendError(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO,
                "internal server error: " + ex.toString(), OutputUtils.stackTraceToString(ex));
            return;
        }
    }
    
    @Override
    public void startupPerformed() {
    }
    
    @Override
    public void shutdownPerformed() {
    }
    
    @Override
    public void crashPerformed(Throwable cause) {
        final String report = CrashReporter
                .createCrashReport("DIR", VersionManagement.RELEASE_VERSION, cause);
        System.out.println(report);
        CrashReporter.reportXtreemFSCrash(report);
        try {
            shutdown();
        } catch (Exception e) {
            Logging.logError(Logging.LEVEL_ERROR, this, e);
        }
    }
    
    public long getNumRequests() {
        return server.getPendingRequests();
    }
    
    public int getNumConnections() {
        return server.getNumConnections();
    }
    
    //public HashMap<InetSocketAddress, VivaldiClientValue> getVivaldiClientMap(){
    public VivaldiClientMap getVivaldiClientMap(){
        return vivaldiClientMap;
    }
    
    public DIRConfig getConfig() {
        return config;
    }
    
    private void initDB(DatabaseManager dbMan, SnapshotManager sMan) {
        final byte[] versionKey = "version".getBytes();
        try {
            Database db = dbMan.createDatabase("dirdbver", 2);
            ReusableBuffer rb = null;
            try {
                byte[] keyData = new byte[4];
                rb = ReusableBuffer.wrap(keyData);
                rb.putInt(DB_VERSION);
                db.singleInsert(0, versionKey, keyData, null).get();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("cannot initialize database");
                System.exit(1);
            } finally {
                if (rb != null)
                    BufferPool.free(rb);
            }
        } catch (BabuDBException ex) {
            // database exists: check version
            if (ex.getErrorCode() == BabuDBException.ErrorCode.DB_EXISTS) {
                ReusableBuffer rb = null;
                try {
                    Database db = dbMan.getDatabase("dirdbver");
                    
                    byte[] value = db.lookup(0, versionKey, null).get();
                    int ver = -1;
                    if ((value != null) && (value.length == 4)) {
                        rb = ReusableBuffer.wrap(value);
                        ver = rb.getInt();
                    }
                    if (ver != DB_VERSION) {
                        Logging.logMessage(Logging.LEVEL_ERROR, this, "OUTDATED DATABASE VERSION DETECTED!");
                        Logging
                                .logMessage(
                                    Logging.LEVEL_ERROR,
                                    this,
                                    "the database was created contains data with version no %d, this DIR uses version %d.",
                                    ver, DB_VERSION);
                        Logging.logMessage(Logging.LEVEL_ERROR, this,
                            "please start an older version of the DIR or remove the old database");
                        System.exit(1);
                    }
                } catch (Exception ex2) {
                    ex2.printStackTrace();
                    System.err.println("cannot initialize database");
                    System.exit(1);
                } finally {
                    if (rb != null)
                        BufferPool.free(rb);
                }
            } else {
                ex.printStackTrace();
                System.err.println("cannot initialize database");
                System.exit(1);
            }
        }
        
        try {
            dbMan.createDatabase("dirdb", 3);
        } catch (BabuDBException ex) {
            // database already created
        }
    }

    public void addStatusListener(DIRStatusListener listener) {
        this.statusListener.add(listener);
    }

    public void removeStatusListener(DIRStatusListener listener) {
        this.statusListener.remove(listener);
    }

    /**
     * Tells all listeners when an AddressMapping was added.
     */
    public void notifyAddressMappingAdded(String uuid, String uri) {
        for (DIRStatusListener listener : statusListener) {
            listener.addressMappingAdded();
        }
    }

    /**
     * Tells all listeners when an AddressMapping was deleted.
     */
    public void notifyAddressMappingDeleted(String uuid, String uri) {
        for (DIRStatusListener listener : statusListener) {
            listener.addressMappingDeleted();
        }
    }

    /**
     * Tells all listeners when the configuration has changed.
     */
    public void notifyConfigurationChange() {
        for (DIRStatusListener listener : statusListener) {
            listener.DIRConfigChanged(this.config);
        }
    }

    /**
     * Tells all listeners when an ServiceRegistred or Updated its registration at the DIR.
     * 
     */
    public void notifyServiceRegistred(String uuid, String name, String type, String pageUrl,
            String geoCoordinates, long totalRam, long usedRam, long lastUpdated, int status, int load,
            int protoVersion) {
        for (DIRStatusListener listener : statusListener) {
            listener.serviceRegistered();
        }
    }

    /**
     * Tells all listeners that a service was deregistred.
     * 
     */
    public void notifyServiceDeregistred(String uuid) {
        for (DIRStatusListener listener : statusListener) {
            listener.serviceDeregistered();
        }
    }
}
