/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.DeletionStage;
import org.xtreemfs.osd.stages.PreprocStage.DeleteOnCloseCallback;
import org.xtreemfs.osd.storage.StorageLayout.FileData;
import org.xtreemfs.osd.storage.StorageLayout.FileList;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_check_file_existsResponse;

/**
 *
 * @author bjko
 */
public class CleanupThread extends LifeCycleThread {
    // where zombies will be restored
    public final static String DEFAULT_RESTORE_PATH = "lost+found";
    
    // PATTERN for the output. Brackets are not allowed to be used at the format strings.
    public final static String STATUS_FORMAT = "files checked: %8d   zombies: %8d   running since: %s";        
    public final static String STOPPED_FORMAT = "not running, last check started %s";
    public final static String DEAD_VOLUME_FORMAT = "volume %s is dead - not registered at directory service";    
    public final static String DELETED_VOLUME_FORMAT = "volume %s was removed from MRC %s";    
    public final static String VOLUME_RESULT_FORMAT = "volume %s had %8d zombies - out of %8d files checked";    
    public final static String ERROR_FORMAT = "ERROR: cannot check volume %s , reason: %s";    
    public final static String ZOMBIES_RESTORED_FORMAT = "%8d zombies restored to '"+DEFAULT_RESTORE_PATH+"' on volume %s";
    public final static String ZOMBIES_DELETED_FORMAT = "%8d zombies deleted from %s volume %s";
    public final static String ZOMBIE_DELETE_ERROR_FORMAT = "%s could not be deleted, because: %s";
    
    private final OSDRequestDispatcher master;
    
    private volatile boolean isRunning;
    
    private volatile boolean quit;
    
    /**
     * remove files for which no metadata exists. if false, zombies
     * are just reported.
     */
    private volatile boolean removeZombies;

    /**
     * remove files if the volume does not exist at the DIR anymore.
     * Can be dangerous if a volume is deregistered by accident.
     */
    private volatile boolean removeDeadVolumes;

    /**
     * if enabled, zombies will be moved to new files in a lost+found
     * directory.
     */
    private volatile boolean lostAndFound;

    private final List<String> results;

    private final StorageLayout layout;

    private volatile long filesChecked;

    private final AtomicLong zombies;

    private volatile long startTime;
    
    private UserCredentials uc;
    
    final ServiceUUID localUUID;

    public CleanupThread(OSDRequestDispatcher master, StorageLayout layout) {
        super("CleanupThr");
        this.zombies = new AtomicLong(0L);
        this.master = master;
        this.isRunning = false;
        this.quit = false;
        this.layout = layout;
        this.results = new LinkedList();
        this.localUUID = master.getConfig().getUUID();
        this.startTime = 0L;
        this.filesChecked = 0L;
    }

    public boolean cleanupStart(boolean removeZombies, boolean removeDeadVolumes, boolean lostAndFound, UserCredentials uc) {
        synchronized (this) {
            if (isRunning) {
                return false;
            } else {
                this.removeZombies = removeZombies;
                this.removeDeadVolumes = removeDeadVolumes;
                this.lostAndFound = lostAndFound;
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

    public List<String> getResult() {
        synchronized (this) {
            return results;
        }
    }

    public String getStatus() {
        synchronized (this) {
            String d = DateFormat.getDateInstance().format(new Date(startTime));
            assert (d != null);
            if (isRunning) {
                return String.format(STATUS_FORMAT,
                        filesChecked, zombies.get(), d);
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
                results.clear();
                filesChecked = 0;
                zombies.set(0L);
                startTime = TimeSync.getGlobalTime();
                final MRCServiceClient mrcClient = new MRCServiceClient(master.getRPCClient(),null);
                do {
                    l = layout.getFileList(l, 1024*4);
                    Map<Volume,List<String>> perVolume = new Hashtable<Volume, List<String>>();
                    for (String fileName : l.files.keySet()) {
                        filesChecked++;
                        String[] tmp = fileName.split(":");
                        List<String> flist = perVolume.get((Volume) new Volume(tmp[0]));
                        if (flist == null) {
                            flist = new LinkedList<String>();
                            perVolume.put(new Volume(tmp[0]),flist);
                        }
                        flist.add(tmp[1]);
                    }

                    synchronized (this) {
                        if (!isRunning)
                            break;
                    }

                    //check each volume
                    Map<Volume,Map<String,FileData>> zombieFilesPerVolume = new Hashtable<Volume, Map<String,FileData>>();
                    
                    for (Volume volume : perVolume.keySet()) {
                        final Map<String,FileData> zombieFiles = new Hashtable<String,FileData>();
                        try {
                             RPCResponse<ServiceSet> vol = master.getDIRClient().xtreemfs_service_get_by_uuid(null, RPCAuthentication.authNone,RPCAuthentication.userService, volume.id);
                             ServiceSet s = vol.get();
                             
                             if (s.getServicesCount() == 0) {
                                 //volume does not exist
                                 results.add(String.format(DEAD_VOLUME_FORMAT, volume.id));
                                 volume.dead();
                                 // retrieve fileData
                                 for (String zombie : perVolume.get(volume)){
                                     zombieFiles.put(zombie,l.files.get(zombie));
                                 }
                             } else {
                                 
                                 final boolean cowEnabled = false; // FIXME: fetch COW policy for current volume
                                 
                                 String mrcUUID = null;
                                 for (KeyValuePair kvp : s.getServices(0).getData().getDataList()) {
                                     if (kvp.getKey().equals("mrc"))
                                         mrcUUID = kvp.getValue();
                                 }
                                 volume.mrc = new ServiceUUID(mrcUUID);
                                 RPCResponse<xtreemfs_check_file_existsResponse> r = mrcClient.xtreemfs_check_file_exists(volume.mrc.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService, volume.id, perVolume.get(volume), localUUID.toString());
                                 String eval = r.get().getBitmap();
                                 r.freeBuffers();
                                 if (eval.equals("2")) {
                                     //volume was deleted (not a dead volume, since MRC answered!)
                                     results.add(String.format(DELETED_VOLUME_FORMAT, volume.id,volume.mrc));
                                 } else {
                                     //check all files... 
                                     List<String> files = perVolume.get(volume);
                                     final AtomicInteger numZombies = new AtomicInteger(0);
                                     final AtomicInteger openOFTChecks = new AtomicInteger(0);
                                     
                                     for (int i = 0; i < files.size(); i++) {                                         
                                         if (eval.charAt(i) == '0' || eval.charAt(i) == '3') {
                                             // remove abandoned replicas immediately
                                             final boolean abandoned = (eval.charAt(i) == '3');
                                             
                                             // retrieve the fileName
                                             final String fName = volume.id+":"+files.get(i);
                                             
                                             // retrieve the fileData
                                             final FileData fData = l.files.get(fName);
                                             
                                             // check against the OFT
                                             openOFTChecks.incrementAndGet();
                                             master.getPreprocStage().checkDeleteOnClose(files.get(i), new DeleteOnCloseCallback() {
                                                 @Override
                                                 public void deleteOnCloseResult(boolean isDeleteOnClose, ErrorResponse error) {
                                                     if (!isDeleteOnClose && !abandoned){
                                                         // file is zombie
                                                         numZombies.incrementAndGet();
                                                         zombies.incrementAndGet();
                                                         zombieFiles.put(fName, fData);
                                                         if (openOFTChecks.decrementAndGet() <= 0) {
                                                             synchronized (openOFTChecks) {
                                                                 openOFTChecks.notify();
                                                             }
                                                         }
                                                     // deal with the unrestoreable replica
                                                     } else if (!isDeleteOnClose) {
                                                         master.getDeletionStage().deleteObjects(fName, null, cowEnabled, null, new DeletionStage.DeleteObjectsCallback() {
                                                             
                                                             @Override
                                                             public void deleteComplete(ErrorResponse error) {
                                                                 if (error != null)
                                                                     results.add(String.format(ZOMBIE_DELETE_ERROR_FORMAT, fName, error.getErrorMessage()));
                                                             }
                                                         });
                                                     }
                                                 }
                                             });
                                         } 
                                     }
                                     
                                     synchronized (openOFTChecks) {
                                         while (openOFTChecks.get()>0)
                                             openOFTChecks.wait();
                                     }
                                     results.add(String.format(VOLUME_RESULT_FORMAT, volume.id, numZombies.get(), files.size()));
                                 }
                             }
                        } catch (Exception ex) {
                            results.add(String.format(ERROR_FORMAT, volume.id,ex.getMessage()));
                        }
                        
                        if (zombieFiles.size()!=0) zombieFilesPerVolume.put(volume, zombieFiles);
                    }
                    
                    synchronized (this) {
                        if (!isRunning)
                            break;
                    }
                    
                    
                    // deal with the zombies
                    for (Volume volume : zombieFilesPerVolume.keySet()){
                        // restore files if the flag is set (files from dead volumes cannot be restored
                        if (!volume.isDead() && lostAndFound){
                            Map<String,FileData> zombieFiles = zombieFilesPerVolume.get(volume);
                            
                            for (String fileName : zombieFiles.keySet()) {
                                FileData data = zombieFiles.get(fileName);
                                RPCResponse r = mrcClient.xtreemfs_restore_file(volume.mrc.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                                        DEFAULT_RESTORE_PATH, fileName, data.size, 
                                        localUUID.toString(), Integer.valueOf(String.valueOf(data.objectSize)));
                                
                                // the response does not matter
                                r.get(); r.freeBuffers();
                            }  
                            
                            results.add(String.format(ZOMBIES_RESTORED_FORMAT,zombieFiles.keySet().size(),volume.id));
                        // delete files of dead volumes if the flag is set
                        } else if ((volume.isDead() && removeDeadVolumes) || 
                             // delete zombies if the flag is set
                                (!volume.isDead() && removeZombies)) {
                            Map<String,FileData> zombieFiles = zombieFilesPerVolume.get(volume);
                            final AtomicInteger openDeletes = new AtomicInteger(0);
                            
                            final boolean cowEnabled = false; // FIXME: fetch COW policy for current volume
                            
                            for (final String fileName : zombieFiles.keySet()) {
                                openDeletes.incrementAndGet(); 
                                master.getDeletionStage().deleteObjects(fileName, null, cowEnabled, null, new DeletionStage.DeleteObjectsCallback() {
                                
                                    @Override
                                    public void deleteComplete(ErrorResponse error) {
                                        if (error!=null)
                                            results.add(String.format(ZOMBIE_DELETE_ERROR_FORMAT, fileName, error.getErrorMessage()));
                                        
                                        if (openDeletes.decrementAndGet() <= 0) {
                                            synchronized (openDeletes) {
                                                openDeletes.notify();
                                            }
                                        }
                                    }
                                });
                            }   
                            
                            synchronized (openDeletes) {
                                while (openDeletes.get()>0)
                                    openDeletes.wait();
                            }
                            results.add(String.format(ZOMBIES_DELETED_FORMAT, zombieFiles.keySet().size(),(volume.isDead() ? "dead" : "existing"),volume.id));
                        }
                    }
                    
                    synchronized (this) {
                        if (!isRunning)
                            break;
                    }
                    
                } while (l.hasMore);
                isRunning = false;


            } while (!quit);

        } catch (Exception thr) {
            this.notifyCrashed(thr);
        }

        notifyStopped();
    }
    
    /**
     * 
     * @param format
     * @return a regular expression for retrieving the information build into a string made with the given format.
     */
    public static String getRegex(String format) {
        return format.replaceAll("\\+",".").replaceAll("%8d", "(\\\\s*\\\\d+)").replaceAll("%s", "([\\\\S\\\\p{Punct}]+)");
    }

    /**
     * Contains VolumeId and MRC Address.
     * 
     * 23.04.2009
     * @author flangner
     */
    public class Volume {
        final String id;
        ServiceUUID mrc = null;
        boolean dead = false;
        /**
         * Constructor for Volumes with unknown MRC.
         */
        Volume(String volId) {
            this.id = volId;
        }
        
        /**
         * to mark dead volumes.
         */
        void dead() { this.dead = true; }        
        boolean isDead() { return this.dead; }
        
        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Volume)) return false;
            return id.equals(((Volume) obj).id);
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return this.id.hashCode();
        }
    }
}
