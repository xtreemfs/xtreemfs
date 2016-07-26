/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;


import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_broadcast_gmaxRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/**
 *
 * <br>15.06.2009
 */
public class EventGmax extends OSDOperation {

    final String sharedSecret;

    final ServiceUUID localUUID;

    public EventGmax(OSDRequestDispatcher master) {
        super(master);
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_BROADCAST_GMAX;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_broadcast_gmaxRequest args = (xtreemfs_broadcast_gmaxRequest) rq
                .getRequestArgs();
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this,
                "received GMAX packet for: %s from %s", rq.getFileId(), rq.getRPCRequest().getSenderAddress());

        master.getStorageStage().receivedGMAX_ASYNC(args.getFileId(), args.getTruncateEpoch(), args.getLastObject());

    }


    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        rq.setFileId("");

        return null;
    }

    @Override
    public boolean requiresCapability() {
        return false;
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
