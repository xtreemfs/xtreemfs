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
import org.xtreemfs.new_mrc.ErrNo;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.UserException;
import org.xtreemfs.new_mrc.operations.AddReplicaOperation;
import org.xtreemfs.new_mrc.operations.ChangeAccessModeOperation;
import org.xtreemfs.new_mrc.operations.ChangeOwnerOperation;
import org.xtreemfs.new_mrc.operations.CheckAccessOperation;
import org.xtreemfs.new_mrc.operations.CreateDirOperation;
import org.xtreemfs.new_mrc.operations.CreateFileOperation;
import org.xtreemfs.new_mrc.operations.CreateLinkOperation;
import org.xtreemfs.new_mrc.operations.CreateSymLinkOperation;
import org.xtreemfs.new_mrc.operations.CreateVolumeOperation;
import org.xtreemfs.new_mrc.operations.DeleteOperation;
import org.xtreemfs.new_mrc.operations.DeleteVolumeOperation;
import org.xtreemfs.new_mrc.operations.DumpDBOperation;
import org.xtreemfs.new_mrc.operations.GetLocalVolumesOperation;
import org.xtreemfs.new_mrc.operations.GetProtocolVersionOperation;
import org.xtreemfs.new_mrc.operations.GetW32AttrsOperation;
import org.xtreemfs.new_mrc.operations.GetXAttrOperation;
import org.xtreemfs.new_mrc.operations.MRCOperation;
import org.xtreemfs.new_mrc.operations.MoveOperation;
import org.xtreemfs.new_mrc.operations.OpenOperation;
import org.xtreemfs.new_mrc.operations.ReadDirAndStatOperation;
import org.xtreemfs.new_mrc.operations.ReadDirOperation;
import org.xtreemfs.new_mrc.operations.RemoveACLEntriesOperation;
import org.xtreemfs.new_mrc.operations.RemoveReplicaOperation;
import org.xtreemfs.new_mrc.operations.RenewOperation;
import org.xtreemfs.new_mrc.operations.RestoreDBOperation;
import org.xtreemfs.new_mrc.operations.SetACLEntriesOperation;
import org.xtreemfs.new_mrc.operations.SetW32AttrsOperation;
import org.xtreemfs.new_mrc.operations.SetXAttrsOperation;
import org.xtreemfs.new_mrc.operations.ShutdownOperation;
import org.xtreemfs.new_mrc.operations.StatFSOperation;
import org.xtreemfs.new_mrc.operations.StatOperation;
import org.xtreemfs.new_mrc.operations.StatusPageOperation;
import org.xtreemfs.new_mrc.operations.UpdateFileSizeOperation;
import org.xtreemfs.new_mrc.operations.UtimeOperation;

/**
 * 
 * @author bjko
 */
public class ProcessingStage extends MRCStage {
    
    public static final int                 STAGEOP_PARSE_AND_EXECUTE = 1;
    
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
        operations.put(GetProtocolVersionOperation.RPC_NAME,
            new GetProtocolVersionOperation(master));
        operations.put(CreateVolumeOperation.RPC_NAME, new CreateVolumeOperation(master));
        operations.put(DeleteVolumeOperation.RPC_NAME, new DeleteVolumeOperation(master));
        operations.put(GetLocalVolumesOperation.RPC_NAME, new GetLocalVolumesOperation(master));
        operations.put(StatOperation.RPC_NAME, new StatOperation(master));
        operations.put(CheckAccessOperation.RPC_NAME, new CheckAccessOperation(master));
        operations.put(ReadDirOperation.RPC_NAME, new ReadDirOperation(master));
        operations.put(ReadDirAndStatOperation.RPC_NAME, new ReadDirAndStatOperation(master));
        operations.put(CreateFileOperation.RPC_NAME, new CreateFileOperation(master));
        operations.put(CreateDirOperation.RPC_NAME, new CreateDirOperation(master));
        operations.put(CreateSymLinkOperation.RPC_NAME, new CreateSymLinkOperation(master));
        operations.put(DeleteOperation.RPC_NAME, new DeleteOperation(master));
        operations.put(GetXAttrOperation.RPC_NAME, new GetXAttrOperation(master));
        operations.put(SetXAttrsOperation.RPC_NAME, new SetXAttrsOperation(master));
        operations.put(OpenOperation.RPC_NAME, new OpenOperation(master));
        operations.put(UpdateFileSizeOperation.RPC_NAME, new UpdateFileSizeOperation(master));
        operations.put(RenewOperation.RPC_NAME, new RenewOperation(master));
        operations.put(ChangeOwnerOperation.RPC_NAME, new ChangeOwnerOperation(master));
        operations.put(ChangeAccessModeOperation.RPC_NAME, new ChangeAccessModeOperation(master));
        operations.put(AddReplicaOperation.RPC_NAME, new AddReplicaOperation(master));
        operations.put(RemoveReplicaOperation.RPC_NAME, new RemoveReplicaOperation(master));
        operations.put(MoveOperation.RPC_NAME, new MoveOperation(master));
        operations.put(CreateLinkOperation.RPC_NAME, new CreateLinkOperation(master));
        operations.put(StatFSOperation.RPC_NAME, new StatFSOperation(master));
        operations.put(UtimeOperation.RPC_NAME, new UtimeOperation(master));
        operations.put(SetW32AttrsOperation.RPC_NAME, new SetW32AttrsOperation(master));
        operations.put(GetW32AttrsOperation.RPC_NAME, new GetW32AttrsOperation(master));
        operations.put(SetACLEntriesOperation.RPC_NAME, new SetACLEntriesOperation(master));
        operations.put(RemoveACLEntriesOperation.RPC_NAME, new RemoveACLEntriesOperation(master));
        operations.put(DumpDBOperation.RPC_NAME, new DumpDBOperation(master));
        operations.put(RestoreDBOperation.RPC_NAME, new RestoreDBOperation(master));
    }
    
    @Override
    protected void processMethod(StageMethod method) {
        switch (method.getStageMethod()) {
        case STAGEOP_PARSE_AND_EXECUTE:
            parseAndExecute(method);
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
    
}
