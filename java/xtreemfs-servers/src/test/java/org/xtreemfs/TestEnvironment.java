/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs;

import java.io.File;
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

/**
 * 
 * @author bjko
 */
public class TestEnvironment {
    
	/**
	 * Wrapper for OSDRequestDispatcher which stores if an OSD is started.
	 * 
	 * @author lkairies
	 *
	 */
    private class TestOSD {
    	private OSDRequestDispatcher osd;
    	private boolean started;
    	
    	public TestOSD(OSDRequestDispatcher osd) {
            this.osd = osd;
            this.started = false;
    	}
    	
    	public void start() {
            osd.start();
            started = true;
    	}
    	
    	public void shutdown() {
            osd.shutdown();
            started = false;
    	}
    	
    	public String getPrimary(String fileId) {
            return osd.getPrimary(fileId);
    	}
    	
    	public OSDConfig getConfig() {
            return osd.getConfig();
    	}
    }
    
    public InetSocketAddress getMRCAddress() throws UnknownUUIDException {
        return mrc.getConfig().getUUID().getAddress();
    }
    
    public InetSocketAddress getDIRAddress() throws IOException {
        return new InetSocketAddress("localhost", SetupUtils.createDIRConfig().getPort());
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
    
    /**
     * returns always the address of the first OSD
     */
    public InetSocketAddress getOSDAddress() throws UnknownUUIDException {
        return firstOSDAddress;
    }
    
    /**
     * Returns the OSD config of the OSD with the UUID "osdUuid" or null if no OSD with the UUID "osdUuid"
     * exists.
     * 
     * @param osdUuid
     */
    public OSDConfig getOSDConfig(String osdUuid) {
        return osds.get(osdUuid).getConfig();
    }

    /**
     * Returns the OSD config of the first OSD.
     */
    public OSDConfig getOSDConfig() {
        return osdConfigs[0];
    }

    /**
     * Returns the OSD configs currently used.
     */
    public OSDConfig[] getOSDConfigs() {
        return osdConfigs.clone();
    }

    /**
     * Returns the OSD UUIDS currently used.
     */
    public String[] getOSDUUIDs() {
        String[] OSDUUIDs = new String[osdConfigs.length];
        for (int i = 0; i < osdConfigs.length; i++) {
            OSDUUIDs[i] = osdConfigs[i].getUUID().toString();
        }
        return OSDUUIDs;
    }

    /**
     * Stops the OSD with the UUID "osdUuid".
     * 
     * @param osdUuid
     */
    public void stopOSD(String osdUuid) throws Exception{
        TestOSD osd = osds.get(osdUuid);
        
        if (osd == null) {
            throw new Exception("No OSD with UUID " + osdUuid + " available.");
        }

        if (osd.started) {
            osd.shutdown();
        } else {
            throw new Exception("OSD " + osdUuid + " was already stoped!");
        }
    }

    /**
     * Starts a previously shut downed OSD with the UUID "osdUuid".
     * 
     * @param osdUuid
     * @throws Exception 
     */
    public void startOSD(String osdUuid) throws Exception {	
        TestOSD osd = osds.get(osdUuid);

        if (osd == null) {
            throw new Exception("No OSD with UUID " + osdUuid + " available.");
        }

        if (!osd.started) {
            OSDConfig config = getOSDConfig(osdUuid);   
            
            //Create new OSDRequestDispatcher with same config.
            osd = new TestOSD (new OSDRequestDispatcher(config));
            osd.start();
            
            // Replace old OSDRequestDispatcher.
            osds.put(osdUuid, osd);
        } else {
            throw new Exception("OSD " + osdUuid + " is already running!");
        }
    }

    /**
     * starts "number" additional OSDs.
     * 
     * @param number
     * @throws Exception
     */
    public void startAdditionalOSDs(int number) throws Exception {
        OSDConfig[] osdConfigs = SetupUtils.createMultipleOSDConfigs(number, osds.size());
        for (OSDConfig config : osdConfigs) {
        	TestOSD osd = new TestOSD (new OSDRequestDispatcher(config));
        	osd.start();
        	osds.put(config.getUUID().toString(), osd);
        }
    }

    /**
     * Returns the primary OSD UUID for the file with ID "fileId" or null if the file does not exists.
     * 
     * @param fileId
     * @return
     */
    public String getPrimary(String fileId) {
        String primary = null;
        for (TestOSD osd : osds.values()) {
            primary = osd.getPrimary(fileId);
            if (primary != null) {
                break;
            }
        }
        return primary;
    }

    public enum Services {
        TIME_SYNC, // time sync facility
        UUID_RESOLVER, // UUID resolver
        RPC_CLIENT, // RPC client
        DIR_CLIENT, // Directory Service client
        MRC_CLIENT, // MRC client
        OSD_CLIENT, // OSD client
        DIR_SERVICE, // Directory Service
        MRC, // MRC
        MOCKUP_OSD, // mock-up OSD: registers a non-existing OSD at the DIR
        MOCKUP_OSD2, // mock-up OSD: registers a non-existing OSD at the DIR
        MOCKUP_OSD3, // mock-up OSD: registers a non-existing OSD at the DIR
        OSD
        // an OSD
    };
    
    private RPCNIOSocketClient                                      rpcClient;
    
    private org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient pbrpcClient;
    
    private DIRServiceClient                                        dirClient;
    
    private MRCServiceClient                                        mrcClient;
    
    private OSDServiceClient                                        osdClient;
    
    private DIRRequestDispatcher                                    dirService;
    
    private MRCRequestDispatcher                                    mrc;
    
    private HashMap<String, TestOSD>                                osds;

    private final List<Services>                                    enabledServs;
    
    private TimeSync                                                tsInstance;
    
    private OSDConfig[]                                             osdConfigs;
    
    private InetSocketAddress                                       firstOSDAddress;

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
                int osdCount = Collections.frequency(enabledServs, Services.OSD);
                osds = new HashMap<String, TestOSD>(osdCount);               
                osdConfigs = SetupUtils.createMultipleOSDConfigs(osdCount);                           
                for (OSDConfig config : osdConfigs) {
                	TestOSD osd = new TestOSD(new OSDRequestDispatcher(config));
                    osd.start();
                    osds.put(config.getUUID().toString(), osd);
                }
                
                // Save address of first OSD for getOSDAdress method.
                firstOSDAddress = osdConfigs[0].getUUID().getAddress();

                Logging.logMessage(Logging.LEVEL_DEBUG, this, "OSDs 1-" + osdCount + " running");
            }
            
            if (enabledServs.contains(Services.MRC)) {
                mrc = new MRCRequestDispatcher(SetupUtils.createMRC1Config(), SetupUtils.createMRC1dbsConfig());
                mrc.startup();
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "MRC running");
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
        
        if (enabledServs.contains(Services.MRC) && mrc != null) {
            try {
                mrc.shutdown();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
        
        if (enabledServs.contains(Services.OSD)) {
            try {
                for (TestOSD osd : osds.values()) {
                    if (osd != null && osd.started) {
                        osd.shutdown();
                    }
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

        if (enabledServs.contains(Services.DIR_SERVICE) && dirService != null) {
            try {
                dirService.shutdown();
                dirService.waitForShutdown();
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
                if (tsInstance != null) {
                    tsInstance.shutdown();
                    tsInstance.waitForShutdown();
                }
            } catch (Throwable th) {
            }
        }
        
        // cleanup
        File testDir = new File(SetupUtils.TEST_DIR);
        // FSUtils.delTree(testDir);
    }
}
