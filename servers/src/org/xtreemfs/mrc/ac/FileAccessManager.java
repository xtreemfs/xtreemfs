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
import org.xtreemfs.mrc.PolicyContainer;
import org.xtreemfs.mrc.brain.BrainException;
import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.mrc.slices.SliceManager;

/**
 * This class is responsible for checking policy-based file access.
 * 
 * @author stender
 * 
 */
public class FileAccessManager {
    
    public static final int                   READ_ACCESS         = 1;
    
    public static final int                   SEARCH_ACCESS       = 2;
    
    public static final int                   WRITE_ACCESS        = 3;
    
    public static final int                   DELETE_ACCESS       = 4;
    
    public static final int                   RM_MV_IN_DIR_ACCESS = 5;
    
    private final SliceManager                sliceMan;
    
    private final Map<Long, FileAccessPolicy> policies;
    
    private PolicyContainer                   policyContainer;
    
    public FileAccessManager(SliceManager sliceMan, PolicyContainer policyContainer) {
        
        this.sliceMan = sliceMan;
        this.policyContainer = policyContainer;
        
        policies = new HashMap<Long, FileAccessPolicy>();
        policies.put(POSIXFileAccessPolicy.POLICY_ID, new POSIXFileAccessPolicy(sliceMan));
        policies.put(YesToAnyoneFileAccessPolicy.POLICY_ID, new YesToAnyoneFileAccessPolicy());
        policies.put(VolumeACLFileAccessPolicy.POLICY_ID, new VolumeACLFileAccessPolicy(sliceMan));
    }
    
    public void checkSearchPermission(String volumeId, String path, String userId,
        boolean superUser, List<String> groupIds) throws UserException, BrainException {
        
        if (superUser)
            return;
        
        getVolumeFileAccessPolicy(volumeId).checkSearchPermission(volumeId, path, userId, groupIds);
    }
    
    public void checkPrivilegedPermissions(String volumeId, long fileId, String userId,
        boolean superUser, List<String> groupIds) throws UserException, BrainException {
        
        if (superUser)
            return;
        
        getVolumeFileAccessPolicy(volumeId).checkPrivilegedPermissions(volumeId, fileId, userId,
            groupIds);
    }
    
    public void checkPermission(int accessMode, String volumeId, long fileId, long parentId,
        String userId, boolean superUser, List<String> groupIds) throws UserException,
        BrainException {
        
        checkPermission(translateAccessMode(volumeId, accessMode), volumeId, fileId, parentId,
            userId, superUser, groupIds);
    }
    
    public void checkPermission(String accessMode, String volumeId, long fileId, long parentId,
        String userId, boolean superUser, List<String> groupIds) throws UserException,
        BrainException {
        
        if (superUser)
            return;
        
        getVolumeFileAccessPolicy(volumeId).checkPermission(volumeId, fileId, parentId, userId,
            groupIds, accessMode);
    }
    
    public String translateAccessMode(String volumeId, int accessMode) throws BrainException {
        return getVolumeFileAccessPolicy(volumeId).translateAccessMode(accessMode);
    }
    
    public Map<String, Object> createDefaultVolumeACL(String volumeId) throws BrainException {
        return getVolumeFileAccessPolicy(volumeId).createDefaultVolumeACL(volumeId);
    }
    
    public Map<String, Object> convertToACL(String volumeId, long mode) throws BrainException {
        return getVolumeFileAccessPolicy(volumeId).convertToACL(mode);
    }
    
    public long getPosixAccessMode(String volumeId, long fileId, String userId,
        List<String> groupIds) throws BrainException {
        return getVolumeFileAccessPolicy(volumeId).getPosixAccessRights(volumeId, fileId, userId,
            groupIds);
    }
    
    public void setPosixAccessMode(String volumeId, long fileId, String userId,
        List<String> groupIds, long posixRights) throws BrainException, UserException {
        getVolumeFileAccessPolicy(volumeId).setPosixAccessRights(volumeId, fileId, userId,
            groupIds, posixRights);
    }
    
    public void setACLEntries(String volumeId, long fileId, String userId, List<String> groupIds,
        Map<String, Object> entries) throws BrainException, UserException {
        getVolumeFileAccessPolicy(volumeId).setACLEntries(volumeId, fileId, userId, groupIds,
            entries);
    }
    
    public void removeACLEntries(String volumeId, long fileId, String userId,
        List<String> groupIds, List<Object> entities) throws BrainException, UserException {
        getVolumeFileAccessPolicy(volumeId).removeACLEntries(volumeId, fileId, userId, groupIds,
            entities);
    }
    
    public FileAccessPolicy getFileAccessPolicy(long policyId) {
        
        FileAccessPolicy policy = policies.get(policyId);
        
        // if the policy is not built-in, try to load it from the plug-in
        // directory
        if (policy == null) {
            try {
                policy = policyContainer.getFileAccessPolicy(policyId, sliceMan);
                policies.put(policyId, policy);
            } catch (Exception exc) {
                Logging.logMessage(Logging.LEVEL_WARN, this,
                    "could not load FileAccessPolicy with ID " + policyId);
                Logging.logMessage(Logging.LEVEL_WARN, this, exc);
            }
        }
        
        return policy;
    }
    
    protected FileAccessPolicy getVolumeFileAccessPolicy(String volumeId) throws BrainException {
        
        try {
            long policyId = sliceMan.getVolumeById(volumeId).getAcPolicyId();
            
            FileAccessPolicy policy = getFileAccessPolicy(policyId);
            
            if (policy == null)
                throw new BrainException("unknown file access policy for volume " + volumeId + ": "
                    + policyId);
            
            return policy;
            
        } catch (UserException exc) {
            throw new BrainException(exc);
        }
    }
}
