/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.FileOperationCallback;
import org.xtreemfs.osd.InternalObjectData;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.ec.ECStage;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.osd.stages.StorageStage.ReadObjectCallback;
import org.xtreemfs.osd.stages.StorageStage.WriteObjectCallback;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.writeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class WriteOperation extends OSDOperation {

    final String sharedSecret;
    final ServiceUUID localUUID;

    public WriteOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_WRITE;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final writeRequest args = (writeRequest)rq.getRequestArgs();

        if (args.getObjectNumber() < 0) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "object number must be >= 0");
            return;
        }

        if (args.getOffset() < 0) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "offset must be >= 0");
            return;
        }

        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

        if (args.getOffset() >= sp.getStripeSizeForObject(args.getObjectNumber())) {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, "offset must be < stripe size");
            return;
        }
        
        if (rq.getLocationList().getReplicaUpdatePolicy().equals(ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY)) {
            // file is read only
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EPERM, "Cannot write on read-only files.");
        } else {

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                        "received write op");
            }
            boolean syncWrite = (rq.getCapability().getAccessMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_SYNC.getNumber()) > 0;


            master.objectReceived();
            master.dataReceived(rq.getRPCRequest().getData().capacity());

            // TODO(jdillmann): Use centralized method to check if a lease is required.
            if (rq.getLocationList().getNumReplicas() > 1
                    && ReplicaUpdatePolicies.isRwReplicated(rq.getLocationList().getReplicaUpdatePolicy())) {

                replicatedWrite(rq,args,syncWrite);

            } else if (sp.getPolicy().getType() == GlobalTypes.StripingPolicyType.STRIPING_POLICY_ERASURECODE) {

                ecWrite(rq, args, syncWrite);

            } else {

                ReusableBuffer viewBuffer = rq.getRPCRequest().getData().createViewBuffer();
                master.getStorageStage().writeObject(args.getFileId(), args.getObjectNumber(), sp,
                        args.getOffset(), viewBuffer, rq.getCowPolicy(),
                        rq.getLocationList(), syncWrite, null, rq, viewBuffer, new WriteObjectCallback() {

                            @Override
                            public void writeComplete(OSDWriteResponse result, ErrorResponse error) {
                                sendResult(rq, result, error);
                            }
                        });
            }
        }
    }

    public void ecWrite(final OSDRequest rq, final writeRequest args, final boolean syncWrite) {
        // TODO(jan): add write code..see how replicatedWrite interacts with RWReplicationStage

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                    "erasure coded write started");
        }
        master.getECStage().prepareOperation(args.getFileCredentials(), rq.getLocationList(),args.getObjectNumber(),
                args.getObjectVersion(), ECStage.Operation.WRITE,
                new FileOperationCallback() {

                    @Override
                    public void success(final long newObjectVersion) {
                        assert(newObjectVersion > 0);

                        // TODO(jan) when we have new version distribute updates

                        //FIXME: ignore canExecOperation for now...
                        ReusableBuffer viewBuffer = rq.getRPCRequest().getData().createViewBuffer();
                        master.getStorageStage().writeObject(args.getFileId(), args.getObjectNumber(),
                                rq.getLocationList().getLocalReplica().getStripingPolicy(),
                                args.getOffset(), viewBuffer, rq.getCowPolicy(),
                                rq.getLocationList(), syncWrite, newObjectVersion, rq, viewBuffer, new WriteObjectCallback() {

                                    @Override
                                    public void writeComplete(OSDWriteResponse result, ErrorResponse error) {
                                        if (error != null) {
                                            if (Logging.isDebug()) {
                                                Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                                                        "erasure coded write failed");
                                            }
                                            sendResult(rq, null, error);
                                            // TODO: distributed updates here
                                        } else {
                                            if (Logging.isDebug()) {
                                                Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                                                        "erasure coded write was successful");
                                            }
                                            sendResult(rq, result, null);
                                        }
                                    }
                                });
                    }

                    @Override
                    public void redirect(String redirectTo) {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                                    "write was redirected...NOT IMPLEMENTED YET");
                        }
                        rq.getRPCRequest().sendRedirect(redirectTo);
                    }

                    @Override
                    public void failed(ErrorResponse err) {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.ec, this,
                                    "write failed");
                        }
                        rq.sendError(err);
                    }
                }, rq);

    }

    public void replicatedWrite(final OSDRequest rq, final writeRequest args, final boolean syncWrite) {
        //prepareWrite first

        master.getRWReplicationStage().prepareOperation(args.getFileCredentials(), rq.getLocationList(),args.getObjectNumber(),
                args.getObjectVersion(), RWReplicationStage.Operation.WRITE,
                new FileOperationCallback() {

            @Override
            public void success(final long newObjectVersion) {
                assert(newObjectVersion > 0);

                //FIXME: ignore canExecOperation for now...
                ReusableBuffer viewBuffer = rq.getRPCRequest().getData().createViewBuffer();
                master.getStorageStage().writeObject(args.getFileId(), args.getObjectNumber(),
                        rq.getLocationList().getLocalReplica().getStripingPolicy(),
                        args.getOffset(), viewBuffer, rq.getCowPolicy(),
                        rq.getLocationList(), syncWrite, newObjectVersion, rq, viewBuffer, new WriteObjectCallback() {

                            @Override
                            public void writeComplete(OSDWriteResponse result, ErrorResponse error) {
                                if (error != null)
                                    sendResult(rq, null, error);
                                else
                                    sendUpdates(rq,args,result,newObjectVersion);
                            }
                        });
            }

            @Override
            public void redirect(String redirectTo) {
                rq.getRPCRequest().sendRedirect(redirectTo);
            }

            @Override
            public void failed(ErrorResponse err) {
                rq.sendError(err);
            }
        }, rq);

    }

    public void sendUpdates(final OSDRequest rq, final writeRequest args, final OSDWriteResponse result, final long newObjVersion) {
        final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();
        if (rq.getRPCRequest().getData().remaining() == sp.getStripeSizeForObject(args.getObjectNumber())) {

            ReusableBuffer viewBuffer = rq.getRPCRequest().getData().createViewBuffer();
            
            sendUpdates2(rq, args, result, newObjVersion, new InternalObjectData(args.getObjectData(), viewBuffer), viewBuffer);
        } else {

            master.getStorageStage().readObject(args.getFileId(), args.getObjectNumber(), sp, 0, -1, 0l, rq, new ReadObjectCallback() {

                @Override
                public void readComplete(ObjectInformation result2, ErrorResponse error) {
                    if (error != null)
                        sendResult(rq, null, error);
                    else {
                        InternalObjectData od = result2.getObjectData(false, 0, sp.getStripeSizeForObject(args.getObjectNumber()));
                        sendUpdates2(rq, args, result, newObjVersion, od, null);
                    }
                }
            });
        }
    }
    public void sendUpdates2(final OSDRequest rq, final writeRequest args, final OSDWriteResponse result, final long newObjVersion,
            final InternalObjectData data, final ReusableBuffer createdViewBuffer) {
        master.getRWReplicationStage().replicatedWrite(args.getFileCredentials(),rq.getLocationList(),
                    args.getObjectNumber(), newObjVersion, data, createdViewBuffer,
                    new FileOperationCallback() {

            @Override
            public void success(long newObjectVersion) {
                sendResult(rq, result, null);
            }

            @Override
            public void redirect(String redirectTo) {
                rq.getRPCRequest().sendRedirect(redirectTo);
            }

            @Override
            public void failed(ErrorResponse err) {
               rq.sendError(err);
            }
        }, rq);
    }


    public void sendResult(final OSDRequest rq, OSDWriteResponse result, ErrorResponse error) {
        if (error != null) {
            rq.sendError(error);
        } else {
            sendResponse(rq, result);
        }

    }

    public void sendResponse(OSDRequest rq, OSDWriteResponse result) {
        rq.sendSuccess(result,null);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            writeRequest rpcrq = (writeRequest)rq.getRequestArgs();
            rq.setFileId(rpcrq.getFileCredentials().getXcap().getFileId());
            rq.setCapability(new Capability(rpcrq.getFileCredentials().getXcap(), sharedSecret));
            rq.setLocationList(new XLocations(rpcrq.getFileCredentials().getXlocs(), localUUID));

            return null;
        } catch (InvalidXLocationsException ex) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, ex.toString());
        } catch (Throwable ex) {
            return ErrorUtils.getInternalServerError(ex);
        }
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