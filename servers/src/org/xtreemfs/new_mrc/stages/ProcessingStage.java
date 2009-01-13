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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.new_mrc.stages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.auth.AuthenticationException;
import org.xtreemfs.common.auth.UserCredentials;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.mrc.brain.ErrNo;
import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.operations.CheckAccessOperation;
import org.xtreemfs.new_mrc.operations.CreateDirOperation;
import org.xtreemfs.new_mrc.operations.CreateFileOperation;
import org.xtreemfs.new_mrc.operations.CreateVolumeOperation;
import org.xtreemfs.new_mrc.operations.GetLocalVolumesOperation;
import org.xtreemfs.new_mrc.operations.GetProtocolVersionOperation;
import org.xtreemfs.new_mrc.operations.MRCOperation;
import org.xtreemfs.new_mrc.operations.ReadDirOperation;
import org.xtreemfs.new_mrc.operations.ShutdownOperation;
import org.xtreemfs.new_mrc.operations.StatOperation;
import org.xtreemfs.new_mrc.operations.StatusPageOperation;

/**
 * 
 * @author bjko
 */
public class ProcessingStage extends MRCStage {
    
    public static final int                 STAGEOP_PARSE_AND_EXECUTE = 1;
    
    public static final int                 STAGEOP_FINISH            = 2;
    
    private final MRCRequestDispatcher      master;
    
    private final Map<String, MRCOperation> operations;
    
    public ProcessingStage(MRCRequestDispatcher master) {
        super("ProcSt");
        this.master = master;
        operations = new HashMap();
        
        installOperations();
    }
    
    public void installOperations() {
        operations.put(ShutdownOperation.RPC_NAME, new ShutdownOperation(master));
        operations.put(StatusPageOperation.RPC_NAME, new StatusPageOperation(master));
        operations.put(GetProtocolVersionOperation.RPC_NAME, new GetProtocolVersionOperation(master));
        operations.put(CreateVolumeOperation.RPC_NAME, new CreateVolumeOperation(master));
        operations.put(GetLocalVolumesOperation.RPC_NAME, new GetLocalVolumesOperation(master));
        operations.put(StatOperation.RPC_NAME, new StatOperation(master));
        operations.put(CheckAccessOperation.RPC_NAME, new CheckAccessOperation(master));
        operations.put(ReadDirOperation.RPC_NAME, new ReadDirOperation(master));
        operations.put(CreateFileOperation.RPC_NAME, new CreateFileOperation(master));
        operations.put(CreateDirOperation.RPC_NAME, new CreateDirOperation(master));
    }
    
    @Override
    protected void processMethod(StageMethod method) {
        switch (method.getStageMethod()) {
        case STAGEOP_PARSE_AND_EXECUTE:
            parseAndExecute(method);
            break;
        case STAGEOP_FINISH:
            finish(method);
            break;
        default:
            method.getRq().setError(
                new ErrorRecord(ErrorRecord.ErrorClass.INTERNAL_SERVER_ERROR,
                    "unknown stage operation"));
            master.requestFinished(method.getRq());
        }
    }
    
    /**
     * Parse request and execute method
     * 
     * @param method
     *            stagemethod to execute
     */
    private void parseAndExecute(StageMethod method) {
        final MRCRequest rq = method.getRq();
        final PinkyRequest theRequest = rq.getPinkyRequest();
        final String URI = theRequest.requestURI.startsWith("/") ? theRequest.requestURI
                .substring(1) : theRequest.requestURI;
        
        final MRCOperation op = operations.get(URI);
        if (op == null) {
            rq.setError(new ErrorRecord(ErrorRecord.ErrorClass.BAD_REQUEST,
                "BAD REQUEST: unknown operation '" + URI + "'"));
            master.requestFinished(rq);
            return;
        }
        
        // parse arguments, if necessary
        if (op.hasArguments()) {
            if ((theRequest.requestBody == null) || (theRequest.requestBody.capacity() == 0)) {
                rq.setError(new ErrorRecord(ErrorRecord.ErrorClass.BAD_REQUEST,
                    "BAD REQUEST: operation '" + URI + "' requires arguments"));
                master.requestFinished(rq);
                return;
            }
            List<Object> args = null;
            try {
                final JSONString jst = new JSONString(new String(theRequest.getBody(),
                    HTTPUtils.ENC_UTF8));
                Object o = JSONParser.parseJSON(jst);
                args = (List<Object>) o;
            } catch (ClassCastException ex) {
                rq.setError(new ErrorRecord(ErrorRecord.ErrorClass.BAD_REQUEST,
                    "BAD REQUEST: arguments must be JSON List"));
                master.requestFinished(rq);
                return;
            } catch (JSONException ex) {
                rq.setError(new ErrorRecord(ErrorRecord.ErrorClass.BAD_REQUEST,
                    "BAD REQUEST: body is not valid JSON: " + ex));
                master.requestFinished(rq);
                return;
            }
            op.parseRPCBody(rq, args);
        }
        
        if (op.isAuthRequired()) {
            try {
                
                // parse the user Id from the "AUTHORIZATION" header
                String authHeader = theRequest.requestHeaders
                        .getHeader(HTTPHeaders.HDR_AUTHORIZATION);
                
                if (authHeader == null)
                    throw new UserException(ErrNo.EPERM, "authorization mechanism required");
                
                UserCredentials cred = null;
                try {
                    cred = master.getAuthProvider().getEffectiveCredentials(authHeader,
                        theRequest.getChannelIO());
                    rq.getDetails().superUser = cred.isSuperUser();
                    rq.getDetails().groupIds = cred.getGroupIDs();
                    rq.getDetails().userId = cred.getUserID();
                } catch (AuthenticationException ex) {
                    throw new UserException(ErrNo.EPERM, ex.getMessage());
                }
                
            } catch (Exception exc) {
                
                method.getRq().setError(
                    new ErrorRecord(ErrorRecord.ErrorClass.BAD_REQUEST,
                        "could not initialize authentication module", exc));
                master.requestFinished(method.getRq());
                return;
            }
        }
        
        op.startRequest(rq);
    }
    
    private void finish(StageMethod method) {
        master.requestFinished(method.getRq());
    }
    
}
