/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getxattrRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.getxattrResponse;

import com.google.protobuf.ByteString;

/**
 * 
 * @author stender
 */
public class GetXAttrOperation extends MRCOperation {
    
    public GetXAttrOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final getxattrRequest rqArgs = (getxattrRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        Path p = new Path(rqArgs.getVolumeName(), rqArgs.getPath());
        
        StorageManager sMan = vMan.getStorageManagerByName(p.getComp(0));
        PathResolver res = new PathResolver(sMan, p);
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether file exists
        res.checkIfFileDoesNotExist();
        
        // retrieve and prepare the metadata to return
        FileMetadata file = res.getFile();
        
        byte[] value = null;
        if (rqArgs.getName().startsWith("xtreemfs."))
            value = MRCHelper.getSysAttrValue(master.getConfig(), sMan, master.getOSDStatusManager(), faMan,
                res.toString(), file, rqArgs.getName().substring(9)).getBytes();
        else {
            
            // first, try to fetch an individual user attribute
            value = sMan.getXAttr(file.getId(), rq.getDetails().userId, rqArgs.getName());
            
            // if no such attribute exists, try to fetch a global attribute
            if (value == null)
                value = sMan.getXAttr(file.getId(), StorageManager.GLOBAL_ID, rqArgs.getName());
        }
        
        if (value == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENODATA);
        
        // set the response
        rq.setResponse(getxattrResponse.newBuilder().setValue(new String(value))
                .setValueBytes(ByteString.copyFrom(value)).build());
        finishRequest(rq);
    }
    
}
