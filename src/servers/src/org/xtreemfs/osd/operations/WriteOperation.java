/*  Copyright (c) 2009-2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.interfaces.OSDInterface.RedirectException;
import org.xtreemfs.interfaces.OSDInterface.writeRequest;
import org.xtreemfs.interfaces.OSDInterface.writeResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.osd.stages.StorageStage.InternalGetMaxObjectNoCallback;
import org.xtreemfs.osd.stages.StorageStage.ReadObjectCallback;
import org.xtreemfs.osd.stages.StorageStage.WriteObjectCallback;
import org.xtreemfs.osd.storage.ObjectInformation;

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

            if ( (rq.getLocationList().getReplicaUpdatePolicy().length() == 0)
               || (rq.getLocationList().getNumReplicas() == 1) ){

                master.getStorageStage().writeObject(args.getFile_id(), args.getObject_number(), sp,
                        args.getOffset(), args.getObject_data().getData(), rq.getCowPolicy(),
                        rq.getLocationList(), syncWrite, null, rq, new WriteObjectCallback() {

                            @Override
                            public void writeComplete(OSDWriteResponse result, Exception error) {
                                sendResult(rq, result, error);
                            }
                        });
            } else {
                replicatedWrite(rq,args,syncWrite);
            }
        }
    }

    public void replicatedWrite(final OSDRequest rq, final writeRequest args, final boolean syncWrite) {
        //prepareWrite first

        master.getRWReplicationStage().prepareOperation(args.getFile_credentials(), rq.getLocationList(),args.getObject_number(),
                args.getObject_version(), RWReplicationStage.Operation.WRITE,
                new RWReplicationStage.RWReplicationCallback() {

            @Override
            public void success(final long newObjectVersion) {
                assert(newObjectVersion > 0);
                System.out.println("preparOpComplete called");

                //FIXME: ignore canExecOperation for now...
                master.getStorageStage().writeObject(args.getFile_id(), args.getObject_number(),
                        rq.getLocationList().getLocalReplica().getStripingPolicy(),
                        args.getOffset(), args.getObject_data().getData().createViewBuffer(), rq.getCowPolicy(),
                        rq.getLocationList(), syncWrite, newObjectVersion, rq, new WriteObjectCallback() {

                            @Override
                            public void writeComplete(OSDWriteResponse result, Exception error) {
                                if (error != null)
                                    sendResult(rq, null, error);
                                else
                                    sendUpdates(rq,args,result,newObjectVersion);
                            }
                        });
            }

            @Override
            public void redirect(RedirectException redirectTo) {
                BufferPool.free(args.getObject_data().getData());
                sendResult(rq, null, redirectTo);
            }

            @Override
            public void failed(Exception ex) {
                //BufferPool.free(args.getObject_data().getData());
                sendResult(rq, null, ex);
            }
        }, rq);

    }

    public void sendUpdates(final OSDRequest rq, final writeRequest args, final OSDWriteResponse result, final long newObjVersion) {
        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();
        if (args.getObject_data().getData().remaining() == sp.getStripeSizeForObject(args.getObject_number())) {

            sendUpdates2(rq, args, result, newObjVersion, args.getObject_data());
        } else {
            //load object from disk
            BufferPool.free(args.getObject_data().getData());

            master.getStorageStage().readObject(args.getFile_id(), args.getObject_number(), sp, 0, -1, 0l, rq, new ReadObjectCallback() {

                @Override
                public void readComplete(ObjectInformation result2, Exception error) {
                    if (error != null)
                        sendResult(rq, null, error);
                    else {
                        ObjectData od = result2.getObjectData(false, 0, sp.getStripeSizeForObject(args.getObject_number()));
                        sendUpdates2(rq, args, result, newObjVersion, od);
                    }
                }
            });
        }
    }
    public void sendUpdates2(final OSDRequest rq, final writeRequest args, final OSDWriteResponse result, final long newObjVersion,
            final ObjectData data) {
        master.getRWReplicationStage().replicatedWrite(args.getFile_credentials(),rq.getLocationList(),
                    args.getObject_number(), newObjVersion, data,
                    new RWReplicationStage.RWReplicationCallback() {

            @Override
            public void success(long newObjectVersion) {
                sendResult(rq, result, null);
            }

            @Override
            public void redirect(RedirectException redirectTo) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void failed(Exception ex) {
                sendResult(rq, result, ex);
            }
        }, rq);
    }


    public void sendResult(final OSDRequest rq, OSDWriteResponse result, Exception error) {
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
    public yidl.runtime.Object parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
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