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
import java.util.Map.Entry;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseResultSet;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.ACLEntry;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;

/**
 * This policy evaluates access rights according to POSIX permissions and access control lists (ACLs). The
 * implementation is based on the description provided in "POSIX Access Control Lists on Linux" by Andreas
 * Grünbacher (http://www.suse.de/~agruen/acl/linux-acls/online/).
 * 
 * <p>
 * In the general case, access permissions are granted or denied based on the 16 bit permissions value which
 * is directly associated with the file metadata. This allows to distinguish 'read', 'write' and 'execute'
 * rights for the owner, the owing group and the rest of the world.
 * 
 * <p>
 * A more fine-grained access control model can be supported by using ACLs. Access control decisions will be
 * made based on ACLs if at least the following ACL entries exist for a file:
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
 * The access control granularity w/ ACLs is not restricted to 'read', 'write' and 'execute'. Further access
 * modes and bits are evaluated (the values in brackets decribe the corresponding bit value in the ACL entry):
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
 * When checking access to a file or directory, the policy will search for the relevant ACL entry according to
 * the POSIX access check algorithm, where the 'rights' value of the ACL entry is interpreted as a bit mask.
 * The bits for the corresponding access modes are set in the reverse order as they are enumerated above.
 * 
 * <p>
 * Example: an ACL entry ("user:", 35) would grant read, write and create access to the file owner, because 35
 * represents the bit mask 000100011.
 * 
 * @author stender
 */
public class POSIXFileAccessPolicy implements FileAccessPolicy {

    public static final short     POLICY_ID          = (short) AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX
                                                             .getNumber();

    protected static final String OWNER              = "u:";

    protected static final String OWNER_GROUP        = "g:";

    protected static final String OTHER              = "o:";

    protected static final String MASK               = "m:";

    protected static final String NAMED_USER_PREFIX  = "u:";

    protected static final String NAMED_GROUP_PREFIX = "g:";

    protected static final String STICKY_BIT         = "sticky";

    protected static final String AM_WRITE           = "w";

    protected static final String AM_READ            = "r";

    protected static final String AM_READ_WRITE      = "rw";

    protected static final String AM_EXECUTE         = "x";

    protected static final String AM_DELETE          = "d";

    protected static final String AM_MV_RM_IN_DIR    = "m";

    protected static final int    POSIX_OTHER_EXEC   = 1 << 0;

    protected static final int    POSIX_OTHER_WRITE  = 1 << 1;

    protected static final int    POSIX_OTHER_READ   = 1 << 2;

    protected static final int    POSIX_GROUP_EXEC   = 1 << 3;

    protected static final int    POSIX_GROUP_WRITE  = 1 << 4;

    protected static final int    POSIX_GROUP_READ   = 1 << 5;

    protected static final int    POSIX_OWNER_EXEC   = 1 << 6;

    protected static final int    POSIX_OWNER_WRITE  = 1 << 7;

    protected static final int    POSIX_OWNER_READ   = 1 << 8;

    protected static final int    POSIX_STICKY       = 1 << 9;

    protected static final int    POSIX_SGID         = 1 << 10;

    protected static final int    POSIX_SUID         = 1 << 11;

    protected static final short  PERM_READ          = 1 << 0;

    protected static final short  PERM_WRITE         = 1 << 1;

    protected static final short  PERM_EXECUTE       = 1 << 2;

    protected static final short  PERM_APPEND        = 1 << 3;

    protected static final short  PERM_GFS_APPEND    = 1 << 4;

    protected static final short  PERM_CREATE        = 1 << 5;

    protected static final short  PERM_TRUNCATE      = 1 << 6;

    protected static final short  PERM_STRICT_READ   = 1 << 7;

    protected static final short  PERM_DELETE        = 1 << 8;

    protected static final short  PERM_SUID_SGID     = 1 << 14;

    protected static final short  READ_MASK          = PERM_READ | PERM_STRICT_READ;

    protected static final short  WRITE_MASK         = PERM_WRITE | PERM_APPEND | PERM_GFS_APPEND | PERM_CREATE
                                                             | PERM_TRUNCATE | PERM_DELETE;

    protected static final short  EXEC_MASK          = PERM_EXECUTE;

    protected static final short  READ_ONLY_MASK     = (-1 & 365);

    public POSIXFileAccessPolicy() {
    }

    @Override
    public String translateAccessFlags(int accessMode) {

        accessMode = accessMode
                & (FileAccessManager.O_RDWR | FileAccessManager.O_WRONLY | FileAccessManager.O_APPEND
                        | FileAccessManager.O_TRUNC | FileAccessManager.NON_POSIX_SEARCH
                        | FileAccessManager.NON_POSIX_DELETE | FileAccessManager.NON_POSIX_RM_MV_IN_DIR);

        if (accessMode == FileAccessManager.O_RDONLY)
            return AM_READ;
        if (((accessMode & FileAccessManager.O_WRONLY) != 0) || ((accessMode & FileAccessManager.O_APPEND) != 0)
                || ((accessMode & FileAccessManager.O_TRUNC) != 0))
            return AM_WRITE;
        if ((accessMode & FileAccessManager.O_RDWR) != 0)
            return AM_READ_WRITE;
        if ((accessMode & FileAccessManager.NON_POSIX_SEARCH) != 0)
            return AM_EXECUTE;
        if ((accessMode & FileAccessManager.NON_POSIX_DELETE) != 0)
            return AM_DELETE;
        if ((accessMode & FileAccessManager.NON_POSIX_RM_MV_IN_DIR) != 0)
            return AM_MV_RM_IN_DIR;

        assert (false) : "unknown access mode: " + accessMode;
        return null;
    }

    @Override
    public String translatePermissions(int permissions) {

        StringBuilder sb = new StringBuilder();
        sb.append((permissions & PERM_READ) > 0 ? "r" : "-");
        sb.append((permissions & PERM_WRITE) > 0 ? "w" : "-");
        sb.append((permissions & PERM_EXECUTE) > 0 ? "x" : "-");

        return sb.toString();
    }

    @Override
    public void checkPermission(StorageManager sMan, FileMetadata file, long parentId, String userId,
            List<String> groupIds, String accessMode) throws UserException, MRCException {

        assert (file != null);

        DatabaseResultSet<ACLEntry> aclSet = null;
        try {

            // check whether an ACL exists; if so, use the ACL for the access
            // check ...
            aclSet = sMan.getACL(file.getId());
            if (aclSet.hasNext()) {

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
                        accessDenied(sMan.getVolumeInfo().getId(), file, accessMode, userId);

                }

                // otherwise, check whether both the entry and the mask entry
                // grant access
                ACLEntry maskEntry = sMan.getACLEntry(file.getId(), MASK);
                if (checkIfAllowed(sMan, accessMode, entry.getRights(), file, parentId, userId)
                        && (maskEntry == null || checkIfAllowed(sMan, accessMode, maskEntry.getRights(), file,
                                parentId, userId)))
                    return;
                else
                    accessDenied(sMan.getVolumeInfo().getId(), file, accessMode, userId);

            }

            // if not, use the file permissions for the access check ...
            else {
                if (checkIfAllowed(sMan, accessMode,
                        toRelativeACLRights(file.getPerms(), file, parentId, userId, groupIds), file, parentId, userId))
                    return;
                else
                    accessDenied(sMan.getVolumeInfo().getId(), file, accessMode, userId);
            }

        } catch (UserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MRCException(exc);
        } finally {
            if (aclSet != null)
                aclSet.destroy();
        }

    }

    @Override
    public void checkSearchPermission(StorageManager sMan, PathResolver res, String userId, List<String> groupIds)
            throws UserException, MRCException {

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
    public void checkPrivilegedPermissions(StorageManager sMan, FileMetadata file, String userId, List<String> groupIds)
            throws UserException, MRCException {

        try {

            if (!file.getOwnerId().equals(userId))
                throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "no privileged permissions granted");

        } catch (UserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }

    @Override
    public void updateACLEntries(StorageManager sMan, FileMetadata file, long parentId, Map<String, Object> entries,
            AtomicDBUpdate update) throws MRCException, UserException {

        DatabaseResultSet<ACLEntry> acl = null;
        try {

            Map<String, Object> aclMap = null;

            // if no ACL has been defined yet, create a minimal ACL first
            acl = sMan.getACL(file.getId());
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

            for (Entry<String, Object> entry : entries.entrySet()) {

                String entity = entry.getKey();
                String rwx = (String) entry.getValue();

                if (rwx != null) {
                    int rights = 0;

                    // numeric value
                    if (rwx.length() == 1 && rwx.charAt(0) >= '0' && rwx.charAt(0) <= '7') {

                        rights = Integer.parseInt(rwx, 8);

                        // fix: swap 'r' and 'x' bits for a correct internal
                        // representation
                        int tmp = ((rights >> 2) ^ rights) & 1;
                        rights = rights ^ ((tmp << 2) | (tmp << 0));
                    }

                    // 'rwx' value
                    else {
                        if (rwx.indexOf('r') != -1)
                            rights |= PERM_READ;
                        if (rwx.indexOf('w') != -1)
                            rights |= PERM_WRITE;
                        if (rwx.indexOf('x') != -1)
                            rights |= PERM_EXECUTE;
                    }

                    aclMap.put(entity, rights);
                } else
                    aclMap.put(entity, null);
            }

            // add the ACL entries
            for (Entry<String, Object> entry : aclMap.entrySet()) {
                Number rights = (Number) entry.getValue();
                sMan.setACLEntry(file.getId(), entry.getKey(), rights == null ? null : rights.shortValue(), update);
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
        } finally {
            if (acl != null)
                acl.destroy();
        }

    }

    @Override
    public Map<String, Object> getACLEntries(StorageManager sMan, FileMetadata file) throws MRCException {

        try {
            DatabaseResultSet<ACLEntry> acl = sMan.getACL(file.getId());
            Map<String, Object> aclMap = Converter.aclToMap(acl, this);
            acl.destroy();
            return aclMap;

        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }

    @Override
    public void removeACLEntries(StorageManager sMan, FileMetadata file, long parentId, List<Object> entities,
            AtomicDBUpdate update) throws MRCException, UserException {

        Map<String, Object> entries = new HashMap<String, Object>();
        for (Object entity : entities)
            entries.put((String) entity, null);

        updateACLEntries(sMan, file, parentId, entries, update);
    }

    @Override
    public void setPosixAccessRights(StorageManager sMan, FileMetadata file, long parentId, String userId,
            List<String> groupIds, int posixAccessRights, boolean superUser, AtomicDBUpdate update)
            throws MRCException, UserException {

        DatabaseResultSet<ACLEntry> aclSet = null;
        try {

            // clear SGID flag if file's owning group is not contained in user
            // groups
            if ((posixAccessRights & POSIX_SGID) > 0 && !superUser && !file.isDirectory()
                    && !groupIds.contains(file.getOwningGroupId()))
                posixAccessRights ^= POSIX_SGID;

            // update the permissions value
            file.setPerms(posixAccessRights);
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);

            // check whether an ACL is defined; if not, return;
            // otherwise, change the ACL entries accordingly
            aclSet = sMan.getACL(file.getId());
            if (!aclSet.hasNext())
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
        } finally {
            if (aclSet != null)
                aclSet.destroy();
        }

    }

    @Override
    public int getPosixAccessRights(StorageManager sMan, FileMetadata file, String userId, List<String> groupIds)
            throws MRCException {
        return !file.isDirectory() && file.isReadOnly() ? file.getPerms() & READ_ONLY_MASK : file.getPerms();
    }

    @Override
    public ACLEntry[] getDefaultRootACL() {
        return null;
    }

    private static boolean checkIfAllowed(StorageManager sMan, String accessMode, short aclRights, FileMetadata file,
            long parentId, String userId) throws DatabaseException {

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

                assert (parent != null) : "cannot resolve metadata for file ID " + parentId;

                // evaluate the parent's sticky bit
                if ((parent.getPerms() & POSIX_STICKY) != 0)
                    return parent.getOwnerId().equals(userId) || file.getOwnerId().equals(userId);
                else
                    return true;
            }

        } else if (accessMode.length() == 2) {
            if (accessMode.equals("rw") && (aclRights & PERM_READ) != 0 & (aclRights & PERM_WRITE) != 0)
                return true;
            else if (accessMode.equals("ga") && (aclRights & PERM_GFS_APPEND) != 0)
                return true;
            else if (accessMode.equals("sr") && (aclRights & PERM_STRICT_READ) != 0)
                return true;
        }

        return false;
    }

    private static short toRelativeACLRights(int posixRights, FileMetadata file, long parentId, String userId,
            List<String> groupIDs) {

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

    private static ACLEntry getRelevantACLEntry(StorageManager sMan, FileMetadata file, long parentId, String userId,
            List<String> groupIds, String accessMode) throws UserException, DatabaseException {

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
            accessDenied(sMan.getVolumeInfo().getId(), file, accessMode, userId);

        entry = sMan.getACLEntry(file.getId(), OTHER);
        assert (entry != null);
        return entry;
    }

    protected static Map<String, Object> convertToACL(long mode) throws MRCException {

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

    private static void accessDenied(String volumeId, FileMetadata file, String accessMode, String userId)
            throws UserException {

        throw new UserException(POSIXErrno.POSIX_ERROR_EACCES, "access denied, volumeId = " + volumeId + ", file = "
                + file.getId() + " (" + file.getFileName() + "), accessMode = \"" + accessMode
                + "\", requestor's uid = \"" + userId + "\", owner = \"" + file.getOwnerId() + "\"");
    }

}
