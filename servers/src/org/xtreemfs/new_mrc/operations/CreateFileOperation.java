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

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.mrc.brain.ErrNo;
import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.new_mrc.ac.FileAccessManager;
import org.xtreemfs.new_mrc.dbaccess.AtomicDBUpdate;
import org.xtreemfs.new_mrc.dbaccess.StorageManager;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
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
    public void startRequest(MRCRequest rq) {
        
        try {
            
            Args rqArgs = (Args) rq.getRequestArgs();
            
            final VolumeManager vMan = master.getVolumeManager();
            final FileAccessManager faMan = master.getFileAccessManager();
            final Path p = new Path(rqArgs.filePath);
            
            final VolumeInfo volume = vMan.getVolumeByName(p.getComp(0));
            final StorageManager sMan = vMan.getStorageManager(volume.getId());
            
            // retrieve parent directory
            final String fileName = p.getLastComp(0);
            final String parentDirName = p.getCompCount() == 2 ? "" : p.getLastComp(1);
            final long parentsParentId = sMan.resolvePath(p.getComps(1, p.getCompCount() - 2));
            final FileMetadata parentDir = sMan.getMetadata(parentsParentId, parentDirName);
            
            // check whether the parent directory is searchable
            faMan.checkSearchPermission(volume.getId(), p.getComps(1, p.getCompCount() - 1), rq
                    .getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
            
            // check whether the parent directory grants write access
            faMan.checkPermission(FileAccessManager.WRITE_ACCESS, volume.getId(),
                parentDir.getId(), 0, rq.getDetails().userId, rq.getDetails().superUser, rq
                        .getDetails().groupIds);
            
            if (p.getComps(1, p.getCompCount() - 1).length() == 0
                || sMan.getMetadata(parentDir.getId(), fileName) != null)
                throw new UserException(ErrNo.EEXIST, "file or directory '" + rqArgs.filePath
                    + "' already exists");
            
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
            
            // create the metadata object
            FileMetadata file = sMan.create(parentDir.getId(), fileName, rq.getDetails().userId, rq
                    .getDetails().groupIds.get(0), rqArgs.stripingPolicy, rqArgs.mode, null, false,
                update);
            
            // create the user attributes
            for (Entry<String, Object> attr : rqArgs.xAttrs.entrySet())
                sMan.setXAttr(file.getId(), rq.getDetails().userId, attr.getKey(), attr.getValue()
                        .toString(), update);
            
            // TODO: handle open flag
            // HTTPHeaders headers = null;
            //            
            //            
            // if (open) {
            // // create a capability for O_CREAT open calls
            // String capability =
            // BrainHelper.createCapability(AccessMode.w.toString(),
            // volume.getId(), file.getId(), 0,
            // config.getCapabilitySecret()).toString();
            //                
            // XLocationsList xLocList = null;
            // if (assignedXLocList == null) {
            // // assign a new list
            // xLocList = BrainHelper.createXLocList(null, sMan, osdMan, p,
            // file.getId(),
            // parentDir.getId(), volume,
            // rq.getPinkyRequest().getClientAddress());
            // } else {
            // // log replay, use assigned list
            // xLocList = Converter.listToXLocList(assignedXLocList);
            // }
            //                
            // // assign the OSDs
            // file.setXLocList(xLocList);
            // if (Logging.isDebug())
            // Logging.logMessage(Logging.LEVEL_DEBUG, this,
            // "assigned xloc list to " + p
            // + ": " + xLocList);
            //                
            // headers = BrainHelper.createXCapHeaders(capability, xLocList);
            //                
            // if (assignedXLocList == null) {
            // // not necessary when in log replay mode!
            // // rewrite body
            // // prepare the request for the log replay
            // List<Object> args = new ArrayList<Object>(5);
            // args.add(filePath);
            // args.add(xAttrs);
            // args.add(stripingPolicy);
            // args.add(mode);
            // args.add(true);
            // args.add(Converter.xLocListToList(xLocList));
            //                    
            // ReusableBuffer body =
            // ReusableBuffer.wrap(JSONParser.writeJSON(args).getBytes(
            // HTTPUtils.ENC_UTF8));
            // }
            // }
            
            // update POSIX timestamps of parent directory
            MRCOpHelper
                    .updateFileTimes(parentsParentId, parentDir, false, true, true, sMan, update);
            
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
            if (arguments.size() == 1)
                return null;
            
            args.xAttrs = (Map<String, Object>) arguments.get(1);
            args.stripingPolicy = (Map<String, Object>) arguments.get(2);
            args.mode = (Short) arguments.get(3);
            if (arguments.size() == 4)
                return null;
            
            boolean open = (Boolean) arguments.get(4);
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
