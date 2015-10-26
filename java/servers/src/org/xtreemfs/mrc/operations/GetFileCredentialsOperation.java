/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.net.InetSocketAddress;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.quota.QuotaConstants;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_get_file_credentialsRequest;

public class GetFileCredentialsOperation extends MRCOperation {

    public GetFileCredentialsOperation(MRCRequestDispatcher master) {
        super(master);
    }

    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        final xtreemfs_get_file_credentialsRequest rqArgs = (xtreemfs_get_file_credentialsRequest) rq
                .getRequestArgs();

        final VolumeManager vMan = master.getVolumeManager();

        final FileAccessManager faMan = master.getFileAccessManager();

        validateContext(rq);

        GlobalFileIdResolver gfr = new GlobalFileIdResolver(rqArgs.getFileId());

        final String volId = gfr.getVolumeId();
        final Long localFileID = gfr.getLocalFileId();

        StorageManager sMan = vMan.getStorageManager(volId);
        VolumeInfo volume = sMan.getVolumeInfo();

        // get file and check if it exist
        FileMetadata file = sMan.getMetadata(localFileID);
        if (file == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file '" + rqArgs.getFileId()
                    + "' does not exist");

        // check whether privileged permissions are granted for starting replication
        faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser,
                rq.getDetails().groupIds);

        boolean enableTracing = volume.isTracingEnabled();
        String traceTarget = volume.getTraceTarget();
        String tracingPolicy = volume.getTracingPolicy();

        // create FileCredentials
        Capability cap = new Capability(MRCHelper.createGlobalFileId(volume, file), FileAccessManager.O_RDONLY,
                master.getConfig().getCapabilityTimeout(), TimeSync.getGlobalTime() / 1000
                        + master.getConfig().getCapabilityTimeout(), ((InetSocketAddress) rq.getRPCRequest()
                        .getSenderAddress()).getAddress().getHostAddress(), file.getEpoch(), false,
                !volume.isSnapshotsEnabled() ? SnapConfig.SNAP_CONFIG_SNAPS_DISABLED
                        : volume.isSnapVolume() ? SnapConfig.SNAP_CONFIG_ACCESS_SNAP
                                : SnapConfig.SNAP_CONFIG_ACCESS_CURRENT, volume.getCreationTime(), enableTracing,
                traceTarget, tracingPolicy, QuotaConstants.UNLIMITED_VOUCHER, 0L, master.getConfig().getCapabilitySecret());

        // build new XlocSet with readonlyFileSize set. Necessary to check if replication is complete.
        XLocSet newXlocSet = null;
        if (file.isReadOnly()) {
            newXlocSet = Converter.xLocListToXLocSet(file.getXLocList()).setReadOnlyFileSize(file.getSize())
                    .build();
        } else {
            newXlocSet = Converter.xLocListToXLocSet(file.getXLocList()).build();
        }

        FileCredentials fc = FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(newXlocSet).build();

        rq.setResponse(fc);
        finishRequest(rq);
    }
}
