/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.test;

import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.clients.osd.OSDClient;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.mrc.client.MRCClient;

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
    
    /**
     * @return the osdClient
     */
    public OSDClient getOsdClient() {
        return osdClient;
    }
    
    public enum Services {
        TIME_SYNC, UUID_RESOLVER, RPC_CLIENT, DIR_CLIENT, MRC_CLIENT, OSD_CLIENT, DIR_SERVICE
    };
    
    private RPCNIOSocketClient   rpcClient;
    
    private DIRClient            dirClient;
    
    private MRCClient            mrcClient;
    
    private OSDClient            osdClient;
    
    private DIRRequestDispatcher dirService;
    
    private final List<Services> enabledServs;
    
    public TestEnvironment(Services[] servs) {
        enabledServs = new ArrayList(servs.length);
        for (Services serv : servs)
            enabledServs.add(serv);
    }
    
    public void start() throws Exception {
        
        rpcClient = SetupUtils.createRPCClient(10000);
        getRpcClient().start();
        getRpcClient().waitForStartup();
        
        dirClient = SetupUtils.createDIRClient(getRpcClient());
        
        if (enabledServs.contains(Services.DIR_SERVICE)) {
            dirService = new DIRRequestDispatcher(SetupUtils.createDIRConfig());
            dirService.startup();
            dirService.waitForStartup();
            System.out.println("dir running");
        }
        
        if (enabledServs.contains(Services.TIME_SYNC)) {
            TimeSync.initialize(dirClient, 60 * 1000, 50);
        }
        
        if (enabledServs.contains(Services.UUID_RESOLVER)) {
            UUIDResolver.start(getDirClient(), 1000, 10 * 10 * 1000);
            SetupUtils.localResolver();
        }
        
        if (enabledServs.contains(Services.MRC_CLIENT)) {
            mrcClient = new MRCClient(rpcClient, null);
        }
        
        if (enabledServs.contains(Services.OSD_CLIENT)) {
            // osdClient = new OSDClient(rpcClient, null);
            // TODO
        }
        
    }
    
    public void shutdown() {
        
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
                TimeSync.getInstance().shutdown();
            } catch (Throwable th) {
            }
        }
        
    }
    
}
