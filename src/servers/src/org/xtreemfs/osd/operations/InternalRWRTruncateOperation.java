/*  Copyright (c) 2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCException;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.interfaces.OSDInterface.RedirectException;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_rwr_truncateRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_rwr_truncateResponse;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.osd.stages.StorageStage.TruncateCallback;

public final class InternalRWRTruncateOperation extends OSDOperation {

    final int procId;
    final String sharedSecret;
    final ServiceUUID localUUID;

    public InternalRWRTruncateOperation(OSDRequestDispatcher master) {
        super(master);
        procId = xtreemfs_rwr_truncateRequest.TAG;
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return procId;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_rwr_truncateRequest args = (xtreemfs_rwr_truncateRequest)rq.getRequestArgs();

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"RWR truncate for file %s objVer %d",args.getFile_id(),
                    args.getObject_version());
        }

       prepareLocalTruncate(rq,args);
    }
    
    public void localTruncate(final OSDRequest rq, final xtreemfs_rwr_truncateRequest args) {
         if (args.getNew_file_size() < 0) {
            rq.sendException(new OSDException(ErrorCodes.INVALID_PARAMS, "new_file_size for truncate must be >= 0", ""));
            return;
        }

        master.getStorageStage().truncate(args.getFile_id(), args.getNew_file_size(),
            rq.getLocationList().getLocalReplica().getStripingPolicy(),
            rq.getLocationList().getLocalReplica(), rq.getCapability().getEpochNo(), rq.getCowPolicy(),
            args.getObject_version(), rq,
            new TruncateCallback() {

            @Override
            public void truncateComplete(OSDWriteResponse result, Exception error) {
                sendResult(rq, error);
            }
        });
    }

    public void prepareLocalTruncate(final OSDRequest rq, final xtreemfs_rwr_truncateRequest args) {
        master.getRWReplicationStage().prepareOperation(args.getFile_credentials(), rq.getLocationList(), 0, args.getObject_version(),
                RWReplicationStage.Operation.INTERNAL_TRUNCATE, new RWReplicationStage.RWReplicationCallback() {

            @Override
            public void success(long newObjectVersion) {
                localTruncate(rq, args);
            }

            @Override
            public void redirect(RedirectException redirectTo) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void failed(Exception ex) {
                sendResult(rq,ex);
            }
        }, rq);
    }


    public void sendResult(final OSDRequest rq, Exception error) {

        if (error != null) {
            if (error instanceof ONCRPCException)
                rq.sendException((ONCRPCException)error);
            else
                rq.sendInternalServerError(error);
        } else {
            //only locally
            sendResponse(rq);
        }
    }

    
    public void sendResponse(OSDRequest rq) {
        xtreemfs_rwr_truncateResponse response = new xtreemfs_rwr_truncateResponse();
        rq.sendSuccess(response);
    }

    @Override
    public yidl.runtime.Object parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        xtreemfs_rwr_truncateRequest rpcrq = new xtreemfs_rwr_truncateRequest();
        rpcrq.unmarshal(new XDRUnmarshaller(data));

        rq.setFileId(rpcrq.getFile_id());
        rq.setCapability(new Capability(rpcrq.getFile_credentials().getXcap(),sharedSecret));
        rq.setLocationList(new XLocations(rpcrq.getFile_credentials().getXlocs(), localUUID));

        return rpcrq;
    }

    @Override
    public boolean requiresCapability() {
        return true;
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    

}