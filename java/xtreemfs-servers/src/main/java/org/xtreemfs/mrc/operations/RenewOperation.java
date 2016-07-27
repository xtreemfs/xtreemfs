/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.quota.QuotaConstants;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;

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
        
        final XCap xcap = (XCap) rq.getRequestArgs();
        
        // perform master redirect if necessary due to DB operation
        if (master.getReplMasterUUID() != null
                && !master.getReplMasterUUID().equals(master.getConfig().getUUID().toString()))
            throw new DatabaseException(ExceptionType.REDIRECT);

        // create a capability object to verify the capability
        Capability cap = new Capability(xcap, master.getConfig().getCapabilitySecret());
        
        // check whether the capability has a valid signature
        if (!cap.hasValidSignature())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, cap + " does not have a valid signature");
        
        // check whether the capability has expired
        if (cap.hasExpired() && !renewTimedOutCaps)
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, cap + " has expired");
        
        Capability newCap = new Capability(cap.getFileId(), cap.getAccessMode(), master.getConfig()
                .getCapabilityTimeout(), TimeSync.getGlobalTime() / 1000 + master.getConfig().getCapabilityTimeout(),
                cap.getClientIdentity(), cap.getEpochNo(), cap.isReplicateOnClose(), cap.getSnapConfig(),
                cap.getSnapTimestamp(), cap.getTraceConfig().getTraceRequests(), cap.getTraceConfig().getTracingPolicy(),
                cap.getTraceConfig().getTracingPolicyConfig(), cap.getVoucherSize(), 0L,
                master.getConfig().getCapabilitySecret());
        
        // set the response
        rq.setResponse(newCap.getXCap());
        finishRequest(rq);
    }
}
