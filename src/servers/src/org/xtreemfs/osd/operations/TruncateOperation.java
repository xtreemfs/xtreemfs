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

import java.io.IOException;
import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.StripingPolicyImpl;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.OSDWriteResponse;
import org.xtreemfs.interfaces.OSDInterface.OSDException;
import org.xtreemfs.interfaces.OSDInterface.RedirectException;
import org.xtreemfs.interfaces.OSDInterface.truncateRequest;
import org.xtreemfs.interfaces.OSDInterface.truncateResponse;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.rwre.RWReplicationStage;
import org.xtreemfs.osd.stages.StorageStage.InternalGetMaxObjectNoCallback;
import org.xtreemfs.osd.stages.StorageStage.TruncateCallback;

public final class TruncateOperation extends OSDOperation {

    final int procId;

    final String sharedSecret;

    final ServiceUUID localUUID;

    public TruncateOperation(OSDRequestDispatcher master) {
        super(master);
        procId = truncateRequest.TAG;
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return procId;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final truncateRequest args = (truncateRequest) rq.getRequestArgs();

        if (args.getNew_file_size() < 0) {
            rq.sendException(new OSDException(ErrorCodes.INVALID_PARAMS, "new_file_size for truncate must be >= 0", ""));
            return;
        }

        if (!rq.getLocationList().getLocalReplica().getHeadOsd().equals(localUUID)) {
            rq.sendException(new OSDException(ErrorCodes.INVALID_PARAMS, "truncate must be executed at the head OSD (first OSD in replica)", ""));
            return;
        }

        if (rq.getLocationList().getReplicaUpdatePolicy().equals(Constants.REPL_UPDATE_PC_RONLY)) {
            // file is read only
            rq.sendException(new OSDException(ErrorCodes.FILE_IS_READ_ONLY,
                    "Cannot write on read-only files.", ""));
        }

        if ((rq.getLocationList().getReplicaUpdatePolicy().length() == 0)
            || (rq.getLocationList().getNumReplicas() == 1)) {

            master.getStorageStage().truncate(args.getFile_id(), args.getNew_file_size(),
                rq.getLocationList().getLocalReplica().getStripingPolicy(),
                rq.getLocationList().getLocalReplica(), rq.getCapability().getEpochNo(), rq.getCowPolicy(),
                null, rq,
                new TruncateCallback() {

                    @Override
                    public void truncateComplete(OSDWriteResponse result, Exception error) {
                        step2(rq, args, result, error);
                    }
            });
        } else {
            replicatedFileOpen(rq, args);
        }
    }

    public void replicatedFileOpen(final OSDRequest rq,
            final truncateRequest args) {

        if (rq.isFileOpen()) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "open rw/ repl file: " + rq.getFileId());
            }
            //initialize replication state

            //load max obj ver from disk
            master.getStorageStage().internalGetMaxObjectNo(rq.getFileId(),
                    rq.getLocationList().getLocalReplica().getStripingPolicy(),
                    new InternalGetMaxObjectNoCallback() {

                        @Override
                        public void maxObjectNoCompleted(long maxObjNo, long filesize, long tepoch, Exception error) {
                            if (Logging.isDebug()) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, this, "received max objNo for: " + rq.getFileId() + " maxObj: " + maxObjNo +
                                        " error: " + error);
                            }
                            if (error != null) {
                                sendError(rq, error);
                            } else {
                                //open file in repl stage
                                master.getRWReplicationStage().openFile(args.getFile_credentials(),
                                        rq.getLocationList(), maxObjNo, new RWReplicationStage.RWReplicationCallback() {

                                        @Override
                                        public void success(long newObjectVersion) {
                                            if (Logging.isDebug()) {
                                                Logging.logMessage(Logging.LEVEL_DEBUG, this, "open success for file: " + rq.getFileId());
                                            }
                                            rwReplicatedTruncate(rq, args);
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
                                            sendError(rq, ex);
                                        }
                                    }, rq);

                            }

                        }
                    });
        } else {
            rwReplicatedTruncate(rq, args);
        }
    }

    public void rwReplicatedTruncate(final OSDRequest rq,
            final truncateRequest args) {
        master.getRWReplicationStage().prepareOperation(args.getFile_credentials(), 0, 0, RWReplicationStage.Operation.TRUNCATE, new RWReplicationStage.RWReplicationCallback() {

            @Override
            public void success(final long newObjectVersion) {
                System.out.println("preparOpComplete called");
                final StripingPolicyImpl sp = rq.getLocationList().getLocalReplica().getStripingPolicy();

                //FIXME: ignore canExecOperation for now...
               master.getStorageStage().truncate(args.getFile_id(), args.getNew_file_size(),
                    rq.getLocationList().getLocalReplica().getStripingPolicy(),
                    rq.getLocationList().getLocalReplica(), rq.getCapability().getEpochNo(), rq.getCowPolicy(),
                    newObjectVersion, rq, new TruncateCallback() {

                    @Override
                    public void truncateComplete(OSDWriteResponse result, Exception error) {
                        replicateTruncate(rq, newObjectVersion, args, result, error);
                    }
                });
            }

            @Override
            public void redirect(RedirectException redirectTo) {
                sendError(rq, redirectTo);
            }

            @Override
            public void failed(Exception ex) {
                sendError(rq, ex);
            }
        }, rq);
    }

    public void replicateTruncate(final OSDRequest rq,
            final long newObjVersion,
            final truncateRequest args,
            final OSDWriteResponse result, Exception error) {
            if (error != null)
                step2(rq, args, result, error);
            else {
                master.getRWReplicationStage().replicateTruncate(args.getFile_credentials(),
                    args.getNew_file_size(), newObjVersion,
                    rq.getLocationList(), new RWReplicationStage.RWReplicationCallback() {

                    @Override
                    public void success(long newObjectVersion) {
                        step2(rq, args, result, null);
                    }

                    @Override
                    public void redirect(RedirectException redirectTo) {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                    @Override
                    public void failed(Exception ex) {
                        step2(rq, args, result, ex);
                    }
                }, rq);

            }
    }

    public void step2(final OSDRequest rq,
            final truncateRequest args,
            OSDWriteResponse result, Exception error) {

        if (error != null) {
            if (error instanceof ONCRPCException) {
                rq.sendException((ONCRPCException) error);
            } else {
                rq.sendInternalServerError(error);
            }
        } else {
            //check for striping
            if (rq.getLocationList().getLocalReplica().isStriped()) {
                //disseminate internal truncate to all other OSDs
                disseminateTruncates(rq, args, result);
            } else {
                //non-striped
                sendResponse(rq, result);
            }

        }
    }

    private void disseminateTruncates(final OSDRequest rq,
            final truncateRequest args,
            final OSDWriteResponse result) {
        try {
            final List<ServiceUUID> osds = rq.getLocationList().getLocalReplica().getOSDs();
            final RPCResponse[] gmaxRPCs = new RPCResponse[osds.size() - 1];
            int cnt = 0;
            for (ServiceUUID osd : osds) {
                if (!osd.equals(localUUID)) {
                    gmaxRPCs[cnt++] = master.getOSDClient().internal_truncate(osd.getAddress(),
                            args.getFile_id(), args.getFile_credentials(), args.getNew_file_size());
                }

            }
            this.waitForResponses(gmaxRPCs, new ResponsesListener() {

                @Override
                public void responsesAvailable() {
                    analyzeTruncateResponses(rq, result, gmaxRPCs);
                }
            });
        } catch (IOException ex) {
            rq.sendInternalServerError(ex);
        } catch (Throwable ex) {
            rq.sendInternalServerError(ex);
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
            return;

        }


    }

    private void analyzeTruncateResponses(OSDRequest rq, OSDWriteResponse result, RPCResponse[] gmaxRPCs) {
        //analyze results
        try {
            for (int i = 0; i <
                    gmaxRPCs.length; i++) {
                gmaxRPCs[i].get();
            }

            sendResponse(rq, result);
        } catch (IOException ex) {
            rq.sendInternalServerError(ex);
        } catch (Throwable ex) {
            rq.sendInternalServerError(ex);
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        } finally {
            for (RPCResponse r : gmaxRPCs) {
                r.freeBuffers();
            }
        }

    }

    public void sendResponse(OSDRequest rq, OSDWriteResponse result) {
        truncateResponse response = new truncateResponse(result);
        rq.sendSuccess(response);
    }

    @Override
    public yidl.runtime.Object parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        truncateRequest rpcrq = new truncateRequest();
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
