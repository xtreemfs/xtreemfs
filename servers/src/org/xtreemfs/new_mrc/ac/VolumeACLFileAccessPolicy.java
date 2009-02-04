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
import java.util.Map.Entry;

import org.xtreemfs.new_mrc.ErrNo;
import org.xtreemfs.new_mrc.MRCException;
import org.xtreemfs.new_mrc.UserException;
import org.xtreemfs.new_mrc.dbaccess.AtomicDBUpdate;
import org.xtreemfs.new_mrc.dbaccess.StorageManager;
import org.xtreemfs.new_mrc.metadata.ACLEntry;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.new_mrc.operations.PathResolver;

/**
 * This policy grants or denies access based on immutable volume ACLs. Note that
 * ACLs are no POSIX ACLs. A 'default' entry may be defined that is valid for
 * any user except for those having a user-specific entry.
 * 
 * @author stender
 * 
 */
public class VolumeACLFileAccessPolicy implements FileAccessPolicy {
    
    public static final short   POLICY_ID          = 3;
    
    private static final String AM_WRITE           = "w";
    
    private static final String AM_READ            = "r";
    
    private static final String AM_DELETE          = "d";
    
    private static final String DEFAULT_ENTRY_NAME = "default";
    
    @Override
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
    
    @Override
    public void checkPermission(StorageManager sMan, FileMetadata file, long parentId,
        String userId, List<String> groupIds, String accessMode) throws UserException, MRCException {
        
        try {
            
            if (file == null)
                return;
            
            ACLEntry entry = sMan.getACLEntry(1, userId);
            if (entry == null)
                entry = sMan.getACLEntry(1, DEFAULT_ENTRY_NAME);
            
            long rights = entry.getRights();
            
            if (accessMode.length() == 1) {
                switch (accessMode.charAt(0)) {
                case 'r':
                    if ((rights & (1 << 0)) != 0)
                        return;
                    break;
                case 'w':
                    if ((rights & (1 << 1)) != 0)
                        return;
                    break;
                case 'a':
                    if ((rights & (1 << 2)) != 0)
                        return;
                    break;
                case 'c':
                    if ((rights & (1 << 4)) != 0)
                        return;
                    break;
                case 't':
                    if ((rights & (1 << 5)) != 0)
                        return;
                    break;
                case 'd':
                    if ((rights & (1 << 7)) != 0)
                        return;
                    break;
                }
            } else if (accessMode.length() == 2) {
                if (accessMode.equals("ga") && (rights & (1 << 3)) != 0)
                    return;
                if (accessMode.equals("sr"))
                    if ((rights & (1 << 6)) != 0)
                        return;
            }
            
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
        
        throw new UserException(ErrNo.EACCES, "access denied, volumeId = " + sMan.getVolumeId()
            + ", fileId = " + file.getId() + ", accessMode = \"" + accessMode + "\"");
    }
    
    @Override
    public void checkSearchPermission(StorageManager sMan, PathResolver res, String userId,
        List<String> groupIds) throws UserException, MRCException {
        
        try {
            FileMetadata rootDir = sMan.getMetadata(0, sMan.getVolumeName());
            checkPermission(sMan, rootDir, 0, userId, groupIds, AM_READ);
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }
    
    @Override
    public void checkPrivilegedPermissions(StorageManager sMan, FileMetadata file, String userId,
        List<String> groupIds) throws UserException, MRCException {
        
        try {
            
            if (!sMan.getMetadata(0, sMan.getVolumeName()).getOwnerId().equals(userId))
                throw new UserException(ErrNo.EPERM, "no privileged permissions granted");
            
        } catch (UserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }
    
    @Override
    public short getPosixAccessRights(StorageManager sMan, FileMetadata file, String userId,
        List<String> groupIds) throws MRCException {
        
        try {
            ACLEntry entry = sMan.getACLEntry(1, userId);
            if (entry == null)
                entry = sMan.getACLEntry(1, DEFAULT_ENTRY_NAME);
            
            // rw - mask, x = r
            short rights = (short) (entry.getRights() & 3 | ((entry.getRights() & 1) << 2));
            return (short) (rights * (1 << 6));
            
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }
    
    @Override
    public void setPosixAccessRights(StorageManager sMan, FileMetadata file, long parentId,
        String userId, List<String> groupIds, short posixAccessRights, AtomicDBUpdate update)
        throws MRCException {
        
        try {
            sMan.setACLEntry(1, DEFAULT_ENTRY_NAME, posixAccessRights, update);
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }
    
    @Override
    public void setACLEntries(StorageManager sMan, FileMetadata file, long parentId, String userId,
        List<String> groupIds, Map<String, Object> entries, AtomicDBUpdate update)
        throws MRCException, UserException {
        
        try {
            for (Entry<String, Object> entry : entries.entrySet())
                sMan.setACLEntry(1, entry.getKey(), (Short) entry.getValue(), update);
            
        } catch (Exception exc) {
            throw new MRCException(exc);
        }
    }
    
    @Override
    public void removeACLEntries(StorageManager sMan, FileMetadata file, long parentId,
        String userId, List<String> groupIds, List<Object> entities, AtomicDBUpdate update)
        throws MRCException, UserException {
        // do nothing
    }
    
    @Override
    public ACLEntry[] getDefaultRootACL(StorageManager sMan) {
        ACLEntry[] acl = new ACLEntry[1];
        acl[0] = sMan.createACLEntry(1, DEFAULT_ENTRY_NAME, (short) 511);
        return acl;
    }
    
    @Override
    public short getDefaultRootRights() {
        return 0;
    }
    
}
