/*  Copyright (c) 2009-2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.BabuDBFactory;
import org.xtreemfs.babudb.StaticInitialization;
import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.babudb.config.ReplicationConfig;
import org.xtreemfs.babudb.lsmdb.Database;
import org.xtreemfs.babudb.lsmdb.DatabaseManager;
import org.xtreemfs.babudb.replication.ReplicationManager;
import org.xtreemfs.babudb.snapshots.SnapshotManager;
import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.dir.data.ServiceRecord;
import org.xtreemfs.dir.data.ServiceRecords;
import org.xtreemfs.dir.discovery.DiscoveryMsgThread;
import org.xtreemfs.dir.operations.DIROperation;
import org.xtreemfs.dir.operations.DeleteAddressMappingOperation;
import org.xtreemfs.dir.operations.DeregisterServiceOperation;
import org.xtreemfs.dir.operations.GetAddressMappingOperation;
import org.xtreemfs.dir.operations.GetGlobalTimeOperation;
import org.xtreemfs.dir.operations.GetServiceByNameOperation;
import org.xtreemfs.dir.operations.GetServiceByUuidOperation;
import org.xtreemfs.dir.operations.GetServicesByTypeOperation;
import org.xtreemfs.dir.operations.RegisterServiceOperation;
import org.xtreemfs.dir.operations.ServiceOfflineOperation;
import org.xtreemfs.dir.operations.SetAddressMappingOperation;
import org.xtreemfs.foundation.CrashReporter;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.oncrpc.server.RPCServerRequestListener;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.DIRInterface.DIRInterface;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.xtreemfs.common.util.Nettest;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.oncrpc.server.NullAuthFlavorProvider;
import org.xtreemfs.interfaces.DIRInterface.DIRException;
import org.xtreemfs.interfaces.NettestInterface.NettestInterface;

/**
 * 
 * @author bjko
 */
public class DIRRequestDispatcher extends LifeCycleThread 
    implements RPCServerRequestListener, LifeCycleListener, StaticInitialization {
    
    /**
     * index for address mappings, stores uuid -> AddressMappingSet
     */
    public static final int                    INDEX_ID_ADDRMAPS = 0;
    
    /**
     * index for service registries, stores uuid -> ServiceRegistry
     */
    public static final int                    INDEX_ID_SERVREG  = 1;

    public static final int                    DB_VERSION = 2009082718;
    
    private final HttpServer                   httpServ;
    
    private int                                numRequests;
    
    private final Map<Integer, DIROperation>   registry;
    
    private final RPCNIOSocketServer           server;
    
    private final BlockingQueue<ONCRPCRequest> queue;
    
    private volatile boolean                   quit;
    
    private final BabuDB                       database;
        
    private final DiscoveryMsgThread           discoveryThr;

    private final MonitoringThread             monThr;
    
    private final DIRConfig                    config;
            
    public static final String                 DB_NAME           = "dirdb";
    
    public DIRRequestDispatcher(final DIRConfig config, 
            final BabuDBConfig dbsConfig) throws IOException, BabuDBException {
        super("DIR RqDisp");
        this.config = config;
        
        Logging.logMessage(Logging.LEVEL_INFO, this, "XtreemFS Direcory Service version "
            + VersionManagement.RELEASE_VERSION);
        
        registry = new HashMap<Integer, DIROperation>();
        
        // start up babudb
        if (dbsConfig instanceof ReplicationConfig)
            database = BabuDBFactory
                        .createReplicatedBabuDB(
                                (ReplicationConfig) dbsConfig, this);
        else
            database = BabuDBFactory.createBabuDB(dbsConfig, this);
        
        registerOperations();
        
        // start the server
        
        SSLOptions sslOptions = null;
        if (config.isUsingSSL()) {
            sslOptions = new SSLOptions(new FileInputStream(config.getServiceCredsFile()), config
                    .getServiceCredsPassphrase(), config.getServiceCredsContainer(), new FileInputStream(
                config.getTrustedCertsFile()), config.getTrustedCertsPassphrase(), config
                    .getTrustedCertsContainer(), false, config.isGRIDSSLmode());
        }
        
        queue = new LinkedBlockingQueue<ONCRPCRequest>();
        quit = false;
        
        server = new RPCNIOSocketServer(config.getPort(), config.getAddress(), this, sslOptions, new NullAuthFlavorProvider());
        
        if (config.isAutodiscoverEnabled()) {
            
            String scheme = Constants.ONCRPC_SCHEME;
            if (config.isGRIDSSLmode())
                scheme = Constants.ONCRPCG_SCHEME;
            else if (config.isUsingSSL())
                scheme = Constants.ONCRPCS_SCHEME;

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
                    return (arg0.equals("admin")&& arg1.equals(config.getAdminPassword()));
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
                final ONCRPCRequest rq = queue.take();
                synchronized (database) {
                    processRequest(rq);
                }
            }
        } catch (InterruptedException ex) {
            quit = true;
        } catch (Exception ex) {
            final String report = CrashReporter.createCrashReport("DIR", VersionManagement.RELEASE_VERSION, ex);
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
            Iterator<Entry<byte[], byte[]>> iter = db.prefixLookup(
                DIRRequestDispatcher.INDEX_ID_SERVREG, new byte[0], null).get();

            ServiceRecords services = new ServiceRecords();

            while (iter.hasNext()) {
                final Entry<byte[],byte[]> e = iter.next();
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
    }
    
    public Database getDirDatabase() {
        try {
            return database.getDatabaseManager().getDatabase(DB_NAME);
        } catch (BabuDBException e) {
            Logging.logError(Logging.LEVEL_ERROR, this, e);
            return null;
        }
    }
    
    public ReplicationManager getDBSReplicationService() {
        return database.getReplicationManager();
    }
    
    @Override
    public void receiveRecord(ONCRPCRequest rq) {
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "received new request: %s", rq
                    .toString());
        this.queue.add(rq);
    }
    
    public void processRequest(ONCRPCRequest rq) {
        final ONCRPCRequestHeader hdr = rq.getRequestHeader();

        if (hdr.getInterfaceVersion() == NettestInterface.getVersion()) {
            Nettest.handleNettest(hdr,rq);
            return;
        }

        if (hdr.getInterfaceVersion() != DIRInterface.getVersion()) {
            rq.sendProgMismatch();
            return;
        }
        
        // everything ok, find the right operation
        DIROperation op = registry.get(hdr.getProcedure());
        if (op == null) {
            rq.sendProcUnavail();
            return;
        }
        
        DIRRequest dirRq = new DIRRequest(rq);
        try {
            op.parseRPCMessage(dirRq);
            numRequests++;
            op.startRequest(dirRq);
        } catch (Throwable ex) {
            ex.printStackTrace();
            rq.sendException(new DIRException(ErrNo.EIO, "internal server error: "+ex.toString(), OutputUtils.stackTraceToString(ex)));
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
        final String report = CrashReporter.createCrashReport("DIR", VersionManagement.RELEASE_VERSION, cause);
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

    /*
     * (non-Javadoc)
     * @see org.xtreemfs.babudb.StaticInitialization#initialize(org.xtreemfs.babudb.lsmdb.DatabaseManager, org.xtreemfs.babudb.snapshots.SnapshotManager, org.xtreemfs.babudb.replication.ReplicationManager)
     */
    @Override
    public void initialize(DatabaseManager dbMan, SnapshotManager sMan, 
            ReplicationManager replMan) {
        final byte[] versionKey = "version".getBytes();
        try {
            Database db = dbMan.createDatabase("dirdbver", 1);
            ReusableBuffer rb = null;
            try {
                byte[] keyData = new byte[4];
                rb = ReusableBuffer.wrap(keyData);
                rb.putInt(DB_VERSION);
                db.singleInsert(0, versionKey, keyData,null).get();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("cannot initialize database");
                System.exit(1);
            } finally {
                if (rb != null) BufferPool.free(rb);
            }
        } catch (BabuDBException ex) {
            // database exists: check version
            if (ex.getErrorCode() == BabuDBException.ErrorCode.DB_EXISTS) {
                ReusableBuffer rb = null;
                try {
                    Database db = dbMan.getDatabase("dirdbver");
                    
                    byte[] value = db.lookup(0, versionKey,null).get();
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
                    if (rb != null) BufferPool.free(rb);
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
