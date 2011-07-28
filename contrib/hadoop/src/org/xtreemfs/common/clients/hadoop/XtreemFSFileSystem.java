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

package org.xtreemfs.common.clients.hadoop;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FSInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.DirectoryEntry;
import org.xtreemfs.interfaces.Stat;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.UserCredentials;

/**
 * A XtreemFS driver for hadoop
 * URI format: xtreemfs://dirAddr:port/path...
 * required configuration:
 * <PRE>
 * <property>
 * <name>xtreemfs.volumeName</name>
 * <value>volumeName</value>
 * <description>Name of the volume to use within XtreemFS.</description>
 * </property>
 * 
 * <property>
 * <name>fs.xtreemfs.impl</name>
 * <value>org.xtreemfs.common.clients.hadoop.XtreemFSFileSystem</value>
 * <description>The FileSystem for xtreemfs: uris.</description>
 * </property>
 * </PRE>
 * @author bjko
 */
public class XtreemFSFileSystem extends FileSystem {

    private Client  xtreemfsClient;

    private Volume  volume;

    private URI     fsURI;
    
    private Path    workingDirectory;


    @Override
    public void initialize(URI uri, Configuration conf) throws IOException {
        super.initialize(uri, conf);
        int logLevel = Logging.LEVEL_WARN;
        if (conf.getBoolean("xtreemfs.client.debug",false)) {
            logLevel = Logging.LEVEL_DEBUG;
        }
        
        String volumeName = conf.get("xtreemfs.volumeName");
        if (volumeName == null)
            throw new IOException("You have to specify a volume name at the" +
            " core-site.xml! (xtreemfs.volumeName)");
            
        Logging.start(logLevel, Category.all);
        Logging.logMessage(Logging.LEVEL_DEBUG, this,"init : "+uri);
        InetSocketAddress dir = new InetSocketAddress(uri.getHost(), uri.getPort());
        xtreemfsClient = new Client(new InetSocketAddress[]{dir}, 30*1000, 15*60*1000, null);
        try {
            xtreemfsClient.start();
        } catch (Exception ex) {
            throw new IOException("cannot start XtreemFS client", ex);
        }

        UserCredentials uc = null;
        if ( (conf.get("xtreemfs.client.userid") != null)
            && (conf.get("xtreemfs.client.groupid") != null) ){
            StringSet grps = new StringSet();
            grps.add(conf.get("xtreemfs.client.groupid"));
            uc = new UserCredentials(conf.get("xtreemfs.client.userid"), grps, "");
        }
        if (uc == null) {
            //try to guess from env
            if (System.getenv("USER") != null) {
                StringSet grps = new StringSet();
                grps.add("users");
                uc = new UserCredentials(System.getProperty("user.name"), grps, "");
            }
        }

        fsURI = uri;
        workingDirectory = getHomeDirectory();
        volume = xtreemfsClient.getVolume(volumeName, uc);
        Logging.logMessage(Logging.LEVEL_DEBUG, this,"file system init complete: "+uri.getUserInfo());
    }

    @Override
    public URI getUri() {
        return fsURI;
    }

    @Override
    public FSDataInputStream open(Path file, final int buffSize) throws IOException {
        final String path = file.toUri().getPath();
        File f = volume.getFile(path);
        final RandomAccessFile raf = f.open("r",0);

        return new FSDataInputStream(new FSInputStream() {

            @Override
            public void seek(long pos) throws IOException {
                raf.seek(pos);
            }

            @Override
            public long getPos() throws IOException {
                return raf.getFilePointer();
            }

            @Override
            public boolean seekToNewSource(long arg0) throws IOException {
                return false;
            }

            @Override
            public int read() throws IOException {
                byte[] b = new byte[1];
                int numRead = raf.read(b,0,1);
                if (numRead == 0)
                    return -1;
                return b[0];
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int nbRead = raf.read(b,off,len);
                if ((nbRead == 0) && (len > 0))
                    return -1;
                else
                    return nbRead;
            }

            @Override
            public int read(byte[] b) throws IOException {
                return raf.read(b,0,b.length);
            }
        });

    }

    @SuppressWarnings("deprecation")
    @Override
    public FSDataOutputStream create(Path file, FsPermission permissions,
            boolean overwrite, int bufferSize, short replication, long blockSize, Progressable prog) throws IOException {
        final String path = file.toUri().getPath();
        File f = volume.getFile(path);
        String openMode = "rw";
        if (overwrite)
            openMode += "t";
        
        final RandomAccessFile raf = f.open(openMode,permissions.toShort());

        return new FSDataOutputStream(new OutputStream() {

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                raf.write(b,off,len);
            }

            @Override
            public void write(byte[] b) throws IOException {
                raf.write(b,0,b.length);
            }

            @Override
            public void write(int b) throws IOException {
                raf.write(new byte[]{(byte)b},0,1);
            }

            @Override
            public void close() throws IOException {
                raf.close();
            }
        });
    }

    @Override
    public FSDataOutputStream append(Path arg0, int arg1, Progressable arg2) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean rename(Path src, Path dest) throws IOException {
        final String srcPath = src.toUri().getPath();
        final String destPath = dest.toUri().getPath();

        File srcFile = volume.getFile(srcPath);
        File destFile = volume.getFile(destPath);
        srcFile.renameTo(destFile);
        return true;
    }

    @Override
    public boolean delete(Path file) throws IOException {
        return delete(file,false);
    }

    @Override
    public boolean delete(Path file, boolean recursive) throws IOException {
        try {
            final String path = file.toUri().getPath();
            return delete(path);
        } catch (FileNotFoundException f) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "'%s' could not be " +
            		"deleted, because it is not available.", file.toString());
            return false;
        }
    }

    protected boolean delete(String path) throws IOException {
        Logging.logMessage(Logging.LEVEL_DEBUG, this,"getattr: "+path);
        File f = volume.getFile(path);
        if (f.isDirectory()) {

            //delete all entries
            String[] entries = volume.list(path);
            for (String e : entries) {
                if (delete(path+"/"+e) == false)
                    return false;
            }
            f.delete();

            return true;
        } else {
            //file
            f.delete();
            return true;
        }
    }

    @Override
    public FileStatus[] listStatus(Path hdPath) throws IOException {
        if (hdPath == null) return null;
        
        final String path = hdPath.toUri().getPath();
        Logging.logMessage(Logging.LEVEL_DEBUG, this,"ls: "+path);
        DirectoryEntry[] list = volume.listEntries(path);
        if (list == null) return null;
        
        FileStatus[] fslist = new FileStatus[list.length];
        for (int i = 0; i < list.length; i++) {
            final DirectoryEntry e = list[i];
            final Stat s = e.getStbuf().get(0);
            final boolean isDir = (s.getMode() & Constants.SYSTEM_V_FCNTL_H_S_IFDIR) > 0;
            fslist[i] = new FileStatus(s.getSize(), isDir , 1, 1,(long) (s.getMtime_ns() / 1e6),
                    (long) (s.getAtime_ns() / 1e6), new FsPermission((short)s.getMode()),
                    s.getUser_id(), s.getGroup_id(), new Path(hdPath,e.getName()));
        }
        return fslist;
    }

    @Override
    public void setWorkingDirectory(Path arg0) {
        this.workingDirectory = arg0;
    }

    @Override
    public Path getWorkingDirectory() {
        return this.workingDirectory;
    }

    @Override
    public boolean mkdirs(Path path, FsPermission perm) throws IOException {
        final String pathStr = path.toUri().getPath();
        final String[] dirs = pathStr.split("/");
        String tmpPath = "";
        for (String dir : dirs) {
            tmpPath += dir+"/";
            File d = volume.getFile(tmpPath);
            if (d.exists()) {
                if (!d.isDirectory())
                    return false;
            } else {
                d.mkdir((int)perm.toShort());
            }
        }
        return true;
    }

    @Override
    public FileStatus getFileStatus(Path file) throws IOException {
        final String path = file.toUri().getPath();
        Logging.logMessage(Logging.LEVEL_DEBUG, this,"getattr: "+path);
        File f = volume.getFile(path);
        Stat s = f.stat();
        final boolean isDir = (s.getMode() & Constants.SYSTEM_V_FCNTL_H_S_IFDIR) > 0;
        return new FileStatus(s.getSize(), isDir , 1, 1,(long) (s.getMtime_ns() / 1e6),
                    (long) (s.getAtime_ns() / 1e6), new FsPermission((short)s.getMode()),
                    s.getUser_id(), s.getGroup_id(), file);
    }

    public void close() {
        xtreemfsClient.stop();
    }
}
