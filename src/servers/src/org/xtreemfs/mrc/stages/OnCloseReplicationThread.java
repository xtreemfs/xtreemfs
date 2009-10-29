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
 * AUTHORS: Jan Stender, Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.stages;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.XCap;
import org.xtreemfs.interfaces.XLocSet;
import org.xtreemfs.interfaces.MRCInterface.closeRequest;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;

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
                final XCap xcap = ((closeRequest) req.getRequestArgs()).getWrite_xcap();
                final XLocSet xlocSet = (XLocSet) req.getDetails().context.get("xLocList");
                final FileCredentials creds = new FileCredentials(xlocSet, xcap);
                
                try {
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                            "triggering replication for %s", xlocSet.toString());
                    
                    for (int i = 1; i < xlocSet.getReplicas().size(); i++) {
                        
                        Replica repl = xlocSet.getReplicas().get(i);
                        StripingPolicyImpl spol = StripingPolicyImpl.getPolicy(repl);
                        
                        for (int j = 0; j < repl.getOsd_uuids().size(); j++) {
                            
                            Iterator<Long> objs = spol.getObjectsOfOSD(j, 0, Long.MAX_VALUE);
                            long obj = objs.next();
                            
                            RPCResponse<ObjectData> resp = null;
                            try {
                                
                                InetSocketAddress osd = new ServiceUUID(repl.getOsd_uuids().get(j))
                                        .getAddress();
                                
                                // read one byte from each OSD in each replica
                                // to trigger the replication
                                resp = master.getOSDClient()
                                        .read(osd, xcap.getFile_id(), creds, obj, 0, 0, 1);
                                ObjectData data = resp.get();
                                BufferPool.free(data.getData());
                                
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
