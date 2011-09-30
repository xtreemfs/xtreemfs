/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test;

import java.io.File;
import java.io.IOException;
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
        return osds[0].getConfig().getUUID().getAddress();
    }
    
    public InetSocketAddress getOSDAddress(int osdNumber) throws UnknownUUIDException {
        return osds[osdNumber].getConfig().getUUID().getAddress();
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
            MOCKUP_OSD, // mock-up OSD: registers a non-existing OSD at the DIR
            MOCKUP_OSD2, // mock-up OSD: registers a non-existing OSD at the DIR
            MOCKUP_OSD3, // mock-up OSD: registers a non-existing OSD at the DIR
            OSD
        // an OSD
    };
    
    private RPCNIOSocketClient     rpcClient;

    private org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient     pbrpcClient;
    
    private DIRServiceClient       dirClient;
    
    private MRCServiceClient              mrcClient;
    
    private OSDServiceClient              osdClient;
    
    private DIRRequestDispatcher   dirService;
    
    private MRCRequestDispatcher   mrc;
    
    private OSDRequestDispatcher[] osds;
    
    private final List<Services>   enabledServs;
    
    private TimeSync               tsInstance;
    
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
                dirService = new DIRRequestDispatcher(SetupUtils.createDIRConfig(),
                        SetupUtils.createDIRdbsConfig());
                dirService.startup();
                dirService.waitForStartup();
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "DIR running");
            }

            if (enabledServs.contains(Services.TIME_SYNC) || enabledServs.contains(Services.MOCKUP_OSD)) {
                tsInstance = TimeSync.initializeLocal(60 * 1000, 50);
                tsInstance.waitForStartup();
            }

            if (enabledServs.contains(Services.UUID_RESOLVER)) {
                DIRClient dc = new DIRClient(dirClient, new InetSocketAddress[]{getDIRAddress()}, 10, 1000 * 5);
                UUIDResolver.start(dc, 1000, 10 * 10 * 1000);
                SetupUtils.localResolver();
            }

            if (enabledServs.contains(Services.MOCKUP_OSD)) {
                Map<String,String> dmap = new HashMap();
                dmap.put("free", "1000000000");
                dmap.put("total", "1000000000");
                dmap.put("load", "0");
                dmap.put("totalRAM", "1000000000");
                dmap.put("usedRAM", "0");
                dmap.put("proto_version", "" + OSDServiceConstants.INTERFACE_ID);
                Service reg = Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setName("mockUpOSD").setUuid("mockUpOSD").
                        setVersion(0).setLastUpdatedS(0).setData(ServiceDataMap.newBuilder().addAllData(KeyValuePairs.fromMap(dmap))).build();
                RPCResponse<serviceRegisterResponse> response = dirClient.xtreemfs_service_register(null, RPCAuthentication.authNone, RPCAuthentication.userService, reg);
                response.get();
                response.freeBuffers();

                UUIDResolver.addLocalMapping("mockUpOSD", 11111, Schemes.SCHEME_PBRPC);
            }

            if (enabledServs.contains(Services.MOCKUP_OSD2)) {
                Map<String,String> dmap = new HashMap();
                dmap.put("free", "1000000000");
                dmap.put("total", "1000000000");
                dmap.put("load", "0");
                dmap.put("totalRAM", "1000000000");
                dmap.put("usedRAM", "0");
                dmap.put("proto_version", "" + OSDServiceConstants.INTERFACE_ID);
                Service reg = Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setName("mockUpOSD2").setUuid("mockUpOSD2").
                        setVersion(0).setLastUpdatedS(0).setData(ServiceDataMap.newBuilder().addAllData(KeyValuePairs.fromMap(dmap))).build();
                RPCResponse<serviceRegisterResponse> response = dirClient.xtreemfs_service_register(null, RPCAuthentication.authNone, RPCAuthentication.userService, reg);
                response.get();
                response.freeBuffers();

                UUIDResolver.addLocalMapping("mockUpOSD2", 11111, Schemes.SCHEME_PBRPC);
            }

            if (enabledServs.contains(Services.MOCKUP_OSD3)) {
                Map<String,String> dmap = new HashMap();
                dmap.put("free", "1000000000");
                dmap.put("total", "1000000000");
                dmap.put("load", "0");
                dmap.put("totalRAM", "1000000000");
                dmap.put("usedRAM", "0");
                dmap.put("proto_version", "" + OSDServiceConstants.INTERFACE_ID);
                Service reg = Service.newBuilder().setType(ServiceType.SERVICE_TYPE_OSD).setName("mockUpOSD3").setUuid("mockUpOSD3").
                        setVersion(0).setLastUpdatedS(0).setData(ServiceDataMap.newBuilder().addAllData(KeyValuePairs.fromMap(dmap))).build();
                RPCResponse<serviceRegisterResponse> response = dirClient.xtreemfs_service_register(null, RPCAuthentication.authNone, RPCAuthentication.userService, reg);
                response.get();
                response.freeBuffers();

                UUIDResolver.addLocalMapping("mockUpOSD3", 11111, Schemes.SCHEME_PBRPC);
            }

            if (enabledServs.contains(Services.OSD)) {
                int osdCount = Collections.frequency(enabledServs, Services.OSD);
                osds = new OSDRequestDispatcher[osdCount];
                OSDConfig[] configs = SetupUtils.createMultipleOSDConfigs(osdCount);
                for (int i = 0; i < configs.length; i++) {
                    osds[i] = new OSDRequestDispatcher(configs[i]);
                }
                for (int i = 0; i < configs.length; i++) {
                    osds[i].start();
                }
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
            throw ex;
        }
    }
    
    public void shutdown() {
        Logging.logMessage(Logging.LEVEL_DEBUG, this,"shutting down testEnv...");
                
        if (enabledServs.contains(Services.MRC)) {
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
                    osd.shutdown();
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
