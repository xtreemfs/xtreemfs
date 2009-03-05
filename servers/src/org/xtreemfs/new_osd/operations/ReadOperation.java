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

package org.xtreemfs.new_osd.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.interfaces.OSDInterface.readRequest;
import org.xtreemfs.interfaces.OSDInterface.readResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.interfaces.utils.Serializable;
import org.xtreemfs.new_osd.OSDRequest;
import org.xtreemfs.new_osd.OSDRequestDispatcher;
import org.xtreemfs.new_osd.stages.StorageStage.ReadObjectCallback;
import org.xtreemfs.new_osd.storage.ObjectInformation;

public final class ReadOperation extends OSDOperation {

    final int procId;
    final String sharedSecret;
    final ServiceUUID localUUID;

    public ReadOperation(OSDRequestDispatcher master) {
        super(master);
        readRequest rq = new readRequest();
        procId = rq.getOperationNumber();
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return procId;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final readRequest args = (readRequest)rq.getRequestArgs();
        master.getStorageStage().readObject(args.getFile_id(), args.getObject_number(), rq.getLocationList().getLocalReplica().getStripingPolicy(), args.getOffset(), args.getLength(), rq, new ReadObjectCallback() {

            @Override
            public void readComplete(ObjectInformation result, Exception error) {
                step2(rq, args, result, error);
            }
        });
    }

    public void step2(final OSDRequest rq, readRequest args, ObjectInformation result, Exception error) {

        if (error != null) {
            if (error instanceof ONCRPCException)
                rq.sendException((ONCRPCException)error);
            else
                rq.sendInternalServerError(error);
        } else {
            ObjectData data;
            final boolean isLastObject = result.getLastLocalObjectNo()==args.getObject_number();
            final boolean isRangeRequested = (args.getOffset() > 0) || (args.getLength() < result.getStripeSize());
            if (isRangeRequested) {
                data = result.getObjectData(isLastObject, args.getOffset(), args.getLength());
            } else {
                 data = result.getObjectData(isLastObject);
            }
            sendResponse(rq, data);
        }

    }

    public void sendResponse(OSDRequest rq, ObjectData result) {
        readResponse response = new readResponse(result);
        rq.sendSuccess(response);
    }

    @Override
    public Serializable parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        readRequest rpcrq = new readRequest();
        rpcrq.deserialize(data);

        rq.setFileId(rpcrq.getFile_id());
        rq.setCapability(new Capability(rpcrq.getCredentials().getXcap(),sharedSecret));
        rq.setLocationList(new XLocations(rpcrq.getCredentials().getXlocs(), localUUID));

        return rpcrq;
    }

    @Override
    public boolean requiresCapability() {
        return true;
    }

    

}