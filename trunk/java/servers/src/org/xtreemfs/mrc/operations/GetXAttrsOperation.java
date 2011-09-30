/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.DatabaseResultSet;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XAttr;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.mrc.utils.MRCHelper.SysAttrs;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrResponse;

/**
 * 
 * @author stender
 */
public class GetXAttrsOperation extends MRCOperation {
    
    public GetXAttrsOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final listxattrRequest rqArgs = (listxattrRequest) rq.getRequestArgs();
        
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
        
        Map<String, String> attrs = new HashMap<String, String>();
        
        DatabaseResultSet<XAttr> myAttrs = sMan.getXAttrs(file.getId(), rq.getDetails().userId);
        DatabaseResultSet<XAttr> globalAttrs = sMan.getXAttrs(file.getId(), StorageManager.GLOBAL_ID);
        
        // include global attributes
        while (globalAttrs.hasNext()) {
            XAttr attr = globalAttrs.next();
            attrs.put(attr.getKey(), attr.getValue());
        }
        globalAttrs.destroy();
        
        // include individual user attributes
        while (myAttrs.hasNext()) {
            XAttr attr = myAttrs.next();
            attrs.put(attr.getKey(), attr.getValue());
        }
        myAttrs.destroy();
        
        // include system attributes
        for (SysAttrs attr : SysAttrs.values()) {
            String key = "xtreemfs." + attr.toString();
            String value = MRCHelper.getSysAttrValue(master.getConfig(), sMan, master.getOSDStatusManager(),
                faMan, res.toString(), file, attr.toString());
            if (!value.equals(""))
                attrs.put(key, value);
        }
        
        // include policy attributes
        List<String> policyAttrNames = MRCHelper.getPolicyAttrNames(sMan, file.getId());
        for (String attr : policyAttrNames)
            attrs.put(attr, "");
        
        listxattrResponse.Builder result = listxattrResponse.newBuilder();
        Iterator<Entry<String, String>> it = attrs.entrySet().iterator();
        while (it.hasNext()) {
            
            Entry<String, String> attr = it.next();
            org.xtreemfs.pbrpc.generatedinterfaces.MRC.XAttr.Builder builder = org.xtreemfs.pbrpc.generatedinterfaces.MRC.XAttr
                    .newBuilder().setName(attr.getKey());
            
            if (!rqArgs.getNamesOnly())
                builder.setValue(attr.getValue());
            
            result.addXattrs(builder.build());
        }
        
        // set the response
        rq.setResponse(result.build());
        finishRequest(rq);
        
    }
}
