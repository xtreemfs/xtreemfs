/*  Copyright (c) 2008 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Juan Gonzalez de Benito (BSC) & Björn Kolbeck (ZIB)
 */
package org.xtreemfs.osd.stages;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_pingRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_pingResponse;
import org.xtreemfs.interfaces.VivaldiCoordinates;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.striping.UDPMessage;


import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.osd.vivaldi.VivaldiNode;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_pingRequest;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;

import java.util.HashMap;
import java.util.ArrayList;


/**
 * Stage used in the OSD to manage Vivaldi.
 * 
 * @author Juan Gonzalez de Benito (BSC) & Björn Kolbeck (ZIB)
 */
public class VivaldiStage extends Stage {

    private static final int STAGEOP_COORD_XCHG_REQUEST = 1;


    /**
     * System time when the timer was last executed.
     */
    private long lastCheck;

    //Added in r1249
    //TOFIX:Remove?
    private static final int STAGEOP_TCP_VIVALID_PING = 2;


    /**
     * The main OSDRequestDispatcher used by the OSD.
     */
    private final OSDRequestDispatcher master;

    /**
     * Client used to communicate with the Directory Service.
     */
    private final DIRClient            dirClient;
    
    /**
     * List of already sent Vivaldi REQUESTS.
     */
    private HashMap<InetSocketAddress,SentRequest> sentRequests;
    
    private HashMap<InetSocketAddress,VivaldiRetry> sentRetries;
    
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
    private ServiceSet knownOSDs;
    
    /**
     * Number of elapsed Vivaldi iterations
     */
    private long vivaldiIterations;
    
    private static final int MAX_RETRIES_FOR_A_REQUEST = 3;
    
    /**
     * Minimum recalculation period.
     * 
     * The recalculation period is randomly determined and is always included between
     * the minimum and the maximum period.
     */
    private static final int MIN_RECALCULATION_IN_MS = 1000 * 50;
    
    /**
     * Maximum recalculation period.
     * 
     * The recalculation period is randomly determined and is always included between
     * the minimum and the maximum period.
     */
    private static final int MAX_RECALCULATION_IN_MS = 1000 * 70;
    
    /**
     * Number of times the node recalculates its position before updating
     * its list of existent OSDs.
     */
    private static final int ITERATIONS_BEFORE_UPDATING = 20;
    
    /**
     * Maximum number of milliseconds an OSD waits for a RESPONSE before discarding
     * its corresponding REQUEST. Expiration times under {@code TIMER_INTERVAL_IN_MS}
     * are not granted.
     */
    private static final int MAX_REQUEST_TIMEOUT_IN_MS = 1000 * 10;
    
    /**
     * Period of time between timer executions.
     */
    private static final int TIMER_INTERVAL_IN_MS = 1000 * 60;

    
    
    
    public VivaldiStage(OSDRequestDispatcher master) {
        super("VivaldiSt");
        this.master = master;
        this.dirClient = master.getDIRClient();
        
        this.sentRequests = new HashMap<InetSocketAddress,SentRequest>();
        this.sentRetries = new HashMap<InetSocketAddress,VivaldiRetry>();
        this.vNode = new VivaldiNode();
        
        //TOFIX: should  the coordinates be initialized from a file?
        if(Logging.isInfo())
            Logging.logMessage( Logging.LEVEL_INFO,this, String.format("Coordinates initialized:(%.3f,%.3f)", 
                                                                        vNode.getCoordinates().getX_coordinate(),
                                                                        vNode.getCoordinates().getY_coordinate()) );

        this.knownOSDs = null;
        this.lastCheck = 0;
    }


    private void forceVivaldiRecalculation(VivaldiCoordinates coordinatesJ, ArrayList<Long> rtts){

        //TOFIX:In this version, the recalculation is discarded when the last retry times out: (coordinatesJ!=null)
        if( (coordinatesJ!=null) && (rtts.size()>0) ){
                                            
            //Determine the minimum RTT of the whole sample
            long minRTT= rtts.get(0);
            for(int i=1;i<rtts.size();i++){
                if(rtts.get(i) < minRTT){
                    minRTT = rtts.get(i);
                }
            }

            vNode.recalculatePosition(coordinatesJ, minRTT,true);
        }        
    }
    //Added in r1249
    //TOFIX:Remove?
    public void getVivaldiCoordinates(VivaldiCoordinates coordinates, OSDRequest request, VivaldiPingCallback listener) {
        this.enqueueOperation(STAGEOP_TCP_VIVALID_PING, new Object[]{coordinates}, request, listener);
    }

    //Added in r1249
    //TOFIX:Remove?
    public static interface VivaldiPingCallback {

        public void coordinatesCallback(VivaldiCoordinates myCoordinates, Exception error);
    }


    @Override
    protected void processMethod(StageRequest method) {

        if (method.getStageMethod() == STAGEOP_COORD_XCHG_REQUEST) {

            UDPMessage msg = (UDPMessage)method.getArgs()[0];
            //This avoids ReusableBuffer finalized but not freed before errors
            final ReusableBuffer data = msg.getPayload();
            try{
                if (msg.isRequest()) {
    
                    //send a response if it was a request
                    sendVivaldiResponse(msg, this.vNode.getCoordinates());
    
                }else if(msg.isResponse()){
    
                    xtreemfs_pingResponse response = (xtreemfs_pingResponse)msg.getResponseData();
                    VivaldiCoordinates coordinatesJ = response.getRemote_coordinates();
    
                    //The received response must have a related entry in our internal structures
                    SentRequest correspondingReq = sentRequests.remove(msg.getAddress());
    
                    if( correspondingReq != null ){
                        
                        //Calculate the RTT
                        long now = System.currentTimeMillis();
                        long estimatedRTT = now - correspondingReq.getSystemTime();
    
                        
                        //Two nodes will never be at the same position
                        if (estimatedRTT == 0){
                            estimatedRTT = 1;
                        }
    
                        //Recalculate Vivaldi
    
                        VivaldiRetry prevRetry = sentRetries.get(msg.getAddress());
                        
                        if( !vNode.recalculatePosition(coordinatesJ, estimatedRTT,false) ){
                            
                            //The RTT seems to be too big, so it might be a good idea to go for a retry
    
                            if(prevRetry==null){
                                sentRetries.put(msg.getAddress(), new VivaldiRetry(estimatedRTT));
                            }else{
                                
                                prevRetry.addRTT(estimatedRTT);
                                prevRetry.setRetried(false);
                                
                                if(prevRetry.numberOfRetries()>=MAX_RETRIES_FOR_A_REQUEST){
                                    
                                    //Recalculate using the previous RTTs
                                    forceVivaldiRecalculation(coordinatesJ,prevRetry.getRTTs());
                                    
                                    sentRetries.remove(msg.getAddress());
                                }
                            }
                            
                        }else{
                            if(Logging.isInfo()){
                                //TOFIX: Printing getHostName() without any kind of control could be dangerous (?)
                                Logging.logMessage( Logging.LEVEL_INFO,
                                                    this,
                                                    String.format("RTT:%d(Viv:%.3f) Own:(%.3f,%.3f) lE=%.3f Rem:(%.3f,%.3f) rE=%.3f %s", 
                                                            estimatedRTT,
                                                            VivaldiNode.calculateDistance(vNode.getCoordinates(), coordinatesJ),
                                                            vNode.getCoordinates().getX_coordinate(),
                                                            vNode.getCoordinates().getY_coordinate(),
                                                            vNode.getCoordinates().getLocal_error(),
                                                            coordinatesJ.getX_coordinate(),
                                                            coordinatesJ.getY_coordinate(),
                                                            coordinatesJ.getLocal_error(),
                                                            msg.getAddress().getHostName()));
                            }
                            if(prevRetry!=null){
                                //The received RTT is correct but it's been necessary to retry some request previously, so our structures must be updated. 
                                sentRetries.remove(msg.getAddress());
                            }
                        }
    
                    }//there's not any previously registered request , so we just discard the response
                }
            }finally{
                if(data!=null){
                    BufferPool.free(data);
                }
            }
        //Added in r1249
        //TOFIX:Remove?
        } else if (method.getStageMethod() == STAGEOP_TCP_VIVALID_PING) {
            VivaldiPingCallback cb = (VivaldiPingCallback)method.getCallback();
            VivaldiCoordinates remoteCoordinates = (VivaldiCoordinates) method.getArgs()[0];
            cb.coordinatesCallback(remoteCoordinates, null);
        } else {
            throw new RuntimeException("programmatic error, unknown stage operation");
        }
    }
    
    private void sendVivaldiResponse(UDPMessage request, VivaldiCoordinates myCoordinates) {
        
        xtreemfs_pingResponse resp = new xtreemfs_pingResponse(myCoordinates);
        
        UDPMessage msg = request.createResponse(resp);
        
        master.getUdpComStage().send(msg);
    }
    
    private void sendVivaldiRequest(InetSocketAddress osd, VivaldiCoordinates myCoordinates) {
    
        xtreemfs_pingRequest req = new xtreemfs_pingRequest(myCoordinates);

        //It's not allowed to send two requests to the same OSD simultaneously
        if( sentRequests.get(osd) == null){
            
            //Second(xid) and third parameter(proc) are not currently used
            UDPMessage msg = new UDPMessage(osd, 0, 0, req);
            
            //If we're sending a request, we need to register it in our structures so we can process its response later
            long systemTimeNow = System.currentTimeMillis();
            //getLocalSystemTime does not introduce such a big overhead, while currentTimeMillis is required to get the necessary precision.
            long localTimeNow = TimeSync.getLocalSystemTime();

            //TOFIX:Should we use an UUID as the key?
            sentRequests.put(osd, new SentRequest(localTimeNow,systemTimeNow));
            
            master.getUdpComStage().send(msg);
        }
    }

    /**
     * Keeps the list of sent requests updated by eliminating those whose timeout
     * has expired.
     */
    private void maintainSentRequests(){
        
        final long localNow = TimeSync.getLocalSystemTime();
        
        ArrayList<InetSocketAddress> removedRequests = new ArrayList<InetSocketAddress>();

        //Check which requests have timed out
        for( InetSocketAddress reqKey : sentRequests.keySet() ){
            if( localNow >= sentRequests.get(reqKey).getLocalTime()+MAX_REQUEST_TIMEOUT_IN_MS ){
                
                if(Logging.isInfo())
                    Logging.logMessage(Logging.LEVEL_INFO, this,"OSD times out:"+reqKey.getHostName());
                
                removedRequests.add(reqKey);
            }
        }

        //Manage the timed out requests
        for( InetSocketAddress removed : removedRequests){
            
            //Is it the first time the node times out?
            VivaldiRetry prevRetry = sentRetries.get(removed);

            //The retry is marked as 'not retried' so it will have priority when sending the next request
            if(prevRetry==null){
                sentRetries.put(removed, new VivaldiRetry());
            }else{

                //Take note of the new time out
                prevRetry.addTimeout();
                prevRetry.setRetried(false);
                
                //We've already retried too many times, so it's time to recalculate with the available info
                if(prevRetry.numberOfRetries()>=MAX_RETRIES_FOR_A_REQUEST){
                    forceVivaldiRecalculation(null,prevRetry.getRTTs());
                    sentRetries.remove(removed);
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
        enqueueOperation(STAGEOP_COORD_XCHG_REQUEST, new Object[]{msg}, null, null);
    }

    /**
     * Updates the list of known OSDs, from the data stored in the DS. This
     * function is responsible of keeping a list of OSDs used by the algorithm.
     */
    private void updateKnownOSDs(){

        RPCResponse<ServiceSet> r = null;
        
        try{
            
            r = dirClient.xtreemfs_service_get_by_type(null, ServiceType.SERVICE_TYPE_OSD);
            knownOSDs = r.get();
            
            //We need our own UUID, to discard its corresponding entry
            String ownUUID = master.getConfig().getUUID().toString();
            
            ServiceSet newOSDs = new ServiceSet();
            
            for(Service osd:knownOSDs){

                //If it's not our own entry and the referred service is not offline
                if( !ownUUID.equals(osd.getUuid()) && osd.getLast_updated_s()!=0 ){
                    newOSDs.add(osd);
                }
            }
            
            knownOSDs = newOSDs;
            
            if (Logging.isInfo())
                Logging.logMessage(Logging.LEVEL_INFO, this, "Updating list of known OSDs");
            
        } catch (Exception exc) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "Error while updating known OSDs:"+exc);
            //Logging.logError(Logging.LEVEL_ERROR, this, exc);
            //this.notifyCrashed(exc);
        } finally {
            if (r != null){
                r.freeBuffers();
            }
            
            //Previous requests are discarded
            sentRequests.clear();
            sentRetries.clear();
        }
    }
    
    /**
     * Executes one Vivaldi iteration. For each of these iterations, the algorithm
     * chooses one random node from the list of known OSDs and sends it a Vivaldi
     * request, to recalculate then the position of the OSD using the received information
     */
    private void iterateVivaldi() {
        
        if( vivaldiIterations%ITERATIONS_BEFORE_UPDATING == 1 ){
            updateKnownOSDs();
        }
        
        //Start recalculation
        if( (knownOSDs!=null) && (!knownOSDs.isEmpty()) ){

            //It's still necessary to retry some request
            if(sentRetries.size()>0){
                for(InetSocketAddress addr:sentRetries.keySet()){
                    
                    if(!sentRetries.get(addr).hasBeenRetried()){
                        sendVivaldiRequest(addr, vNode.getCoordinates());
                        sentRetries.get(addr).setRetried(true);
                    }
                    
                }
                
            }else{
            
                //Choose a random OSD and send a new request
                int chosenIndex = (int)(Math.random()*knownOSDs.size());

                Service chosenService = knownOSDs.get(chosenIndex);

                try{

                    //Get the corresponding InetAddress
                    ServiceUUID sUUID = new ServiceUUID(chosenService.getUuid());

                    sUUID.resolve();

                    if(Logging.isInfo())
                        Logging.logMessage(Logging.LEVEL_INFO, this,"Recalculating against:"+chosenService.getUuid());                        
                    
                    //After receiving the response, we will be able to recalculate
                    sendVivaldiRequest(sUUID.getAddress(), vNode.getCoordinates());

                }catch(UnknownUUIDException unke){
                    Logging.logMessage(Logging.LEVEL_ERROR, this,"Unknown UUID:"+chosenService.getUuid());
                }catch(Exception e){
                        Logging.logMessage(Logging.LEVEL_ERROR, this,"Error detected while iterating Vivaldi");
                }
            }

        }
        
        vivaldiIterations = (vivaldiIterations+1) % Long.MAX_VALUE;
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
            } catch (Exception ex) {
                Logging.logError(Logging.LEVEL_ERROR, this, ex);
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
        long elapsedTime = lastCheck>0 ? (now - lastCheck) : 0;            
            
        lastCheck = now;
        
        nextRecalculationInMS -= elapsedTime;
        nextTimerRunInMS -= elapsedTime;

        //Need to execute our timer
        if ( nextTimerRunInMS<=0 ){
            executeTimer();
            nextTimerRunInMS = TIMER_INTERVAL_IN_MS;
        }        
        
        //Time to iterate
        if( nextRecalculationInMS<=0 ){
            
            //We must recalculate our position now
            iterateVivaldi();
            
            //Determine when the next recalculation will be executed
            nextRecalculationInMS = MIN_RECALCULATION_IN_MS + (long)((MAX_RECALCULATION_IN_MS - MIN_RECALCULATION_IN_MS)*Math.random());
        }
        
        long nextCheck = nextTimerRunInMS > nextRecalculationInMS ? nextRecalculationInMS : nextTimerRunInMS;
        
        return nextCheck;
    }
    
    public class VivaldiRetry{
        
        private ArrayList<Long> prevRTTs;
        private int numRetries;
        private boolean retried;
        
        public VivaldiRetry(){
            //New retry caused by a time out
            prevRTTs = new ArrayList<Long>();
            numRetries=1;
            retried = false;
        }
        public VivaldiRetry(long firstRTT){
            //New retry caused by a excessively big RTT
            prevRTTs = new ArrayList<Long>();
            prevRTTs.add(firstRTT);
            numRetries=1;
            retried = false;
        }
        
        
        public ArrayList<Long> getRTTs(){
            return prevRTTs;
        }
        public void addRTT(long newRTT){
            prevRTTs.add(newRTT);
            numRetries++;
        }
        public void addTimeout(){
            numRetries++;
        }
        
        public int numberOfRetries(){
            return numRetries;
        }
        public void setRetried(boolean p){
            retried = p;
        }
        public boolean hasBeenRetried(){
            return retried;
        }

    }
    
    public class SentRequest{
        
        private long localTime;
        private long systemTime;
        
        public SentRequest(long localTime,long systemTime){
            this.localTime = localTime;
            this.systemTime = systemTime;
        }
        
        public long getLocalTime(){
            return this.localTime;
        }
        public long getSystemTime(){
            return this.systemTime;
        }
    }
}
