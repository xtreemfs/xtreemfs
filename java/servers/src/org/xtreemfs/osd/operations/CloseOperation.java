/*
 * Copyright (c) 2009-2012 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import java.io.IOException;
import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.Replica;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.LRUCache;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.DeletionStage.DeleteObjectsCallback;
import org.xtreemfs.osd.stages.PreprocStage.CloseCallback;
import org.xtreemfs.osd.stages.StorageStage.CachesFlushedCallback;
import org.xtreemfs.osd.stages.StorageStage.CreateFileVersionCallback;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.closeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.closeResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public final class CloseOperation extends OSDOperation {

    final String      sharedSecret;
    final ServiceUUID localUUID;

    public CloseOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_CLOSE;
    }

    @Override
    public void startRequest(final OSDRequest rq) {

        /*
         * To close a file, the following steps are necessary:
         * 
         * 1. remove the file from the open file table (PreprocStage)
         * 
         * 2. flush the file's internal metadata cache (StorageStage)
         * 
         * 3. create a new file version if versioning / COW is enabled (StorageStage)
         * 
         * 4. delete the file along with its objects if the file has been marked for deletion (DeletionStage)
         * 
         * 5. disseminate the close request to other OSDs if the file is striped
         */

        final closeRequest args = (closeRequest) rq.getRequestArgs();
        master.getPreprocStage().close(args.getFileId(), new CloseCallback() {

            @Override
            public void closeResult(boolean delete, LRUCache<String, Capability> cachedCaps, ErrorResponse error) {
                flushCaches(rq, delete, args, cachedCaps, error);
            }
        });
    }

    public void flushCaches(final OSDRequest rq, final boolean delete, final closeRequest args,
            final LRUCache<String, Capability> cachedCaps, ErrorResponse error) {

        if (error != null) {
            rq.sendError(error);
            return;
        }

        master.getStorageStage().flushCaches(args.getFileId(), new CachesFlushedCallback() {

            @Override
            public void cachesFlushed(ErrorResponse error, FileMetadata fi) {
                createVersionIfNecessary(rq, delete, args, cachedCaps, fi, error);
            }
        });

        // asynchronously report close operation to read-write replication stage
        master.getRWReplicationStage().fileClosed(args.getFileId());

    }

    public void createVersionIfNecessary(final OSDRequest rq, final boolean delete, final closeRequest args,
            LRUCache<String, Capability> cachedCaps, final FileMetadata fi, ErrorResponse error) {

        if (error != null) {
            rq.sendError(error);
            return;
        }

        // if COW is enabled, create a new version
        if (rq.getCowPolicy().cowEnabled()) {

            // first, check if there are any write capabilities among the cached
            // capabilities
            boolean writeCap = false;
            if (cachedCaps != null)
                for (Capability cap : cachedCaps.values()) {
                    int accessMode = cap.getAccessMode();
                    if ((accessMode & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber()) != 0
                            || (accessMode & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber()) != 0
                            || (accessMode & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY.getNumber()) != 0
                            || (accessMode & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_APPEND.getNumber()) != 0) {
                        writeCap = true;
                        break;
                    }
                }

            // if there are no write capabilities, there is no need to create a new
            // version
            if (!writeCap) {
                deleteIfNecessary(rq, delete, args, null);
                return;
            }

            master.getStorageStage().createFileVersion(rq.getFileId(), fi, rq, new CreateFileVersionCallback() {

                @Override
                public void createFileVersionComplete(long fileSize, ErrorResponse error) {
                    deleteIfNecessary(rq, delete, args, error);
                }
            });
        }

        else {
            deleteIfNecessary(rq, delete, args, null);
        }

    }

    public void deleteIfNecessary(final OSDRequest rq, final boolean delete, final closeRequest args,
            ErrorResponse error) {

        if (error != null) {
            rq.sendError(error);
            return;
        }

        if (delete) {

            // cancel replication of file
            master.getReplicationStage().cancelReplicationForFile(args.getFileId());

            // delete the file across all stripes
            master.getDeletionStage().deleteObjects(args.getFileId(), null, rq.getCowPolicy().cowEnabled(), rq,
                    new DeleteObjectsCallback() {

                        @Override
                        public void deleteComplete(ErrorResponse error) {

                            if (error != null)
                                rq.sendError(error);
                            else
                                disseminateCloseIfNecessary(rq, delete, args, error);
                        }
                    });
        }

        else {
            disseminateCloseIfNecessary(rq, delete, args, null);
        }
    }

    public void disseminateCloseIfNecessary(final OSDRequest rq, final boolean delete, final closeRequest args,
            ErrorResponse error) {

        if (error != null) {
            rq.sendError(error);
            return;
        }

        final Replica localReplica = rq.getLocationList().getLocalReplica();
        if (localReplica.isStriped() && localReplica.getHeadOsd().equals(localUUID)) {
            // striped replica, dissmeninate close requests
            try {
                final List<ServiceUUID> osds = rq.getLocationList().getLocalReplica().getOSDs();
                final RPCResponse[] gmaxRPCs = new RPCResponse[osds.size() - 1];
                int cnt = 0;
                for (ServiceUUID osd : osds) {
                    if (!osd.equals(localUUID)) {
                        gmaxRPCs[cnt++] = master.getOSDClient().close(osd.getAddress(), RPCAuthentication.authNone,
                                RPCAuthentication.userService, args.getFileCredentials(), args.getFileId());
                    }
                }
                this.waitForResponses(gmaxRPCs, new ResponsesListener() {

                    @Override
                    public void responsesAvailable() {
                        analyzeCloseReponses(rq, delete, args, gmaxRPCs);
                    }
                });
            } catch (IOException ex) {
                rq.sendInternalServerError(ex);
                return;
            }
        } else {
            // non-striped replica
            sendResponse(rq);
        }
    }

    public void analyzeCloseReponses(final OSDRequest rq, boolean delete, final closeRequest args,
            RPCResponse[] gmaxRPCs) {
        // analyze results
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

        // create a response with the current server time
        closeResponse.Builder response = closeResponse.newBuilder();
        response.setServerTimestamp(TimeSync.getGlobalTime());

        // send the response to the client
        rq.sendSuccess(response.build(), null);
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            closeRequest rpcrq = (closeRequest) rq.getRequestArgs();
            rq.setFileId(rpcrq.getFileId());
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