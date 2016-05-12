package org.xtreemfs.utils.xtfs_scrub;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.libxtreemfs.AdminFileHandle;
import org.xtreemfs.common.libxtreemfs.AdminVolume;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.InvalidChecksumException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.utils.xtfs_scrub.xtfs_scrub.FileScrubbedListener;
import org.xtreemfs.utils.xtfs_scrub.xtfs_scrub.ReturnStatus;

public class FileScrubber implements Runnable {

    private AdminFileHandle            fileHandle;

    private AdminVolume                volume;

    private String                     fileName;

    private final FileScrubbedListener listener;

    private long                       nextObjectToScrub;

    /** Size of the file on the OSD(s). */
    private long                       byteCounter;

    private final Set<String>          removedOSDs;

    private Set<ReturnStatus>          returnStatus;

    private final boolean              repair, delete;

    private boolean                    isReadOnly;

    public FileScrubber(String fileName, AdminVolume volume, FileScrubbedListener listener,
            Set<String> removedOSDs, boolean repair, boolean delete) throws PosixErrorException,
            AddressToUUIDNotFoundException, IOException {

        this.volume = volume;
        try {
            this.fileHandle = volume.openFile(xtfs_scrub.credentials, fileName,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());
            isReadOnly = false;
        } catch (PosixErrorException e) {
            this.fileHandle = volume.openFile(xtfs_scrub.credentials, fileName,
                    SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber());
            isReadOnly = true;
        }
        this.fileName = fileName;
        this.listener = listener;
        returnStatus = new TreeSet<ReturnStatus>();

        nextObjectToScrub = 0;
        byteCounter = 0;
        this.removedOSDs = removedOSDs;
        this.repair = repair;
        this.delete = delete;
    }

    public void run() {
        String replicaUpdatePolicy = fileHandle.getReplicaUpdatePolicy();

        // If file has only one replica, treat it as non replicated.
        if (ReplicaUpdatePolicies.isRO(replicaUpdatePolicy) 
                && fileHandle.getReplicasList().size() > 1) {
            scrubReadOnlyReplicatedFile();
        } else {
            scrubRWOrNonReplicatedFile();
        }
        // FIXME (jdillmann): Handle EC files

        listener.fileScrubbed(fileName, byteCounter, returnStatus);
        try {
            fileHandle.close();
        } catch (IOException e) {
            printFileErrorMessage("unable to close file, because" + e);
        }
    }

    private void scrubReadOnlyReplicatedFile() {

        // Get replicas and number of objects.
        List<Replica> replicas = fileHandle.getReplicasList();
        long numObjs = 0;
        boolean checkObjects = true;
        try {
            numObjs = fileHandle.getNumObjects(xtfs_scrub.credentials);
        } catch (IOException ex) {
            printFileErrorMessage("cannot get Number of Objects: " + ex);
            checkObjects = false;
            returnStatus.add(ReturnStatus.UNREACHABLE);
        }

        // Read all replicas.
        List<Replica> removedReplicas = new LinkedList<Replica>();
        for (int r = 0; r < replicas.size(); r++) {
            Replica replica = replicas.get(r);
            // check if an OSD was removed.
            boolean isReplOnDeadOsd = false;
            for (String osd : replica.getOsdUuidsList()) {
                if (removedOSDs.contains(osd)) {
                    // Store replica for later re-creation.
                    removedReplicas.add(replica);
                    isReplOnDeadOsd = true;
                    break;
                }
            }

            // check objects only if numObjs is available and replica is not on an dead OSD
            if (checkObjects & !isReplOnDeadOsd) {
                try {
                    // This effectively marks the replica as 'complete' if not done yet.
                    fileHandle.checkAndMarkIfReadOnlyReplicaComplete(r, xtfs_scrub.credentials);
                } catch (IOException ex) {
                    printFileErrorMessage("cannot mark replica# " + r + " of file " + fileName
                            + " as complete, because " + ex);
                }
                // check objects of the replica
                for (long o = 0; o < numObjs; o++) {
                    try {
                        fileHandle.checkObjectAndGetSize(r, o);
                    } catch (InvalidChecksumException ex) {
                    	String errormsg = "";
                        if (repair) {
                            try {
                                fileHandle.repairObject(r, o);
                                errormsg = "object #" + o + " of replica " + r + " had an invalid checksum on OSD "
                                        + getOSDUUIDFromObjectNo(replica, o) + " and was repaired";
                                returnStatus.add(ReturnStatus.FAILURE_OBJECTS);
                            } catch (IOException e) {
                                errormsg = "object #" + o + " of replica " + r + " has an invalid checksum on OSD "
                                        + getOSDUUIDFromObjectNo(replica, o) + " and is irreparable";
                                returnStatus.add(ReturnStatus.FAILURE_OBJECTS);
                            }
                        } else {
                            errormsg = "object #" + o + " of replica " + r + " has an invalid checksum on OSD "
                                    + getOSDUUIDFromObjectNo(replica, o);
                            returnStatus.add(ReturnStatus.FAILURE_OBJECTS);                            	
                        }
                        printFileErrorMessage(errormsg);
                    } catch (IOException ex) {
                        printFileErrorMessage("unable to check object #" + o + " of replica " + r + ": " + ex);
                        returnStatus.add(ReturnStatus.UNREACHABLE);
                    }
                }
            }
        }
        // handle removed replicas
        if (!removedReplicas.isEmpty()) {
            if (repair) {
                recreateReplicas(removedReplicas);
            } else {
                printFileErrorMessage("lost " + removedReplicas.size() + " replicas due to dead OSDs");
            }
            returnStatus.add(ReturnStatus.FAILURE_REPLICAS);
        }
        // if everything is fine, set returnStatus to FILE_OK
        if (returnStatus.size() == 0) {
            returnStatus.add(ReturnStatus.FILE_OK);
        }
    }

    private void scrubRWOrNonReplicatedFile() {

        // Get replicas.
        List<Replica> replicas = fileHandle.getReplicasList();

        // Read all replicas.
        List<Replica> removedReplicas = new LinkedList<Replica>();
        for (int r = 0; r < replicas.size(); r++) {
            Replica replica = replicas.get(r);
            // Check if an OSD was removed.
            boolean isReplOnDeadOsd = false;
            for (String osd : replica.getOsdUuidsList()) {
                if (removedOSDs.contains(osd)) {
                    // If file is not replicated or has only one replica, delete it.
                    if (ReplicaUpdatePolicies.isNONE(fileHandle.getReplicaUpdatePolicy())
                            || fileHandle.getReplicasList().size() == 1) {
                        String errMsg = "file data was stored on removed OSD. File is lost.";

                        if (delete) {
                            errMsg = "file data was stored on removed OSD. File was deleted.";
                            try {
                                // Delete file.
                                volume.unlink(xtfs_scrub.credentials, fileName);
                            } catch (IOException ex2) {
                                errMsg = "unable to delete " + fileName + ", because: " + ex2.getMessage();
                            }
                        }
                        printFileErrorMessage(errMsg);
                        returnStatus.add(ReturnStatus.FILE_LOST);
                        return;
                    } else {
                        // Store replica for later re-creation.
                        removedReplicas.add(replica);
                        isReplOnDeadOsd = true;
                        break;
                    }
                }
            }
            if (!isReplOnDeadOsd) {
                // check objects of the replica
                boolean eof = false;
                nextObjectToScrub = 0;
                while (!eof) {
                    try {
                        int objSize = fileHandle.checkObjectAndGetSize(r, nextObjectToScrub++);
                        if (objSize < replica.getStripingPolicy().getStripeSize()) {
                            eof = true;
                        }
                    } catch (InvalidChecksumException ex) {
                        printFileErrorMessage("object #" + (nextObjectToScrub - 1) + " of replica " + r
                                + " has an invalid checksum on OSD " + replica.getOsdUuids((int) ((nextObjectToScrub - 1) % replica.getOsdUuidsCount())));
                        returnStatus.add(ReturnStatus.FAILURE_OBJECTS);
                        break;
                    } catch (IOException ex) {
                        printFileErrorMessage("unable to check object #" + (nextObjectToScrub - 1)
                                + " of replica " + r + ": " + ex);
                        returnStatus.add(ReturnStatus.UNREACHABLE);
                        break;
                    }
                }
            }
        }
        // Handle removed replicas.
        if (!removedReplicas.isEmpty()) {
            if (repair) {
                recreateReplicas(removedReplicas);
            } else {
                printFileErrorMessage("lost " + removedReplicas.size() + " replicas due to dead OSDs");
            }
            returnStatus.add(ReturnStatus.FAILURE_REPLICAS);

        }
        // Check file size on MRC.
        try {
            // Get file size on MRC and OSDs
            long mrcFileSize = volume.getAttr(xtfs_scrub.credentials, fileName).getSize();
            byteCounter = fileHandle.getSizeOnOSD();
            
            if (byteCounter != mrcFileSize) {
                if (repair) {
                    // update file size on MRC
                    fileHandle.truncate(xtfs_scrub.credentials, byteCounter, true);
                    printFileErrorMessage("corrected file size from " + mrcFileSize + " to "
                            + byteCounter + " bytes");
                } else {
                    printFileErrorMessage("incorrect file size: is " + mrcFileSize + " but should be "
                            + byteCounter + " bytes");
                }
                returnStatus.add(ReturnStatus.WRONG_FILE_SIZE);
            }
        } catch (IOException ex) {
            printFileErrorMessage("unable to get file size: " + ex);
            returnStatus.add(ReturnStatus.UNREACHABLE);
        }

        // if everything is fine, set returnStatus to FILE_OK
        if (returnStatus.size() == 0) {
            returnStatus.add(ReturnStatus.FILE_OK);
        }
    }

    private void printFileErrorMessage(String error) {
        System.err.format("file '%s' (%s):\n\t%s\n", fileName, fileHandle.getGlobalFileId(), error);
    }

    static private String getOSDUUIDFromObjectNo(Replica replica, long objectNo) {
        return replica.getOsdUuids((int) objectNo % replica.getOsdUuidsCount());
    }


    private void recreateReplicas(List<Replica> removedReplicas) {
        int numReplRemoved = removedReplicas.size();
        int numNewReplicas = 0;

        for (Replica replica : removedReplicas) {

            // remove old replica
            try {
                volume.removeReplica(xtfs_scrub.credentials, fileName, replica.getOsdUuids(0));
            } catch (IOException ex) {
                printFileErrorMessage("unable to remove replica of " + fileName + ", because: " + ex);
                continue;
            }

            // get suitable OSDs for re-creation
            List<String> osds;
            try {
                osds = volume.getSuitableOSDs(xtfs_scrub.credentials, fileName, replica.getStripingPolicy()
                        .getWidth());
                if (osds.size() < replica.getStripingPolicy().getWidth()) {
                    printFileErrorMessage("cannot create new replica, not enough OSDs available");
                    continue;
                }
            } catch (IOException e) {
                printFileErrorMessage("cannot create new replicas, unable to get suitable OSDs");
                break;
            }

            // create new replica
            Replica.Builder newReplicaBuilder = Replica.newBuilder();

            // set OSDs
            newReplicaBuilder.addAllOsdUuids(osds.subList(0, replica.getStripingPolicy().getWidth()));

            // recycle the replication flags of the removed replica(except for the complete
            // flag)
            newReplicaBuilder.setReplicationFlags(replica.getReplicationFlags()
                    & ~REPL_FLAG.REPL_FLAG_IS_COMPLETE.getNumber());

            // recycle the striping policy of the removed replica
            newReplicaBuilder.setStripingPolicy(replica.getStripingPolicy());

            Replica newReplica = newReplicaBuilder.build();

            try {
                // add replica
                volume.addReplica(xtfs_scrub.credentials, fileName, newReplica);
                numNewReplicas++;
            } catch (IOException ex) {
                printFileErrorMessage("cannot create new replica: " + ex);
            }
        }
        if (numNewReplicas == numReplRemoved) {
            printFileErrorMessage("lost " + numReplRemoved + " replicas due to dead OSDs. Created "
                    + numReplRemoved + " new replicas.");
        } else {
            printFileErrorMessage("lost " + numReplRemoved + " replicas due to dead OSDs. Could only create "
                    + numNewReplicas + " due to a lack of suitable OSDs or communication errors.");
        }

    }
}
