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

package org.xtreemfs.new_mrc.operations;

import java.util.List;
import java.util.Map;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.new_mrc.ErrNo;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.UserException;
import org.xtreemfs.new_mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.new_mrc.ac.FileAccessManager;
import org.xtreemfs.new_mrc.database.AtomicDBUpdate;
import org.xtreemfs.new_mrc.database.StorageManager;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.new_mrc.metadata.StripingPolicy;
import org.xtreemfs.new_mrc.metadata.XLoc;
import org.xtreemfs.new_mrc.metadata.XLocList;
import org.xtreemfs.new_mrc.utils.Converter;
import org.xtreemfs.new_mrc.utils.MRCOpHelper;
import org.xtreemfs.new_mrc.utils.Path;
import org.xtreemfs.new_mrc.utils.PathResolver;
import org.xtreemfs.new_mrc.volumes.VolumeManager;
import org.xtreemfs.new_mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class AddReplicaOperation extends MRCOperation {
    
    static class Args {
        
        public String              fileId;
        
        public Map<String, Object> stripingPolicy;
        
        public List<Object>        osdList;
        
    }
    
    public static final String RPC_NAME = "addReplica";
    
    public AddReplicaOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public boolean hasArguments() {
        return true;
    }
    
    @Override
    public boolean isAuthRequired() {
        return true;
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        
        try {
            
            Args rqArgs = (Args) rq.getRequestArgs();
            
            final FileAccessManager faMan = master.getFileAccessManager();
            final VolumeManager vMan = master.getVolumeManager();
            
            // parse volume and file ID from global file ID
            long fileId = 0;
            String volumeId = null;
            try {
                String globalFileId = rqArgs.fileId;
                int i = globalFileId.indexOf(':');
                volumeId = rqArgs.fileId.substring(0, i);
                fileId = Long.parseLong(rqArgs.fileId.substring(i + 1));
            } catch (Exception exc) {
                throw new UserException("invalid global file ID: " + rqArgs.fileId
                    + "; expected pattern: <volume_ID>:<local_file_ID>");
            }
            
            StorageManager sMan = vMan.getStorageManager(volumeId);
            
            // retrieve the file metadata
            FileMetadata file = sMan.getMetadata(fileId);
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file '" + fileId + "' does not exist");
            
            // if the file refers to a symbolic link, resolve the link
            String target = sMan.getSoftlinkTarget(file.getId());
            if (target != null) {
                String path = target;
                Path p = new Path(path);
                
                // if the local MRC is not responsible, send a redirect
                if (!vMan.hasVolume(p.getComp(0))) {
                    finishRequest(rq, new ErrorRecord(ErrorClass.REDIRECT, target));
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
            faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId,
                rq.getDetails().superUser, rq.getDetails().groupIds);
            
            // check whether a striping policy is explicitly assigned to the
            // replica; if not, use the one from the file; if none is assigned
            // to the file either, use the one from the volume; if the volume
            // does not have a default striping policy, throw an exception
            StripingPolicy sPol = Converter.mapToStripingPolicy(sMan, rqArgs.stripingPolicy);
            if (sPol == null)
                sPol = sMan.getDefaultStripingPolicy(file.getId());
            if (sPol == null)
                sPol = sMan.getDefaultStripingPolicy(1);
            if (sPol == null)
                throw new UserException(ErrNo.EPERM,
                    "either the replica, the file or the volume need a striping policy");
            
            if (!file.isReadOnly())
                throw new UserException(ErrNo.EPERM,
                    "the file has to be made read-only before adding replicas");
            
            // check whether the new replica relies on a set of OSDs which
            // hasn't been used yet
            XLocList xLocList = file.getXLocList();
            
            if (!MRCOpHelper.isAddable(xLocList, rqArgs.osdList))
                throw new UserException(
                    "at least one OSD already used in current X-Locations list '"
                        + JSONParser.writeJSON(Converter.xLocListToList(xLocList)) + "'");
            
            // create a new replica and add it to the client's X-Locations list
            // (this will automatically increment the X-Locations list version)
            XLoc replica = sMan.createXLoc(sPol, rqArgs.osdList.toArray(new String[rqArgs.osdList
                    .size()]));
            if (xLocList == null)
                xLocList = sMan.createXLocList(new XLoc[] { replica }, 1);
            else {
                XLoc[] repls = new XLoc[xLocList.getReplicaCount() + 1];
                for (int i = 0; i < xLocList.getReplicaCount(); i++)
                    repls[i] = xLocList.getReplica(i);
                
                repls[repls.length - 1] = replica;
                xLocList = sMan.createXLocList(repls, xLocList.getVersion() + 1);
            }
            
            file.setXLocList(xLocList);
            
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
            
            // update the X-Locations list
            sMan.setMetadata(file, FileMetadata.XLOC_METADATA, update);
            
            // FIXME: this line is needed due to a BUG in the client which
            // expects some useless return value
            rq.setData(ReusableBuffer.wrap(JSONParser.writeJSON(null).getBytes()));
            
            update.execute();
            
        } catch (UserException exc) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc
                    .getMessage(), exc));
        } catch (Exception exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR,
                "an error has occurred", exc));
        }
    }
    
    @Override
    public ErrorRecord parseRPCBody(MRCRequest rq, List<Object> arguments) {
        
        Args args = new Args();
        
        try {
            
            args.fileId = (String) arguments.get(0);
            args.stripingPolicy = (Map<String, Object>) arguments.get(1);
            args.osdList = (List<Object>) arguments.get(2);
            
            if (arguments.size() == 3)
                return null;
            
            throw new Exception();
            
        } catch (Exception exc) {
            try {
                return new ErrorRecord(ErrorClass.BAD_REQUEST, "invalid arguments for operation '"
                    + getClass().getSimpleName() + "': " + JSONParser.writeJSON(arguments));
            } catch (JSONException je) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
                return new ErrorRecord(ErrorClass.BAD_REQUEST, "invalid arguments for operation '"
                    + getClass().getSimpleName() + "'");
            }
        } finally {
            rq.setRequestArgs(args);
        }
    }
    
}
