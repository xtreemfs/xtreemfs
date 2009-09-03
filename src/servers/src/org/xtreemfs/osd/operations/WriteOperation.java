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
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.interfaces.OSDInterface.writeRequest;
import org.xtreemfs.interfaces.OSDInterface.writeResponse;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.StorageStage.WriteObjectCallback;

public final class WriteOperation extends OSDOperation {

    final int procId;
    final String sharedSecret;
    final ServiceUUID localUUID;

    public WriteOperation(OSDRequestDispatcher master) {
        super(master);
        procId = writeRequest.TAG;
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return procId;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final writeRequest args = (writeRequest)rq.getRequestArgs();

        if (args.getObject_number() < 0) {
            rq.sendException(new OSDException(ErrorCodes.INVALID_PARAMS, "object number must be >= 0", ""));
            return;
        }

        if (args.getOffset() < 0) {
            rq.sendException(new OSDException(ErrorCodes.INVALID_PARAMS, "offset must be >= 0", ""));
            return;
        }

        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

        if (args.getOffset() >= sp.getStripeSizeForObject(args.getObject_number())) {
            rq.sendOSDException(ErrorCodes.INVALID_PARAMS, "offset must be < stripe size");
            return;
        }
        
        if (rq.getLocationList().getReplicaUpdatePolicy().equals(Constants.REPL_UPDATE_PC_RONLY)) {
            // file is read only
            rq.sendException(new OSDException(ErrorCodes.FILE_IS_READ_ONLY,
                    "Cannot write on read-only files.", ""));
        } else {

            boolean syncWrite = (rq.getCapability().getAccessMode() & Constants.SYSTEM_V_FCNTL_H_O_SYNC) > 0;


            master.objectReceived();
            master.dataReceived(args.getObject_data().getData().capacity());

            master.getStorageStage().writeObject(args.getFile_id(), args.getObject_number(), sp,
                    args.getOffset(), args.getObject_data().getData(), rq.getCowPolicy(),
                    rq.getLocationList(), syncWrite, rq, new WriteObjectCallback() {

                        @Override
                        public void writeComplete(OSDWriteResponse result, Exception error) {
                            step2(rq, result, error);
                        }
                    });
        }
    }

    public void step2(final OSDRequest rq, OSDWriteResponse result, Exception error) {
        if (error != null) {
            if (error instanceof ONCRPCException)
                rq.sendException((ONCRPCException)error);
            else
                rq.sendInternalServerError(error);
        } else {
            sendResponse(rq, result);
        }

    }

    public void sendResponse(OSDRequest rq, OSDWriteResponse result) {
        writeResponse response = new writeResponse(result);
        rq.sendSuccess(response);
    }

    @Override
    public yidl.Object parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        writeRequest rpcrq = new writeRequest();
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