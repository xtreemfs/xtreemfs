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
 * A file entity encapsulates all data needed for a file metadata object stored
 * in the database. Unlike a {@link DirEntity}, a file entity has a size and a
 * set of replicas.
 *
 * @see DirEntity
 *
 * @author stender
 *
 */
public class FileEntity extends AbstractFileEntity implements Serializable {

    private long           size;

    private XLocationsList xLocationsList;

    private long           epoch;

    private long           issuedEpoch;

    public FileEntity() {
    }

    public FileEntity(FileEntity entity) {
        this(entity.getId(), entity.getUserId(), entity.getGroupId(), entity
                .getAtime(), entity.getCtime(), entity.getMtime(), entity
                .getSize(), entity.getXLocationsList(), entity.getAcl(), entity
                .getLinkCount(), entity.getEpoch(), entity
                .getIssuedEpoch());
    }

    public FileEntity(long id, String userId, String groupId, long atime,
        long ctime, long mtime, long size, XLocationsList replicas,
        ACLEntry[] acl, long linkCount, long writeEpoch, long truncateEpoch) {

        super(id, userId, groupId, atime, ctime, mtime, acl, linkCount);

        this.size = size;
        this.xLocationsList = replicas;
        this.epoch = writeEpoch;
        this.issuedEpoch = truncateEpoch;
    }

    public XLocationsList getXLocationsList() {
        return xLocationsList;
    }

    public void setXLocationsList(XLocationsList replicas) {
        this.xLocationsList = replicas;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getEpoch() {
        return epoch;
    }

    public void setEpoch(long currentEpoch) {
        this.epoch = currentEpoch;
    }

    public long getIssuedEpoch() {
        return issuedEpoch;
    }

    public void setIssuedEpoch(long issuedEpoch) {
        this.issuedEpoch = issuedEpoch;
    }

    public boolean isDirectory() {
        return false;
    }

    public void setContent(FileEntity entity) {
        setUserId(entity.getUserId());
        setGroupId(entity.getGroupId());
        setAtime(entity.getAtime());
        setCtime(entity.getCtime());
        setMtime(entity.getMtime());
        setSize(entity.getSize());
        setXLocationsList(entity.getXLocationsList());
        setAcl(entity.getAcl());
        setLinkCount(entity.getLinkCount());
        setEpoch(entity.getEpoch());
        setIssuedEpoch(entity.getIssuedEpoch());
    }

}
