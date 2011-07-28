/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease.proposer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.flease.Flease;
import org.xtreemfs.foundation.flease.FleaseConfig;
import org.xtreemfs.foundation.flease.FleaseStage;
import org.xtreemfs.foundation.flease.FleaseViewChangeListenerInterface;
import org.xtreemfs.foundation.flease.FleaseStatusListener;
import org.xtreemfs.foundation.flease.MasterEpochHandlerInterface;
import org.xtreemfs.foundation.flease.acceptor.FleaseAcceptor;
import org.xtreemfs.foundation.flease.acceptor.LearnEventListener;
import org.xtreemfs.foundation.flease.comm.FleaseCommunicationInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.comm.FleaseMessage.MsgType;
import org.xtreemfs.foundation.flease.comm.ProposalNumber;
import org.xtreemfs.foundation.flease.proposer.FleaseProposerCell.State;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 *
 * @author bjko
 */
public class FleaseProposer {

    final Map<ASCIIString, FleaseProposerCell> cells;

    final FleaseConfig config;

    final FleaseAcceptor localAcceptor;

    final FleaseCommunicationInterface comm;

    //private long lastBallotNo;

    private FleaseViewChangeListenerInterface viewListener;

    private final FleaseStatusListener          leaseListener;

    private final LearnEventListener            evListener;

    private final FleaseLocalQueueInterface     localQueue;

    private final MasterEpochHandlerInterface   meHandler;

    public FleaseProposer(FleaseConfig config, FleaseAcceptor localAcceptor, FleaseCommunicationInterface comm,
            FleaseStatusListener leaseListener, LearnEventListener evListener, FleaseLocalQueueInterface localQueue,
            MasterEpochHandlerInterface meHandler) {

        cells = new HashMap(100000);
        this.config = config;
        this.localAcceptor = localAcceptor;
        this.comm = comm;
        assert(leaseListener != null);
        this.leaseListener = leaseListener;
        this.evListener = evListener;
        this.localQueue = localQueue;
        this.meHandler = meHandler;
        assert ((meHandler == null) || (meHandler != null) && (localQueue != null));
    }

    public void setViewChangeListener(FleaseViewChangeListenerInterface listener) {
        viewListener = listener;
    }

    public void setViewId(ASCIIString cellId, int viewId) throws FleaseException {
        FleaseProposerCell cell = cells.get(cellId);
        if (cell == null) {
            throw new FleaseException("cell must be opened before any operation!");
        }
        cell.setViewId(viewId);
    }

    public Flease updatePrevLeaseForCell(ASCIIString cellId, Flease lease) {
        FleaseProposerCell cell = cells.get(cellId);
        if (cell == null) {
            return null;
        }
        final Flease prevLease = cell.getPrevLease();
        if (!prevLease.equals(lease)) {
            cell.setPrevLease(lease);
            return prevLease;
        }
        return null;
    }

    public ProposalNumber getCurrentBallotNo(ASCIIString cellId) {
        FleaseProposerCell cell = cells.get(cellId);
        if (cell == null) {
            return null;
        }
        return cell.getBallotNo();
    }

    public void openCell(ASCIIString cellId, List<InetSocketAddress> acceptors, boolean requestMasterEpoch) throws FleaseException {
        FleaseProposerCell cell = cells.get(cellId);
        if (cell == null) {
            cell = new FleaseProposerCell(cellId, acceptors, config.getSenderId());
            cell.setCellState(State.IDLE);
            cell.setRequestMasteEpoch(requestMasterEpoch);
            cells.put(cellId, cell);
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.replication, this, "P created new cellId %s",
                        cellId);
            }
            acquireLease(cell);
        } else {
            throw new FleaseException("cell already opened");
        }
    }

    public void closeCell(ASCIIString cellId) {
        cells.remove(cellId);
    }

    private void acquireLease(FleaseProposerCell cell) throws FleaseException {

        final ASCIIString cellId = cell.getCellId();
        //check local results
        FleaseMessage localInfo = localAcceptor.getLocalLeaseInformation(cellId);
        if ((localInfo != null) && (localInfo.hasNotTimedOut(config, TimeSync.getGlobalTime()))) {
            //we can safely return the learned values
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P request served from local state: %s",
                        cellId);
            }
            evListener.learnedEvent(localInfo.getCellId(), localInfo.getLeaseHolder(), localInfo.getLeaseTimeout(), localInfo.getMasterEpochNumber());
            return;
        }

        if (cell.getCellState() == State.IDLE) {
            //can safely start the proposal
            cell.setNumFailures(0);
            cell.setHandoverTo(null);
            startPrepare(cell,config.getIdentity());
        } else {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_WARN, Logging.Category.replication, this, "P cellId %s is not idle, ignoring acquireLease",
                        cellId);
            }
        }
    }

    public void renewLease(ASCIIString cellId) throws FleaseException {
        FleaseProposerCell cell = cells.get(cellId);
        if (cell == null) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "P ignore renew for closed/unknown cell %s", cellId);
            }
            return;
        }
        //fixme

        if (cell.isHandoverInProgress()) {
            Logging.logMessage(Logging.LEVEL_INFO, this,"handover in progress for cell %s, renew canceled",cell.getCellId());
            return;
        }

        //check local state
        FleaseMessage localInfo = localAcceptor.getLocalLeaseInformation(cellId);

        if (localInfo == null) {
            throw new FleaseException("cannot renew lease, no local lease information!");
        }
        if (!localInfo.getLeaseHolder().equals(config.getIdentity())) {
            throw new FleaseException("cannot renew lease, not lease owner (owner is " + localInfo.getLeaseHolder() + ")!");
        }
        //make sure there is enough time before timeout
        if (localInfo.getLeaseTimeout() - config.getDMax() < TimeSync.getGlobalTime() + config.getRoundTimeout() * 2) {
            throw new FleaseException("cannot renew lease, not enough time left for renew! " + (localInfo.getLeaseTimeout() - config.getDMax()) + " < " + (TimeSync.getGlobalTime() + config.getRoundTimeout() * 2));
        }

        //no need for a new master epoch upon renew!
        cell.setRequestMasteEpoch(false);

        //we can safely set the instance no +1
        //ONLY ALLOWED DURING RENEW!

        startPrepare(cell,config.getIdentity());
    }

    public void handoverLease(ASCIIString cellId, ASCIIString newOwner) throws FleaseException {
        // TODO(bjko): Change to return lease.
        FleaseProposerCell cell = cells.get(cellId);
        if (cell == null) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "P ignore renew for closed/unknown cell %s", cellId);
            }
            return;
        }
        //fixme

        //check local state
        FleaseMessage localInfo = localAcceptor.getLocalLeaseInformation(cellId);

        if (localInfo == null) {
            throw new FleaseException("cannot handover lease, no local lease information!");
        }
        if (!localInfo.getLeaseHolder().equals(config.getIdentity())) {
            throw new FleaseException("cannot handover lease, not lease owner (owner is " + localInfo.getLeaseHolder() + ")!");
        }
        //make sure there is enough time before timeout
        if (localInfo.getLeaseTimeout() - config.getDMax() < TimeSync.getGlobalTime() + config.getRoundTimeout() * 2) {
            throw new FleaseException("cannot handover lease, not enough time left for renew! " + (localInfo.getLeaseTimeout() - config.getDMax()) + " < " + (TimeSync.getGlobalTime() + config.getRoundTimeout() * 2));
        }
        //we can safely set the instance no +1
        //ONLY ALLOWED DURING RENEW!

        cell.setHandoverTo(newOwner);
        if (cell.getCellState() == State.IDLE) {
            //set state to unknown
            evListener.learnedEvent(localInfo.getCellId(), null, 0, FleaseMessage.IGNORE_MASTER_EPOCH);
            startPrepare(cell,newOwner);
        }
    }

    public void processMessage(FleaseMessage msg) {

        //get the cell
        FleaseProposerCell cell = cells.get(msg.getCellId());
        if (cell == null) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P drop message for unknown cellId %s from %s",
                        msg.getCellId(), msg.getSender());
            }
            return;
        }

        //reject anything if viewID < own viewID or if own viewId is -1
        //ignore if viewId > own viewID but notify listener
        final int myViewId = cell.getViewId();
        if (myViewId == FleaseMessage.VIEW_ID_INVALIDATED) {
            //just ignore things
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "P drop message because of INVALIDATED local view for cell %s", cell.getCellId());
            }
            return;
        }
        /*if (myViewId < msg.getViewId()) {
        //notify listener
        //viewListener.viewIdChangeEvent(msg.getCellId(), msg.getViewId());
        }*/
        if (myViewId > msg.getViewId()) {
            //drop
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "P drop message because of outdated remote view for cell %s: local view %d, remote %d", cell.getCellId(), myViewId, msg.getViewId());
            }
            return;
        }

        //events
        switch (cell.getCellState()) {
            case IDLE: {
                if (msg.getMsgType() == FleaseMessage.MsgType.EVENT_RESTART) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P restart event: %s",
                            msg.toString());
                    }
                    startPrepare(cell,config.getIdentity());
                } else if (msg.getMsgType() == FleaseMessage.MsgType.EVENT_RENEW) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P renew event: %s",
                            msg.toString());
                    }
                    try {
                        renewLease(cell.getCellId());
                    } catch (FleaseException ex) {
                        Logging.logError(Logging.LEVEL_ERROR, this, ex);
                        closeCell(cell.getCellId());
                    }
                } else {
                    //ignore everything else
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P droped message in state IDLE: %s",
                            msg.toString());
                    }
                }
                break;
            }
            case WAIT_FOR_PREP_ACK: {
                processPrepareResponse(cell, msg);
                break;
            }
            case WAIT_FOR_ACCEPT_ACK: {
                processAcceptResponse(cell, msg);
                break;
            }
        }

    }

    protected void startPrepare(FleaseProposerCell cell, ASCIIString leaseHolder) {
        if (cell.getCellState() != State.IDLE) {
            throw new RuntimeException("startPrepare must not be called when cell is not idle");
        }

        if (cell.getLastPrepareTimestamp_ms() + config.getMaxLeaseTimeout() < TimeSync.getLocalSystemTime()) {
            //timed out, set new ballot number
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "P reset cell id %s due to timeout", cell.getCellId());
            }
            cell.setBallotNo(new ProposalNumber(TimeSync.getGlobalTime(), config.getSenderId()));
        }

        //propose myself
        cell.getResponses().clear();
        cell.touch();

        //assert (lastBallotNo < cell.getBallotNo().getProposalNo()) : ("lastBallotNo="+lastBallotNo+", cell="+cell.getBallotNo().getProposalNo());
        assert (cell.getBallotNo().getSenderId() == config.getSenderId());

        //lastBallotNo = cell.getBallotNo().getProposalNo();

        final int numRemoteAcc = cell.getNumAcceptors();
        FleaseMessage msg = new FleaseMessage(FleaseMessage.MsgType.MSG_PREPARE);
        msg.setCellId(cell.getCellId());
        msg.setProposalNo(cell.getBallotNo());
        msg.setLeaseHolder(leaseHolder);
        msg.setLeaseTimeout(TimeSync.getGlobalTime() + config.getMaxLeaseTimeout());
        msg.setSendTimestamp(TimeSync.getGlobalTime());
        msg.setSender(null);
        if (cell.isRequestMasteEpoch())
            msg.setMasterEpochNumber(FleaseMessage.REQUEST_MASTER_EPOCH);
        cell.setMessageSent(msg);

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,
                    "P start PREPARE: %s",
                    msg.toString());
        }

        for (int i = 0; i < numRemoteAcc; i++) {
            try {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,
                            "P send prepare to: %s",
                            cell.getAcceptors().get(i));
                }
                Thread.yield();
                comm.sendMessage(msg, cell.getAcceptors().get(i));
            } catch (IOException ex) {
                //cancel(cell, ex, 0);
            }
        }



        FleaseMessage timer = new FleaseMessage(MsgType.EVENT_TIMEOUT_PREPARE);
        timer.setCellId(cell.getCellId());
        timer.setProposalNo(cell.getBallotNo());
        comm.requestTimer(timer, TimeSync.getLocalSystemTime() + config.getRoundTimeout());

        cell.setCellState(State.WAIT_FOR_PREP_ACK);

        final FleaseMessage localResponse = localAcceptor.handlePREPARE(msg);
        if (localResponse != null) {
            if ( (meHandler != null) && cell.isRequestMasteEpoch() && (localResponse.getMsgType() == FleaseMessage.MsgType.MSG_PREPARE_ACK)) {
                meHandler.sendMasterEpoch(localResponse, new MasterEpochHandlerInterface.Continuation() {
                    @Override
                    public void processingFinished() {
                        localQueue.enqueueMessage(localResponse);
                    }
                });
            } else {
                processPrepareResponse(cell, localResponse);
            }
        }

    }

    protected void processPrepareResponse(FleaseProposerCell cell, FleaseMessage msg) {
        assert (cell.getCellState() == State.WAIT_FOR_PREP_ACK);

        if (msg.getSendTimestamp() + config.getMessageTimeout() < TimeSync.getGlobalTime()) {
            //drop outdated message
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P ignore message (too old): %s",
                        msg.toString());
            }
            return;
        }

        if (msg.getSendTimestamp() > TimeSync.getGlobalTime() + config.getDMax()) {
            //drop outdated message
            Logging.logMessage(Logging.LEVEL_WARN, Logging.Category.replication, this, "RECEIVED MESSAGE WITH TIMESTAMP TOO FAR IN THE FUTURE (likely cause: clocks aren't in sync). SYSTEM IS NOT IN A SAFE STATE: %s",
                    msg.toString());
            cancel(cell, new FleaseException("System is not in sync (clock sync drift exceeded)!"), 0);
            return;
        }

        if (msg.before(cell.getMessageSent())) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P ignore message (before my request): %s",
                        msg.toString());
            }
            return;
        }

        if ((msg.getMsgType() != MsgType.MSG_PREPARE_ACK) &&
                (msg.getMsgType() != MsgType.MSG_PREPARE_NACK) &&
                (msg.getMsgType() != MsgType.MSG_WRONG_VIEW) &&
                (msg.getMsgType() != MsgType.EVENT_TIMEOUT_PREPARE)) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P ignore message (UNEXPECTED MESSAGE TYPE %s): %s",
                        msg.getMsgType(), msg.toString());
            }
            return;
        }

        if (msg.getMsgType() == MsgType.EVENT_TIMEOUT_PREPARE) {
            //not enough responses :( abort or re-try
            cancel(cell, new FleaseException("did not receive enough responses for PREPARE"), 0);
            return;
        }

        final List<FleaseMessage> responses = cell.getResponses();
        responses.add(msg);
        if (cell.majorityAvail()) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P majority responded for proposal %s: %d",
                        cell.getBallotNo(), responses.size());
            }
            //analyze responses
            ProposalNumber maxBallot = ProposalNumber.EMPTY_PROPOSAL_NUMBER;
            FleaseMessage prevAccepted = new FleaseMessage(MsgType.MSG_ACCEPT);
            int maxViewId = 0;
            for (FleaseMessage resp : responses) {
                switch (resp.getMsgType()) {

                    case MSG_WRONG_VIEW: {
                        if (resp.getViewId() > maxViewId) {
                            maxViewId = resp.getViewId();
                        }
                        break;
                    }

                    case MSG_PREPARE_ACK: {
                        //check for previously accepted proposals
                        if (!resp.getPrevProposalNo().isEmpty()) {
                            if ((prevAccepted == null) ||
                                    (resp.getPrevProposalNo().after(prevAccepted.getPrevProposalNo()))) {
                                prevAccepted = resp;
                            }
                        }
                        break;
                    }

                    case MSG_PREPARE_NACK: {
                        if (resp.getPrevProposalNo().after(maxBallot)) {
                            maxBallot = resp.getPrevProposalNo();
                        }
                        break;
                    }

                }
            }

            if (maxViewId != cell.getViewId()) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P prepare failed due to outdated view local=%d max=%",
                            cell.getViewId(), maxViewId);
                }
                viewListener.viewIdChangeEvent(cell.getCellId(), maxViewId);
                cancel(cell, new FleaseException("local viewId is outdated"), 0);
                return;
            }

            if (!maxBallot.isEmpty()) {
                cell.setBallotNo(new ProposalNumber(maxBallot.getProposalNo() + (int) (Math.random() * 10) + 1, cell.getBallotNo().getSenderId()));
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P prepare OVERRULED by %s, restart with ballot number: %s",
                            maxBallot.getProposalNo(), cell.getBallotNo());
                }
                //request a timer event for restart
                cancel(cell, new FleaseException("local proposal was overruled by remote proposal"), 0);
                return;
            }

            if (!prevAccepted.getProposalNo().isEmpty()) {
                //change our values for accept

                //check prev accepted value
                if (prevAccepted.hasNotTimedOut(config, TimeSync.getGlobalTime())) {
                    //we must use the previous value
                    //except for renew instances
                    if (prevAccepted.getLeaseHolder().equals(config.getIdentity())) {
                        //renew!
                        assert (cell.getMessageSent().getLeaseHolder().equals(config.getIdentity()) || cell.isHandoverInProgress());
                        assert (cell.getMessageSent().getLeaseTimeout() >= prevAccepted.getLeaseTimeout());
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P prepare ACK processing with my proposal (renew): prev=%s/%d %s",
                                    prevAccepted.getLeaseHolder(), prevAccepted.getLeaseTimeout(), prevAccepted.getPrevProposalNo());
                        }
                    } else {
                        //use other value
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P prepare ACK processing with prior proposal (still valid): prev=%s/%d %s",
                                    prevAccepted.getLeaseHolder(), prevAccepted.getLeaseTimeout(), prevAccepted.getPrevProposalNo());
                        }
                        cell.getMessageSent().setLeaseHolder(prevAccepted.getLeaseHolder());
                        cell.getMessageSent().setLeaseTimeout(prevAccepted.getLeaseTimeout());
                    }
                } else {
                    if (prevAccepted.hasTimedOut(config, TimeSync.getGlobalTime())) {
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P prepare ACK processing with my proposal (old lease has timed out): prev=%s/%d %s",
                                    prevAccepted.getLeaseHolder(), prevAccepted.getLeaseTimeout(), prevAccepted.getPrevProposalNo());
                        }
                    } else {
                        //unknown state of the lease, must use prev value
                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P processing with prior proposal (old lease is in GP): %s/%d %s",
                                    prevAccepted.getLeaseHolder(), prevAccepted.getLeaseTimeout(), prevAccepted.getPrevProposalNo());
                        }
                        cell.getMessageSent().setLeaseHolder(prevAccepted.getLeaseHolder());
                        cell.getMessageSent().setLeaseTimeout(prevAccepted.getLeaseTimeout());
                    }
                }
            } else {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P processing with no prior proposal");
                }
            }

            //master epoch
            if (cell.isRequestMasteEpoch()) {
                long maxMasterEpoch = -1;
                for (FleaseMessage resp : responses) {
                    if (resp.getMsgType() == FleaseMessage.MsgType.MSG_PREPARE_ACK) {
                        if (resp.getMasterEpochNumber() > maxMasterEpoch) {
                            maxMasterEpoch = resp.getMasterEpochNumber();
                        }
                    }
                }
                assert(maxMasterEpoch > -1) : "no valid master epoch sent: "+maxMasterEpoch;
                cell.setMasterEpochNumber(maxMasterEpoch+1);
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P using masterEpoch %s",maxMasterEpoch+1);
                }
            }

            responses.clear();

            startAccept(cell);

        }
    }

    public void startAccept(FleaseProposerCell cell) {

        cell.setCellState(State.WAIT_FOR_ACCEPT_ACK);

        final int numRemoteAcc = cell.getNumAcceptors();

        FleaseMessage msg = new FleaseMessage(FleaseMessage.MsgType.MSG_ACCEPT);
        msg.setCellId(cell.getCellId());
        msg.setProposalNo(cell.getBallotNo());
        msg.setLeaseHolder(cell.getMessageSent().getLeaseHolder());
        msg.setLeaseTimeout(cell.getMessageSent().getLeaseTimeout());
        msg.setSendTimestamp(TimeSync.getGlobalTime());
        msg.setSender(null);
        if (cell.isRequestMasteEpoch())
            msg.setMasterEpochNumber(cell.getMasterEpochNumber());
        cell.setMessageSent(msg);

        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,
                    "P start ACCEPT: %s",
                    msg.toString());
        }

        for (int i = 0; i < numRemoteAcc; i++) {
            try {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,
                            "P send accept to: %s",
                            cell.getAcceptors().get(i));
                }
                Thread.yield();
                comm.sendMessage(msg, cell.getAcceptors().get(i));
            } catch (IOException ex) {
                //cancel(cell, ex, 0);
            }
        }

        FleaseMessage timer = new FleaseMessage(MsgType.EVENT_TIMEOUT_ACCEPT);
        timer.setCellId(cell.getCellId());
        timer.setProposalNo(cell.getBallotNo());
        comm.requestTimer(timer, TimeSync.getLocalSystemTime() + config.getRoundTimeout());

        final FleaseMessage localResponse = localAcceptor.handleACCEPT(msg);
        if (localResponse != null) {
            if ( (meHandler != null) && cell.isRequestMasteEpoch() && (localResponse.getMsgType() == FleaseMessage.MsgType.MSG_ACCEPT_ACK)) {
                meHandler.storeMasterEpoch(localResponse, new MasterEpochHandlerInterface.Continuation() {
                    @Override
                    public void processingFinished() {
                        localQueue.enqueueMessage(localResponse);
                    }
                });
            } else {
                processAcceptResponse(cell, localResponse);
            }
        }

    }

    protected void processAcceptResponse(FleaseProposerCell cell, FleaseMessage msg) {
        assert (cell.getCellState() == State.WAIT_FOR_ACCEPT_ACK);

        if (msg.getSendTimestamp() + config.getMessageTimeout() < TimeSync.getGlobalTime()) {
            //drop outdated message
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P ignore message (too old): %s",
                        msg.toString());
            }
            return;
        }

        if (msg.getSendTimestamp() > TimeSync.getGlobalTime() + config.getDMax()) {
            //drop outdated message
            Logging.logMessage(Logging.LEVEL_WARN, Logging.Category.replication, this, "RECEIVED MESSAGE WITH TIMESTAMP TOO FAR IN THE FUTURE (likely cause: clocks aren't in sync). SYSTEM IS NOT IN A SAFE STATE: %s",
                    msg.toString());
            cancel(cell, new FleaseException("System is not in sync (clock sync drift exceeded)!"), 0);
            return;
        }

        if (msg.before(cell.getMessageSent())) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P ignore message (before my request): %s",
                        msg.toString());
            }
            return;
        }

        if ((msg.getMsgType() != MsgType.MSG_ACCEPT_ACK) &&
                (msg.getMsgType() != MsgType.MSG_ACCEPT_NACK) &&
                (msg.getMsgType() != MsgType.MSG_WRONG_VIEW) &&
                (msg.getMsgType() != MsgType.EVENT_TIMEOUT_ACCEPT)) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P ignore message (unexpected message type): %s",
                        msg.toString());
            }
            return;
        }

        if (msg.getMsgType() == MsgType.EVENT_TIMEOUT_ACCEPT) {
            //not enough responses :( abort or re-try
            cancel(cell, new FleaseException("did not receive enough responses for ACCEPT"), 0);
            return;
        }

        final List<FleaseMessage> responses = cell.getResponses();
        responses.add(msg);
        if (cell.majorityAvail()) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P majority responded for proposal %s: %d",
                        cell.getBallotNo(), responses.size());
            }
            //analyze responses
            int maxViewId = 0;
            ProposalNumber maxBallot = ProposalNumber.EMPTY_PROPOSAL_NUMBER;
            for (FleaseMessage resp : responses) {
                if (resp.getViewId() > maxViewId) {
                    maxViewId = resp.getViewId();
                } else if (resp.getMsgType() == MsgType.MSG_ACCEPT_NACK) {
                    maxBallot = resp.getPrevProposalNo();
                }
            }

            if (maxViewId != cell.getViewId()) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P accept failed due to outdated view local=%d max=%",
                            cell.getViewId(), maxViewId);
                }
                viewListener.viewIdChangeEvent(cell.getCellId(), maxViewId);
                cancel(cell, new FleaseException("local viewId is outdated"), 0);
                return;
            }

            //take action
            if (!maxBallot.isEmpty()) {
                cell.setBallotNo(new ProposalNumber(maxBallot.getProposalNo() + (int) (Math.random() * 10) + 1, cell.getBallotNo().getSenderId()));
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this, "P accept  OVERRULED by %s, restart with ballot number: %s",
                            maxBallot.getProposalNo(), cell.getBallotNo());
                }
                //request a timer event for restart
                cancel(cell, new FleaseException("proposal was overruled by remote proposal during ACCEPT"), 0);
                return;
            }



            responses.clear();

            learn(cell);

        }

    }

    public void learn(FleaseProposerCell cell) {
        cell.setCellState(State.IDLE);


        final int numRemoteAcc = cell.getNumAcceptors();
        FleaseMessage msg = new FleaseMessage(FleaseMessage.MsgType.MSG_LEARN);
        msg.setCellId(cell.getCellId());
        msg.setProposalNo(cell.getBallotNo());
        msg.setLeaseHolder(cell.getMessageSent().getLeaseHolder());
        msg.setLeaseTimeout(cell.getMessageSent().getLeaseTimeout());
        msg.setSendTimestamp(TimeSync.getGlobalTime());
        msg.setSender(null);
        if (cell.isRequestMasteEpoch()) {
            assert(cell.getMasterEpochNumber() > -1);
            msg.setMasterEpochNumber(cell.getMasterEpochNumber());
        }
        cell.setMessageSent(msg);


        //check the result...
        //if the lease is already outdated, we can immediately retry
        //if thg lease is in the grace period, we have to trigger a timer

        if (msg.hasTimedOut(config, TimeSync.getGlobalTime())) {
            //timed out
            //retry
            cell.setBallotNo(new ProposalNumber(cell.getBallotNo().getProposalNo() + 1, cell.getBallotNo().getSenderId()));

            startPrepare(cell,cell.getMessageSent().getLeaseHolder());

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,
                        "P finished round, lease has timed out, restart prepare: %s",
                        msg.toString());
            }
            
        } else if (msg.hasNotTimedOut(config, TimeSync.getGlobalTime())) {
            //lease is valid, do learn step

            cell.setBallotNo(new ProposalNumber(cell.getBallotNo().getProposalNo() + 1, cell.getBallotNo().getSenderId()));

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,
                        "P finished round, lease is valid: %s",
                        msg.toString());
            }

            //only here does a learn make sense
            if (config.isSendLearnMessages()) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,
                            "P start LEARN: %s",
                            msg.toString());
                }

                for (int i = 0; i < numRemoteAcc; i++) {
                    try {
                        comm.sendMessage(msg, cell.getAcceptors().get(i));
                    } catch (IOException ex) {
                        //ignore them here
                    }
                }
            }

            //FIXME: local message
            //msgs[numRemoteAcc] = new FleaseMessage(msg);
            //msgs[numRemoteAcc].setSender(null);
            localAcceptor.handleLEARN(msg);

            if (!FleaseStage.DISABLE_RENEW_FOR_TESTING && cell.getMessageSent().getLeaseHolder().equals(config.getIdentity())) {
                //renew after half of the time
                //FIXE: could be relaxed
                final long renewTime = cell.getMessageSent().getLeaseTimeout() - config.getRoundTimeout() * 4;
                if (TimeSync.getGlobalTime() < renewTime) {
                    final FleaseMessage timer = new FleaseMessage(MsgType.EVENT_RENEW);
                    timer.setCellId(cell.getCellId());
                    timer.setProposalNo(cell.getBallotNo());

                    comm.requestTimer(timer, globalToLocalTime(renewTime));
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,"scheduled renew for %s",cell.getCellId());
                    }
                } else {
                    final int wait_ms = (int) (msg.getLeaseTimeout() - TimeSync.getGlobalTime() + config.getDMax());
                    Logging.logMessage(Logging.LEVEL_WARN, Logging.Category.replication, this,"too late to schedule renew for cell %s, restart in %d ms (now=%d, renew=%d)",
                            cell.getCellId(),wait_ms,TimeSync.getGlobalTime(),renewTime);
                    //schedule a retry instead
                    cancel(cell, new FleaseException("too late for renew, re-start after lease has timed out in "+wait_ms+"ms"), wait_ms);
                }
            }

        } else {
            //grace period
            final int wait_ms = (int) (msg.getLeaseTimeout() - TimeSync.getGlobalTime() + config.getDMax());
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Logging.Category.replication, this,
                        "P finished round, lease is in grace period, scheduled restart in %d ms: %s",
                        wait_ms, msg.toString());
            }


            cancel(cell, new FleaseException("current lease not yet timed out"), wait_ms);


        }


    }

    public long globalToLocalTime(long globalTime) {
        return globalTime-TimeSync.getInstance().getDrift();
    }

    public void cancel(FleaseProposerCell cell, Throwable reason, long retryAfter_ms) {
        int numFailures = cell.getNumFailures() + 1;
        cell.setNumFailures(numFailures);
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.replication, this, "P proposal failed for cell %s: %s", cell.getCellId(), reason);
        }
        cell.setCellState(State.IDLE);
        cell.setMessageSent(null);
        cell.setBallotNo(new ProposalNumber(cell.getBallotNo().getProposalNo() + 1, cell.getBallotNo().getSenderId()));
        cell.getResponses().clear();

        if (numFailures > config.getMaxRetries()) {
            FleaseException error;
            try {
                error = (FleaseException) reason;
            } catch (ClassCastException ex) {
                error = new FleaseException("internal flease error", reason);
            }
            leaseListener.leaseFailed(cell.getCellId(), error);
            //full lease timeout wait here...
            cell.setNumFailures(0);

            FleaseMessage timer = new FleaseMessage(MsgType.EVENT_RESTART);
            timer.setCellId(cell.getCellId());
            timer.setProposalNo(cell.getBallotNo());

            comm.requestTimer(timer, TimeSync.getLocalSystemTime() + config.getMaxLeaseTimeout());

        } else {
            FleaseMessage timer = new FleaseMessage(MsgType.EVENT_RESTART);
            final int retryTmp = (int) (retryAfter_ms > 0 ? retryAfter_ms : 50 + (int) (Math.random() * 100));
            timer.setCellId(cell.getCellId());
            timer.setProposalNo(cell.getBallotNo());

            comm.requestTimer(timer, TimeSync.getLocalSystemTime() + retryTmp);

        }
    }

    
}
