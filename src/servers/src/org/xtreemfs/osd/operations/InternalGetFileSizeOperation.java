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
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.InternalGmax;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_get_file_sizeRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_get_file_sizeResponse;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.interfaces.utils.Serializable;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.StorageStage.GetFileSizeCallback;

public final class InternalGetFileSizeOperation extends OSDOperation {

    final int procId;

    final String sharedSecret;

    final ServiceUUID localUUID;

    public InternalGetFileSizeOperation(OSDRequestDispatcher master) {
        super(master);
        xtreemfs_internal_get_file_sizeRequest rq = new xtreemfs_internal_get_file_sizeRequest();
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
        final xtreemfs_internal_get_file_sizeRequest args = (xtreemfs_internal_get_file_sizeRequest) rq.getRequestArgs();

        System.out.println("rq: "+args);


        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

        master.getStorageStage().getFilesize(args.getFile_id(), sp, rq, new GetFileSizeCallback() {

            @Override
            public void getFileSizeComplete(long fileSize, Exception error) {
                step2(rq, args, fileSize, error);
            }
        });
    }

    public void step2(final OSDRequest rq, xtreemfs_internal_get_file_sizeRequest args, long localFS, Exception error) {
        if (error != null) {
            if (error instanceof ONCRPCException) {
                rq.sendException((ONCRPCException) error);
            } else {
                rq.sendInternalServerError(error);
            }
        } else {
            if (rq.getLocationList().getLocalReplica().isStriped()) {
                //striped read
                stripedGetFS(rq, args, localFS);
            } else {
                //non-striped case
                sendResponse(rq, localFS);
            }
        }
    }

    private void stripedGetFS(final OSDRequest rq, final xtreemfs_internal_get_file_sizeRequest args, final long localFS) {
        try {
            final List<ServiceUUID> osds = rq.getLocationList().getLocalReplica().getOSDs();
            final RPCResponse[] gmaxRPCs = new RPCResponse[osds.size() - 1];
            int cnt = 0;
            for (ServiceUUID osd : osds) {
                if (!osd.equals(localUUID)) {
                    gmaxRPCs[cnt++] = master.getOSDClient().internal_get_gmax(osd.getAddress(), args.getFile_id(), args.getFile_credentials());
                }
            }
            this.waitForResponses(gmaxRPCs, new ResponsesListener() {

                @Override
                public void responsesAvailable() {
                    stripedReadAnalyzeGmax(rq, args, localFS, gmaxRPCs);
                }
            });
        } catch (UnknownUUIDException ex) {
            rq.sendInternalServerError(ex);
            return;
        }
        
    }

    private void stripedReadAnalyzeGmax(final OSDRequest rq, final xtreemfs_internal_get_file_sizeRequest args,
            final long localFS, RPCResponse[] gmaxRPCs) {
        long maxFS = localFS;

        try {
            for (int i = 0; i < gmaxRPCs.length; i++) {
                InternalGmax gmax = (InternalGmax) gmaxRPCs[i].get();
                if (gmax.getFile_size() > maxFS) {
                    //found new max
                    maxFS = gmax.getFile_size();
                }
            }
            sendResponse(rq, maxFS);
        } catch (Exception ex) {
            rq.sendInternalServerError(ex);
        } finally {
            for (RPCResponse r : gmaxRPCs)
                r.freeBuffers();
        }
    }

    public void sendResponse(OSDRequest rq, long fileSize) {
        xtreemfs_internal_get_file_sizeResponse response = new xtreemfs_internal_get_file_sizeResponse(fileSize);
        rq.sendSuccess(response);
    }

    @Override
    public Serializable parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        xtreemfs_internal_get_file_sizeRequest rpcrq = new xtreemfs_internal_get_file_sizeRequest();
        rpcrq.deserialize(data);

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