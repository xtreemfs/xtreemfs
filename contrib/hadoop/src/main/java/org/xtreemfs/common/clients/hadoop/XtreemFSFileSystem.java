/*
 * Copyright (c) 2009-2012 by Paul Seiferth,
 *               2015 by Robert Schmidtke,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.clients.hadoop;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;
import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.common.libxtreemfs.Volume.StripeLocation;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.XtreemFSException;
import org.xtreemfs.common.libxtreemfs.jni.NativeHelper;
import org.xtreemfs.foundation.SSLOptions;
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

    private URI                 fileSystemURI;
    private Client              xtreemfsClient;
    private Map<String, Volume> xtreemfsVolumes;
    Set<String>                 defaultVolumeDirectories;
    private Path                workingDirectory;
    private UserCredentials     userCredentials;
    private boolean             useReadBuffer;
    private boolean             useWriteBuffer;
    private int                 readBufferSize;
    private int                 writeBufferSize;
    private Volume              defaultVolume;
    private static final int    STANDARD_DIR_PORT = 32638;

    @Override
    public void initialize(URI uri, Configuration conf) throws IOException {
        super.initialize(uri, conf);
        setConf(conf);
        
        int logLevel = Logging.LEVEL_WARN;
        if (conf.getBoolean("xtreemfs.client.debug", false)) {
            logLevel = Logging.LEVEL_DEBUG;
        }

        Logging.start(logLevel, Logging.Category.all);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "init : " + uri);
        }

        String defaultVolumeName = conf.get("xtreemfs.defaultVolumeName");
        if(Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Default Volume: '%s'",
                    defaultVolumeName);
        }

        int uriPort = uri.getPort();
        if (uriPort  == -1) {
            uriPort = STANDARD_DIR_PORT;
            Logging.logMessage(Logging.LEVEL_INFO, this, "No DIR port was specified "
                    + "using standard port " + STANDARD_DIR_PORT);
        }

        useReadBuffer = conf.getBoolean("xtreemfs.io.buffer.read", false);
        readBufferSize = conf.getInt("xtreemfs.io.buffer.size.read", 0);
        if (useReadBuffer && readBufferSize == 0) {
            useReadBuffer = false;
        }

        useWriteBuffer = conf.getBoolean("xtreemfs.io.buffer.write", false);
        writeBufferSize = conf.getInt("xtreemfs.io.buffer.size.write", 0);
        if (useWriteBuffer && writeBufferSize == 0) {
            useWriteBuffer = false;
        }

        // Create UserCredentials.
        if ((conf.get("xtreemfs.client.userid") != null) && (conf.get("xtreemfs.client.groupid") != null)) {
            userCredentials = UserCredentials.newBuilder().setUsername(conf.get("xtreemfs.client.userid"))
                    .addGroups(conf.get("xtreemfs.client.groupid")).build();
        }
        if (userCredentials == null) {
            if (System.getProperty("user.name") != null) {
                userCredentials = UserCredentials.newBuilder().setUsername(System.getProperty("user.name"))
                        .addGroups("users").build();
            } else {
                userCredentials = UserCredentials.newBuilder().setUsername("xtreemfs").addGroups("xtreemfs").build();
            }
        }

        // Create SSLOptions.
        SSLOptions sslOptions = null;

        if (conf.getBoolean("xtreemfs.ssl.enabled", false)) {

            // Get credentials from config.
            String credentialFilePath = conf.get("xtreemfs.ssl.credentialFile");
            if (credentialFilePath == null) {
                throw new IOException("You have to specify a server credential file in"
                        + " core-site.xml! (xtreemfs.ssl.serverCredentialFile)");
            }
            String credentialFilePassphrase = conf.get("xtreemfs.ssl.credentialFile.passphrase");

            // Get trusted certificates form config.
            String trustedCertificatesFilePath = conf.get("xtreemfs.ssl.trustedCertificatesFile");
            String trustedCertificatesFilePassphrase = conf.get("xtreemfs.ssl.trustedCertificatesFile.passphrase");
            String trustedCertificatesFileContainer = null;
            String sslProtocolString = conf.get("xtreemfs.ssl.protocol");
            if (trustedCertificatesFilePath == null) {
                trustedCertificatesFileContainer = "none";
            } else {
                trustedCertificatesFileContainer = SSLOptions.JKS_CONTAINER;
            }

            sslOptions = new SSLOptions(credentialFilePath, credentialFilePassphrase,
                    SSLOptions.PKCS12_CONTAINER, trustedCertificatesFilePath, trustedCertificatesFilePassphrase,
                    trustedCertificatesFileContainer, conf.getBoolean("xtreemfs.ssl.authenticationWithoutEncryption",
                            false), false, sslProtocolString, null);
        }
        
        // Initialize default options
        Options xtreemfsOptions = new Options();
        xtreemfsOptions.setMetadataCacheSize(0);
        
        // Use the native client and use async writes if requested.
        ClientFactory.ClientType clientType = ClientFactory.ClientType.JAVA;
        if (conf.getBoolean("xtreemfs.jni.enabled", false)) {
            clientType = ClientFactory.ClientType.NATIVE;
            
            String libraryPath = conf.get("xtreemfs.jni.libraryPath");
            if (libraryPath != null && !libraryPath.isEmpty()) {
                NativeHelper.setXtreemfsLibPath(libraryPath);
            }
            
            if (conf.getBoolean("xtreemfs.asyncWrites.enabled", false)) {
                xtreemfsOptions.setEnableAsyncWrites(true);
            }
            
            int maxWriteAhead = conf.getInt("xtreemfs.asyncWrites.maxRequests", -1);
            if (maxWriteAhead > -1) {
                xtreemfsOptions.setMaxWriteAhead(maxWriteAhead);
            }
        }
        
        // Initialize XtreemFS Client
        xtreemfsClient = ClientFactory.createClient(clientType, uri.getHost() + ":" + uriPort, userCredentials,
                sslOptions, xtreemfsOptions);
        try {
            // TODO: Fix stupid Exception in libxtreemfs
            xtreemfsClient.start(true);
        } catch (Exception ex) {
            Logger.getLogger(XtreemFSFileSystem.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Get all available volumes.
        String[] volumeNames = xtreemfsClient.listVolumeNames();

        xtreemfsVolumes = new HashMap<String, Volume>(volumeNames.length);
        for (String volumeName : volumeNames) {
            try {
                xtreemfsVolumes.put(volumeName, xtreemfsClient.openVolume(volumeName, sslOptions, xtreemfsOptions));
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
        }

        // Get directories in root of defaultVolume.
        defaultVolumeDirectories = new HashSet<String>();
        defaultVolume = xtreemfsVolumes.get(defaultVolumeName);
        for (DirectoryEntry dirEntry : defaultVolume.readDir(userCredentials, "/", 0, 0, true).getEntriesList()) {
            if (dirEntry.getName().equals("..") || dirEntry.getName().equals(".")) {
                continue;
            }
            
            if (isXtreemFSDirectory("/" + dirEntry.getName(), defaultVolume)) {
                defaultVolumeDirectories.add(dirEntry.getName());
            }
        }

        /* getVolumeFromPath relies on workingDirectory to be set (via makeAbsolute)
         * and getHomeDirectory relies on this.fileSystemURI to be set
         * uris that initialize a Filesystem can be assumed to be absolute
         */
        Path uriPath = new Path(uri);
        // if no path is given prepend /
        if (! uriPath.isAbsolute()) {
            uriPath = new Path(uriPath, "/");
        }

        this.fileSystemURI = URI.create(uri.getScheme() + "://"
                + uri.getHost() + ":"
                + uriPort + "/"
                + getVolumeFromAbsolutePath(uriPath).getVolumeName());
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
    public String getScheme() {
        return "xtreemfs";
    }

    @Override
    public FSDataInputStream open(Path path, int bufferSize) throws IOException {
        Volume xtreemfsVolume = getVolumeFromPath(path);
        final String pathString = preparePath(path, xtreemfsVolume);
        final FileHandle fileHandle = xtreemfsVolume.openFile(userCredentials, pathString,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDONLY.getNumber(), 0);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Opening file %s", pathString);
        }
        statistics.incrementReadOps(1);
        return new FSDataInputStream(new XtreemFSInputStream(userCredentials, fileHandle, pathString, useReadBuffer,
                readBufferSize, statistics));
    }

    @Override
    public FSDataOutputStream create(Path path, FsPermission fp, boolean overwrite, int bufferSize, short replication,
            long blockSize, Progressable p) throws IOException {
        // block replication for the file
        Volume xtreemfsVolume = getVolumeFromPath(path);
        final String pathString = preparePath(path, xtreemfsVolume);
        
        if (!overwrite && isXtreemFSFile(pathString, xtreemfsVolume)) {
            throw new IOException("Cannot overwrite existing file '" + pathString + "'");
        }
        
        int flags = SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber()
                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber();
        if (overwrite) {
            flags |= SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber();
        }

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Creating file %s. Overwrite = %s", pathString, overwrite);
        }
        // If some of the parent directories don't exist they should be created (with default permissions for directory).
        if (pathString.lastIndexOf("/") != 0) {
            mkdirs(path.getParent());
        }

        final FileHandle fileHandle = xtreemfsVolume.openFile(userCredentials, pathString, flags, applyUMask(fp).toShort());
        return new FSDataOutputStream(new XtreemFSFileOutputStream(userCredentials, fileHandle, pathString,
                useWriteBuffer, writeBufferSize), statistics);
    }

    @Override
    public FSDataOutputStream append(Path path, int bufferSize, Progressable p) throws IOException {
    	Volume xtreemfsVolume = getVolumeFromPath(path);
        final String pathString = preparePath(path, xtreemfsVolume);
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Append new content to file %s.", pathString);
        }

        // Open file.
        final FileHandle fileHandle = xtreemfsVolume.openFile(userCredentials, pathString,
                SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());
        
        return new FSDataOutputStream(new XtreemFSFileOutputStream(userCredentials, fileHandle, pathString,
                useWriteBuffer, writeBufferSize, true), statistics);
    }

    @Override
    public boolean rename(Path src, Path dest) throws IOException {
        Volume xtreemfsVolume = getVolumeFromPath(src);
        final String srcPath = preparePath(src, xtreemfsVolume);
        String destPath = preparePath(dest, xtreemfsVolume);
        
        // add possibility to override POSIX behavior
        boolean overwrite = getConf().getBoolean("xtreemfs.rename.overwrite", false);
        if (isXtreemFSFile(srcPath, xtreemfsVolume)
                && isXtreemFSFile(destPath, xtreemfsVolume)
                && !overwrite) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "Not renaming '%s' to existing file '%s'."
                    + " Set 'xtreemfs.rename.overwrite' to true to change this behavior.",
                    srcPath, destPath);
            return false;
        }
        
        // add mv semantics
        if (isXtreemFSDirectory(destPath, xtreemfsVolume)) {
            destPath = preparePath(new Path(dest, src.getName()), xtreemfsVolume);
        }

        try {
            xtreemfsVolume.rename(userCredentials, srcPath, destPath);
        } catch(PosixErrorException e) {
            Logging.logMessage(Logging.LEVEL_WARN, this,
                    "Rename file/directory '%s' to '%s' failed with '%s'",
                    srcPath, destPath, e.getMessage());
            return false;
        }
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Renamed file/dir. src: %s, dst: %s", srcPath, destPath);
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
        Volume xtreemfsVolume = getVolumeFromPath(path);
        final String pathString = preparePath(path, xtreemfsVolume);
        if (isXtreemFSFile(pathString, xtreemfsVolume)) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Deleting file %s", pathString);
            }
            return deleteXtreemFSFile(pathString, xtreemfsVolume);
        }
        if (isXtreemFSDirectory(pathString, xtreemfsVolume)) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Deleting directory %s", pathString);
            }
            return deleteXtreemFSDirectory(pathString, xtreemfsVolume, recursive);
        }
        // path is neither a file nor a directory. Consider it as not existing.
        return false;
    }

    private boolean deleteXtreemFSDirectory(String path, Volume xtreemfsVolume, boolean recursive) throws IOException {
        DirectoryEntries dirEntries = xtreemfsVolume.readDir(userCredentials, path, 0, 0, true);
        boolean isEmpty = (dirEntries.getEntriesCount() <= 2);

        if (recursive) {
            return deleteXtreemFSDirRecursive(path, xtreemfsVolume);
        } else {
            if (isEmpty) {
                xtreemfsVolume.removeDirectory(userCredentials, path);
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean deleteXtreemFSDirRecursive(String path, Volume xtreemfsVolume) throws IOException {
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
                    success = deleteXtreemFSDirRecursive(path + "/" + dirEntry.getName(), xtreemfsVolume);
                }
            }
            xtreemfsVolume.removeDirectory(userCredentials, path);
        } catch (XtreemFSException xe) {
            success = false;
        }
        return success;
    }

    private boolean deleteXtreemFSFile(String path, Volume xtreemfsVolume) throws IOException {
        try {
            xtreemfsVolume.unlink(userCredentials, path);
            return true;
        } catch (XtreemFSException xe) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.misc, this,
                    "failed to delete file %s, reason: %s", path, xe.getMessage());
            return false;
        }
    }

    private boolean isXtreemFSFile(String path, Volume xtreemfsVolume) throws IOException {
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

    private boolean isXtreemFSDirectory(String path, Volume xtreemfsVolume) throws IOException {
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
        Volume xtreemfsVolume = getVolumeFromPath(path);
        final String pathString = preparePath(path, xtreemfsVolume);

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "ls: " + pathString);
        }

        if (isXtreemFSDirectory(pathString, xtreemfsVolume) == false) {
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
                        .getAtimeNs() / 1e6), new FsPermission((short) stat.getMode()), stat.getUserId(), stat
                        .getGroupId(), new Path(makeAbsolute(path), entry.getName())));
            } else {
                // for files, set blocksize to stripeSize of the volume
                fileStatus.add(new FileStatus(stat.getSize(), isDir, 1, xtreemfsVolume.statFS(userCredentials)
                        .getDefaultStripingPolicy().getStripeSize() * 1024, (long) (stat.getMtimeNs() / 1e6),
                        (long) (stat.getAtimeNs() / 1e6), new FsPermission((short) stat.getMode()), stat.getUserId(),
                        stat.getGroupId(), new Path(makeAbsolute(path), entry.getName())));
            }
        }
        return fileStatus.toArray(new FileStatus[fileStatus.size()]);
    }

    @Override
    public void setWorkingDirectory(Path path) {
        Volume xtreemfsVolume = getVolumeFromPath(path);
        this.workingDirectory = new Path(preparePath(path, xtreemfsVolume))
                .makeQualified(this.fileSystemURI, this.workingDirectory);
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
        Volume xtreemfsVolume = getVolumeFromPath(path);
        final String pathString = preparePath(path, xtreemfsVolume);
        final String[] dirs = pathString.split("/");
        statistics.incrementWriteOps(1);

        final short mode = applyUMask(fp).toShort();
        String dirString = "";

        if (xtreemfsVolume == defaultVolume) {
            defaultVolumeDirectories.add(dirs[0]);
        }

        for (String dir : dirs) {
            dirString += dir + "/";
            if (isXtreemFSFile(dirString, xtreemfsVolume)) {
                throw new IOException("Cannot make subdirectory of existing file " + dirString);
            }
            if (isXtreemFSDirectory(dirString, xtreemfsVolume) == false) { // stringPath does not exist, create it
                xtreemfsVolume.createDirectory(userCredentials, dirString, mode);
            }
        }
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Created directory %s", pathString);
        }
        return true;
    }

    @Override
    public FileStatus getFileStatus(Path path) throws IOException {
        Volume xtreemfsVolume = getVolumeFromPath(path);
        final String pathString = preparePath(path, xtreemfsVolume);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "getting file status for file %s", pathString);
        }
        Stat stat = null;
        try {
            stat = xtreemfsVolume.getAttr(userCredentials, pathString);
        } catch (PosixErrorException pee) {
            if (pee.getPosixError().equals(POSIXErrno.POSIX_ERROR_ENOENT)) {
                throw new FileNotFoundException(pathString);
            }
            throw pee;
        }
        final boolean isDir = isXtreemFSDirectory(stat);
        if (isDir) {
            // for directories, set blocksize to 0
            return new FileStatus(0, isDir, 1, 0, (long) (stat.getMtimeNs() / 1e6), (long) (stat.getAtimeNs() / 1e6),
                    new FsPermission((short) stat.getMode()), stat.getUserId(), stat.getGroupId(), makeQualified(path));
        } else {
            // for files, set blocksize to stripesize of the volume
            return new FileStatus(stat.getSize(), isDir, 1, xtreemfsVolume.statFS(userCredentials)
                    .getDefaultStripingPolicy().getStripeSize() * 1024, (long) (stat.getMtimeNs() / 1e6),
                    (long) (stat.getAtimeNs() / 1e6), new FsPermission((short) stat.getMode()), stat.getUserId(),
                    stat.getGroupId(), makeQualified(path));
        }
    }

    @Override
    public void close() throws IOException {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Closing %s", XtreemFSFileSystem.class.getName());
        }
        super.close();
        for (Volume xtreemfsVolume : xtreemfsVolumes.values()) {
            xtreemfsVolume.close();
        }
        xtreemfsClient.shutdown();
    }
    
    @Override
    public BlockLocation[] getFileBlockLocations(FileStatus file, long start, long length) throws IOException {
        if (file == null) {
            return null;
        }
        Volume xtreemfsVolume = getVolumeFromPath(file.getPath());
        String pathString = preparePath(file.getPath(), xtreemfsVolume);
        List<StripeLocation> stripeLocations = xtreemfsVolume.getStripeLocations(userCredentials, pathString, start,
                length);

        BlockLocation[] result = new BlockLocation[stripeLocations.size()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = new BlockLocation(stripeLocations.get(i).getUuids(), stripeLocations.get(i).getHostnames(),
                    stripeLocations.get(i).getStartSize(), stripeLocations.get(i).getLength());
        }
        return result;
    }
    
    /**
     * Check the configuration for a umask and apply if set.
     * 
     * @param fp
     * @return
     */
    private FsPermission applyUMask(FsPermission fp) {
        String umaskString = getConf().get(CommonConfigurationKeys.FS_PERMISSIONS_UMASK_KEY);
        if (umaskString == null) {
            return fp;
        } else {
            return fp.applyUMask(new FsPermission(umaskString));
        }
    }

    /**
     * Make path absolute and remove volume if path starts with a volume
     * 
     * @param path
     * @param volume
     * @return
     */
    private String preparePath(Path path, Volume volume) {
        String pathString = makeAbsolute(path).toUri().getPath();
        if (volume == defaultVolume) {
            String[] splitPath = pathString.split("/");
            if (splitPath.length > 1 && splitPath[1].equals(defaultVolume.getVolumeName())) {
                // Path starts with default volume name, strip off volume name if
                // there is no similarly named directory in the default volume.
                // (i.e. the default volume has been specified explicitly)
                if (!defaultVolumeDirectories.contains(splitPath[1])) {
                    pathString = pathString.substring(pathString.indexOf("/", 1));
                }
            }
            return pathString;
        } else {
            int pathBegin = pathString.indexOf("/", 1);
            String pathStringWithoutVolume = pathString.substring(pathBegin);
            return pathStringWithoutVolume;
        }
    }

    /**
     * Returns the volume name from the path or the default volume, if the path does not contain a volume name or the
     * default volume has a directory equally named to the volume.
     * 
     * @param path
     * @return
     * @throws IOException
     */
    private Volume getVolumeFromPath(Path path) {
        return getVolumeFromAbsolutePath(makeAbsolute(path));
    }

    /**
     * Returns the volume name from the path or the default volume, if the path does not contain a volume name or the
     * default volume has a directory equally named to the volume.
     *
     * @param path
     * @return
     * @throws IOException
     */
    private Volume getVolumeFromAbsolutePath(Path path) {
        String pathString = path.toUri().getPath();
        if (isDirOrFileInDefVol(pathString)) {
            // First part of path is a directory or path is a file in the root of defaultVolume
            return defaultVolume;
        } else {
            // First part of path is a volume
            String volumeName = pathString.substring(1, pathString.indexOf("/", 1));
            Volume volume = xtreemfsVolumes.get(volumeName);

            if (volume == null) {
                // If no volume or directory exist, assume a invalid path on default volume.
                return defaultVolume;
            } else {
                return volume;
            }
        }
    }

    private boolean isDirOrFileInDefVol(String pathString) {
        String[]splitPath = pathString.split("/");
        if (splitPath.length > 1 && defaultVolumeDirectories.contains(splitPath[1])
                || pathString.lastIndexOf("/") == 0) {
            // First part of path is a directory or path is a file in the root of defaultVolume
            return true;
        } else
            return false;
    }
}
