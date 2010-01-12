/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

package org.xtreemfs.common.clients.internal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.NewFileSize;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.XCap;
import org.xtreemfs.mrc.client.MRCClient;

/**
 *
 * @author bjko
 */
public class OpenFileList extends Thread {

    private final Map<String,CapEntry> capabilities;

    private final Map<String,OSDWriteResponse> fsUpdateCache;

    private final MRCClient client;

    private volatile boolean quit;

    public OpenFileList(MRCClient client) {
        super("XCapRNThr");
        capabilities = new HashMap();
        fsUpdateCache = new HashMap<String, OSDWriteResponse>();
        this.client = client;
    }

    public void openFile(XCap capability) {
        synchronized (capabilities) {
            capabilities.put(capability.getFile_id(),new CapEntry(capability, TimeSync.getLocalSystemTime()+capability.getExpire_timeout_s()*1000));
        }
    }

    public void closeFile(String fileId) {
        synchronized (capabilities) {
            capabilities.remove(fileId);
        }
        synchronized (fsUpdateCache) {
            fsUpdateCache.remove(fileId);
        }
    }

    public OSDWriteResponse sendFsUpdate(String fileId) {
        synchronized (fsUpdateCache) {
            return fsUpdateCache.remove(fileId);
        }
    }

    public OSDWriteResponse getLocalFS(String fileId) {
        synchronized (fsUpdateCache) {
            return fsUpdateCache.get(fileId);
        }
    }

    public void fsUpdate(String fileId, OSDWriteResponse resp) {
        if ((resp == null) || (resp.getNew_file_size().size() == 0))
            return;
        synchronized (fsUpdateCache) {
            OSDWriteResponse last = fsUpdateCache.get(fileId);
            if (last == null) {
                fsUpdateCache.put(fileId, resp);
                return;
            }
            final NewFileSize newFS = resp.getNew_file_size().get(0);
            final NewFileSize oldFS = last.getNew_file_size().get(0);

            if ( (newFS.getTruncate_epoch() > oldFS.getTruncate_epoch()) ||
                 (newFS.getTruncate_epoch() == oldFS.getTruncate_epoch()) &&
                 (newFS.getSize_in_bytes() > oldFS.getSize_in_bytes()) ) {
                fsUpdateCache.put(fileId, resp);
            }
        }
    }

    public XCap getCapability(String fileId) {
        synchronized (capabilities) {
            CapEntry e = capabilities.get(fileId);
            if (e == null)
                return null;
            return e.cap;
        }
    }

    public void shutdown() {
        this.quit = true;
        this.interrupt();
    }

    public void run() {

        //check for CAP-renew

        do {
            List<XCap> renewList = new LinkedList();
            synchronized (capabilities) {
                //check caps
                final long expTime = TimeSync.getLocalSystemTime()+Constants.XCAP_EXPIRE_TIMEOUT_S_MIN;
                for (CapEntry e : capabilities.values()) {
                    if (e.localTimestamp <= expTime ) {
                        //needs renew!
                        renewList.add(e.cap);
                    }
                }
            }
            for (XCap cap : renewList) {
                renewCap(cap);
            }
            try {
                sleep(Constants.XCAP_EXPIRE_TIMEOUT_S_MIN*60*1000);
            } catch (InterruptedException ex) {
                break;
            }
        } while (!quit);

    }

    protected void renewCap(XCap cap) {
        assert(cap != null);
        RPCResponse<XCap> r = null;
        try {
            r = client.xtreemfs_renew_capability(client.getDefaultServerAddress(), cap);
            XCap newCap = r.get();
            synchronized (capabilities) {
                capabilities.put(newCap.getFile_id(), new CapEntry(newCap, TimeSync.getLocalSystemTime()+newCap.getExpire_timeout_s()*1000));
            }
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this,"cannot renew cap due to exception");
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        } finally {
            if (r != null)
                r.freeBuffers();
        }
        
    }

    public static class CapEntry {
        XCap cap;
        long localTimestamp;

        public CapEntry(XCap c, long ts) {
            cap = c;
            localTimestamp = ts;
        }
    }
}
