/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

 This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
 Grid Operating System, see <http://www.xtreemos.eu> for more details.
 The XtreemOS project has been developed with the financial support of the
 European Commission's IST program under contract #FP6-033576.

 XtreemFS is free software: you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 2 of the License, or (at your option)
 any later version.

 XtreemFS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_renew_capabilityRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_renew_capabilityResponse;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;

/**
 * 
 * @author stender
 */
public class RenewOperation extends MRCOperation {
    
    public static final int OP_ID = 25;
    
    public RenewOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        
        try {
            
            final xtreemfs_renew_capabilityRequest rqArgs = (xtreemfs_renew_capabilityRequest) rq
                    .getRequestArgs();
            
            // create a capability object to verify the capability
            Capability cap = new Capability(rqArgs.getOld_xcap(), master.getConfig().getCapabilitySecret());
            
            // check whether the capability has a valid signature
            if (!cap.hasValidSignature())
                throw new UserException(cap + " does not have a valid signature");
            
            // check whether the capability has expired
            if (cap.hasExpired())
                throw new UserException(cap + " has expired");
            
            Capability newCap = new Capability(cap.getFileId(), cap.getAccessMode(), TimeSync.getGlobalTime()
                / 1000 + Capability.DEFAULT_VALIDITY, cap.getClientIdentity(), cap.getEpochNo(), master
                    .getConfig().getCapabilitySecret());
            
            // set the response
            rq.setResponse(new xtreemfs_renew_capabilityResponse(newCap.getXCap()));
            finishRequest(rq);
            
        } catch (UserException exc) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc.getMessage(),
                exc));
        } catch (Exception exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, "an error has occurred", exc));
        }
    }
}
