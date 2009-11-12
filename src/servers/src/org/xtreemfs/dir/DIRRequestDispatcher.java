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
 * AUTHORS: Björn Kolbeck (ZIB)
 */
package org.xtreemfs.dir;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
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
import org.xtreemfs.babudb.lsmdb.BabuDBInsertGroup;
import org.xtreemfs.babudb.lsmdb.Database;
import org.xtreemfs.babudb.lsmdb.DatabaseManager;
import org.xtreemfs.babudb.replication.ReplicationManager;
import org.xtreemfs.common.VersionManagement;
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
import org.xtreemfs.dir.operations.ReplicationToMasterOperation;
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
import org.xtreemfs.include.common.config.BabuDBConfig;
import org.xtreemfs.include.common.config.ReplicationConfig;
import org.xtreemfs.interfaces.DIRInterface.DIRInterface;
import org.xtreemfs.interfaces.DIRInterface.ProtocolException;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.xtreemfs.interfaces.Constants;

/**
 * 
 * @author bjko
 */
public class DIRRequestDispatcher extends LifeCycleThread 
    implements RPCServerRequestListener, LifeCycleListener {
    
    /**
     * index for address mappings, stores uuid -> AddressMappingSet
     */
    public static final int                    INDEX_ID_ADDRMAPS = 0;
    
    /**
     * index for service registries, stores uuid -> ServiceRegistry
     */
    public static final int                    INDEX_ID_SERVREG  = 1;
    
    private final HttpServer                   httpServ;
    
    private int                                numRequests;
    
    private final Map<Integer, DIROperation>   registry;
    
    private final RPCNIOSocketServer           server;
    
    private final BlockingQueue<ONCRPCRequest> queue;
    
    private volatile boolean                   quit;
    
    private final BabuDB                       database;
    
    private final DatabaseManager              dbMan;
    
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
                        .createReplicatedBabuDB((ReplicationConfig) dbsConfig);
        else
            database = BabuDBFactory.createBabuDB(dbsConfig);
            
        dbMan = database.getDatabaseManager();
        
        database.disableSlaveCheck();
        initializeDatabase();
        database.enableSlaveCheck();
        
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
        
        server = new RPCNIOSocketServer(config.getPort(), config.getAddress(), this, sslOptions);
        
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
            notifyCrashed(ex);
        }
        notifyStopped();
    }

    public ServiceRecords getServices() throws Exception {

        synchronized (database) {
            Database db = getDirDatabase();
            Iterator<Entry<byte[], byte[]>> iter = db.directPrefixLookup(
                DIRRequestDispatcher.INDEX_ID_SERVREG, new byte[0]);

            ServiceRecords services = new ServiceRecords();

            // long now = System.currentTimeMillis() / 1000l;

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
    
    private void initializeDatabase() {
        final byte[] versionKey = "version".getBytes();
        try {
            Database db = dbMan.createDatabase("dirdbver", 1);
            try {
                BabuDBInsertGroup ig = db.createInsertGroup();
                byte[] keyData = new byte[4];
                ReusableBuffer rb = ReusableBuffer.wrap(keyData);
                rb.putInt(DIRInterface.getVersion());
                ig.addInsert(0, versionKey, keyData);
                db.directInsert(ig);
            } catch (BabuDBException ex) {
                ex.printStackTrace();
                System.err.println("cannot initialize database");
                System.exit(1);
            }
        } catch (BabuDBException ex) {
            // database exists: check version
            if (ex.getErrorCode() == BabuDBException.ErrorCode.DB_EXISTS) {
                try {
                    Database db = dbMan.getDatabase("dirdbver");
                    
                    byte[] value = db.directLookup(0, versionKey);
                    int ver = -1;
                    if ((value != null) && (value.length == 4)) {
                        ReusableBuffer rb = ReusableBuffer.wrap(value);
                        ver = rb.getInt();
                    }
                    if (ver != DIRInterface.getVersion()) {
                        Logging.logMessage(Logging.LEVEL_ERROR, this, "OUTDATED DATABASE VERSION DETECTED!");
                        Logging
                                .logMessage(
                                    Logging.LEVEL_ERROR,
                                    this,
                                    "the database was created contains data with version no %d, this DIR uses version %d.",
                                    ver, DIRInterface.getVersion());
                        Logging.logMessage(Logging.LEVEL_ERROR, this,
                            "please start an older version of the DIR or remove the old database");
                        System.exit(1);
                    }
                } catch (BabuDBException ex2) {
                    ex2.printStackTrace();
                    System.err.println("cannot initialize database");
                    System.exit(1);
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
        
        op = new ReplicationToMasterOperation(this);
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
        
        if (hdr.getInterfaceVersion() != DIRInterface.getVersion()) {
            rq.sendException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_PROG_MISMATCH,
                ErrNo.EINVAL, "invalid version requested"));
            return;
        }
        
        // everything ok, find the right operation
        DIROperation op = registry.get(hdr.getProcedure());
        if (op == null) {
            rq.sendException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_PROC_UNAVAIL,
                ErrNo.EINVAL, "requested operation is not available on this DIR"));
            return;
        }
        
        DIRRequest dirRq = new DIRRequest(rq);
        try {
            op.parseRPCMessage(dirRq);
        } catch (Throwable ex) {
            ex.printStackTrace();
            rq.sendGarbageArgs(ex.toString(), new ProtocolException());
            return;
        }
        
        try {
            numRequests++;
            op.startRequest(dirRq);
        } catch (Throwable ex) {
            ex.printStackTrace();
            rq.sendErrorCode(ONCRPCResponseHeader.ACCEPT_STAT_SYSTEM_ERR);
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
}
