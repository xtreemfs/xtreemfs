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

import java.util.List;
import java.util.Map;

import org.xtreemfs.interfaces.AccessControlPolicyType;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.PathResolver;

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
                                                .intValue();
    
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
        List<String> groupIds, int posixAccessRights, AtomicDBUpdate update) throws MRCException {
        
        try {
            FileMetadata rootDir = sMan.getMetadata(0, sMan.getVolumeInfo().getName());
            super.setPosixAccessRights(sMan, rootDir, parentId, userId, groupIds, posixAccessRights, update);
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
