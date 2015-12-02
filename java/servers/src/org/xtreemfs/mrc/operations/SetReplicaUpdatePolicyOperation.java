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
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
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

            fileId = MRCHelper.createGlobalFileId(sMan.getVolumeInfo(), file);

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
        // the admin password accounts for priviledged access
        faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser || privileged,
                rq.getDetails().groupIds);

        // Check if a xLocSetChange is already in progress.
        XLocSetLock lock = master.getXLocSetCoordinator().getXLocSetLock(file, sMan);
        if (lock.isLocked()) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EAGAIN, "xLocSet change already in progress. Please retry.");
        }

        XLocList curXLocList = file.getXLocList();
        String curReplUpdatePolicy = curXLocList.getReplUpdatePolicy();

        // WaRa was renamed to WaR1.
        if (ReplicaUpdatePolicies.REPL_UPDATE_PC_WARA.equals(newReplUpdatePolicy)) {
            throw new UserException(
                    POSIXErrno.POSIX_ERROR_EINVAL,
                    "Do no longer use the policy WaRa. Instead you're probably looking for the WaR1 policy (write all replicas, read from one)."
                            + newReplUpdatePolicy);
        }

        // Check allowed policies.
        if (!ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE.equals(newReplUpdatePolicy)
                && !ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(newReplUpdatePolicy)
                && !ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(newReplUpdatePolicy)
                && !ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ.equals(newReplUpdatePolicy))
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid replica update policy: "
                    + newReplUpdatePolicy);

        // Removing a replicated policy is only allowed if just 1 replica exists.
        if (ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(newReplUpdatePolicy)) {
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
                && (ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ.equals(newReplUpdatePolicy)
                        || ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE.equals(newReplUpdatePolicy)))
                || (ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(newReplUpdatePolicy)
                        && (ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ.equals(curReplUpdatePolicy)
                                || ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE.equals(curReplUpdatePolicy)))) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                    "Currently, it is not possible to change from a read-only to a read/write replication policy or vise versa.");
        }

        // check if striping + rw replication would be set
        StripingPolicy stripingPolicy = file.getXLocList().getReplica(0).getStripingPolicy();
        if (stripingPolicy.getWidth() > 1
                && (newReplUpdatePolicy.equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE) || newReplUpdatePolicy
                        .equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ))) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "RW-replication of striped files is not supported yet.");
        }

        // Create a new XLoc.
        XLoc[] xLocs = new XLoc[file.getXLocList().getReplicaCount()];
        for (int i = 0; i < file.getXLocList().getReplicaCount(); i++) {
            xLocs[i] = file.getXLocList().getReplica(i);

            // mark the first replica in the list as 'complete' (only relevant for read-only replication)
            if (i == 0 && ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(newReplUpdatePolicy)) {
                int replFlags = ReplicationFlags
                        .setFullReplica(ReplicationFlags.setReplicaIsComplete(xLocs[i].getReplicationFlags()));
                xLocs[i].setReplicationFlags(replFlags);
            }
        }

        // TODO (jdillmann): Invalidate old xLocSet?!?
        XLocList newXLocList = sMan.createXLocList(xLocs, newReplUpdatePolicy, file.getXLocList().getVersion() + 1);

        // Update the X-Locations list.
        file.setXLocList(newXLocList);

        // Set the file as readOnly in case of the read-only replication.
        if (ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(newReplUpdatePolicy)) {
            file.setReadOnly(true);
        }

        // Remove read only state of file if readonly policy gets reverted.
        if (ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(curReplUpdatePolicy)
                && ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(newReplUpdatePolicy)) {
            file.setReadOnly(false);
        }

        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        sMan.setMetadata(file, FileMetadata.RC_METADATA, update);

        // Set the response.
        rq.setResponse(emptyResponse.getDefaultInstance());

        update.execute();

        // TODO (jdillmann): Make async like AddReplica etc?
    }

}
