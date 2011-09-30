/*
 * Copyright (c) 2009 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients.internal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.CONSTANTS;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;

/**
 *
 * @author bjko
 */
public class OpenFileList extends Thread {

    private final Map<String,CapEntry> capabilities;

    private final Map<String,OSDWriteResponse> fsUpdateCache;

    private final MRCServiceClient client;

    private volatile boolean quit;

    public OpenFileList(MRCServiceClient client) {
        super("XCapRNThr");
        capabilities = new HashMap();
        fsUpdateCache = new HashMap<String, OSDWriteResponse>();
        this.client = client;
    }

    public void openFile(XCap capability, RandomAccessFile f) {
        synchronized (capabilities) {
            CapEntry e = capabilities.get(capability.getFileId());
            if (e == null) {
                e = new CapEntry(capability, TimeSync.getLocalSystemTime()+capability.getExpireTimeoutS()*1000);
                capabilities.put(capability.getFileId(),e);
            } else {
                e.upgradeCap(capability, TimeSync.getLocalSystemTime()+capability.getExpireTimeoutS()*1000);
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
        if ((resp == null) || !resp.hasSizeInBytes())
            return;
        synchronized (fsUpdateCache) {
            OSDWriteResponse last = fsUpdateCache.get(fileId);
            if (last == null) {
                fsUpdateCache.put(fileId, resp);
                return;
            }
            final OSDWriteResponse newFS = resp;
            final OSDWriteResponse oldFS = last;

            if ( (newFS.getTruncateEpoch() > oldFS.getTruncateEpoch()) ||
                 (newFS.getTruncateEpoch() == oldFS.getTruncateEpoch()) &&
                 (newFS.getSizeInBytes() > oldFS.getSizeInBytes()) ) {
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
                final long expTime = TimeSync.getLocalSystemTime()+CONSTANTS.XCAP_RENEW_INTERVAL_IN_MIN.getNumber()*60*1000;
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
                sleep(CONSTANTS.XCAP_RENEW_INTERVAL_IN_MIN.getNumber()*60*1000/2);
            } catch (InterruptedException ex) {
                break;
            }
        } while (!quit);

    }

    protected void renewCap(CapEntry cap) {
        assert(cap != null);
        RPCResponse<XCap> r = null;
        try {
            r = client.xtreemfs_renew_capability(null, RPCAuthentication.authNone, RPCAuthentication.userService, cap.getCap());
            XCap newCap = r.get();
            synchronized (capabilities) {
                cap.updateCap(newCap, TimeSync.getLocalSystemTime()+CONSTANTS.XCAP_RENEW_INTERVAL_IN_MIN.getNumber()*60*1000-1000);
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
            if ( ((cap.getAccessMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber()) > 0)
                 && ((c.getAccessMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber()) > 0) ) {
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
