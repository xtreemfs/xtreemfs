/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.operations;

import java.net.InetSocketAddress;

import org.xtreemfs.common.stage.AbstractRPCRequestCallback;
import org.xtreemfs.common.stage.RPCRequestCallback;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.vivaldi.VivaldiNode;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_pingMesssage;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/**
 *
 * <br>15.06.2009
 */
public class VivaldiPingOperation extends OSDOperation {

    final String sharedSecret;

    final ServiceUUID localUUID;

    public VivaldiPingOperation(OSDRequestDispatcher master) {
        super(master);
        
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        
        return OSDServiceConstants.PROC_ID_XTREEMFS_PING;
    }

    @Override
    public ErrorResponse startRequest(final OSDRequest rq, final RPCRequestCallback callback) {
        
        final xtreemfs_pingMesssage args = (xtreemfs_pingMesssage) rq.getRequestArgs();
        
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG,Category.all,this,"vivaldi ping with coordinates %s",
                    VivaldiNode.coordinatesToString(args.getCoordinates()));
        }

        // Check for udp connections which don't have a channel.
        if (rq.getRPCRequest().getConnection().getChannel() == null) {
            
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG,Category.all,this,"Async Ping");
            }
            master.getVivaldiStage().getVivaldiCoordinatesAsync(args, 
                    (InetSocketAddress) rq.getRPCRequest().getSenderAddress(), rq);
            callback.success();
        } else {
            
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG,Category.all, this, "Sync Ping");
            }
            master.getVivaldiStage().getVivaldiCoordinatesSync(args, rq, new AbstractRPCRequestCallback(callback) {
                
                @Override
                public boolean success(Object result) {
                    callback.success(xtreemfs_pingMesssage.newBuilder().setCoordinates((VivaldiCoordinates) result)
                            .setRequestResponse(false).build());
                    
                    return true;
                }
            });
        }

        return null;
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