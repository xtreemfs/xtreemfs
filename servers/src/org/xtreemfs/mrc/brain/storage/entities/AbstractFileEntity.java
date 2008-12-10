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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.brain.storage.entities;

import java.io.Serializable;

/**
 * The abstract file entity is the base class for file and directory entities.
 *
 * File and directory entities are used to store such metadata about a file that
 * is mandatory and frequently used. File metadata that is only assigned to
 * certain files is stored in the form of meta attributes, such as a target
 * reference in case the file is a symbolic link.
 *
 * The abstract file entity encapsulates all data that files and directories
 * have in common, such as a name, POSIX timestamps or an ACL.
 *
 * @see FileEntity
 * @see DirEntity
 *
 * @author stender
 *
 */
public abstract class AbstractFileEntity implements Serializable {

    private long       id;

    private String     indexId;

    private long       atime;

    private long       ctime;

    private long       mtime;

    private String     userId;

    private String     groupId;

    private ACLEntry[] acl;

    private long       linkCount;

    public AbstractFileEntity() {
    }

    public AbstractFileEntity(long id, String userId, String groupId, long atime, long ctime,
        long mtime, ACLEntry[] acl, long linkCount) {

        this.id = id;
        this.userId = userId;
        this.groupId = groupId;
        this.atime = atime;
        this.ctime = ctime;
        this.mtime = mtime;
        this.acl = acl;
        this.linkCount = linkCount;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAtime() {
        return atime;
    }

    public void setAtime(long atime) {
        this.atime = atime;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public ACLEntry[] getAcl() {
        return acl;
    }

    public void setAcl(ACLEntry[] acl) {
        this.acl = acl;
    }

    public long getLinkCount() {
        return linkCount;
    }

    public void setLinkCount(long linkCount) {
        this.linkCount = linkCount;
    }

    public abstract boolean isDirectory();

    public String toString() {
        return (isDirectory() ? "D" : "") + id + " (" + linkCount + " links)";
    }

}
