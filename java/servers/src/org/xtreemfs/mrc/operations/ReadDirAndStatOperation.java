/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.io.File;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseResultSet;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntry;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.readdirRequest;

/**
 * 
 * @author stender
 */
public class ReadDirAndStatOperation extends MRCOperation {
    
    public ReadDirAndStatOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final readdirRequest rqArgs = (readdirRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        validateContext(rq);
        
        Path p = new Path(rqArgs.getVolumeName(), rqArgs.getPath());
        
        StorageManager sMan = vMan.getStorageManagerByName(p.getComp(0));
        PathResolver res = new PathResolver(sMan, p);
        VolumeInfo volume = sMan.getVolumeInfo();
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        // check whether file exists
        res.checkIfFileDoesNotExist();
        
        FileMetadata file = res.getFile();
        
        // check whether the directory grants read access
        faMan.checkPermission(FileAccessManager.O_RDONLY, sMan, file, res.getParentDirId(),
            rq.getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
        
        // TODO: support dirs w/ more than Integer.MAX_VALUE entries
        int seenEntries = (int) rqArgs.getSeenDirectoryEntriesCount();
        int numEntries = rqArgs.getLimitDirectoryEntriesCount() <= 0 ? Integer.MAX_VALUE : rqArgs
                .getLimitDirectoryEntriesCount();
        boolean namesOnly = rqArgs.getNamesOnly();
        
        // do not report stat info for individual files if there are no search
        // permissions on the directory
        try {
            faMan.checkPermission(FileAccessManager.NON_POSIX_SEARCH, sMan, file, res.getParentDirId(), rq
                    .getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);
        } catch (UserException exc) {
            if (exc.getErrno() == POSIXErrno.POSIX_ERROR_EACCES)
                namesOnly = true;
            else
                throw exc;
        }
        
        DirectoryEntries.Builder dirContent = DirectoryEntries.newBuilder();
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // if required, update POSIX timestamps
        int time = (int) (TimeSync.getGlobalTime() / 1000);
        if (!master.getConfig().isNoAtime())
            MRCHelper.updateFileTimes(res.getParentDirId(), file, true, false, false, sMan, time, update);
        
        // get the parent directory
        FileMetadata parentDir = res.getParentDir();
        
        if (seenEntries == 0 && numEntries > 0) {
            
            // dir is not root directory
            if (parentDir != null) {
                
                DirectoryEntry.Builder entry = DirectoryEntry.newBuilder().setName("..");
                if (!namesOnly)
                    entry.setStbuf(getStat(sMan, faMan, rq, volume, parentDir));
                
                dirContent.addEntries(entry);
                
            }

            // dir is root directory
            else {
                
                DirectoryEntry.Builder entry = DirectoryEntry.newBuilder().setName("..");
                if (!namesOnly)
                    entry.setStbuf(getStat(sMan, faMan, rq, volume, file));
                
                dirContent.addEntries(entry);
            }
            
        }
        
        // get the current directory
        Stat stat = getStat(sMan, faMan, rq, volume, file);
        long newEtag = stat.getEtag();
        long knownEtag = rqArgs.getKnownEtag();
        
        if (newEtag != knownEtag) {
            
            if ((seenEntries == 0 && numEntries >= 2) || (seenEntries == 1 && numEntries >= 1)) {
                
                DirectoryEntry.Builder entry = DirectoryEntry.newBuilder().setName(".");
                if (!namesOnly)
                    entry.setStbuf(stat);
                
                dirContent.addEntries(entry);
            }
            
            // get all children
            DatabaseResultSet<FileMetadata> it = sMan.getChildren(res.getFile().getId(), seenEntries - 2, numEntries
                - dirContent.getEntriesCount());
            while (it.hasNext()) {
                
                FileMetadata child = it.next();
                if (child.getFileName().equals("")) {
                    Logging.logMessage(Logging.LEVEL_WARN, this, "WARNING: found nested %s w/ empty name", child
                            .isDirectory() ? "directory" : "file");
                    continue;
                }
                
                DirectoryEntry.Builder entry = DirectoryEntry.newBuilder().setName(child.getFileName());
                if (!namesOnly)
                    entry.setStbuf(getStat(sMan, faMan, rq, volume, child));
                
                dirContent.addEntries(entry);
            }
            it.destroy();
            
        }
        
        // set the response
        rq.setResponse(dirContent.build());
        
        update.execute();
    }
    
    private Stat getStat(StorageManager sMan, FileAccessManager faMan, MRCRequest rq, VolumeInfo volume,
        FileMetadata file) throws DatabaseException, MRCException {
        
        // FIXME: merge w/ 'stat' operation
        
        String linkTarget = sMan.getSoftlinkTarget(file.getId());
        int mode = faMan.getPosixAccessMode(sMan, file, rq.getDetails().userId, rq.getDetails().groupIds);
        mode |= linkTarget != null ? GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFLNK.getNumber()
            : file.isDirectory() ? GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFDIR.getNumber()
                : ((file.getPerms() & GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFIFO.getNumber()) != 0) ? GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFIFO
                        .getNumber()
                    : GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFREG.getNumber();
        long size = linkTarget != null ? linkTarget.length() : file.isDirectory() ? 0 : file.getSize();
        int blkSize = 0;
        if ((linkTarget == null) && (!file.isDirectory())) {
            XLocList xlocList = file.getXLocList();
            if ((xlocList != null) && (xlocList.getReplicaCount() > 0))
                blkSize = xlocList.getReplica(0).getStripingPolicy().getStripeSize() * 1024;
        }
        
        final long newEtag = file.getMtime() + file.getCtime();
        
        Stat stat = Stat.newBuilder().setDev(volume.getId().hashCode()).setIno(file.getId()).setMode(mode)
                .setNlink(file.getLinkCount()).setUserId(file.getOwnerId()).setGroupId(
                    file.getOwningGroupId()).setSize(size).setAtimeNs((long) file.getAtime() * (long) 1e9)
                .setCtimeNs((long) file.getCtime() * (long) 1e9).setMtimeNs(
                    (long) file.getMtime() * (long) 1e9).setBlksize(blkSize).setTruncateEpoch(
                    file.isDirectory() ? 0 : file.getEpoch()).setAttributes((int) file.getW32Attrs())
                .setEtag(newEtag).build();
        
        return stat;
    }
    
    public static void main(String[] args) throws Exception {
        
        String path = "/home/stender/mnt";
        File f = new File(path);
        System.out.println(f.createNewFile());
    }
    
}
