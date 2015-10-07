/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.ReplicationPolicy;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.stages.XLocSetLock;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.Path;
import org.xtreemfs.mrc.utils.PathResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.openRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.openResponse;

/**
 * 
 * @author stender
 */
public class OpenOperation extends MRCOperation {
    
    public OpenOperation(MRCRequestDispatcher master) {
        super(master);
        /*
         * try { logfile = new PrintWriter(new
         * FileOutputStream("/var/lib/xtreemfs/open.log",true)); } catch
         * (IOException ex) { ex.printStackTrace(); }
         */
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        // perform master redirect if necessary
        if (master.getReplMasterUUID() != null && !master.getReplMasterUUID().equals(master.getConfig().getUUID().toString()))
            throw new DatabaseException(ExceptionType.REDIRECT);
        
        final openRequest rqArgs = (openRequest) rq.getRequestArgs();
        
        final VolumeManager vMan = master.getVolumeManager();
        final FileAccessManager faMan = master.getFileAccessManager();
        
        Path p = new Path(rqArgs.getVolumeName(), rqArgs.getPath());
        
        StorageManager sMan = vMan.getStorageManagerByName(p.getComp(0));
        PathResolver res = new PathResolver(sMan, p);
        VolumeInfo volume = sMan.getVolumeInfo();
        
        // check whether the path prefix is searchable
        faMan.checkSearchPermission(sMan, res, rq.getDetails().userId, rq.getDetails().superUser, rq
                .getDetails().groupIds);
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        FileMetadata file = null;
        
        // analyze the flags
        boolean create = (rqArgs.getFlags() & FileAccessManager.O_CREAT) != 0;
        boolean excl = (rqArgs.getFlags() & FileAccessManager.O_EXCL) != 0;
        boolean truncate = (rqArgs.getFlags() & FileAccessManager.O_TRUNC) != 0;
        boolean write = (rqArgs.getFlags() & (FileAccessManager.O_WRONLY | FileAccessManager.O_RDWR)) != 0;
        
        boolean createNew = false;
        
        // atime, ctime, mtime
        int time = (int) (TimeSync.getGlobalTime() / 1000);
        
        // check whether the file/directory exists
        try {
            
            // check if volume is full
            long volumeQuota = volume.getVolumeQuota();
            if ((write || create) && volumeQuota != 0 && volumeQuota <= volume.getVolumeSize()) {
                throw new UserException(POSIXErrno.POSIX_ERROR_ENOSPC, "the volume's quota is reached");
            }
            
            res.checkIfFileDoesNotExist();
            
            // check if O_CREAT and O_EXCL are set; if so, send an exception
            if (create && excl)
                res.checkIfFileExistsAlready();
            
            file = res.getFile();
            
            if (file.isDirectory() || sMan.getSoftlinkTarget(file.getId()) != null)
                throw new UserException(POSIXErrno.POSIX_ERROR_EISDIR, "open is restricted to files");
            
            // check whether the file is marked as 'read-only'; in this
            // case, throw an exception if write access is requested
            if (file.isReadOnly()
                && ((rqArgs.getFlags() & (FileAccessManager.O_RDWR | FileAccessManager.O_WRONLY
                    | FileAccessManager.O_TRUNC | FileAccessManager.O_APPEND)) != 0))
                throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "read-only files cannot be written");
            
            // check whether the permission is granted
            faMan.checkPermission(rqArgs.getFlags(), sMan, file, res.getParentDirId(),
                rq.getDetails().userId, rq.getDetails().superUser, rq.getDetails().groupIds);

        } catch (UserException exc) {
            
            // if the file does not exist, check whether the O_CREAT flag
            // has been provided
            if (exc.getErrno() == POSIXErrno.POSIX_ERROR_ENOENT && create) {
                
                // check for write permission in parent dir
                // check whether the parent directory grants write access
                faMan.checkPermission(FileAccessManager.O_WRONLY, sMan, res.getParentDir(), res
                        .getParentsParentId(), rq.getDetails().userId, rq.getDetails().superUser, rq
                        .getDetails().groupIds);
                
                // get the next free file ID
                long fileId = sMan.getNextFileId();
                
                if ((rqArgs.getMode() & GlobalTypes.SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFIFO.getNumber()) != 0) {
                    throw new UserException(POSIXErrno.POSIX_ERROR_EIO, "FIFOs not supported");
                }
                
                // Inherit the groupId if the setgid bit is set
                String groupId = rq.getDetails().groupIds.get(0);
                int parentMode = faMan.getPosixAccessMode(sMan, res.getParentDir(), rq.getDetails().userId,
                        rq.getDetails().groupIds);
                if ((parentMode & 02000) > 0) {
                    groupId = res.getParentDir().getOwningGroupId();
                }

                // create the metadata object
                file = sMan.createFile(fileId, res.getParentDirId(), res.getFileName(), time, time, time,
                        rq.getDetails().userId, groupId, rqArgs.getMode(), rqArgs.getAttributes(), 0, false, 0, 0,
                        update);
                
                // set the file ID as the last one
                sMan.setLastFileId(fileId, update);
                
                createNew = true;
            }

            else
                throw exc;
        }
        
        XLocList xLocList = file.getXLocList();

        // Check if a xLocSet change is in progress and recover if a previous change failed due to an MRC error.
        XLocSetLock lock = master.getXLocSetCoordinator().getXLocSetLock(file, sMan);
        if (lock.isLocked()) {
            if (lock.hasCrashed()) {
                // If the xLocSet change did not finish, the majority of the replicas could be invalidated. To allow
                // operations on the (old) xLocSet, the version has to be increased to revalidate the replicas.
                XLoc[] replicas = new XLoc[xLocList.getReplicaCount()];
                for (int i = 0; i < xLocList.getReplicaCount(); i++) {
                    replicas[i] = xLocList.getReplica(i);
                }
                xLocList = sMan.createXLocList(replicas, xLocList.getReplUpdatePolicy(), xLocList.getVersion() + 1);

                // Unlock the replica.
                master.getXLocSetCoordinator().unlockXLocSet(file, sMan, update);
            } else {
                throw new UserException(POSIXErrno.POSIX_ERROR_EAGAIN,
                        "xLocSet change already in progress. Please retry.");
            }
        }

        // get the current epoch, use (and increase) the truncate number if
        // the open mode is truncate
        int trEpoch = file.getEpoch();
        if (truncate) {
            file.setIssuedEpoch(file.getIssuedEpoch() + 1);
            trEpoch = file.getIssuedEpoch();
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
        }

        // retrieve the default replication policy
        ReplicationPolicy defaultReplPolicy = sMan.getDefaultReplicationPolicy(res.getParentDirId());
        if (defaultReplPolicy == null)
            defaultReplPolicy = sMan.getDefaultReplicationPolicy(1);
        
        // flag indicating whether on-close replication will be triggered
        boolean replicateOnClose = defaultReplPolicy != null
            && ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY.equals(defaultReplPolicy.getName());
        
        // if no replicas have been assigned yet ...
        if ((xLocList == null || xLocList.getReplicaCount() == 0) && (create || write)) {
            
            // if the file is supposed to be read-only replicated, create a
            // non-replicated file and defer the replication until the file is
            // closed
            boolean singleReplica = defaultReplPolicy == null
                || ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE.equals(defaultReplPolicy.getName())
                || replicateOnClose;
            
            if (singleReplica) {
                
                // create a replica with the default striping policy together
                // with a set of feasible OSDs from the OSD status manager
                XLoc replica = MRCHelper.createReplica(null, sMan, master.getOSDStatusManager(), volume, res
                        .getParentDirId(), p.toString(), ((InetSocketAddress) rq.getRPCRequest()
                        .getSenderAddress()).getAddress(), rqArgs.getCoordinates(), xLocList, 0);
                
                // integrate the new replica in the XLoc list
                String[] osds = new String[replica.getOSDCount()];
                for (int j = 0; j < osds.length; j++)
                    osds[j] = replica.getOSD(j);
                
                List<XLoc> repls = new ArrayList<XLoc>();
                repls.add(replica);
                
                // update the XLoc list with the new replica; this is
                // necessary to ensure that its OSDs will be included when
                // adding further replias in the following loop iterations
                xLocList = sMan.createXLocList(repls.toArray(new XLoc[repls.size()]),
                    ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE, 0);
                
            }

            // otherwise, create the requested number of replicas
            else {
                
                // determine the number of replicas to be added
                int numReplicas = 1;
                if (defaultReplPolicy != null)
                    numReplicas = defaultReplPolicy.getFactor();
                
                // assign as many new replicas as needed
                List<XLoc> repls = new ArrayList<XLoc>();
                for (int i = 0; i < numReplicas; i++) {
                    
                    // create a replica with the default striping policy
                    // together
                    // with a set of feasible OSDs from the OSD status manager
                    XLoc replica = MRCHelper.createReplica(null, sMan, master.getOSDStatusManager(), volume,
                        res.getParentDirId(), p.toString(), ((InetSocketAddress) rq.getRPCRequest()
                                .getSenderAddress()).getAddress(), rqArgs.getCoordinates(), xLocList,
                        defaultReplPolicy != null ? defaultReplPolicy.getFlags() : 0);
                    
                    // integrate the new replica in the XLoc list
                    String[] osds = new String[replica.getOSDCount()];
                    for (int j = 0; j < osds.length; j++)
                        osds[j] = replica.getOSD(j);
                    
                    repls.add(replica);
                    
                    // update the XLoc list with the new replica; this is
                    // necessary to ensure that its OSDs will be included when
                    // adding further replias in the following loop iterations
                    xLocList = sMan.createXLocList(repls.toArray(new XLoc[repls.size()]),
                        defaultReplPolicy != null ? defaultReplPolicy.getName()
                            : ReplicaUpdatePolicies.REPL_UPDATE_PC_NONE, 0);
                }
                
            }
            
            // update the file's XLoc list
            file.setXLocList(xLocList);
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                    "assigned the following XLoc list to file %s:%d: %s", volume.getId(), file.getId(),
                    xLocList.toString());
        }
        
        // convert the XLocList to an XLocSet (necessary for later client
        // transfer)
        XLocSet.Builder xLocSet = Converter.xLocListToXLocSet(xLocList);
        
        // re-order the replica list, based on the replica selection policy
        List<Replica> sortedReplList = master.getOSDStatusManager().getSortedReplicaList(volume.getId(),
            ((InetSocketAddress) rq.getRPCRequest().getSenderAddress()).getAddress(),
            rqArgs.getCoordinates(), xLocSet.getReplicasList(), xLocList).getReplicasList();
        xLocSet.clearReplicas();
        xLocSet.addAllReplicas(sortedReplList);
        xLocSet.setReadOnlyFileSize(file.getSize());
        
        // issue a new capability
        Capability cap = new Capability(MRCHelper.createGlobalFileId(volume, file), rqArgs.getFlags(), master
                .getConfig().getCapabilityTimeout(), TimeSync.getGlobalTime() / 1000
            + master.getConfig().getCapabilityTimeout(), ((InetSocketAddress) rq.getRPCRequest()
                .getSenderAddress()).getAddress().getHostAddress(), trEpoch, replicateOnClose, !volume
                .isSnapshotsEnabled() ? SnapConfig.SNAP_CONFIG_SNAPS_DISABLED
            : volume.isSnapVolume() ? SnapConfig.SNAP_CONFIG_ACCESS_SNAP
                : SnapConfig.SNAP_CONFIG_ACCESS_CURRENT, volume.getCreationTime(), master.getConfig()
                .getCapabilitySecret(), sMan.getVolumePriority());
        
        if (Logging.isDebug())
            Logging
                    .logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                        "issued the following capability for %s:%d: %s", volume.getId(), file.getId(), cap
                                .toString());
        
        // update POSIX timestamps of file
        
        if (createNew || truncate) {
            // create or truncate: ctime + mtime, atime only if create
            MRCHelper.updateFileTimes(res.getParentsParentId(), file, createNew ? !master.getConfig()
                    .isNoAtime() : false, true, true, sMan, time, update);
        
        } else if (!master.getConfig().isNoAtime()) {
            // otherwise: only atime, if necessary
            MRCHelper.updateFileTimes(res.getParentsParentId(), file, true, false, false, sMan, time, update);
        } else {
            time = 0;
        }
        
        // update POSIX timestamps of parent directory, in case of a newly
        // created file
        if (createNew)
            MRCHelper.updateFileTimes(res.getParentsParentId(), res.getParentDir(), false, true, true, sMan,
                time, update);
        
        // set the response
        rq.setResponse(openResponse.newBuilder().setCreds(
            FileCredentials.newBuilder().setXcap(cap.getXCap()).setXlocs(xLocSet)).setTimestampS(time)
                .build());
        
        update.execute();
        
        // enable only for test servers that should log each file
        // create/write/trunc
        /*
         * if (create || write || truncate) { try {
         * logfile.print(System.currentTimeMillis
         * ()+";"+rq.getRPCRequest().getClientIdentity
         * ()+";"+rqArgs.getPath()+"\n"); logfile.flush(); } catch (Exception
         * ex) {
         * 
         * } }
         */
    }
}
