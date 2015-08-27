/*
 * Copyright (c) 2015 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.operations;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.stages.XLocSetCoordinator;
import org.xtreemfs.mrc.stages.XLocSetCoordinatorCallback;
import org.xtreemfs.mrc.stages.XLocSetLock;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_reselect_osdsRequest;

public class ReselectOSDsOperation extends MRCOperation implements XLocSetCoordinatorCallback {

    public ReselectOSDsOperation(MRCRequestDispatcher master) {
        super(master);
    }

    @Override
    public void startRequest(MRCRequest rq) throws Throwable {

        // perform master redirect if necessary
        if (master.getReplMasterUUID() != null
                && !master.getReplMasterUUID().equals(master.getConfig().getUUID().toString()))
            throw new DatabaseException(ExceptionType.REDIRECT);

        final xtreemfs_reselect_osdsRequest rqArgs = (xtreemfs_reselect_osdsRequest) rq.getRequestArgs();

        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();

        final Path p = new Path(rqArgs.getVolumeName(), rqArgs.getPath());
        final StorageManager sMan = vMan.getStorageManagerByName(p.getComp(0));
        final PathResolver res = new PathResolver(sMan, p);
        final VolumeInfo volume = sMan.getVolumeInfo();

        final FileMetadata file = res.getFile();
        final String fileId = MRCHelper.createGlobalFileId(volume, file);

        if (file.isDirectory()) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "replicas may only be added to files");
        }

        if (sMan.getSoftlinkTarget(file.getId()) != null) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "file '" + p + "' is a symbolic link");
        }
        
        // Check if a xLocSetChange is already in progress.
        XLocSetLock lock = master.getXLocSetCoordinator().getXLocSetLock(file, sMan);
        if (lock.isLocked()) {
            if (lock.hasCrashed()) {
                // Ignore if a previous xLocSet change did not finish, because the replicas will be revalidated when the
                // new xLocSet is installed by this operation.
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "Previous xLocSet change did not finish.");
                }
            } else {
                throw new UserException(POSIXErrno.POSIX_ERROR_EAGAIN,
                        "xLocSet change already in progress. Please retry.");
            }
        }

        // Get the current xLoclist
        XLocList curXLocList = file.getXLocList();

        String replicationUpdatePolicy = curXLocList.getReplUpdatePolicy();
        int numReplicas = curXLocList.getReplicaCount();
        
        // Reselecting OSDs is only available for RW replicated files
        if (!(replicationUpdatePolicy.equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ)
                || replicationUpdatePolicy.equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE)
                || replicationUpdatePolicy.equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARA))) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                    "Reselection OSDs is not available for this ReplicaUpdatePolicy.");
        } 

        List<XLoc> repls = new ArrayList<XLoc>();
        XLocList newXLocList = null;
        int newXLocListVersion = curXLocList.getVersion() + 1;
        
        for (int i = 0; i < numReplicas; i++) {
            // TODO(jdillmann): COMPLETE should be probably removed!!!
            int replFlags = curXLocList.getReplica(i).getReplicationFlags();
            StripingPolicy stripingPolicy = curXLocList.getReplica(i).getStripingPolicy();
            InetAddress clientAddress = ((InetSocketAddress) rq.getRPCRequest().getSenderAddress()).getAddress();

            // Create a new replica based on the OSD Selection policy
            XLoc replica = MRCHelper.createReplica(stripingPolicy, sMan, master.getOSDStatusManager(), volume,
                    res.getParentDirId(), p.toString(), clientAddress, rqArgs.getCoordinates(), newXLocList, replFlags);
            repls.add(replica);

            // Successively build the new xLocList
            newXLocList = sMan.createXLocList(repls.toArray(new XLoc[repls.size()]), replicationUpdatePolicy,
                    newXLocListVersion);
        }

        XLocSetCoordinator coordinator = master.getXLocSetCoordinator();
        XLocSetCoordinator.RequestMethod m = coordinator.requestXLocSetChange(fileId, file, curXLocList, newXLocList,
                rq, this);

        // Make an update with the RequestMethod as context and the Coordinator as callback. This will enqueue
        // the RequestMethod when the update is complete.
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(coordinator, m);

        // Lock the replica and start the coordination.
        coordinator.lockXLocSet(file, sMan, update);

        update.execute();
    }

    @Override
    public void installXLocSet(MRCRequest rq, String fileId, XLocList newXLocList, XLocList prevXLocList)
            throws Throwable {
        final VolumeManager vMan = master.getVolumeManager();
        final GlobalFileIdResolver idRes = new GlobalFileIdResolver(fileId);
        final StorageManager sMan = vMan.getStorageManager(idRes.getVolumeId());

        // Retrieve the file metadata.
        final FileMetadata file = sMan.getMetadata(idRes.getLocalFileId());
        if (file == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file '" + fileId + "' does not exist");

        file.setXLocList(newXLocList);

        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);

        // Update the X-Locations list.
        sMan.setMetadata(file, FileMetadata.RC_METADATA, update);

        // Unlock the replica.
        master.getXLocSetCoordinator().unlockXLocSet(file, sMan, update);

        // Set the response.
        rq.setResponse(emptyResponse.getDefaultInstance());

        update.execute();
    }

}
