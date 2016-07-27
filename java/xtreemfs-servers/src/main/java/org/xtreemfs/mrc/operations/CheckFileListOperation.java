/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.util.Iterator;

import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_check_file_existsRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_check_file_existsResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_check_file_existsResponse.FILE_STATE;

public class CheckFileListOperation extends MRCOperation {

    public CheckFileListOperation(MRCRequestDispatcher master) {
        super(master);
    }

    @Override
    public void startRequest(MRCRequest rq) throws Throwable {

        final xtreemfs_check_file_existsRequest rqArgs = (xtreemfs_check_file_existsRequest) rq.getRequestArgs();
        final String osd = rqArgs.getOsdUuid();
        final VolumeManager vMan = master.getVolumeManager();
        StorageManager sMan = null;

        xtreemfs_check_file_existsResponse.Builder response = xtreemfs_check_file_existsResponse.newBuilder();

        // Try to open the requested Volume if it exists.
        try {
            sMan = vMan.getStorageManager(rqArgs.getVolumeId());
            response.setVolumeExists(true);
        } catch (UserException e) {
            response.setVolumeExists(false);
        }

        if (sMan != null && rqArgs.getFileIdsCount() > 0) {

            for (String fileId : rqArgs.getFileIdsList()) {
                if (fileId == null) {
                    throw new MRCException("checkFileList caused an Exception: file ID was null!");
                }

                FileMetadata mData = sMan.getMetadata(Long.parseLong(fileId));
                if (mData == null) {
                    // If no metadata exists, the file has been deleted.
                    response.addFileStates(FILE_STATE.DELETED);
                } else {
                    // Check the xLocations-list of the recognized file.
                    boolean registered = false;
                    XLocList list = mData.getXLocList();

                    if (list != null) {
                        // Try to find the requesting OSD in the XLocList.
                        Iterator<XLoc> iter = list.iterator();
                        XLoc loc;
                        while (iter.hasNext() && !registered) {
                            loc = iter.next();
                            short count = loc.getOSDCount();
                            for (int i = 0; i < count; i++) {
                                // Stop if entry was found.
                                if (loc.getOSD(i).equals(osd)) {
                                    registered = true;
                                    break;
                                }
                            }
                        }
                    }

                    response.addFileStates(registered ? FILE_STATE.REGISTERED : FILE_STATE.ABANDONED);
                }
            }
        }

        // Set the response.
        rq.setResponse(response.build());
        finishRequest(rq);
    }

}
