/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.util.HashMap;
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
import org.xtreemfs.mrc.utils.MRCHelper.SysAttrs;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.listxattrResponse;

import com.google.protobuf.ByteString;

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
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser,
                rq.getDetails().groupIds);
        
        // check whether file exists
        res.checkIfFileDoesNotExist();
        
        // retrieve and prepare the metadata to return
        FileMetadata file = res.getFile();
        
        Map<String, byte[]> attrs = new HashMap<String, byte[]>();
        
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
            String value = MRCHelper.getSysAttrValue(master.getConfig(), sMan, master.getOSDStatusManager(), faMan,
                    res.toString(), file, attr.toString());
            if (!value.equals(""))
                attrs.put(key, value.getBytes());
        }
        
        // if file ID is root volume
        if (file.getId() == 1) {
            
            // include policy attributes
            List<String> policyAttrNames = MRCHelper.getSpecialAttrNames(sMan, MRCHelper.POLICY_ATTR_PREFIX);
            for (String attr : policyAttrNames)
                attrs.put(attr, sMan.getXAttr(1, StorageManager.SYSTEM_UID, attr));
            
            // include volume attributes
            List<String> volAttrAttrNames = MRCHelper.getSpecialAttrNames(sMan, MRCHelper.VOL_ATTR_PREFIX);
            for (String attr : volAttrAttrNames)
                attrs.put(attr, sMan.getXAttr(1, StorageManager.SYSTEM_UID, attr));
        }
        
        listxattrResponse.Builder result = listxattrResponse.newBuilder();
        for (Entry<String, byte[]> attr : attrs.entrySet()) {
            
            org.xtreemfs.pbrpc.generatedinterfaces.MRC.XAttr.Builder builder = org.xtreemfs.pbrpc.generatedinterfaces.MRC.XAttr
                    .newBuilder().setName(attr.getKey());
            
            if (!rqArgs.getNamesOnly())
                builder.setValue(new String(attr.getValue())).setValueBytes(ByteString.copyFrom(attr.getValue()));
            
            result.addXattrs(builder.build());
        }
        
        // set the response
        rq.setResponse(result.build());
        finishRequest(rq);
        
    }
}
