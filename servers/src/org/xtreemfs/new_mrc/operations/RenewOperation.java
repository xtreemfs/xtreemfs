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

package org.xtreemfs.new_mrc.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.new_mrc.ErrNo;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.UserException;
import org.xtreemfs.new_mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.new_mrc.database.AtomicDBUpdate;
import org.xtreemfs.new_mrc.database.StorageManager;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.new_mrc.utils.MRCOpHelper;

/**
 * 
 * @author stender
 */
public class RenewOperation extends MRCOperation {

    public static final String RPC_NAME = "renew";

    public RenewOperation(MRCRequestDispatcher master) {
        super(master);
    }

    @Override
    public boolean hasArguments() {
        return false;
    }

    @Override
    public boolean isAuthRequired() {
        return true;
    }

    @Override
    public void startRequest(MRCRequest rq) {

        try {

            String capString = rq.getPinkyRequest().requestHeaders.getHeader(HTTPHeaders.HDR_XCAPABILITY);
            String newSizeString = rq.getPinkyRequest().requestHeaders
                    .getHeader(HTTPHeaders.HDR_XNEWFILESIZE);

            if (capString == null)
                throw new UserException("missing " + HTTPHeaders.HDR_XCAPABILITY + " header");

            // create a capability object to verify the capability
            Capability cap = new Capability(capString, master.getConfig().getCapabilitySecret());

            // check whether the received capability has a valid signature
            if (!cap.isValid())
                throw new UserException(capString + " is invalid");

            Capability newCap = new Capability(cap.getFileId(), cap.getAccessMode(), cap.getEpochNo(), master
                    .getConfig().getCapabilitySecret());

            HTTPHeaders headers = MRCOpHelper.createXCapHeaders(newCap.toString(), null);
            rq.setAdditionalResponseHeaders(headers);

            if (newSizeString != null) {
                if (Logging.isDebug())
                    Logging
                            .logMessage(Logging.LEVEL_DEBUG, this,
                                    "received 'X-New-Filesize' header w/ 'renew' operation. 'updateFileSize' should be used instead of 'renew'");

                // parse volume and file ID from global file ID
                String globalFileId = cap.getFileId();
                int i = globalFileId.indexOf(':');
                String volumeId = cap.getFileId().substring(0, i);
                long fileId = Long.parseLong(cap.getFileId().substring(i + 1));
                StorageManager sMan = master.getVolumeManager().getStorageManager(volumeId);

                FileMetadata file = sMan.getMetadata(fileId);
                if (file == null)
                    throw new UserException(ErrNo.ENOENT, "file '" + fileId + "' does not exist");

                int index = newSizeString.indexOf(',');
                if (index == -1)
                    throw new UserException(ErrNo.EINVAL, "invalid " + HTTPHeaders.HDR_XNEWFILESIZE
                            + " header");

                // parse the file size and epoch number
                long newFileSize = Long.parseLong(newSizeString.substring(1, index));
                int epochNo = Integer
                        .parseInt(newSizeString.substring(index + 1, newSizeString.length() - 1));

                // FIXME: this line is needed due to a BUG in the client which
                // expects some useless return value
                rq.setData(ReusableBuffer.wrap(JSONParser.writeJSON(null).getBytes()));

                // discard outdated file size updates
                if (epochNo < file.getEpoch()) {
                    finishRequest(rq);
                    return;
                }

                // accept any file size in a new epoch but only larger file
                // sizes in the current epoch
                if (epochNo > file.getEpoch() || newFileSize > file.getSize()) {

                    file.setSize(newFileSize);
                    file.setEpoch(epochNo);

                    AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
                    sMan.setMetadata(file, FileMetadata.FC_METADATA, update);

                    // TODO: update POSIX time stamps
                    // // update POSIX timestamps
                    // MRCOpHelper.updateFileTimes(parentId, file,
                    // !master.getConfig().isNoAtime(),
                    // false, true, sMan, update);

                    update.execute();
                } else
                    finishRequest(rq);
            } else
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
