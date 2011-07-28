/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.ac;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.MRCPolicyContainer;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;

/**
 * This class is responsible for checking policy-based file access.
 * 
 * @author stender
 * 
 */
public class FileAccessManager {
    
    public static final int                    O_RDONLY               = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY
                                                                              .getNumber();
    
    public static final int                    O_WRONLY               = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_WRONLY
                                                                              .getNumber();
    
    public static final int                    O_RDWR                 = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR
                                                                              .getNumber();
    
    public static final int                    O_CREAT                = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT
                                                                              .getNumber();
    
    public static final int                    O_TRUNC                = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC
                                                                              .getNumber();
    
    public static final int                    O_APPEND               = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_APPEND
                                                                              .getNumber();
    
    public static final int                    O_EXCL                 = GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_EXCL
                                                                              .getNumber();
    
    public static final int                    NON_POSIX_SEARCH       = 04000000;
    
    public static final int                    NON_POSIX_DELETE       = 010000000;
    
    public static final int                    NON_POSIX_RM_MV_IN_DIR = 020000000;
    
    private final VolumeManager                volMan;
    
    private final Map<Short, FileAccessPolicy> policies;
    
    private MRCPolicyContainer                 policyContainer;
    
    public FileAccessManager(VolumeManager volMan, MRCPolicyContainer policyContainer) {
        
        this.volMan = volMan;
        this.policyContainer = policyContainer;
        
        policies = new HashMap<Short, FileAccessPolicy>();
    }
    
    public void checkSearchPermission(StorageManager sMan, PathResolver path, String userId,
        boolean superUser, List<String> groupIds) throws UserException, MRCException {
        
        if (superUser)
            return;
        
        getVolumeFileAccessPolicy(sMan.getVolumeInfo().getId()).checkSearchPermission(sMan, path, userId,
            groupIds);
    }
    
    public void checkPrivilegedPermissions(StorageManager sMan, FileMetadata file, String userId,
        boolean superUser, List<String> groupIds) throws UserException, MRCException {
        
        if (superUser)
            return;
        
        getVolumeFileAccessPolicy(sMan.getVolumeInfo().getId()).checkPrivilegedPermissions(sMan, file,
            userId, groupIds);
    }
    
    public void checkPermission(int flags, StorageManager sMan, FileMetadata file, long parentDirId,
        String userId, boolean superUser, List<String> groupIds) throws UserException, MRCException {
        
        checkPermission(translateAccessFlags(sMan.getVolumeInfo().getId(), flags), sMan, file, parentDirId,
            userId, superUser, groupIds);
    }
    
    public void checkPermission(String accessMode, StorageManager sMan, FileMetadata file, long parentDirId,
        String userId, boolean superUser, List<String> groupIds) throws UserException, MRCException {
        
        if (superUser)
            return;
        
        getVolumeFileAccessPolicy(sMan.getVolumeInfo().getId()).checkPermission(sMan, file, parentDirId,
            userId, groupIds, accessMode);
    }
    
    public String translateAccessFlags(String volumeId, int accessMode) throws MRCException {
        return getVolumeFileAccessPolicy(volumeId).translateAccessFlags(accessMode);
    }
    
    public int getPosixAccessMode(StorageManager sMan, FileMetadata file, String userId, List<String> groupIds)
        throws MRCException {
        return getVolumeFileAccessPolicy(sMan.getVolumeInfo().getId()).getPosixAccessRights(sMan, file,
            userId, groupIds);
    }
    
    public void setPosixAccessMode(StorageManager sMan, FileMetadata file, long parentId, String userId,
        List<String> groupIds, int posixRights, boolean superUser, AtomicDBUpdate update)
        throws MRCException, UserException {
        getVolumeFileAccessPolicy(sMan.getVolumeInfo().getId()).setPosixAccessRights(sMan, file, parentId,
            userId, groupIds, posixRights, superUser, update);
    }
    
    public Map<String, Object> getACLEntries(StorageManager sMan, FileMetadata file) throws MRCException {
        return getVolumeFileAccessPolicy(sMan.getVolumeInfo().getId()).getACLEntries(sMan, file);
    }
    
    public void updateACLEntries(StorageManager sMan, FileMetadata file, long parentId,
        Map<String, Object> entries, AtomicDBUpdate update) throws MRCException, UserException {
        getVolumeFileAccessPolicy(sMan.getVolumeInfo().getId()).updateACLEntries(sMan, file, parentId,
            entries, update);
    }
    
    public void removeACLEntries(StorageManager sMan, FileMetadata file, long parentId,
        List<Object> entities, AtomicDBUpdate update) throws MRCException, UserException {
        getVolumeFileAccessPolicy(sMan.getVolumeInfo().getId()).removeACLEntries(sMan, file, parentId,
            entities, update);
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
                Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                    "could not load FileAccessPolicy with ID %d", policyId);
                Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this, OutputUtils
                        .stackTraceToString(exc));
            }
        }
        
        return policy;
    }
    
    protected FileAccessPolicy getVolumeFileAccessPolicy(String volumeId) throws MRCException {
        
        try {
            
            short policyId = volMan.getStorageManager(volumeId).getVolumeInfo().getAcPolicyId();
            FileAccessPolicy policy = getFileAccessPolicy(policyId);
            
            if (policy == null)
                throw new MRCException("unknown file access policy for volume " + volumeId + ": " + policyId);
            
            return policy;
            
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }
}
