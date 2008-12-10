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
 * This class encapsulates data of a file attribute in the database. File
 * attributes are used to store additional data about files, such as
 * user-defined extended metadata or other file metadata which is not stored in
 * file or directory entities.
 *
 * There is a many-to-one relationship between files and file attributes; a file
 * may have multiple attributes, and each file attribute entity belongs to a
 * single file which is identified by the 'fileId' field.
 *
 * A file attribute has a key and a value. In the scope of a file, the key
 * uniquely identifies the attribute. The value holds the attribute data.
 *
 * A file attribute may be either of type 'SYSTEM' or 'USER'. System attributes
 * are used for storing file metadata that is not part of the file entity or
 * directory entity describing the file itself, such as a reference for a
 * symbolic link or a striping policy string. User attributes are used for
 * storing extended user metadata, such as annotations describing the file
 * content.
 *
 * @author stender
 *
 */
public class FileAttributeEntity<T extends Object> implements Serializable {

    public static final int TYPE_USER   = 0;

    public static final int TYPE_SYSTEM = 1;

    private String          id;

    private String          key;

    private T               value;

    private long            type;

    private long            fileId;

    private String          userId;

    public FileAttributeEntity() {
    }

    public FileAttributeEntity(FileAttributeEntity<T> entity) {
        this.id = entity.id;
        this.key = entity.key;
        this.value = entity.value;
        this.type = entity.type;
        this.fileId = entity.fileId;
        this.userId = entity.userId;
    }

    public FileAttributeEntity(String key, T value, long type, long fileId,
        String userId) {
        this.id = fileId + ":" + key
            + (type == TYPE_SYSTEM ? "" : (":" + userId));
        this.key = key;
        this.value = value;
        this.fileId = fileId;
        this.userId = userId;
        this.type = type;
    }

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public String toString() {
        return "<" + key + "=" + value + ", file: " + fileId + ", type: "
            + (type == TYPE_SYSTEM ? "SYSTEM" : "USER") + ", user: " + userId
            + ">";
    }

}
