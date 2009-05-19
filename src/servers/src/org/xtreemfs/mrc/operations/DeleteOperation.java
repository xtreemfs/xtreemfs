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

import java.net.InetSocketAddress;

import org.xtreemfs.common.Capability;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.interfaces.FileCredentials;
import org.xtreemfs.interfaces.FileCredentialsSet;
import org.xtreemfs.interfaces.MRCInterface.rmdirRequest;
import org.xtreemfs.interfaces.MRCInterface.unlinkRequest;
import org.xtreemfs.interfaces.MRCInterface.unlinkResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.mrc.volumes.VolumeManager;
import org.xtreemfs.mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class DeleteOperation extends MRCOperation {
        
    public DeleteOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        final Path p = new Path(rq.getRequestArgs() instanceof unlinkRequest ? ((unlinkRequest) rq
                .getRequestArgs()).getPath() : ((rmdirRequest) rq.getRequestArgs()).getPath());
        
        final VolumeInfo volume = vMan.getVolumeByName(p.getComp(0));
        final StorageManager sMan = vMan.getStorageManager(volume.getId());
        final PathResolver res = new PathResolver(sMan, p);
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether the parent directory grants write access
        faMan.checkPermission(FileAccessManager.O_WRONLY, sMan, res.getParentDir(), 0,
            rq.getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
        
        // check whether the file/directory exists
        res.checkIfFileDoesNotExist();
        
        FileMetadata file = res.getFile();
        
        // check whether the entry itself can be deleted (this is e.g.
        // important w/ POSIX access control if the sticky bit is set)
        faMan.checkPermission(FileAccessManager.NON_POSIX_RM_MV_IN_DIR, sMan, file, res.getParentDirId(), rq
                .getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
        
        if (file.isDirectory() && sMan.getChildren(file.getId()).hasNext())
            throw new UserException(ErrNo.ENOTEMPTY, "'" + p + "' is not empty");
        
        FileCredentialsSet creds = new FileCredentialsSet();
        
        // unless the file is a directory, retrieve X-headers for file
        // deletion on OSDs; if the request was authorized before,
        // assume that a capability has been issued already.
        if (!file.isDirectory()) {
            
            // create a deletion capability for the file
            Capability cap = new Capability(volume.getId() + ":" + file.getId(),
                FileAccessManager.NON_POSIX_DELETE, Integer.MAX_VALUE, ((InetSocketAddress) rq
                        .getRPCRequest().getClientIdentity()).getAddress().getHostAddress(), file.getEpoch(),
                master.getConfig().getCapabilitySecret());
            
            // set the XCapability and XLocationsList headers
            XLocList xloc = file.getXLocList();
            if (xloc != null && xloc.getReplicaCount() > 0)
                creds.add(new FileCredentials(Converter.xLocListToXLocSet(xloc), cap.getXCap()));
        }
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // unlink the file; if there are still links to the file, reset the
        // X-headers to null, as the file content must not be deleted
        sMan.delete(res.getParentDirId(), res.getFileName(), update);
        if (file.getLinkCount() > 1)
            creds.clear();
        
        // update POSIX timestamps of parent directory
        MRCHelper.updateFileTimes(res.getParentsParentId(), res.getParentDir(), false, true, true, sMan,
            update);
        
        if (file.getLinkCount() > 1)
            MRCHelper.updateFileTimes(res.getParentDirId(), file, false, true, false, sMan, update);
        
        // set the response
        rq.setResponse(new unlinkResponse(creds));
        
        update.execute();
    }
    
}
