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
import org.xtreemfs.common.clients.RandomAccessFile;
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

    public void openFile(XCap capability, RandomAccessFile f) {
        synchronized (capabilities) {
            CapEntry e = capabilities.get(capability.getFile_id());
            if (e == null) {
                e = new CapEntry(capability, TimeSync.getLocalSystemTime()+capability.getExpire_timeout_s()*1000);
                capabilities.put(capability.getFile_id(),e);
            } else {
                e.upgradeCap(capability, TimeSync.getLocalSystemTime()+capability.getExpire_timeout_s()*1000);
            }
            e.addFile(f);
        }
    }

    public void closeFile(String fileId, RandomAccessFile f) {
        boolean lastFile = false;
        synchronized (capabilities) {
            CapEntry e = capabilities.get(fileId);
            if (e != null) {
                lastFile = e.removeFile(f);
                if (lastFile)
                    capabilities.remove(fileId);
            } else {
                throw new IllegalStateException("entry must nut be null");
            }
        }
        if (lastFile) {
            synchronized (fsUpdateCache) {
                fsUpdateCache.remove(fileId);
            }
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
            return e.getCap();
        }
    }

    public void shutdown() {
        this.quit = true;
        this.interrupt();
    }

    public void run() {

        //check for CAP-renew

        do {
            List<CapEntry> renewList = new LinkedList();
            synchronized (capabilities) {
                //check caps
                final long expTime = TimeSync.getLocalSystemTime()+Constants.XCAP_EXPIRE_TIMEOUT_S_MIN;
                for (CapEntry e : capabilities.values()) {
                    if (e.getLocalTimestamp() <= expTime ) {
                        //needs renew!
                        renewList.add(e);
                    }
                }
            }
            for (CapEntry cap : renewList) {
                renewCap(cap);
            }
            try {
                sleep(Constants.XCAP_EXPIRE_TIMEOUT_S_MIN*60*1000);
            } catch (InterruptedException ex) {
                break;
            }
        } while (!quit);

    }

    protected void renewCap(CapEntry cap) {
        assert(cap != null);
        RPCResponse<XCap> r = null;
        try {
            r = client.xtreemfs_renew_capability(client.getDefaultServerAddress(), cap.getCap());
            XCap newCap = r.get();
            synchronized (capabilities) {
                cap.updateCap(newCap, TimeSync.getLocalSystemTime()+newCap.getExpire_timeout_s()*1000-1000);
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
        private XCap cap;
        private long localTimestamp;
        final List<RandomAccessFile> files;

        public CapEntry(XCap c, long ts) {
            cap = c;
            localTimestamp = ts;
            files = new LinkedList();
        }

        public void addFile(RandomAccessFile file) {
            files.add(file);
        }

        public boolean removeFile(RandomAccessFile file) {
            files.remove(file);
            return files.isEmpty();
        }

        public void updateCap(XCap c, long ts) {
            cap = c;
            localTimestamp = ts;
            for (RandomAccessFile file : files)
                file.updateCap(c);
        }

        public void upgradeCap(XCap c, long ts) {
            //FIXME
            /*
             * upgrade: always from R to RW
             * never from RW to R
             */
            if ( ((cap.getAccess_mode() & Constants.SYSTEM_V_FCNTL_H_O_RDONLY) > 0)
                 && ((c.getAccess_mode() & Constants.SYSTEM_V_FCNTL_H_O_RDWR) > 0) ) {
                updateCap(c, ts);
            }
        }

        /**
         * @return the cap
         */
        public XCap getCap() {
            return cap;
        }

        /**
         * @return the localTimestamp
         */
        public long getLocalTimestamp() {
            return localTimestamp;
        }
    }
}
