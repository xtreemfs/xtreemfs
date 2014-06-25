/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.foundation.CrashReporter;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.util.FSUtils;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceDataMap;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceRegisterResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.SchedulerServiceClient;
import org.xtreemfs.scheduler.SchedulerConfig;
import org.xtreemfs.scheduler.SchedulerRequestDispatcher;

/**
 * 
 * @author bjko
 */
public class TestEnvironment {
    
    public InetSocketAddress getMRCAddress() throws UnknownUUIDException {
        return mrc.getConfig().getUUID().getAddress();
    }
    
    public InetSocketAddress getDIRAddress() throws IOException {
        return new InetSocketAddress("localhost", SetupUtils.createDIRConfig().getPort());
    }

    public InetSocketAddress getSchedulerAddress() throws IOException {
        return new InetSocketAddress("localhost", SetupUtils.createSchedulerConfig(false).getPort());
    }
    
    /**
     * @return the rpcClient
     */
    public RPCNIOSocketClient getRpcClient() {
        return rpcClient;
    }
    
    /**
     * @return the dirClient
     */
    public DIRServiceClient getDirClient() {
        return dirClient;
    }
    
    /**
     * @return the mrcClient
     */
    public MRCServiceClient getMrcClient() {
        return mrcClient;
    }
    
    public OSDServiceClient getOSDClient() {
        return osdClient;
    }
    
    public SchedulerServiceClient getSchedulerClient() {
    	return schedulerClient;
    }
    
    /**
     * returns always the address of the first OSD
     */
    public InetSocketAddress getOSDAddress() throws UnknownUUIDException {
        return osds[0].getConfig().getUUID().getAddress();
    }
    
    public InetSocketAddress getOSDAddress(int osdNumber) throws UnknownUUIDException {
        return osds[osdNumber].getConfig().getUUID().getAddress();
    }
    
    /**
     * Returns the OSD config of the OSD with the UUID "osdUuid" or null if no OSD with the UUID "osdUuid"
     * exists.
     * 
     * @param osdUuid
     */
    public OSDConfig getOSDConfig(String osdUuid) {

        OSDConfig config = null;
        for (OSDRequestDispatcher osd : osds) {
            if (osd.getConfig().getUUID().toString().equals(osdUuid)) {
                config = osd.getConfig();
                break;
            }
        }
        if (hasAdditionalOsds && config == null) {
            for (OSDRequestDispatcher osd : additionalOsds) {
                if (osd.getConfig().getUUID().toString().equals(osdUuid)) {
                    config = osd.getConfig();
                    break;
                }
            }
        }
        return config;
    }

    /**
     * Stops the OSD with the UUID "osdUuid".
     * 
     * @param osdUuid
     */
    public void stopOSD(String osdUuid) {
        boolean osdStoped = false;
        for (OSDRequestDispatcher osd : osds) {
            if (osd.getConfig().getUUID().toString().equals(osdUuid)) {
                osd.shutdown();
                osdStoped = true;
                break;
            }
        }

        if (hasAdditionalOsds && !osdStoped) {
            for (OSDRequestDispatcher osd : additionalOsds) {
                if (osd.getConfig().getUUID().toString().equals(osdUuid)) {
                    osd.shutdown();
                    break;
                }
            }
        }
    }

    /**
     * Restarts the OSD with the UUID "osdUuid".
     * 
     * @param osdUuid
     * @throws Exception 
     */
    public void restartOSD(String osdUuid) throws Exception {
        boolean osdStarted = false;
        for (int i = 0; i < osds.length; i++) {
            if (osds[i].getConfig().getUUID().toString().equals(osdUuid)) {
                osds[i] = new OSDRequestDispatcher(osdConfigs[i]);
                osds[i].start();
                break;
            }
        }
        
        if (hasAdditionalOsds && !osdStarted) {
            for (int i = 0; i < additionalOsdCount; i++) {
                if (additionalOsds[i].getConfig().getUUID().toString().equals(osdUuid)) {
                    additionalOsds[i] = new OSDRequestDispatcher(osdConfigs[osdCount + i]);
                    additionalOsds[i].start();
                    break;
                }
            }
        }
    }

    /**
     * starts "number" additional OSDs (up to 5).
     * 
     * @param number
     * @throws Exception
     */
    public void startAdditionalOSDs(int number) throws Exception {
        assert (number <= 5);
        hasAdditionalOsds = true;
        for (int i = 0; i < number; i++) {
            additionalOsds[i] = new OSDRequestDispatcher(osdConfigs[osdCount + i]);
            additionalOsds[i].start();
        }
        additionalOsdCount = number;
    }

    /**
     * Stops all additional OSDs
     */
    public void stopAdditionalOSDs() {
        hasAdditionalOsds = false;
        for (int i = 0; i < additionalOsdCount; i++) {
            additionalOsds[i].shutdown();
        }
        additionalOsdCount = 0;
    }

    /**
     * Returns the primary OSD UUID for the file with ID "fileId" or null if the file does not exists.
     * 
     * @param fileId
     * @return
     */
    public String getPrimary(String fileId) {
        String primary = null;

        for (OSDRequestDispatcher osd : osds) {
            primary = osd.getPrimary(fileId);
            if (primary != null) {
                break;
            }
        }

        if (hasAdditionalOsds && primary == null) {
            for (OSDRequestDispatcher osd : additionalOsds) {
                primary = osd.getPrimary(fileId);
                if (primary != null) {
                    break;
                }
            }
        }
        return primary;
    }

    /**
     * @return the osdClient
     */
    // public OSDClient getOsdClient() {
    // return osdClient;
    // }
    public enum Services {
        TIME_SYNC, // time sync facility
        UUID_RESOLVER, // UUID resolver
        RPC_CLIENT, // RPC client
        DIR_CLIENT, // Directory Service client
        MRC_CLIENT, // MRC client
        OSD_CLIENT, // OSD client
        DIR_SERVICE, // Directory Service
        MRC, // MRC
        MRC_QOS, // MRC with QoS features
        MOCKUP_OSD, // mock-up OSD: registers a non-existing OSD at the DIR
        MOCKUP_OSD2, // mock-up OSD: registers a non-existing OSD at the DIR
        MOCKUP_OSD3, // mock-up OSD: registers a non-existing OSD at the DIR
        OSD, // an OSD
        SCHEDULER_SERVICE, // Scheduler
        SCHEDULER_CLIENT // Scheduler client
    };
    
    private RPCNIOSocketClient                                      rpcClient;
    
    private org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient pbrpcClient;
    

    private DIRServiceClient                                        dirClient;
    
    private MRCServiceClient                                        mrcClient;
    
    private OSDServiceClient                                        osdClient;
    
    private SchedulerServiceClient									schedulerClient;
    
    private DIRRequestDispatcher                                    dirService;
    
    private MRCRequestDispatcher                                    mrc;
    
    private OSDRequestDispatcher[]                                  osds;
    
    private SchedulerRequestDispatcher                              scheduler;

    private OSDRequestDispatcher[]                                  additionalOsds;

    private final List<Services>                                    enabledServs;
    
    private TimeSync                                                tsInstance;

    private boolean                                                 hasAdditionalOsds = false;
    
    private OSDConfig[]                                             osdConfigs;
    
    private int                                                     osdCount;

    private int                                                     additionalOsdCount;

    public TestEnvironment(Services... servs) {
        enabledServs = new ArrayList(servs.length);
        for (Services serv : servs)
            enabledServs.add(serv);
    }
    
    public void start() throws Exception {
        try {
            // ensure that TEST_DIR is empty
            File testDir = new File(SetupUtils.TEST_DIR);
            FSUtils.delTree(testDir);
            testDir.mkdirs();
            
            rpcClient = SetupUtils.createRPCClient(10000);
            getRpcClient().start();
            getRpcClient().waitForStartup();
            
            dirClient = SetupUtils.createDIRClient(getRpcClient());

            if (enabledServs.contains(Services.SCHEDULER_CLIENT)) {
                schedulerClient = SetupUtils.createSchedulerClient(getRpcClient());
            }

            if (enabledServs.contains(Services.DIR_SERVICE)) {
                dirService = new DIRRequestDispatcher(SetupUtils.createDIRConfig(), SetupUtils.createDIRdbsConfig());
                dirService.startup();
                dirService.waitForStartup();
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "DIR running");
            }
            
            if (enabledServs.contains(Services.TIME_SYNC) || enabledServs.contains(Services.MOCKUP_OSD)) {
                tsInstance = TimeSync.initializeLocal(50);
                tsInstance.waitForStartup();
            }
            
            if (enabledServs.contains(Services.UUID_RESOLVER)) {
                DIRClient dc = new DIRClient(dirClient, new InetSocketAddress[] { getDIRAddress() }, 10, 1000 * 5);
                UUIDResolver.start(dc, 1000, 10 * 10 * 1000);
                SetupUtils.localResolver();
            }
            
            if (enabledServs.contains(Services.MOCKUP_OSD)) {
                Map<String, String> dmap = new HashMap();
                dmap.put("free", "1000000000");
                dmap.put("total", "1000000000");
                dmap.put("load", "0");
                dmap.put("totalRAM", "1000000000");
                dmap.put("usedRAM", "0");
                dmap.put("proto_version", "" + OSDServiceConstants.INTERFACE_ID);
                Service reg = Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setName("mockUpOSD")
                        .setUuid("mockUpOSD").setVersion(0).setLastUpdatedS(0)
                        .setData(ServiceDataMap.newBuilder().addAllData(KeyValuePairs.fromMap(dmap))).build();
                RPCResponse<serviceRegisterResponse> response = dirClient.xtreemfs_service_register(null,
                        RPCAuthentication.authNone, RPCAuthentication.userService, reg);
                response.get();
                response.freeBuffers();
                
                UUIDResolver.addLocalMapping("mockUpOSD", 11111, Schemes.SCHEME_PBRPC);
            }
            
            if (enabledServs.contains(Services.MOCKUP_OSD2)) {
                Map<String, String> dmap = new HashMap();
                dmap.put("free", "1000000000");
                dmap.put("total", "1000000000");
                dmap.put("load", "0");
                dmap.put("totalRAM", "1000000000");
                dmap.put("usedRAM", "0");
                dmap.put("proto_version", "" + OSDServiceConstants.INTERFACE_ID);
                Service reg = Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setName("mockUpOSD2")
                        .setUuid("mockUpOSD2").setVersion(0).setLastUpdatedS(0)
                        .setData(ServiceDataMap.newBuilder().addAllData(KeyValuePairs.fromMap(dmap))).build();
                RPCResponse<serviceRegisterResponse> response = dirClient.xtreemfs_service_register(null,
                        RPCAuthentication.authNone, RPCAuthentication.userService, reg);
                response.get();
                response.freeBuffers();
                
                UUIDResolver.addLocalMapping("mockUpOSD2", 11111, Schemes.SCHEME_PBRPC);
            }
            
            if (enabledServs.contains(Services.MOCKUP_OSD3)) {
                Map<String, String> dmap = new HashMap();
                dmap.put("free", "1000000000");
                dmap.put("total", "1000000000");
                dmap.put("load", "0");
                dmap.put("totalRAM", "1000000000");
                dmap.put("usedRAM", "0");
                dmap.put("proto_version", "" + OSDServiceConstants.INTERFACE_ID);
                Service reg = Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setName("mockUpOSD3")
                        .setUuid("mockUpOSD3").setVersion(0).setLastUpdatedS(0)
                        .setData(ServiceDataMap.newBuilder().addAllData(KeyValuePairs.fromMap(dmap))).build();
                RPCResponse<serviceRegisterResponse> response = dirClient.xtreemfs_service_register(null,
                        RPCAuthentication.authNone, RPCAuthentication.userService, reg);
                response.get();
                response.freeBuffers();
                
                UUIDResolver.addLocalMapping("mockUpOSD3", 11111, Schemes.SCHEME_PBRPC);
            }
            
            if (enabledServs.contains(Services.OSD)) {
                osdCount = Collections.frequency(enabledServs, Services.OSD);
                osds = new OSDRequestDispatcher[osdCount];
                //for up to 5 additional OSDs
                additionalOsds = new OSDRequestDispatcher[5];
                osdConfigs = SetupUtils.createMultipleOSDConfigs(osdCount + 5);

                for (int i = 0; i < osdCount; i++) {
                    osds[i] = new OSDRequestDispatcher(osdConfigs[i]);
                    osds[i].start();
                }

                Logging.logMessage(Logging.LEVEL_DEBUG, this, "OSDs 1-" + osdCount + " running");
            }
            
            if (enabledServs.contains(Services.MRC)) {
                mrc = new MRCRequestDispatcher(SetupUtils.createMRC1Config(), SetupUtils.createMRC1dbsConfig());
                mrc.startup();
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "MRC running");
            }

            if (enabledServs.contains(Services.MRC_QOS)) {
                if (enabledServs.contains(Services.MRC)) {
                    Logging.logMessage(Logging.LEVEL_WARN, this, "MRC without QoS already running, QoS is ignored");
                } else {
                    mrc = new MRCRequestDispatcher(SetupUtils.createMRCQoSConfig(), SetupUtils.createMRCQoSdbsConfig());
                    mrc.startup();
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "MRC with QoS running");
                }
            }
            
            if (enabledServs.contains(Services.SCHEDULER_SERVICE)) {
                SchedulerConfig schedulerConfig = SetupUtils.createSchedulerConfig(false);
                String capabilityFile = schedulerConfig.getOSDCapabilitiesFile();
                BufferedWriter output = new BufferedWriter(new FileWriter(capabilityFile));
                if(osdConfigs != null) {
                    for (OSDConfig osdConfig : osdConfigs) {
                        output.write(osdConfig.getUUID() + ";100.0;100.0;100.0,99.0,98.0,97.0,96.0,95.0\n");
                    }
                }
                output.close();

            	scheduler = new SchedulerRequestDispatcher(schedulerConfig, SetupUtils.createSchedulerdbsConfig());
            	scheduler.startup();
            	scheduler.waitForStartup();
            	Logging.logMessage(Logging.LEVEL_DEBUG, this, "Scheduler running");
            }
            
            if (enabledServs.contains(Services.MRC_CLIENT)) {
                mrcClient = new MRCServiceClient(rpcClient, null);
            }
            
            if (enabledServs.contains(Services.OSD_CLIENT)) {
                osdClient = new OSDServiceClient(rpcClient, null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            // Shutdown servers which were already started or they will block ports.
            shutdown();

            // After shutdown, log remaining threads in case of blocked ports to debug the issue.
            if (ex instanceof BindException && ex.getMessage().contains("Address already in use")) {
                Logging.logMessage(
                        Logging.LEVEL_ERROR,
                        this,
                        "TestEnvironment could not be started because: "
                                + ex.getMessage()
                                + " Please examine the following dump of threads to check if a previous test method did not correctly stop all servers.");
                StringBuilder threadStates = new StringBuilder();
                CrashReporter.reportThreadStates(threadStates);
                Logging.logMessage(Logging.LEVEL_ERROR, this, "Thread States: %s", threadStates.toString());
            }
            
            throw ex;
        }
    }
    
    public void shutdown() {
        Logging.logMessage(Logging.LEVEL_DEBUG, this, "shutting down testEnv...");
        
        if (enabledServs.contains(Services.MRC) || enabledServs.contains(Services.MRC_QOS)) {
            try {
                if (mrc != null)
                    mrc.shutdown();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }

        if (enabledServs.contains(Services.OSD)) {
            try {
                for (OSDRequestDispatcher osd : osds) {
                    if (osd != null) {
                        osd.shutdown();
                    }
                }
                if (hasAdditionalOsds) {
                    stopAdditionalOSDs();
                }
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        
        if (enabledServs.contains(Services.UUID_RESOLVER)) {
            try {
                UUIDResolver.shutdown();
            } catch (Throwable th) {
            }
        }
        
        if (enabledServs.contains(Services.DIR_SERVICE)) {
            try {
                dirService.shutdown();
                dirService.waitForShutdown();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        
        if (enabledServs.contains(Services.SCHEDULER_SERVICE)) {
        	try {
        		scheduler.shutdown();
        		scheduler.waitForShutdown();
        	} catch (Throwable th) {
        		th.printStackTrace();
        	}
        }
        
        try {
            getRpcClient().shutdown();
            getRpcClient().waitForShutdown();
        } catch (Throwable th) {
            th.printStackTrace();
        }
        
        if (enabledServs.contains(Services.TIME_SYNC)) {
            try {
                tsInstance = TimeSync.getInstance();
                tsInstance.shutdown();
                tsInstance.waitForShutdown();
            } catch (Throwable th) {
            }
        }
        
        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);
        // FSUtils.delTree(testDir);
    }
}
