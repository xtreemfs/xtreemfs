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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.mrc.ErrNo;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.ACLEntry;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.PathResolver;

/**
 * This policy evaluates access rights according to POSIX permissions and access
 * control lists (ACLs). The implementation is based on the description provided
 * in "POSIX Access Control Lists on Linux" by Andreas Grünbacher
 * (http://www.suse.de/~agruen/acl/linux-acls/online/).
 * 
 * <p>
 * In the general case, access permissions are granted or denied based on the 16
 * bit permissions value which is directly associated with the file metadata.
 * This allows to distinguish 'read', 'write' and 'execute' rights for the
 * owner, the owing group and the rest of the world.
 * 
 * <p>
 * A more fine-grained access control model can be supported by using ACLs.
 * Access control decisions will be made based on ACLs if at least the following
 * ACL entries exist for a file:
 * 
 * <ul>
 * <li>"user:" - owner, has to occur exactly once
 * <li>"user:&lt;name&gt;" - named user, may occur zero or more times
 * <li>"group:" - owning group, has to occur exactly once
 * <li>"group:&lt;name&gt;" - named group, may occur zero or more times
 * <li>"other:" - other, has to occur exactly once
 * <li>"mask:" - mask, may occur at most once
 * </ul>
 * 
 * <p>
 * The access control granularity w/ ACLs is not restricted to 'read', 'write'
 * and 'execute'. Further access modes and bits are evaluated (the values in
 * brackets decribe the corresponding bit value in the ACL entry):
 * 
 * <ul>
 * <li>"r" - read (0x01)
 * <li>"w" - write (0x02)
 * <li>"x" - execute (0x04)
 * <li>"a" - append (0x08)
 * <li>"ga" - GFS-like append (0x10)
 * <li>"c" - create (0x20)
 * <li>"t" - truncate (0x40)
 * <li>"sr" - strict read-only (0x80)
 * <li>"d" - delete (0x100)
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
 * Example: an ACL entry ("user:", 35) would grant read, write and create access
 * to the file owner, because 35 represents the bit mask 000100011.
 * 
 * @author stender
 */
public class POSIXFileAccessPolicy implements FileAccessPolicy {
    
    public static final short   POLICY_ID          = 2;
    
    private static final String OWNER              = "user:";
    
    private static final String OWNER_GROUP        = "group:";
    
    private static final String OTHER              = "other:";
    
    private static final String MASK               = "mask:";
    
    private static final String NAMED_USER_PREFIX  = "user:";
    
    private static final String NAMED_GROUP_PREFIX = "group:";
    
    private static final String STICKY_BIT         = "sticky";
    
    private static final String AM_WRITE           = "w";
    
    private static final String AM_READ            = "r";
    
    private static final String AM_EXECUTE         = "x";
    
    private static final String AM_DELETE          = "d";
    
    private static final String AM_MV_RM_IN_DIR    = "m";
    
    private static final int    POSIX_OTHER_EXEC   = 1 << 0;
    
    private static final int    POSIX_OTHER_WRITE  = 1 << 1;
    
    private static final int    POSIX_OTHER_READ   = 1 << 2;
    
    private static final int    POSIX_GROUP_EXEC   = 1 << 3;
    
    private static final int    POSIX_GROUP_WRITE  = 1 << 4;
    
    private static final int    POSIX_GROUP_READ   = 1 << 5;
    
    private static final int    POSIX_OWNER_EXEC   = 1 << 6;
    
    private static final int    POSIX_OWNER_WRITE  = 1 << 7;
    
    private static final int    POSIX_OWNER_READ   = 1 << 8;
    
    private static final int    POSIX_STICKY       = 1 << 9;
    
    private static final int    POSIX_SGID         = 1 << 10;
    
    private static final int    POSIX_SUID         = 1 << 11;
    
    private static final short  PERM_READ          = 1 << 0;
    
    private static final short  PERM_WRITE         = 1 << 1;
    
    private static final short  PERM_EXECUTE       = 1 << 2;
    
    private static final short  PERM_APPEND        = 1 << 3;
    
    private static final short  PERM_GFS_APPEND    = 1 << 4;
    
    private static final short  PERM_CREATE        = 1 << 5;
    
    private static final short  PERM_TRUNCATE      = 1 << 6;
    
    private static final short  PERM_STRICT_READ   = 1 << 7;
    
    private static final short  PERM_DELETE        = 1 << 8;
    
    private static final short  PERM_SUID_SGID     = 1 << 14;
    
    private static final short  READ_MASK          = PERM_READ | PERM_STRICT_READ;
    
    private static final short  WRITE_MASK         = PERM_WRITE | PERM_APPEND | PERM_GFS_APPEND | PERM_CREATE
                                                       | PERM_TRUNCATE | PERM_DELETE;
    
    private static final short  EXEC_MASK          = PERM_EXECUTE;
    
    public POSIXFileAccessPolicy() {
    }
    
    @Override
    public String translateAccessFlags(int accessMode) {
        
        accessMode = accessMode
            & (FileAccessManager.O_RDWR | FileAccessManager.O_WRONLY | FileAccessManager.O_APPEND
                | FileAccessManager.NON_POSIX_SEARCH | FileAccessManager.NON_POSIX_DELETE | FileAccessManager.NON_POSIX_RM_MV_IN_DIR);
        
        if (accessMode == FileAccessManager.O_RDONLY)
            return AM_READ;
        if (((accessMode & FileAccessManager.O_RDWR) != 0)
            || ((accessMode & FileAccessManager.O_WRONLY) != 0)
            || ((accessMode & FileAccessManager.O_APPEND) != 0))
            return AM_WRITE;
        if ((accessMode & FileAccessManager.NON_POSIX_SEARCH) != 0)
            return AM_EXECUTE;
        if ((accessMode & FileAccessManager.NON_POSIX_DELETE) != 0)
            return AM_DELETE;
        if ((accessMode & FileAccessManager.NON_POSIX_RM_MV_IN_DIR) != 0)
            return AM_MV_RM_IN_DIR;
        
        assert (false) : "never ever! mode is " + accessMode;
        return null;
    }
    
    @Override
    public void checkPermission(StorageManager sMan, FileMetadata file, long parentId, String userId,
        List<String> groupIds, String accessMode) throws UserException, MRCException {
        
        assert (file != null);
        
        try {
            
            // check whether an ACL exists; if so, use the ACL for the access
            // check ...
            if (sMan.getACL(file.getId()).hasNext()) {
                
                // retrieve the relevant ACL entry for evaluating the access
                // rights
                ACLEntry entry = getRelevantACLEntry(sMan, file, parentId, userId, groupIds, accessMode);
                assert (entry != null);
                
                // if the ACL entry is 'owner' or 'others', evaluate the access
                // rights without taking into account the 'mask' entry
                if (OTHER.equals(entry.getEntity()) || OWNER.equals(entry.getEntity())) {
                    
                    if (checkIfAllowed(sMan, accessMode, entry.getRights(), file, parentId, userId)) {
                        return;
                    } else
                        accessDenied(sMan.getVolumeId(), file, accessMode);
                    
                }
                
                // otherwise, check whether both the entry and the mask entry
                // grant access
                ACLEntry maskEntry = sMan.getACLEntry(file.getId(), MASK);
                if (checkIfAllowed(sMan, accessMode, entry.getRights(), file, parentId, userId)
                    && (maskEntry == null || checkIfAllowed(sMan, accessMode, maskEntry.getRights(), file,
                        parentId, userId)))
                    return;
                else
                    accessDenied(sMan.getVolumeId(), file, accessMode);
                
            }

            // if not, use the file permissions for the access check ...
            else {
                if (checkIfAllowed(sMan, accessMode, toRelativeACLRights(file.getPerms(), file, parentId,
                    userId, groupIds), file, parentId, userId))
                    return;
                else
                    accessDenied(sMan.getVolumeId(), file, accessMode);
            }
            
        } catch (UserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
        
    }
    
    @Override
    public void checkSearchPermission(StorageManager sMan, PathResolver res, String userId,
        List<String> groupIds) throws UserException, MRCException {
        
        try {
            
            // iteratively check search permissions for all directories in the
            // path
            FileMetadata[] rp = res.getResolvedPath();
            for (int i = 0; i < rp.length - 1; i++)
                checkPermission(sMan, rp[i], i == 0 ? 0 : rp[i - 1].getId(), userId, groupIds, AM_EXECUTE);
            
        } catch (UserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }
    
    @Override
    public void checkPrivilegedPermissions(StorageManager sMan, FileMetadata file, String userId,
        List<String> groupIds) throws UserException, MRCException {
        
        try {
            
            if (!file.getOwnerId().equals(userId))
                throw new UserException(ErrNo.EPERM, "no privileged permissions granted");
            
        } catch (UserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }
    
    @Override
    public void setACLEntries(StorageManager sMan, FileMetadata file, long parentId, String userId,
        List<String> groupIds, Map<String, Object> entries, AtomicDBUpdate update) throws MRCException,
        UserException {
        
        try {
            
            Map<String, Object> aclMap = null;
            
            // if no ACl has been defined yet, create a minimal ACL first
            Iterator<ACLEntry> acl = sMan.getACL(file.getId());
            if (!acl.hasNext())
                aclMap = convertToACL(file.getPerms());
            
            // otherwise, retrieve the current ACL
            else {
                aclMap = new HashMap<String, Object>();
                while (acl.hasNext()) {
                    ACLEntry next = acl.next();
                    aclMap.put(next.getEntity(), next.getRights());
                }
            }
            
            aclMap.putAll(entries);
            
            // add the ACL entries
            for (Entry<String, Object> entry : aclMap.entrySet()) {
                Number rights = (Number) entry.getValue();
                sMan.setACLEntry(file.getId(), entry.getKey(), rights == null ? null : rights.shortValue(),
                    update);
            }
            
            // modify the POSIX access value
            int owner = ((Number) aclMap.get(OWNER)).intValue();
            Integer group = aclMap.get(MASK) != null ? ((Number) aclMap.get(MASK)).intValue() : null;
            if (group == null)
                group = ((Number) aclMap.get(OWNER_GROUP)).intValue();
            int other = ((Number) aclMap.get(OTHER)).intValue();
            
            int posixRights = ((owner & PERM_SUID_SGID) > 0 ? POSIX_SUID : 0)
                | ((group & PERM_SUID_SGID) > 0 ? POSIX_SGID : 0) | (file.getPerms() & POSIX_STICKY)
                | ((owner & PERM_READ) > 0 ? POSIX_OWNER_READ : 0)
                | ((owner & PERM_WRITE) > 0 ? POSIX_OWNER_WRITE : 0)
                | ((owner & PERM_EXECUTE) > 0 ? POSIX_OWNER_EXEC : 0)
                | ((group & PERM_READ) > 0 ? POSIX_GROUP_READ : 0)
                | ((group & PERM_WRITE) > 0 ? POSIX_GROUP_WRITE : 0)
                | ((group & PERM_EXECUTE) > 0 ? POSIX_GROUP_EXEC : 0)
                | ((other & PERM_READ) > 0 ? POSIX_OTHER_READ : 0)
                | ((other & PERM_WRITE) > 0 ? POSIX_OTHER_WRITE : 0)
                | ((other & PERM_EXECUTE) > 0 ? POSIX_OTHER_EXEC : 0);
            
            file.setPerms(posixRights);
            
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
        
    }
    
    @Override
    public Map<String, Object> getACLEntries(StorageManager sMan, FileMetadata file) throws MRCException {
        
        try {
            Iterator<ACLEntry> acl = sMan.getACL(file.getId());
            return Converter.aclToMap(acl);
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }
    
    @Override
    public void removeACLEntries(StorageManager sMan, FileMetadata file, long parentId, String userId,
        List<String> groupIds, List<Object> entities, AtomicDBUpdate update) throws MRCException,
        UserException {
        
        Map<String, Object> entries = new HashMap<String, Object>();
        for (Object entity : entities)
            entries.put((String) entity, null);
        
        setACLEntries(sMan, file, parentId, userId, groupIds, entries, update);
    }
    
    @Override
    public void setPosixAccessRights(StorageManager sMan, FileMetadata file, long parentId, String userId,
        List<String> groupIds, int posixAccessRights, AtomicDBUpdate update) throws MRCException,
        UserException {
        
        try {
            
            // update the permissions value
            file.setPerms(posixAccessRights);
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            
            // check whether an ACL is defined; if not, return;
            // otherwise, change the ACL entries accordingly
            if (!sMan.getACL(file.getId()).hasNext())
                return;
            
            // determine rights mask for owner
            short owr = (posixAccessRights & POSIX_OWNER_EXEC) > 0 ? EXEC_MASK : 0;
            owr |= (posixAccessRights & POSIX_OWNER_WRITE) > 0 ? WRITE_MASK : 0;
            owr |= (posixAccessRights & POSIX_OWNER_READ) > 0 ? READ_MASK : 0;
            owr |= (posixAccessRights & POSIX_SUID) > 0 ? PERM_SUID_SGID : 0;
            
            // determine rights mask for group
            short grr = (posixAccessRights & POSIX_GROUP_EXEC) > 0 ? EXEC_MASK : 0;
            grr |= (posixAccessRights & POSIX_GROUP_WRITE) > 0 ? WRITE_MASK : 0;
            grr |= (posixAccessRights & POSIX_GROUP_READ) > 0 ? READ_MASK : 0;
            grr |= (posixAccessRights & POSIX_SGID) > 0 ? PERM_SUID_SGID : 0;
            
            // determine rights mask for others
            short otr = (posixAccessRights & POSIX_OTHER_EXEC) > 0 ? EXEC_MASK : 0;
            otr |= (posixAccessRights & POSIX_OTHER_WRITE) > 0 ? WRITE_MASK : 0;
            otr |= (posixAccessRights & POSIX_OTHER_READ) > 0 ? READ_MASK : 0;
            
            sMan.setACLEntry(file.getId(), OWNER, owr, update);
            sMan.setACLEntry(file.getId(), OWNER_GROUP, grr, update);
            sMan.setACLEntry(file.getId(), MASK, grr, update);
            sMan.setACLEntry(file.getId(), OTHER, otr, update);
            
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
        
    }
    
    @Override
    public int getPosixAccessRights(StorageManager sMan, FileMetadata file, String userId,
        List<String> groupIds) throws MRCException {
        return file.getPerms();
    }
    
    @Override
    public ACLEntry[] getDefaultRootACL(StorageManager sMan) {
        return null;
    }
    
    private static boolean checkIfAllowed(StorageManager sMan, String accessMode, short aclRights,
        FileMetadata file, long parentId, String userId) throws DatabaseException {
        
        if (accessMode.length() == 1) {
            switch (accessMode.charAt(0)) {
            case 'r':
                return (aclRights & PERM_READ) != 0;
            case 'w':
                return (aclRights & PERM_WRITE) != 0;
            case 'x':
                return (aclRights & PERM_EXECUTE) != 0;
            case 'a':
                return (aclRights & PERM_APPEND) != 0;
            case 'c':
                return (aclRights & PERM_CREATE) != 0;
            case 't':
                return (aclRights & PERM_TRUNCATE) != 0;
            case 'd':
                return (aclRights & PERM_DELETE) != 0;
            case 'm':

                assert (parentId != 0);
                
                // get the parent directory
                FileMetadata parent = sMan.getMetadata(parentId);
                
                // evaluate the parent's sticky bit
                if ((parent.getPerms() & POSIX_STICKY) != 0)
                    return file.getOwnerId().equals(userId);
                else
                    return true;
            }
            
        } else if (accessMode.length() == 2) {
            if (accessMode.equals("ga") && (aclRights & PERM_GFS_APPEND) != 0)
                return true;
            else if (accessMode.equals("sr") && (aclRights & PERM_STRICT_READ) != 0)
                return true;
        }
        
        return false;
    }
    
    private static short toRelativeACLRights(int posixRights, FileMetadata file, long parentId,
        String userId, List<String> groupIDs) {
        
        // owner is relevant
        if (userId.equals(file.getOwnerId())) {
            
            short tmp = 0;
            
            if ((posixRights & POSIX_OWNER_EXEC) > 0)
                tmp |= EXEC_MASK;
            if ((posixRights & POSIX_OWNER_WRITE) > 0)
                tmp |= WRITE_MASK;
            if ((posixRights & POSIX_OWNER_READ) > 0)
                tmp |= READ_MASK;
            
            return tmp;
        }

        // owning group is relevant
        else if (groupIDs.contains(file.getOwningGroupId())) {
            
            short tmp = 0;
            
            if ((posixRights & POSIX_GROUP_EXEC) > 0)
                tmp |= EXEC_MASK;
            if ((posixRights & POSIX_GROUP_WRITE) > 0)
                tmp |= WRITE_MASK;
            if ((posixRights & POSIX_GROUP_READ) > 0)
                tmp |= READ_MASK;
            
            return tmp;
        }

        // other is relevant
        else {
            
            short tmp = 0;
            
            if ((posixRights & POSIX_OTHER_EXEC) > 0)
                tmp |= EXEC_MASK;
            if ((posixRights & POSIX_OTHER_WRITE) > 0)
                tmp |= WRITE_MASK;
            if ((posixRights & POSIX_OTHER_READ) > 0)
                tmp |= READ_MASK;
            
            return tmp;
        }
        
    }
    
    private static ACLEntry getRelevantACLEntry(StorageManager sMan, FileMetadata file, long parentId,
        String userId, List<String> groupIds, String accessMode) throws UserException, DatabaseException {
        
        // if the user ID is the owner, check access according to the rights
        // associated with the owner entry
        if (file.getOwnerId().equals(userId)) {
            
            ACLEntry entry = sMan.getACLEntry(file.getId(), OWNER);
            assert (entry != null);
            
            return entry;
        }
        
        // if the user ID refers to a named user, check access according to
        // the corresponding user rights
        ACLEntry entry = sMan.getACLEntry(file.getId(), NAMED_USER_PREFIX + userId);
        if (entry != null)
            return entry;
        
        boolean groupFound = false;
        
        // if a group ID refers to the owning group, check whether access is
        // granted according to the owning group rights
        for (String groupId : groupIds) {
            if (groupId.equals(file.getOwningGroupId())) {
                
                entry = sMan.getACLEntry(file.getId(), OWNER_GROUP);
                if (checkIfAllowed(sMan, accessMode, entry.getRights(), file, parentId, userId))
                    return entry;
                
                groupFound = true;
            }
        }
        
        // if a group ID refers to any of the named groups, check whether
        // access is granted according to the corresponding group rights
        for (String groupId : groupIds) {
            
            entry = sMan.getACLEntry(file.getId(), NAMED_GROUP_PREFIX + groupId);
            
            if (entry != null) {
                
                if (checkIfAllowed(sMan, accessMode, entry.getRights(), file, parentId, userId))
                    return entry;
                
                groupFound = true;
            }
        }
        
        // if there was a matching entry but access was not granted, access
        // is denied
        if (groupFound)
            accessDenied(sMan.getVolumeId(), file, accessMode);
        
        entry = sMan.getACLEntry(file.getId(), OTHER);
        assert (entry != null);
        return entry;
    }
    
    private Map<String, Object> convertToACL(long mode) throws MRCException {
        
        try {
            
            Map<String, Object> aclMap = new HashMap<String, Object>();
            
            // determine the sticky bit
            long stickyBit = (mode & (1 << 9)) > 0 ? 1 : 0;
            if (stickyBit != 0)
                aclMap.put(STICKY_BIT, stickyBit);
            
            // determine ACL for owner
            long owr = (mode & POSIX_OWNER_EXEC) > 0 ? EXEC_MASK : 0;
            owr |= (mode & POSIX_OWNER_WRITE) > 0 ? WRITE_MASK : 0;
            owr |= (mode & POSIX_OWNER_READ) > 0 ? READ_MASK : 0;
            owr |= (mode & POSIX_SUID) > 0 ? PERM_SUID_SGID : 0;
            aclMap.put(OWNER, owr);
            
            // determine ACL for group
            long grr = (mode & POSIX_GROUP_EXEC) > 0 ? EXEC_MASK : 0;
            grr |= (mode & POSIX_GROUP_WRITE) > 0 ? WRITE_MASK : 0;
            grr |= (mode & POSIX_GROUP_READ) > 0 ? READ_MASK : 0;
            grr |= (mode & POSIX_SGID) > 0 ? PERM_SUID_SGID : 0;
            aclMap.put(OWNER_GROUP, grr);
            
            // determine ACL for others
            long otr = (mode & POSIX_OTHER_EXEC) > 0 ? EXEC_MASK : 0;
            otr |= (mode & POSIX_OTHER_WRITE) > 0 ? WRITE_MASK : 0;
            otr |= (mode & POSIX_OTHER_READ) > 0 ? READ_MASK : 0;
            aclMap.put(OTHER, otr);
            
            return aclMap;
            
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }
    
    private static void accessDenied(String volumeId, FileMetadata file, String accessMode)
        throws UserException {
        
        throw new UserException(ErrNo.EACCES, "access denied, volumeId = " + volumeId + ", file = "
            + file.getId() + " (" + file.getFileName() + "), accessMode = \"" + accessMode + "\"");
    }
    
}
