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
        xtreemfs_rwr_flease_msgRequest args = (xtreemfs_rwr_flease_msgRequest) rq.getRequestArgs();
        try {
            InetSocketAddress sender = new InetSocketAddress(args.getSenderHostname(), args.getSenderPort());
            master.getFleaseHandler().receiveMessage(rq.getRpcRequest().getData().createViewBuffer(), sender);
            rq.sendSuccess(null, null);
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_WARN, this, ex);
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
