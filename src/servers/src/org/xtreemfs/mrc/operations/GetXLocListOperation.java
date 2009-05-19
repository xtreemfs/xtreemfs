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

import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_listRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_listResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.mrc.volumes.VolumeManager;

/**
 * 
 * @author stender
 */
public class GetXLocListOperation extends MRCOperation {
        
    public GetXLocListOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final xtreemfs_replica_listRequest rqArgs = (xtreemfs_replica_listRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        
        validateContext(rq);
        
        // parse volume and file ID from global file ID
        GlobalFileIdResolver idRes = new GlobalFileIdResolver(rqArgs.getFile_id());
        
        StorageManager sMan = vMan.getStorageManager(idRes.getVolumeId());
        
        FileMetadata file = sMan.getMetadata(idRes.getLocalFileId());
        if (file == null)
            throw new UserException(ErrNo.ENOENT, "file '" + idRes.getLocalFileId() + "' does not exist");
        
        // get the replicas from the X-Loc list
        ReplicaSet replicas = Converter.xLocListToXLocSet(file.getXLocList()).getReplicas();
        
        // set the response
        rq.setResponse(new xtreemfs_replica_listResponse(replicas));
        finishRequest(rq);
    }
    
}
