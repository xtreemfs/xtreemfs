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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.osd;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.Request;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.common.HeartbeatThread.ServiceDataGenerator;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.checksums.ChecksumFactory;
import org.xtreemfs.common.checksums.provider.JavaChecksumProvider;
import org.xtreemfs.common.clients.RPCClient;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.clients.osd.OSDClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.common.trace.Tracer;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.foundation.pinky.PinkyRequestListener;
import org.xtreemfs.foundation.pinky.PipelinedPinky;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.foundation.pinky.HTTPHeaders.HeaderEntry;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.osd.ops.AcquireLease;
import org.xtreemfs.osd.ops.CheckObjectRPC;
import org.xtreemfs.osd.ops.CleanUpOperation;
import org.xtreemfs.osd.ops.CloseFileEvent;
import org.xtreemfs.osd.ops.DeleteLocalRPC;
import org.xtreemfs.osd.ops.DeleteOFTRPC;
import org.xtreemfs.osd.ops.DeleteOperation;
import org.xtreemfs.osd.ops.FetchAndWriteReplica;
import org.xtreemfs.osd.ops.FetchGmaxRPC;
import org.xtreemfs.osd.ops.GetProtocolVersionOperation;
import org.xtreemfs.osd.ops.GetStatistics;
import org.xtreemfs.osd.ops.GmaxEvent;
import org.xtreemfs.osd.ops.Operation;
import org.xtreemfs.osd.ops.ReadLocalRPC;
import org.xtreemfs.osd.ops.ReadOperation;
import org.xtreemfs.osd.ops.ReturnLease;
import org.xtreemfs.osd.ops.ShutdownOperation;
import org.xtreemfs.osd.ops.StatisticsConfig;
import org.xtreemfs.osd.ops.StatusPageOperation;
import org.xtreemfs.osd.ops.TruncateLocalRPC;
import org.xtreemfs.osd.ops.TruncateRPC;
import org.xtreemfs.osd.ops.WriteOperation;
import org.xtreemfs.osd.stages.AuthenticationStage;
import org.xtreemfs.osd.stages.DeletionStage;
import org.xtreemfs.osd.stages.ParserStage;
import org.xtreemfs.osd.stages.ReplicationStage;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.stages.StageStatistics;
import org.xtreemfs.osd.stages.StatisticsStage;
import org.xtreemfs.osd.stages.StorageStage;
import org.xtreemfs.osd.storage.HashStorageLayout;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.osd.storage.Striping;

public class OSDRequestDispatcher implements RequestDispatcher, PinkyRequestListener,
    LifeCycleListener, UDPReceiverInterface {
    
    protected final Stage[]         stages;
    
    protected final Operation[]     operations;
    
    protected final UDPCommunicator udpCom;
    
    protected final PipelinedPinky  pinky;
    
    protected final MultiSpeedy     speedy;
    
    protected final HeartbeatThread heartbeatThread;
    
    protected final OSDConfig       config;
    
    protected final StageStatistics statistics;
    
    protected final DIRClient       dirClient;

    protected final OSDClient       osdClient;

    protected long                  requestId;
    
    protected String                authString;
    
    public OSDRequestDispatcher(OSDConfig config) throws IOException, JSONException {
        
        this.config = config;
        assert (config.getUUID() != null);
        
        if (Tracer.COLLECT_TRACES) {
            Tracer.initialize("/tmp/OSD_" + config.getUUID() + ".trace");
        }
        
        // generate an authorization string for Directory Service operations
        authString = NullAuthProvider.createAuthString(config.getUUID().toString(), config
                .getUUID().toString());
        
        // initialize the checksum factory
        ChecksumFactory.getInstance().addProvider(new JavaChecksumProvider());
        
        // ---------------------
        // initialize operations
        // ---------------------
        
        // IMPORTANT: the order of operations defined in
        // 'RequestDispatcher.Operations' has to be preserved!
        operations = new Operation[] { new ReadOperation(this), new WriteOperation(this),
            new StatusPageOperation(this), new FetchGmaxRPC(this), new TruncateRPC(this),
            new TruncateLocalRPC(this), new DeleteOperation(this), new DeleteOFTRPC(this),
            new DeleteLocalRPC(this), new GetProtocolVersionOperation(this),
            new ShutdownOperation(this), new CheckObjectRPC(this), new GmaxEvent(this),
            new CloseFileEvent(this), new GetStatistics(this), new StatisticsConfig(this),
            new AcquireLease(this), new ReturnLease(this), new CleanUpOperation(this),
            new FetchAndWriteReplica(this), new ReadLocalRPC(this) };
        
        // -------------------------------
        // initialize communication stages
        // -------------------------------
        
        pinky = config.isUsingSSL() ? new PipelinedPinky(config.getPort(), config.getAddress(),
            this, new SSLOptions(config.getServiceCredsFile(), config.getServiceCredsPassphrase(),
                config.getServiceCredsContainer(), config.getTrustedCertsFile(), config
                        .getTrustedCertsPassphrase(), config.getTrustedCertsContainer(), false))
            : new PipelinedPinky(config.getPort(), config.getAddress(), this);
        pinky.setLifeCycleListener(this);

        speedy = config.isUsingSSL() ? new MultiSpeedy(new SSLOptions(config
                    .getServiceCredsFile(), config.getServiceCredsPassphrase(), config
                    .getServiceCredsContainer(), config.getTrustedCertsFile(), config
                    .getTrustedCertsPassphrase(), config.getTrustedCertsContainer(), false))
                : new MultiSpeedy();
        speedy.setLifeCycleListener(this);
        
        udpCom = new UDPCommunicator(config.getPort(), this);
        udpCom.setLifeCycleListener(this);
        
        // --------------------------
        // initialize internal stages
        // --------------------------
        
        MetadataCache metadataCache = new MetadataCache();
        StorageLayout storageLayout = new HashStorageLayout(config, metadataCache);
        
        // TODO: use UUID resolution instead
        Striping striping = new Striping(config.getUUID(), metadataCache);
        
        statistics = new StageStatistics();
        
        // IMPORTANT: the order of stages defined in 'RequestDispatcher.Stages'
        // has to be preserved!
        stages = new Stage[] { new ParserStage(this), new AuthenticationStage(this),
            new StorageStage(this, striping, metadataCache, storageLayout, 1),
            new DeletionStage(this, striping, metadataCache, storageLayout),
            new StatisticsStage(this, statistics, 60 * 10), new ReplicationStage(this) };
        
        for (Stage stage : stages)
            stage.setLifeCycleListener(this);
        
        // ----------------------------------------
        // initialize TimeSync and Heartbeat thread
        // ----------------------------------------
        
        dirClient = new DIRClient(speedy, config.getDirectoryService());
        osdClient = new OSDClient(speedy);
        
        TimeSync.initialize(dirClient, config.getRemoteTimeSync(), config.getLocalClockRenew(),
            authString);
        UUIDResolver.start(dirClient, 10 * 1000, 600 * 1000);
        UUIDResolver.addLocalMapping(config.getUUID(), config.getPort(), config.isUsingSSL());
        
        ServiceDataGenerator gen = new ServiceDataGenerator() {
            public Map<String, Map<String, Object>> getServiceData() {
                
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
                long usedRAM = Runtime.getRuntime().totalMemory()
                    - Runtime.getRuntime().freeMemory();
                
                Map<String, Map<String, Object>> data = new HashMap<String, Map<String, Object>>();
                data.put(config.getUUID().toString(), RPCClient.generateMap("type", "OSD", "free",
                    freeSpace, "total", totalSpace, "load", load, "prot_versions",
                    VersionManagement.getSupportedProtVersAsString(), "totalRAM", Long
                            .toString(totalRAM), "usedRAM", Long.toString(usedRAM),
                    "geoCoordinates", config.getGeoCoordinates()));
                return data;
            }
        };
        heartbeatThread = new HeartbeatThread("OSD HB Thr", dirClient, config.getUUID(), gen,
            authString,config);
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "OSD at " + this.config.getUUID()
                + " ready");
    }
    
    public void start() {
        
        try {
            
            pinky.start();
            speedy.start();
            udpCom.start();
            
            pinky.waitForStartup();
            speedy.waitForStartup();
            udpCom.waitForStartup();
            
            TimeSync.initialize(new DIRClient(speedy, new InetSocketAddress("localhost", 32638)),
                60000, 50, authString);
            
            for (Stage stage : stages)
                stage.start();
            
            for (Stage stage : stages)
                stage.waitForStartup();
            
            heartbeatThread.start();
            heartbeatThread.waitForStartup();
            
            Logging.logMessage(Logging.LEVEL_INFO, this,
                "RequestController and all services operational");
            
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
            
            pinky.shutdown();
            speedy.shutdown();
            udpCom.shutdown();
            
            for (Stage stage : stages)
                stage.shutdown();
            
            pinky.waitForShutdown();
            try {
                speedy.waitForShutdown();
            } catch (Exception exc) {
                // FIXME: workaround to protect the system from crashing if an
                // error occurs during speedy shutdown. A proper better error
                // handling is the better solution here!
                Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
            }
            udpCom.waitForShutdown();
            
            for (Stage stage : stages)
                stage.waitForShutdown();
            
            Logging.logMessage(Logging.LEVEL_INFO, this, "OSD and all stages terminated");
            
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "shutdown failed");
            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
        }
    }
    
    public void receiveRequest(PinkyRequest theRequest) {
        
        /*
         * if (Logging.tracingEnabled()) Logging.logMessage(Logging.LEVEL_DEBUG,
         * this, "received request: " + theRequest.requestMethod + " #" +
         * requestId);
         */
        if (Tracer.COLLECT_TRACES) {
            Tracer.trace(theRequest.requestHeaders.getHeader(HTTPHeaders.HDR_XREQUESTID),
                requestId, Tracer.TraceEvent.RECEIVED, null, null);
        }
        
        OSDRequest rq = new OSDRequest(requestId++);
        rq.setPinkyRequest(theRequest);
        
        if (Logging.tracingEnabled())
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "proessing " + rq);
        
        stages[RequestDispatcher.Stages.PARSER.ordinal()].enqueueOperation(rq,
            ParserStage.STAGEOP_PARSE, null);
    }
    
    public void sendSpeedyRequest(Request originalRequest, SpeedyRequest speedyRq,
        InetSocketAddress server) throws IOException {
        
        speedyRq.setOriginalRequest(originalRequest);
        speedy.sendRequest(speedyRq, server);
        
    }
    
    public void requestFinished(OSDRequest rq) {
        
        assert (rq != null);
        
        final PinkyRequest pr = rq.getPinkyRequest();
        assert (pr != null);
        
        if (!pr.responseSet) {
            if (rq.getError() == null) {
                
                HTTPHeaders headers = new HTTPHeaders();
                
                String fsUpdate = rq.getDetails().getNewFSandEpoch();
                if (fsUpdate != null)
                    headers.addHeader(HTTPHeaders.HDR_XNEWFILESIZE, fsUpdate);
                
                // if the checksum was invalid when reading data, return the
                // data but add a header indicating a wrong checksum to the
                // client
                if (rq.getDetails().isInvalidChecksum())
                    headers.addHeader(HTTPHeaders.HDR_XINVALIDCHECKSUM, "true");
                
                final String rqId = rq.getDetails().getRequestId();
                if (rqId != null)
                    headers.addHeader(HTTPHeaders.HDR_XREQUESTID, rqId);
                
                // add additional headers, if exist
                if(rq.getAdditionalResponseHTTPHeaders()!=null)
                    for(HeaderEntry header : rq.getAdditionalResponseHTTPHeaders()){
                        headers.addHeader(header.name, header.value);
                    }
                
//                String mimeType = null;
//                if (rq.getData() != null) {
//                    
//                    String mimeType = null;
//                    
//                    switch (rq.getDataType()) {
//                    case BINARY:
//                        mimeType = HTTPUtils.BIN_TYPE;
//                        break;
//                    case JSON:
//                        mimeType = HTTPUtils.JSON_TYPE;
//                        break;
//                    case HTML:
//                        mimeType = HTTPUtils.HTML_TYPE;
//                        break;
//                    }
//                    
//                    headers.addHeader(HTTPHeaders.HDR_CONTENT_TYPE, mimeType);
//                }
                
                if (Tracer.COLLECT_TRACES)
                    Tracer.trace(rqId, rq.getRequestId(), Tracer.TraceEvent.RESPONSE_SENT, null,
                        null);
                
                pr.setResponse(HTTPUtils.SC_OKAY, rq.getData(), rq.getDataType() == null ? HTTPUtils.DATA_TYPE.JSON : rq.getDataType(), headers);
                
            } else {
                
                final ErrorRecord error = rq.getError();
                switch (error.getErrorClass()) {
                case INTERNAL_SERVER_ERROR: {
                    pr.setResponse(HTTPUtils.SC_SERVER_ERROR, error.getErrorMessage() + "\n\n");
                    break;
                }
                case USER_EXCEPTION: {
                    pr.setResponse(HTTPUtils.SC_USER_EXCEPTION, error.toJSON());
                    break;
                }
                case REDIRECT: {
                    pr.setResponse(HTTPUtils.SC_SEE_OTHER, error.getErrorMessage());
                    break;
                }
                default: {
                    pr.setResponse(HTTPUtils.SC_SERVER_ERROR,
                        "an unknown error type was returned: " + error);
                    break;
                }
                }
                
                if (rq.getData() != null)
                    BufferPool.free(rq.getData());
                
                if (Tracer.COLLECT_TRACES)
                    Tracer.trace(rq.getDetails().getRequestId(), rq.getRequestId(),
                        Tracer.TraceEvent.ERROR_SENT, null, error.getErrorClass().toString());
            }
        }
        pinky.sendResponse(pr);
        
    }
    
    public OSDConfig getConfig() {
        return config;
    }
    
    public int getPinkyCons() {
        return pinky.getNumConnections();
    }
    
    public int getPinkyQueueLength() {
        return pinky.getTotalQLength();
    }
    
    public Stage getStage(RequestDispatcher.Stages stage) {
        return stages[stage.ordinal()];
    }
    
    public Operation getOperation(RequestDispatcher.Operations opCode) {
        return operations[opCode.ordinal()];
    }
    
    public StageStatistics getStatistics() {
        return statistics;
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
        Logging.logMessage(Logging.LEVEL_ERROR, this,
            "a component crashed... shutting down system!");
        this.shutdown();
    }
    
    public void sendUDP(ReusableBuffer data, InetSocketAddress receiver) {
        udpCom.send(data, receiver);
    }
    
    public void receiveUDP(ReusableBuffer data, InetSocketAddress sender) {
        data.position(0);
        
        int type = (int) data.get();
        
        if (type == UDPMessageType.Striping.ordinal()) {
            // globalmax info for the storage stage
            final Operation gMaxEvent = getOperation(RequestDispatcher.Operations.GMAX);
            OSDRequest rq = new OSDRequest(-1);
            rq.setData(data, HTTPUtils.DATA_TYPE.BINARY);
            rq.setOperation(gMaxEvent);
            gMaxEvent.startRequest(rq);
            
        } else if (type == UDPMessageType.MPXLN.ordinal()) {
            // ignore for now!
        } else {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "unknown UDP message type!");
        }
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
    public boolean isHeadOSD(Location xloc) {
        final ServiceUUID headOSD = xloc.getOSDs().get(0);
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
    
}
