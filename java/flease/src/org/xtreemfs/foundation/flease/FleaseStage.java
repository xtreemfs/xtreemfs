/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ASCIIString;
import org.xtreemfs.foundation.flease.acceptor.FleaseAcceptor;
import org.xtreemfs.foundation.flease.acceptor.FleaseAcceptorCell;
import org.xtreemfs.foundation.flease.acceptor.LearnEventListener;
import org.xtreemfs.foundation.flease.comm.FleaseCommunicationInterface;
import org.xtreemfs.foundation.flease.comm.FleaseMessage;
import org.xtreemfs.foundation.flease.proposer.FleaseException;
import org.xtreemfs.foundation.flease.proposer.FleaseListener;
import org.xtreemfs.foundation.flease.proposer.FleaseLocalQueueInterface;
import org.xtreemfs.foundation.flease.proposer.FleaseProposer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 *
 * @author bjko
 */
public class FleaseStage extends LifeCycleThread implements LearnEventListener, FleaseLocalQueueInterface {

    public static final String FLEASE_VERSION = "0.2.4 (trunk)";

    public static final int TIMER_INTERVAL_IN_MS = 50;

    public static final boolean ENABLE_TIMEOUT_EVENTS = true;

    public static final boolean DISABLE_RENEW_FOR_TESTING = false;

    public static final boolean COLLECT_STATISTICS = false;

    private final FleaseProposer proposer;

    private final FleaseAcceptor acceptor;

    private final PriorityQueue<TimerEntry> timers;

    private final PriorityQueue<Flease> leaseTimeouts;

    private final LinkedBlockingQueue messages;

    private volatile boolean quit;

    private long lastTimerRun;

    private final FleaseConfig config;

    private final FleaseMessageSenderInterface sender;

    public static final int MAX_BATCH_SIZE = 20;

    private final FleaseStatusListener leaseListener;

    private final AtomicReference<List<Integer>> durRequests, durMsgs, durTimers;

    private final AtomicInteger                  inRequests, inMsgs, inTimers, outMsgs;

    private final FleaseStats                    statThr;

    private final MasterEpochHandlerInterface    meHandler;

    /**
     * Creates a new instance of Flease.
     * @param config flease configuration used for all cells and leases.
     * @param lockfileDir a lockfile for this flease instance is created in this directory.
     * @param sender interface to send flease messages to other flease nodes.
     * @param ignoreLockForTesting should only be used by unit tests.
     * @param viewListener not used at the moment, can be null.
     * @param leaseListener listener is notified when the lease changes for any open cell.
     * @param meHandler handler for storing/retrieving master epochs, can be null.
     * @throws IOException
     */
    public FleaseStage(FleaseConfig config, String lockfileDir,
            final FleaseMessageSenderInterface sender, boolean ignoreLockForTesting,
            final FleaseViewChangeListenerInterface viewListener, final FleaseStatusListener leaseListener,
            final MasterEpochHandlerInterface meHandler) throws IOException {
        super("FleaseSt");
        assert (sender != null);
        assert(leaseListener != null);

        timers = new PriorityQueue<TimerEntry>();
        messages = new LinkedBlockingQueue();
        quit = false;
        this.config = config;
        this.leaseListener = leaseListener;
        this.meHandler = meHandler;

        acceptor = new FleaseAcceptor(this, config, lockfileDir, ignoreLockForTesting);
        proposer = new FleaseProposer(config, acceptor, new FleaseCommunicationInterface() {

            public void sendMessage(FleaseMessage msg, InetSocketAddress receiver) throws IOException {
                sender.sendMessage(msg, receiver);
            }

            public void requestTimer(FleaseMessage msg, long timestamp) {
                createTimer(msg, timestamp);
            }
        }, leaseListener, this, this,meHandler);
        acceptor.setViewChangeListener(viewListener);
        proposer.setViewChangeListener(viewListener);
        this.sender = sender;

        leaseTimeouts = new PriorityQueue<Flease>(1000, new Comparator<Flease>() {

            public int compare(Flease o1, Flease o2) {
                return (int) (o1.getLeaseTimeout_ms() - o2.getLeaseTimeout_ms());
            }
        });
        if (COLLECT_STATISTICS) {
            durRequests = new AtomicReference(new LinkedList());
            durTimers = new AtomicReference(new LinkedList());
            durMsgs = new AtomicReference(new LinkedList());
            inRequests = new AtomicInteger();
            inTimers = new AtomicInteger();
            inMsgs = new AtomicInteger();
            outMsgs = new AtomicInteger();
            statThr = new FleaseStats(this, lockfileDir+"/flease.stats");
        } else {
            durRequests = null;
            durTimers = null;
            durMsgs = null;
            outMsgs = null;
            inRequests = null;
            inTimers = null;
            inMsgs = null;
            statThr = null;
        }
    }

    public ASCIIString getIdentity() {
        return config.getIdentity();
    }

    @Deprecated
    public FleaseFuture openCell(ASCIIString cellId, List<InetSocketAddress> acceptors, boolean requestMasterEpoch) {
        return openCell(cellId, acceptors, requestMasterEpoch, 0);
    }

    /**
     * Opens a cell. The leaseListener will be notified of all lease events for this cell. The local flease
     * instance will try to acquire the lease.
     * 
     * @param cellId
     *            unique ID of the cell to open.
     * @param acceptors
     *            list of remote flease instances, do not include local flease instance.
     * @param requestMasterEpoch
     *            if true, a master epoch will be requested when the local instance is lease owner.
     * @param viewId
     *            the current view id to open the cell with.
     * @return
     */
    public FleaseFuture openCell(ASCIIString cellId, List<InetSocketAddress> acceptors, boolean requestMasterEpoch,
            int viewId) {
        FleaseFuture f = new FleaseFuture();
        Request rq = new Request(Request.RequestType.OPEN_CELL_REQUEST);
        rq.cellId = cellId;
        rq.acceptors = acceptors;
        rq.listener = f;
        rq.requestME = requestMasterEpoch;
        rq.viewId = viewId;

        if (COLLECT_STATISTICS)
            inRequests.incrementAndGet();

        this.messages.add(rq);
        return f;
    }

    public void batchOpenCells(ASCIIString[] cellIds, List<InetSocketAddress>[] acceptors, boolean requestMasterEpoch) {

        List<Request> batch = new ArrayList(cellIds.length);
        for (int i =0; i < cellIds.length; i++) {
            Request rq = new Request(Request.RequestType.OPEN_CELL_REQUEST);
            rq.cellId = cellIds[i];
            rq.acceptors = acceptors[i];
            rq.listener = null;
            rq.requestME = requestMasterEpoch;
            batch.add(rq);
        }
        
        for (int i = 0; i < batch.size(); i += MAX_BATCH_SIZE) {
            int endIndex = i+MAX_BATCH_SIZE;
            if (endIndex > batch.size()-1)
                endIndex = batch.size();
            List<Request> sublist = batch.subList(i, endIndex);

            if (COLLECT_STATISTICS)
                inRequests.addAndGet(sublist.size());

            this.messages.addAll(sublist);
        }

    }

    /**
     * Closes a cell which must be open. If the local instance is the current lease owner,
     * the lease will not be renewed.
     * @param cellId
     * @param returnLease if true, the lease will immediately be released.
     * @return
     */
    public FleaseFuture closeCell(ASCIIString cellId, boolean returnLease) {
        // TODO(bjko): return lease, check if not_owner event is triggered.
        FleaseFuture f = new FleaseFuture();
        Request rq = new Request(Request.RequestType.CLOSE_CELL_REQUEST);
        rq.cellId = cellId;
        rq.listener = f;

        if (COLLECT_STATISTICS)
            inRequests.incrementAndGet();

        this.messages.add(rq);
        return f;
    }

    public void setViewId(ASCIIString cellId, int viewId, FleaseListener listener) {
        Request rq = new Request(Request.RequestType.SET_VIEW);
        rq.cellId = cellId;
        rq.viewId = viewId;
        rq.listener = listener;

        if (COLLECT_STATISTICS)
            inRequests.incrementAndGet();

        this.messages.add(rq);
    }

    public Map<ASCIIString, FleaseMessage> getLocalState() throws InterruptedException {
        final Request rq = new Request(Request.RequestType.GET_STATE);
        final Map[] map = new Map[1];
        rq.cback = new FleaseStateCallback() {

            public void localStateResult(Map<ASCIIString, FleaseMessage> state) {
                synchronized (rq) {
                    map[0] = state;
                    rq.notifyAll();
                }
            }
        };
        this.messages.add(rq);
        synchronized (rq) {
            if (map[0] == null) {
                rq.wait();
            }
            return map[0];
        }
    }

    public void receiveMessage(FleaseMessage msg) {
        assert (msg.getSender() != null);

        if (COLLECT_STATISTICS)
            inMsgs.incrementAndGet();

        this.messages.add(msg);
    }

    public FleaseMessage _test_get_local_lease_state(ASCIIString cellId) {
        return acceptor.getLocalLeaseInformation(cellId);
    }

    public String _dump_acceptor_state(ASCIIString cellId) {
        FleaseAcceptorCell cell = acceptor.cells.get(cellId);
        if (cell == null) {
            return cellId + ": does not exist";
        } else {
            return cellId + ": " + cell.toString();
        }

    }

    public void learnedEvent(ASCIIString cellId, ASCIIString leaseHolder, long leaseTimeout_ms, long masterEpochNumber) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.flease, this,"learned event: "+leaseHolder+"/"+leaseTimeout_ms);
        }
        Flease newFlease = new Flease(cellId, leaseHolder, leaseTimeout_ms, masterEpochNumber);
        Flease oldFlease = proposer.updatePrevLeaseForCell(cellId, newFlease);
        if (oldFlease != null) {
            if (oldFlease.isValid()) {
                if (!oldFlease.isSameLeaseHolder(newFlease)) {
                    Logging.logMessage(
                            Logging.LEVEL_DEBUG,
                            Category.flease,
                            this,
                            "New lease replaced old lease which is still valid according to this OSD's clocks. Make sure all OSD clocks are synchronized. New Lease: %s Old Lease: %s",
                            newFlease, oldFlease);
                }
            }
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.flease, this,"lease state change: %s %s %d",cellId,leaseHolder,leaseTimeout_ms);
            }
            leaseListener.statusChanged(cellId, newFlease);
            if (ENABLE_TIMEOUT_EVENTS) {
                leaseTimeouts.remove(oldFlease);
                leaseTimeouts.add(newFlease);
            }
        }
    }

    @Override
    public void run() {

        if (COLLECT_STATISTICS)
            statThr.start();

        Logging.logMessage(Logging.LEVEL_INFO, Category.flease, this, "Flease (version %s) ready", FLEASE_VERSION);

        notifyStarted();

        // interval to check the OFT

        long nextTimerRunInMS = TIMER_INTERVAL_IN_MS;
        lastTimerRun = 0;

        List<Object> rqList = new ArrayList(1000);

        while (!quit) {
            try {
                final Object tmp = messages.poll(nextTimerRunInMS, TimeUnit.MILLISECONDS);

                if (quit) {
                    break;
                }

                if ((tmp == null) ||
                        (TimeSync.getLocalSystemTime() >= lastTimerRun + nextTimerRunInMS)) {
                    if (ENABLE_TIMEOUT_EVENTS) {
                        //nextTimerRunInMS =
                        checkTimers();
                        checkLeaseTimeouts();
                    } else {
                        nextTimerRunInMS = checkTimers();
                    }
                    lastTimerRun = TimeSync.getLocalSystemTime();
                }
                if (tmp == null) {
                    continue;
                }

                rqList.add(tmp);
                messages.drainTo(rqList, 25);
                /*final int numItems = messages.poll(rqList, 25, nextTimerRunInMS);
                if ((numItems == 0) ||
                (TimeSync.getLocalSystemTime() >= lastTimerRun+nextTimerRunInMS)) {
                nextTimerRunInMS = checkTimers();
                lastTimerRun = TimeSync.getLocalSystemTime();
                }
                if (numItems == 0)
                continue;*/

                while (!rqList.isEmpty()) {

                    final Object request = rqList.remove(rqList.size() - 1);

                    long rqStart;
                    if (COLLECT_STATISTICS) {
                        rqStart = System.nanoTime();
                    }
                    if (request instanceof FleaseMessage) {
                        final FleaseMessage msg = (FleaseMessage) request;

                        if (msg.isInternalEvent()) {
                            //should never happen!
                            Logging.logMessage(Logging.LEVEL_ERROR, Category.flease, this, "received internal event: %s", msg);
                        } else if (msg.isAcceptorMessage()) {
                            final FleaseMessage response = acceptor.processMessage(msg);
                            if (response != null) {
                                if (msg.getMasterEpochNumber() == FleaseMessage.REQUEST_MASTER_EPOCH
                                        && response.getMsgType() == FleaseMessage.MsgType.MSG_PREPARE_ACK) {
                                    // Respond with the current master epoch.
                                    if (meHandler != null) {
                                        MasterEpochHandlerInterface.Continuation cont = new MasterEpochHandlerInterface.Continuation() {
                                            @Override
                                            public void processingFinished() {
                                                sender.sendMessage(response, msg.getSender());
                                            }
                                        };
                                        meHandler.sendMasterEpoch(response, cont);
                                    } else {
                                        Logging.logMessage(Logging.LEVEL_ERROR, Category.flease, this,
                                                "MASTER EPOCH WAS REQUESTED, BUT NO MASTER EPOCH HANDLER DEFINED!!!");
                                        sender.sendMessage(response, msg.getSender());
                                    }
                                } else if (msg.getMasterEpochNumber() != FleaseMessage.IGNORE_MASTER_EPOCH
                                        && response.getMsgType() == FleaseMessage.MsgType.MSG_ACCEPT_ACK) {
                                    // Write the current master epoch to disk.
                                    if (meHandler != null) {
                                        MasterEpochHandlerInterface.Continuation cont = new MasterEpochHandlerInterface.Continuation() {
                                            @Override
                                            public void processingFinished() {
                                                sender.sendMessage(response, msg.getSender());
                                            }
                                        };
                                        meHandler.storeMasterEpoch(response, cont);
                                    }
                                } else {
                                    sender.sendMessage(response, msg.getSender());
                                }
                            }
                        } else {
                            proposer.processMessage(msg);
                        }
                        if (COLLECT_STATISTICS) {
                            long rqEnd = System.nanoTime();
                            durMsgs.get().add(Integer.valueOf((int)(rqEnd-rqStart)));
                            outMsgs.incrementAndGet();
                        }
                    } else {
                        Request rq = (Request) request;
                        switch (rq.type) {
                            case OPEN_CELL_REQUEST: {
                                assert (rq.acceptors != null);
                                try {
                                proposer.openCell(rq.cellId, rq.acceptors, rq.requestME, rq.viewId);
                                acceptor.setViewId(rq.cellId, rq.viewId);
                                    if (rq.listener != null)
                                        rq.listener.proposalResult(rq.cellId, null, 0, FleaseMessage.IGNORE_MASTER_EPOCH);
                                } catch (FleaseException ex) {
                                    Logging.logError(Logging.LEVEL_DEBUG, this, ex);
                                    leaseListener.leaseFailed(rq.cellId, ex);
                                }
                                break;
                            }
                            case CLOSE_CELL_REQUEST: {
                                proposer.closeCell(rq.cellId);
                                rq.listener.proposalResult(rq.cellId, null, 0, FleaseMessage.IGNORE_MASTER_EPOCH);
                                break;
                            }
                            case HANDOVER_LEASE: {
                                try {
                                    Flease prevLease = proposer.updatePrevLeaseForCell(rq.cellId, Flease.EMPTY_LEASE);
                                    if (prevLease != null) {
                                        //cancel the lease
                                        leaseTimeouts.remove(prevLease);
                                    }
                                    proposer.handoverLease(rq.cellId, rq.newLeaseOwner);
                                } catch (FleaseException ex) {
                                    rq.listener.proposalFailed(rq.cellId, ex);
                                }
                                break;
                            }
                            case SET_VIEW: {
                                proposer.setViewId(rq.cellId, rq.viewId);
                                acceptor.setViewId(rq.cellId, rq.viewId);
                                rq.listener.proposalResult(rq.cellId, null, 0, FleaseMessage.IGNORE_MASTER_EPOCH);
                                break;
                            }
                            case GET_STATE: {
                                try {
                                    rq.cback.localStateResult(acceptor.localState());
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                        if (COLLECT_STATISTICS) {
                            long rqEnd = System.nanoTime();
                            durRequests.get().add(Integer.valueOf((int)(rqEnd-rqStart)));
                        }
                    }
                    
                }
                if (DISABLE_RENEW_FOR_TESTING) {
                    Thread.sleep(0, 2);
                }

            } catch (InterruptedException ex) {
                if (quit) {
                    break;
                }
            } catch (Throwable ex) {
                notifyCrashed(ex);
                break;
            }
        }

        acceptor.shutdown();
        notifyStopped();
        Logging.logMessage(Logging.LEVEL_INFO, Category.flease, this, "Flease stopped", FLEASE_VERSION);
    }

    public void shutdown() {
        if (COLLECT_STATISTICS)
            statThr.shutdown();
        Logging.logMessage(Logging.LEVEL_DEBUG, Category.flease, this, "received shutdown call...");
        quit = true;
        this.interrupt();
    }

    private int checkTimers() throws Throwable {
        final long now = TimeSync.getLocalSystemTime();

        TimerEntry e = timers.peek();
        if (e == null) {
            return TIMER_INTERVAL_IN_MS;
        }
        if (e.getScheduledTime() <= now + TIMER_INTERVAL_IN_MS) {
            //execute timer
            
            do {
                e = timers.poll();

                long rqStart;
                if (COLLECT_STATISTICS) {
                    rqStart = System.nanoTime();
                    inTimers.incrementAndGet();
                }
                if (e.getScheduledTime() < now) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.flease, this, "event sent after deadline: %s",
                            e.message);
                }
                e.getMessage().setSendTimestamp(TimeSync.getGlobalTime());
                proposer.processMessage(e.getMessage());

                if (COLLECT_STATISTICS) {
                    long rqEnd = System.nanoTime();
                    durTimers.get().add(Integer.valueOf((int)(rqEnd-rqStart)));
                }

                e = timers.peek();
                if (e == null) {
                    return TIMER_INTERVAL_IN_MS;
                }
            } while (e.getScheduledTime() <= now + TIMER_INTERVAL_IN_MS);
            
            return (int) (e.getScheduledTime() - now);
        } else {
            //tell how long we have to wait
            return (int) (e.getScheduledTime() - now);
        }
    }

    private void checkLeaseTimeouts() {
        final long now = TimeSync.getGlobalTime();
        final long deadline = now + TIMER_INTERVAL_IN_MS + TimeSync.getLocalRenewInterval()+config.getToNotification_ms();

        Flease f = leaseTimeouts.peek();
        if (f == null)
            return;
        if (f.getLeaseTimeout_ms() <= deadline) {
            //execute timer
            do {
                f = leaseTimeouts.poll();

                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.flease, this,"lease state change: %s timed out (old lease: %s)",f.getCellId(),f.toString());
                }
                proposer.updatePrevLeaseForCell(f.getCellId(), f.EMPTY_LEASE);
                leaseListener.statusChanged(f.getCellId(), Flease.EMPTY_LEASE);
                //create restart event
                FleaseMessage restartEvt = new FleaseMessage(FleaseMessage.MsgType.EVENT_RESTART);
                restartEvt.setCellId(f.getCellId());
                restartEvt.setProposalNo(proposer.getCurrentBallotNo(f.getCellId()));
                createTimer(restartEvt, TimeSync.getLocalSystemTime() + config.getDMax());

                f = leaseTimeouts.peek();
                if (f == null) {
                    return;
                }
            } while (f.getLeaseTimeout_ms() <= deadline);
        }
    }

    protected void createTimer(FleaseMessage msg, long timestamp) {
        msg.validateMessage();
        TimerEntry e = new TimerEntry(timestamp, msg);
        timers.add(e);
    }

    int getInRequests() {
        return this.inRequests.getAndSet(0);
    }

    int getInMessages() {
        return this.inMsgs.getAndSet(0);
    }

    int getOutMessages() {
        return this.outMsgs.getAndSet(0);
    }

    int getInTimers() {
        return this.inTimers.getAndSet(0);
    }

    List<Integer> getRequestDurations() {
        return durRequests.getAndSet(new LinkedList());
    }

    List<Integer> getMessageDurations() {
        return durMsgs.getAndSet(new LinkedList());
    }

    List<Integer> getTimersDurations() {
        return durTimers.getAndSet(new LinkedList());
    }

    @Override
    public void enqueueMessage(FleaseMessage message) {
        messages.add(message);
    }

    private final static class TimerEntry implements Comparable {

        private final long scheduledTime;

        private final FleaseMessage message;

        public TimerEntry(long scheduledTime, FleaseMessage message) {
            this.scheduledTime = scheduledTime;
            this.message = message;
        }

        /**
         * @return the scheduledTime
         */
        public long getScheduledTime() {
            return scheduledTime;
        }

        public FleaseMessage getMessage() {
            return this.message;
        }

        public int compareTo(Object o) {
            TimerEntry e2 = (TimerEntry) o;
            return (int) (this.scheduledTime - e2.scheduledTime);
        }
    }

    private final static class Request {

        public boolean autoRenew;
        public boolean requestME;

        public enum RequestType {

            OPEN_CELL_REQUEST,
            CLOSE_CELL_REQUEST,
            GET_LEASE_REQUEST,
            RETURN_LEASE_REQUEST,
            HANDOVER_LEASE,
            GET_STATE,
            SET_VIEW

        };
        public final RequestType type;

        public ASCIIString cellId;

        public ASCIIString newLeaseOwner; //< for handover only!

        public List<InetSocketAddress> acceptors;

        public FleaseListener listener;

        public int viewId;

        public FleaseStateCallback cback;

        public Request(RequestType type) {
            this.type = type;
        }
    }

    private static interface FleaseStateCallback {

        public void localStateResult(Map<ASCIIString, FleaseMessage> state);
    }
}
