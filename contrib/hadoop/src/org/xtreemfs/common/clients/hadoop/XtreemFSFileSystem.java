/*
 * Copyright (c) 2009-2012 by Paul Seiferth,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.clients.hadoop;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FSInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.XtreemFSException;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntry;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;

/**
 * 
 * @author PaulSeiferth
 */
public class XtreemFSFileSystem extends FileSystem {

    private URI             fileSystemURI;
    private Client          xtreemfsClient;
    private Volume          xtreemfsVolume;
    private Path            workingDirectory;
    private UserCredentials userCredentials;
    private long            stripeSize;

    @Override
    public void initialize(URI uri, Configuration conf) throws IOException {
        super.initialize(uri, conf);

        TimeSync.initializeLocal(1000, 100);
        int logLevel = Logging.LEVEL_WARN;
        if (conf.getBoolean("xtreemfs.client.debug", true)) {
            logLevel = Logging.LEVEL_DEBUG;
        }

        Logging.start(logLevel, Logging.Category.all);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "init : " + uri);
        }

        String volumeName = conf.get("xtreemfs.volumeName");
        if (volumeName == null) {
            throw new IOException("You have to specify a volume name in"
                    + " core-site.xml! (xtreemfs.volumeName)");
        }

        // create UserCredentials
        if ((conf.get("xtreemfs.client.userid") != null) && (conf.get("xtreemfs.client.groupid") != null)) {
            userCredentials =
                    UserCredentials.newBuilder().setUsername(conf.get("xtreemfs.client.userid"))
                            .addGroups(conf.get("xtreemfs.client.groupid")).build();
        }
        if (userCredentials == null) {
            if (System.getProperty("user.name") != null) {
                userCredentials =
                        UserCredentials.newBuilder().setUsername(System.getProperty("user.name"))
                                .addGroups("users").build();
            } else {
                userCredentials =
                        UserCredentials.newBuilder().setUsername("xtreemfs").addGroups("xtreemfs").build();
            }
        }

        // initialize XtreemFS Client with default Options and without SSL.
        Options xtreemfsOptions = new Options();
        xtreemfsOptions.setMetadataCacheSize(0);
        xtreemfsClient =
                Client.createClient(uri.getHost() + ":" + uri.getPort(), userCredentials,
                        xtreemfsOptions.generateSSLOptions(), xtreemfsOptions);
        try {
            // TODO: Fix stupid Exception in libxtreemfs
            xtreemfsClient.start();
        } catch (Exception ex) {
            Logger.getLogger(XtreemFSFileSystem.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            xtreemfsVolume = xtreemfsClient.openVolume(volumeName, null, xtreemfsOptions);
            xtreemfsVolume.start();
        } catch (VolumeNotFoundException ve) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.misc, this,
                    "Unable to open volume %s. Make sure this volume exists!", volumeName);
            throw new IOException("Unable to open volume " + volumeName);
        } catch (AddressToUUIDNotFoundException aue) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.misc, this,
                    "Unable to resolve UUID for volumeName %s", volumeName);
            throw new IOException(aue);
        } catch (Exception e) {
            e.printStackTrace();
        }

        stripeSize = xtreemfsVolume.statFS(userCredentials).getDefaultStripingPolicy().getStripeSize();
        stripeSize *= 1024;
        fileSystemURI = uri;
        workingDirectory = getHomeDirectory();

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "file system init complete: " + uri.getUserInfo());
        }
    }

    @Override
    public URI getUri() {
        return this.fileSystemURI;
    }

    @Override
    public FSDataInputStream open(Path path, int bufferSize) throws IOException {
        final String pathString = makeAbsolute(path).toUri().getPath();
        final FileHandle fileHandle =
                xtreemfsVolume.openFile(userCredentials, pathString,
                        SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber(), 0);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Opening file %s", pathString);
        }
        statistics.incrementReadOps(1);

        return new FSDataInputStream(new FSInputStream() {
            private long position = 0;

            @Override
            public synchronized void seek(long l) throws IOException {
                this.position = l;
            }

            @Override
            public synchronized long getPos() throws IOException {
                return position;
            }

            @Override
            public synchronized boolean seekToNewSource(long l) throws IOException {
                return false;
            }

            @Override
            public synchronized int read() throws IOException {
                byte[] buf = new byte[1];
                int numRead = fileHandle.read(userCredentials, buf, 1, (int) position);
                if (numRead == 0) {
                    return -1;
                }
                seek(getPos() + 1);
                statistics.incrementBytesRead(1);
                return (int) (buf[0] & 0xFF);
            }

            @Override
            public synchronized int read(byte[] bytes, int offset, int length) throws IOException {
                int bytesRead = fileHandle.read(userCredentials, bytes, length, (int) getPos());
                if ((bytesRead == 0) && (length > 0)) {
                    return -1;
                }
                seek(getPos() + bytesRead);
                statistics.incrementBytesRead(bytesRead);
                return bytesRead;
            }

            @Override
            public synchronized int read(long position, byte[] bytes, int offset, int length)
                    throws IOException {
                int bytesRead = fileHandle.read(userCredentials, bytes, length, (int) position);
                if ((bytesRead == 0) && (length > 0)) {
                    return -1;
                }
                statistics.incrementBytesRead(bytesRead);
                return bytesRead;
            }

            @Override
            public synchronized int read(byte[] bytes) throws IOException {
                return read(position, bytes, 0, bytes.length);
            }

            @Override
            public synchronized void close() throws IOException {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "Closing file %s", pathString);
                }
                super.close();
                fileHandle.close();
            }
        });
    }

    @Override
    public FSDataOutputStream create(Path path, FsPermission fp, boolean overwrite, int bufferSize,
            short replication, long blockSize, Progressable p) throws IOException {
        // block replication for the file
        final String pathString = makeAbsolute(path).toUri().getPath();
        int flags =
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber()
                        | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber();
        if (overwrite) {
            flags |= SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber();
        }

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Creating file %s. Overwrite = %s", pathString,
                    overwrite);
        }

        // If some of the parent directories don't exist they should be created.
        final String[] dirs = pathString.split("/");
        String tempPath = "";
        for (int i = 0; i < dirs.length - 1; i++) {
            if (dirs[i].isEmpty() == false) {
                tempPath = tempPath + "/" + dirs[i];
                if (isXtreemFSDirectory(tempPath) == false) {
                    xtreemfsVolume.createDirectory(userCredentials, tempPath, fp.toShort());
                }
            }
        }

        final FileHandle fileHandle =
                xtreemfsVolume.openFile(userCredentials, pathString, flags, fp.toShort());
        statistics.incrementWriteOps(1);

        return new FSDataOutputStream(new OutputStream() {
            private int position = 0;

            @Override
            public synchronized void write(int b) throws IOException {
                byte[] data = new byte[4];
                data[0] = (byte) (b >>> 24);
                data[1] = (byte) (b >>> 16);
                data[2] = (byte) (b >>> 8);
                data[3] = (byte) b;
                int writtenBytes = fileHandle.write(userCredentials, data, 4, position);
                if (statistics != null) {
                    statistics.incrementBytesWritten(writtenBytes);
                }
                position += writtenBytes;
            }

            @Override
            public synchronized void write(byte b[], int off, int len) throws IOException {
                if (b == null) {
                    throw new NullPointerException();
                } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length)
                        || ((off + len) < 0)) {
                    throw new IndexOutOfBoundsException();
                } else if (len == 0) {
                    return;
                }
                int writtenBytes = fileHandle.write(userCredentials, b, off, len, position);
                if (statistics != null) {
                    statistics.incrementBytesWritten(writtenBytes);
                }
                position += writtenBytes;
            }

            @Override
            public synchronized void close() throws IOException {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "Closing file %s", pathString);
                }
                super.close();
                fileHandle.close();
            }
        }, statistics);
    }

    @Override
    public FSDataOutputStream append(Path path, int i, Progressable p) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean rename(Path src, Path dest) throws IOException {
        final String srcPath = makeAbsolute(src).toUri().getPath();
        final String destPath = makeAbsolute(dest).toUri().getPath();
        xtreemfsVolume.rename(userCredentials, srcPath, destPath);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Renamed file/dir. src: %s, dst: %s", srcPath,
                    destPath);
        }
        statistics.incrementWriteOps(1);
        return true;
    }

    @Override
    public boolean delete(Path path) throws IOException {
        return delete(path, false);
    }

    @Override
    public boolean delete(Path path, boolean recursive) throws IOException {
        statistics.incrementWriteOps(1);
        final String pathString = makeAbsolute(path).toUri().getPath();
        if (isXtreemFSFile(pathString)) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Deleting file %s", pathString);
            }
            return deleteXtreemFSFile(pathString);
        }
        if (isXtreemFSDirectory(pathString)) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Deleting directory %s", pathString);
            }
            return deleteXtreemFSDirectory(pathString, recursive);
        }
        // path is neither a file nor a directory. Consider it as not existing.
        return false;
    }

    private boolean deleteXtreemFSDirectory(String path, boolean recursive) throws IOException {
        DirectoryEntries dirEntries = xtreemfsVolume.readDir(userCredentials, path, 0, 0, true);
        boolean isEmpty = (dirEntries.getEntriesCount() <= 2);

        if (recursive) {
            return deleteXtreemFSDirRecursive(path);
        } else {
            if (isEmpty) {
                xtreemfsVolume.removeDirectory(userCredentials, path);
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean deleteXtreemFSDirRecursive(String path) throws IOException {
        boolean success = true;
        try {
            DirectoryEntries dirEntries = xtreemfsVolume.readDir(userCredentials, path, 0, 0, false);
            for (DirectoryEntry dirEntry : dirEntries.getEntriesList()) {
                if (dirEntry.getName().equals(".") || dirEntry.getName().equals("..")) {
                    continue;
                }
                if (isXtreemFSFile(dirEntry.getStbuf())) {
                    xtreemfsVolume.unlink(userCredentials, path + "/" + dirEntry.getName());
                }
                if (isXtreemFSDirectory(dirEntry.getStbuf())) {
                    success = deleteXtreemFSDirRecursive(path + "/" + dirEntry.getName());
                }
            }
            xtreemfsVolume.removeDirectory(userCredentials, path);
        } catch (XtreemFSException xe) {
            success = false;
        }
        return success;
    }

    private boolean deleteXtreemFSFile(String path) throws IOException {
        try {
            xtreemfsVolume.unlink(userCredentials, path);
            return true;
        } catch (XtreemFSException xe) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.misc, this,
                    "failed to delete file %s, reason: %s", path, xe.getMessage());
            return false;
        }
    }

    private boolean isXtreemFSFile(String path) throws IOException {
        Stat stat = null;
        try {
            stat = xtreemfsVolume.getAttr(userCredentials, path);
        } catch (PosixErrorException pee) {
            if (pee.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOENT)) {
                return false;
            } else {
                throw pee;
            }
        }
        if (stat != null) {
            return isXtreemFSFile(stat);
        } else {
            return false;
        }
    }

    private boolean isXtreemFSFile(Stat stat) {
        return (stat.getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFREG.getNumber()) > 0;
    }

    private boolean isXtreemFSDirectory(String path) throws IOException {
        Stat stat = null;
        try {
            stat = xtreemfsVolume.getAttr(userCredentials, path);
        } catch (PosixErrorException pee) {
            if (pee.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOENT)) {
                return false;
            } else {
                throw pee;
            }
        }
        if (stat != null) {
            return isXtreemFSDirectory(stat);
        } else {
            return false;
        }
    }

    private boolean isXtreemFSDirectory(Stat stat) {
        return (stat.getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFDIR.getNumber()) > 0;
    }

    @Override
    public FileStatus[] listStatus(Path path) throws IOException {
        if (path == null) {
            return null;
        }

        final String pathString = makeAbsolute(path).toUri().getPath();
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "ls: " + pathString);
        }

        if (isXtreemFSDirectory(pathString) == false) {
            return null;
        }

        DirectoryEntries dirEntries = xtreemfsVolume.readDir(userCredentials, pathString, 0, 0, false);
        statistics.incrementLargeReadOps(1);
        ArrayList<FileStatus> fileStatus = new ArrayList<FileStatus>(dirEntries.getEntriesCount() - 2);
        for (DirectoryEntry entry : dirEntries.getEntriesList()) {
            if (entry.getName().equals("..") || entry.getName().equals(".")) {
                continue;
            }
            final Stat stat = entry.getStbuf();
            final boolean isDir = isXtreemFSDirectory(stat);
            if (isDir) {
                // for directories, set blocksize to 0
                fileStatus.add(new FileStatus(0, isDir, 1, 0, (long) (stat.getMtimeNs() / 1e6), (long) (stat
                        .getAtimeNs() / 1e6), new FsPermission((short) stat.getMode()), stat.getUserId(),
                        stat.getGroupId(), new Path(makeAbsolute(path), entry.getName())));
            } else {
                // for files, set blocksize to stripeSize of the volume
                fileStatus.add(new FileStatus(stat.getSize(), isDir, 1, stripeSize,
                        (long) (stat.getMtimeNs() / 1e6), (long) (stat.getAtimeNs() / 1e6), new FsPermission(
                                (short) stat.getMode()), stat.getUserId(), stat.getGroupId(), new Path(
                                makeAbsolute(path), entry.getName())));
            }
        }
        return fileStatus.toArray(new FileStatus[fileStatus.size()]);
    }

    @Override
    public void setWorkingDirectory(Path path) {
        this.workingDirectory = makeAbsolute(path);
    }

    @Override
    public Path getWorkingDirectory() {
        return this.workingDirectory;
    }

    private Path makeAbsolute(Path p) {
        if (p.isAbsolute()) {
            return p;
        } else {
            return new Path(workingDirectory, p);
        }
    }

    @Override
    public boolean mkdirs(Path path, FsPermission fp) throws IOException {
        final String pathString = makeAbsolute(path).toUri().getPath();
        final String[] dirs = pathString.split("/");
        statistics.incrementWriteOps(1);

        final short mode = fp.toShort();
        String dirString = "";
        for (String dir : dirs) {
            dirString += dir + "/";
            if (isXtreemFSFile(dirString)) {
                return false;
            }
            if (isXtreemFSDirectory(dirString) == false) { // stringPath does not exist, create it
                xtreemfsVolume.createDirectory(userCredentials, dirString, mode);
            }
        }
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Created direcotry %s", pathString);
        }
        return true;
    }

    @Override
    public FileStatus getFileStatus(Path path) throws IOException {
        final String pathString = makeAbsolute(path).toUri().getPath();
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "getting file status for file %s", pathString);
        }
        Stat stat = null;
        try {
            stat = xtreemfsVolume.getAttr(userCredentials, pathString);
        } catch (PosixErrorException pee) {
            if (pee.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOENT)) {
                throw new FileNotFoundException();
            }
            throw pee;
        }
        final boolean isDir = isXtreemFSDirectory(stat);
        if (isDir) {
            // for directories, set blocksize to 0
            return new FileStatus(0, isDir, 1, 0, (long) (stat.getMtimeNs() / 1e6),
                    (long) (stat.getAtimeNs() / 1e6), new FsPermission((short) stat.getMode()),
                    stat.getUserId(), stat.getGroupId(), makeAbsolute(path));
        } else {
            // for files, set blocksize to stripesize of the volume
            return new FileStatus(stat.getSize(), isDir, 1, stripeSize, (long) (stat.getMtimeNs() / 1e6),
                    (long) (stat.getAtimeNs() / 1e6), new FsPermission((short) stat.getMode()),
                    stat.getUserId(), stat.getGroupId(), makeAbsolute(path));
        }
    }

    // private long calculateNumberOfblock(Stat statOfFile) {
    // if ((statOfFile.getSize() % statOfFile.getBlksize()) == 0) {
    // return statOfFile.getSize() / statOfFile.getBlksize();
    // } else {
    // return statOfFile.getSize() / statOfFile.getBlksize() + 1;
    // }
    // }

    @Override
    public void close() throws IOException {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Closing %s", XtreemFSFileSystem.class.getName());
        }
        super.close();
        xtreemfsVolume.close();
        xtreemfsClient.shutdown();
    }
}
