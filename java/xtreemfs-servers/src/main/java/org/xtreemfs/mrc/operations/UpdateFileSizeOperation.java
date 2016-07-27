/*
 * Copyright (c) 2010-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.xloc.ReplicationFlags;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.ReplicationPolicy;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_update_file_sizeRequest;

/**
 * Sets attributes of a file.
 * 
 * @author stender
 */
public class UpdateFileSizeOperation extends MRCOperation {
    
    public UpdateFileSizeOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        // perform master redirect if necessary
        if (master.getReplMasterUUID() != null && !master.getReplMasterUUID().equals(master.getConfig().getUUID().toString()))
            throw new DatabaseException(ExceptionType.REDIRECT);
        
        final xtreemfs_update_file_sizeRequest rqArgs = (xtreemfs_update_file_sizeRequest) rq
                .getRequestArgs();
        
        Capability cap = new Capability(rqArgs.getXcap(), master.getConfig().getCapabilitySecret());
        
        // check whether the capability has a valid signature
        if (!cap.hasValidSignature())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, cap + " does not have a valid signature");
        
        // check whether the capability has expired
        if (cap.hasExpired())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, cap + " has expired");
        
        // parse volume and file ID from global file ID
        GlobalFileIdResolver idRes = new GlobalFileIdResolver(cap.getFileId());
        
        StorageManager sMan = master.getVolumeManager().getStorageManager(idRes.getVolumeId());
        
        FileMetadata file = sMan.getMetadata(idRes.getLocalFileId());
        if (file == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file '" + cap.getFileId()
                + "' does not exist");
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        // update the file size if necessary
        if (rqArgs.getOsdWriteResponse().hasSizeInBytes()) {
            
            if (file.isReadOnly())
                throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "file '" + cap.getFileId()
                    + "' is read-only");
            
            if (!rqArgs.getOsdWriteResponse().hasTruncateEpoch())
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                    "missing truncate epoch in OSDWriteResponse");
            
            long newFileSize = rqArgs.getOsdWriteResponse().getSizeInBytes();
            int epochNo = rqArgs.getOsdWriteResponse().getTruncateEpoch();
            
            // only accept valid file size updates
            if (epochNo >= file.getEpoch()) {
                
                boolean epochChanged = epochNo > file.getEpoch();
                
                // accept any file size in a new epoch but only larger file
                // sizes in the current epoch
                if (epochChanged || newFileSize > file.getSize()) {
                    
                    long oldFileSize = file.getSize();
                    int time = (int) (TimeSync.getGlobalTime() / 1000);
                    
                    file.setSize(newFileSize);
                    file.setEpoch(epochNo);
                    file.setCtime(time);
                    file.setMtime(time);
                    
                    sMan.setMetadata(file, FileMetadata.FC_METADATA, update);
                    
                    if (epochChanged)
                        sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
                    
                    // update the volume size
                    sMan.getVolumeInfo().updateVolumeSize(newFileSize - oldFileSize, update);
                }

                else if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                        "received update for outdated file size: " + newFileSize + ", current file size="
                            + file.getSize());
            }

            else {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this,
                        "received file size update w/ outdated epoch: " + epochNo + ", current epoch="
                            + file.getEpoch());
            }
        }
        
        // check if file is closed and on-close replication is required
        if (rqArgs.getCloseFile() && cap.getXCap().getReplicateOnClose()) {
            
            VolumeInfo vol = sMan.getVolumeInfo();
            
            file.setReadOnly(true);
            
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "file closed and set to readOnly");
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "replicating file");
            
            XLocList xLocList = file.getXLocList();
            
            // retrieve the default replication policy
            // FIXME: use the parent directory's default replication policy
            ReplicationPolicy defaultReplPolicy = sMan.getDefaultReplicationPolicy(file.getId());
            if (defaultReplPolicy == null)
                defaultReplPolicy = sMan.getDefaultReplicationPolicy(1);
            
            // determine the number of replicas to create
            int requiredReplicaCount;
            if (defaultReplPolicy == null) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.replication,
                    "replicate on close enabled w/o default replication policy");
                requiredReplicaCount = 1;
                
            } else
                requiredReplicaCount = defaultReplPolicy.getFactor();
            
            // if replicas need to be created on close ...
            if (requiredReplicaCount > xLocList.getReplicaCount()) {
                
                assert (defaultReplPolicy != null);
                
                List<XLoc> repls = new ArrayList<XLoc>();
                for (int i = 0; i < xLocList.getReplicaCount(); i++)
                    repls.add(xLocList.getReplica(i));
                
                int newVer = xLocList.getVersion() + 1;
                int initialReplCount = xLocList.getReplicaCount();
                
                XLoc firstRepl = repls.get(0);
                
                int replFlags = firstRepl.getReplicationFlags();

                // Add the default strategy, if the current replication flags miss it.
                replFlags = MRCHelper.restoreStrategyFlag(replFlags, defaultReplPolicy);

                // mark the first replica as complete and full
                firstRepl.setReplicationFlags(
                        ReplicationFlags.setFullReplica(ReplicationFlags.setReplicaIsComplete(replFlags)));
                
                // determine the replication flags for the new replicas:
                // full + 'rarest first' strategy for full replicas,
                // 'sequential prefetching' strategy for partial replicas
                // 
                // final int replFlags = vol.getAutoReplFull() ?
                // ReplicationFlags
                // .setFullReplica(ReplicationFlags.setRarestFirstStrategy(0)) :
                // ReplicationFlags
                // .setSequentialPrefetchingStrategy(0);
                
                // try to replicate the file
                try {
                    
                    for (int i = 0; i < requiredReplicaCount - initialReplCount; i++) {
                        
                        // create the new replica
                        XLoc newRepl = MRCHelper.createReplica(firstRepl.getStripingPolicy(), sMan, master
                                .getOSDStatusManager(), vol, -1, cap.getFileId(), ((InetSocketAddress) rq
                                .getRPCRequest().getSenderAddress()).getAddress(), rqArgs.getCoordinates(),
                            xLocList, defaultReplPolicy.getFlags());
                        
                        String[] osds = new String[newRepl.getOSDCount()];
                        for (int j = 0; j < osds.length; j++)
                            osds[j] = newRepl.getOSD(j);
                        
                        repls.add(sMan.createXLoc(firstRepl.getStripingPolicy(), osds, newRepl
                                .getReplicationFlags()));
                        
                        xLocList = sMan.createXLocList(repls.toArray(new XLoc[repls.size()]),
                            ReplicaUpdatePolicies.REPL_UPDATE_PC_RONLY, newVer);
                        
                    }
                    
                } catch (Exception exc) {
                    
                    // if the attempt to replicate the file fails for whatever
                    // reason, print a warning message and continue
                    
                    Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this,
                        "could not replicate file '%d' on close", file.getId());
                    Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this, OutputUtils
                            .stackTraceToString(exc));
                }
                
                file.setXLocList(xLocList);
            }
            
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "added %d replicas",
                    requiredReplicaCount - xLocList.getReplicaCount());
            
            // if the file has data and 'full' replicas are enabled, trigger the replication
            if (file.getSize() > 0 && ReplicationFlags.isFullReplica(defaultReplPolicy.getFlags())) {
                
                XLocSet.Builder xLocSet = Converter.xLocListToXLocSet(xLocList);
                xLocSet.setReadOnlyFileSize(file.getSize());
                
                rq.getDetails().context = new HashMap<String, Object>();
                rq.getDetails().context.put("xLocList", xLocSet.build());
                master.getOnCloseReplicationThread().enqueueRequest(rq);
            }
        }
        
        // set the response
        rq.setResponse(emptyResponse.getDefaultInstance());
        
        update.execute();
        
    }
    
}
