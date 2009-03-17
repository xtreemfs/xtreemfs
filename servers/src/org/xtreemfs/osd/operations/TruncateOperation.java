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

import java.util.List;
import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Exceptions.OSDException;
import org.xtreemfs.interfaces.OSDInterface.truncateRequest;
import org.xtreemfs.interfaces.OSDInterface.truncateResponse;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.interfaces.utils.Serializable;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.StorageStage.TruncateCallback;

public final class TruncateOperation extends OSDOperation {

    final int procId;
    final String sharedSecret;
    final ServiceUUID localUUID;

    public TruncateOperation(OSDRequestDispatcher master) {
        super(master);
        truncateRequest rq = new truncateRequest();
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
        final truncateRequest args = (truncateRequest)rq.getRequestArgs();
        System.out.println("rq: "+args);

        if (args.getNew_file_size() < 0) {
            rq.sendException(new OSDException(ErrorCodes.INVALID_PARAMS, "new_file_size for truncate must be >= 0", ""));
            return;
        }

        if (!rq.getLocationList().getLocalReplica().isHeadOsd(localUUID)) {
            rq.sendException(new OSDException(ErrorCodes.INVALID_PARAMS, "truncate must be executed at the head OSD (first OSD in replica)", ""));
            return;
        }

        master.getStorageStage().truncate(args.getFile_id(), args.getNew_file_size(),
                rq.getLocationList().getLocalReplica().getStripingPolicy(),
                rq.getLocationList().getLocalReplica(), rq.getCapability().getEpochNo(),
                rq, new TruncateCallback() {

            @Override
            public void truncateComplete(OSDWriteResponse result, Exception error) {
                step2(rq, args, result, error);
            }
        });
    }

    public void step2(final OSDRequest rq, final truncateRequest args, OSDWriteResponse result, Exception error) {

        if (error != null) {
            if (error instanceof ONCRPCException)
                rq.sendException((ONCRPCException)error);
            else
                rq.sendInternalServerError(error);
        } else {
            //check for striping
            if (rq.getLocationList().getLocalReplica().isStriped()) {
                //disseminate internal truncate to all other OSDs
                disseminateTruncates(rq,args,result);
            } else {
                //non-striped
                sendResponse(rq, result);
            }
        }
    }

    private void disseminateTruncates(final OSDRequest rq, final truncateRequest args, final OSDWriteResponse result) {
        try {
            final List<ServiceUUID> osds = rq.getLocationList().getLocalReplica().getOSDs();
            final RPCResponse[] gmaxRPCs = new RPCResponse[osds.size() - 1];
            int cnt = 0;
            for (ServiceUUID osd : osds) {
                if (!osd.equals(localUUID)) {
                    gmaxRPCs[cnt++] = master.getOSDClient().internal_truncate(osd.getAddress(),
                            args.getFile_id(), args.getCredentials(),args.getNew_file_size());
                }
            }
            this.waitForResponses(gmaxRPCs, new ResponsesListener() {

                @Override
                public void responsesAvailable() {
                    analyzeTruncateResponses(rq,result,gmaxRPCs);
                }

            });
        } catch (UnknownUUIDException ex) {
            rq.sendInternalServerError(ex);
            return;
        }
    }

    private void analyzeTruncateResponses(OSDRequest rq, OSDWriteResponse result, RPCResponse[] gmaxRPCs) {
        //analyze results
        try {
            for (int i = 0; i < gmaxRPCs.length; i++) {
                gmaxRPCs[i].get();
            }
            sendResponse(rq, result);
        } catch (Exception ex) {
            rq.sendInternalServerError(ex);
        } finally {
            for (RPCResponse r : gmaxRPCs)
                r.freeBuffers();
        }
    }

    public void sendResponse(OSDRequest rq, OSDWriteResponse result) {
        truncateResponse response = new truncateResponse(result);
        rq.sendSuccess(response);
    }

    @Override
    public Serializable parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        truncateRequest rpcrq = new truncateRequest();
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

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    

}