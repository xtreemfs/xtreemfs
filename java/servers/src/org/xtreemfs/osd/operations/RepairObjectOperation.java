package org.xtreemfs.osd.operations;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.InvalidXLocationsException;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.ReplicationStage.FetchObjectCallback;
import org.xtreemfs.osd.storage.ObjectInformation;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_repair_objectRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

public class RepairObjectOperation extends OSDOperation {

    final String      sharedSecret;

    final ServiceUUID localUUID;

    public RepairObjectOperation(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_REPAIR_OBJECT;
    }

    @Override
    public void startRequest(OSDRequest rq) {
        final xtreemfs_repair_objectRequest args = (xtreemfs_repair_objectRequest) rq.getRequestArgs();

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Repair object request for file %s-%d", args.getFileId(),
                    args.getObjectNumber());
        }
        String replPolicy = rq.getLocationList().getReplicaUpdatePolicy();
          
        if (ReplicaUpdatePolicies.isRO(replPolicy)) {
        	repairROnlyObject(rq, args);
        } else if (ReplicaUpdatePolicies.isRW(replPolicy)) {
            repairRWObject(rq, args);
        } else {
            rq.sendError(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL,
                    "Invalid ReplicaUpdatePolicy: " + replPolicy);
        }
    }

    private void repairROnlyObject(final OSDRequest rq, final xtreemfs_repair_objectRequest args) {
    	if (rq.getLocationList().getLocalReplica().isComplete()) {
    		
    		//rest complete flag, otherwise replica is unable to fetch objects
            rq.getLocationList().getLocalReplica().resetCompleteFlagAndRestoreStrategyFlag();
    	}
    	//Assumption: fetched Object is not corrupted
		master.getReplicationStage().fetchObject(args.getFileId(), args.getObjectNumber(), rq.getLocationList(),
                rq.getCapability(), rq.getCowPolicy(), rq, new FetchObjectCallback() {

            @Override
            public void fetchComplete(ObjectInformation objectInfo, ErrorResponse error) {
                if (error == null) {
                    rq.sendSuccess(null, null);
                } else {
                    rq.sendError(error);
                }
            }
		});     
    }

    private void repairRWObject(final OSDRequest rq, final xtreemfs_repair_objectRequest args) {   	
    	//TODO(lukas) add rw support
    }

    @Override
    public void startInternalEvent(Object[] args) {
        // TODO Auto-generated method stub
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        try {
            xtreemfs_repair_objectRequest rpcrq = (xtreemfs_repair_objectRequest) rq.getRequestArgs();
            rq.setFileId(rpcrq.getFileId());
            rq.setCapability(new Capability(rpcrq.getFileCredentials().getXcap(), sharedSecret));
            rq.setLocationList(new XLocations(rpcrq.getFileCredentials().getXlocs(), localUUID));

            return null;
        } catch (InvalidXLocationsException ex) {
            return ErrorUtils.getErrorResponse(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_EINVAL, ex.toString());
        } catch (Throwable ex) {
            return ErrorUtils.getInternalServerError(ex);
        }
    }

    @Override
    public boolean requiresCapability() {
        return true;
    }
}
