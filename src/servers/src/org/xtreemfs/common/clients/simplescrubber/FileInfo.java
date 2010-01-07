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
 * AUTHORS: Björn Kolbeck(ZIB)
 */


package org.xtreemfs.common.clients.simplescrubber;

import java.io.IOException;
import java.util.Set;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.InvalidChecksumException;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.Replica;
import org.xtreemfs.interfaces.Constants;


/**
 *
 * @author bjko
 */
public class FileInfo implements Runnable {

    private final RandomAccessFile   file;

    private final FileScrubbedListener listener;

    private long                     nextObjectToScrub;

    private long                     byteCounter;

    private final Set<String>        removedOSDs;

    private final boolean           repair, delete;

    public FileInfo(RandomAccessFile file, FileScrubbedListener listener, Set<String> removedOSDs,
            boolean repair, boolean delete) {
        this.file = file;
        this.listener = listener;

        nextObjectToScrub = 0;
        byteCounter = 0;
        this.removedOSDs = removedOSDs;
        this.repair = repair;
        this.delete = delete;
    }

    public void run() {

        if (file.isReadOnly()) {
            System.out.println("read-only file");
            //check replicas...
            Replica[] replicas = null;
            long numObjs = 0;
            try {
                replicas = file.getFile().getReplicas();
                numObjs = file.getNumObjects();
            } catch (IOException ex) {
                printFileErrorMessage(file.getFile(), file.getFileId(), "cannot read file size from MRC: "+ex);
                listener.fileScrubbed(file, byteCounter, ReturnStatus.UNREACHABLE);
                return;
            }
            //read all replicas
            int numFullReplRm = 0;
            int numPartialReplRm = 0;
            int numComplete = 0;
            for (int r = 0; r < replicas.length; r++) {
                System.out.println("check replica: "+r);
                Replica replica = replicas[r];
                //check if an OSD was removed
                boolean replDeleted = false;
                for (int o = 0; o < replica.getStripeWidth(); o++) {
                    if (removedOSDs.contains(replica.getOSDUuid(o))) {
                        //FIXME: remove
                        if (repair) {
                            try {
                                replica.removeReplica(false);
                                replDeleted = true;
                            } catch (Exception ex) {
                            }
                        }
                        if (replica.isFullReplica()) {
                            numFullReplRm++;
                        } else {
                            numPartialReplRm++;
                        }
                        break;
                    } else {
                        try {
                            if (replica.isFullReplica() && replica.isCompleteReplica())
                                numComplete++;
                        } catch (Exception ex) {
                        }
                    }
                }
                if (numPartialReplRm > 0) {
                    printFileErrorMessage(file.getFile(), file.getFileId(), "file has "+numPartialReplRm+" dead partial replicas (non recoverable)");
                }
                if (replDeleted)
                    continue;
                file.forceReplica(replica.getOSDUuid(0));
                System.out.println("checking replica: "+file.getCurrentReplica());
                
                for (long o = 0; o < numObjs; o++) {
                    try {
                        int objSize = file.checkObject(o);
                        byteCounter += objSize;
                    } catch (InvalidChecksumException ex) {
                        printFileErrorMessage(file.getFile(), file.getFileId(), "object #"+o+" has an invalid checksum on OSD: "+ex);
                        listener.fileScrubbed(file, byteCounter, ReturnStatus.FILE_CORRUPT);
                        continue;
                    } catch (IOException ex) {
                        printFileErrorMessage(file.getFile(), file.getFileId(), "unable to check object #"+o+": "+ex);
                        listener.fileScrubbed(file, byteCounter, ReturnStatus.UNREACHABLE);
                        return;
                    }
                }
            }
            if (numFullReplRm > 0) {
                //FIXME: create new replicas
                if (repair) {
                    //add replicas
                    int numNewReplicas = 0;
                    while (numNewReplicas < numFullReplRm) {
                        try {
                            String[] osds = file.getFile().getSuitableOSDs(1);
                            if (osds.length == 0) {
                                break;
                            }
                            file.getFile().addReplica(1, osds, Constants.REPL_FLAG_FULL_REPLICA);
                            numNewReplicas++;
                        } catch (Exception ex) {
                            break;
                        }
                    }
                    if (numNewReplicas == numFullReplRm) {
                        printFileErrorMessage(file.getFile(), file.getFileId(), "lost "+numFullReplRm+" replicas due to dead OSDs. Created "+numFullReplRm+" new replicas.");
                    } else {
                        printFileErrorMessage(file.getFile(), file.getFileId(), "lost "+numFullReplRm+" replicas due to dead OSDs. Could only create "+numNewReplicas+" due to a lack of suitable OSDs or communication errors.");
                    }
                } else {
                    printFileErrorMessage(file.getFile(), file.getFileId(), "lost "+numFullReplRm+" replicas due to dead OSDs");
                }
            }


        } else {
            boolean eof = false;
            do {
                try {

                    int objSize = file.checkObject(nextObjectToScrub++);
                    if (objSize < file.getCurrentReplicaStripeSize()) {
                        eof = true;
                    }
                    byteCounter += objSize;
                } catch (InvalidChecksumException ex) {
                    printFileErrorMessage(file.getFile(), file.getFileId(), "object #"+(nextObjectToScrub-1)+" has an invalid checksum on OSD: "+ex);
                    listener.fileScrubbed(file, byteCounter, ReturnStatus.FILE_CORRUPT);
                } catch (IOException ex) {
                    //check if there is a dead OSD...
                    boolean isDead = false;
                    for (String uuid : file.getLocationsList().getReplicas().get(0).getOsd_uuids()) {
                        if (removedOSDs.contains(uuid)) {
                            isDead = true;
                        }
                    }
                    if (isDead) {
                        String errMsg = "file data was stored on removed OSD. File is lost.";
                        if (delete) {
                            errMsg = "file data was stored on removed OSD. File was deleted.";
                            try {
                                file.getFile().delete();
                            } catch (Exception ex2) {
                            }
                        }
                        printFileErrorMessage(file.getFile(), file.getFileId(), errMsg);
                        listener.fileScrubbed(file, byteCounter, ReturnStatus.FILE_CORRUPT);
                    } else {
                        printFileErrorMessage(file.getFile(), file.getFileId(), "unable to check object #"+(nextObjectToScrub-1)+": "+ex);
                        listener.fileScrubbed(file, byteCounter, ReturnStatus.UNREACHABLE);
                    }
                    return;
                }
            } while (!eof);

            try {
                long mrcFileSize = file.length();
                if (!file.isReadOnly()  && (byteCounter != mrcFileSize)) {
                    if (repair) {
                        file.forceFileSize(byteCounter);
                        printFileErrorMessage(file.getFile(), file.getFileId(), "corrected file size from "+mrcFileSize+" to "+byteCounter+" bytes");
                    } else {
                        printFileErrorMessage(file.getFile(), file.getFileId(), "incorrect file size: is "+mrcFileSize+" but should be "+byteCounter+" bytes");
                    }
                }

            } catch (IOException ex) {
                printFileErrorMessage(file.getFile(), file.getFileId(), "unable to read file size from MRC: "+ex);
                listener.fileScrubbed(file, byteCounter, ReturnStatus.UNREACHABLE);
                return;
            }
        }
        listener.fileScrubbed(file, byteCounter, ReturnStatus.FILE_OK);
    }


    public static interface FileScrubbedListener {
        public void fileScrubbed(RandomAccessFile file, long bytesScrubbed, ReturnStatus rstatus);
    }

    public static enum ReturnStatus {
        FILE_OK,
        CORRECTED_FILE_SIZE,
        REPLACED_OBJECTS,
        REPLACED_REPLICAS,
        UNREACHABLE,
        FILE_CORRUPT
    };

    public static void printFileErrorMessage(File f, String fileId, String error) {
        System.err.format("file '%s' (%s):\n\t%s\n",f.getPath(),fileId,error);
    }



}
