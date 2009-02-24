/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.osd.stages;

import java.io.IOException;
import java.util.List;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.foundation.speedy.SpeedyResponseListener;
import org.xtreemfs.new_mrc.ErrNo;
import org.xtreemfs.osd.ErrorRecord;
import org.xtreemfs.osd.OSDException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.ErrorRecord.ErrorClass;
import org.xtreemfs.osd.ops.Operation;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.osd.storage.Striping;
import org.xtreemfs.osd.storage.Striping.RPCMessage;

public class DeletionStage extends Stage {

    public static final int   STAGEOP_DELETE_FILE      = 0;

    public static final int   STAGEOP_CHECK_OPEN_STATE = 1;

    public static final int   STAGEOP_DELETE_OBJECTS   = 2;

    private MetadataCache     cache;

    private StorageLayout     layout;

    private Striping          striping;

    private RequestDispatcher master;

    private DeleteThread      deletor;

    public DeletionStage(RequestDispatcher master, Striping striping, MetadataCache cache,
        StorageLayout layout) {

        super("OSD Deletion Stage");

        this.master = master;
        this.striping = striping;
        this.cache = cache;
        this.layout = layout;

        deletor = new DeleteThread(layout);
    }

    public void start() {
        super.start();
        deletor.start();
        deletor.setPriority(MIN_PRIORITY);
    }

    public void shutdown() {
        super.shutdown();
        deletor.shutdown();
    }

    @Override
    protected void processMethod(StageMethod method) {

        try {
            switch (method.getStageMethod()) {
            case STAGEOP_DELETE_FILE:
                processDisseminateRequests(method);
                break;
            case STAGEOP_CHECK_OPEN_STATE:
                processCheckOpenState(method);
                break;
            case STAGEOP_DELETE_OBJECTS:
                processDeleteObjects(method);
                methodExecutionSuccess(method, StageResponseCode.OK);
                break;
            }

        } catch (OSDException exc) {
            methodExecutionFailed(method, exc.getErrorRecord());
        } catch (Exception exc) {
            Logging.logMessage(Logging.LEVEL_ERROR, this,exc);
            methodExecutionFailed(method, new ErrorRecord(
                ErrorRecord.ErrorClass.INTERNAL_SERVER_ERROR, "an error has occurred", exc));
        }
    }

    private boolean processDisseminateRequests(StageMethod rq) throws IOException, JSONException {

        final Location currentLoc = rq.getRq().getDetails().getCurrentReplica();

        // for the sake of robustness, check if contacted OSD is head OSD and
        // redirect if necessary
        if (!master.isHeadOSD(currentLoc)) {
            throw new OSDException(ErrorClass.REDIRECT, currentLoc.getOSDs().get(0).toString());
        }

        if (currentLoc.getWidth() == 1) {
            // if no dissemination of delete requests is necessary, immediately
            // proceed with next step
            processCheckOpenState(rq);
            return true;

        } else {
            // if requests need to be disseminated, send RPCs to all remote OSDs
            sendDeleteRequests(rq);

            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "sent delete requests for file "
                    + rq.getRq().getDetails().getFileId() + " to remote OSDs in "
                    + currentLoc.getOSDs() + ", local OSD is " + master.getConfig().getUUID());
        }

        return false;
    }

    private void processCheckOpenState(StageMethod rq) throws IOException, JSONException {

        String fileId = rq.getRq().getDetails().getFileId();

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "preparing deletion of file " + fileId
                + ": sending request to delete file from open file table");

        // send an internal RPC to the Authentication Stage to initiate the
        // file deletion in the open file table and to find out whether the
        // object deletion has to be deferred due to a valid open state
        final Operation deleteOftRPC = master.getOperation(RequestDispatcher.Operations.OFT_DELETE);
        final OSDRequest rpc = new OSDRequest(-1);
        rpc.getDetails().setFileId(fileId);
        rpc.setOperation(deleteOftRPC);
        rpc.setAttachment(rq.getRq());
        deleteOftRPC.startRequest(rpc);
    }

    private void processDeleteObjects(StageMethod rq) throws IOException {

        final String fileId = rq.getRq().getDetails().getFileId();

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this,
                "received request for deleting objects of file " + fileId);

        // do not perform an immediate deletion if "delete on close" is set
        if (rq.getRq().getAttachment() != null && (Boolean) rq.getRq().getAttachment()) {
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "file " + fileId
                    + " is still open, object deletion will be deferred");
            return;
        }

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "deleting objects of file " + fileId);

        // remove the file info from the storage cache
        cache.removeFileInfo(fileId);

        // otherwise, remove all local objects
        if (layout.fileExists(fileId))
            deletor.enqueueFileForDeletion(fileId);
        else
            throw new OSDException(ErrorClass.USER_EXCEPTION, 2, "file " + fileId + " not found");
    }

    private void sendDeleteRequests(StageMethod rq) throws IOException, JSONException {

        List<RPCMessage> deleteReqs = striping.createDeleteRequests(rq.getRq().getDetails());

        final StageMethod req = rq;

        SpeedyRequest[] reqs = new SpeedyRequest[deleteReqs.size()];
        int i = 0;
        for (RPCMessage msg : deleteReqs) {
            SpeedyRequest sr = msg.req;
            sr.listener = new SpeedyResponseListener() {

                public void receiveRequest(SpeedyRequest theRequest) {

                    theRequest.freeBuffer();

                    // count received responses
                    OSDRequest osdReq = (OSDRequest) theRequest.getOriginalRequest();
                    long count = (Long) osdReq.getAttachment();
                    count++;
                    osdReq.setAttachment(count);

                    // check if all responses have been received;
                    // if so, enqueue an operation for the next step
                    if (count == osdReq.getHttpRequests().length)
                        enqueueOperation(osdReq, STAGEOP_CHECK_OPEN_STATE, req.getCallback());
                }

            };
            reqs[i++] = sr;
        }

        rq.getRq().setHttpRequests(reqs);
        rq.getRq().setAttachment(0L);

        Logging.logMessage(Logging.LEVEL_DEBUG, this, "sending delete requests to remote OSDs ...");

        for (RPCMessage msg : deleteReqs)
            master.sendSpeedyRequest(rq.getRq(), msg.req, msg.addr);

        master.getStatistics().numDeletes++;
    }

    private final static class DeleteThread extends Thread {

        private transient boolean quit;

        private final StorageLayout layout;
        private final LinkedBlockingQueue<String> files;

        public DeleteThread(StorageLayout layout) {
            quit = false;
            this.layout = layout;
            files = new LinkedBlockingQueue<String>();
        }

        public void shutdown() {
            this.quit = true;
            this.interrupt();
        }

        public void enqueueFileForDeletion(String fileID) {
            assert(this.isAlive());
            assert(fileID != null);
            files.add(fileID);
        }

        public void run() {
            try {
                do {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,"DeleteThread started");
                    final String fileID = files.take();
                    try {
                        layout.deleteFile(fileID);
                    } catch (IOException ex) {
                        Logging.logMessage(Logging.LEVEL_ERROR, this,ex);
                    }
                } while (!quit);
            } catch (InterruptedException ex) {
                //idontcare
            }
            Logging.logMessage(Logging.LEVEL_DEBUG, this,"DeleteThread finished");
        }

    }

}
