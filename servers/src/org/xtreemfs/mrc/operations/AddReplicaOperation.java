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

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_addRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_addResponse;
import org.xtreemfs.mrc.ErrNo;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.StripingPolicy;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.mrc.volumes.VolumeManager;
import org.xtreemfs.mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class AddReplicaOperation extends MRCOperation {
    
    public static final int OP_ID = 26;
    
    public AddReplicaOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        
        try {
            
            final xtreemfs_replica_addRequest rqArgs = (xtreemfs_replica_addRequest) rq.getRequestArgs();
            
            final FileAccessManager faMan = master.getFileAccessManager();
            final VolumeManager vMan = master.getVolumeManager();
            
            validateContext(rq);
            
            // parse volume and file ID from global file ID
            GlobalFileIdResolver idRes = new GlobalFileIdResolver(rqArgs.getFile_id());
            
            StorageManager sMan = vMan.getStorageManager(idRes.getVolumeId());
            
            // retrieve the file metadata
            FileMetadata file = sMan.getMetadata(idRes.getLocalFileId());
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file '" + rqArgs.getFile_id() + "' does not exist");
            
            // if the file refers to a symbolic link, resolve the link
            String target = sMan.getSoftlinkTarget(file.getId());
            if (target != null) {
                String path = target;
                Path p = new Path(path);
                
                // if the local MRC is not responsible, send a redirect
                if (!vMan.hasVolume(p.getComp(0))) {
                    finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, ErrNo.ENOENT, "link target "
                        + target + " does not exist"));
                    return;
                }
                
                VolumeInfo volume = vMan.getVolumeByName(p.getComp(0));
                sMan = vMan.getStorageManager(volume.getId());
                PathResolver res = new PathResolver(sMan, p);
                file = res.getFile();
            }
            
            if (file.isDirectory())
                throw new UserException(ErrNo.EPERM, "replicas may only be added to files");
            
            // check whether privileged permissions are granted for adding
            // replicas
            faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId, rq.getDetails().superUser,
                rq.getDetails().groupIds);
            
            Replica newRepl = rqArgs.getNew_replica();
            org.xtreemfs.interfaces.StripingPolicy sp = newRepl.getStriping_policy();
            StringSet osds = newRepl.getOsd_uuids();
            
            StripingPolicy sPol = sMan.createStripingPolicy(sp.getType().toString(), sp.getStripe_size(), sp
                    .getWidth());
            
            if (!file.isReadOnly())
                throw new UserException(ErrNo.EPERM,
                    "the file has to be made read-only before adding replicas");
            
            // check whether the new replica relies on a set of OSDs which
            // hasn't been used yet
            XLocList xLocList = file.getXLocList();
            
            if (!MRCHelper.isAddable(xLocList, newRepl.getOsd_uuids()))
                throw new UserException("at least one OSD already used in current X-Locations list '"
                    + Converter.xLocListToXLocSet(xLocList).toString() + "'");
            
            // create a new replica and add it to the client's X-Locations list
            // (this will automatically increment the X-Locations list version)
            XLoc replica = sMan.createXLoc(sPol, osds.toArray(new String[osds.size()]));
            if (xLocList == null)
                xLocList = sMan.createXLocList(new XLoc[] { replica },
                    file.isReadOnly() ? Constants.REPL_UPDATE_PC_RONLY : Constants.REPL_UPDATE_PC_NONE, 1);
            else {
                XLoc[] repls = new XLoc[xLocList.getReplicaCount() + 1];
                for (int i = 0; i < xLocList.getReplicaCount(); i++)
                    repls[i] = xLocList.getReplica(i);
                
                repls[repls.length - 1] = replica;
                xLocList = sMan.createXLocList(repls, xLocList.getReplUpdatePolicy(),
                    xLocList.getVersion() + 1);
            }
            
            file.setXLocList(xLocList);
            
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
            
            // update the X-Locations list
            sMan.setMetadata(file, FileMetadata.XLOC_METADATA, update);
            
            // set the response
            rq.setResponse(new xtreemfs_replica_addResponse());
            
            update.execute();
            
        } catch (UserException exc) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc.getMessage(),
                exc));
        } catch (Throwable exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, "an error has occurred", exc));
        }
    }
    
}
