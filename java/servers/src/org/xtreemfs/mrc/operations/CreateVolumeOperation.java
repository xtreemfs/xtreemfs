/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceDataMap;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Volume;

/**
 * 
 * @author stender
 */
public class CreateVolumeOperation extends MRCOperation {
    
    public CreateVolumeOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(final MRCRequest rq) throws Throwable {
        
        // perform master redirect if replicated and required
        String replMasterUUID = master.getReplMasterUUID();
        if (replMasterUUID != null && !replMasterUUID.equals(master.getConfig().getUUID().toString())) {
            ServiceUUID uuid = new ServiceUUID(replMasterUUID);
            throw new DatabaseException(ExceptionType.REDIRECT, uuid.getAddress().getHostName() + ":"
                    + uuid.getAddress().getPort());
        }
        
        final Volume volData = (Volume) rq.getRequestArgs();
        
        // check password to ensure that user is authorized
        if (master.getConfig().getAdminPassword().length() > 0
                && !master.getConfig().getAdminPassword().equals(rq.getDetails().password))
            throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "invalid password");
        
        validateContext(rq);
        
        try {
            master.getFileAccessManager().getFileAccessPolicy((short) volData.getAccessControlPolicy().getNumber());
        } catch (Exception exc) {
            throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL, "invalid file access policy ID: "
                    + volData.getAccessControlPolicy());
        }
        
        // in order to allow volume creation in a single-threaded
        // non-blocking manner, it needs to be performed in two steps:
        // * first, the volume is registered with the directory service
        // * when registration has been confirmed at the directory service,
        // request processing is continued with step 2
        
        final String volumeId = master.getVolumeManager().newVolumeId();
        
        // check whether a volume with the same name has already been
        // registered at the Directory Service
        
        Map<String, Object> queryMap = new HashMap<String, Object>();
        queryMap.put("name", volData.getName());
        List<String> attrs = new LinkedList<String>();
        attrs.add("version");
        
        // Ugly workaround for async call.
        Runnable rqThr = new Runnable() {
            @Override
            public void run() {
                try {
                    ServiceSet sset = master.getDirClient().xtreemfs_service_get_by_type(null, rq.getDetails().auth,
                            RPCAuthentication.userService, ServiceType.SERVICE_TYPE_VOLUME);
                    processStep2(volData, volumeId, rq, sset);
                } catch (Exception ex) {
                    finishRequest(rq, new ErrorRecord(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                            "an error has occurred", ex));
                }
            }
        };
        Thread thr = new Thread(rqThr);
        thr.start();
    }
    
    private void processStep2(final Volume volData, final String volumeId, final MRCRequest rq, ServiceSet response) {
        try {
            // check if the volume already exists; if so, return an error
            for (Service reg : response.getServicesList())
                if (volData.getName().equals(reg.getName())) {
                    String uuid = reg.getUuid();
                    throw new UserException(POSIXErrno.POSIX_ERROR_EEXIST, "volume '" + volData.getName()
                            + "' already exists in Directory Service, id='" + uuid + "'");
                }
            
            // determine owner and owning group for the new volume
            String uid = volData.getOwnerUserId();
            String gid = volData.getOwnerGroupId();
            
            if ("".equals(uid))
                uid = rq.getDetails().userId;
            if ("".equals(gid))
                gid = rq.getDetails().groupIds.get(0);

            long quota = volData.hasQuota()?volData.getQuota():0L;
            int priority = volData.hasPriority()?volData.getPriority():0;
            
            // create the volume locally
            master.getVolumeManager().createVolume(master.getFileAccessManager(), volumeId, volData.getName(),
                    (short) volData.getAccessControlPolicy().getNumber(), uid, gid, volData.getDefaultStripingPolicy(),
                    volData.getMode(), quota, priority, volData.getAttrsList());
            
            master.notifyVolumeCreated();
            
            // register the volume at the Directory Service
            
            ServiceDataMap.Builder dmap = ServiceDataMap.newBuilder();
            dmap.addData(KeyValuePair.newBuilder().setKey("mrc").setValue(master.getConfig().getUUID().toString()));
            dmap.addData(KeyValuePair.newBuilder().setKey("free").setValue("0"));
            
            // add all user-defined volume attributes
            for (KeyValuePair kv : volData.getAttrsList())
                dmap.addData(KeyValuePair.newBuilder().setKey("attr." + kv.getKey()).setValue(kv.getValue()));
            
            final Service vol = Service.newBuilder().setType(ServiceType.SERVICE_TYPE_VOLUME).setUuid(volumeId)
                    .setVersion(0).setName(volData.getName()).setLastUpdatedS(0).setData(dmap).build();
            
            // Ugly workaround for async call.
            Runnable rqThr = new Runnable() {
                @Override
                public void run() {
                    try {
                        master.getDirClient().xtreemfs_service_register(null, rq.getDetails().auth,
                                RPCAuthentication.userService, vol);
                        processStep3(volData, volumeId, rq);
                    } catch (Exception ex) {
                        finishRequest(rq, new ErrorRecord(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                                "an error has occurred", ex));
                    }
                }
            };
            Thread thr = new Thread(rqThr);
            thr.start();
            
        } catch (UserException exc) {
            if (Logging.isDebug())
                Logging.logUserError(Logging.LEVEL_DEBUG, Category.proc, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorType.ERRNO, exc.getErrno(), exc.getMessage(), exc));
        } catch (DatabaseException exc) {
            finishRequest(rq, new ErrorRecord(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                    "an error has occurred", exc));
        } catch (Throwable exc) {
            finishRequest(rq, new ErrorRecord(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                    "an error has occurred", exc));
        }
    }
    
    public void processStep3(final Volume rqArgs, final String volumeId, final MRCRequest rq) {
        try {
            // set the response
            rq.setResponse(emptyResponse.getDefaultInstance());
            finishRequest(rq);
        } catch (Throwable exc) {
            finishRequest(rq, new ErrorRecord(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
                    "an error has occurred", exc));
        }
    }
    
}
