/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.storage;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.storage.HashStorageLayout.FileList;

/**
 *
 * @author bjko
 */
public class CleanupThread extends LifeCycleThread {

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

    private long zombies;

    private long startTime;

    public CleanupThread(OSDRequestDispatcher master, HashStorageLayout layout) {
        super("CleanupThr");
        this.master = master;
        this.isRunning = false;
        this.quit = false;
        this.layout = layout;
        this.results = new StringSet();
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
                        filesChecked, zombies, d.toGMTString());
            } else {
                Date d = new Date(startTime);
                return "not running, last check started "+d.toGMTString();
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
                zombies = 0;
                startTime = TimeSync.getGlobalTime();
                final MRCClient mrcClient = new MRCClient(master.getRPCClient(),null);
                do {
                    l = layout.getFileList(l, 1024*4);
                    Map<String,StringSet> perVolume = new HashMap();
                    for (String fname : l.files) {
                        filesChecked++;
                        String[] tmp = fname.split(":");
                        StringSet flist = perVolume.get(tmp[0]);
                        if (flist == null) {
                            flist = new StringSet();
                            perVolume.put(tmp[0],flist);
                        }
                        flist.add(tmp[1]);
                    }

                    synchronized (this) {
                        if (!isRunning)
                            break;
                    }

                    //check each volume
                    for (String volId : perVolume.keySet()) {
                        try {
                             RPCResponse<ServiceSet> vol = master.getDIRClient().xtreemfs_service_get_by_uuid(null, volId);
                             ServiceSet s = vol.get();
                             if (s.size() == 0) {
                                 //volume does not exist
                                 results.add("volume "+volId+" is dead (not registered at directory service)");
                             } else {
                                 String mrc = s.get(0).getData().get("mrc");
                                 ServiceUUID mrcUuid = new ServiceUUID(mrc);
                                 RPCResponse<String> r = mrcClient.xtreemfs_checkFileExists(mrcUuid.getAddress(), volId, perVolume.get(volId));
                                 String eval = r.get();
                                 r.freeBuffers();
                                 if (eval.equals("2")) {
                                     //volume was deleted (not a dead volume, since MRC answered!)
                                     results.add("volume "+volId+" was removed from MRC "+mrc);
                                 } else {
                                     //check all files...
                                     StringSet files = perVolume.get(volId);
                                     int numZombies = 0;
                                     for (int i = 0; i < files.size(); i++) {
                                         if (eval.charAt(i) == '0') {
                                             //file ok
                                             numZombies++;
                                             zombies++;
                                         }
                                     }
                                     results.add("volume "+volId+" had "+numZombies+" zombie (out of "+files.size()+" files checked) ;"+volId+";"+numZombies+";"+files.size());
                                 }
                             }
                        } catch (Exception ex) {
                            results.add("ERROR: cannot check volume "+volId+", reason: "+ex);
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

}
