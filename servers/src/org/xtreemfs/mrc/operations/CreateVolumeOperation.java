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
import org.xtreemfs.interfaces.Context;
import org.xtreemfs.interfaces.KeyValuePair;
import org.xtreemfs.interfaces.KeyValuePairSet;
import org.xtreemfs.interfaces.ServiceRegistry;
import org.xtreemfs.interfaces.ServiceRegistrySet;
import org.xtreemfs.interfaces.MRCInterface.mkvolRequest;
import org.xtreemfs.interfaces.MRCInterface.mkvolResponse;
import org.xtreemfs.interfaces.ServiceRegistryDataMap;
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
            
            final mkvolRequest rqArgs = (mkvolRequest) rq.getRequestArgs();

            validateContext(rq);
            
            // first, check whether the given policies are supported
            
            if (master.getOSDStatusManager().getOSDSelectionPolicy((short) rqArgs.getOsd_selection_policy()) == null)
                throw new UserException(ErrNo.EINVAL, "invalid OSD selection policy ID: "
                    + rqArgs.getOsd_selection_policy());
            
            if (master.getFileAccessManager().getFileAccessPolicy((short) rqArgs.getAccess_control_policy()) == null)
                throw new UserException(ErrNo.EINVAL, "invalid file access policy ID: "
                    + rqArgs.getAccess_control_policy());
            
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
            queryMap.put("name", rqArgs.getVolume_name());
            List<String> attrs = new LinkedList<String>();
            attrs.add("version");
            
            RPCResponse<ServiceRegistrySet> response = master.getDirClient().service_get_by_type(null,
                Constants.SERVICE_TYPE_VOLUME);
            response.registerListener(new RPCResponseAvailableListener<ServiceRegistrySet>() {
                
                @Override
                public void responseAvailable(RPCResponse<ServiceRegistrySet> r) {
                    processStep2(rqArgs, volumeId, rq, r);
                }
            });
            
        } catch (UserException exc) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc.getMessage(),
                exc));
        } catch (Exception exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, "an error has occurred", exc));
        }
    }
    
    private void processStep2(final mkvolRequest rqArgs, final String volumeId, final MRCRequest rq,
        RPCResponse<ServiceRegistrySet> rpcResponse) {
        
        try {
            
            // check the response; if a volume with the same name has already
            // been registered, return an error
            
            ServiceRegistrySet response = rpcResponse.get();
            
            // check if the volume already exists
            for (ServiceRegistry reg : response)
                if (rqArgs.getVolume_name().equals(reg.getService_name())) {
                    String uuid = reg.getUuid();
                    throw new UserException(ErrNo.EEXIST, "volume '" + rqArgs.getVolume_name()
                        + "' already exists in Directory Service, id='" + uuid + "'");
                }
            
            // otherwise, register the volume at the Directory Service
            
            ServiceRegistryDataMap dmap = new ServiceRegistryDataMap();
            dmap.put("mrc", master.getConfig().getUUID().toString());
            dmap.put("free", "0");
            ServiceRegistry vol = new ServiceRegistry(volumeId, 0, Constants.SERVICE_TYPE_VOLUME, rqArgs
                    .getVolume_name(), 0, dmap);
            
            RPCResponse<Long> rpcResponse2 = master.getDirClient().service_register(null, vol);
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
        } catch (Exception exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, "an error has occurred", exc));
        } finally {
            rpcResponse.freeBuffers();
        }
    }
    
    public void processStep3(final mkvolRequest rqArgs, final String volumeId, final MRCRequest rq,
        RPCResponse<Long> rpcResponse) {
        
        try {
            
            // check whether an exception has occured; if so, an exception is
            // thrown when trying to parse the response
            
            rpcResponse.get();
            
            // create the volume and its database
            master.getVolumeManager().createVolume(master.getFileAccessManager(), volumeId,
                rqArgs.getVolume_name(), (short) rqArgs.getAccess_control_policy(),
                (short) rqArgs.getOsd_selection_policy(), null, rq.getDetails().userId,
                rq.getDetails().groupIds.get(0), rqArgs.getDefault_striping_policy());
            
            // set the response
            rq.setResponse(new mkvolResponse());
            finishRequest(rq);
            
        } catch (UserException exc) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc.getMessage(),
                exc));
        } catch (Exception exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, "an error has occurred", exc));
        } finally {
            rpcResponse.freeBuffers();
        }
    }
    
    public Context getContext(MRCRequest rq) {
        return ((mkvolRequest) rq.getRequestArgs()).getContext();
    }
    
}
