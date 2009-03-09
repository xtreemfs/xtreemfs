/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.util.FSUtils;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.include.common.logging.Logging;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.new_osd.client.OSDClient;

/**
 * 
 * @author bjko
 */
public class TestEnvironment {
    
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
     * @return the osdClient
     */
    // public OSDClient getOsdClient() {
    // return osdClient;
    // }
    public enum Services {
        TIME_SYNC, UUID_RESOLVER, RPC_CLIENT, DIR_CLIENT, MRC_CLIENT, OSD_CLIENT, DIR_SERVICE, MRC
    };
    
    private RPCNIOSocketClient   rpcClient;
    
    private DIRClient            dirClient;
    
    private MRCClient            mrcClient;
    
    private OSDClient osdClient;
    
    private DIRRequestDispatcher dirService;
    
    private MRCRequestDispatcher mrc;
    
    private final List<Services> enabledServs;
    
    private TimeSync            tsInstance;
    
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
        
        if (enabledServs.contains(Services.MRC)) {
            mrc = new MRCRequestDispatcher(SetupUtils.createMRC1Config());
            mrc.startup();
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "MRC running");
        }
        
        if (enabledServs.contains(Services.TIME_SYNC)) {
            tsInstance = TimeSync.initialize(null, 60*1000, 50);
            tsInstance.waitForStartup();
        }
        

        if (enabledServs.contains(Services.UUID_RESOLVER)) {
            UUIDResolver.start(getDirClient(), 1000, 10 * 10 * 1000);
            SetupUtils.localResolver();
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
        FSUtils.delTree(testDir);
    }
    
}
