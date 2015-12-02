/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease.acceptor;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.flease.FleaseConfig;
import org.xtreemfs.foundation.flease.FleaseViewChangeListenerInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.comm.ProposalNumber;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 * @author bjko
 */
public class FleaseAcceptor {

    public static final String LOCKFILE_NAME = "flease_lock.";
    public final Map<ASCIIString, FleaseAcceptorCell> cells;
    private final String lockfile;

    private final long waitUntilTimestamp_ms;
    private final FleaseConfig config;
    private final LearnEventListener evtListener;
    // set to true to quit
    public boolean quit;
    private FleaseViewChangeListenerInterface viewListener;


    /**
     * Creates a new instance of FleaseAcceptor
     */
    public FleaseAcceptor(LearnEventListener evtListener, FleaseConfig localConfig, String lockfileDir, boolean ignoreLockFileForTesting)
            throws IOException {
        this.config = localConfig;

        cells = new HashMap<ASCIIString, FleaseAcceptorCell>();

        quit = false;

        lockfile = lockfileDir + "/" + LOCKFILE_NAME + config.getIdentity().hashCode();

        this.evtListener = evtListener;

        File lock = new File(lockfile);
        if (lock.exists() && !ignoreLockFileForTesting) {
            /*waitUntilTimestamp_ms = TimeSync.getLocalSystemTime()+TimeSync.getLocalRenewInterval()+
                    config.getRestartWait();*/
            waitUntilTimestamp_ms = System.currentTimeMillis() + config.getRestartWait();
            Logging.logMessage(Logging.LEVEL_INFO, Category.replication, this, "restarted after crash (lock file %s exists). acceptor will ignore all messages for %d ms (recovery period until %s)",
                    lockfile, config.getRestartWait(), (new Date(waitUntilTimestamp_ms)).toString());

        } else {
            waitUntilTimestamp_ms = 0;
            if (ignoreLockFileForTesting)
                lock.delete();
            if (!lock.createNewFile())
                throw new IOException("Lock file exists!");
        }

    }

    public void setViewChangeListener(FleaseViewChangeListenerInterface listener) {
        viewListener = listener;
    }

    public void setViewId(ASCIIString cellId, int viewId) {
        FleaseAcceptorCell cell = getCell(cellId);

        // Set the viewId or invalidate the cell if the VIEW_ID_INVALIDATED is passed.
        if (viewId == FleaseMessage.VIEW_ID_INVALIDATED) {
            cell.invalidateView();
        } else {
            cell.setViewId(viewId);
        }
    }

    private FleaseAcceptorCell getCell(FleaseMessage msg) {
        assert (msg != null);
        return getCell(msg.getCellId());
    }

    private FleaseAcceptorCell getCell(ASCIIString cellId) {
        FleaseAcceptorCell cell = cells.get(cellId);
        if (cell == null) {
            cell = new FleaseAcceptorCell();
            cells.put(cellId, cell);
        } else {
            if ((cell.lastAccess + config.getCellTimeout()) < System.currentTimeMillis()) {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "A GCed cell " + cellId);
                // Cell is outdated and GCed.

                // Create a new cell and transfer the previous view.
                FleaseAcceptorCell tmp = new FleaseAcceptorCell();
                tmp.setViewId(cell.getViewId());
                if (cell.isViewInvalidated()) {
                    tmp.invalidateView();
                }

                cells.put(cellId, tmp);
                cell = tmp;
            }
        }
        /*if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG,this,"using cell "+cellId);*/
        cell.touch();
        return cell;
    }

    /**
     * Handles paxos prepare messages
     *
     * @param msg the incoming message
     * @return a response message
     */
    public FleaseMessage handlePREPARE(FleaseMessage msg) {

        final FleaseAcceptorCell cell = getCell(msg);
        cell.touch();

        if ((cell.getPrepared() != null) && (cell.getPrepared().after(msg))) {
            if (Logging.isDebug() && config.isDebugPrintMessages()) {
                final String prepared = (cell.getPrepared() == null) ? ProposalNumber.EMPTY_PROPOSAL_NUMBER.toString() : cell.getPrepared().getProposalNo().toString();
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "A prepare NACK p:" + prepared + " is after " + msg.getProposalNo() + "");
            }
            FleaseMessage reject = new FleaseMessage(
                    FleaseMessage.MsgType.MSG_PREPARE_NACK, msg);
            reject.setPrevProposalNo(cell.getPrepared().getProposalNo());
            reject.setLeaseHolder(null);
            reject.setLeaseTimeout(0);
            reject.setSendTimestamp(TimeSync.getGlobalTime());
            return reject;
        } else {
            if (Logging.isDebug() && config.isDebugPrintMessages()) {
                final String prepared = (cell.getPrepared() == null) ? ProposalNumber.EMPTY_PROPOSAL_NUMBER.toString() : cell.getPrepared().getProposalNo().toString();
                final String accepted = (cell.getAccepted() == null) ? ProposalNumber.EMPTY_PROPOSAL_NUMBER.toString() : cell.getAccepted().getProposalNo() + "=" + cell.getAccepted().getLeaseHolder() + "/" + cell.getAccepted().getLeaseTimeout();
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "A prepare ACK  p:" + prepared + " -> " + msg.getProposalNo() + " a:" + accepted);
            }
            cell.setPrepared(msg);
            // FIXME:Persistently write to disk
            FleaseMessage response = new FleaseMessage(
                    FleaseMessage.MsgType.MSG_PREPARE_ACK, msg);

            if (cell.getAccepted() != null) {

                response.setPrevProposalNo(cell.getAccepted().getProposalNo());
                response.setLeaseHolder(cell.getAccepted().getLeaseHolder());
                assert (response.getLeaseHolder() != null);
                response.setLeaseTimeout(cell.getAccepted()
                        .getLeaseTimeout());
            }
            response.setSendTimestamp(TimeSync.getGlobalTime());
            return response;


        }
    }

    /**
     * Handles paxos accept (vote) messages.
     *
     * @param msg incoming message
     * @return a response message or null
     */
    public FleaseMessage handleACCEPT(FleaseMessage msg) {

        final FleaseAcceptorCell cell = getCell(msg);

        cell.touch();
        if ((cell.getPrepared() != null) && (cell.getPrepared().after(msg))) {
            // reject the request
            if (Logging.isDebug() && config.isDebugPrintMessages()) {
                final String prepared = (cell.getPrepared() == null) ? ProposalNumber.EMPTY_PROPOSAL_NUMBER.toString() : cell.getPrepared().getProposalNo().toString();
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "A accept  NACK p:" + prepared + " is after " + msg.getProposalNo() + "");
            }
            FleaseMessage tmp = new FleaseMessage(
                    FleaseMessage.MsgType.MSG_ACCEPT_NACK, msg);
            tmp.setSendTimestamp(TimeSync.getGlobalTime());
            tmp.setLeaseHolder(null);
            tmp.setLeaseTimeout(0);
            tmp.setPrevProposalNo(cell.getPrepared().getProposalNo());
            return tmp;
        } else {
            // okay accept it
            if (Logging.isDebug() && config.isDebugPrintMessages()) {
                final String prepared = (cell.getPrepared() == null) ? ProposalNumber.EMPTY_PROPOSAL_NUMBER.toString() : cell.getPrepared().getProposalNo().toString();
                final String accepted = (cell.getAccepted() == null) ? ProposalNumber.EMPTY_PROPOSAL_NUMBER.toString() : cell.getAccepted().getProposalNo() + "=" + cell.getAccepted().getLeaseHolder() + "/" + cell.getAccepted().getLeaseTimeout();
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "A accept  ACK  p:" + prepared + " a: " + accepted + " -> " + msg.getProposalNo() + "=" + msg.getLeaseHolder() + "/" + msg.getLeaseTimeout());
            }
            assert (msg.getLeaseHolder() != null);
            cell.setAccepted(msg);
            cell.setPrepared(msg);


            FleaseMessage tmp = new FleaseMessage(
                    FleaseMessage.MsgType.MSG_ACCEPT_ACK, msg);
            tmp.setSendTimestamp(TimeSync.getGlobalTime());
            return tmp;

        }
    }

    /**
     * Handles paxos learn messages. Removes oudated instances and updates
     * maxLearnedInstId accordingly
     *
     * @param msg incomming message
     */
    public void handleLEARN(FleaseMessage msg) {
        final FleaseAcceptorCell cell = getCell(msg);

        cell.touch();
        if ((cell.getPrepared() != null) && (cell.getPrepared().after(msg))
                || (cell.getAccepted() != null) && (cell.getAccepted().after(msg))) {
            if (Logging.isDebug() && config.isDebugPrintMessages())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "A ignore outdated LEARN message " + msg.getProposalNo());
        } else {
            if (Logging.isDebug() && config.isDebugPrintMessages()) {
                final String prepared = (cell.getPrepared() == null) ? ProposalNumber.EMPTY_PROPOSAL_NUMBER.toString() : cell.getPrepared().getProposalNo().toString();
                final String accepted = (cell.getAccepted() == null) ? ProposalNumber.EMPTY_PROPOSAL_NUMBER.toString() : cell.getAccepted().getProposalNo() + "=" + cell.getAccepted().getLeaseHolder() + "/" + cell.getAccepted().getLeaseTimeout();
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "A learn        p:" + prepared + " a: " + accepted + " -> " + msg.getProposalNo() + "=" + msg.getLeaseHolder() + "/" + msg.getLeaseTimeout());
            }

            cell.setAccepted(msg);
            cell.setPrepared(msg);
            cell.setLatestLearn(msg);
            evtListener.learnedEvent(msg.getCellId(), msg.getLeaseHolder(), msg.getLeaseTimeout(), msg.getMasterEpochNumber());
        }

    }

    /**
     * Handles requests for lease information. Returns current lease
     * holder + timeout or the current instance id.
     */
    public FleaseMessage getLocalLeaseInformation(ASCIIString cellId) {
        FleaseAcceptorCell cc = getCell(cellId);
        return cc.getLatestLearn();
    }

    public Map<ASCIIString, FleaseMessage> localState() {
        Map<ASCIIString, FleaseMessage> state = new HashMap();

        for (ASCIIString cellId : cells.keySet()) {
            FleaseAcceptorCell cell = cells.get(cellId);
            FleaseMessage lrn = null;
            if (cell.isLearned())
                lrn = cell.getAccepted();
            state.put(cellId, lrn);
        }

        return state;
    }


    /**
     * main loop
     */
    public FleaseMessage processMessage(FleaseMessage msg) {

        assert (!quit);

        /*if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG,this,"received %s",msg.toString());*/

        final long now = TimeSync.getLocalSystemTime();
        if (msg.getSendTimestamp() + config.getMessageTimeout() < TimeSync.getGlobalTime()) {
            //old message, ignore
            if (Logging.isDebug() && config.isDebugPrintMessages())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "A outdated message discarded: %s", msg.toString());
            return null;
        }
        if (this.waitUntilTimestamp_ms >= now) {
            if (Logging.isDebug() && config.isDebugPrintMessages())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "A message discarded, acceptor is still in recovery period");
            return null;
        }


        assert (msg.getCellId() != null);
        final FleaseAcceptorCell cell = getCell(msg.getCellId());

        if (cell.getViewId() < msg.getViewId()) {
            // If the local view is lower than the delivered one, the view listener has to be informed to update
            // the local view. But the request can still be answered.
            viewListener.viewIdChangeEvent(msg.getCellId(), msg.getViewId(), false);

        } else if (cell.getViewId() > msg.getViewId() || (cell.getViewId() == msg.getViewId() && cell.isViewInvalidated())) {
            // If the request is from an older view, or the a view that has been invalidated on this
            // AcceptorCell, the request has to be aborted.
            FleaseMessage response = new FleaseMessage(FleaseMessage.MsgType.MSG_WRONG_VIEW, msg);
            response.setViewId(cell.getViewId());
            return response;
        }

        FleaseMessage response = null;
        if (msg.getMsgType() == FleaseMessage.MsgType.MSG_PREPARE)
            response = handlePREPARE(msg);
        else if (msg.getMsgType() == FleaseMessage.MsgType.MSG_ACCEPT)
            response = handleACCEPT(msg);
        else if (msg.getMsgType() == FleaseMessage.MsgType.MSG_LEARN)
            handleLEARN(msg);
        /*else if (msg.getMsgType() == FleaseMessage.MsgType.MSG_GET_LEASE)
            response = handleGETLEASE(msg);
        else if (msg.getMsgType() == FleaseMessage.MsgType.MSG_RENEW_LEASE)
            response = handleRENEWINSTANCE(msg);*/
        else
            Logging.logMessage(Logging.LEVEL_ERROR, Category.replication, this, "A invalid message type received: %s", msg.toString());

        /*if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG,this,"response %s",(response != null) ? response.toString() : "<empty>");*/

        return response;
    }

    /*private void notifyLeaseListener(FleaseAcceptorCell cell, ASCIIString leaseHolder, long leaseTimeout) {
        if (leaseListener != null) {
            final ASCIIString prevLeaseHolder = cell.getPrevLeaseHolder();
            if ( ( (((prevLeaseHolder == null) || (leaseHolder == null)) && (prevLeaseHolder != leaseHolder)) ||
                 (!prevLeaseHolder.equals(leaseHolder)) ) || (leaseTimeout != cell.getPrevLeaseTimeout())) {
                leaseListener.statusChanged(cell.getCellId(), leaseHolder,leaseTimeout);
                cell.setPrevLeaseHolder(leaseHolder);
                cell.setPrevLeaseTimeout(leaseTimeout);
            }
        }
    }*/

    /**
     * string representation
     *
     * @return a string
     */
    public String toString() {
        return "Acceptor @ " + config.getIdentity();
    }

    // only for testing!
    /*public String getLocalLeaseInfo(String cellId) {
        
        synchronized (cells) {
            FleaseAcceptorCell cc = getCell(cellId);
            FleaseInstance currentL = cc.getInstances().get(cc.getMaxLearnedInstanceId());
            if (currentL != null) {
                // there is an instance
                if (currentL.getAccepted().getLeaseTimeout() > (System
                        .currentTimeMillis() / 1000)
                        - config.getDMax()) {
                    return currentL.getAccepted().getLeaseHolder();
                } else {
                    return null;
                }
            }
            return null;
        }
        
    }*/

    public void shutdown() {
        this.quit = true;
        File f = new File(lockfile);
        f.delete();
    }


}
