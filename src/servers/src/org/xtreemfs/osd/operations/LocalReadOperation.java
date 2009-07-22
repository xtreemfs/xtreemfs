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
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.osd.operations;

import java.io.IOException;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.interfaces.InternalReadLocalResponse;
import org.xtreemfs.interfaces.ObjectData;
import org.xtreemfs.interfaces.ObjectList;
import org.xtreemfs.interfaces.ObjectListSet;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_read_localRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_internal_read_localResponse;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.interfaces.utils.Serializable;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.replication.ObjectSet;
import org.xtreemfs.osd.stages.StorageStage.GetObjectListCallback;
import org.xtreemfs.osd.stages.StorageStage.ReadObjectCallback;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.osd.storage.ObjectInformation.ObjectStatus;

public final class LocalReadOperation extends OSDOperation {

    final int procId;

    final String sharedSecret;

    final ServiceUUID localUUID;

    public LocalReadOperation(OSDRequestDispatcher master) {
        super(master);
        xtreemfs_internal_read_localRequest rq = new xtreemfs_internal_read_localRequest();
        procId = xtreemfs_internal_read_localRequest.TAG;
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return procId;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_internal_read_localRequest args = (xtreemfs_internal_read_localRequest) rq
                .getRequestArgs();

//        System.out.println("rq: " + args);

        if (args.getObject_number() < 0) {
            rq.sendException(new OSDException(ErrorCodes.INVALID_PARAMS, "object number must be >= 0", ""));
            return;
        }

        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

        master.getStorageStage().readObject(args.getFile_id(), args.getObject_number(), sp, (int)args.getOffset(),
                (int)args.getLength(), rq,
                new ReadObjectCallback() {

                    @Override
                    public void readComplete(ObjectInformation result, Exception error) {
                        postRead(rq, args, result, error);
                    }
                });
    }

    public void postRead(final OSDRequest rq, final xtreemfs_internal_read_localRequest args,
            final ObjectInformation result, Exception error) {
        if (error != null) {
            if (error instanceof ONCRPCException) {
                rq.sendException((ONCRPCException) error);
            } else {
                rq.sendInternalServerError(error);
            }
        } else {
            if (args.getAttachObjectList()) { // object list is requested
                master.getStorageStage().getObjectSet(args.getFile_id(), rq,
                        new GetObjectListCallback() {
                            @Override
                            public void getObjectSetComplete(ObjectSet objectSet, Exception error) {
                                postReadObjectSet(rq, args, result, objectSet, error);
                            }
                        });
            } else
                readFinish(rq, args, result, null);
        }
    }

    public void postReadObjectSet(final OSDRequest rq, xtreemfs_internal_read_localRequest args,
            ObjectInformation data, ObjectSet result, Exception error) {
        if (error != null) {
            if (error instanceof ONCRPCException) {
                rq.sendException((ONCRPCException) error);
            } else {
                rq.sendInternalServerError(error);
            }
        } else {
            // serialize objectSet
            ReusableBuffer objectSetBuffer = null;
            byte[] serialized;
            try {
                serialized = result.getSerializedBitSet();
                objectSetBuffer = ReusableBuffer.wrap(serialized);

                // TODO: set "is complete" flag correctly
                // TODO: interface must be changed and then this must be adapted
                ObjectList objList = new ObjectList(objectSetBuffer, result.getStripeWidth(),
                        false);
                readFinish(rq, args, data, objList);
            } catch (IOException e) {
                rq.sendInternalServerError(e);
            }
        }
    }

    private void readFinish(OSDRequest rq, xtreemfs_internal_read_localRequest args,
            ObjectInformation result, ObjectList objectList) {
        ObjectData data = null;
        // send raw data
        if (result.getStatus() == ObjectStatus.EXISTS) {
            // TODO: better implementation
            final boolean isRangeRequested = (args.getOffset() > 0)
                    || (args.getLength() < result.getStripeSize());
            if (isRangeRequested) {
                // FIXME: check if implementation delivers only raw data
                data = result.getObjectData(true, (int) args.getOffset(), (int) args.getLength());
            } else {
                data = new ObjectData(0, result.isChecksumInvalidOnOSD(), 0, result.getData());
            }
        } else
            data = new ObjectData(0, result.isChecksumInvalidOnOSD(), 0, null);

        master.objectSent();
        if (data.getData() != null)
            master.dataSent(data.getData().capacity());

        // System.out.println("resp: " + data);
        sendResponse(rq, data, objectList);
    }

    public void sendResponse(OSDRequest rq, ObjectData result, ObjectList objectList) {
        ObjectListSet set = new ObjectListSet();
        if (objectList != null && objectList.getObject_list().limit() != 0)
            set.add(objectList);

        InternalReadLocalResponse readLocalResponse = new InternalReadLocalResponse(result, set);
        xtreemfs_internal_read_localResponse response = new xtreemfs_internal_read_localResponse(
                readLocalResponse);
        rq.sendSuccess(response);
    }

    @Override
    public Serializable parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        xtreemfs_internal_read_localRequest rpcrq = new xtreemfs_internal_read_localRequest();
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