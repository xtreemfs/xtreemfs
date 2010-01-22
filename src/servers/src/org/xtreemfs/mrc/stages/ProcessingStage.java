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

package org.xtreemfs.mrc.stages;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.common.auth.AuthenticationException;
import org.xtreemfs.common.auth.UserCredentials;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.interfaces.MRCInterface.closeRequest;
import org.xtreemfs.interfaces.MRCInterface.fsetattrRequest;
import org.xtreemfs.interfaces.MRCInterface.ftruncateRequest;
import org.xtreemfs.interfaces.MRCInterface.getattrRequest;
import org.xtreemfs.interfaces.MRCInterface.getxattrRequest;
import org.xtreemfs.interfaces.MRCInterface.linkRequest;
import org.xtreemfs.interfaces.MRCInterface.listxattrRequest;
import org.xtreemfs.interfaces.MRCInterface.mkdirRequest;
import org.xtreemfs.interfaces.MRCInterface.openRequest;
import org.xtreemfs.interfaces.MRCInterface.readdirRequest;
import org.xtreemfs.interfaces.MRCInterface.readlinkRequest;
import org.xtreemfs.interfaces.MRCInterface.removexattrRequest;
import org.xtreemfs.interfaces.MRCInterface.renameRequest;
import org.xtreemfs.interfaces.MRCInterface.rmdirRequest;
import org.xtreemfs.interfaces.MRCInterface.setattrRequest;
import org.xtreemfs.interfaces.MRCInterface.setxattrRequest;
import org.xtreemfs.interfaces.MRCInterface.statvfsRequest;
import org.xtreemfs.interfaces.MRCInterface.symlinkRequest;
import org.xtreemfs.interfaces.MRCInterface.unlinkRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_check_file_existsRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_checkpointRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_dump_databaseRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_get_suitable_osdsRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_internal_debugRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_listdirRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_lsvolRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_mkvolRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_renew_capabilityRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_addRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_listRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_replica_removeRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_restore_databaseRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_restore_fileRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_rmvolRequest;
import org.xtreemfs.interfaces.MRCInterface.xtreemfs_shutdownRequest;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.operations.AddReplicaOperation;
import org.xtreemfs.mrc.operations.CheckFileListOperation;
import org.xtreemfs.mrc.operations.CheckpointOperation;
import org.xtreemfs.mrc.operations.CloseOperation;
import org.xtreemfs.mrc.operations.CreateDirOperation;
import org.xtreemfs.mrc.operations.CreateLinkOperation;
import org.xtreemfs.mrc.operations.CreateSymLinkOperation;
import org.xtreemfs.mrc.operations.CreateVolumeOperation;
import org.xtreemfs.mrc.operations.DeleteOperation;
import org.xtreemfs.mrc.operations.DeleteVolumeOperation;
import org.xtreemfs.mrc.operations.DumpDBOperation;
import org.xtreemfs.mrc.operations.FSetAttrOperation;
import org.xtreemfs.mrc.operations.GetLocalVolumesOperation;
import org.xtreemfs.mrc.operations.GetSuitableOSDsOperation;
import org.xtreemfs.mrc.operations.GetXAttrOperation;
import org.xtreemfs.mrc.operations.GetXAttrsOperation;
import org.xtreemfs.mrc.operations.GetXLocListOperation;
import org.xtreemfs.mrc.operations.InternalDebugOperation;
import org.xtreemfs.mrc.operations.MRCOperation;
import org.xtreemfs.mrc.operations.MoveOperation;
import org.xtreemfs.mrc.operations.OpenOperation;
import org.xtreemfs.mrc.operations.ReadDirAndStatOperation;
import org.xtreemfs.mrc.operations.ReadDirOperation;
import org.xtreemfs.mrc.operations.ReadLinkOperation;
import org.xtreemfs.mrc.operations.RemoveReplicaOperation;
import org.xtreemfs.mrc.operations.RemoveXAttrOperation;
import org.xtreemfs.mrc.operations.RenewOperation;
import org.xtreemfs.mrc.operations.RestoreDBOperation;
import org.xtreemfs.mrc.operations.RestoreFileOperation;
import org.xtreemfs.mrc.operations.SetXAttrOperation;
import org.xtreemfs.mrc.operations.SetattrOperation;
import org.xtreemfs.mrc.operations.ShutdownOperation;
import org.xtreemfs.mrc.operations.StatFSOperation;
import org.xtreemfs.mrc.operations.StatOperation;
import org.xtreemfs.mrc.operations.TruncateOperation;

/**
 * 
 * @author bjko
 */
public class ProcessingStage extends MRCStage {
    
    public static final int                  STAGEOP_PARSE_AND_EXECUTE = 1;
    
    private final MRCRequestDispatcher       master;
    
    private final Map<Integer, MRCOperation> operations;
    
    private final Map<Integer, Integer>      _opCountMap;
    
    private final boolean                    statisticsEnabled         = true;
    
    public ProcessingStage(MRCRequestDispatcher master) {
        super("ProcSt");
        this.master = master;
        
        operations = new HashMap<Integer, MRCOperation>();
        installOperations();
        
        if (statisticsEnabled) {
            // initialize operations counter
            _opCountMap = new HashMap<Integer, Integer>();
            for (Integer i : operations.keySet())
                _opCountMap.put(i, 0);
        }
    }
    
    public void installOperations() {
        operations.put(xtreemfs_shutdownRequest.TAG, new ShutdownOperation(master));
        operations.put(xtreemfs_mkvolRequest.TAG, new CreateVolumeOperation(master));
        operations.put(xtreemfs_rmvolRequest.TAG, new DeleteVolumeOperation(master));
        operations.put(xtreemfs_lsvolRequest.TAG, new GetLocalVolumesOperation(master));
        operations.put(getattrRequest.TAG, new StatOperation(master));
        operations.put(readdirRequest.TAG, new ReadDirAndStatOperation(master));
        operations.put(xtreemfs_listdirRequest.TAG, new ReadDirOperation(master));
        operations.put(mkdirRequest.TAG, new CreateDirOperation(master));
        operations.put(symlinkRequest.TAG, new CreateSymLinkOperation(master));
        operations.put(unlinkRequest.TAG, new DeleteOperation(master));
        operations.put(rmdirRequest.TAG, new DeleteOperation(master));
        operations.put(getxattrRequest.TAG, new GetXAttrOperation(master));
        operations.put(listxattrRequest.TAG, new GetXAttrsOperation(master));
        operations.put(setxattrRequest.TAG, new SetXAttrOperation(master));
        operations.put(removexattrRequest.TAG, new RemoveXAttrOperation(master));
        operations.put(openRequest.TAG, new OpenOperation(master));
        operations.put(xtreemfs_renew_capabilityRequest.TAG, new RenewOperation(master));
        operations.put(xtreemfs_replica_addRequest.TAG, new AddReplicaOperation(master));
        operations.put(xtreemfs_replica_removeRequest.TAG, new RemoveReplicaOperation(master));
        operations.put(renameRequest.TAG, new MoveOperation(master));
        operations.put(linkRequest.TAG, new CreateLinkOperation(master));
        operations.put(statvfsRequest.TAG, new StatFSOperation(master));
        operations.put(readlinkRequest.TAG, new ReadLinkOperation(master));
        // operations.put(-1, new SetACLEntriesOperation(master));
        // operations.put(-1, new RemoveACLEntriesOperation(master));
        operations.put(xtreemfs_dump_databaseRequest.TAG, new DumpDBOperation(master));
        operations.put(xtreemfs_restore_databaseRequest.TAG, new RestoreDBOperation(master));
        operations.put(xtreemfs_check_file_existsRequest.TAG, new CheckFileListOperation(master));
        operations.put(xtreemfs_restore_fileRequest.TAG, new RestoreFileOperation(master));
        operations.put(xtreemfs_checkpointRequest.TAG, new CheckpointOperation(master));
        operations.put(setattrRequest.TAG, new SetattrOperation(master));
        operations.put(fsetattrRequest.TAG, new FSetAttrOperation(master));
        operations.put(xtreemfs_get_suitable_osdsRequest.TAG, new GetSuitableOSDsOperation(master));
        operations.put(ftruncateRequest.TAG, new TruncateOperation(master));
        operations.put(xtreemfs_internal_debugRequest.TAG, new InternalDebugOperation(master));
        operations.put(xtreemfs_replica_listRequest.TAG, new GetXLocListOperation(master));
        operations.put(closeRequest.TAG, new CloseOperation(master));
      //TODO  operations.put(replication_toMasterRequest.TAG, new ReplicationToMasterOperation(master));
    }
    
    public Map<Integer, Integer> get_opCountMap() {
        return _opCountMap;
    }
    
    @Override
    protected void processMethod(StageMethod method) {
        switch (method.getStageMethod()) {
        case STAGEOP_PARSE_AND_EXECUTE:
            parseAndExecute(method);
            break;
        default:
            method.getRq().setError(
                new ErrorRecord(ErrorRecord.ErrorClass.INTERNAL_SERVER_ERROR, "unknown stage operation"));
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
        final ONCRPCRequest rpcRequest = rq.getRPCRequest();
        
        final MRCOperation op = operations.get(rpcRequest.getRequestHeader().getTag());
        
        if (op == null) {
            rq.setError(new ErrorRecord(ErrorClass.UNKNOWN_OPERATION, "requested operation ("
                + rpcRequest.getRequestHeader().getTag() + ") is not available on this MRC"));
            master.requestFinished(rq);
            return;
        }
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "operation for request %s: %s", rq
                    .toString(), op.getClass().getSimpleName());
        
        if (statisticsEnabled) {
            _opCountMap.put(rpcRequest.getRequestHeader().getTag(), _opCountMap.get(rpcRequest
                    .getRequestHeader().getTag()) + 1);
        }
        
        // parse request arguments
        ErrorRecord error = op.parseRequestArgs(rq);
        if (error != null) {
            rq.setError(error);
            master.requestFinished(rq);
            return;
        }
        
        try {
            
            // get the context
            org.xtreemfs.interfaces.UserCredentials ctx = op.getUserCredentials(rq);
            
            if (ctx != null)
                try {
                    UserCredentials cred = master.getAuthProvider().getEffectiveCredentials(ctx,
                        rpcRequest.getChannel());
                    rq.getDetails().superUser = cred.isSuperUser();
                    rq.getDetails().groupIds = cred.getGroupIDs();
                    rq.getDetails().userId = cred.getUserID();
                    rq.getDetails().password = ctx.getPassword();
                } catch (AuthenticationException ex) {
                    throw new UserException(ErrNo.EPERM, ex.getMessage());
                }
            
        } catch (Exception exc) {
            
            method.getRq().setError(
                new ErrorRecord(ErrorRecord.ErrorClass.INTERNAL_SERVER_ERROR,
                    "could not initialize authentication module", exc));
            master.requestFinished(method.getRq());
            return;
        }
        
        try {
            op.startRequest(rq);
            
        } catch (UserException exc) {
            reportUserError(op, rq, exc, exc.getErrno());
            
        } catch (MRCException exc) {
            Throwable cause = exc.getCause();
            if (cause instanceof DatabaseException
                && ((DatabaseException) cause).getType() == ExceptionType.NOT_ALLOWED)
                reportUserError(op, rq, exc, ErrNo.EPERM);
            else
                reportServerError(op, rq, exc);
            
        } catch (DatabaseException exc) {
            if (exc.getType() == ExceptionType.NOT_ALLOWED) {
                reportUserError(op, rq, exc, ErrNo.EPERM);
            } else if (exc.getType() == ExceptionType.REDIRECT) {
                rq.sendRedirectException((InetSocketAddress) exc.getAttachment());
            } else
                reportServerError(op, rq, exc);
            
        } catch (Throwable exc) {
            reportServerError(op, rq, exc);
        }
    }
    
    private void reportUserError(MRCOperation op, MRCRequest rq, Throwable exc, int errno) {
        if (Logging.isDebug())
            Logging.logUserError(Logging.LEVEL_DEBUG, Category.proc, this, exc);
        op.finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, errno, exc.getMessage(), exc));
    }
    
    private void reportServerError(MRCOperation op, MRCRequest rq, Throwable exc) {
        if (Logging.isDebug())
            Logging.logUserError(Logging.LEVEL_DEBUG, Category.proc, this, exc);
        op.finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR, "an error has occurred", exc));
    }
    
}
