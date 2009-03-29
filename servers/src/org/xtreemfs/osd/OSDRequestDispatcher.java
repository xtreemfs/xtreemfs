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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.HeartbeatThread.ServiceDataGenerator;
import org.xtreemfs.common.checksums.ChecksumFactory;
import org.xtreemfs.common.checksums.provider.JavaChecksumProvider;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.oncrpc.server.RPCServerRequestListener;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.OSDInterface.OSDInterface;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceDataMap;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.osd.operations.OSDOperation;
import org.xtreemfs.osd.stages.DeletionStage;
import org.xtreemfs.osd.stages.PreprocStage;
import org.xtreemfs.osd.stages.StorageStage;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.osd.operations.CheckObjectOperation;
import org.xtreemfs.osd.operations.DeleteOperation;
import org.xtreemfs.osd.operations.EventCloseFile;
import org.xtreemfs.osd.operations.InternalGetFileSizeOperation;
import org.xtreemfs.osd.operations.InternalGetGmaxOperation;
import org.xtreemfs.osd.operations.InternalTruncateOperation;
import org.xtreemfs.osd.operations.KeepFileOpenOperation;
import org.xtreemfs.osd.operations.ReadOperation;
import org.xtreemfs.osd.operations.ShutdownOperation;
import org.xtreemfs.osd.operations.TruncateOperation;
import org.xtreemfs.osd.operations.WriteOperation;
import org.xtreemfs.osd.striping.GMAXMessage;
import org.xtreemfs.osd.striping.UDPCommunicator;
import org.xtreemfs.osd.striping.UDPMessage;
import org.xtreemfs.osd.striping.UDPReceiverInterface;

public class OSDRequestDispatcher implements RPCServerRequestListener, LifeCycleListener, UDPReceiverInterface {

    public final static String VERSION = "1.0.0 (v1.0 RC1)";

    protected final Map<Integer,OSDOperation>     operations;

    protected final Map<Class,OSDOperation>       internalEvents;
    
    protected final HeartbeatThread heartbeatThread;
    
    protected final OSDConfig       config;
    
    protected final DIRClient       dirClient;
    
    protected final OSDClient       osdClient;

    protected final RPCNIOSocketClient rpcClient;

    protected final RPCNIOSocketServer rpcServer;
    
    protected long                  requestId;
    
    protected String                authString;

    protected final PreprocStage    preprocStage;

    protected final StorageStage    stStage;

    protected final DeletionStage   delStage;

    protected final UDPCommunicator udpCom;

    protected final HttpServer      httpServ;
    
    protected final long            startupTime;

    protected final AtomicLong      numBytesTX, numBytesRX, numObjsTX, numObjsRX;

    
    public OSDRequestDispatcher(OSDConfig config) throws IOException {
        
        this.config = config;
        assert (config.getUUID() != null);

        numBytesTX = new AtomicLong();
        numBytesRX = new AtomicLong();
        numObjsTX = new AtomicLong();
        numObjsRX = new AtomicLong();
        
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

        SSLOptions serverSSLopts  = config.isUsingSSL() ? new SSLOptions(new FileInputStream(config.getServiceCredsFile()), config
                    .getServiceCredsPassphrase(), config.getServiceCredsContainer(), new FileInputStream(
                config.getTrustedCertsFile()), config.getTrustedCertsPassphrase(), config
                    .getTrustedCertsContainer(), false) : null;

        rpcServer = new RPCNIOSocketServer(config.getPort(), config.getAddress(), this, serverSSLopts);
        rpcServer.setLifeCycleListener(this);

        final SSLOptions clientSSLopts = config.isUsingSSL() ? new SSLOptions(new FileInputStream(config
                .getServiceCredsFile()), config.getServiceCredsPassphrase(), config
                .getServiceCredsContainer(), new FileInputStream(config.getTrustedCertsFile()), config
                .getTrustedCertsPassphrase(), config.getTrustedCertsContainer(), false) : null;


        rpcClient = new RPCNIOSocketClient(clientSSLopts, 5000, 5*60*1000);
        rpcClient.setLifeCycleListener(this);
        
        
        // --------------------------
        // initialize internal stages
        // --------------------------
        
        MetadataCache metadataCache = new MetadataCache();
        StorageLayout storageLayout = new HashStorageLayout(config, metadataCache);

        udpCom = new UDPCommunicator(config.getPort(), this);
        udpCom.setLifeCycleListener(this);

        preprocStage = new PreprocStage(this);
        preprocStage.setLifeCycleListener(this);

        stStage = new StorageStage(this, metadataCache, storageLayout, 1);
        stStage.setLifeCycleListener(this);

        delStage = new DeletionStage(this, metadataCache, storageLayout);
        delStage.setLifeCycleListener(this);
        
        // ----------------------------------------
        // initialize TimeSync and Heartbeat thread
        // ----------------------------------------
        
        dirClient = new DIRClient(rpcClient, config.getDirectoryService());
        osdClient = new OSDClient(rpcClient);
        
        TimeSync.initialize(dirClient, config.getRemoteTimeSync(), config.getLocalClockRenew());
        UUIDResolver.start(dirClient, 10 * 1000, 600 * 1000);
        UUIDResolver.addLocalMapping(config.getUUID(), config.getPort(), config.isUsingSSL());
        
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
                try {
                    dmap.put("status_page_url", "http://" + config.getUUID().getAddress().getHostName() + ":" + config.getHttpPort());
                } catch (UnknownUUIDException ex) {
                    //should never happen
                }
                Service me = new Service(config.getUUID().toString(), 0
                        , Constants.SERVICE_TYPE_OSD, "OSD @ "+config.getUUID(), 0, dmap);
                data.add(me);

                return data;
            }
        };
        heartbeatThread = new HeartbeatThread("OSD HB Thr", dirClient, config.getUUID(), gen,
            config);

        httpServ = HttpServer.create(new InetSocketAddress(config.getHttpPort()), 0);
        httpServ.createContext("/", new HttpHandler() {
            public void handle(HttpExchange httpExchange) throws IOException {
                byte[] content;
                try {
                    content = StatusPage.getStatusPage(OSDRequestDispatcher.this).getBytes("ascii");
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

        startupTime = System.currentTimeMillis();

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "OSD at " + this.config.getUUID() + " ready");
    }
    
    public void start() {
        
        try {
            
            rpcServer.start();
            rpcClient.start();
            
            rpcServer.waitForStartup();
            rpcClient.waitForStartup();

            udpCom.start();
            preprocStage.start();
            delStage.start();
            stStage.start();

            udpCom.waitForStartup();
            preprocStage.waitForStartup();
            delStage.waitForStartup();
            stStage.waitForStartup();
            
            heartbeatThread.start();
            heartbeatThread.waitForStartup();
            
            Logging.logMessage(Logging.LEVEL_INFO, this, "RequestController and all services operational");
            
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "startup failed");
            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
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
            
            rpcServer.waitForShutdown();
            
            rpcClient.waitForShutdown();

            udpCom.shutdown();
            preprocStage.shutdown();
            delStage.shutdown();
            stStage.shutdown();

            udpCom.waitForShutdown();
            preprocStage.waitForShutdown();
            delStage.waitForShutdown();
            stStage.waitForShutdown();

            httpServ.stop(0);
            
            Logging.logMessage(Logging.LEVEL_INFO, this, "OSD and all stages terminated");
            
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "shutdown failed");
            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public void asyncShutdown() {
        try {

            heartbeatThread.shutdown();

            UUIDResolver.shutdown();

            rpcServer.shutdown();
            rpcClient.shutdown();

            udpCom.shutdown();
            preprocStage.shutdown();
            delStage.shutdown();
            stStage.shutdown();

            httpServ.stop(0);

            Logging.logMessage(Logging.LEVEL_INFO, this, "OSD and all stages terminated");

        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "shutdown failed");
            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
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
    
    public OSDClient getOSDClient() {
        return osdClient;
    }
    
    public void startupPerformed() {
        
    }
    
    public void shutdownPerformed() {
        
    }
    
    public void crashPerformed() {
        Logging.logMessage(Logging.LEVEL_ERROR, this, "a component crashed... shutting down system!");
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
        OSDRequest request = new OSDRequest(rq);
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"received new request: "+rq);
        preprocStage.prepareRequest(request, new PreprocStage.ParseCompleteCallback() {

            @Override
            public void parseComplete(OSDRequest result, Exception error) {
                if (error == null) {
                    result.getOperation().startRequest(result);
                } else {
                    if (error instanceof ONCRPCException)
                        result.sendException((ONCRPCException)error);
                    else
                        result.sendInternalServerError(error);
                }
            }
        });
    }

    int getNumClientConnections() {
        return rpcServer.getNumConnections();
    }

    long getPendingRequests() {
        return rpcServer.getPendingRequests();
    }

    private void initializeOperations() {
        //register all ops
        OSDOperation op = new ReadOperation(this);
        operations.put(op.getProcedureId(),op);

        op = new WriteOperation(this);
        operations.put(op.getProcedureId(),op);

        op = new DeleteOperation(this);
        operations.put(op.getProcedureId(),op);

        op = new TruncateOperation(this);
        operations.put(op.getProcedureId(),op);

        op = new KeepFileOpenOperation(this);
        operations.put(op.getProcedureId(),op);

        op = new InternalGetGmaxOperation(this);
        operations.put(op.getProcedureId(),op);

        op = new InternalTruncateOperation(this);
        operations.put(op.getProcedureId(),op);

        op = new CheckObjectOperation(this);
        operations.put(op.getProcedureId(),op);

        op = new InternalGetFileSizeOperation(this);
        operations.put(op.getProcedureId(),op);

        op = new ShutdownOperation(this);
        operations.put(op.getProcedureId(),op);

        //--internal events here--

        op = new EventCloseFile(this);
        internalEvents.put(EventCloseFile.class,op);
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

    public UDPCommunicator getUdpComStage() {
        return udpCom;
    }

    @Override
    public void receiveUDP(UDPMessage msg) {
        if (msg.getMsgType() == UDPMessage.Type.GMAX) {
            //send gmax to storage stage
            try {
                GMAXMessage gmax = new GMAXMessage(msg);
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"received GMAX packet for : "+gmax.getFileId());
                stStage.receivedGMAX_ASYNC(gmax.getFileId(), gmax.getTruncateEpoch(), gmax.getLastObject());
            } catch (Exception ex) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this,ex);
            }
        }
    }

    public void objectReceived() {
        numObjsRX.incrementAndGet();
    }

    public void objectSent() {
        numObjsTX.incrementAndGet();
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
    
}
