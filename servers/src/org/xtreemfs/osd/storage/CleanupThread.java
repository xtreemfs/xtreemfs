/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.storage;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.PreprocStage.DeleteOnCloseCallback;
import org.xtreemfs.osd.storage.HashStorageLayout.FileData;
import org.xtreemfs.osd.storage.HashStorageLayout.FileList;

/**
 *
 * @author bjko
 */
public class CleanupThread extends LifeCycleThread {

    public final static String DEFAULT_RESTORE_PATH = "lost+found";
    
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

    private final StringSet results;

    private final HashStorageLayout layout;

    private long filesChecked;

    private final AtomicLong zombies;

    private long startTime;
    
    final ServiceUUID localUUID;

    public CleanupThread(OSDRequestDispatcher master, HashStorageLayout layout) {
        super("CleanupThr");
        this.zombies = new AtomicLong(0L);
        this.master = master;
        this.isRunning = false;
        this.quit = false;
        this.layout = layout;
        this.results = new StringSet();
        localUUID = master.getConfig().getUUID();
    }

    public boolean cleanupStart(boolean removeZombies, boolean removeDeadVolumes, boolean lostAndFound) {
        synchronized (this) {
            if (isRunning) {
                return false;
            } else {
                this.removeZombies = removeZombies;
                this.removeDeadVolumes = removeDeadVolumes;
                this.lostAndFound = lostAndFound;
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

    public StringSet getResult() {
        synchronized (this) {
            return results;
        }
    }

    public String getStatus() {
        synchronized (this) {
            if (isRunning) {
                Date d = new Date(startTime);
                return String.format("files checked: %8d   zombies: %8d   running since: %s",
                        filesChecked, zombies.get(), DateFormat.getDateInstance().format(d));
            } else {
                Date d = new Date(startTime);
                return "not running, last check started "+DateFormat.getDateInstance().format(d);
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
                final MRCClient mrcClient = new MRCClient(master.getRPCClient(),null);
                do {
                    l = layout.getFileList(l, 1024*4);
                    Map<Volume,StringSet> perVolume = new HashMap<Volume, StringSet>();
                    for (String fileName : l.files.keySet()) {
                        filesChecked++;
                        String[] tmp = fileName.split(":");
                        StringSet flist = perVolume.get(tmp[0]);
                        if (flist == null) {
                            flist = new StringSet();
                            perVolume.put(new Volume(tmp[0]),flist);
                        }
                        flist.add(tmp[1]);
                    }

                    synchronized (this) {
                        if (!isRunning)
                            break;
                    }

                    //check each volume
                    Map<Volume,Map<String,FileData>> zombieFilesPerVolume = new HashMap<Volume, Map<String,FileData>>();
                    
                    for (Volume volume : perVolume.keySet()) {
                        final Map<String,FileData> zombieFiles = new Hashtable<String,FileData>();
                        try {
                             RPCResponse<ServiceSet> vol = master.getDIRClient().xtreemfs_service_get_by_uuid(null, volume.id);
                             ServiceSet s = vol.get();
                             
                             if (s.size() == 0) {
                                 //volume does not exist
                                 results.add("volume "+volume.id+" is dead (not registered at directory service)");
                                 volume.dead();
                                 // retrieve fileData
                                 for (String zombie : perVolume.get(volume)){
                                     zombieFiles.put(zombie,l.files.get(zombie));
                                 }
                             } else {
                                 String mrc = s.get(0).getData().get("mrc");
                                 volume.mrc = new ServiceUUID(mrc);
                                 RPCResponse<String> r = mrcClient.xtreemfs_checkFileExists(volume.mrc.getAddress(), volume.id, perVolume.get(volume));
                                 String eval = r.get();
                                 r.freeBuffers();
                                 if (eval.equals("2")) {
                                     //volume was deleted (not a dead volume, since MRC answered!)
                                     results.add("volume "+volume.id+" was removed from MRC "+mrc);
                                 } else {
                                     //check all files...
                                     StringSet files = perVolume.get(volume);
                                     
                                     final AtomicInteger numZombies = new AtomicInteger(0);
                                     for (int i = 0; i < files.size(); i++) {
                                         if (eval.charAt(i) == '0') {
                                             // retrieve the fileName
                                             final String fName = volume.id+":"+files.get(i);
                                             // retrieve the fileData
                                             final FileData fData = l.files.get(fName);
                                             
                                             // check against the OFT
                                             master.getPreprocStage().checkDeleteOnClose(files.get(i), new DeleteOnCloseCallback() {
                                                 @Override
                                                 public void deleteOnCloseResult(boolean isDeleteOnClose, Exception error) {
                                                     if (!isDeleteOnClose){
                                                         // file is zombie
                                                         numZombies.incrementAndGet();
                                                         zombies.incrementAndGet();
                                                         zombieFiles.put(fName, fData);
                                                     }
                                                 }
                                             });
                                         }
                                     }
                                     results.add("volume "+volume.id+" had "+numZombies+" zombie (out of "+files.size()+" files checked) ;"+volume.id+";"+numZombies+";"+files.size());
                                 }
                             }
                        } catch (Exception ex) {
                            results.add("ERROR: cannot check volume "+volume.id+", reason: "+ex);
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
                                RPCResponse<?> r = mrcClient.xtreemfs_restore_file(volume.mrc.getAddress(), 
                                        DEFAULT_RESTORE_PATH, fileName, data.size, 
                                        localUUID.toString(), Integer.valueOf(String.valueOf(data.objectSize)));
                                
                                // the response does not matter
                                r.get(); r.freeBuffers();
                            }   
                        // delete files of dead volumes if the flag is set
                        } else if ((volume.isDead() && removeDeadVolumes) || 
                             // delete zombies if the flag is set
                                (!volume.isDead() && removeZombies)) {
                            Map<String,FileData> zombieFiles = zombieFilesPerVolume.get(volume);
                            
                            for (String fileName : zombieFiles.keySet()) {
                                master.getDeletionStage().deleteObjects(fileName, null, null);
                            }    
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
        Logging.logMessage(Logging.LEVEL_INFO, this,"cleanup thread stopped");
        notifyStopped();
    }

    /**
     * Contains VolumeId and MRC Address.
     * 
     * 23.04.2009
     * @author flangner
     */
    final class Volume {
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
    }
}
