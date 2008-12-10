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
 * A directory entity encapsulates all data needed for a directory metadata
 * object stored in the database.
 *
 * @see DirEntity
 *
 * @author stender
 *
 */
public class DirEntity extends AbstractFileEntity implements Serializable {

    public DirEntity() {
    }

    public DirEntity(DirEntity entity) {
        super(entity.getId(), entity.getUserId(), entity.getGroupId(), entity.getAtime(), entity
                .getCtime(), entity.getMtime(), entity.getAcl(), entity.getLinkCount());
    }

    public DirEntity(long id, String userId, String groupId, long atime, long ctime, long mtime,
        ACLEntry[] acl, long linkCount) {
        super(id, userId, groupId, atime, ctime, mtime, acl, linkCount);
    }

    public boolean isDirectory() {
        return true;
    }

    public void setContent(DirEntity entity) {
        setUserId(entity.getUserId());
        setGroupId(entity.getGroupId());
        setAtime(entity.getAtime());
        setCtime(entity.getCtime());
        setMtime(entity.getMtime());
        setAcl(entity.getAcl());
        setLinkCount(entity.getLinkCount());
    }

}
