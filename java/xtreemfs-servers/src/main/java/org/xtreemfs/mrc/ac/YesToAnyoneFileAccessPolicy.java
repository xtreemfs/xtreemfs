/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.ac;

import java.util.List;
import java.util.Map;

import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.ACLEntry;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;

/**
 * This policy will grant access to anyone. It does not allow changeing access
 * rights, any ACLs set on files or volumes will be ignored.
 * 
 * @author stender
 * 
 */
public class YesToAnyoneFileAccessPolicy implements FileAccessPolicy {
    
    public static final short POLICY_ID = (short) AccessControlPolicyType.ACCESS_CONTROL_POLICY_NULL
                                                .getNumber();
    
    @Override
    public String translateAccessFlags(int accessMode) {
        return null;
    }
    
    @Override
    public String translatePermissions(int permissions) {
        return null;
    }
    
    @Override
    public void checkPermission(StorageManager sMan, FileMetadata file, long parentId, String userId,
        List<String> groupIds, String accessMode) {
        // do nothing
    }
    
    @Override
    public void checkSearchPermission(StorageManager sMan, PathResolver res, String userId,
        List<String> groupIds) {
        // do nothing
    }
    
    @Override
    public void checkPrivilegedPermissions(StorageManager sMan, FileMetadata file, String userId,
        List<String> groupIds) {
        // do nothing
    }
    
    @Override
    public int getPosixAccessRights(StorageManager sMan, FileMetadata file, String userId,
        List<String> groupIds) {
        return 511; // rwxrwxrwx
    }
    
    @Override
    public void setPosixAccessRights(StorageManager sMan, FileMetadata file, long parentId, String userId,
        List<String> groupIds, int posixAccessRights, boolean superUser, AtomicDBUpdate update) {
        // do nothing
    }
    
    @Override
    public Map<String, Object> getACLEntries(StorageManager sMan, FileMetadata file) throws MRCException {
        return null;
    }
    
    @Override
    public void updateACLEntries(StorageManager sMan, FileMetadata file, long parentId,
        Map<String, Object> entries, AtomicDBUpdate update) throws MRCException, UserException {
        // do nothing
    }
    
    @Override
    public void removeACLEntries(StorageManager sMan, FileMetadata file, long parentId,
        List<Object> entities, AtomicDBUpdate update) throws MRCException, UserException {
        // do nothing
    }
    
    @Override
    public ACLEntry[] getDefaultRootACL() {
        return null;
    }
    
}
