/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.util.Iterator;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_check_file_existsRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_check_file_existsResponse;

import com.google.protobuf.Message;

/**
 * 
 * @author stender
 */
public class CheckFileListOperation extends MRCOperation {
        
    public CheckFileListOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final xtreemfs_check_file_existsRequest rqArgs = (xtreemfs_check_file_existsRequest) rq
                .getRequestArgs();
        final String osd = rqArgs.getOsdUuid(); 
        final VolumeManager vMan = master.getVolumeManager();
        StorageManager sMan = vMan.getStorageManager(rqArgs.getVolumeId());
        
        String response = sMan == null ? "2" : "";
        if (sMan != null)
            try {
                if (rqArgs.getFileIdsCount() == 0)
                    throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "fileList was empty!");
                for (String fileId : rqArgs.getFileIdsList()) {
                    if (fileId == null)
                        throw new MRCException("file ID was null!");
                    
                    FileMetadata mData = sMan.getMetadata(Long.parseLong(fileId));
                    if (mData == null) response += "0";
                    else {
                        // check the xLocations-list of the recognized file
                        boolean registered = false;
                        XLocList list = mData.getXLocList();
                        if (list==null) {
                            StripingPolicy sp = sMan.createStripingPolicy("RAID0", (int) mData.getSize(), 1);
                            mData.setXLocList(
                                sMan.createXLocList(
                                    new XLoc[] {
                                        sMan.createXLoc(sp, new String[]{osd}, 0)
                                    }, ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE, 0
                                )
                            );
                            registered = true;
                        } else {
                            Iterator<XLoc> iter = list.iterator();
                            XLoc loc;
                            while (iter.hasNext()) {
                                loc = iter.next();
                                short count = loc.getOSDCount();
                                for (int i=0;i<count;i++) {
                                    // stop if entry was found
                                    if (loc.getOSD(i).equals(osd)) {
                                        registered = true;
                                        break;
                                    }
                                }
                                
                                if (registered) break;
                            }
                        }
                        response += (registered) ? "1" : "3";
                    }
                }
            } catch (UserException ue) {
                response = "2";
            } catch (MRCException be) {
                throw new MRCException("checkFileList caused an Exception: " + be.getMessage());
            }
        
        // set the response
        rq.setResponse(xtreemfs_check_file_existsResponse.newBuilder().setBitmap(response).build());
        finishRequest(rq);
    }
    
}
