/*  Copyright (c) 2008,2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB), Bjoern Kolbeck (ZIB)
 */

package org.xtreemfs.new_osd.stages;

import java.io.IOException;

import java.util.concurrent.LinkedBlockingQueue;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.interfaces.Exceptions.OSDException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.new_osd.storage.MetadataCache;
import org.xtreemfs.new_osd.storage.StorageLayout;

public class DeletionStage extends Stage {

    public static final int   STAGEOP_DELETE_OBJECTS   = 0;

    private MetadataCache     cache;

    private StorageLayout     layout;

    private OSDRequestDispatcher master;

    private DeleteThread      deletor;

    private long              numFilesDeleted;

    public DeletionStage(OSDRequestDispatcher master, MetadataCache cache,
        StorageLayout layout) {

        super("OSD Deletion Stage");

        this.master = master;
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

    public void deleteObjects(String fileId, OSDRequest request, DeleteObjectsCallback listener) {
        this.enqueueOperation(STAGEOP_DELETE_OBJECTS, new Object[]{fileId}, request, listener);
    }

    /**
     * @return the numFilesDeleted
     */
    public long getNumFilesDeleted() {
        return numFilesDeleted;
    }

    public static interface DeleteObjectsCallback {

        public void deleteComplete(Exception error);
    }

    @Override
    protected void processMethod(StageRequest method) {

        try {
            switch (method.getStageMethod()) {
                case STAGEOP_DELETE_OBJECTS:
                    numFilesDeleted++;
                    processDeleteObjects(method);
                    break;
                default:
                    method.sendInternalServerError(new RuntimeException("unknown stage op request"));
            }

        } catch (Throwable exc) {
            Logging.logMessage(Logging.LEVEL_ERROR, this,exc);
            method.sendInternalServerError(exc);
            return;
        }
    }

    private void processDeleteObjects(StageRequest rq) throws OSDException {

        final DeleteObjectsCallback cback = (DeleteObjectsCallback)rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];

        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "deleting objects of file " + fileId);

        // remove the file info from the storage cache
        cache.removeFileInfo(fileId);

        // otherwise, remove all local objects
        if (layout.fileExists(fileId))
            deletor.enqueueFileForDeletion(fileId);
        cback.deleteComplete(null);
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
                        Logging.logMessage(Logging.LEVEL_DEBUG, this,"deleting objects for "+fileID);
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
