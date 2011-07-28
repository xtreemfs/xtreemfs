/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
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
import org.xtreemfs.foundation.CrashReporter;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.VersionManagement;
import org.xtreemfs.foundation.SSLOptions.TrustManager;
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
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

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
    
    private final HttpServer                      httpServ;
    
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
    
    public DIRRequestDispatcher(final DIRConfig config, final BabuDBConfig dbsConfig) throws IOException,
        BabuDBException {
        super("DIR RqDisp");
        this.config = config;
        
        Logging.logMessage(Logging.LEVEL_INFO, this, "XtreemFS Direcory Service version "
            + VersionManagement.RELEASE_VERSION);
        
        registry = new HashMap<Integer, DIROperation>();
        
        // start up babudb
        database = BabuDBFactory.createBabuDB(dbsConfig, new StaticInitialization() {
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
            
            sslOptions = new SSLOptions(new FileInputStream(config.getServiceCredsFile()), config
                    .getServiceCredsPassphrase(), config.getServiceCredsContainer(), new FileInputStream(
                config.getTrustedCertsFile()), config.getTrustedCertsPassphrase(), config
                    .getTrustedCertsContainer(), false, config.isGRIDSSLmode(), tm);
            
            if (Logging.isInfo() && tm != null)
                Logging.logMessage(Logging.LEVEL_INFO, Category.misc, this,
                    "using custom trust manager '%s'", tm.getClass().getName());
        }
        
        queue = new LinkedBlockingQueue<RPCServerRequest>();
        quit = false;
        
        server = new RPCNIOSocketServer(config.getPort(), config.getAddress(), this, sslOptions);
        
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
        
        httpServ = HttpServer.create(new InetSocketAddress(config.getHttpPort()), 0);
        final HttpContext ctx = httpServ.createContext("/", new HttpHandler() {
            public void handle(HttpExchange httpExchange) throws IOException {
                byte[] content;
                try {
                    content = StatusPage.getStatusPage(DIRRequestDispatcher.this, config).getBytes("ascii");
                    httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                    httpExchange.sendResponseHeaders(200, content.length);
                    httpExchange.getResponseBody().write(content);
                    httpExchange.getResponseBody().close();
                } catch (BabuDBException ex) {
                    ex.printStackTrace();
                    httpExchange.sendResponseHeaders(500, 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    httpExchange.sendResponseHeaders(500, 0);
                }
                
            }
        });
        
        if (config.getAdminPassword().length() > 0) {
            ctx.setAuthenticator(new BasicAuthenticator("XtreemFS DIR") {
                @Override
                public boolean checkCredentials(String arg0, String arg1) {
                    return (arg0.equals("admin") && arg1.equals(config.getAdminPassword()));
                }
            });
        }
        
        httpServ.start();
        
        numRequests = 0;
        
        if (config.isMonitoringEnabled()) {
            monThr = new MonitoringThread(config, this);
            monThr.setLifeCycleListener(this);
        } else {
            monThr = null;
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
        } catch (Exception ex) {
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
    
    public void shutdown() throws Exception {
        httpServ.stop(0);
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
    
    private void registerOperations() {
        
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
    }
    
    public Database getDirDatabase() {
        try {
            return database.getDatabaseManager().getDatabase(DB_NAME);
        } catch (BabuDBException e) {
            Logging.logError(Logging.LEVEL_ERROR, this, e);
            return null;
        }
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
            rq.sendError(ErrorType.INVALID_INTERFACE_ID, POSIXErrno.POSIX_ERROR_EIO,
                "invalid interface id. Maybe wrong service address/port configured?");
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
}
