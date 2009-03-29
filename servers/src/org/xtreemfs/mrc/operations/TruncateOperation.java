/*  Copyright (c) 2008,2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Bjoern Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.mrc.operations;

import java.net.InetSocketAddress;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.interfaces.MRCInterface.ftruncateRequest;
import org.xtreemfs.interfaces.MRCInterface.ftruncateResponse;
import org.xtreemfs.mrc.ErrNo;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;

/**
 * 
 * @author stender
 */
public class TruncateOperation extends MRCOperation {
    
    public static final int OP_ID = 30;
    
    public TruncateOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        
        try {
            
            final ftruncateRequest rqArgs = (ftruncateRequest) rq.getRequestArgs();
            
            Capability writeCap = new Capability(rqArgs.getWrite_xcap(), master.getConfig()
                    .getCapabilitySecret());
            
            // check whether the capability has a valid signature
            if (!writeCap.hasValidSignature())
                throw new UserException(writeCap + " does not have a valid signature");
            
            // check whether the capability has expired
            if (writeCap.hasExpired())
                throw new UserException(writeCap + " has expired");
            
            // parse volume and file ID from global file ID
            long fileId = 0;
            String volumeId = null;
            try {
                String globalFileId = writeCap.getFileId();
                int i = globalFileId.indexOf(':');
                volumeId = globalFileId.substring(0, i);
                fileId = Long.parseLong(globalFileId.substring(i + 1));
            } catch (Exception exc) {
                throw new UserException("invalid global file ID: " + writeCap.getFileId()
                    + "; expected pattern: <volume_ID>:<local_file_ID>");
            }
            StorageManager sMan = master.getVolumeManager().getStorageManager(volumeId);
            
            FileMetadata file = sMan.getMetadata(fileId);
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file '" + fileId + "' does not exist");
            
            // get the current epoch, use (and increase) the truncate number if
            // the open mode is truncate
            
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
            
            int newEpoch = file.getIssuedEpoch() + 1;
            file.setIssuedEpoch(newEpoch);
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            
            Capability truncCap = new Capability(volumeId + ":" + file.getId(), writeCap.getAccessMode()
                | FileAccessManager.O_TRUNC, TimeSync.getGlobalTime() / 1000 + Capability.DEFAULT_VALIDITY,
                ((InetSocketAddress) rq.getRPCRequest().getClientIdentity()).getAddress().getHostAddress(),
                newEpoch, master.getConfig().getCapabilitySecret());
                       
            // set the response
            rq.setResponse(new ftruncateResponse(truncCap.getXCap()));
            update.execute();
            
        } catch (UserException exc) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc.getMessage(),
                exc));
        } catch (Throwable exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, "an error has occurred", exc));
        }
    }
    
}
