package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.stages.XLocSetCoordinator;
import org.xtreemfs.mrc.stages.XLocSetCoordinatorCallback;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC
        .xtreemfs_replica_mark_completeRequest;

import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;

/**
 * MRC operation to mark a previously added replica as a complete replica.
 * This operation should only be used by an OSD when it fetched and stored all
 * objects locally (e.g. when the replication completed successfully).
 * <p>
 * This operation is only necessary for read-only replication.
 * Probably we should throw some exception if the file's replication policy is
 * not read-only.
 *
 * TODO support stripe width > 1 (e.g., single replica on more than one OSD).
 */
public class MarkReplicaCompleteOperation extends MRCOperation implements
        XLocSetCoordinatorCallback {

    public MarkReplicaCompleteOperation(MRCRequestDispatcher master) {
        super(master);
    }

    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        final xtreemfs_replica_mark_completeRequest rqArgs =
                (xtreemfs_replica_mark_completeRequest) rq.getRequestArgs();

        String fileID = rqArgs.getFileId();
        String osdWithCompleteReplica = rqArgs.getOsdUuid();

        MRCHelper.GlobalFileIdResolver idResolver = new MRCHelper
                .GlobalFileIdResolver(fileID);

        VolumeManager volumeManager = master.getVolumeManager();

        StorageManager storageManager =
                volumeManager.getStorageManager(idResolver.getVolumeId());

        org.xtreemfs.mrc.metadata.FileMetadata fileMetadata =
                storageManager.getMetadata(idResolver.getLocalFileId());

        XLocList xLocList = fileMetadata.getXLocList();

        if (Logging.isDebug()) {
            logXLocInfo(xLocList);
        }

        // assume that there is exactly one entry in the xLocList for
        // OSD osdWithCompleteReplica
        XLoc replicaToBeUpdated = null;
        for (int i = 0; i < xLocList.getReplicaCount(); i++) {
            if (osdWithCompleteReplica
                    .equals(xLocList.getReplica(i).getOSD(0))) {
                replicaToBeUpdated = xLocList.getReplica(i);
                break;
            }
        }
        if (replicaToBeUpdated == null) {
            // the file has probably been deleted in the meantime.
            Logging.logMessage(Logging.LEVEL_WARN, Logging.Category.replication, this,
                               "OSD %s not found in XLocs of file %s",
                               osdWithCompleteReplica, fileID);
            return;
        }

        int updatedReplicationFlag =
                ReplicationFlags.setReplicaIsComplete(
                        replicaToBeUpdated.getReplicationFlags());
        replicaToBeUpdated.setReplicationFlags(updatedReplicationFlag);
        XLoc updatedReplica = replicaToBeUpdated;

        XLoc[] updatedXLocs = new XLoc[xLocList.getReplicaCount()];
        for (int i = 0; i < xLocList.getReplicaCount(); i++) {
            if (osdWithCompleteReplica
                    .equals(xLocList.getReplica(i).getOSD(0))) {
                // if the read-only replica has more than one OSD location,
                // setting the complete flag will likely be erroneous
                // (as all OSDs have to fetch all their objects of the file,
                // in order for the replica to be complete)
                updatedXLocs[i] = updatedReplica;
            } else {
                updatedXLocs[i] = xLocList.getReplica(i);
            }
        }

        if (Logging.isDebug()) {
            StringBuilder updateSB = new StringBuilder();
            updateSB.append("updated XLocs: ");
            for (int i = 0; i < updatedXLocs.length; i++) {
                updateSB
                        .append("replica: ")
                        .append(i)
                        .append(" replication flags: ")
                        .append(updatedXLocs[i].getReplicationFlags())
                        .append(" osdUUID: ")
                        .append(updatedXLocs[i].getOSD(0))
                        .append(" ");
            }
            Logging.logMessage(Logging.LEVEL_DEBUG,
                               Logging.Category.replication,
                               this,
                               updateSB.toString());
        }

        XLocList updatedXLocList =
                storageManager.createXLocList(updatedXLocs,
                                              xLocList.getReplUpdatePolicy(),
                                              xLocList.getVersion() + 1);

        XLocSetCoordinator xLocSetCoordinator = master.getXLocSetCoordinator();
        XLocSetCoordinator.RequestMethod requestMethod =
                xLocSetCoordinator.requestXLocSetChange(fileID,
                                                        fileMetadata,
                                                        xLocList,
                                                        updatedXLocList,
                                                        rq, this);

        AtomicDBUpdate atomicDBUpdate =
                storageManager.createAtomicDBUpdate(xLocSetCoordinator,
                                                    requestMethod);

        xLocSetCoordinator.lockXLocSet(fileMetadata,
                                       storageManager,
                                       atomicDBUpdate);

        rq.setResponse(emptyResponse.getDefaultInstance());

        atomicDBUpdate.execute();

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this,
                               "Replica on OSD %s of file with id %s is now " +
                                       "marked as complete",
                               osdWithCompleteReplica, fileID);
        }
    }

    @Override
    public void installXLocSet(String fileId, XLocList newXLocList, XLocList
            prevXLocList) throws Throwable {
        VolumeManager volumeManager = master.getVolumeManager();
        MRCHelper.GlobalFileIdResolver idResolver = new MRCHelper
                .GlobalFileIdResolver(fileId);
        StorageManager storageManager =
                volumeManager.getStorageManager(idResolver.getVolumeId());

        // Retrieve the file metadata.
        final FileMetadata fileMetadata =
                storageManager.getMetadata(idResolver.getLocalFileId());
        if (fileMetadata == null) {
            // if the file does not exist, it has probably deleted in the
            // meantime, which should be fine
            Logging.logMessage(Logging.LEVEL_DEBUG,
                               Logging.Category.replication,
                               this,
                               "marking replica as complete failed" +
                                       "because file %s does not exist " +
                                       "(anymore)", fileId);
            return;
        }

        AtomicDBUpdate update = storageManager.createAtomicDBUpdate(null,
                                                                    null);

        // Update the X-Locations list.
        fileMetadata.setXLocList(newXLocList);
        storageManager.setMetadata(fileMetadata, FileMetadata.RC_METADATA,
                                   update);

        master.getXLocSetCoordinator().unlockXLocSet(fileMetadata,
                                                     storageManager,
                                                     update);
        update.execute();
    }

    @Override
    public void handleInstallXLocSetError(Throwable error, String fileId,
                                          XLocList newXLocList, XLocList
                                                  prevXLocList) throws
            Throwable {
        // not really clear under which conditions this method is called
        Logging.logMessage(Logging.LEVEL_WARN,
                           Logging.Category.replication,
                           this,
                           "marking replica for file %s" +
                                   " as complete failed" +
                                   "because XLocSet change failed. Error: %s",
                           fileId,
                           error.toString());
    }

    private void logXLocInfo(XLocList xLocList) {
        StringBuilder replicaInfo = new StringBuilder();
        for (int i = 0; i < xLocList.getReplicaCount(); i++) {
            replicaInfo
                    .append("replica ")
                    .append(i)
                    .append(" osd: ")
                    .append(xLocList.getReplica(i)
                                    .getOSD(0))
                    .append(" replication flags: ")
                    .append(xLocList.getReplica(i)
                                    .getReplicationFlags())
                    .append("\n");
        }
        Logging.logMessage(Logging.LEVEL_DEBUG, this,
                           replicaInfo.toString());
    }

}
