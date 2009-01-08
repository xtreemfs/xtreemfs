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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.mrc.brain.ErrNo;
import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.mrc.brain.storage.entities.ACLEntry;
import org.xtreemfs.mrc.brain.storage.entities.AbstractFileEntity;
import org.xtreemfs.new_mrc.MRCException;
import org.xtreemfs.new_mrc.volumes.VolumeManager;

/**
 * This policy evaluates access rights according to POSIX access control lists
 * (ACLs). The implementation is based on the description provided in "POSIX
 * Access Control Lists on Linux" by Andreas Grünbacher
 * (http://www.suse.de/~agruen/acl/linux-acls/online/).
 * 
 * <p>
 * Evaluation of access rights assumes that at least a minimal ACL exists on the
 * corresponding file system entity. Minimal ACLs contain one entry for the
 * owner, one for the owning group and one for the other users. A more
 * fine-grained access control model can be supported by using extended ACLs. In
 * order to be interpreted correctly, an ACL entry needs to correspond to at
 * least one of the following patterns:
 * 
 * <ul>
 * <li>"user::" - owner, has to occur exactly once
 * <li>"user:&lt;name&gt;" - named user, may occur zero or more times
 * <li>"group::" - owning group, has to occur exactly once
 * <li>"group:&lt;name&gt;" - named group, may occur zero or more times
 * <li>"other::" - other, has to occur exactly once
 * <li>"mask::" - mask, may occur at most once
 * </ul>
 * 
 * <p>
 * <code>checkPermission()</code> supports the following access modes:
 * 
 * <ul>
 * <li>"r" - read
 * <li>"w" - write
 * <li>"x" - execute
 * <li>"a" - append
 * <li>"ga" - GFS-like append (concurrent appends are properly synchronized)
 * <li>"c" - create
 * <li>"t" - truncate
 * <li>"sr" - strict read-only
 * <li>"d" - delete
 * </ul>
 * 
 * <p>
 * When checking access to a file or directory, the policy will search for the
 * relevant ACL entry according to the POSIX access check algorithm, where the
 * 'rights' value of the ACL entry is interpreted as a bit mask. The bits for
 * the corresponding access modes are set in the reverse order as they are
 * enumerated above.
 * 
 * <p>
 * Example: an ACL entry ("user::", 35) would grant read, write and create
 * access to the file owner, because 35 represents the bit mask 000100011.
 * 
 * <p>
 * The conversion between ACLs and the POSIX access mode only takes into account
 * the read, write and execute bits of the owner, owning group and others ACL
 * entry. Search access on a file or directory is determined by means of
 * checking execute access on each component of the parent tree.
 * 
 * @author stender
 */
public class POSIXFileAccessPolicy implements FileAccessPolicy {
    
    public static final long    POLICY_ID          = 2;
    
    private static final String OWNER              = "user::";
    
    private static final String OWNER_GROUP        = "group::";
    
    private static final String OTHER              = "other::";
    
    private static final String MASK               = "mask::";
    
    private static final String NAMED_USER_PREFIX  = "user:";
    
    private static final String NAMED_GROUP_PREFIX = "group:";
    
    private static final String STICKY_BIT         = "sticky";
    
    private static final String AM_WRITE           = "w";
    
    private static final String AM_READ            = "r";
    
    private static final String AM_EXECUTE         = "x";
    
    private static final String AM_DELETE          = "d";
    
    private static final String AM_MV_RM_IN_DIR    = "m";
    
    private static final long   PERM_READ          = 1 << 0;
    
    private static final long   PERM_WRITE         = 1 << 1;
    
    private static final long   PERM_EXECUTE       = 1 << 2;
    
    private static final long   PERM_APPEND        = 1 << 3;
    
    private static final long   PERM_GFS_APPEND    = 1 << 4;
    
    private static final long   PERM_CREATE        = 1 << 5;
    
    private static final long   PERM_TRUNCATE      = 1 << 6;
    
    private static final long   PERM_STRICT_READ   = 1 << 7;
    
    private static final long   PERM_DELETE        = 1 << 8;
    
    private static final long   PERM_SUID_SGID     = 1 << 16;
    
    private static final long   READ_MASK          = PERM_READ | PERM_STRICT_READ;
    
    private static final long   WRITE_MASK         = PERM_WRITE | PERM_APPEND | PERM_GFS_APPEND
                                                       | PERM_CREATE | PERM_TRUNCATE | PERM_DELETE;
    
    private static final long   EXEC_MASK          = PERM_EXECUTE;
    
    private VolumeManager       volMan;
    
    public POSIXFileAccessPolicy(VolumeManager volMan) {
        this.volMan = volMan;
    }
    
    public String translateAccessMode(int accessMode) {
        switch (accessMode) {
        case FileAccessManager.READ_ACCESS:
            return AM_READ;
        case FileAccessManager.WRITE_ACCESS:
            return AM_WRITE;
        case FileAccessManager.SEARCH_ACCESS:
            return AM_EXECUTE;
        case FileAccessManager.DELETE_ACCESS:
            return AM_DELETE;
        case FileAccessManager.RM_MV_IN_DIR_ACCESS:
            return AM_MV_RM_IN_DIR;
        }
        
        return null;
    }
    
    public void checkPermission(String volumeId, long fileId, long parentId, String userId,
        List<String> groupIds, String accessMode) throws UserException, MRCException {
        
        // TODO
        
        // try {
        //            
        // StorageManager sMan = volMan.getStorageManager(volumeId);
        //            
        //            
        // // retrieve the parent entity from the database
        // AbstractFileEntity parent = sMan.getFileEntity(parentId);
        //            
        // // retrieve the file entity from the database
        // AbstractFileEntity file = sMan.getFileEntity(fileId);
        //            
        // // retrieve the relevant ACL entry for evaluating the access
        // // rights
        // ACLEntry entry = getRelevantACLEntry(volumeId, file, parent, userId,
        // groupIds,
        // accessMode);
        // assert (entry != null);
        //            
        // // if the ACL entry is 'owner' or 'others', evaluate the access
        // // rights without taking into account the 'mask' entry
        // if (OTHER.equals(entry.getEntity()) ||
        // OWNER.equals(entry.getEntity())) {
        //                
        // if (checkRights(accessMode, entry.getRights(), file, parent, userId))
        // {
        // return;
        // } else
        // accessDenied(volumeId, fileId, accessMode);
        //                
        // }
        //            
        // // otherwise, check whether both the entry and the mask entry
        // // grant access
        // ACLEntry maskEntry = findACL(file.getAcl(), MASK);
        // if (checkRights(accessMode, entry.getRights(), file, parent, userId)
        // && (maskEntry == null || checkRights(accessMode,
        // maskEntry.getRights(), file,
        // parent, userId)))
        // return;
        // else
        // accessDenied(volumeId, fileId, accessMode);
        //            
        // } catch (UserException exc) {
        // throw exc;
        // } catch (Exception exc) {
        // throw new MRCException(exc);
        // }
        
    }
    
    public void checkSearchPermission(String volumeId, String path, String userId,
        List<String> groupIds) throws UserException, MRCException {
        
        // TODO
        
        // try {
        //            
        // // check search permission for the root directory
        // StorageManager sMan = volMan.getStorageManager(volumeId);
        //            
        // AbstractFileEntity parentDir = sMan.getFileEntity(1);
        // checkPermission(volumeId, 1, 0, userId, groupIds, "x");
        //            
        // // iteratively check search permissions for all directories in the
        // // path
        // for (int index = 0; index < path.length();) {
        //                
        // int newIndex = path.indexOf('/', index + 1);
        // if (newIndex == -1)
        // newIndex = path.length();
        //                
        // String nextComponent = path.substring(index, newIndex);
        //                
        // sMan = sliceMan.getSliceDB(volumeId, path.substring(0, newIndex - 1),
        // 'r');
        //                
        // parentDir = sMan.getChild(nextComponent, parentDir.getId());
        // checkPermission(volumeId, parentDir.getId(), 0, userId, groupIds,
        // "x");
        //                
        // index = newIndex + 1;
        // }
        //            
        // } catch (UserException exc) {
        // throw exc;
        // } catch (Exception exc) {
        // throw new MRCException(exc);
        // }
    }
    
    public void checkPrivilegedPermissions(String volumeId, long fileId, String userId,
        List<String> groupIds) throws UserException, MRCException {
        
        // TODO
        //        
        // try {
        //            
        // StorageManager sMan = sliceMan.getSliceDB(volumeId, fileId, 'r');
        //            
        // if (!sMan.getFileEntity(fileId).getUserId().equals(userId))
        // throw new UserException(ErrNo.EPERM,
        // "no privileged permissions granted");
        //            
        // } catch (UserException exc) {
        // throw exc;
        // } catch (Exception exc) {
        // throw new MRCException(exc);
        // }
    }
    
    public void setACLEntries(String volumeId, long fileId, String userId, List<String> groupIDs,
        Map<String, Object> entries) throws MRCException, UserException {
        
        // TODO
        
        // try {
        //            
        // StorageManager sMan = sliceMan.getSliceDB(volumeId, fileId, 'w');
        //            
        // // check whether the current user is the owner of the file
        // Map<String, Object> acl =
        // Converter.aclToMap(sMan.getFileEntity(fileId).getAcl());
        //            
        // if (acl == null)
        // acl = entries;
        // else
        // for (String entity : entries.keySet())
        // acl.put(entity, entries.get(entity));
        //            
        // sMan.setFileACL(fileId, acl);
        //            
        // } catch (UserException exc) {
        // throw exc;
        // } catch (Exception exc) {
        // throw new MRCException(exc);
        // }
        
    }
    
    public void removeACLEntries(String volumeId, long fileId, String userId,
        List<String> groupIds, List<Object> entities) throws MRCException, UserException {
        
        // TODO
        
        // try {
        //            
        // StorageManager sMan = sliceMan.getSliceDB(volumeId, fileId, 'w');
        //            
        // if (!sMan.getFileEntity(fileId).getUserId().equals(userId))
        // throw new UserException(ErrNo.EPERM,
        // "changing access mode is restricted to file owner");
        //            
        // // check whether the current user is the owner of the file
        // Map<String, Object> acl =
        // Converter.aclToMap(sMan.getFileEntity(fileId).getAcl());
        // assert (acl != null);
        //            
        // for (Object entity : entities)
        // acl.remove(entity);
        //            
        // sMan.setFileACL(fileId, acl);
        //            
        // } catch (UserException exc) {
        // throw exc;
        // } catch (Exception exc) {
        // throw new MRCException(exc);
        // }
        
    }
    
    public Map<String, Object> convertToACL(long mode) throws MRCException {
        
        try {
            
            Map<String, Object> aclMap = new HashMap<String, Object>();
            
            // determine the sticky bit
            long stickyBit = (mode & (1 << 9)) > 0 ? 1 : 0;
            if (stickyBit != 0)
                aclMap.put(STICKY_BIT, stickyBit);
            
            // determine ACL for owner
            long owr = (mode & (1 << 6)) > 0 ? EXEC_MASK : 0;
            owr |= (mode & (1 << 7)) > 0 ? WRITE_MASK : 0;
            owr |= (mode & (1 << 8)) > 0 ? READ_MASK : 0;
            owr |= (mode & (1 << 11)) > 0 ? PERM_SUID_SGID : 0;
            aclMap.put(OWNER, owr);
            
            // determine ACL for group
            long grr = (mode & (1 << 3)) > 0 ? EXEC_MASK : 0;
            grr |= (mode & (1 << 4)) > 0 ? WRITE_MASK : 0;
            grr |= (mode & (1 << 5)) > 0 ? READ_MASK : 0;
            grr |= (mode & (1 << 10)) > 0 ? PERM_SUID_SGID : 0;
            aclMap.put(OWNER_GROUP, grr);
            
            // determine ACL for others
            long otr = (mode & (1 << 0)) > 0 ? EXEC_MASK : 0;
            otr |= (mode & (1 << 1)) > 0 ? WRITE_MASK : 0;
            otr |= (mode & (1 << 2)) > 0 ? READ_MASK : 0;
            aclMap.put(OTHER, otr);
            
            return aclMap;
            
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }
    
    public short getDefaultVolumeRights(String volumeId) throws MRCException {
        return 509;
    }
    
    public void setPosixAccessRights(String volumeId, long fileId, String userId,
        List<String> groupIds, long posixAccessRights) throws MRCException, UserException {
        
        // TODO
        
        // try {
        //            
        // StorageManager sMan = sliceMan.getSliceDB(volumeId, fileId, 'w');
        //            
        // // check whether the current user is the owner of the file
        // ACLEntry[] acl = sMan.getFileEntity(fileId).getAcl();
        // assert (acl != null);
        //            
        // ACLEntry owner = findACL(acl, OWNER);
        // ACLEntry group = findACL(acl, MASK);
        // if (group == null)
        // group = findACL(acl, OWNER_GROUP);
        // ACLEntry other = findACL(acl, OTHER);
        // assert (owner != null);
        // assert (group != null);
        // assert (other != null);
        //            
        // ACLEntry sticky = findACL(acl, STICKY_BIT);
        //            
        // // determine rights mask for owner
        // long owr = (posixAccessRights & (1 << 6)) > 0 ? EXEC_MASK : 0;
        // owr |= (posixAccessRights & (1 << 7)) > 0 ? WRITE_MASK : 0;
        // owr |= (posixAccessRights & (1 << 8)) > 0 ? READ_MASK : 0;
        // owr |= (posixAccessRights & (1 << 11)) > 0 ? PERM_SUID_SGID : 0;
        //            
        // // determine rights mask for group
        // long grr = (posixAccessRights & (1 << 3)) > 0 ? EXEC_MASK : 0;
        // grr |= (posixAccessRights & (1 << 4)) > 0 ? WRITE_MASK : 0;
        // grr |= (posixAccessRights & (1 << 5)) > 0 ? READ_MASK : 0;
        // grr |= (posixAccessRights & (1 << 10)) > 0 ? PERM_SUID_SGID : 0;
        //            
        // // determine rights mask for others
        // long otr = (posixAccessRights & (1 << 0)) > 0 ? EXEC_MASK : 0;
        // otr |= (posixAccessRights & (1 << 1)) > 0 ? WRITE_MASK : 0;
        // otr |= (posixAccessRights & (1 << 2)) > 0 ? READ_MASK : 0;
        //            
        // // determine whether the sticky bit is set
        // boolean isSticky = (posixAccessRights & (1 << 9)) > 0;
        //            
        // if (sticky == null && isSticky) {
        // sticky = new ACLEntry(STICKY_BIT, 1);
        // ACLEntry[] newAcl = new ACLEntry[acl.length + 1];
        // for (int i = 0, j = 0; i < acl.length; i++, j++) {
        // if (STICKY_BIT.compareTo(acl[i].getEntity()) < 0) {
        // newAcl[j++] = sticky;
        // }
        // newAcl[j] = acl[i];
        // }
        // acl = newAcl;
        // }
        //
        // else if (sticky != null)
        // sticky.setRights(isSticky ? 1 : 0);
        //            
        // owner.setRights(owr);
        // group.setRights(grr);
        // other.setRights(otr);
        //            
        // sMan.setFileACL(fileId, acl);
        //            
        // } catch (UserException exc) {
        // throw exc;
        // } catch (Exception exc) {
        // throw new MRCException(exc);
        // }
        
    }
    
    public long getPosixAccessRights(String volumeId, long fileId, String userId,
        List<String> groupIds) throws MRCException {
        
        return 511;
        // TODO
        
        // try {
        //            
        // StorageManager sMan = sliceMan.getSliceDB(volumeId, fileId, 'r');
        //            
        // ACLEntry[] acl = sMan.getFileEntity(fileId).getAcl();
        // if (acl == null)
        // return 0;
        //            
        // ACLEntry owner = findACL(acl, OWNER);
        // ACLEntry group = findACL(acl, MASK);
        // if (group == null)
        // group = findACL(acl, OWNER_GROUP);
        // ACLEntry other = findACL(acl, OTHER);
        // assert (owner != null);
        // assert (group != null);
        // assert (other != null);
        //            
        // ACLEntry sticky = findACL(acl, STICKY_BIT);
        //            
        // return ((owner.getRights() & PERM_SUID_SGID) > 0 ? 1 << 11 : 0)
        // | ((group.getRights() & PERM_SUID_SGID) > 0 ? 1 << 10 : 0)
        // | ((sticky != null && sticky.getRights() == 1) ? 1 << 9 : 0)
        // | ((owner.getRights() & PERM_READ) > 0 ? 1 << 8 : 0)
        // | ((owner.getRights() & PERM_WRITE) > 0 ? 1 << 7 : 0)
        // | ((owner.getRights() & PERM_EXECUTE) > 0 ? 1 << 6 : 0)
        // | ((group.getRights() & PERM_READ) > 0 ? 1 << 5 : 0)
        // | ((group.getRights() & PERM_WRITE) > 0 ? 1 << 4 : 0)
        // | ((group.getRights() & PERM_EXECUTE) > 0 ? 1 << 3 : 0)
        // | ((other.getRights() & PERM_READ) > 0 ? 1 << 2 : 0)
        // | ((other.getRights() & PERM_WRITE) > 0 ? 1 << 1 : 0)
        // | ((other.getRights() & PERM_EXECUTE) > 0 ? 1 : 0);
        //            
        // } catch (Exception exc) {
        // throw new MRCException(exc);
        // }
    }
    
    private static ACLEntry findACL(ACLEntry[] acl, String entityName) {
        
        // find the ACL entry by means of a binary search
        int low = 0;
        int high = acl.length - 1;
        
        while (low <= high) {
            int mid = (low + high) >>> 1;
            ACLEntry midEntry = acl[mid];
            
            int cmp = midEntry.getEntity().compareTo(entityName);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return acl[mid];
        }
        
        return null;
    }
    
    private static boolean checkRights(String accessMode, long rights, AbstractFileEntity file,
        AbstractFileEntity parent, String userId) {
        
        if (accessMode.length() == 1) {
            switch (accessMode.charAt(0)) {
            case 'r':
                return (rights & (1 << 0)) != 0;
            case 'w':
                return (rights & (1 << 1)) != 0;
            case 'x':
                return (rights & (1 << 2)) != 0;
            case 'a':
                return (rights & (1 << 3)) != 0;
            case 'c':
                return (rights & (1 << 5)) != 0;
            case 't':
                return (rights & (1 << 6)) != 0;
            case 'd':
                return (rights & (1 << 8)) != 0;
            case 'm':

                // evaluate the parent's sticky bit
                ACLEntry stickyBitEntry = findACL(parent.getAcl(), STICKY_BIT);
                if (stickyBitEntry != null && stickyBitEntry.getRights() != 0)
                    return file.getUserId().equals(userId);
                else
                    return true;
            }
            
        } else if (accessMode.length() == 2) {
            if (accessMode.equals("ga") && (rights & (1 << 4)) != 0)
                return true;
            else if (accessMode.equals("sr") && (rights & (1 << 7)) != 0)
                return true;
        }
        
        return false;
    }
    
    private static ACLEntry getRelevantACLEntry(String volumeId, AbstractFileEntity file,
        AbstractFileEntity parent, String userId, List<String> groupIds, String accessMode)
        throws UserException {
        
        // if the user ID is the owner, check access according to the rights
        // associated with the owner entry
        if (file.getUserId().equals(userId)) {
            
            ACLEntry entry = findACL(file.getAcl(), OWNER);
            assert (entry != null);
            
            return entry;
        }
        
        // if the user ID refers to a named user, check access according to
        // the corresponding user rights
        ACLEntry entry = findACL(file.getAcl(), NAMED_USER_PREFIX + userId);
        if (entry != null)
            return entry;
        
        boolean groupFound = false;
        
        // if a group ID refers to the owning group, check whether access is
        // granted according to the owning group rights
        for (String groupId : groupIds) {
            if (groupId.equals(file.getGroupId())) {
                
                entry = findACL(file.getAcl(), OWNER_GROUP);
                assert (entry != null);
                
                if (checkRights(accessMode, entry.getRights(), file, parent, userId))
                    return entry;
                
                groupFound = true;
            }
        }
        
        // if a group ID refers to any of the named groups, check whether
        // access is granted according to the corresponding group rights
        for (String groupId : groupIds) {
            
            entry = findACL(file.getAcl(), NAMED_GROUP_PREFIX + groupId);
            
            if (entry != null) {
                
                if (checkRights(accessMode, entry.getRights(), file, parent, userId))
                    return entry;
                
                groupFound = true;
            }
        }
        
        // if there was a matching entry but access was not granted, access
        // is denied
        if (groupFound)
            accessDenied(volumeId, file.getId(), accessMode);
        
        entry = findACL(file.getAcl(), OTHER);
        assert (entry != null);
        return entry;
    }
    
    private static void accessDenied(String volumeId, long fileId, String accessMode)
        throws UserException {
        
        throw new UserException(ErrNo.EACCES, "access denied, volumeId = " + volumeId
            + ", fileId = " + fileId + ", accessMode = \"" + accessMode + "\"");
    }
    
}
