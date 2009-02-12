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
import java.util.Map.Entry;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.UserException;
import org.xtreemfs.new_mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.new_mrc.ac.FileAccessManager;
import org.xtreemfs.new_mrc.dbaccess.AtomicDBUpdate;
import org.xtreemfs.new_mrc.dbaccess.StorageManager;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.new_mrc.metadata.XLocList;
import org.xtreemfs.new_mrc.operations.MRCOpHelper.AccessMode;
import org.xtreemfs.new_mrc.utils.Converter;
import org.xtreemfs.new_mrc.volumes.VolumeManager;
import org.xtreemfs.new_mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class CreateFileOperation extends MRCOperation {
    
    static class Args {
        
        public String              filePath;
        
        public Map<String, Object> xAttrs;
        
        public Map<String, Object> stripingPolicy;
        
        public short               mode;
        
        public boolean             open;
        
        public List<Object>        assignedXLocList;
        
    }
    
    public static final String RPC_NAME = "createFile";
    
    public CreateFileOperation(MRCRequestDispatcher master) {
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
            
            final VolumeManager vMan = master.getVolumeManager();
            final FileAccessManager faMan = master.getFileAccessManager();
            
            final Path p = new Path(rqArgs.filePath);
            
            final VolumeInfo volume = vMan.getVolumeByName(p.getComp(0));
            final StorageManager sMan = vMan.getStorageManager(volume.getId());
            final PathResolver res = new PathResolver(sMan, p);
            
            // check whether the path prefix is searchable
            faMan.checkSearchPermission(sMan, res, rq.getDetails().userId,
                rq.getDetails().superUser, rq.getDetails().groupIds);
            
            // check whether the parent directory grants write access
            faMan.checkPermission(FileAccessManager.WRITE_ACCESS, sMan, res.getParentDir(), res
                    .getParentsParentId(), rq.getDetails().userId, rq.getDetails().superUser, rq
                    .getDetails().groupIds);
            
            // check whether the file/directory exists already
            res.checkIfFileExistsAlready();
            
            // prepare file creation in database
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
            
            // get the next free file ID
            long fileId = sMan.getNextFileId();
            
            // atime, ctime, mtime
            int time = (int) (TimeSync.getGlobalTime() / 1000);
            
            // create the metadata object
            FileMetadata file = sMan.createFile(fileId, res.getParentDirId(), res.getFileName(),
                time, time, time, rq.getDetails().userId, rq.getDetails().groupIds.get(0),
                rqArgs.mode, 0, 0, false, 0, 0, update);
            
            // set the file ID as the last one
            sMan.setLastFileId(fileId, update);
            
            if (rqArgs.stripingPolicy != null)
                sMan.setDefaultStripingPolicy(fileId, Converter.mapToStripingPolicy(sMan,
                    rqArgs.stripingPolicy), update);
            
            // create the user attributes
            if (rqArgs.xAttrs != null) {
                for (Entry<String, Object> attr : rqArgs.xAttrs.entrySet())
                    sMan.setXAttr(file.getId(), rq.getDetails().userId, attr.getKey(), attr
                            .getValue().toString(), update);
            }
            
            // if O_CREAT flag is set ...
            if (rqArgs.open) {
                
                // create a capability for O_CREAT open calls
                String capability = MRCOpHelper.createCapability(AccessMode.w.toString(),
                    volume.getId(), file.getId(), 0, master.getConfig().getCapabilitySecret())
                        .toString();
                
                // get the list of replicas associated with the file
                XLocList xLocList = file.getXLocList();
                
                // if no replica exists yet, create one using the default
                // striping policy together with a set of feasible OSDs from the
                // OSD status manager
                if (xLocList == null || !xLocList.iterator().hasNext()) {
                    
                    xLocList = MRCOpHelper.createXLocList(Converter.mapToStripingPolicy(sMan,
                        rqArgs.stripingPolicy), xLocList, sMan, master.getOSDStatusManager(), res
                            .toString(), file.getId(), res.getParentDirId(), volume, rq
                            .getPinkyRequest().getClientAddress());
                    
                    file.setXLocList(xLocList);
                    
                    sMan.setMetadata(file, FileMetadata.XLOC_METADATA, update);
                }
                
                HTTPHeaders headers = MRCOpHelper.createXCapHeaders(capability, xLocList);
                rq.setAdditionalResponseHeaders(headers);
                
                // update POSIX timestamps of file
                MRCOpHelper.updateFileTimes(res.getParentsParentId(), file, !master.getConfig()
                        .isNoAtime(), true, true, sMan, update);
                
                // update POSIX timestamps of parent directory
                MRCOpHelper.updateFileTimes(res.getParentsParentId(), res.getParentDir(), false,
                    true, true, sMan, update);
                
            }
            
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
            
            args.filePath = (String) arguments.get(0);
            if (arguments.size() == 1) {
                args.mode = 511;
                return null;
            }
            
            args.xAttrs = (Map<String, Object>) arguments.get(1);
            args.stripingPolicy = (Map<String, Object>) arguments.get(2);
            args.mode = ((Long) arguments.get(3)).shortValue();
            if (arguments.size() == 4)
                return null;
            
            args.open = (Boolean) arguments.get(4);
            if (arguments.size() == 5)
                return null;
            
            args.assignedXLocList = (List<Object>) arguments.get(5);
            if (arguments.size() == 6)
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
