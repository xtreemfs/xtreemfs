/*
 * Copyright (c) 2009-2014 by Bjoern Kolbeck, Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.DeletionStage.DeleteObjectsCallback;
import org.xtreemfs.osd.stages.StorageStage.CachesFlushedCallback;
import org.xtreemfs.osd.stages.StorageStage.CreateFileVersionCallback;
import org.xtreemfs.osd.storage.FileMetadata;

/**
 * EventCloseFile flushes the internal metadata caches, creates a new file version if versioning / COW is enabled
 * and deletes the file along with its objects if the file has been marked for deletion. <br>
 * 
 * It is not responsible for removing files from the open file table (which has to be ensured previously) and doesn't
 * disseminate to other replicas.
 */
public class EventCloseFile extends OSDOperation {

    public EventCloseFile(OSDRequestDispatcher master) {
        super(master);
    }

    public interface EventCloseCallback {
        void closeEventResult(ErrorResponse error);
    }

    /**
     * @param args
     *            [ fileId, deleteOnClose, cowEnabled, createVersion, {@link EventCloseCallback} (optional) ]
     */
    @Override
    public void startInternalEvent(Object[] args) {
        final String fileId = (String) args[0];
        final Boolean deleteOnClose = (Boolean) args[1];
        final Boolean cowEnabled = (Boolean) args[2];
        final Boolean createVersion = (Boolean) args[3];

        final EventCloseCallback cb = args.length > 4 ? (EventCloseCallback) args[4] : null;

        flushCaches(fileId, deleteOnClose, cowEnabled, createVersion, cb);
    }

    public void flushCaches(final String fileId, final boolean deleteOnClose, final boolean cowEnabled,
            final boolean createVersion, final EventCloseCallback cb) {
        master.getStorageStage().flushCaches(fileId, new CachesFlushedCallback() {
            @Override
            public void cachesFlushed(ErrorResponse error, FileMetadata fi) {
                createVersionIfNecessary(fileId, deleteOnClose, cowEnabled, createVersion, fi, cb, error);
            }

        });

        // Asynchronously report close operation to read-write replication stage.
        master.getRWReplicationStage().fileClosed(fileId);
    }

    public void createVersionIfNecessary(final String fileId, final boolean deleteOnClose, final boolean cowEnabled,
            final boolean createVersion, final FileMetadata fi, final EventCloseCallback cb, final ErrorResponse error) {

        if (error != null) {
            finishEvent(cb, error);
            return;
        }

        // If versioning on close is enabled, create a new version.
        if (cowEnabled && createVersion) {

            master.getStorageStage().createFileVersion(fileId, fi, null, new CreateFileVersionCallback() {

                @Override
                public void createFileVersionComplete(long fileSize, ErrorResponse error) {
                    deleteIfNecessary(fileId, deleteOnClose, cowEnabled, fi, cb, error);
                }
            });

        } else {
            deleteIfNecessary(fileId, deleteOnClose, cowEnabled, fi, cb, null);
        }
    }

    public void deleteIfNecessary(final String fileId, final boolean deleteOnClose, final boolean cowEnabled,
            final FileMetadata fi, final EventCloseCallback cb, final ErrorResponse error) {

        if (error != null) {
            finishEvent(cb, error);
            return;
        }

        if (deleteOnClose) {

            // Cancel replication of file.
            master.getReplicationStage().cancelReplicationForFile(fileId);

            // Delete the file across all stripes.
            master.getDeletionStage().deleteObjects(fileId, fi, cowEnabled, null, false, new DeleteObjectsCallback() {

                @Override
                public void deleteComplete(ErrorResponse error) {
                    finishEvent(cb, error);
                }
            });

        } else {
            finishEvent(cb, null);
        }
    }

    public void finishEvent(final EventCloseCallback cb, final ErrorResponse error) {
        if (cb != null) {
            cb.closeEventResult(error);

        } else if (error != null) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "exception in internal close event: %s",
                    ErrorUtils.formatError(error));
        }
    }

    @Override
    public int getProcedureId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void startRequest(OSDRequest rq) {
        throw new UnsupportedOperationException("Not supported yet.");

    }

    @Override
    public boolean requiresCapability() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
