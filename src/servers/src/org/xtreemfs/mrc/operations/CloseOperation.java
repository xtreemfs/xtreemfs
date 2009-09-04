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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.mrc.operations;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.MRCInterface.closeRequest;
import org.xtreemfs.interfaces.MRCInterface.closeResponse;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.metadata.FileMetadata;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.mrc.utils.MRCHelper.GlobalFileIdResolver;

/**
 * 
 * @author stender
 */
public class CloseOperation extends MRCOperation {
    
    public CloseOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        final closeRequest rqArgs = (closeRequest) rq.getRequestArgs();
        
        Capability cap = new Capability(rqArgs.getWrite_xcap(), master.getConfig().getCapabilitySecret());
        
        // check whether the capability has a valid signature
        if (!cap.hasValidSignature())
            throw new UserException(ErrNo.EPERM, cap + " does not have a valid signature");
        
        // check whether the capability has expired
        if (cap.hasExpired())
            throw new UserException(ErrNo.EPERM, cap + " has expired");
        
        if (cap.getXCap().getReplicateOnClose()) {
            
            // parse volume and file ID from global file ID
            GlobalFileIdResolver idRes = new GlobalFileIdResolver(cap.getFileId());
            StorageManager sMan = master.getVolumeManager().getStorageManager(idRes.getVolumeId());
            VolumeInfo vol = sMan.getVolumeInfo();
            
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
            
            FileMetadata file = sMan.getMetadata(idRes.getLocalFileId());
            if (file == null)
                throw new UserException(ErrNo.ENOENT, "file '" + cap.getFileId() + "' does not exist");
            
            file.setReadOnly(true);
            
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.proc, this, "file closed and set to readOnly");
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "replicating file");
            
            XLocList xLocList = file.getXLocList();
            
            // replicate
            int replFactor = vol.getAutoReplFactor();
            List<XLoc> repls = new LinkedList<XLoc>();
            for (int i = 0; i < xLocList.getReplicaCount(); i++)
                repls.add(xLocList.getReplica(i));
            
            int newVer = xLocList.getVersion() + 1;
            for (int i = 0; i < replFactor - 1; i++) {
                
                Replica newRepl = MRCHelper.createReplica(file.getXLocList().getReplica(0)
                        .getStripingPolicy(), sMan, master.getOSDStatusManager(), vol, -1, cap.getFileId(),
                    ((InetSocketAddress) rq.getRPCRequest().getClientIdentity()).getAddress(), xLocList);
                
                String[] osds = newRepl.getOsd_uuids().toArray(new String[newRepl.getOsd_uuids().size()]);
                repls.add(sMan.createXLoc(xLocList.getReplica(0).getStripingPolicy(), osds, newRepl
                        .getReplication_flags()));
                
                xLocList = sMan.createXLocList(repls.toArray(new XLoc[repls.size()]), xLocList
                        .getReplUpdatePolicy(), newVer);
            }
            
            file.setXLocList(xLocList);
            
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "added %d replicas", vol
                    .getAutoReplFactor() - 1);
            
            // trigger the replication
            master.getOnCloseReplicationThread().enqueueRequest(rq);
            
            // set the response
            rq.setResponse(new closeResponse());
            
            update.execute();
        }

        else {
            
            // set the response
            rq.setResponse(new closeResponse());
            
            Logging.logMessage(Logging.LEVEL_WARN, this,
                "got close() request for non-replicate-on-close file, cap=%s, ignoring it", cap.toString());
            
            finishRequest(rq);
        }
        
    }
}
