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

package org.xtreemfs.mrc.operations;

import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;

/**
 * 
 * @author stender
 */
public class SetACLEntriesOperation extends MRCOperation {
    
    public static final int OP_ID = -1;
    
    public SetACLEntriesOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        
        // TODO
        
        // try {
        //            
        // Args rqArgs = (Args) rq.getRequestArgs();
        //            
        // final VolumeManager vMan = master.getVolumeManager();
        // final FileAccessManager faMan = master.getFileAccessManager();
        //            
        // Path p = new Path(rqArgs.path);
        //            
        // VolumeInfo volume = vMan.getVolumeByName(p.getComp(0));
        // StorageManager sMan = vMan.getStorageManager(volume.getId());
        // PathResolver res = new PathResolver(sMan, p);
        //            
        // // check whether the path prefix is searchable
        // faMan.checkSearchPermission(sMan, res, rq.getDetails().userId,
        // rq.getDetails().superUser, rq.getDetails().groupIds);
        //            
        // // check whether file exists
        // res.checkIfFileDoesNotExist();
        //            
        // // retrieve and prepare the metadata to return
        // FileMetadata file = res.getFile();
        //            
        // // if the file refers to a symbolic link, resolve the link
        // String target = sMan.getSoftlinkTarget(file.getId());
        // if (target != null) {
        // rqArgs.path = target;
        // p = new Path(rqArgs.path);
        //                
        // // if the local MRC is not responsible, send a redirect
        // if (!vMan.hasVolume(p.getComp(0))) {
        // finishRequest(rq, new ErrorRecord(ErrorClass.REDIRECT, target));
        // return;
        // }
        //                
        // volume = vMan.getVolumeByName(p.getComp(0));
        // sMan = vMan.getStorageManager(volume.getId());
        // res = new PathResolver(sMan, p);
        // file = res.getFile();
        // }
        //            
        // // check whether the access mode may be changed
        // faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId,
        // rq.getDetails().superUser, rq.getDetails().groupIds);
        //            
        // AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        //            
        // // change the ACL
        // faMan.setACLEntries(sMan, file, res.getParentDirId(),
        // rq.getDetails().userId, rq
        // .getDetails().groupIds, rqArgs.entries, update);
        //            
        // // FIXME: this line is needed due to a BUG in the client which
        // // expects some useless return value
        //rq.setData(ReusableBuffer.wrap(JSONParser.writeJSON(null).getBytes()))
        // ;
        //            
        // // update POSIX timestamps
        // MRCHelper.updateFileTimes(res.getParentDirId(), file, false, true,
        // false, sMan,
        // update);
        //            
        // update.execute();
        //            
        // } catch (UserException exc) {
        // Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
        // finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION,
        // exc.getErrno(), exc
        // .getMessage(), exc));
        // } catch (Exception exc) {
        // finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR,
        // "an error has occurred", exc));
        // }
    }
    
    
}
