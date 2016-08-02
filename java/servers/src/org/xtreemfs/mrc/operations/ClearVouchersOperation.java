/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.operations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.quota.FinalizeVoucherResponseHelper;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.quota.QuotaFileInformation;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDFinalizeVouchersResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_clear_vouchersRequest;

/**
 * Operation to handle a clear voucher request to free voucher with the finalize responses of the osds.
 */
public class ClearVouchersOperation extends MRCOperation {

    public ClearVouchersOperation(MRCRequestDispatcher master) {
        super(master);

    }

    @Override
    public void startRequest(MRCRequest rq) throws Throwable {

        // perform master redirect if necessary
        if (master.getReplMasterUUID() != null
                && !master.getReplMasterUUID().equals(master.getConfig().getUUID().toString()))
            throw new DatabaseException(ExceptionType.REDIRECT);

        final xtreemfs_clear_vouchersRequest cvRequest = (xtreemfs_clear_vouchersRequest) rq.getRequestArgs();

        Capability cap = new Capability(cvRequest.getCreds().getXcap(), master.getConfig().getCapabilitySecret());
        Set<Long> expireTimeSet = new HashSet<Long>(cvRequest.getExpireTimeMsList());
        List<OSDFinalizeVouchersResponse> osdFinalizeVouchersResponseList = cvRequest
                .getOsdFinalizeVouchersResponseList();

        // check whether the capability has a valid signature
        if (!cap.hasValidSignature())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, cap + " does not have a valid signature");

        // check whether the capability has expired
        if (cap.hasExpired())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, cap + " has expired");

        // Striping with active replication isn't supported, so the policy should be the same on all replicas
        StripingPolicy sp = cvRequest.getCreds().getXlocs().getReplicasList().get(0).getStripingPolicy();

        if (osdFinalizeVouchersResponseList.size() != (sp.getWidth() + sp.getParityWidth())) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, cvRequest
                    + " has a mismach for the width of the striping policy and the OSDFinalizeVouchersRespone count");
        }

        // add expire time of xcap
        expireTimeSet.add(cap.getExpireMs());

        FinalizeVoucherResponseHelper responseHelper = new FinalizeVoucherResponseHelper(master.getConfig()
                .getCapabilitySecret());

        long newFileSizeMax = -1;
        long truncateEpoch = -1;
        for (OSDFinalizeVouchersResponse osdFinalizeVouchersResponse : osdFinalizeVouchersResponseList) {
            boolean valid = responseHelper.validateSignature(osdFinalizeVouchersResponse, expireTimeSet);
            if (!valid) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, osdFinalizeVouchersResponse
                        + " does not have a valid signature");
            }

            if (truncateEpoch == -1) {
                osdFinalizeVouchersResponse.getTruncateEpoch();
            } else if (truncateEpoch != osdFinalizeVouchersResponse.getTruncateEpoch()) {
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, osdFinalizeVouchersResponse
                        + " does not match the truncateEpoch of other responses:" + truncateEpoch);
            }

            if (osdFinalizeVouchersResponse.getSizeInBytes() > newFileSizeMax) {
                newFileSizeMax = osdFinalizeVouchersResponse.getSizeInBytes();
            }
        }

        if (newFileSizeMax == -1) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                    "The OSDFinalizeVouchersResponse(s) did not contain a valid filesize or valid truncate epoch.");
        }

        GlobalFileIdResolver globalFileIdResolver = new GlobalFileIdResolver(cap.getFileId());
        StorageManager sMan = master.getVolumeManager().getStorageManager(globalFileIdResolver.getVolumeId());
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);

        FileMetadata metadata = sMan.getMetadata(globalFileIdResolver.getLocalFileId());

        // check for deleted file
        if (metadata != null) {
            QuotaFileInformation quotaFileInformation = new QuotaFileInformation(globalFileIdResolver.getVolumeId(),
                    metadata);

            master.getMrcVoucherManager().clearVouchers(quotaFileInformation, cap.getClientIdentity(), expireTimeSet,
                    newFileSizeMax, update);
        }

        // TODO(baerhold): update file size for storage

        rq.setResponse(emptyResponse.getDefaultInstance());
        update.execute();
    }
}
