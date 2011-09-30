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
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;

/**
 * This policy grants or denies access based on immutable volume ACLs. Note that
 * ACLs are no POSIX ACLs. A 'default' entry may be defined that is valid for
 * any user except for those having a user-specific entry.
 * 
 * @author stender
 * 
 */
public class VolumeACLFileAccessPolicy extends POSIXFileAccessPolicy {
    
    public static final short POLICY_ID = (short) AccessControlPolicyType.ACCESS_CONTROL_POLICY_VOLUME
                                                .getNumber();
    
    @Override
    public void checkPermission(StorageManager sMan, FileMetadata file, long parentId, String userId,
        List<String> groupIds, String accessMode) throws UserException, MRCException {
        
        try {
            FileMetadata rootDir = sMan.getMetadata(0, sMan.getVolumeInfo().getName());
            super.checkPermission(sMan, rootDir, parentId, userId, groupIds, accessMode);
        } catch (DatabaseException exc) {
            throw new MRCException(exc);
        }
        
    }
    
    @Override
    public void checkSearchPermission(StorageManager sMan, PathResolver res, String userId,
        List<String> groupIds) throws UserException, MRCException {
        
        try {
            FileMetadata rootDir = sMan.getMetadata(0, sMan.getVolumeInfo().getName());
            super.checkPermission(sMan, rootDir, 0, userId, groupIds, AM_EXECUTE);
        } catch (UserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }
    
    @Override
    public int getPosixAccessRights(StorageManager sMan, FileMetadata file, String userId,
        List<String> groupIds) throws MRCException {
        
        try {
            FileMetadata rootDir = sMan.getMetadata(0, sMan.getVolumeInfo().getName());
            return super.getPosixAccessRights(sMan, rootDir, userId, groupIds);
        } catch (DatabaseException e) {
            throw new MRCException(e);
        }
    }
    
    @Override
    public void setPosixAccessRights(StorageManager sMan, FileMetadata file, long parentId, String userId,
        List<String> groupIds, int posixAccessRights, boolean superUser, AtomicDBUpdate update)
        throws MRCException {
        
        try {
            FileMetadata rootDir = sMan.getMetadata(0, sMan.getVolumeInfo().getName());
            super.setPosixAccessRights(sMan, rootDir, parentId, userId, groupIds, posixAccessRights,
                superUser, update);
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }
    
    @Override
    public Map<String, Object> getACLEntries(StorageManager sMan, FileMetadata file) throws MRCException {
        
        try {
            FileMetadata rootDir = sMan.getMetadata(0, sMan.getVolumeInfo().getName());
            return super.getACLEntries(sMan, rootDir);
        } catch (DatabaseException e) {
            throw new MRCException(e);
        }
    }
    
    @Override
    public void updateACLEntries(StorageManager sMan, FileMetadata file, long parentId,
        Map<String, Object> entries, AtomicDBUpdate update) throws MRCException, UserException {
        
        try {
            FileMetadata rootDir = sMan.getMetadata(0, sMan.getVolumeInfo().getName());
            super.updateACLEntries(sMan, rootDir, parentId, entries, update);
        } catch (DatabaseException e) {
            throw new MRCException(e);
        }
    }
    
    @Override
    public void removeACLEntries(StorageManager sMan, FileMetadata file, long parentId,
        List<Object> entities, AtomicDBUpdate update) throws MRCException, UserException {
        
        try {
            FileMetadata rootDir = sMan.getMetadata(0, sMan.getVolumeInfo().getName());
            super.removeACLEntries(sMan, rootDir, parentId, entities, update);
        } catch (DatabaseException e) {
            throw new MRCException(e);
        }
    }
    
}
