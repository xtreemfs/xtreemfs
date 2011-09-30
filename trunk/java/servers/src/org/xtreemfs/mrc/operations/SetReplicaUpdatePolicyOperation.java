/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.ReplicaUpdatePolicies;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;

import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_set_replica_update_policyRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_set_replica_update_policyResponse;

public class SetReplicaUpdatePolicyOperation extends MRCOperation {

    public SetReplicaUpdatePolicyOperation(MRCRequestDispatcher master) {
        super(master);
    }

    @Override
    public void startRequest(MRCRequest rq) throws Throwable {

        final xtreemfs_set_replica_update_policyRequest rqArgs = (xtreemfs_set_replica_update_policyRequest) rq
                .getRequestArgs();

        final String newReplUpdatePolicy = rqArgs.getUpdatePolicy();

        final FileAccessManager fam = master.getFileAccessManager();
        final VolumeManager vMan = master.getVolumeManager();

        validateContext(rq);

        GlobalFileIdResolver gfr = new GlobalFileIdResolver(rqArgs.getFileId());

        final String volId = gfr.getVolumeId();
        final Long localFileID = gfr.getLocalFileId();

        StorageManager sMan = vMan.getStorageManager(volId);

        FileMetadata file = sMan.getMetadata(localFileID);

        if (file == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file '" + rqArgs.getFileId()
                    + "' does not exist");

        if (file.isDirectory())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM,
                    "replica-update policies may only be set on files");

        // check whether privileged permissions are granted for adding
        // replicas
        fam.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);

        XLocList xlocs = file.getXLocList();

//        if (ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(xlocs.getReplUpdatePolicy()))
//            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
//                    "changing replica update policies of read-only-replicated files is not allowed");

        if (ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(newReplUpdatePolicy)) {
            // if there is more than one replica, report an error
            if (xlocs.getReplicaCount() > 1)
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                        "number of replicas has to be reduced 1 before replica update policy can be set to "
                                + ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE + " (current replica count = "
                                + xlocs.getReplicaCount() + ")");
        }

        if (!ReplicaUpdatePolicies.REPL_UPDATE_PC_WARA.equals(newReplUpdatePolicy)
                && !ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE.equals(newReplUpdatePolicy)
                && !ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(newReplUpdatePolicy)
                && !ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(newReplUpdatePolicy)
                && !ReplicaUpdatePolicies.REPL_UPDATE_PC_WQRQ.equals(newReplUpdatePolicy))
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid replica update policy: "
                    + newReplUpdatePolicy);

        // Get old ReplicaUpdatePolicy for response
        String oldPolicy = file.getXLocList().getReplUpdatePolicy();

        // Set the Xattr value
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);

        XLoc[] xLocs = new XLoc[file.getXLocList().getReplicaCount()];
        for (int i = 0; i < file.getXLocList().getReplicaCount(); i++) {
            xLocs[i] = file.getXLocList().getReplica(i);
        }

        file.setXLocList(sMan.createXLocList(xLocs, rqArgs.getUpdatePolicy(),
                file.getXLocList().getVersion() + 1));

        sMan.setMetadata(file, FileMetadata.RC_METADATA, update);

        rq.setResponse(xtreemfs_set_replica_update_policyResponse.newBuilder()
                .setOldUpdatePolicy(oldPolicy).build());
        
        update.execute();

    }

}
