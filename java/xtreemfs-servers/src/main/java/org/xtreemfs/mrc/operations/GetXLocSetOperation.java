/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.operations;

import java.net.InetSocketAddress;
import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replicas;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_get_xlocsetRequest;

/**
 * Returns the current XLocSet for the file specified in the request.<br>
 * This is different to {@link GetXLocListOperation} which returns a list of {@link Replicas}.
 */
public class GetXLocSetOperation extends MRCOperation {

    public GetXLocSetOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {

        final xtreemfs_get_xlocsetRequest rqArgs = (xtreemfs_get_xlocsetRequest) rq.getRequestArgs();

        final FileAccessManager faMan = master.getFileAccessManager();
        final VolumeManager vMan = master.getVolumeManager();
        final VolumeInfo volume;

        StorageManager sMan = null;
        FileMetadata file = null;

        if (rqArgs.hasXcap()) {
            // Create a capability object to verify the capability.
            Capability cap = new Capability(rqArgs.getXcap(), master.getConfig().getCapabilitySecret());

            // Check whether the capability has a valid signature.
            if (!cap.hasValidSignature()) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, cap + " does not have a valid signature");
            }

            // Check whether the capability has expired.
            if (cap.hasExpired()) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, cap + " has expired");
            }
        } else {
            validateContext(rq);
        }


        // Retrieve the FileMetadata.
        if (rqArgs.hasXcap() || rqArgs.hasFileId()) {
            String fileId = rqArgs.hasFileId() ? rqArgs.getFileId() : rqArgs.getXcap().getFileId();

            // Parse volume and file ID from global file ID.
            GlobalFileIdResolver idRes = new GlobalFileIdResolver(fileId);

            sMan = vMan.getStorageManager(idRes.getVolumeId());

            // Retrieve the file metadata.
            file = sMan.getMetadata(idRes.getLocalFileId());
            if (file == null) {
                throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file '" + idRes.getLocalFileId()
                        + "' does not exist");
            }
            volume = sMan.getVolumeInfo();

        } else if (rqArgs.hasVolumeName() && rqArgs.hasPath()) {

            final Path p = new Path(rqArgs.getVolumeName(), rqArgs.getPath());

            sMan = vMan.getStorageManagerByName(p.getComp(0));
            final PathResolver res = new PathResolver(sMan, p);

            res.checkIfFileDoesNotExist();
            file = res.getFile();

            // Check whether the path prefix is searchable.
            faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser,
                    rq.getDetails().groupIds);
            volume = sMan.getVolumeInfo();

        } else {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                    "Either file ID, volume name + path or a valid XCap is required");
        }

        if (file.isDirectory()) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EISDIR, file.getId() + " is a directory");
        }

        if (sMan.getSoftlinkTarget(file.getId()) != null) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "file '" + rqArgs.getFileId()
                    + "' is a symbolic link");
        }

        // Get the XLocSet from the XLocList.
        XLocList xLocList = file.getXLocList();
        assert (xLocList != null);
        XLocSet.Builder xLocSetBuilder = Converter.xLocListToXLocSet(xLocList);

        String path = null;
        if (rqArgs.hasVolumeName() && rqArgs.hasPath()) {
            path = new Path(rqArgs.getVolumeName(), rqArgs.getPath()).toString();
        }
        
        // Sort the XLocSet according to the policy.
        List<Replica> sortedReplList = master.getOSDStatusManager()
                .getSortedReplicaList(volume.getId(),
                        ((InetSocketAddress) rq.getRPCRequest().getSenderAddress()).getAddress(),
                        rqArgs.getCoordinates(), xLocSetBuilder.getReplicasList(), xLocList, path)
                .getReplicasList();
        xLocSetBuilder.clearReplicas();
        xLocSetBuilder.addAllReplicas(sortedReplList);
        xLocSetBuilder.setReadOnlyFileSize(file.getSize());

        // Set the response.
        rq.setResponse(xLocSetBuilder.build());
        finishRequest(rq);
    }

}
