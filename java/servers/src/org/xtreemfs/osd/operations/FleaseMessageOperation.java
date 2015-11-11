/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import java.net.InetSocketAddress;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.flease_destination;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_rwr_flease_msgRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/**
 *
 * @author bjko
 */
public class FleaseMessageOperation extends OSDOperation {

    public FleaseMessageOperation(OSDRequestDispatcher master) {
        super(master);
    }

    @Override
    public int getProcedureId() {
        return OSDServiceConstants.PROC_ID_XTREEMFS_RWR_FLEASE_MSG;
    }

    @Override
    public void startRequest(OSDRequest rq) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.ec, this,
                    "start flease operation");
        }
        xtreemfs_rwr_flease_msgRequest args = (xtreemfs_rwr_flease_msgRequest) rq.getRequestArgs();
        // destination rwreplication is default for backwards compatibility
        flease_destination dest = flease_destination.RWR;
        if (args.hasDestination()) {
            dest = args.getDestination();
        }
        try {
            InetSocketAddress sender = new InetSocketAddress(args.getSenderHostname(), args.getSenderPort());
            if (dest == flease_destination.EC) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.ec, this,
                            "relaying Flease message to ECStage");
                }
                master.getECStage().receiveFleaseMessage(rq.getRPCRequest().getData().createViewBuffer(), sender);
            } else {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,
                            "relaying Flease message to RwReplicationStage");
                }
                master.getRWReplicationStage().receiveFleaseMessage(rq.getRPCRequest().getData().createViewBuffer(), sender);
            }
            rq.sendSuccess(null,null);
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_WARN, this,ex);
        }
        
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("not an internal event!");
    }

    @Override
    public boolean requiresCapability() {
        return false;
    }

    @Override
    public ErrorResponse parseRPCMessage(OSDRequest rq) {
        return null;
    }


}
