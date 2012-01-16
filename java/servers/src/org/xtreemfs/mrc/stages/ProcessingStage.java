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
import org.xtreemfs.common.olp.OLPStageRequest;
import org.xtreemfs.common.olp.OverloadProtectedStage;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;
import org.xtreemfs.foundation.pbrpc.server.RPCServerRequest;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.mrc.MRCException;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
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
import org.xtreemfs.mrc.operations.ReadLinkOperation;
import org.xtreemfs.mrc.operations.RemoveReplicaOperation;
import org.xtreemfs.mrc.operations.RemoveXAttrOperation;
import org.xtreemfs.mrc.operations.RenewOperation;
import org.xtreemfs.mrc.operations.RestoreDBOperation;
import org.xtreemfs.mrc.operations.RestoreFileOperation;
import org.xtreemfs.mrc.operations.SetReadOnlyXattrOperation;
import org.xtreemfs.mrc.operations.SetReplicaUpdatePolicyOperation;
import org.xtreemfs.mrc.operations.SetXAttrOperation;
import org.xtreemfs.mrc.operations.SetattrOperation;
import org.xtreemfs.mrc.operations.ShutdownOperation;
import org.xtreemfs.mrc.operations.GetFileCredentialsOperation;
import org.xtreemfs.mrc.operations.StatFSOperation;
import org.xtreemfs.mrc.operations.StatOperation;
import org.xtreemfs.mrc.operations.StatusPageOperation;
import org.xtreemfs.mrc.operations.TruncateOperation;
import org.xtreemfs.mrc.operations.UpdateFileSizeOperation;

import com.google.protobuf.Descriptors.FieldDescriptor;

import static org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceConstants.*;

/**
 * 
 * @author bjko
 */
public class ProcessingStage extends OverloadProtectedStage<MRCRequest> {
    
    private final static int                 NUM_RQ_TYPES              = 38;
    private final static int                 NUM_INTERNAL_RQ_TYPES     = 0;
    private final static int                 STAGE_ID                  = 1;
    private final static int                 NUM_SUB_SEQ_STAGES        = 1;
    
    private final MRCRequestDispatcher       master;
    
    private final Map<Integer, MRCOperation> operations;
    
    private final Map<Integer, Integer>      _opCountMap;
    
    private final boolean                    statisticsEnabled         = true;
    
    public final Map<Integer, Integer>       requestTypeMap = new HashMap<Integer, Integer>(NUM_RQ_TYPES);
    
    public ProcessingStage(MRCRequestDispatcher master) {
        super("ProcSt", STAGE_ID, NUM_RQ_TYPES, NUM_INTERNAL_RQ_TYPES, NUM_SUB_SEQ_STAGES, 0L, true);
        this.master = master;
        
        operations = new HashMap<Integer, MRCOperation>();
        installOperations();
        
        if (statisticsEnabled) {
            
            // initialize operations counter
            _opCountMap = new HashMap<Integer, Integer>();
            for (Integer i : operations.keySet()) {
                _opCountMap.put(i, 0);
            }
        }
    }
    
    private void installOperations() {
        int type = 0;
        operations.put(PROC_ID_XTREEMFS_SET_READ_ONLY_XATTR, new SetReadOnlyXattrOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_SET_READ_ONLY_XATTR, type++);
        operations.put(PROC_ID_XTREEMFS_SET_REPLICA_UPDATE_POLICY, new SetReplicaUpdatePolicyOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_SET_REPLICA_UPDATE_POLICY, type++);
        operations.put(PROC_ID_XTREEMFS_UPDATE_FILE_SIZE, new UpdateFileSizeOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_UPDATE_FILE_SIZE, type++);
        operations.put(PROC_ID_FTRUNCATE, new TruncateOperation(master));
        requestTypeMap.put(PROC_ID_FTRUNCATE, type++);
        operations.put(PROC_ID_FSETATTR, new FSetAttrOperation(master));
        requestTypeMap.put(PROC_ID_FSETATTR, type++);
        operations.put(PROC_ID_READDIR, new ReadDirAndStatOperation(master));
        requestTypeMap.put(PROC_ID_READDIR, type++);
        operations.put(PROC_ID_MKDIR, new CreateDirOperation(master));
        requestTypeMap.put(PROC_ID_MKDIR, type++);
        operations.put(PROC_ID_SYMLINK, new CreateSymLinkOperation(master));
        requestTypeMap.put(PROC_ID_SYMLINK, type++);
        operations.put(PROC_ID_UNLINK, new DeleteOperation(master));
        requestTypeMap.put(PROC_ID_UNLINK, type);
        operations.put(PROC_ID_RMDIR, new DeleteOperation(master));
        requestTypeMap.put(PROC_ID_RMDIR, type++);
        operations.put(PROC_ID_SETATTR, new SetattrOperation(master));
        requestTypeMap.put(PROC_ID_SETATTR, type++);
        operations.put(PROC_ID_XTREEMFS_RESTORE_FILE, new RestoreFileOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_RESTORE_FILE, type++);
        operations.put(PROC_ID_SETXATTR, new SetXAttrOperation(master));
        requestTypeMap.put(PROC_ID_SETXATTR, type++);
        operations.put(PROC_ID_REMOVEXATTR, new RemoveXAttrOperation(master));
        requestTypeMap.put(PROC_ID_REMOVEXATTR, type++);
        operations.put(PROC_ID_OPEN, new OpenOperation(master));
        requestTypeMap.put(PROC_ID_OPEN, type++);
        operations.put(PROC_ID_RENAME, new MoveOperation(master));
        requestTypeMap.put(PROC_ID_RENAME, type++);
        operations.put(PROC_ID_XTREEMFS_REPLICA_ADD, new AddReplicaOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_REPLICA_ADD, type++);
        operations.put(PROC_ID_XTREEMFS_REPLICA_REMOVE, new RemoveReplicaOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_REPLICA_REMOVE, type++);
        operations.put(PROC_ID_LINK, new CreateLinkOperation(master));
        requestTypeMap.put(PROC_ID_LINK, type++);
        operations.put(PROC_ID_XTREEMFS_RESTORE_DATABASE, new RestoreDBOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_RESTORE_DATABASE, type++);
        operations.put(PROC_ID_XTREEMFS_REPLICA_LIST, new GetXLocListOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_REPLICA_LIST, type++);
        operations.put(PROC_ID_STATVFS, new StatFSOperation(master));
        requestTypeMap.put(PROC_ID_STATVFS, type++);
        operations.put(PROC_ID_READLINK, new ReadLinkOperation(master));
        requestTypeMap.put(PROC_ID_READLINK, type++);
        operations.put(PROC_ID_XTREEMFS_DUMP_DATABASE, new DumpDBOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_DUMP_DATABASE, type++);
        operations.put(PROC_ID_XTREEMFS_RENEW_CAPABILITY, new RenewOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_RENEW_CAPABILITY, type++);
        operations.put(PROC_ID_XTREEMFS_CHECK_FILE_EXISTS, new CheckFileListOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_CHECK_FILE_EXISTS, type++);
        operations.put(PROC_ID_LISTXATTR, new GetXAttrsOperation(master));
        requestTypeMap.put(PROC_ID_LISTXATTR, type++);
        operations.put(PROC_ID_XTREEMFS_CHECKPOINT, new CheckpointOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_CHECKPOINT, type++);
        operations.put(PROC_ID_GETXATTR, new GetXAttrOperation(master));
        requestTypeMap.put(PROC_ID_GETXATTR, type++);
        operations.put(PROC_ID_GETATTR, new StatOperation(master));
        requestTypeMap.put(PROC_ID_GETATTR, type++);
        operations.put(PROC_ID_XTREEMFS_GET_SUITABLE_OSDS, new GetSuitableOSDsOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_GET_SUITABLE_OSDS, type++);
        operations.put(PROC_ID_XTREEMFS_LSVOL, new GetLocalVolumesOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_LSVOL, type++);
        operations.put(PROC_ID_XTREEMFS_INTERNAL_DEBUG, new InternalDebugOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_INTERNAL_DEBUG, type++);
        operations.put(PROC_ID_XTREEMFS_RMVOL, new DeleteVolumeOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_RMVOL, type++);
        operations.put(PROC_ID_ACCESS, new AccessOperation(master));
        requestTypeMap.put(PROC_ID_ACCESS, type++);
        operations.put(PROC_ID_XTREEMFS_MKVOL, new CreateVolumeOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_MKVOL, type++);
        operations.put(PROC_ID_XTREEMFS_SHUTDOWN, new ShutdownOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_SHUTDOWN, type++);
        operations.put(PROC_ID_XTREEMFS_GET_FILE_CREDENTIALS, new GetFileCredentialsOperation(master));
        requestTypeMap.put(PROC_ID_XTREEMFS_GET_FILE_CREDENTIALS, type++);
    }
    
    public Map<Integer, Integer> get_opCountMap() {
        return _opCountMap;
    }

    /* (non-Javadoc)
     * @see org.xtreemfs.common.olp.OverloadProtectedStage#_processMethod(org.xtreemfs.common.olp.OLPStageRequest)
     */
    @Override
    protected boolean _processMethod(OLPStageRequest<MRCRequest> stageRequest) {
        
        final MRCRequest rq = stageRequest.getRequest();
        final RPCServerRequest rpcRequest = rq.getRPCRequest();
        final RPCHeader.RequestHeader rqHeader = rpcRequest.getHeader().getRequestHeader();
        final MRCOperation op = operations.get(stageRequest.getStageMethod());
        final RPCRequestCallback callback = (RPCRequestCallback) stageRequest.getCallback();
        
        if (op == null) {
            
            stageRequest.voidMeasurments();
            callback.failed(ErrorType.INVALID_PROC_ID, POSIXErrno.POSIX_ERROR_NONE, "requested operation (" + 
                    rqHeader.getProcId() + ") is not available on this MRC");
            return true;
        }
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "operation for request %s: %s", rq
                    .toString(), op.getClass().getSimpleName());
        }
        
        if (statisticsEnabled) {
            _opCountMap.put(rqHeader.getProcId(), _opCountMap.get(rqHeader.getProcId()) + 1);
        }
        
        // parse request arguments
        try {
            op.parseRequestArgs(rq);
        } catch (Exception e) {
            
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this,
                    "could not parse request arguments:");
                Logging.logUserError(Logging.LEVEL_DEBUG, Category.stage, this, e);
            }
            stageRequest.voidMeasurments();
            callback.failed(ErrorType.GARBAGE_ARGS, POSIXErrno.POSIX_ERROR_EINVAL, e);
            return true;
        }
        
        try {
            
            // get the auth data if available
            Auth auth = rqHeader.hasAuthData() ? rqHeader.getAuthData() : null;
            
            // get the user credentials
            org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials ctx = op
                    .getUserCredentials(rq);
            
            if (ctx != null) {
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
            }
        } catch (Exception exc) {
            
            stageRequest.voidMeasurments();
            callback.failed(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_EIO, 
                    "could not initialize authentication module");
            return true;
        }
        
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
                    StatusPageOperation.getOpName(rqHeader.getProcId()), params.toString());
            }
            
            op.startRequest(rq, callback);
            
        } catch (UserException exc) {
            
            stageRequest.voidMeasurments();
            callback.failed(ErrorType.ERRNO, exc.getErrno(), exc);
        } catch (MRCException exc) {
            
            stageRequest.voidMeasurments();
            Throwable cause = exc.getCause();
            if (cause instanceof DatabaseException 
                && ((DatabaseException) cause).getType() == ExceptionType.NOT_ALLOWED) {
                callback.failed(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EPERM, exc);
            } else {
                callback.failed(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, "an error has occurred", 
                        OutputUtils.stackTraceToString(exc));
            }
        } catch (DatabaseException exc) {
            
            stageRequest.voidMeasurments();
            if (exc.getType() == ExceptionType.NOT_ALLOWED) {
                callback.failed(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EPERM, exc);
            } else if (exc.getType() == ExceptionType.REDIRECT) {
                try {
                    callback.redirect(
                            exc.getAttachment() != null? (String) exc.getAttachment(): master.getReplMasterUUID());
                } catch (MRCException e) {
                    callback.failed(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, 
                            "an error has occurred", OutputUtils.stackTraceToString(e));
                }
            } else {
                callback.failed(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, "an error has occurred", 
                        OutputUtils.stackTraceToString(exc));
            }
        } catch (Throwable exc) {
            
            stageRequest.voidMeasurments();
            callback.failed(ErrorType.INTERNAL_SERVER_ERROR, POSIXErrno.POSIX_ERROR_NONE, "an error has occurred", 
                    OutputUtils.stackTraceToString(exc));
        }
        
        return true;
    }
}