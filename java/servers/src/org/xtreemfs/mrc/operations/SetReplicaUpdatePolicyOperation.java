/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.ReplicationPolicy;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.stages.XLocSetLock;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_set_replica_update_policyRequest;

public class SetReplicaUpdatePolicyOperation extends MRCOperation {

    public SetReplicaUpdatePolicyOperation(MRCRequestDispatcher master) {
        super(master);
    }

    @Override
    public void startRequest(MRCRequest rq) throws Throwable {

        final xtreemfs_set_replica_update_policyRequest rqArgs = (xtreemfs_set_replica_update_policyRequest) rq
                .getRequestArgs();

        final String newReplUpdatePolicy = rqArgs.getUpdatePolicy();

        final FileAccessManager faMan = master.getFileAccessManager();
        final VolumeManager vMan = master.getVolumeManager();

        validateContext(rq);

        StorageManager sMan = null;
        FileMetadata file = null;
        String fileId;

        if (rqArgs.hasFileId()) {

            fileId = rqArgs.getFileId();

            // Parse volume and file ID from global file ID.
            GlobalFileIdResolver idRes = new GlobalFileIdResolver(fileId);

            sMan = vMan.getStorageManager(idRes.getVolumeId());

            // Retrieve the file metadata.
            file = sMan.getMetadata(idRes.getLocalFileId());
        } else if (rqArgs.hasVolumeName() && rqArgs.hasPath()) {

            final Path p = new Path(rqArgs.getVolumeName(), rqArgs.getPath());

            sMan = vMan.getStorageManagerByName(p.getComp(0));
            final PathResolver res = new PathResolver(sMan, p);

            res.checkIfFileDoesNotExist();
            file = res.getFile();

            MRCHelper.createGlobalFileId(sMan.getVolumeInfo(), file);

            // Check whether the path prefix is searchable.
            faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser,
                    rq.getDetails().groupIds);

        } else {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "either file ID or volume name + path required");
        }


        if (file == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file '" + rqArgs.getFileId()
                    + "' does not exist");

        if (file.isDirectory())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM,
                    "replica-update policies may only be set on files");

        // check for the admin password to match the provided password
        boolean privileged = (master.getConfig().getAdminPassword().length() > 0
                && master.getConfig().getAdminPassword().equals(rq.getDetails().password));

        // whether the user has privileged permissions to set system attributes
        // the admin password accounts for privileged access
        faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser || privileged,
                rq.getDetails().groupIds);

        // Set the new replicaUpdatePolicy
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        setReplicaUpdatePolicy(master, sMan, update, file, newReplUpdatePolicy);

        // Set the response.
        rq.setResponse(emptyResponse.getDefaultInstance());

        update.execute();

        // TODO (jdillmann): Make async like AddReplica etc?
    }

    /**
     * This method has been separated to allow backwards compatibility with previous versions of the xtfsutil which is
     * was using xattribs to set the replica update policy via {@link MRCHelper#setSysAttrValue}.
     * 
     * @throws UserException
     * @throws DatabaseException
     */
    public static void setReplicaUpdatePolicy(MRCRequestDispatcher master, StorageManager sMan, AtomicDBUpdate update,
            FileMetadata file, String newReplicaUpdatePolicy) throws UserException, DatabaseException {
        // Check if a xLocSetChange is already in progress.
        XLocSetLock lock = master.getXLocSetCoordinator().getXLocSetLock(file, sMan);
        if (lock.isLocked()) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EAGAIN, "xLocSet change already in progress. Please retry.");
        }

        XLocList curXLocList = file.getXLocList();
        String curReplUpdatePolicy = curXLocList.getReplUpdatePolicy();

        // WaRa was renamed to WaR1.
        if (ReplicaUpdatePolicies.REPL_UPDATE_PC_WARA.equals(newReplicaUpdatePolicy)) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                    "Do no longer use the policy WaRa. Instead you're probably looking for the WaR1 policy (write all replicas, read from one)."
                            + newReplicaUpdatePolicy);
        }

        // Check allowed policies.
        if (!ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE.equals(newReplicaUpdatePolicy)
                && !ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(newReplicaUpdatePolicy)
                && !ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(newReplicaUpdatePolicy)
                && !ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ.equals(newReplicaUpdatePolicy))
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                    "invalid replica update policy: " + newReplicaUpdatePolicy);

        // Removing a replicated policy is only allowed if just 1 replica exists.
        if (ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(newReplicaUpdatePolicy)) {
            // if there is more than one replica, report an error
            if (curXLocList.getReplicaCount() > 1)
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                        "number of replicas has to be reduced 1 before replica update policy can be set to "
                                + ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE + " (current replica count = "
                                + curXLocList.getReplicaCount() + ")");
        }

        // Do not allow to switch between read-only and read/write replication
        // as there are currently no mechanisms in place to guarantee that the replicas are synchronized.
        if ((ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(curReplUpdatePolicy)
                && (ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ.equals(newReplicaUpdatePolicy)
                        || ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE.equals(newReplicaUpdatePolicy)))
                || (ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(newReplicaUpdatePolicy)
                        && (ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ.equals(curReplUpdatePolicy)
                                || ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE.equals(curReplUpdatePolicy)))) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                    "Currently, it is not possible to change from a read-only to a read/write replication policy or vise versa.");
        }

        // It is not supported to change a erasure coded files policy
        if ((ReplicaUpdatePolicies.isEC(curReplUpdatePolicy))) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                    "Currently it is not supported to change back from a erasure coding replication");
        }

        // check if striping + rw replication would be set
        StripingPolicy stripingPolicy = file.getXLocList().getReplica(0).getStripingPolicy();
        if (stripingPolicy.getWidth() > 1 && (newReplicaUpdatePolicy.equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE)
                || newReplicaUpdatePolicy.equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ))) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                    "RW-replication of striped files is not supported yet.");
        }

        // Create a new XLoc.
        XLoc[] xLocs = new XLoc[file.getXLocList().getReplicaCount()];
        for (int i = 0; i < file.getXLocList().getReplicaCount(); i++) {
            xLocs[i] = file.getXLocList().getReplica(i);

            // mark the first replica in the list as 'complete' (only relevant for read-only replication)
            if (i == 0 && ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(newReplicaUpdatePolicy)) {
                int replFlags = xLocs[i].getReplicationFlags();

                // Add the default strategy, if the current replication flags miss it.
                if (!ReplicationFlags.containsStrategy(replFlags)) {
                    // FIXME: use the parent directory's default replication policy (see also UpdateFileSizeOperation)
                    ReplicationPolicy defaultReplPolicy = sMan.getDefaultReplicationPolicy(file.getId());
                    if (defaultReplPolicy == null)
                        defaultReplPolicy = sMan.getDefaultReplicationPolicy(1);
                    replFlags = MRCHelper.restoreStrategyFlag(replFlags, defaultReplPolicy);
                }
                
                // Mark the replica as full and complete.
                replFlags = ReplicationFlags.setFullReplica(ReplicationFlags.setReplicaIsComplete(replFlags));
                xLocs[i].setReplicationFlags(replFlags);
            }
        }

        // TODO (jdillmann): Invalidate old xLocSet?!?
        XLocList newXLocList = sMan.createXLocList(xLocs, newReplicaUpdatePolicy, file.getXLocList().getVersion() + 1);

        // Update the X-Locations list.
        file.setXLocList(newXLocList);

        // Set the file as readOnly in case of the read-only replication.
        if (ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(newReplicaUpdatePolicy)) {
            file.setReadOnly(true);
        }

        // Remove read only state of file if readonly policy gets reverted.
        if (ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(curReplUpdatePolicy)
                && ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(newReplicaUpdatePolicy)) {
            file.setReadOnly(false);
        }

        sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
    }

}
