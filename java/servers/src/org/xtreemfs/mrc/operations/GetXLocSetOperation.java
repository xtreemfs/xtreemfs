/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ac.FileAccessManager;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replicas;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;

/**
 * Returns the current XLocSet for the file specified in the request.<br>
 * This is different to {@link GetXLocListOperation} which returns a list of {@link Replicas}.
 */
public class GetXLocSetOperation extends MRCOperation {

    public GetXLocSetOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        final XCap xcap = (XCap) rq.getRequestArgs();

        final FileAccessManager faMan = master.getFileAccessManager();
        final VolumeManager vMan = master.getVolumeManager();

        StorageManager sMan = null;
        FileMetadata file = null;

        // Create a capability object to verify the capability.
        Capability cap = new Capability(xcap, master.getConfig().getCapabilitySecret());

        // Check whether the capability has a valid signature.
        if (!cap.hasValidSignature()) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, cap + " does not have a valid signature");
        }

        // Check whether the capability has expired.
        if (cap.hasExpired()) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, cap + " has expired");
        }

        // Parse volume and file ID from global file ID.
        GlobalFileIdResolver idRes = new GlobalFileIdResolver(cap.getFileId());

        sMan = vMan.getStorageManager(idRes.getVolumeId());

        // Retrieve the file metadata.
        file = sMan.getMetadata(idRes.getLocalFileId());
        if (file == null) {
            throw new UserException(POSIXErrno.POSIX_ERROR_ENOENT, "file '" + idRes.getLocalFileId()
                    + "' does not exist");
        }

        // Get the XLocSet from the XLocList.
        XLocList xLocList = file.getXLocList();
        assert (xLocList != null);
        XLocSet xLocSet = Converter.xLocListToXLocSet(xLocList).build();
        
        // Set the response.
        rq.setResponse(xLocSet);
        finishRequest(rq);
    }

}
