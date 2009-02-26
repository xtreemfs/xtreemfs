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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.UserException;
import org.xtreemfs.new_mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.new_mrc.ac.FileAccessManager;
import org.xtreemfs.new_mrc.database.StorageManager;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.new_mrc.metadata.XAttr;
import org.xtreemfs.new_mrc.metadata.XLocList;
import org.xtreemfs.new_mrc.utils.MRCHelper;
import org.xtreemfs.new_mrc.utils.Path;
import org.xtreemfs.new_mrc.utils.PathResolver;
import org.xtreemfs.new_mrc.utils.MRCHelper.SysAttrs;
import org.xtreemfs.new_mrc.volumes.VolumeManager;
import org.xtreemfs.new_mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class StatOperation extends MRCOperation {
    
    static class Args {
        
        public String  path;
        
        public boolean inclReplicas;
        
        public boolean inclXAttrs;
        
        public boolean inclACLs;
        
    }
    
    public static final String RPC_NAME = "stat";
    
    public StatOperation(MRCRequestDispatcher master) {
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
            
            final Path p = new Path(rqArgs.path);
            
            final VolumeInfo volume = vMan.getVolumeByName(p.getComp(0));
            final StorageManager sMan = vMan.getStorageManager(volume.getId());
            final PathResolver res = new PathResolver(sMan, p);
            
            // check whether the path prefix is searchable
            faMan.checkSearchPermission(sMan, res, rq.getDetails().userId,
                rq.getDetails().superUser, rq.getDetails().groupIds);
            
            // check whether file exists
            res.checkIfFileDoesNotExist();
            
            // retrieve and prepare the metadata to return
            FileMetadata file = res.getFile();
            String ref = sMan.getSoftlinkTarget(file.getId());
            XLocList xLocList = !file.isDirectory() && rqArgs.inclReplicas ? file.getXLocList()
                : null;
            
            Map<String, Object> xAttrs = null;
            if (rqArgs.inclXAttrs) {
                
                Iterator<XAttr> myAttrs = sMan.getXAttrs(file.getId(), rq.getDetails().userId);
                Iterator<XAttr> globalAttrs = sMan
                        .getXAttrs(file.getId(), StorageManager.GLOBAL_ID);
                
                // include global attributes
                xAttrs = new HashMap<String, Object>();
                while (globalAttrs.hasNext()) {
                    XAttr attr = globalAttrs.next();
                    xAttrs.put(attr.getKey(), attr.getValue());
                }
                
                // include individual user attributes
                while (myAttrs.hasNext()) {
                    XAttr attr = myAttrs.next();
                    xAttrs.put(attr.getKey(), attr.getValue());
                }
                
                // include system attributes
                for (SysAttrs attr : SysAttrs.values()) {
                    String key = "xtreemfs." + attr.toString();
                    Object value = MRCHelper.getSysAttrValue(master.getConfig(), sMan, master
                            .getOSDStatusManager(), volume, res.toString(), file, attr.toString());
                    if (!value.equals(""))
                        xAttrs.put(key, value);
                }
            }
            
            Map<String, Object> acl = rqArgs.inclACLs ? faMan.getACLEntries(sMan, file) : null;
            
            Object statInfo = MRCHelper.createStatInfo(sMan, faMan, file, ref,
                rq.getDetails().userId, rq.getDetails().groupIds, xLocList, xAttrs, acl);
            
            rq.setData(ReusableBuffer.wrap(JSONParser.writeJSON(statInfo).getBytes()));
            finishRequest(rq);
            
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
            
            args.path = (String) arguments.get(0);
            args.inclReplicas = (Boolean) arguments.get(1);
            args.inclXAttrs = (Boolean) arguments.get(2);
            args.inclACLs = (Boolean) arguments.get(3);
            
            if (arguments.size() == 4)
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
