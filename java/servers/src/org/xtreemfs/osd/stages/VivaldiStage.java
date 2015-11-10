/*
 * Copyright (c) 2008-2011 by Juan Gonzalez de Benito, Bjoern Kolbeck,
 *               Barcelona Supercomputing Center, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.stages;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.common.uuids.Mapping;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.dir.DIRClient;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.MessageType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.server.UDPMessage;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.vivaldi.VivaldiNode;
import org.xtreemfs.osd.vivaldi.ZipfGenerator;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.xtreemfs_pingMesssage;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

/**
 * Stage used in the OSD to manage Vivaldi.
 *
 * @author Juan Gonzalez de Benito (BSC) & Björn Kolbeck (ZIB)
 */
public class VivaldiStage extends Stage {

    private static final int STAGEOP_ASYNC_PING = 1;
    private static final int STAGEOP_SYNC_PING = 2;
    /**
     * System time when the timer was last executed.
     */
    private long lastCheck;
    /**
     * The main OSDRequestDispatcher used by the OSD.
     */
    private final OSDRequestDispatcher master;
    /**
     * Client used to communicate with the Directory Service.
     */
    private final DIRClient dirClient;
    /**
     * List of already sent Vivaldi requests.
     */
    private HashMap<InetSocketAddress, SentRequest> sentRequests;
    /**
     * List of simultaneous VivaldiRetrys. Each of them keep track of the RTTs measured for the same node.
     */
    private HashMap<InetSocketAddress, VivaldiRetry> toBeRetried;
    /**
     * The node that includes the Vivaldi information of the OSD.
     */
    private VivaldiNode vNode;
    /**
     * Number of milliseconds until the next Vivaldi recalculation
     */
    private long nextRecalculationInMS;
    /**
     * Number of milliseconds until the next timer execution
     */
    private long nextTimerRunInMS;
    /**
     * List of existent OSDs. The OSD contact them to get their Vivaldi information
     * and use it to recalculate its position
     */
    private LinkedList<KnownOSD> knownOSDs;
    /**
     * Number of elapsed Vivaldi iterations
     */
    private long vivaldiIterations;
    private ZipfGenerator rankGenerator;
    private final double ZIPF_GENERATOR_SKEW = 0.5; // TODO(mno): move to global config?! (depends on keeping ZIPF or not)
    /**
     * Number of retries to be sent before accepting 'suspiciously high' RTT
     */
    private final int MAX_RETRIES_FOR_A_REQUEST;
    /**
     * Recalculation interval.
     *
     * The recalculation period is randomly determined and lies within:
     * [RECALCULATION_INTERVAL - RECALCULATION_EPSILON, RECALCULATION_INTERVAL + RECALCULATION_EPSILON]
     */
    private final int RECALCULATION_INTERVAL;
    /**
     * Recalculation epsilon.
     *
     * The recalculation period is randomly determined and lies within:
     * [RECALCULATION_INTERVAL - RECALCULATION_EPSILON, RECALCULATION_INTERVAL + RECALCULATION_EPSILON]
     */
    private final int RECALCULATION_EPSILON;
    /**
     * Number of times the node recalculates its position before updating
     * its list of existent OSDs.
     */
    private final int ITERATIONS_BEFORE_UPDATING;
    /**
     * Maximum number of milliseconds an OSD waits for a RESPONSE before discarding
     * its corresponding REQUEST. Expiration times under {@code TIMER_INTERVAL_IN_MS}
     * are not granted.
     */
    private final int MAX_REQUEST_TIMEOUT_IN_MS;
    /**
     * Period of time between timer executions.
     */
    private final int TIMER_INTERVAL_IN_MS;

    public VivaldiStage(OSDRequestDispatcher master, int maxRequestsQueueLength) {
        super("VivaldiSt", maxRequestsQueueLength);
        this.master = master;
        this.dirClient = master.getDIRClient();

        this.sentRequests = new HashMap<InetSocketAddress, SentRequest>();
        this.toBeRetried = new HashMap<InetSocketAddress, VivaldiRetry>();
        this.vNode = new VivaldiNode();

        MAX_RETRIES_FOR_A_REQUEST = master.getConfig().getVivaldiMaxRetriesForARequest();
        RECALCULATION_INTERVAL = master.getConfig().getVivaldiRecalculationInterval();
        RECALCULATION_EPSILON = master.getConfig().getVivaldiRecalculationEpsilon();
        ITERATIONS_BEFORE_UPDATING = master.getConfig().getVivaldiIterationsBeforeUpdating();
        MAX_REQUEST_TIMEOUT_IN_MS = master.getConfig().getVivaldiMaxRequestTimeout();
        TIMER_INTERVAL_IN_MS = master.getConfig().getVivaldiTimerInterval();
        
        //TOFIX: should  the coordinates be initialized from a file?
        if (Logging.isDebug()) {
            Logging.logMessage(
                    Logging.LEVEL_DEBUG,
                    this,
                    String.format("Coordinates initialized:(%.3f,%.3f)",
                    vNode.getCoordinates().getXCoordinate(),
                    vNode.getCoordinates().getYCoordinate()));
        }
        this.knownOSDs = null;
        this.rankGenerator = null;

        this.lastCheck = 0;
    }

    /**
     * The position of the node is recalculated from a list of previously measured RTTs. Even if the
     * resulting movement is too big, the node's coordinates will be modified.
     *
     * @param coordinatesJ Coordinates of the node where recalculating against. If the operation
     * has been triggered by a timeout, this parameter is {@code null}.
     *
     * @param availableRTTs List of RTTs measured after several retries.
     */
    private void forceVivaldiRecalculation(VivaldiCoordinates coordinatesJ, ArrayList<Long> availableRTTs) {

        //TOFIX:In this version, the recalculation is discarded when the last retry times out: (coordinatesJ==null)
        if ((coordinatesJ != null) && (availableRTTs.size() > 0)) {

            //Determine the minimum RTT of the whole sample
            long minRTT = availableRTTs.get(0);

            StringBuilder strbRTTs = new StringBuilder(Long.toString(minRTT));

            for (int i = 1; i < availableRTTs.size(); i++) {
                strbRTTs.append(",");
                strbRTTs.append(availableRTTs.get(i));

                if (availableRTTs.get(i) < minRTT) {
                    minRTT = availableRTTs.get(i);
                }
            }

            vNode.recalculatePosition(coordinatesJ, minRTT, true);
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG,
                        this,
                        String.format("Forced(%s):%d Viv:%.3f Own:(%.3f,%.3f) lE=%.3f Rem:(%.3f,%.3f) rE=%.3f",
                        strbRTTs.toString(),
                        minRTT,
                        VivaldiNode.calculateDistance(vNode.getCoordinates(), coordinatesJ),
                        vNode.getCoordinates().getXCoordinate(),
                        vNode.getCoordinates().getYCoordinate(),
                        vNode.getCoordinates().getLocalError(),
                        coordinatesJ.getXCoordinate(),
                        coordinatesJ.getYCoordinate(),
                        coordinatesJ.getLocalError()));
            }
        }
    }

    public void getVivaldiCoordinatesAsync(xtreemfs_pingMesssage coordinates, InetSocketAddress sender, OSDRequest request) {
        this.enqueueOperation(STAGEOP_ASYNC_PING, new Object[]{coordinates, sender}, request, null);
    }

    public void getVivaldiCoordinatesSync(xtreemfs_pingMesssage coordinates, OSDRequest request, VivaldiPingCallback listener) {
        this.enqueueOperation(STAGEOP_SYNC_PING, new Object[]{coordinates}, request, listener);
    }

    public static interface VivaldiPingCallback {

        public void coordinatesCallback(VivaldiCoordinates myCoordinates, ErrorResponse error);
    }

    @Override
    protected void processMethod(StageRequest method) {
        xtreemfs_pingMesssage msg = (xtreemfs_pingMesssage) method.getArgs()[0];

        if (method.getStageMethod() == STAGEOP_ASYNC_PING) {
            try {
                InetSocketAddress sender = (InetSocketAddress) method.getArgs()[1];
                if (msg.getRequestResponse()) {
                    RPCHeader.RequestHeader rqHdr = RPCHeader.RequestHeader.newBuilder().setAuthData(RPCAuthentication.authNone).setUserCreds(RPCAuthentication.userService).setInterfaceId(OSDServiceConstants.INTERFACE_ID).setProcId(OSDServiceConstants.PROC_ID_XTREEMFS_PING).build();
                    RPCHeader hdr = RPCHeader.newBuilder().setCallId(0).setMessageType(MessageType.RPC_REQUEST).setRequestHeader(rqHdr).build();
                    xtreemfs_pingMesssage response = xtreemfs_pingMesssage.newBuilder().setCoordinates(this.vNode.getCoordinates()).setRequestResponse(false).build();

                    // TODO(mno): Comment the following sleeps before committing. They are just for local testing with a simulated delay.
                    /*
                    if(master.getConfig().getUUID().toString().equals("test9-localhost-OSD") || sender.getPort() == 32649)
                        Thread.sleep(300);
                    else if (master.getConfig().getUUID().toString().equals("test8-localhost-OSD") || sender.getPort() == 32648)
                        Thread.sleep(150);
                    else
                        Thread.sleep(20);
                    */
                    
                    master.sendUDPMessage(hdr, response, sender);
                } else {
                    recalculateCoordinates(msg.getCoordinates(), sender);
                }
            } catch (Exception ex) {
                Logging.logError(Logging.LEVEL_WARN, this, ex);
            } finally {
                // free the request buffer, as it won't be freed otherwise
                // because the response is sent asynchronously
                method.getRequest().getRPCRequest().freeBuffers();
            }
        } else {
            VivaldiPingCallback callback = (VivaldiPingCallback) method.getCallback();
            callback.coordinatesCallback(this.vNode.getCoordinates(), null);
        }
    }

    protected void recalculateCoordinates(VivaldiCoordinates coordinatesJ, InetSocketAddress sender) {
        try {

            boolean coordinatesModified = false;

            SentRequest correspondingReq = sentRequests.remove(sender);

            if (correspondingReq != null) {

                //Calculate the RTT
                long now = System.currentTimeMillis();
                long estimatedRTT = now - correspondingReq.getSystemTime();


                //Two nodes will never be at the same position
                if (estimatedRTT == 0) {
                    estimatedRTT = 1;
                }

                //Recalculate Vivaldi
                VivaldiRetry prevRetry = toBeRetried.get(sender);
                boolean retriedTraceVar = false, forcedTraceVar = false;

                /* If MAX_RETRIES == 0 the coordinates must be recalculated without
                retrying*/
                boolean retryingDisabled = (MAX_RETRIES_FOR_A_REQUEST <= 0);

                if (!vNode.recalculatePosition(coordinatesJ, estimatedRTT, retryingDisabled)) {

                    //The RTT seems to be too big, so it might be a good idea to go for a retry

                    if (prevRetry == null) {
                        toBeRetried.put(sender, new VivaldiRetry(estimatedRTT));
                        retriedTraceVar = true;
                    } else {

                        prevRetry.addRTT(estimatedRTT);

                        prevRetry.setRetried(false);

                        if (prevRetry.numberOfRetries() > MAX_RETRIES_FOR_A_REQUEST) {

                            //Recalculate using the previous RTTs
                            forceVivaldiRecalculation(coordinatesJ, prevRetry.getRTTs());
                            coordinatesModified = true;

                            //Just for traceVar
                            forcedTraceVar = true;

                            toBeRetried.remove(sender);
                        } else {
                            retriedTraceVar = true;
                        }
                    }

                } else {

                    coordinatesModified = true;

                    if (prevRetry != null) {
                        /*The received RTT is correct but it has been necessary to retry
                        some request previously, so now our structures must be updated.*/
                        toBeRetried.remove(sender);
                    }
                }

                if (!forcedTraceVar && Logging.isDebug()) {
                    //TOFIX: Printing getHostName() without any kind of control could be dangerous (?)
                    Logging.logMessage(Logging.LEVEL_DEBUG,
                            this,
                            String.format("%s:%d Viv:%.3f Own:(%.3f,%.3f) lE=%.3f Rem:(%.3f,%.3f) rE=%.3f %s",
                            retriedTraceVar ? "RETRY" : "RTT",
                            estimatedRTT,
                            VivaldiNode.calculateDistance(vNode.getCoordinates(), coordinatesJ),
                            vNode.getCoordinates().getXCoordinate(),
                            vNode.getCoordinates().getYCoordinate(),
                            vNode.getCoordinates().getLocalError(),
                            coordinatesJ.getXCoordinate(),
                            coordinatesJ.getYCoordinate(),
                            coordinatesJ.getLocalError(),
                            sender.getHostName()));
                }

            }//there's not any previously registered request , so we just discard the response

            //Use coordinatesJ to update knownOSDs if possible
            if ((knownOSDs != null) && (!knownOSDs.isEmpty())) {

                //Check if the message has been sent by some of the knownOSDs
                String strAddress = sender.getHostName()
                        + ":"
                        + sender.getPort();

                int sendingOSD = 0;
                boolean OSDfound = false;

                //Look for the OSD that has sent the message and update its coordinates
                while ((!OSDfound) && (sendingOSD < knownOSDs.size())) {

                    if ((knownOSDs.get(sendingOSD).getStrAddress() != null)
                            && knownOSDs.get(sendingOSD).getStrAddress().equals(strAddress)) {

                        knownOSDs.get(sendingOSD).setCoordinates(coordinatesJ);
                        OSDfound = true;

                    } else {
                        sendingOSD++;
                    }
                }

                if (coordinatesModified) {
                    /*Client's position has been modified so we must
                     * re-sort knownOSDs accordingly to the new vivaldi distances*/

                    LinkedList<KnownOSD> auxOSDList = new LinkedList<KnownOSD>();

                    for (int i = knownOSDs.size() - 1; i >= 0; i--) {

                        KnownOSD insertedOSD = knownOSDs.get(i);

                        double insertedOSDDistance =
                                VivaldiNode.calculateDistance(insertedOSD.getCoordinates(),
                                this.vNode.getCoordinates());
                        int j = 0;
                        boolean inserted = false;

                        while ((!inserted) && (j < auxOSDList.size())) {
                            double prevOSDDistance =
                                    VivaldiNode.calculateDistance(auxOSDList.get(j).getCoordinates(),
                                    this.vNode.getCoordinates());

                            if (insertedOSDDistance <= prevOSDDistance) {
                                auxOSDList.add(j, insertedOSD);
                                inserted = true;
                            } else {
                                j++;
                            }
                        }
                        if (!inserted) {
                            auxOSDList.add(insertedOSD);
                        }

                    }

                    knownOSDs = auxOSDList;

                } else if (OSDfound) {

                    /* It's not necessary to resort the whole knownOSDs but only the OSD
                     * whose coordinates might have changed*/

                    KnownOSD kosd = knownOSDs.remove(sendingOSD);
                    kosd.setCoordinates(coordinatesJ);
                    double osdNewDistance =
                            VivaldiNode.calculateDistance(coordinatesJ, vNode.getCoordinates());

                    int i = 0;
                    boolean inserted = false;
                    while ((!inserted) && (i < knownOSDs.size())) {
                        double existingDistance =
                                VivaldiNode.calculateDistance(knownOSDs.get(i).getCoordinates(),
                                vNode.getCoordinates());
                        if (osdNewDistance <= existingDistance) {
                            knownOSDs.add(i, kosd);
                            inserted = true;
                        } else {
                            i++;
                        }
                    }

                    if (!inserted) {
                        knownOSDs.add(kosd);
                    }
                }
            }

        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    /**
     * Sends a message to a OSD requesting its coordinates.
     * (NOTE: the response is sent in processMethod(..).)
     *
     * @param osd Address of the OSD we want to contact.
     * @param myCoordinates Our own coordinates.
     */
    private void sendVivaldiRequest(InetSocketAddress osd, VivaldiCoordinates myCoordinates) {


        //It's not allowed to send two requests to the same OSD simultaneously
        if (sentRequests.get(osd) == null) {

            RPCHeader.RequestHeader rqHdr = RPCHeader.RequestHeader.newBuilder().
                    setAuthData(RPCAuthentication.authNone).
                    setUserCreds(RPCAuthentication.userService).
                    setInterfaceId(OSDServiceConstants.INTERFACE_ID).
                    setProcId(OSDServiceConstants.PROC_ID_XTREEMFS_PING).build();
            RPCHeader hdr = RPCHeader.newBuilder().setCallId(0).
                    setMessageType(MessageType.RPC_REQUEST).
                    setRequestHeader(rqHdr).build();

            
            xtreemfs_pingMesssage pingMsg = xtreemfs_pingMesssage.newBuilder().setCoordinates(myCoordinates).setRequestResponse(true).build();
            
            long systemTimeNow = System.currentTimeMillis();
            //getLocalSystemTime does not introduce such a big overhead, while currentTimeMillis is required to get the necessary precision.
            long localTimeNow = TimeSync.getLocalSystemTime();

            //If we're sending a request, we need to register it in our structures so we can process its response later
            sentRequests.put(osd, new SentRequest(localTimeNow, systemTimeNow));
            
            try {
                master.sendUDPMessage(hdr, pingMsg, osd);
            } catch (IOException ex) {
                Logging.logError(Logging.LEVEL_ERROR, this, ex);
            }
        }
    }

    /**
     * Keeps the list of sent requests updated, by eliminating those whose timeout
     * has expired.
     */
    private void maintainSentRequests() {

        final long localNow = TimeSync.getLocalSystemTime();
        ArrayList<InetSocketAddress> removedRequests = new ArrayList<InetSocketAddress>();

        //Check which requests have timed out
        for (InetSocketAddress reqKey : sentRequests.keySet()) {
            if (localNow >= sentRequests.get(reqKey).getLocalTime() + MAX_REQUEST_TIMEOUT_IN_MS) {
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "OSD times out: " + reqKey.getHostName());
                }
                removedRequests.add(reqKey);
            }
        }

        //Manage the timed out requests
        for (InetSocketAddress removed : removedRequests) {
            //Is it the first time the node times out?
            VivaldiRetry prevRetry = toBeRetried.get(removed);

            //The retry is marked as 'not retried' so it will have priority when sending the next request
            if (prevRetry == null) {
                toBeRetried.put(removed, new VivaldiRetry());
            } else {
                //Take note of the new time out
                prevRetry.addTimeout();
                prevRetry.setRetried(false);

                //We've already retried too many times, so it's time to recalculate with the available info
                if (prevRetry.numberOfRetries() > MAX_RETRIES_FOR_A_REQUEST) {
                    forceVivaldiRecalculation(null, prevRetry.getRTTs());
                    toBeRetried.remove(removed);
                }
            }
            sentRequests.remove(removed);
        }
    }

    /**
     * Performs some operations that have been defined to be executed periodically.
     *
     * The frequency of the timer is defined by the attribute {@code
     * TIMER_INTERVAL_IN_MS}
     */
    private void executeTimer() {
        master.updateVivaldiCoordinates(vNode.getCoordinates());  

        //Remove the requests that are not needed anymore
        maintainSentRequests();
    }

    public void receiveVivaldiMessage(UDPMessage msg) {
        enqueueOperation(STAGEOP_ASYNC_PING, new Object[]{msg}, null, null);
    }

    /**
     * Updates the list of known OSDs, from the data stored in the DS. This
     * function is responsible of keeping a list of OSDs used by the algorithm.
     */
    private void updateKnownOSDs() {
        try {
            ServiceSet receivedOSDs = dirClient.xtreemfs_service_get_by_type(null, RPCAuthentication.authNone, RPCAuthentication.userService, ServiceType.SERVICE_TYPE_OSD);

            //We need our own UUID, to discard its corresponding entry
            String ownUUID = master.getConfig().getUUID().toString();

            knownOSDs = new LinkedList<KnownOSD>();

            for (Service osd : receivedOSDs.getServicesList()) {

                //If it's not our own entry and the referred service is not offline
                if (!ownUUID.equals(osd.getUuid()) && osd.getLastUpdatedS() != 0) {

                    //Parse the coordinates provided by the DS
                    String strCoords = KeyValuePairs.getValue(osd.getData().getDataList(), "vivaldi_coordinates");


                    if (strCoords != null) {

                        //Calculate distance from the client to the OSD
                        VivaldiCoordinates osdCoords = VivaldiNode.stringToCoordinates(strCoords);
                        KnownOSD newOSD = new KnownOSD(osd.getUuid(), osdCoords);

                        double insertedOSDDistance =
                                VivaldiNode.calculateDistance(osdCoords, this.vNode.getCoordinates());

                        //Insert the new OSD accordingly to its vivaldi distance

                        int i = 0;

                        boolean inserted = false;

                        while ((!inserted) && (i < knownOSDs.size())) {
                            double oldOSDDistance =
                                    VivaldiNode.calculateDistance(knownOSDs.get(i).getCoordinates(),
                                    this.vNode.getCoordinates());

                            if (insertedOSDDistance <= oldOSDDistance) {
                                knownOSDs.add(i, newOSD);
                                inserted = true;
                            } else {
                                i++;
                            }
                        }
                        if (!inserted) {
                            knownOSDs.add(newOSD);
                        }
                    }
                }
            }

            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Updating list of known OSDs (size: " + knownOSDs.size() + ")");
            }

        } catch (Exception exc) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "Error while updating known OSDs: " + exc);
            //Create an empty OSDs set
            knownOSDs = new LinkedList<KnownOSD>();
        } finally {
            //Adapt the Zipf generator to the new sample
            if (rankGenerator == null) {
                rankGenerator = new ZipfGenerator(knownOSDs.size(), ZIPF_GENERATOR_SKEW);
            } else {
                rankGenerator.setSize(knownOSDs.size());
            }

            //Previous requests are discarded
            sentRequests.clear();
            toBeRetried.clear();
        }
    }

    /**
     * Executes one Vivaldi iteration. For each of these iterations, the algorithm
     * chooses one random node from the list of known OSDs and sends it a Vivaldi
     * request, to recalculate then the position of the OSD using the received information.
     */
    private void iterateVivaldi() {

        if (vivaldiIterations % ITERATIONS_BEFORE_UPDATING == 1) {
            updateKnownOSDs();


        } //Start recalculation
        if ((knownOSDs != null) && (!knownOSDs.isEmpty())) {

            //It's still necessary to retry some request
            if (toBeRetried.size() > 0) {
                for (InetSocketAddress addr : toBeRetried.keySet()) {

                    if (!toBeRetried.get(addr).hasBeenRetried()) {

                        if (Logging.isDebug()) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Retrying: " + addr.getHostName());
                        }

                        sendVivaldiRequest(addr, vNode.getCoordinates());
                        toBeRetried.get(addr).setRetried(true);
                    }
                }
            } else {

                //Choose a random OSD and send it a new request
                //int chosenIndex = (int)(Math.random()*knownOSDs.size());

                //if we get here, knownOSDs is not empty and therefore rankGenerator != null
                int chosenIndex = rankGenerator.next();

                KnownOSD chosenOSD = knownOSDs.get(chosenIndex);

                try {

                    //Get the corresponding InetAddress
                    ServiceUUID sUUID = new ServiceUUID(chosenOSD.getUUID());
                    sUUID.resolve(Schemes.SCHEME_PBRPCU);
                    InetSocketAddress osdAddr = null;
                    Mapping serviceMappings[] = sUUID.getMappings();

                    int mapIt = 0;

                    while ((osdAddr == null) && (mapIt < serviceMappings.length)) {

                        if (serviceMappings[mapIt].protocol.equals(Schemes.SCHEME_PBRPCU)) {

                            osdAddr = serviceMappings[mapIt].resolvedAddr;

                            if (Logging.isDebug()) {
                                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Recalculating against: " + chosenOSD.getUUID());
                            }

                            //Only at this point we known which InetAddress corresponds with which UUID
                            chosenOSD.setStrAddress(osdAddr.getAddress().getHostAddress()
                                    + ":"
                                    + osdAddr.getPort());

                            //After receiving the response, we will be able to recalculate
                            sendVivaldiRequest(osdAddr, vNode.getCoordinates());
                        }
                        mapIt++;
                    }

                } catch (UnknownUUIDException unke) {
                    Logging.logMessage(Logging.LEVEL_ERROR, this, "Unknown UUID: " + chosenOSD.getUUID());
                } catch (Exception e) {
                    Logging.logMessage(Logging.LEVEL_ERROR, this, "Error detected while iterating Vivaldi");
                }
            }
        }

        vivaldiIterations = (vivaldiIterations + 1) % Long.MAX_VALUE;
    }

    /**
     * Main function of the stage. It keeps polling request methods from the stage
     * queue and processing them. If there are no methods available, the function
     * blocks until a new request is enqueued or the defined timer expires.
     */
    @Override
    public void run() {

        notifyStarted();

        vivaldiIterations = 0;

        long pollTimeoutInMS;

        nextRecalculationInMS = -1;
        nextTimerRunInMS = -1;

        while (!quit) {
            try {
                pollTimeoutInMS = checkTimer();
                final StageRequest op = q.poll(pollTimeoutInMS, TimeUnit.MILLISECONDS);
                if (op != null) {
                    processMethod(op);
                }
            } catch (InterruptedException ex) {
                break;
            } catch (Throwable ex) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, "Error detected: " + ex);
                notifyCrashed(ex);
                break;
            }
        }
        notifyStopped();
    }

    /**
     * Checks if the main timer or the last recalculation period have expired
     * and executes, depending on the case, the {@code executeTimer}
     * function or a new Vivaldi iteration.
     *
     * @return the number of milliseconds before executing a new checking.
     */
    private long checkTimer() {

        final long now = TimeSync.getLocalSystemTime();

        //Elapsed time since last check


        long elapsedTime = lastCheck > 0 ? (now - lastCheck) : 0;

        lastCheck = now;

        nextRecalculationInMS -= elapsedTime;
        nextTimerRunInMS -= elapsedTime;

        //Need to execute our timer


        if (nextTimerRunInMS <= 0) {
            executeTimer();
            nextTimerRunInMS = TIMER_INTERVAL_IN_MS;
        }

        //Time to iterate
        if (nextRecalculationInMS <= 0) {
            //We must recalculate our position now
            iterateVivaldi();

            //Determine when the next recalculation will be executed
            nextRecalculationInMS = RECALCULATION_INTERVAL - RECALCULATION_EPSILON  + (long)(2 * RECALCULATION_EPSILON * Math.random());
        }

        long nextCheck = nextTimerRunInMS > nextRecalculationInMS ? nextRecalculationInMS : nextTimerRunInMS;

        return nextCheck;
    }

    public class KnownOSD {

        private String uuid;
        private String strAddress;
        private VivaldiCoordinates coordinates;

        public KnownOSD(String uuid, VivaldiCoordinates coordinates) {
            this.uuid = uuid;
            this.strAddress = null;
            this.coordinates = coordinates;
        }

        public String getUUID() {
            return this.uuid;
        }

        public VivaldiCoordinates getCoordinates() {
            return this.coordinates;
        }

        public String getStrAddress() {
            return this.strAddress;
        }

        public void setStrAddress(String strAddress) {
            this.strAddress = strAddress;
        }

        public void setCoordinates(VivaldiCoordinates newCoordinates) {
            this.coordinates = newCoordinates;
        }
    }

    public class VivaldiRetry {

        private ArrayList<Long> prevRTTs;
        private int numRetries;
        private boolean retried;

        public VivaldiRetry() {
            //New retry caused by a time out
            prevRTTs = new ArrayList<Long>();
            numRetries = 1;
            retried = false;
        }

        public VivaldiRetry(long firstRTT) {
            //New retry caused by a excessively big RTT
            prevRTTs = new ArrayList<Long>();
            prevRTTs.add(firstRTT);
            numRetries = 1;
            retried = false;
        }

        public ArrayList<Long> getRTTs() {
            return prevRTTs;
        }

        public void addRTT(long newRTT) {
            prevRTTs.add(newRTT);
            numRetries++;
        }

        public void addTimeout() {
            numRetries++;
        }

        public int numberOfRetries() {
            return numRetries;
        }

        public void setRetried(boolean p) {
            retried = p;
        }

        public boolean hasBeenRetried() {
            return retried;
        }
    }

    public class SentRequest {

        private long localTime;
        private long systemTime;

        public SentRequest(long localTime, long systemTime) {
            this.localTime = localTime;
            this.systemTime = systemTime;
        }

        public long getLocalTime() {
            return this.localTime;
        }

        public long getSystemTime() {
            return this.systemTime;
        }
    }
}
