/*
 * Copyright (c) 2009-2011 by Jan Stender, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.storage;

import java.text.DateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.RAID0Impl;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.storage.StorageLayout.FileList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getxattrRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getxattrResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;

/**
 * 
 * @author bjko
 */
public class CleanupVersionsThread extends LifeCycleThread {
    
    // PATTERN for the output. Brackets are not allowed to be used at the format
    // strings.
    public final static String         STATUS_FORMAT  = "files checked: %8d   versions deleted: %8d   running since: %s";
    
    public final static String         STOPPED_FORMAT = "not running, last check started %s";
    
    private final OSDRequestDispatcher master;
    
    private volatile boolean           isRunning;
    
    private volatile boolean           quit;
    
    private final StorageLayout        layout;
    
    private volatile long              filesChecked;
    
    private volatile long              versionsRemoved;
    
    private volatile long              startTime;
    
    private UserCredentials            uc;
    
    final ServiceUUID                  localUUID;
    
    public CleanupVersionsThread(OSDRequestDispatcher master, StorageLayout layout) {
        super("CleanupVThr");
        this.master = master;
        this.isRunning = false;
        this.quit = false;
        this.layout = layout;
        this.localUUID = master.getConfig().getUUID();
        this.startTime = 0L;
        this.filesChecked = 0L;
        this.versionsRemoved = 0L;
    }
    
    public boolean cleanupStart(UserCredentials uc) {
        synchronized (this) {
            if (isRunning) {
                return false;
            } else {
                this.uc = uc;
                isRunning = true;
                this.notify();
                return true;
            }
        }
    }
    
    public void cleanupStop() {
        synchronized (this) {
            if (isRunning) {
                isRunning = false;
            }
        }
    }
    
    public boolean isRunning() {
        synchronized (this) {
            return isRunning;
        }
    }
    
    public String getStatus() {
        synchronized (this) {
            String d = DateFormat.getDateInstance().format(new Date(startTime));
            assert (d != null);
            if (isRunning) {
                return String.format(STATUS_FORMAT, filesChecked, versionsRemoved, d);
            } else {
                return String.format(STOPPED_FORMAT, d);
            }
        }
        
    }
    
    public void shutdown() {
        synchronized (this) {
            quit = true;
            this.notifyAll();
        }
    }
    
    public void run() {
        notifyStarted();
        try {
            
            do {
                synchronized (this) {
                    if (!isRunning)
                        this.wait();
                    if (quit)
                        break;
                }
                FileList l = null;
                filesChecked = 0;
                startTime = TimeSync.getGlobalTime();
                final MRCServiceClient mrcClient = new MRCServiceClient(master.getRPCClient(), null);
                
                // first, determine all snapshot timestamps + volumes for all
                // files
                
                // volume -> set of file IDs
                Map<Volume, List<String>> perVolume = new Hashtable<Volume, List<String>>();
                
                do {
                    
                    l = layout.getFileList(l, 1024 * 4);
                    
                    for (String fileName : l.files.keySet()) {
                        
                        filesChecked++;
                        
                        // parse volume and file ID
                        String[] tmp = fileName.split(":");
                        String volId = tmp[0];
                        String fileId = tmp[1];
                        
                        // create a new volume and add it to the map if
                        // necessary
                        Volume vol = new Volume(volId);
                        List<String> flist = perVolume.get(vol);
                        
                        // if the volume doesn't exist in the map yet ...
                        if (flist == null) {
                            
                            // determine the list of snapshot timestamps for the
                            // volume
                            
                            // first, determine the volume name
                            ServiceSet s = master.getDIRClient().xtreemfs_service_get_by_uuid(
                                null, RPCAuthentication.authNone, RPCAuthentication.userService, vol.id);
                            if (s.getServicesCount() == 0) {
                                Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                                    "could not retrieve volume information for '%s' from DIR", vol.id);
                                continue;
                            }
                            
                            // get the MRC responsible for the volume
                            String volName = s.getServices(0).getName();
                            vol.mrc = new ServiceUUID(KeyValuePairs.getValue(s.getServices(0).getData()
                                    .getDataList(), "mrc"));
                            
                            // get the list of snapshot timestamps for the
                            // volume
                            RPCResponse<getxattrResponse> tsResponse = null;
                            String ts = null;
                            try {
                                
                                tsResponse = mrcClient.getxattr(vol.mrc.getAddress(),
                                    RPCAuthentication.authNone, uc, getxattrRequest.newBuilder()
                                            .setVolumeName(volName).setPath("").setName(
                                                "xtreemfs.snapshot_time").build());
                                ts = tsResponse.get().getValue();
                                
                                StringTokenizer st = new StringTokenizer(ts);
                                long[] tsArray = new long[st.countTokens()];
                                for (int i = 0; i < tsArray.length; i++)
                                    tsArray[i] = Long.parseLong(st.nextToken());
                                
                                vol.timestamps = tsArray;
                                
                                // add the volume entry to the map
                                flist = new LinkedList<String>();
                                perVolume.put(vol, flist);
                                
                            } finally {
                                if (tsResponse != null)
                                    tsResponse.freeBuffers();
                            }
                            
                        }
                        
                        flist.add(fileId);
                        
                        synchronized (this) {
                            if (!isRunning)
                                break;
                        }
                        
                    }
                    
                    synchronized (this) {
                        if (!isRunning)
                            break;
                    }
                    
                } while (l.hasMore);
                
                // check each volume
                for (Entry<Volume, List<String>> entry : perVolume.entrySet()) {
                    
                    long[] timestamps = entry.getKey().timestamps;
                    List<String> files = entry.getValue();
                    
                    for (String fileId : files) {
                        
                        fileId = entry.getKey().id + ":" + fileId;
                        
                        // get the metadata; provide any striping policy
                        // (doesn't matter here ...)
                        // FIXME (jdillmann): Policy will mater now
                        FileMetadata md = layout.getFileMetadataNoCaching(RAID0Impl.getPolicy(Replica
                                .newBuilder().setReplicationFlags(0).setStripingPolicy(
                                    StripingPolicy.newBuilder().setType(
                                        StripingPolicyType.STRIPING_POLICY_RAID0).setStripeSize(128)
                                            .setWidth(1).build()).build(), 0), fileId);
                        
                        // determine the set of versions to delete
                        Map<Integer, Set<Integer>> versionsToDelete = md.getVersionTable()
                                .cleanup(timestamps);
                        
                        // delete the objects
                        for (Entry<Integer, Set<Integer>> v : versionsToDelete.entrySet())
                            for (int version : v.getValue()) {
                                
                                // delete the object version if it is not part
                                // of the file's current version
                                if (md.getLatestObjectVersion(v.getKey()) != version)
                                    layout.deleteObject(fileId, md, v.getKey(), version);
                            }
                        
                        // save the updated version table
                        if (versionsToDelete.size() != 0)
                            md.getVersionTable().save();
                        
                        synchronized (this) {
                            if (!isRunning)
                                break;
                        }
                    }
                    
                }
                
                synchronized (this) {
                    if (!isRunning)
                        break;
                }

                synchronized (this) {
                    isRunning = false;
                }
                
            } while (!quit);
            
        } catch (Exception thr) {
            Logging.logError(Logging.LEVEL_ERROR, this, thr);
        }
        
        notifyStopped();
    }
    
    public class Volume {
        
        final String id;
        
        ServiceUUID  mrc = null;
        
        long[]       timestamps;
        
        /**
         * Constructor for Volumes with unknown MRC.
         */
        Volume(String volId) {
            this.id = volId;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Volume))
                return false;
            return id.equals(((Volume) obj).id);
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return this.id.hashCode();
        }
    }
}
