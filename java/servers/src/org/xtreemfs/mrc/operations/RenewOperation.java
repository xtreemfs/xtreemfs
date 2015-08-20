/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_renew_capabilityRequest;

/**
 * 
 * @author stender
 */
public class RenewOperation extends MRCOperation {
    
    public final boolean renewTimedOutCaps;
    
    public RenewOperation(MRCRequestDispatcher master) {
        super(master);
        renewTimedOutCaps = master.getConfig().isRenewTimedOutCaps();
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final xtreemfs_renew_capabilityRequest renewCapabilityRequest = (xtreemfs_renew_capabilityRequest) rq
                .getRequestArgs();
        
        // create a capability object to verify the capability
        Capability cap = new Capability(renewCapabilityRequest.getXcap(), master.getConfig().getCapabilitySecret());
        
        // check whether the capability has a valid signature
        if (!cap.hasValidSignature())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, cap + " does not have a valid signature");
        
        // check whether the capability has expired
        if (cap.hasExpired() && !renewTimedOutCaps)
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, cap + " has expired");
        
        // get new voucher, if capability are still valid for vouchers
        long newExpireMs = TimeSync.getGlobalTime() + master.getConfig().getCapabilityTimeout() * 1000;
        long voucherSize = cap.getVoucherSize();
        if (renewCapabilityRequest.getIncreaseVoucher()) {
            voucherSize = master.getMrcVoucherManager().checkAndRenewVoucher(cap.getFileId(), cap.getClientIdentity(),
                    cap.getExpireMs(), newExpireMs);
        }

        Capability newCap = new Capability(cap.getFileId(), cap.getAccessMode(), master.getConfig()
                .getCapabilityTimeout(), TimeSync.getGlobalTime() / 1000 + master.getConfig().getCapabilityTimeout(),
                cap.getClientIdentity(), cap.getEpochNo(), cap.isReplicateOnClose(), cap.getSnapConfig(),
                cap.getSnapTimestamp(), voucherSize, newExpireMs, master.getConfig().getCapabilitySecret());

        // set the response
        rq.setResponse(newCap.getXCap());
        finishRequest(rq);
    }

}
