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

package org.xtreemfs.new_mrc.ac;

import java.util.List;
import java.util.Map;

import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.mrc.brain.storage.entities.ACLEntry;
import org.xtreemfs.new_mrc.MRCException;
import org.xtreemfs.new_mrc.volumes.VolumeManager;

/**
 * This policy grants or denies access based on immutable volume ACLs. Note that
 * ACLs are no POSIX ACLs. A 'default' entry may be defined that is valid for
 * any user except for those having a user-specific entry.
 * 
 * @author stender
 * 
 */
public class VolumeACLFileAccessPolicy implements FileAccessPolicy {
    
    private VolumeManager       volMan;
    
    public static final long    POLICY_ID = 3;
    
    private static final String AM_WRITE  = "w";
    
    private static final String AM_READ   = "r";
    
    private static final String AM_DELETE = "d";
    
    public VolumeACLFileAccessPolicy(VolumeManager volMan) {
        this.volMan = volMan;
    }
    
    public String translateAccessMode(int accessMode) {
        switch (accessMode) {
        case FileAccessManager.READ_ACCESS:
            return AM_READ;
        case FileAccessManager.WRITE_ACCESS:
            return AM_WRITE;
        case FileAccessManager.SEARCH_ACCESS:
            return AM_READ;
        case FileAccessManager.DELETE_ACCESS:
            return AM_DELETE;
        }
        
        return null;
    }
    
    public void checkPermission(String volumeId, long fileId, long parentId, String userId,
        List<String> groupIds, String accessMode) throws UserException, MRCException {
        
        // TODO
        
//        try {
//            
//            if (fileId == 0)
//                return;
//            
//            StorageManager sMan = sliceMan.getSliceDB(volumeId, "/", 'r');
//            
//            ACLEntry[] acl = sMan.getVolumeACL();
//            
//            long rights = getRights(userId, acl);
//            
//            if (accessMode.length() == 1) {
//                switch (accessMode.charAt(0)) {
//                case 'r':
//                    if ((rights & (1 << 0)) != 0)
//                        return;
//                    break;
//                case 'w':
//                    if ((rights & (1 << 1)) != 0)
//                        return;
//                    break;
//                case 'a':
//                    if ((rights & (1 << 2)) != 0)
//                        return;
//                    break;
//                case 'c':
//                    if ((rights & (1 << 4)) != 0)
//                        return;
//                    break;
//                case 't':
//                    if ((rights & (1 << 5)) != 0)
//                        return;
//                    break;
//                case 'd':
//                    if ((rights & (1 << 7)) != 0)
//                        return;
//                    break;
//                }
//            } else if (accessMode.length() == 2) {
//                if (accessMode.equals("ga") && (rights & (1 << 3)) != 0)
//                    return;
//                if (accessMode.equals("sr"))
//                    if ((rights & (1 << 6)) != 0)
//                        return;
//            }
//            
//        } catch (Exception exc) {
//            throw new MRCException(exc);
//        }
//        
//        throw new UserException(ErrNo.EACCES, "access denied, volumeId = " + volumeId
//            + ", fileId = " + fileId + ", accessMode = \"" + accessMode + "\"");
    }
    
    public void checkSearchPermission(String volumeId, String path, String userId,
        List<String> groupIds) throws UserException, MRCException {
        checkPermission(volumeId, 1, 0, userId, groupIds, AM_READ);
    }
    
    public void checkPrivilegedPermissions(String volumeId, long fileId, String userId,
        List<String> groupIds) throws UserException, MRCException {
        
        // TODO
        
//        try {
//            
//            StorageManager sMan = sliceMan.getSliceDB(volumeId, "/", 'r');
//            
//            if (!sMan.getFileEntity(1).getUserId().equals(userId))
//                throw new UserException(ErrNo.EPERM,
//                    "changing file owner is restricted to file owner");
//            
//        } catch (UserException exc) {
//            throw exc;
//        } catch (Exception exc) {
//            throw new MRCException(exc);
//        }
    }
    
    public short getDefaultVolumeRights(String volumeId) throws MRCException {
        return 509;
    }
    
    public Map<String, Object> convertToACL(long mode) throws MRCException {
        return null;
    }
    
    public long getPosixAccessRights(String volumeId, long fileId, String userId,
        List<String> groupIds) throws MRCException {
        
        return 511;
        // TODO
        
//        try {
//            StorageManager sMan = sliceMan.getSliceDB(volumeId, "/", 'r');
//            ACLEntry[] acl = sMan.getVolumeACL();
//            
//            long rights = getRights(userId, acl);
//            rights = rights & 3 | ((rights & 1) << 2); // rw-mask, x=r
//            return rights * (1 << 6);
//            
//        } catch (Exception exc) {
//            throw new MRCException(exc);
//        }
    }
    
    public void setPosixAccessRights(String volumeId, long fileId, String userId,
        List<String> groupIds, long posixRights) throws MRCException {
        // do nothing
    }
    
    public void setACLEntries(String volumeId, long fileId, String userId, List<String> groupIDs,
        Map<String, Object> entries) throws MRCException, UserException {
        
        // TODO
        
//        try {
//            
//            // set volume ACL initially
//            StorageManager sMan = sliceMan.getSliceDB(volumeId, "/", 'w');
//            ACLEntry[] acl = sMan.getVolumeACL();
//            if (acl == null)
//                sMan.setFileACL(1, entries);
//            
//        } catch (Exception exc) {
//            throw new MRCException(exc);
//        }
    }
    
    public void removeACLEntries(String volumeId, long fileId, String userId,
        List<String> groupIds, List<Object> entities) throws MRCException, UserException {
        // do nothing
    }
    
    private static long getRights(String userId, ACLEntry[] acl) {
        
        // do not permit anything by default
        if (acl == null)
            return 0;
        
        // find the ACL entry by means of a binary search
        int low = 0;
        int high = acl.length - 1;
        
        while (low <= high) {
            
            int mid = (low + high) >>> 1;
            ACLEntry midEntry = acl[mid];
            
            int cmp = midEntry.getEntity().compareTo(userId);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return acl[mid].getRights();
        }
        
        if (userId.equals("default"))
            return 0;
        else
            return getRights("default", acl);
    }
    
}
