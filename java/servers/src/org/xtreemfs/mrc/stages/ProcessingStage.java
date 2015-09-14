/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.stages;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.common.auth.AuthenticationException;
import org.xtreemfs.common.auth.UserCredentials;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.MessageType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.StatusPage;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseException.ExceptionType;
import org.xtreemfs.mrc.operations.AccessOperation;
import org.xtreemfs.mrc.operations.AddReplicaOperation;
import org.xtreemfs.mrc.operations.CheckFileListOperation;
import org.xtreemfs.mrc.operations.CheckpointOperation;
import org.xtreemfs.mrc.operations.CreateDirOperation;
import org.xtreemfs.mrc.operations.CreateLinkOperation;
import org.xtreemfs.mrc.operations.CreateSymLinkOperation;
import org.xtreemfs.mrc.operations.CreateVolumeOperation;
import org.xtreemfs.mrc.operations.DeleteOperation;
import org.xtreemfs.mrc.operations.DeleteVolumeOperation;
import org.xtreemfs.mrc.operations.DumpDBOperation;
import org.xtreemfs.mrc.operations.FSetAttrOperation;
import org.xtreemfs.mrc.operations.GetFileCredentialsOperation;
import org.xtreemfs.mrc.operations.GetLocalVolumesOperation;
import org.xtreemfs.mrc.operations.GetSuitableOSDsOperation;
import org.xtreemfs.mrc.operations.GetXAttrOperation;
import org.xtreemfs.mrc.operations.GetXAttrsOperation;
import org.xtreemfs.mrc.operations.GetXLocListOperation;
import org.xtreemfs.mrc.operations.GetXLocSetOperation;
import org.xtreemfs.mrc.operations.InternalDebugOperation;
import org.xtreemfs.mrc.operations.MRCOperation;
import org.xtreemfs.mrc.operations.MoveOperation;
import org.xtreemfs.mrc.operations.OpenOperation;
import org.xtreemfs.mrc.operations.ReadDirAndStatOperation;
import org.xtreemfs.mrc.operations.ReadLinkOperation;
import org.xtreemfs.mrc.operations.RemoveReplicaOperation;
import org.xtreemfs.mrc.operations.RemoveXAttrOperation;
import org.xtreemfs.mrc.operations.RenewOperation;
import org.xtreemfs.mrc.operations.ReselectOSDsOperation;
import org.xtreemfs.mrc.operations.RestoreDBOperation;
import org.xtreemfs.mrc.operations.RestoreFileOperation;
import org.xtreemfs.mrc.operations.SetReadOnlyXattrOperation;
import org.xtreemfs.mrc.operations.SetReplicaUpdatePolicyOperation;
import org.xtreemfs.mrc.operations.SetXAttrOperation;
import org.xtreemfs.mrc.operations.SetattrOperation;
import org.xtreemfs.mrc.operations.ShutdownOperation;
import org.xtreemfs.mrc.operations.StatFSOperation;
import org.xtreemfs.mrc.operations.StatOperation;
import org.xtreemfs.mrc.operations.TruncateOperation;
import org.xtreemfs.mrc.operations.UpdateFileSizeOperation;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceConstants;

import com.google.protobuf.Descriptors.FieldDescriptor;

/**
 * 
 * @author bjko
 */
public class ProcessingStage extends MRCStage {
    
    public static final int                  STAGEOP_PARSE_AND_EXECUTE = 1;

    public static final int                  STAGEOP_INTERNAL_CALLBACK = 2;

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
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_SHUTDOWN, new ShutdownOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_MKVOL, new CreateVolumeOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_RMVOL, new DeleteVolumeOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_LSVOL, new GetLocalVolumesOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_GETATTR, new StatOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_READDIR, new ReadDirAndStatOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_MKDIR, new CreateDirOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_SYMLINK, new CreateSymLinkOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_UNLINK, new DeleteOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_RMDIR, new DeleteOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_GETXATTR, new GetXAttrOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_LISTXATTR, new GetXAttrsOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_SETXATTR, new SetXAttrOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_REMOVEXATTR, new RemoveXAttrOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_OPEN, new OpenOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_RENEW_CAPABILITY, new RenewOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_REPLICA_ADD, new AddReplicaOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_REPLICA_REMOVE,
            new RemoveReplicaOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_REPLICA_LIST, new GetXLocListOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_RENAME, new MoveOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_LINK, new CreateLinkOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_STATVFS, new StatFSOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_READLINK, new ReadLinkOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_DUMP_DATABASE, new DumpDBOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_RESTORE_DATABASE, new RestoreDBOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_CHECK_FILE_EXISTS, new CheckFileListOperation(
            master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_RESTORE_FILE, new RestoreFileOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_CHECKPOINT, new CheckpointOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_SETATTR, new SetattrOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_FSETATTR, new FSetAttrOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_GET_SUITABLE_OSDS, new GetSuitableOSDsOperation(
            master));
        operations.put(MRCServiceConstants.PROC_ID_FTRUNCATE, new TruncateOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_INTERNAL_DEBUG,
            new InternalDebugOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_UPDATE_FILE_SIZE, new UpdateFileSizeOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_ACCESS, new AccessOperation(master));
        // TODO operations.put(replication_toMasterRequest.TAG, new
        // ReplicationToMasterOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_SET_REPLICA_UPDATE_POLICY, 
                new SetReplicaUpdatePolicyOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_SET_READ_ONLY_XATTR, new SetReadOnlyXattrOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_GET_FILE_CREDENTIALS, new GetFileCredentialsOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_GET_XLOCSET, new GetXLocSetOperation(master));
        operations.put(MRCServiceConstants.PROC_ID_XTREEMFS_RESELECT_OSDS, new ReselectOSDsOperation(master));
    }
    
    public Map<Integer, Integer> get_opCountMap() {
        return _opCountMap;
    }
    
//    public String getOpName(int opId) {
//        String opName = operations.get(opId).getClass().getSimpleName();
//        return (opName.charAt(0) + "").toLowerCase() + opName.substring(0, opName.length() - "Operation".length()).substring(1);
//    }
    
    @Override
    protected void processMethod(StageMethod method) {
        switch (method.getStageMethod()) {
        case STAGEOP_PARSE_AND_EXECUTE:
            parseAndExecute(method);
            break;

        default:
            method.getRq().setError(ErrorType.INTERNAL_SERVER_ERROR, "unknown stage operation");
            master.requestFinished(method.getRq());
        }
    }
    
    @Override
    protected void processInternalRequest(StageMethod method) {
        switch (method.getStageMethod()) {
        case STAGEOP_INTERNAL_CALLBACK:
            executeInternalCallback(method);
            break;
        default:
            Logging.logMessage(Logging.LEVEL_WARN, Category.stage, this,
                    "Unknown stage operation (%d) for an internal request.");
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
        final RPCServerRequest rpcRequest = rq.getRPCRequest();
        final RPCHeader header = rpcRequest.getHeader();
        final RPCHeader.RequestHeader rqHeader = header.getRequestHeader();
        
        if (header.getMessageType() != MessageType.RPC_REQUEST) {
            rq.setError(ErrorType.GARBAGE_ARGS, POSIXErrno.POSIX_ERROR_EIO,
                "expected RPC request message type but got " + header.getMessageType());
            return;
        }
        
        final MRCOperation op = operations.get(rqHeader.getProcId());
        
        if (op == null) {
            rq.setError(ErrorType.INVALID_PROC_ID, "requested operation (" + rqHeader.getProcId()
                + ") is not available on this MRC");
            master.requestFinished(rq);
            return;
        }
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "operation for request %s: %s", rq
                    .toString(), op.getClass().getSimpleName());
        
        if (statisticsEnabled) {
            _opCountMap.put(rqHeader.getProcId(), _opCountMap.get(rqHeader.getProcId()) + 1);
        }
        
        // parse request arguments
        ErrorRecord error = op.parseRequestArgs(rq);
        if (error != null) {
            rq.setError(error);
            master.requestFinished(rq);
            return;
        }
        
        try {
            
            // get the auth data if available
            Auth auth = header.getRequestHeader().hasAuthData() ? header.getRequestHeader().getAuthData()
                : null;
            
            // get the user credentials
            org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials ctx = op
                    .getUserCredentials(rq);
            
            if (ctx != null)
                try {
                    UserCredentials cred = master.getAuthProvider().getEffectiveCredentials(ctx,
                        rpcRequest.getConnection().getChannel());
                    rq.getDetails().superUser = cred.isSuperUser();
                    rq.getDetails().groupIds = cred.getGroupIDs();
                    rq.getDetails().userId = cred.getUserID();
                    rq.getDetails().auth = auth;
                    rq.getDetails().password = auth != null && auth.hasAuthPasswd() ? auth.getAuthPasswd()
                            .getPassword() : "";
                } catch (AuthenticationException ex) {
                    throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, ex.getMessage());
                }
            
        } catch (Exception exc) {
            
            method.getRq().setError(ErrorType.INTERNAL_SERVER_ERROR,
                "could not initialize authentication module", exc);
            master.requestFinished(method.getRq());
            return;
        }
        
        execute(op, method);

    }

    /**
     * Execute an operation
     * 
     * @param operation
     *            MRCOperation to execute
     * @param method
     *            StageMethod to serve as the context
     */
    private void execute(MRCOperation op, StageMethod method) {
        final MRCRequest rq = method.getRq();
        final RPCServerRequest rpcRequest = rq.getRPCRequest();
        final RPCHeader header = rpcRequest.getHeader();
        final RPCHeader.RequestHeader rqHeader = header.getRequestHeader();

        try {
            
            if (Logging.isDebug()) {
                
                StringBuffer params = new StringBuffer();
                Map<FieldDescriptor, Object> fieldMap = rq.getRequestArgs() == null ? null : rq
                        .getRequestArgs().getAllFields();
                if (fieldMap != null) {
                    int i = 0;
                    for (Entry<FieldDescriptor, Object> entry : fieldMap.entrySet()) {
                        params.append(entry.getKey().getName() + "='" + entry.getValue()
                            + (i == fieldMap.size() - 1 ? "'" : "', "));
                        i++;
                    }
                }
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "parsed request: %s (%s)\n",
                        StatusPage.getOpName(rqHeader.getProcId()), params.toString());
            }
            
            op.startRequest(rq);
            
        } catch (UserException exc) {
            reportUserError(op, rq, exc, exc.getErrno());
            
        } catch (MRCException exc) {
            Throwable cause = exc.getCause();
            if (cause instanceof DatabaseException
                && ((DatabaseException) cause).getType() == ExceptionType.NOT_ALLOWED)
                reportUserError(op, rq, exc, POSIXErrno.POSIX_ERROR_EPERM);
            else
                reportServerError(op, rq, exc);
            
        } catch (DatabaseException exc) {
            if (exc.getType() == ExceptionType.NOT_ALLOWED) {
                reportUserError(op, rq, exc, POSIXErrno.POSIX_ERROR_EPERM);
            } else if (exc.getType() == ExceptionType.REDIRECT) {
                try {
                    redirect(rq, exc.getAttachment() != null? (String) exc.getAttachment(): master.getReplMasterUUID());
                } catch (MRCException e) {
                    reportServerError(op, rq, e);
                }
            } else
                reportServerError(op, rq, exc);
            
        } catch (Throwable exc) {
            reportServerError(op, rq, exc);
        }
    }
    
    /**
     * Enqueue an internal callback operation.<br>
     * Internal callbacks can be used to execute something in the context of the ProcessingStage. This is needed to
     * ensure database calls are exclusive and always from the same process.
     * 
     * @param rq
     *            The internal callback request.
     */
    void enqueueInternalCallbackOperation(InternalCallbackInterface callback) {
        Object[] args = new Object[] { callback };
        MRCInternalRequest rq = new MRCInternalRequest(args);
        
        q.add(new StageMethod(rq, ProcessingStage.STAGEOP_INTERNAL_CALLBACK, null));
    }

    /**
     * Execute an internal callback operation.
     * 
     * 
     * @param method
     *            with an RPCRequest of type {@link InternalCallbackMRCRequest}.
     */
    private void executeInternalCallback(StageMethod method) {

        MRCInternalRequest rq = method.getInternalRequest();
        Object[] args = rq.getArgs();
        
        if (args.length < 1 || !(args[0] instanceof InternalCallbackInterface)) {
            Logging.logMessage(Logging.LEVEL_WARN, this, "Internal callback called without a callback as an argument.");
            return;
        }

        InternalCallbackInterface callback = (InternalCallbackInterface) args[0];
        try {
            callback.execute();
        } catch (Throwable e) {
            Logging.logMessage(Logging.LEVEL_INFO, this, "Internal callback failed with an exception");
            Logging.logError(Logging.LEVEL_INFO, this, e);
        }
    }


    private void reportUserError(MRCOperation op, MRCRequest rq, Throwable exc, POSIXErrno errno) {
        if (Logging.isDebug())
            Logging.logUserError(Logging.LEVEL_DEBUG, Category.proc, this, exc);
        op.finishRequest(rq, new ErrorRecord(ErrorType.ERRNO, errno, exc.getMessage(), exc));
    }
    
    private void reportServerError(MRCOperation op, MRCRequest rq, Throwable exc) {
        if (Logging.isDebug())
            Logging.logUserError(Logging.LEVEL_DEBUG, Category.proc, this, exc);
        op.finishRequest(rq, new ErrorRecord(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE,
            "An error has occurred at the MRC. Details: " + exc.getMessage(), exc));
    }
    
    private void redirect(MRCRequest rq, String uuid) {
        rq.getRPCRequest().sendRedirect(uuid);
    }
    
}
