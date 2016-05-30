/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.storage;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Collections;
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
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.DeletionStage;
import org.xtreemfs.osd.stages.PreprocStage.DeleteOnCloseCallback;
import org.xtreemfs.osd.storage.StorageLayout.FileData;
import org.xtreemfs.osd.storage.StorageLayout.FileList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_check_file_existsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_check_file_existsResponse.FILE_STATE;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.XLocSetVersionState;

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
    // public final static String DELETED_VOLUME_FORMAT = "volume %s was removed from MRC %s";
    public final static String VOLUME_RESULT_FORMAT = "volume %s had %8d zombies - out of %8d files checked";    
    public final static String ERROR_FORMAT = "ERROR: cannot check volume %s , reason: %s";    
    public final static String ZOMBIES_RESTORED_FORMAT = "%8d zombies restored to '"+DEFAULT_RESTORE_PATH+"' on volume %s";
    public final static String ZOMBIES_DELETED_FORMAT = "%8d zombies deleted from %s volume %s";
    public final static String ZOMBIE_DELETE_ERROR_FORMAT = "%s could not be deleted, because: %s";
    
    private final OSDRequestDispatcher master;

    private volatile boolean           isRunning;

    private volatile boolean           quit;

    /**
     * remove files for which no metadata exists. if false, zombies are just reported.
     */
    private volatile boolean           removeZombies;

    /**
     * remove files if the volume does not exist at the DIR anymore. Can be dangerous if a volume is deregistered by
     * accident.
     */
    private volatile boolean           removeDeadVolumes;

    /**
     * if enabled, zombies will be moved to new files in a lost+found directory.
     */
    private volatile boolean           lostAndFound;

    private volatile boolean           removeMetadata;

    private volatile int               metaDataTimeoutS;

    private final List<String>         results;

    private final StorageLayout        layout;

    private volatile long              filesChecked;

    private final AtomicLong           zombies;

    private volatile long              startTime;

    private UserCredentials            uc;

    final ServiceUUID                  localUUID;

    final MRCServiceClient             mrcClient;

    private final AtomicLong           openDeletes;

    public CleanupThread(OSDRequestDispatcher master, StorageLayout layout) {
        super("CleanupThr");
        this.zombies = new AtomicLong(0L);
        this.master = master;
        this.isRunning = false;
        this.quit = false;
        this.layout = layout;
        this.results = Collections.synchronizedList(new LinkedList<String>());
        this.localUUID = master.getConfig().getUUID();
        this.startTime = 0L;
        this.filesChecked = 0L;
        this.mrcClient = new MRCServiceClient(master.getRPCClient(), null);
        this.openDeletes = new AtomicLong(0L);
        this.removeMetadata = false;
        this.metaDataTimeoutS = 0;
    }

    public boolean cleanupStart(boolean removeZombies, boolean removeDeadVolumes, boolean lostAndFound,
            boolean removeMetaData, int metaDataTimeoutS, UserCredentials uc) {

        synchronized (this) {
            if (isRunning) {
                return false;
            } else {
                this.removeZombies = removeZombies;
                this.removeDeadVolumes = removeDeadVolumes;
                this.lostAndFound = lostAndFound;
                this.removeMetadata = removeMetaData;
                this.metaDataTimeoutS = metaDataTimeoutS;
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
                runCleanup();
                isRunning = false;
            } while (!quit);

        } catch (Throwable thr) {
            this.notifyCrashed(thr);
        }

        notifyStopped();
    }
    

    private void runCleanup() throws Throwable {
        FileList l = null;
        results.clear();
        filesChecked = 0;
        zombies.set(0L);
        startTime = TimeSync.getGlobalTime();

        do { // while(l.hasMore)
            
            // Retrieve the fileList from the storage Layout.
            l = layout.getFileList(l, 1024 * 4);
            
            // Map files to their corresponding volume.
            final Map<Volume, List<String>> perVolume = new Hashtable<Volume, List<String>>();
            final Map<Volume, List<String>> metaOnlyPerVolume = new Hashtable<Volume, List<String>>();

            for (String fileName : l.files.keySet()) {
                filesChecked++;
                String[] tmp = fileName.split(":");
                Volume v = new Volume(tmp[0]);
                String fileId = tmp[1];

                final Map<Volume, List<String>> target;
                if (l.files.get(fileName).metaDataOnly) {
                    target = metaOnlyPerVolume;
                } else {
                    target = perVolume;
                }

                List<String> flist = target.get(v);
                if (flist == null) {
                    flist = new LinkedList<String>();
                    target.put(v, flist);
                }

                flist.add(fileId);
            }

            // Interrupt execution if the cleanup has been stopped.
            synchronized (this) {
                if (!isRunning)
                    break;
            }

            // Check for zombie files on each volume.
            Map<Volume, Map<String, FileData>> zombieFilesPerVolume = new Hashtable<Volume, Map<String, FileData>>();
            // final List<String> deleteableFiles = Collections.synchronizedList(new LinkedList<String>());

            for (Volume volume : perVolume.keySet()) {
                final Map<String, FileData> zombieFiles = new Hashtable<String, FileData>();

                try {
                    ServiceSet s = master.getDIRClient().xtreemfs_service_get_by_uuid(null, RPCAuthentication.authNone,
                            RPCAuthentication.userService, volume.id);

                    if (s.getServicesCount() == 0) {
                        // Volume does not exist (is not registered at the DIR).
                        results.add(String.format(DEAD_VOLUME_FORMAT, volume.id));
                        volume.dead();

                    } else {

                        final boolean cowEnabled = false; // FIXME: fetch COW policy for current volume

                        String mrcUUID = null;
                        for (KeyValuePair kvp : s.getServices(0).getData().getDataList()) {
                            if (kvp.getKey().equals("mrc"))
                                mrcUUID = kvp.getValue();
                        }
                        volume.mrc = new ServiceUUID(mrcUUID);

                        RPCResponse<xtreemfs_check_file_existsResponse> r = mrcClient.xtreemfs_check_file_exists(
                                volume.mrc.getAddress(), RPCAuthentication.authNone, RPCAuthentication.userService,
                                volume.id, perVolume.get(volume), localUUID.toString());
                        xtreemfs_check_file_existsResponse response = r.get();
                        r.freeBuffers();


                        if (!response.getVolumeExists()) {
                            // Volume does not exist (is not found at the MRC VolumeManager),
                            // but it is still known to the DIR. The MRC volume removal DeleteVolumeOperation()
                            // deregisters volumes _after_ they are deleted from the database, therefore it is
                            // possible to end in this case.
                            results.add(String.format(DEAD_VOLUME_FORMAT, volume.id));
                            volume.dead();

                        } else {
                            // Check all files from valid volumes.
                            List<String> files = perVolume.get(volume);
                            final AtomicInteger numZombies = new AtomicInteger(0);
                            final AtomicInteger openOFTChecks = new AtomicInteger(0);

                            for (int i = 0; i < files.size(); i++) {
                                final FILE_STATE fileState = response.getFileStates(i);
                                if (fileState == FILE_STATE.ABANDONED || fileState == FILE_STATE.DELETED) {
                                    // remove abandoned replicas immediately
                                    final boolean abandoned = (fileState == FILE_STATE.ABANDONED);

                                    // retrieve the fileName
                                    final String fName = volume.id + ":" + files.get(i);

                                    // retrieve the fileData
                                    final FileData fData = l.files.get(fName);

                                    // check against the OFT
                                    openOFTChecks.incrementAndGet();
                                    master.getPreprocStage().checkDeleteOnClose(files.get(i),
                                            new DeleteOnCloseCallback() {
                                                @Override
                                                public void deleteOnCloseResult(boolean isDeleteOnClose,
                                                        ErrorResponse error) {

                                                    // file is zombie
                                                    if (!isDeleteOnClose && !abandoned) {
                                                        numZombies.incrementAndGet();
                                                        zombies.incrementAndGet();
                                                        zombieFiles.put(fName, fData);
                                                    }

                                                    // deal with the unrestoreable replica
                                                    if (!isDeleteOnClose && abandoned) {
                                                        deleteFile(fName, cowEnabled);
                                                    }
                                                    
                                                    if (openOFTChecks.decrementAndGet() <= 0) {
                                                        synchronized (openOFTChecks) {
                                                            openOFTChecks.notify();
                                                        }
                                                    }
                                                }
                                            });
                                }
                            }

                            synchronized (openOFTChecks) {
                                while (openOFTChecks.get() > 0)
                                    openOFTChecks.wait();
                            }
                            results.add(String.format(VOLUME_RESULT_FORMAT, volume.id, numZombies.get(), files.size()));
                        }
                    }
                } catch (Exception ex) {
                    results.add(String.format(ERROR_FORMAT, volume.id, OutputUtils.stackTraceToString(ex)));
                }

                // Handle dead volumes.
                if (volume.isDead()) {
                    // Every file associated with a dead or deleted volume is a zombie.
                    List<String> files = perVolume.get(volume);
                    for (int i = 0; i < files.size(); i++) {
                        // Retrieve the fileName and fileData and store them.
                        final String fName = volume.id + ":" + files.get(i);
                        final FileData fData = l.files.get(fName);
                        zombieFiles.put(fName, fData);
                    }

                    // TODO: results.add...
                }

                if (zombieFiles.size() != 0) {
                    zombieFilesPerVolume.put(volume, zombieFiles);
                }
            } // for (Volume volume : perVolume.keySet())

            synchronized (this) {
                if (!isRunning)
                    break;
            }

            // deal with the zombies
            for (Volume volume : zombieFilesPerVolume.keySet()) {
                // restore files if the flag is set (files from dead volumes cannot be restored
                if (!volume.isDead() && lostAndFound) {
                    // FIXME (jdillmann): Check if this actually works with Replication/Striping and maybe exclude for EC
                    
                    Map<String, FileData> zombieFiles = zombieFilesPerVolume.get(volume);

                    for (String fileName : zombieFiles.keySet()) {
                        FileData data = zombieFiles.get(fileName);
                        if (!data.metaDataOnly) {
                            RPCResponse r = mrcClient.xtreemfs_restore_file(volume.mrc.getAddress(),
                                    RPCAuthentication.authNone, RPCAuthentication.userService, DEFAULT_RESTORE_PATH,
                                    fileName, data.size, localUUID.toString(),
                                    Integer.valueOf(String.valueOf(data.objectSize)));

                            // the response does not matter
                            r.get();
                            r.freeBuffers();

                            // TODO(jdillmann): clear stored xlocset, or send version to mrc...
                        }
                    }
                    
                    results.add(String.format(ZOMBIES_RESTORED_FORMAT, zombieFiles.keySet().size(), volume.id));

                } else if ((volume.isDead() && removeDeadVolumes) || (!volume.isDead() && removeZombies)) {
                    // Delete all files of dead volumes if the flag is set
                    // or delete zombies if the flag is set.
                    Map<String, FileData> zombieFiles = zombieFilesPerVolume.get(volume);
                    
                    final boolean cowEnabled = false; // FIXME: fetch COW policy for current volume
                    
                    for (final String fileName : zombieFiles.keySet()) {
                        deleteFile(fileName, cowEnabled);
                    }

                    results.add(String.format(ZOMBIES_DELETED_FORMAT, zombieFiles.keySet().size(),
                            (volume.isDead() ? "dead" : "existing"), volume.id));
                }
            }

            // Deal with metaData only directories.
            if (removeMetadata) {
                for (Volume volume : metaOnlyPerVolume.keySet()) {
                    final boolean cowEnabled = false; // FIXME: fetch COW policy for current volume

                    List<String> metaDataDirs = metaOnlyPerVolume.get(volume);
                    for (String fileId : metaDataDirs) {
                        // retrieve the fileName
                        final String fName = volume.id + ":" + fileId;
                        deleteFile(fName, cowEnabled);
                    }
                    // TODO: results.add(...)
                    // results.add(String.format(ZOMBIES_DELETED_FORMAT, zombieFiles.keySet().size(),
                    // (volume.isDead() ? "dead" : "existing"), volume.id));
                }
            }

            synchronized (openDeletes) {
                while (openDeletes.get() > 0)
                    openDeletes.wait();
            }

            synchronized (this) {
                if (!isRunning)
                    break;
            }

        } while (l.hasMore);
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
     * Pass the fileName to the deletion stage, which will eventually delete the file. <br>
     * The openDeletes variable will be updated on progress and notified if no more deletes are open. <br>
     * Errors will be stored in the results. <br>
     * Metadata will be deleted if the XLocVersionState happened longer then metaDataTimeoutS seconds before.
     * 
     * @param fileName
     * @param cowEnabled
     */
    private void deleteFile(final String fileName, final boolean cowEnabled) {
        boolean deleteMetadata;
        try {
            deleteMetadata = checkXLocVersionStateTimeout(fileName);
        } catch (IOException e) {
            deleteMetadata = false;
        }
        
        openDeletes.incrementAndGet();
        
        master.getDeletionStage().deleteObjects(fileName, null, cowEnabled, null, deleteMetadata,
                new DeletionStage.DeleteObjectsCallback() {

                    @Override
                    public void deleteComplete(ErrorResponse error) {
                        if (error != null) {
                            results.add(String.format(ZOMBIE_DELETE_ERROR_FORMAT, fileName, error.getErrorMessage()));
                        }

                        if (openDeletes.decrementAndGet() <= 0) {
                            synchronized (openDeletes) {
                                openDeletes.notifyAll();
                            }
                        }
                    }
                });
    }

    /**
     * Check if the XLocVersionState's last update happened more then metaDataTimeoutS seconds before.
     * 
     * @param fileName
     * @return
     * @throws IOException
     */
    private boolean checkXLocVersionStateTimeout(final String fileName) throws IOException {
        if (!removeMetadata) {
            return false;
        }

        if (metaDataTimeoutS == 0) {
            return true;
        }

        if (metaDataTimeoutS > 0) {
            long toTimeMs = TimeSync.getGlobalTime() - (metaDataTimeoutS * 1000);
            XLocSetVersionState vs = layout.getXLocSetVersionState(fileName);

            // Delete the metaData if the version is too old.
            if (!vs.hasModifiedTime() || vs.getModifiedTime() < toTimeMs) {
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    /**
     * Contains VolumeId and MRC Address.
     * 
     * 23.04.2009
     * @author flangner
     */
    public class Volume {
        final String id;
        ServiceUUID  mrc     = null;
        boolean      dead    = false;
        boolean      deleted = false;

        /**
         * Constructor for Volumes with unknown MRC.
         */
        Volume(String volId) {
            this.id = volId;
        }

        /**
         * to mark dead volumes.
         */
        void dead() {
            this.dead = true;
        }

        boolean isDead() {
            return this.dead;
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
