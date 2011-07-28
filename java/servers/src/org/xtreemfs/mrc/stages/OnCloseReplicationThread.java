/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.stages;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_update_file_sizeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.ObjectData;

/**
 * 
 * @author stender
 */
public class OnCloseReplicationThread extends LifeCycleThread {
    
    private final MRCRequestDispatcher            master;
    
    private boolean                               quit;
    
    private final LinkedBlockingQueue<MRCRequest> requests;
    
    public OnCloseReplicationThread(MRCRequestDispatcher master) {
        super("OnCloseReplThr");
        this.master = master;
        this.requests = new LinkedBlockingQueue<MRCRequest>();
    }
    
    public void shutdown() {
        this.quit = true;
        this.interrupt();
    }
    
    public void enqueueRequest(MRCRequest req) {
        assert (this.isAlive());
        assert (req != null);
        requests.add(req);
    }
    
    public void run() {
        
        notifyStarted();
        try {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.lifecycle, this,
                    "OnCloseReplicationThread started");
            
            do {
                final MRCRequest req = requests.take();
                final XCap xcap = ((xtreemfs_update_file_sizeRequest) req.getRequestArgs()).getXcap();
                final XLocSet xlocSet = (XLocSet) req.getDetails().context.get("xLocList");
                final FileCredentials creds = FileCredentials.newBuilder().setXcap(xcap).setXlocs(xlocSet)
                        .build();
                
                try {
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                            "triggering replication for %s", xlocSet.toString());
                    
                    for (int i = 1; i < xlocSet.getReplicasCount(); i++) {
                        
                        Replica repl = xlocSet.getReplicas(i);
                        StripingPolicyImpl spol = StripingPolicyImpl.getPolicy(repl, 0);
                        
                        for (int j = 0; j < repl.getOsdUuidsCount(); j++) {
                            
                            Iterator<Long> objs = spol.getObjectsOfOSD(j, 0, Long.MAX_VALUE);
                            long obj = objs.next();
                            
                            RPCResponse<ObjectData> resp = null;
                            try {
                                
                                InetSocketAddress osd = new ServiceUUID(repl.getOsdUuids(j)).getAddress();
                                
                                // read one byte from each OSD in each replica
                                // to trigger the replication
                                resp = master.getOSDClient().read(osd, RPCAuthentication.authNone,
                                    RPCAuthentication.userService, creds, xcap.getFileId(), obj, 0, 0, 1);
                                resp.get();
                                
                            } catch (Exception e) {
                                Logging.logMessage(Logging.LEVEL_WARN, Category.proc, this, OutputUtils
                                        .stackTraceToString(e));
                            } finally {
                                if (resp != null)
                                    resp.freeBuffers();
                            }
                            
                        }
                    }
                    
                } catch (Exception ex) {
                    Logging.logError(Logging.LEVEL_ERROR, this, ex);
                }
            } while (!quit);
        } catch (InterruptedException ex) {
            // idontcare
        }
        
        notifyStopped();
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.lifecycle, this,
                "OnCloseReplicationThread finished");
    }
}
