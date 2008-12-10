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

import org.xtreemfs.mrc.brain.BrainException;
import org.xtreemfs.mrc.brain.UserException;

/**
 * This policy will grant access to anyone. It does not allow changeing access
 * rights, any ACLs set on files or volumes will be ignored.
 * 
 * @author stender
 * 
 */
public class YesToAnyoneFileAccessPolicy implements FileAccessPolicy {
    
    public static final long    POLICY_ID = 1;
    
    private static final String AM_WRITE  = "w";
    
    private static final String AM_READ   = "r";
    
    private static final String AM_DELETE = "d";
    
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
        List<String> groupIds, String accessMode) {
        // do nothing
    }
    
    public void checkSearchPermission(String volumeId, String path, String userId,
        List<String> groupIds) {
        // do nothing
    }
    
    public void checkPrivilegedPermissions(String volumeId, long fileId, String userId,
        List<String> groupIds) {
        // do nothing
    }
    
    public Map<String, Object> createDefaultVolumeACL(String volumeId) throws BrainException {
        return null;
    }
    
    public Map<String, Object> convertToACL(long mode) {
        return null;
    }
    
    public long getPosixAccessRights(String volumeId, long fileId, String userId,
        List<String> groupIds) {
        return 511; // rwxrwxrwx
    }
    
    public void setPosixAccessRights(String volumeId, long fileId, String userId,
        List<String> groupIds, long posixRights) {
        // do nothing
    }
    
    public void setACLEntries(String volumeId, long fileId, String userId, List<String> groupIDs,
        Map<String, Object> entries) throws BrainException, UserException {
        // do nothing
    }
    
    public void removeACLEntries(String volumeId, long fileId, String userId,
        List<String> groupIds, List<Object> entities) throws BrainException, UserException {
        // do nothing
    }
    
}
