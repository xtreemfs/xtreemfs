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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.foundation.oncrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceDataMap;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.Volume;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_mkvolRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_mkvolResponse;
import org.xtreemfs.mrc.ErrNo;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;

/**
 * 
 * @author stender
 */
public class CreateVolumeOperation extends MRCOperation {
    
    public static final int OP_ID = 10;
    
    public CreateVolumeOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(final MRCRequest rq) {
        
        try {
            
            final xtreemfs_mkvolRequest rqArgs = (xtreemfs_mkvolRequest) rq.getRequestArgs();
            
            validateContext(rq);
            Volume volData = rqArgs.getVolume();
            
            // first, check whether the given policies are supported
            
            if (master.getOSDStatusManager().getOSDSelectionPolicy((short) volData.getOsd_selection_policy()) == null)
                throw new UserException(ErrNo.EINVAL, "invalid OSD selection policy ID: "
                    + volData.getOsd_selection_policy());
            
            if (master.getFileAccessManager().getFileAccessPolicy((short) volData.getAccess_control_policy()) == null)
                throw new UserException(ErrNo.EINVAL, "invalid file access policy ID: "
                    + volData.getAccess_control_policy());
            
            // in order to allow volume creation in a single-threaded
            // non-blocking
            // manner, it needs to be performed in two steps:
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
            
            RPCResponse<ServiceSet> response = master.getDirClient().xtreemfs_service_get_by_type(null,
                Constants.SERVICE_TYPE_VOLUME);
            response.registerListener(new RPCResponseAvailableListener<ServiceSet>() {
                
                @Override
                public void responseAvailable(RPCResponse<ServiceSet> r) {
                    processStep2(rqArgs, volumeId, rq, r);
                }
            });
            
        } catch (UserException exc) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc.getMessage(),
                exc));
        } catch (Throwable exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, "an error has occurred", exc));
        }
    }
    
    private void processStep2(final xtreemfs_mkvolRequest rqArgs, final String volumeId, final MRCRequest rq,
        RPCResponse<ServiceSet> rpcResponse) {
        
        try {
            
            // check the response; if a volume with the same name has already
            // been registered, return an error
            
            ServiceSet response = rpcResponse.get();
            Volume volData = rqArgs.getVolume();
            
            // check if the volume already exists
            for (Service reg : response)
                if (volData.getName().equals(reg.getName())) {
                    String uuid = reg.getUuid();
                    throw new UserException(ErrNo.EEXIST, "volume '" + volData.getName()
                        + "' already exists in Directory Service, id='" + uuid + "'");
                }
            
            // otherwise, register the volume at the Directory Service
            
            ServiceDataMap dmap = new ServiceDataMap();
            dmap.put("mrc", master.getConfig().getUUID().toString());
            dmap.put("free", "0");
            Service vol = new Service(volumeId, 0, Constants.SERVICE_TYPE_VOLUME, volData.getName(), 0, dmap);
            
            RPCResponse<Long> rpcResponse2 = master.getDirClient().xtreemfs_service_register(null, vol);
            rpcResponse2.registerListener(new RPCResponseAvailableListener<Long>() {
                
                @Override
                public void responseAvailable(RPCResponse<Long> r) {
                    processStep3(rqArgs, volumeId, rq, r);
                }
            });
            
        } catch (UserException exc) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc.getMessage(),
                exc));
        } catch (Throwable exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, "an error has occurred", exc));
        } finally {
            rpcResponse.freeBuffers();
        }
    }
    
    public void processStep3(final xtreemfs_mkvolRequest rqArgs, final String volumeId, final MRCRequest rq,
        RPCResponse<Long> rpcResponse) {
        
        try {
            
            // check whether an exception has occured; if so, an exception is
            // thrown when trying to parse the response
            
            rpcResponse.get();
            Volume volData = rqArgs.getVolume();
            
            // create the volume and its database
            master.getVolumeManager().createVolume(master.getFileAccessManager(), volumeId,
                volData.getName(), (short) volData.getAccess_control_policy(),
                (short) volData.getOsd_selection_policy(), null, rq.getDetails().userId,
                rq.getDetails().groupIds.get(0), volData.getDefault_striping_policy(), volData.getMode());
            
            // set the response
            rq.setResponse(new xtreemfs_mkvolResponse());
            finishRequest(rq);
            
        } catch (UserException exc) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc.getMessage(),
                exc));
        } catch (Throwable exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, "an error has occurred", exc));
        } finally {
            rpcResponse.freeBuffers();
        }
    }
    
}
