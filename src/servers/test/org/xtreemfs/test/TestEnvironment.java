/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceDataMap;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.interfaces.OSDInterface.OSDInterface;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.client.OSDClient;

/**
 * 
 * @author bjko
 */
public class TestEnvironment {

    public InetSocketAddress getMRCAddress() throws UnknownUUIDException {
        return mrc.getConfig().getUUID().getAddress();
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
    public DIRClient getDirClient() {
        return dirClient;
    }
    
    /**
     * @return the mrcClient
     */
    public MRCClient getMrcClient() {
        return mrcClient;
    }
    
    public OSDClient getOSDClient() {
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
            OSD // an OSD
    };
    
    private RPCNIOSocketClient   rpcClient;
    
    private DIRClient            dirClient;
    
    private MRCClient            mrcClient;
    
    private OSDClient            osdClient;
    
    private DIRRequestDispatcher dirService;
    
    private MRCRequestDispatcher mrc;

    private OSDRequestDispatcher[] osds;
    
    private final List<Services> enabledServs;
    
    private TimeSync             tsInstance;
    
    public TestEnvironment(Services... servs) {
        enabledServs = new ArrayList(servs.length);
        for (Services serv : servs)
            enabledServs.add(serv);
    }
    
    public void start() throws Exception {
        
        // ensure that TEST_DIR is empty
        File testDir = new File(SetupUtils.TEST_DIR);
        FSUtils.delTree(testDir);
        testDir.mkdirs();
        
        rpcClient = SetupUtils.createRPCClient(10000);
        getRpcClient().start();
        getRpcClient().waitForStartup();
        
        dirClient = SetupUtils.createDIRClient(getRpcClient());
        
        if (enabledServs.contains(Services.DIR_SERVICE)) {
            dirService = new DIRRequestDispatcher(SetupUtils.createDIRConfig());
            dirService.startup();
            dirService.waitForStartup();
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "DIR running");
        }
        
        if (enabledServs.contains(Services.TIME_SYNC)) {
            tsInstance = TimeSync.initialize(null, 60 * 1000, 50);
            tsInstance.waitForStartup();
        }
        
        if (enabledServs.contains(Services.UUID_RESOLVER)) {
            UUIDResolver.start(getDirClient(), 1000, 10 * 10 * 1000);
            SetupUtils.localResolver();
        }
        
        if (enabledServs.contains(Services.MOCKUP_OSD)) {
            ServiceDataMap dmap = new ServiceDataMap();
            dmap.put("free", "1000000000");
            dmap.put("total", "1000000000");
            dmap.put("load", "0");
            dmap.put("totalRAM", "1000000000");
            dmap.put("usedRAM", "0");
            dmap.put("proto_version", "" + OSDInterface.getVersion());
            Service reg = new Service(ServiceType.SERVICE_TYPE_OSD, "mockUpOSD", 0, "mockUpOSD", 0, dmap);
            RPCResponse<Long> response = dirClient.xtreemfs_service_register(null, reg);
            response.get();
            response.freeBuffers();
            
            UUIDResolver.addLocalMapping("mockUpOSD", 11111, false);
        }

        if (enabledServs.contains(Services.OSD)) {
            int osdCount = Collections.frequency(enabledServs, Services.OSD);
            osds = new OSDRequestDispatcher[osdCount];
            OSDConfig[] configs = SetupUtils.createMultipleOSDConfigs(osdCount);
            for (int i = 0; i < configs.length; i++) {
                osds[i] = new OSDRequestDispatcher(configs[i]);
                osds[i].start();
            }
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "OSDs 1-" + osdCount + " running");
        }
        
        if (enabledServs.contains(Services.MRC)) {
            mrc = new MRCRequestDispatcher(SetupUtils.createMRC1Config());
            mrc.startup();
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "MRC running");
        }


        
        if (enabledServs.contains(Services.MRC_CLIENT)) {
            mrcClient = new MRCClient(rpcClient, null);
        }
        
        if (enabledServs.contains(Services.OSD_CLIENT)) {
            osdClient = new OSDClient(rpcClient);
        }
        
    }
    
    public void shutdown() {
        
        if (enabledServs.contains(Services.UUID_RESOLVER)) {
            try {
                UUIDResolver.shutdown();
            } catch (Throwable th) {
            }
        }
        
        if (enabledServs.contains(Services.MRC)) {
            try {
                mrc.shutdown();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }

        if (enabledServs.contains(Services.OSD)) {
            try {
                for (OSDRequestDispatcher osd : osds)
                    osd.shutdown();
            } catch (Throwable th) {
                th.printStackTrace();
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
        //FSUtils.delTree(testDir);
    }
    
}
