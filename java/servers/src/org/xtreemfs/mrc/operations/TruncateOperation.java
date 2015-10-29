/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */


package org.xtreemfs.mrc.operations;

import java.net.InetSocketAddress;

import org.xtreemfs.common.Capability;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;

/**
 * 
 * @author stender
 */
public class TruncateOperation extends MRCOperation {
    
    public TruncateOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        // perform master redirect if necessary
        if (master.getReplMasterUUID() != null && !master.getReplMasterUUID().equals(master.getConfig().getUUID().toString()))
            throw new DatabaseException(ExceptionType.REDIRECT);
        
        final XCap xcap = (XCap) rq.getRequestArgs();
        
        Capability writeCap = new Capability(xcap, master.getConfig().getCapabilitySecret());
        
        // check whether the capability has a valid signature
        if (!writeCap.hasValidSignature())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, writeCap
                + " does not have a valid signature");
        
        // check whether the capability has expired
        if (writeCap.hasExpired())
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, writeCap + " has expired");
        
        // check whether the capability grants write permissions
        if ((writeCap.getAccessMode() & (FileAccessManager.O_WRONLY | FileAccessManager.O_RDWR | FileAccessManager.O_TRUNC)) == 0)
            throw new UserException(POSIXErrno.POSIX_ERROR_EACCES, writeCap + " is not a write capability");
        
        // parse volume and file ID from global file ID
        GlobalFileIdResolver idRes = new GlobalFileIdResolver(writeCap.getFileId());
        
        StorageManager sMan = master.getVolumeManager().getStorageManager(idRes.getVolumeId());
        
        FileMetadata file = sMan.getMetadata(idRes.getLocalFileId());
        if (file == null)
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file '" + writeCap.getFileId()
                + "' does not exist");
        
        // get the current epoch, use (and increase) the truncate number if
        // the open mode is truncate
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
        
        int newEpoch = file.getIssuedEpoch() + 1;
        file.setIssuedEpoch(newEpoch);
        sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
        
        // create a truncate capability from the previous write capability
        Capability truncCap = new Capability(writeCap.getFileId(),
                writeCap.getAccessMode() | FileAccessManager.O_TRUNC, master.getConfig().getCapabilityTimeout(),
                TimeSync.getGlobalTime() / 1000 + master.getConfig().getCapabilityTimeout(), ((InetSocketAddress) rq
                        .getRPCRequest().getSenderAddress()).getAddress().getHostAddress(), newEpoch,
                writeCap.isReplicateOnClose(),
                !sMan.getVolumeInfo().isSnapshotsEnabled() ? SnapConfig.SNAP_CONFIG_SNAPS_DISABLED : sMan
                        .getVolumeInfo().isSnapVolume() ? SnapConfig.SNAP_CONFIG_ACCESS_SNAP
                        : SnapConfig.SNAP_CONFIG_ACCESS_CURRENT, sMan.getVolumeInfo().getCreationTime(),
                        writeCap.getTraceConfig().getTraceRequests(), writeCap.getTraceConfig().getTracingPolicyConfig(),
                        writeCap.getTraceConfig().getTracingPolicy(), writeCap.getVoucherSize(),
                        TimeSync.getGlobalTime() + master.getConfig().getCapabilityTimeout() * 1000,
                        master.getConfig().getCapabilitySecret());

        // set the response
        rq.setResponse(truncCap.getXCap());
        update.execute();
    }
    
}
