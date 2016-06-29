/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease.proposer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.comm.ProposalNumber;
import org.xtreemfs.foundation.flease.proposer.CellAction.ActionName;
import org.xtreemfs.foundation.flease.proposer.CellAction.CellActionList;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 *
 * @author bjko
 */
public class FleaseProposerCell {

    /**
     * @return the responses
     */
    public List<FleaseMessage> getResponses() {
        return responses;
    }

    /**
     * @return the messageSent
     */
    public FleaseMessage getMessageSent() {
        return messageSent;
    }

    /**
     * @param messageSent the messageSent to set
     */
    public void setMessageSent(FleaseMessage messageSent) {
        this.messageSent = messageSent;
    }

    /**
     * @return the lastPrepateTimestamp_ms
     */
    public long getLastPrepareTimestamp_ms() {
        return lastPrepateTimestamp_ms;
    }

    public void setViewId(int viewId) {
        this.viewId = viewId;
    }

    public int getViewId() {
        return this.viewId;
    }

    public boolean isHandoverInProgress() {
        return handoverTo != null;
    }

    /**
     * @return the prevLease
     */
    public Flease getPrevLease() {
        return prevLease;
    }

    /**
     * @param prevLease the prevLease to set
     */
    public void setPrevLease(Flease prevLease) {
        this.prevLease = prevLease;
    }

    /**
     * @return the markedClose
     */
    public boolean isMarkedClose() {
        return markedClose;
    }

    /**
     * @param markedClose the markedClose to set
     */
    public void setMarkedClose(boolean markedClose) {
        this.markedClose = markedClose;
    }

    /**
     * @return the requestMasteEpoch
     */
    public boolean isRequestMasteEpoch() {
        return requestMasteEpoch;
    }

    /**
     * @param requestMasteEpoch the requestMasteEpoch to set
     */
    public void setRequestMasteEpoch(boolean requestMasteEpoch) {
        this.requestMasteEpoch = requestMasteEpoch;
    }

    /**
     * @return the masterEpochNumber
     */
    public long getMasterEpochNumber() {
        return masterEpochNumber;
    }

    /**
     * @param masterEpochNumber the masterEpochNumber to set
     */
    public void setMasterEpochNumber(long masterEpochNumber) {
        this.masterEpochNumber = masterEpochNumber;
    }

    public static enum State {
        IDLE,
        WAIT_FOR_PREP_ACK,
        WAIT_FOR_ACCEPT_ACK,
    };

    private final ASCIIString                   cellId;

    private ProposalNumber ballotNo;

    private List<InetSocketAddress> acceptors;

    private final List<FleaseListener> listeners;

    private final List<FleaseMessage>      responses;

    private State cellState;

    private int   numFailures;

    private final int   majority;

    private long  lastPrepateTimestamp_ms;

    private int   viewId;

    private Flease prevLease;

    private boolean markedClose;

    private ASCIIString handoverTo;

    private boolean requestMasteEpoch;

    private long masterEpochNumber;
    
    private final CellActionList actions;

    /**
     * the value to use for prepare, accept and learn
     * might be != than the local host, if the proposer
     * must use a previously accepted value
     */
    private FleaseMessage           messageSent;

    FleaseProposerCell(ASCIIString cellId, List<InetSocketAddress> acceptors, long senderId) {
        this.actions = new CellActionList();
        this.cellId = cellId;
        this.acceptors = acceptors;
        this.responses = new ArrayList(acceptors.size()+1);
        this.listeners = new ArrayList(5);
        this.ballotNo = new ProposalNumber(TimeSync.getGlobalTime(),senderId);
        this.majority = (int) Math.floor((acceptors.size()+1.0)/ 2.0) + 1;
        this.prevLease = Flease.EMPTY_LEASE;
        this.markedClose = false;
        this.handoverTo = null;
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.flease, this,"opened new cell id %s with majority = %d ",cellId,majority);
        }
    }
    
    public void addAction(ActionName actionName) {
        actions.addAction(actionName);
    }
    
    public void addAction(ActionName actionName, String message) {
        actions.addAction(actionName, message);
    }

    public boolean majorityAvail() {
        return responses.size() >= this.majority;
    }

    /**
     * @return the cellId
     */
    public ASCIIString getCellId() {
        return cellId;
    }

    /**
     * @return the ballotNo
     */
    public ProposalNumber getBallotNo() {
        return ballotNo;
    }

    /**
     * @param ballotNo the ballotNo to set
     */
    public void setBallotNo(ProposalNumber ballotNo) {
        this.ballotNo = ballotNo;
    }

    /**
     * @return the acceptors
     */
    public List<InetSocketAddress> getAcceptors() {
        return acceptors;
    }

    /**
     * @param acceptors the acceptors to set
     */
    public void setAcceptors(List<InetSocketAddress> acceptors) {
        this.acceptors = acceptors;
    }

    /**
     * @return the cellState
     */
    public State getCellState() {
        return cellState;
    }

    /**
     * @param cellState the cellState to set
     */
    public void setCellState(State cellState) {
        this.cellState = cellState;
    }

    /**
     * @return the listeners
     */
    public List<FleaseListener> getListeners() {
        return listeners;
    }

    public int getNumAcceptors() {
        return acceptors.size();
    }

    /**
     * @return the numFailures
     */
    public int getNumFailures() {
        return numFailures;
    }

    /**
     * @param numFailures the numFailures to set
     */
    public void setNumFailures(int numFailures) {
        this.numFailures = numFailures;
    }

    public void touch() {
        this.lastPrepateTimestamp_ms = TimeSync.getLocalSystemTime();
    }

    /**
     * @return the handoverTo
     */
    public ASCIIString getHandoverTo() {
        return handoverTo;
    }

    /**
     * @param handoverTo the handoverTo to set
     */
    public void setHandoverTo(ASCIIString handoverTo) {
        this.handoverTo = handoverTo;
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        text.append(getClass().getSimpleName());
        text.append(":{");
        text.append(" cellId:");
        text.append(cellId);
        text.append(" ballotNo:");
        text.append(ballotNo);
        text.append(" numAcceptors:");
        text.append(acceptors != null ? acceptors.size() : "null");
        text.append(" numResponses:");
        text.append(responses != null ? responses.size() : "null");
        text.append(" cellState:");
        text.append(cellState);
        text.append(" numFail:");
        text.append(numFailures);
        text.append(" majority:");
        text.append(majority);
        text.append(" lastPTSP:");
        text.append(lastPrepateTimestamp_ms);
        text.append(" viewId:");
        text.append(viewId);
        text.append(" prevL:");
        text.append(prevLease != null ? prevLease : "none");
        text.append(" markedClose:");
        text.append(markedClose);
        text.append(" rqME:");
        text.append(requestMasteEpoch);
        text.append(" msgSent:");
        text.append(messageSent);
        text.append(" responses:");
        for (FleaseMessage r : responses) {
            text.append(r);
            text.append(",");
        }
        text.append(" actions:");
        text.append(actions);
        text.append("}");
        return text.toString();
    }

}
