/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.common.clients;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.MRCInterface.MRCException;
import org.xtreemfs.interfaces.Stat;
import org.xtreemfs.interfaces.UserCredentials;

/**
 *
 * @author bjko
 */
public class File {

    private final Volume volume;

    private final String path;

    private final UserCredentials userCreds;

    File(Volume volume, UserCredentials userCreds, String path) {
        this.volume = volume;
        this.path = path;
        this.userCreds = userCreds;
    }

    public String getPath() {
        return path;
    }


    /**
     * check if path is a file
     * @see java.io.File
     * @return true if it is a file, false otherwise (also if path does not exist)
     */
    public boolean isFile() throws IOException {
        Stat stat = volume.stat(path);
        if (stat != null)
            return (stat.getMode() & Constants.SYSTEM_V_FCNTL_H_S_IFREG) > 0;
        else
            return false;
    }

    /**
     * check if path is a directory
     * @see java.io.File
     * @return true if it is a directory, false otherwise (also if path does not exist)
     */
    public boolean isDirectory() throws IOException {
        Stat stat = volume.stat(path);
        if (stat != null)
            return (stat.getMode() & Constants.SYSTEM_V_FCNTL_H_S_IFDIR) > 0;
        else
            return false;
    }

    /**
     * check if path exists (file or directory)
     * @see java.io.File
     * @return true if it exists, false otherwise
     */
    public boolean exists() throws IOException {
        try {
            Stat stat = volume.stat(path);
        } catch (FileNotFoundException ex) {
            return false;
        }
        return true;
    }

    /**
     * get file size
     * @return the files size in bytes, or 0L if it does not exist
     * @throws IOException
     */
    public long length() throws IOException {
        Stat stat = volume.stat(path);
        if (stat != null)
            return stat.getSize();
        else
            return 0L;
    }

    public void mkdir(int permissions) throws IOException {
        volume.mkdir(path, permissions);
    }

    public void createFile() throws IOException {
        volume.touch(path);
    }

    public Stat stat() throws IOException {
        return volume.stat(path);
    }

    public void renameTo(File dest) throws IOException {
        volume.rename(this.path,dest.path);
    }

    public void delete() throws IOException {
        volume.unlink(this.path);
    }

    public String getxattr(String name) throws IOException {
        return volume.getxattr(path, name);
    }

    public void setxattr(String name, String value) throws IOException {
        volume.setxattr(path, name, value);
    }

    public RandomAccessFile open(String openMode, int permissions) throws IOException {
        int flags = 0;
        if (openMode.equals("r")) {
            flags |= Constants.SYSTEM_V_FCNTL_H_O_RDONLY;
        } else if (openMode.equals("rw")) {
            flags |= Constants.SYSTEM_V_FCNTL_H_O_RDWR;
            flags |= Constants.SYSTEM_V_FCNTL_H_O_CREAT;
        }
        if (openMode.contains("t")) {
            flags |= Constants.SYSTEM_V_FCNTL_H_O_TRUNC;
        }
        if (openMode.contains("d") || openMode.contains("s")) {
            flags |= Constants.SYSTEM_V_FCNTL_H_O_SYNC;
        }
        return volume.openFile(this, flags, permissions);
    }

    public FileReplication getFileReplication() {
        return new FileReplication(this, volume);
    }

}
