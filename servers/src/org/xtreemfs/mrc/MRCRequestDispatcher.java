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
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.foundation.oncrpc.server.RPCNIOSocketServer;
import org.xtreemfs.foundation.oncrpc.server.RPCServerRequestListener;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.include.foundation.json.JSONException;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.KeyValuePair;
import org.xtreemfs.interfaces.KeyValuePairSet;
import org.xtreemfs.interfaces.ServiceRegistry;
import org.xtreemfs.interfaces.ServiceRegistrySet;
import org.xtreemfs.interfaces.Exceptions.MRCException;
import org.xtreemfs.interfaces.Exceptions.ProtocolException;
import org.xtreemfs.interfaces.Exceptions.RedirectException;
import org.xtreemfs.interfaces.MRCInterface.MRCInterface;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.DBAccessResultListener;
import org.xtreemfs.mrc.database.DatabaseException;
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

/**
 * 
 * @author bjko
 */
public class MRCRequestDispatcher implements RPCServerRequestListener, LifeCycleListener,
    DBAccessResultListener {
    
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
    
    public MRCRequestDispatcher(final MRCConfig config) throws IOException, JSONException,
        ClassNotFoundException, IllegalAccessException, InstantiationException, DatabaseException {
        
        this.config = config;
        
        Logging.logMessage(Logging.LEVEL_DEBUG, this, "use SSL=" + config.isUsingSSL());
        
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
            Logging.logMessage(Logging.LEVEL_INFO, this, "using authentication provider '"
                + authProvider.getClass().getName() + "'");
        
        osdMonitor = new OSDStatusManager(config, dirClient, policyContainer);
        osdMonitor.setLifeCycleListener(this);
        
        procStage = new ProcessingStage(this);
        
        volumeManager = new BabuDBVolumeManager(this);
        fileAccessManager = new FileAccessManager(volumeManager, policyContainer);
        
        ServiceDataGenerator gen = new ServiceDataGenerator() {
            public ServiceRegistrySet getServiceData() {
                
                String uuid = config.getUUID().toString();
                
                OperatingSystemMXBean osb = ManagementFactory.getOperatingSystemMXBean();
                String load = String.valueOf((int) (osb.getSystemLoadAverage() * 100 / osb
                        .getAvailableProcessors()));
                
                long totalRAM = Runtime.getRuntime().maxMemory();
                long usedRAM = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                
                // get service data
                ServiceRegistrySet sregs = new ServiceRegistrySet();
                KeyValuePairSet kvset = new KeyValuePairSet();
                kvset.add(new KeyValuePair("proto_version", Integer.toString(MRCInterface.getVersion())));
                kvset.add(new KeyValuePair("totalRAM", Long.toString(totalRAM)));
                kvset.add(new KeyValuePair("usedRAM", Long.toString(usedRAM)));
                kvset.add(new KeyValuePair("geoCoordinated", config.getGeoCoordinates()));
                
                ServiceRegistry mrcReg = new ServiceRegistry(uuid, 0, Constants.SERVICE_TYPE_MRC, "MRC @ "
                    + uuid, kvset);
                sregs.add(mrcReg);
                
                try {
                    for (VolumeInfo vol : volumeManager.getVolumes()) {
                        ServiceRegistry dsVolumeInfo = MRCHelper.createDSVolumeInfo(vol, osdMonitor, uuid);
                        sregs.add(dsVolumeInfo);
                    }
                } catch (DatabaseException exc) {
                    Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
                }
                
                return sregs;
            }
            
        };
        
        heartbeatThread = new HeartbeatThread("MRC Heartbeat Thread", dirClient, config.getUUID(), gen,
            config);
    }
    
    public void startup() throws Exception {
        
        TimeSync.initialize(dirClient, config.getRemoteTimeSync(), config.getLocalClockRenew());
        
        UUIDResolver.start(dirClient, 10 * 1000, 600 * 1000);
        UUIDResolver.addLocalMapping(config.getUUID(), config.getPort(), config.isUsingSSL());
        
        clientStage.start();
        clientStage.waitForStartup();
        osdMonitor.start();
        osdMonitor.waitForStartup();
        
        procStage.start();
        procStage.waitForStartup();
        
        volumeManager.init();
        volumeManager.addVolumeChangeListener(osdMonitor);
        
        heartbeatThread.start();
        
        serverStage.start();
        serverStage.waitForStartup();
        
        Logging
                .logMessage(Logging.LEVEL_INFO, this, "MRC operational, listening on port "
                    + config.getPort());
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
        
        TimeSync.getInstance().shutdown();
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
                Logging.logMessage(Logging.LEVEL_ERROR, this, error.getErrorMessage() + " / request: "
                    + request);
                Logging.logMessage(Logging.LEVEL_ERROR, this, error.getThrowable());
                rpcRequest.sendInternalServerError(error.getThrowable());
                break;
            }
            case USER_EXCEPTION: {
                rpcRequest.sendGenericException(new MRCException(error.getErrorCode(), error
                        .getErrorMessage(), error.getStackTrace()));
                break;
            }
            case INVALID_ARGS: {
                rpcRequest.sendGarbageArgs(error.getErrorMessage());
                break;
            }
            case UNKNOWN_OPERATION: {
                rpcRequest.sendProtocolException(new ProtocolException(
                    ONCRPCResponseHeader.ACCEPT_STAT_PROC_UNAVAIL, error.getErrorCode(), error
                            .getStackTrace()));
                break;
            }
                
            default: {
                rpcRequest.sendInternalServerError(error.getThrowable());
                break;
            }
            }
        }

        else {
            assert (request.getResponse() != null);
            rpcRequest.sendResponse(request.getResponse());
        }
        
    }
    
    public Map<StatusPageOperation.Vars, String> getStatusInformation() {
        HashMap<StatusPageOperation.Vars, String> data = new HashMap();
        
        data.put(Vars.AVAILPROCS, String.valueOf(Runtime.getRuntime().availableProcessors()));
        data.put(Vars.BPSTATS, BufferPool.getStatus());
        data.put(Vars.DEBUG, Integer.toString(config.getDebugLevel()));
        data.put(Vars.DIRURL, "http://" + config.getDirectoryService().getHostName() + ":"
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
        
        // TODO: add request statistics
        // StringBuffer rqTableBuf = new StringBuffer();
        // long totalRequests = 0;
        // for (String req : brainStage._statMap.keySet()) {
        //
        // long count = brainStage._statMap.get(req);
        // totalRequests += count;
        //
        // rqTableBuf.append("<tr><td align=\"left\">'");
        // rqTableBuf.append(req);
        // rqTableBuf.append("'</td><td>");
        // rqTableBuf.append(count);
        // rqTableBuf.append("</td></tr>");
        // }
        
        // add volume statistics
        try {
            StringBuffer volTableBuf = new StringBuffer();
            Collection<VolumeInfo> vols = volumeManager.getVolumes();
            for (VolumeInfo v : vols) {
                
                ServiceRegistrySet osdList = osdMonitor.getUsableOSDs(v.getId());
                
                volTableBuf.append("<tr><td align=\"left\">");
                volTableBuf.append(v.getName());
                volTableBuf
                        .append("</td><td><table border=\"0\" cellpadding=\"0\"><tr><td class=\"subtitle\">selectable OSDs</td><td align=\"right\">");
                Iterator<ServiceRegistry> it = osdList.iterator();
                while (it.hasNext()) {
                    ServiceRegistry osd = it.next();
                    final ServiceUUID osdUUID = new ServiceUUID(osd.getUuid());
                    volTableBuf.append("<a href=\"");
                    volTableBuf.append(osdUUID.toURL());
                    volTableBuf.append("\">");
                    volTableBuf.append(osdUUID);
                    volTableBuf.append("</a>");
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
    
    public void crashPerformed() {
        Logging.logMessage(Logging.LEVEL_ERROR, this,
            "A component crashed. Shutting down MRC, trying to checkpoint database");
        try {
            shutdown();
        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, e);
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
        Logging.logMessage(Logging.LEVEL_TRACE, this, "received new request");
        
        final ONCRPCRequestHeader hdr = rq.getRequestHeader();
        
        if (hdr.getInterfaceVersion() != MRCInterface.getVersion()) {
            rq.sendProtocolException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_PROG_MISMATCH, 0,
                "invalid version requested"));
            return;
        }
        
        // no callback, special stage which executes the operatios
        procStage.enqueueOperation(new MRCRequest(rq), ProcessingStage.STAGEOP_PARSE_AND_EXECUTE, null);
    }
    
}
