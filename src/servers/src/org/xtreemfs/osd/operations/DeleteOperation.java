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
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.OSDInterface.unlinkRequest;
import org.xtreemfs.interfaces.OSDInterface.unlinkResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.DeletionStage.DeleteObjectsCallback;
import org.xtreemfs.osd.stages.PreprocStage.DeleteOnCloseCallback;

public final class DeleteOperation extends OSDOperation {

    final int procId;

    final String sharedSecret;

    final ServiceUUID localUUID;

    public DeleteOperation(OSDRequestDispatcher master) {
        super(master);
        procId = unlinkRequest.TAG;
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return procId;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final unlinkRequest args = (unlinkRequest) rq.getRequestArgs();
        master.getPreprocStage().checkDeleteOnClose(args.getFile_id(), new DeleteOnCloseCallback() {

            @Override
            public void deleteOnCloseResult(boolean isDeleteOnClose, Exception error) {
                step2(rq, isDeleteOnClose, args, error);
            }
        });
    }

    public void step2(final OSDRequest rq, boolean isDeleteOnClose, final unlinkRequest args, Exception error) {
        if (error != null) {
            rq.sendException(error);
            return;
        }

        if (!isDeleteOnClose) {
            // file is not open and can be deleted immediately

            // cancel replication of file
            if (rq.getLocationList().getReplicaUpdatePolicy().equals(Constants.REPL_UPDATE_PC_RONLY))
                master.getReplicationStage().cancelReplicationForFile(args.getFile_id());
            
            master.getDeletionStage().deleteObjects(args.getFile_id(), null, rq.getCowPolicy().cowEnabled(), rq, new DeleteObjectsCallback() {

                @Override
                public void deleteComplete(Exception error) {
                    disseminateDeletes(rq, args);
                }
            });
        } else {
            //file marked for delete on close, send ok to client
            disseminateDeletes(rq, args);
        }
    }

    public void disseminateDeletes(final OSDRequest rq, final unlinkRequest args) {
        final Replica localReplica = rq.getLocationList().getLocalReplica();
        if (localReplica.isStriped() && localReplica.getHeadOsd().equals(localUUID)) {
            //striped replica, dissmeninate unlink requests
            try {
                final List<ServiceUUID> osds = rq.getLocationList().getLocalReplica().getOSDs();
                final RPCResponse[] gmaxRPCs = new RPCResponse[osds.size() - 1];
                int cnt = 0;
                for (ServiceUUID osd : osds) {
                    if (!osd.equals(localUUID)) {
                        gmaxRPCs[cnt++] = master.getOSDClient().unlink(osd.getAddress(), args.getFile_id(),
                                args.getFile_credentials());
                    }
                }
                this.waitForResponses(gmaxRPCs, new ResponsesListener() {

                    @Override
                    public void responsesAvailable() {
                        analyzeUnlinkReponses(rq, gmaxRPCs);
                    }
                });
            } catch (UnknownUUIDException ex) {
                rq.sendInternalServerError(ex);
                return;
            }
        } else {
            //non striped replica, fini
            sendResponse(rq);
        }
    }

    public void analyzeUnlinkReponses(final OSDRequest rq,RPCResponse[] gmaxRPCs) {
        //analyze results
        try {
            for (int i = 0; i < gmaxRPCs.length; i++) {
                gmaxRPCs[i].get();
            }
            sendResponse(rq);
        } catch (Exception ex) {
            rq.sendInternalServerError(ex);
        } finally {
            for (RPCResponse r : gmaxRPCs)
                r.freeBuffers();
        }
    }
    public void sendResponse(OSDRequest rq) {
        unlinkResponse response = new unlinkResponse();
        rq.sendSuccess(response);
    }

    @Override
    public yidl.runtime.Object parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        unlinkRequest rpcrq = new unlinkRequest();
        rpcrq.unmarshal(new XDRUnmarshaller(data));

        rq.setFileId(rpcrq.getFile_id());
        rq.setCapability(new Capability(rpcrq.getFile_credentials().getXcap(), sharedSecret));
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