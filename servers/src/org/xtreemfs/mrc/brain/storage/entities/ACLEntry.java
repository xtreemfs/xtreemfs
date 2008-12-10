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
 * This class represents a single entry of an ACL assigned to a file, directory
 * or volume. An ACL entry is a key-value pair mapping some entity to a set of
 * access rights.
 *
 * How an ACL is used in detail depends on the access control policy held by the
 * volume.
 *
 * @author stender
 *
 */
public class ACLEntry implements Serializable {

    private String entity;

    private long rights;

    public ACLEntry() {
    }

    public ACLEntry(String userId, long rights) {
        this.entity = userId;
        this.rights = rights;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String userId) {
        this.entity = userId;
    }

    public long getRights() {
        return rights;
    }

    public void setRights(long rights) {
        this.rights = rights;
    }

    public String toString() {
        return entity + "=" + rights;
    }

}
