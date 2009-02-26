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

package org.xtreemfs.mrc.ac;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.PolicyContainer;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.mrc.volumes.VolumeManager;

/**
 * This class is responsible for checking policy-based file access.
 * 
 * @author stender
 * 
 */
public class FileAccessManager {
    
    public static final int                    READ_ACCESS         = 1;
    
    public static final int                    SEARCH_ACCESS       = 2;
    
    public static final int                    WRITE_ACCESS        = 3;
    
    public static final int                    DELETE_ACCESS       = 4;
    
    public static final int                    RM_MV_IN_DIR_ACCESS = 5;
    
    private final VolumeManager                volMan;
    
    private final Map<Short, FileAccessPolicy> policies;
    
    private PolicyContainer                    policyContainer;
    
    public FileAccessManager(VolumeManager volMan, PolicyContainer policyContainer) {
        
        this.volMan = volMan;
        this.policyContainer = policyContainer;
        
        policies = new HashMap<Short, FileAccessPolicy>();
        policies.put(POSIXFileAccessPolicy.POLICY_ID, new POSIXFileAccessPolicy());
        policies.put(YesToAnyoneFileAccessPolicy.POLICY_ID, new YesToAnyoneFileAccessPolicy());
        policies.put(VolumeACLFileAccessPolicy.POLICY_ID, new VolumeACLFileAccessPolicy());
    }
    
    public void checkSearchPermission(StorageManager sMan, PathResolver path, String userId,
        boolean superUser, List<String> groupIds) throws UserException, MRCException {
        
        if (superUser)
            return;
        
        getVolumeFileAccessPolicy(sMan.getVolumeId()).checkSearchPermission(sMan, path, userId,
            groupIds);
    }
    
    public void checkPrivilegedPermissions(StorageManager sMan, FileMetadata file, String userId,
        boolean superUser, List<String> groupIds) throws UserException, MRCException {
        
        if (superUser)
            return;
        
        getVolumeFileAccessPolicy(sMan.getVolumeId()).checkPrivilegedPermissions(sMan, file,
            userId, groupIds);
    }
    
    public void checkPermission(int accessMode, StorageManager sMan, FileMetadata file,
        long parentDirId, String userId, boolean superUser, List<String> groupIds)
        throws UserException, MRCException {
        
        checkPermission(translateAccessMode(sMan.getVolumeId(), accessMode), sMan, file,
            parentDirId, userId, superUser, groupIds);
    }
    
    public void checkPermission(String accessMode, StorageManager sMan, FileMetadata file,
        long parentDirId, String userId, boolean superUser, List<String> groupIds)
        throws UserException, MRCException {
        
        if (superUser)
            return;
        
        getVolumeFileAccessPolicy(sMan.getVolumeId()).checkPermission(sMan, file, parentDirId,
            userId, groupIds, accessMode);
    }
    
    public String translateAccessMode(String volumeId, int accessMode) throws MRCException {
        return getVolumeFileAccessPolicy(volumeId).translateAccessMode(accessMode);
    }
    
    public int getPosixAccessMode(StorageManager sMan, FileMetadata file, String userId,
        List<String> groupIds) throws MRCException {
        return getVolumeFileAccessPolicy(sMan.getVolumeId()).getPosixAccessRights(sMan, file,
            userId, groupIds);
    }
    
    public void setPosixAccessMode(StorageManager sMan, FileMetadata file, long parentId,
        String userId, List<String> groupIds, short posixRights, AtomicDBUpdate update)
        throws MRCException, UserException {
        getVolumeFileAccessPolicy(sMan.getVolumeId()).setPosixAccessRights(sMan, file, parentId,
            userId, groupIds, posixRights, update);
    }
    
    public Map<String, Object> getACLEntries(StorageManager sMan, FileMetadata file)
        throws MRCException {
        return getVolumeFileAccessPolicy(sMan.getVolumeId()).getACLEntries(sMan, file);
    }
    
    public void setACLEntries(StorageManager sMan, FileMetadata file, long parentId, String userId,
        List<String> groupIds, Map<String, Object> entries, AtomicDBUpdate update)
        throws MRCException, UserException {
        getVolumeFileAccessPolicy(sMan.getVolumeId()).setACLEntries(sMan, file, parentId, userId,
            groupIds, entries, update);
    }
    
    public void removeACLEntries(StorageManager sMan, FileMetadata file, long parentId,
        String userId, List<String> groupIds, List<Object> entities, AtomicDBUpdate update)
        throws MRCException, UserException {
        getVolumeFileAccessPolicy(sMan.getVolumeId()).removeACLEntries(sMan, file, parentId,
            userId, groupIds, entities, update);
    }
    
    public FileAccessPolicy getFileAccessPolicy(short policyId) {
        
        FileAccessPolicy policy = policies.get(policyId);
        
        // if the policy is not built-in, try to load it from the plug-in
        // directory
        if (policy == null) {
            try {
                policy = policyContainer.getFileAccessPolicy(policyId, volMan);
                policies.put(policyId, policy);
            } catch (Exception exc) {
                Logging.logMessage(Logging.LEVEL_WARN, this,
                    "could not load FileAccessPolicy with ID " + policyId);
                Logging.logMessage(Logging.LEVEL_WARN, this, exc);
            }
        }
        
        return policy;
    }
    
    protected FileAccessPolicy getVolumeFileAccessPolicy(String volumeId) throws MRCException {
        
        try {
            short policyId = volMan.getVolumeById(volumeId).getAcPolicyId();
            
            FileAccessPolicy policy = getFileAccessPolicy(policyId);
            
            if (policy == null)
                throw new MRCException("unknown file access policy for volume " + volumeId + ": "
                    + policyId);
            
            return policy;
            
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }
}
