/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.ec;

import org.xtreemfs.foundation.intervals.AVLTreeIntervalVector;
import org.xtreemfs.foundation.intervals.IntervalVector;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.Stage.StageRequest;
import org.xtreemfs.osd.stages.StorageStage.ECGetVectorsCallback;
import org.xtreemfs.osd.storage.FileMetadata;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.StorageLayout;

/**
 * This class contains the methods regarding EC data and IntervalVector handling. <br>
 * For sake of clarity the methods are separated to this class. <br>
 * Since the IntervalVectors are tightly coupled to the data integrity they have to be handled in the same stage to
 * reduce the chance of failures and inconsistencies.<br>
 * Unfortunately this means, that the possibly expensive IntervalVector calculations and the expensive encoding
 * operations are also run in the StorageStage. If profiling shows their impact those methods should be moved to a
 * separate stage.
 */
public class ECStorage {
    private final MetadataCache        cache;
    private final StorageLayout        layout;
    private final OSDRequestDispatcher master;
    private final boolean              checksumsEnabled;

    public ECStorage(OSDRequestDispatcher master, MetadataCache cache, StorageLayout layout, boolean checksumsEnabled) {
        this.master = master;
        this.cache = cache;
        this.layout = layout;
        this.checksumsEnabled = checksumsEnabled;
    }

    public void processGetVectors(final StageRequest rq) {
        final ECGetVectorsCallback cback = (ECGetVectorsCallback) rq.getCallback();
        final String fileId = (String) rq.getArgs()[0];

        FileMetadata fi = cache.getFileInfo(fileId);
        if (fi == null) {
            try {
                IntervalVector curVector = new AVLTreeIntervalVector();
                layout.getECIntervalVector(fileId, false, curVector);

                IntervalVector nextVector = new AVLTreeIntervalVector();
                layout.getECIntervalVector(fileId, true, nextVector);

                cback.ecGetVectorsComplete(curVector, nextVector, null);

            } catch (Exception ex) {
                cback.ecGetVectorsComplete(null, null,
                        ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EIO, ex.toString()));
            }
        } else {
            cback.ecGetVectorsComplete(fi.getECCurVector(), fi.getECNextVector(), null);
        }
    }

}
