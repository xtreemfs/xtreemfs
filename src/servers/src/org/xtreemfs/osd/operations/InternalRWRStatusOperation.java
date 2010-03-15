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
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.OSDInterface.RedirectException;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_rwr_statusRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_rwr_statusResponse;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_rwr_updateRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_rwr_updateResponse;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.ReplicaStatus;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.osd.stages.StorageStage.InternalGetMaxObjectNoCallback;
import org.xtreemfs.osd.stages.StorageStage.InternalGetReplicaStateCallback;
import org.xtreemfs.osd.stages.StorageStage.WriteObjectCallback;
import org.xtreemfs.osd.storage.CowPolicy;

public final class InternalRWRStatusOperation extends OSDOperation {

    final int procId;
    final String sharedSecret;
    final ServiceUUID localUUID;

    public InternalRWRStatusOperation(OSDRequestDispatcher master) {
        super(master);
        procId = xtreemfs_rwr_statusRequest.TAG;
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return procId;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_rwr_statusRequest args = (xtreemfs_rwr_statusRequest)rq.getRequestArgs();

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"RWR status request for file %s",args.getFile_id());
        }

        replicatedFileOpen(rq, args);

    }
    
    public void getState(final OSDRequest rq, final xtreemfs_rwr_statusRequest args) {
        master.getStorageStage().internalGetReplicaState(args.getFile_id(),
               rq.getLocationList().getLocalReplica().getStripingPolicy(), args.getMax_local_obj_version(), new InternalGetReplicaStateCallback() {

            @Override
            public void getReplicaStateComplete(ReplicaStatus localState, Exception error) {
                sendResult(rq, new xtreemfs_rwr_statusResponse(localState), error);
            }
        });
    }

    public void replicatedFileOpen(final OSDRequest rq, final xtreemfs_rwr_statusRequest args) {

        if (rq.isFileOpen()) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this,"open rw/ repl file: "+rq.getFileId());
            //initialize replication state

            //load max obj ver from disk
            master.getStorageStage().internalGetMaxObjectNo(rq.getFileId(),
                    rq.getLocationList().getLocalReplica().getStripingPolicy(),
                    new InternalGetMaxObjectNoCallback() {

                @Override
                public void maxObjectNoCompleted(long maxObjNo, long filesize, long tepoch, Exception error) {
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, this,"received max objNo for: "+rq.getFileId()+" maxObj: "+maxObjNo+
                                " error: "+error);
                    if (error != null) {
                        sendResult(rq, null, error);
                    } else {
                        //open file in repl stage
                        master.getRWReplicationStage().openFile(args.getFile_credentials(),
                                rq.getLocationList(), maxObjNo, new RWReplicationStage.RWReplicationCallback() {

                                @Override
                                public void success(long newObjectVersion) {
                                    if (Logging.isDebug()) {
                                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "open success for file: " + rq.getFileId());
                                    }
                                    getState(rq,args);
                                }

                                @Override
                                public void redirect(RedirectException redirectTo) {
                                    throw new UnsupportedOperationException("Not supported yet.");
                                }

                                @Override
                                public void failed(Exception ex) {
                                    if (Logging.isDebug()) {
                                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "open failed for file: " + rq.getFileId() + " error: " + ex);
                                    }
                                    sendResult(rq, null, ex);
                                }
                            }, rq);
                    }
                }
            });
        } else
            getState(rq, args);
    }



    public void sendResult(final OSDRequest rq, xtreemfs_rwr_statusResponse response, Exception error) {

        if (error != null) {
            if (error instanceof ONCRPCException)
                rq.sendException((ONCRPCException)error);
            else
                rq.sendInternalServerError(error);
        } else {
            //only locally
           rq.sendSuccess(response);
        }
    }


    @Override
    public yidl.runtime.Object parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        xtreemfs_rwr_statusRequest rpcrq = new xtreemfs_rwr_statusRequest();
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