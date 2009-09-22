/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.InternalGmax;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_get_gmaxRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_get_gmaxResponse;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.StorageStage.InternalGetGmaxCallback;

public final class InternalGetGmaxOperation extends OSDOperation {

    final int procId;
    final String sharedSecret;
    final ServiceUUID localUUID;

    public InternalGetGmaxOperation(OSDRequestDispatcher master) {
        super(master);;
        procId = xtreemfs_internal_get_gmaxRequest.TAG;
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return procId;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_internal_get_gmaxRequest args = (xtreemfs_internal_get_gmaxRequest)rq.getRequestArgs();
        master.getStorageStage().internalGetGmax(args.getFile_id(),
                rq.getLocationList().getLocalReplica().getStripingPolicy(),
                rq, new InternalGetGmaxCallback() {

            @Override
            public void gmaxComplete(InternalGmax result, Exception error) {
                if (error != null) {
                    if (error instanceof ONCRPCException)
                        rq.sendException((ONCRPCException)error);
                    else
                        rq.sendInternalServerError(error);
                } else
                    sendResponse(rq, result);
            }
        });
    }


    public void sendResponse(OSDRequest rq, InternalGmax result) {
        xtreemfs_internal_get_gmaxResponse response = new xtreemfs_internal_get_gmaxResponse(result);
        rq.sendSuccess(response);
    }

    @Override
    public yidl.runtime.Object parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        xtreemfs_internal_get_gmaxRequest rpcrq = new xtreemfs_internal_get_gmaxRequest();
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