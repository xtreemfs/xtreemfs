/*
 * Copyright (c) 2016 by Johannes Dillmann
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.flease.FleaseMessageSenderInterface;
import org.xtreemfs.foundation.flease.FleaseStatusListener;
import org.xtreemfs.foundation.flease.FleaseViewChangeListenerInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.proposer.FleaseException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.client.RPCResponseAvailableListener;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

/**
 * Simple class that forwards Flease events to different handlers based on a prefix. <br>
 * Also used for encapsuling the Flease network communication interface between OSDs.
 */
public class FleasePrefixHandler
        implements FleaseMessageSenderInterface, FleaseViewChangeListenerInterface, FleaseStatusListener {
    final static byte                         PREFIX_SEP = (byte) '/';

    final OSDServiceClient                    osdClient;
    final OSDRequestDispatcher                master;
    final HashMap<ASCIIString, FleaseHandler> prefixMap;

    public FleasePrefixHandler(OSDRequestDispatcher master, RPCNIOSocketClient client) {
        this.master = master;
        this.osdClient = new OSDServiceClient(client, null);
        prefixMap = new HashMap<ASCIIString, FleasePrefixHandler.FleaseHandler>();
    }

    /**
     * Register handlers for the prefix. Prefixes are separated by PREFIX_SEP.
     * 
     * @param prefix
     * @param viewChangeListener
     * @param statusListener
     */
    public void registerPrefix(String prefix, FleaseViewChangeListenerInterface viewChangeListener,
            FleaseStatusListener statusListener) {
        prefixMap.put(new ASCIIString(prefix), new FleaseHandler(viewChangeListener, statusListener));
    }

    /**
     * Helper to get the prefix of the given cell name.
     * 
     * @param cell
     * @return prefix or empty ASCIIString if separator has not been found
     */
    public static ASCIIString getPrefix(ASCIIString cell) {
        ASCIIString[] split = cell.splitLast(PREFIX_SEP, true);
        if (split.length == 2) {
            return split[0];
        }
        return new ASCIIString("");
    }

    /**
     * Helper to strip the prefix from the cell name.
     * 
     * @param cell
     * @return ASCIIString with the prefix striped.
     */
    public static ASCIIString stripPrefix(ASCIIString cell) {
        ASCIIString[] split = cell.splitLast(PREFIX_SEP, true);
        if (split.length == 2) {
            return split[1];
        }
        return split[0];
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void sendMessage(final FleaseMessage message, final InetSocketAddress recipient) {
        ReusableBuffer data = BufferPool.allocate(message.getSize());
        message.serialize(data);
        data.flip();
        try {
            RPCResponse r = osdClient.xtreemfs_rwr_flease_msg(recipient, RPCAuthentication.authNone,
                    RPCAuthentication.userService, master.getHostName(), master.getConfig().getPort(), data);
            r.registerListener(new RPCResponseAvailableListener() {

                @Override
                public void responseAvailable(RPCResponse r) {
                    r.freeBuffers();
                }
            });
        } catch (IOException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public void receiveMessage(ReusableBuffer message, InetSocketAddress sender) {
        FleaseMessage msg = new FleaseMessage(message);
        BufferPool.free(message);
        msg.setSender(sender);
        master.getFleaseStage().receiveMessage(msg);
    }

    @Override
    public void statusChanged(final ASCIIString cellId, final Flease lease) {
        ASCIIString prefix = getPrefix(cellId);
        FleaseHandler handler = prefixMap.get(prefix);
        if (handler != null) {
            handler.statusListener.statusChanged(cellId, lease);
        }
    }

    @Override
    public void leaseFailed(final ASCIIString cellId, final FleaseException error) {
        ASCIIString prefix = getPrefix(cellId);
        FleaseHandler handler = prefixMap.get(prefix);
        if (handler != null) {
            handler.statusListener.leaseFailed(cellId, error);
        }
    }

    @Override
    public void viewIdChangeEvent(final ASCIIString cellId, final int viewId, final boolean onProposal) {
        ASCIIString prefix = getPrefix(cellId);
        FleaseHandler handler = prefixMap.get(prefix);
        if (handler != null) {
            handler.viewChangeListener.viewIdChangeEvent(cellId, viewId, onProposal);
        }
    }

    private final static class FleaseHandler {
        final FleaseViewChangeListenerInterface viewChangeListener;
        final FleaseStatusListener              statusListener;

        public FleaseHandler(FleaseViewChangeListenerInterface viewChangeListener,
                FleaseStatusListener statusListener) {
            this.viewChangeListener = viewChangeListener;
            this.statusListener = statusListener;
        }
    }

}
