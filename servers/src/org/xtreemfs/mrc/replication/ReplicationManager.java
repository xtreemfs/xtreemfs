/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.replication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.HttpErrorException;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.json.JSONCharBufferString;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.brain.BrainRequestListener;
import org.xtreemfs.mrc.brain.BrainStage;
import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.mrc.brain.storage.DiskLogger;
import org.xtreemfs.mrc.brain.storage.InvalidLogEntryException;
import org.xtreemfs.mrc.brain.storage.LogEntry;
import org.xtreemfs.mrc.brain.storage.SliceID;
import org.xtreemfs.mrc.brain.storage.SyncListener;
import org.xtreemfs.mrc.slices.SliceInfo;
import org.xtreemfs.mrc.slices.SliceManager;
import org.xtreemfs.mrc.slices.VolumeInfo;
import org.xtreemfs.mrc.utils.MessageUtils;

/**
 * Handles all replication specific commands. All persistent commands are sent
 * to replicated MRCs according to the replication mechanism.
 * 
 * @author bjko
 */
public class ReplicationManager extends LifeCycleThread {
    
    public static final int                 EC_SLICE_DOES_NOT_EXIST  = 1000;
    
    public static final int                 EC_SLICE_NOT_OPERATIONAL = 1001;
    
    public static final int                 EC_LOGRANGE_NOT_AVAIL    = 1002;
    
    public static final int                 EC_INVALID_VIEWID        = 1003;
    
    /**
     * Database dir from configuration used to store the settings
     */
    private final String                    dbDir;
    
    /** filename in which the settings are stored */
    private static final String             FILENAME                 = "replication.dat";
    
    /** for crash recovery first a the tmp is written */
    private static final String             TEMP_FILENAME            = "replication.tmp";
    
    /** request queue */
    private LinkedBlockingQueue<MRCRequest> requests;
    
    /**
     * listener which is notfified after a replication request is finished
     */
    private ReplicationRequestListener      rrListener;
    
    /**
     * listener which is notified after a replication system command is finished
     */
    private BrainRequestListener            brListener;
    
    /**
     * if set to true the thread leaves the main loop
     */
    private volatile boolean                quit;
    
    /**
     * MRCClient used to communicate with other MRCs
     */
    private MRCClient                       mrcClient;
    
    /**
     * from config
     */
    private boolean                         debug;
    
    /**
     * Brain stage used to perform retrieve slice details and to execute remote
     * commands
     */
    private BrainStage                      storage;
    
    /** Global settings */
    private MRCConfig                       config;
    
    /**
     * Workaround to intercept Sync listener events
     */
    private StupidListener                  sl;
    
    private final DiskLogger                diskLogger;
    
    private final SliceManager              slices;
    
    /** Creates a new instance of ReplicationManager */
    public ReplicationManager(MRCConfig config, MRCClient mrcClient, DiskLogger diskLogger,
        SliceManager slices) throws IOException, ClassNotFoundException {
        
        super("ReplMgr thr.");
        
        this.diskLogger = diskLogger;
        
        this.slices = slices;
        requests = new LinkedBlockingQueue();
        quit = false;
        this.mrcClient = mrcClient;
        this.config = config;
        sl = new StupidListener(null);
        
        if (!config.getDbDir().endsWith("/")) {
            dbDir = config.getDbDir() + "/";
        } else {
            dbDir = config.getDbDir();
        }
        
    }
    
    /**
     * Sets the BrainStage the replication manager sould use.
     */
    public void setBrainStage(BrainStage storage) {
        this.storage = storage;
    }
    
    /**
     * Registers a listener for BrainRequets (i.e. system commands). For
     * replication commands use ReplicationListener
     */
    public void registerBrainListener(BrainRequestListener brl) {
        this.brListener = brl;
    }
    
    /**
     * Register a listener for Replication Requests. For a system command use
     * BrainListener
     */
    public void registerReplicationListener(ReplicationRequestListener rrl) {
        this.rrListener = rrl;
    }
    
    /**
     * add a request to the job queue
     */
    public void addRequest(MRCRequest rq) {
        this.requests.add(rq);
    }
    
    /** thread main loop */
    public void run() {
        
        notifyStarted();
        while (!quit) {
            try {
                // take next request from Q
                MRCRequest rq = this.requests.take();
                // this assert is somehow superflous as it checks something
                // that is specified in the javadocs...
                assert (rq != null);
                
                if (rq.logEntry != null) {
                    // If there is a logEntry attached to the request
                    // it must be a persistent command already executed
                    // so we have to repliate it to somewhere...
                    doReplication(rq);
                } else {
                    // a replication specific system command was requested
                    // by a remote server
                    if (rq.getPinkyRequest().requestURI.equals(".Replicate")) {
                        // another srver wants to replicate sth. to us
                        this.remoteOperation(rq);
                        // this operation takes care of notification itself
                    } else if (rq.getPinkyRequest().requestURI.equals(".RgetLogEntries")) {
                        this.getLogEntries(rq);
                        rq.details.persistentOperation = false;
                        brListener.brainRequestDone(rq);
                    } else if (rq.getPinkyRequest().requestURI.equals(".RgetSliceDB")) {
                        this.getSliceDB(rq);
                        rq.details.persistentOperation = false;
                        brListener.brainRequestDone(rq);
                    } else if (rq.getPinkyRequest().requestURI.equals(".Rinfo")) {
                        this.getInfos(rq);
                        rq.details.persistentOperation = false;
                        brListener.brainRequestDone(rq);
                    } else if (rq.getPinkyRequest().requestURI.equals(".RchangeStatus")) {
                        this.changeStatus(rq);
                        rq.details.persistentOperation = false;
                        brListener.brainRequestDone(rq);
                    } else if (rq.getPinkyRequest().requestURI.equals(".RforceUpdate")) {
                        this.forceUpdate(rq);
                        rq.details.persistentOperation = false;
                        brListener.brainRequestDone(rq);
                    } else if (rq.getPinkyRequest().requestURI.equals(".RnewSlaveSlice")) {
                        this.newSlaveSlice(rq);
                        // routine takes care of notification
                    } else if (rq.getPinkyRequest().requestURI.equals(".RnewMasterSlice")) {
                        this.newMasterSlice(rq);
                        rq.details.persistentOperation = false;
                        brListener.brainRequestDone(rq);
                    } else if (rq.getPinkyRequest().requestURI.equals(".RnoReplication")) {
                        this.nullReplication(rq);
                        rq.details.persistentOperation = false;
                        brListener.brainRequestDone(rq);
                    } else {
                        // HUH? what is that
                        rq.getPinkyRequest().setResponse(HTTPUtils.SC_NOT_IMPLEMENTED);
                        rq.details.persistentOperation = false;
                        brListener.brainRequestDone(rq);
                    }
                }
                
            } catch (InterruptedException ex) {
                // who cares
            }
            
        }
        
        notifyStopped();
    }
    
    /**
     * enqueues the operation contained in rq for replication
     * 
     * @returns true if the client can get a response immediateley, if false the
     *          response is sent after successful replication
     * @throws IllegalArgumentException
     *             if there is no LogEntry attached to the request
     */
    public boolean replicate(MRCRequest rq) {
        // need a valid log entry to send to remote site
        if (rq.logEntry == null)
            throw new IllegalArgumentException("replication requests must contain a valid LogEntry");
        
        // first we have to check the replication policy for that slice
        SliceInfo info = slices.getSliceInfo(rq.logEntry.slID);
        assert (info != null);
        
        ReplicationMechanism rm = info.getReplicationMechanism();
        assert (rm != null);
        
        // if it is null replication we have nothing todo
        if (rm instanceof NullReplicationMechanism) {
            // release log entry immediateley
            BufferPool.free(rq.logEntry.payload);
            return true;
        }
        
        // add the request to the Q, run will know this is a
        // replicate request since a logEntry is attached
        requests.add(rq);
        
        // bla
        if (rm.sendResponseAfterReplication()) {
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * execute a replication request after it was enqueued w/ replicate
     */
    protected void doReplication(MRCRequest rq) {
        // can't be null replication
        SliceInfo info = slices.getSliceInfo(rq.logEntry.slID);
        assert (info != null);
        
        ReplicationMechanism rm = info.getReplicationMechanism();
        assert (rm != null);
        
        if (rm instanceof MasterReplicationMechanism) {
            // put the log entry into a ByteBuffer
            ReusableBuffer l = null;
            
            try {
                
                l = rq.logEntry.marshall();
                assert (rq.logEntry.slID != null);
                
                List<InetSocketAddress> invalidSlaves = null;
                
                // and notify all registered slaves
                for (InetSocketAddress slave : ((MasterReplicationMechanism) rm).slaves) {
                    if (mrcClient.serverIsAvailable(slave)) {
                        RPCResponse resp = null;
                        try {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this,
                                "requested replication to " + slave + "    size(Q)="
                                    + this.requests.size());
                            resp = mrcClient.sendRPC(slave, ".Replicate", l.createViewBuffer(),
                                null, null);
                            resp.waitForResponse();
                            Logging.logMessage(Logging.LEVEL_DEBUG, this, "replicated to " + slave);
                        } catch (JSONException ex) {
                            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                            // FIXME: this should never happen, but there should
                            // be some
                            // kind of notfication if it occurs
                        } catch (HttpErrorException ex) {
                            
                            SpeedyRequest sr = resp.getSpeedyRequest();
                            String body = new String(sr.responseBody.array(), HTTPUtils.ENC_UTF8);
                            Map<String, Object> exc = null;
                            try {
                                exc = (Map<String, Object>) JSONParser.parseJSON(new JSONString(
                                    body));
                            } catch (JSONException e1) {
                                Logging.logMessage(Logging.LEVEL_ERROR, this, e1);
                            }
                            
                            if ((Long) exc.get("errno") == EC_SLICE_DOES_NOT_EXIST) {
                                // this is a big problem!!!
                                Logging.logMessage(Logging.LEVEL_ERROR, this, "slave " + slave
                                    + " does not have a slice " + rq.logEntry.slID
                                    + " and is removed from the slave list!");
                                if (invalidSlaves == null)
                                    invalidSlaves = new ArrayList();
                                invalidSlaves.add(slave);
                            }
                        } catch (IOException ex) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, this, "cannot replicate to "
                                + slave + " because " + ex);
                        } catch (InterruptedException ex) {
                            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                        } finally {
                            if (resp != null)
                                resp.freeBuffers();
                        }
                    } else {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "speedy says that " + slave
                            + " is not available");
                    }
                }
                if (invalidSlaves != null) {
                    int i = 0;
                    InetSocketAddress[] newSl = new InetSocketAddress[((MasterReplicationMechanism) rm).slaves.length
                        - invalidSlaves.size()];
                    for (InetSocketAddress slave : ((MasterReplicationMechanism) rm).slaves) {
                        if (!invalidSlaves.contains(slave))
                            newSl[i++] = slave;
                    }
                    ((MasterReplicationMechanism) rm).slaves = newSl;
                    try {
                        slices.modifySlice(info);
                    } catch (IOException ex) {
                        Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                    }
                }
                
            } finally {
                if (l != null)
                    BufferPool.free(l);
            }
        }
        
    }
    
    /**
     * Handles remote requests for operation execution, i.e. by a master MRC
     */
    protected void remoteOperation(MRCRequest rq) {
        try {
            
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "remote operation started, size(Q)="
                + this.requests);
            rq.getPinkyRequest().requestBody.position(0);
            // unmarshall the log entry
            LogEntry e = new LogEntry(rq.getPinkyRequest().requestBody);
            assert (e.slID != null);
            
            // get replication info for the affected slice
            SliceInfo info = slices.getSliceInfo(e.slID);
            if (info == null) {
                // slice does not exist on this volume
                MessageUtils.marshallException(rq, new UserException(EC_SLICE_DOES_NOT_EXIST,
                    "slice does not exist"));
                rq.details.persistentOperation = false;
                brListener.brainRequestDone(rq);
            }
            
            synchronized (info) {
                ReplicationMechanism rm = info.getReplicationMechanism();
                assert (rm != null);
                // FIXME: handle error case when slice is not available anymore
                // or the server sent an invalid request
                
                // execute only if the slice is ready
                if (info.isReplicating()) {
                    
                    // check view and sequence ID....
                    if ((e.viewID != info.getCurrentViewID())
                        || (e.sequenceID > info.getCurrentSequenceID())) {
                        // we have missed some ops...
                        Logging.logMessage(Logging.LEVEL_WARN, this, "missing OPS...");
                        // the slice is not operational anymore
                        // until it has fetched all missed log entries from the
                        // master
                        info.setReplicating(false);
                        // we first acknowledge to make sure that the server
                        // does not
                        // time out
                        MessageUtils.marshallException(rq, new UserException(
                            EC_SLICE_NOT_OPERATIONAL, "slice not up to date"));
                        rq.details.persistentOperation = false;
                        brListener.brainRequestDone(rq);
                        
                        // we fetch everything from the master...
                        fetchMasterStatus(e.slID);
                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "remote operation done");
                        return;
                    }
                    // if everything is fine we can just execute the log enry
                    this.replicateLogEntry(e, info);
                    e.attachment = rq;
                    rq.details.sliceId = e.slID;
                    // and append it to our local log file
                    e.listener = sl;
                    diskLogger.append(e);
                    rq.getPinkyRequest().setResponse(HTTPUtils.SC_OKAY);
                    rq.details.persistentOperation = false;
                    brListener.brainRequestDone(rq);
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "remote operation done");
                } else {
                    // slice is out of order...please try again ;-)
                    MessageUtils.marshallException(rq, new UserException(EC_SLICE_NOT_OPERATIONAL,
                        "slice not operational"));
                    rq.details.persistentOperation = false;
                    brListener.brainRequestDone(rq);
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "SLICE IS NOT OPERATIONAL");
                }
            }
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
            if (!rq.getPinkyRequest().responseSet)
                MessageUtils.marshallException(rq, ex);
            rq.details.persistentOperation = false;
            brListener.brainRequestDone(rq);
        }
    }
    
    /**
     * Remote server requested log entries for a slice
     */
    public void getLogEntries(MRCRequest rq) {
        // parse the request
        try {
            Object o = MessageUtils.unmarshallRequest(rq);
            List<Object> args = (List) o;
            // args are ["sliceID",currentViewID,startSequenceID,endSequenceID
            String slID = (String) args.get(0);
            int viewID = ((Long) args.get(1)).intValue();
            int startSQ = ((Long) args.get(2)).intValue();
            int endSQ = ((Long) args.get(3)).intValue();
            
            // get a SliceID obj
            SliceID sl = new SliceID(slID);
            
            // we need the replication info again
            // get replication info for the affected slice
            SliceInfo info = slices.getSliceInfo(sl);
            assert (info != null);
            
            // tell the connection remover that we are still doing sth
            rq.getPinkyRequest().active();
            if (info.getCurrentViewID() != viewID) {
                if (info.getCurrentViewID() > viewID) {
                    // okay, slave lags behind
                    MessageUtils.marshallException(rq, new UserException(EC_LOGRANGE_NOT_AVAIL,
                        "requested range is not available because view has already changed"));
                } else {
                    // the other server is in a newer view
                    // THIS IS EVIL!
                    MessageUtils.marshallException(rq, new UserException(EC_INVALID_VIEWID,
                        "viewID is not valid"));
                }
            } else {
                if (info.getLastAvailSqID() > startSQ) {
                    // there was a compactDB op after startSQ
                    // so the log entries are not available
                    // and the other server probably needs to fetch
                    // the entire database
                    MessageUtils.marshallException(rq, new UserException(EC_LOGRANGE_NOT_AVAIL,
                        "requested range is not available"));
                } else {
                    ReusableBuffer b = diskLogger.getLog(sl, startSQ, endSQ);
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "response is " + b);
                    // send the log file...
                    rq.getPinkyRequest().setResponse(HTTPUtils.SC_OKAY, b,
                        HTTPUtils.DATA_TYPE.BINARY);
                }
            }
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "Log requested for " + slID + " "
                + viewID + "/" + startSQ + "-" + endSQ);
        } catch (Exception e) {
            // FIXME: better exception handling like in brain
            Logging.logMessage(Logging.LEVEL_WARN, this, e);
            rq.getPinkyRequest().setResponse(HTTPUtils.SC_BAD_REQUEST);
        }
    }
    
    /**
     * shut down the Replication Manager thread
     */
    public void shutdown() {
        this.quit = true;
        this.interrupt();
    }
    
    /**
     * Sends the entire slice DB
     */
    private void getSliceDB(MRCRequest rq) {
        try {
            assert (rq != null);
            assert (rq.getPinkyRequest() != null);
            
            Object o = MessageUtils.unmarshallRequest(rq);
            List<Object> args = (List) o;
            String slID = (String) args.get(0);
            
            // remove everything that is not a character to make sure nobody
            // messes around w/ our FS
            slID = slID.replaceAll("/[^abcdefABCDEF0123456789]/", "");
            
            File sliceFile = new File(dbDir + slID + "/mrcdb."
                + VersionManagement.getMrcDataVersion());
            if (sliceFile.exists()) {
                // we are working, connremover should not GC us
                rq.getPinkyRequest().active();
                // files should not get larger, if they do, we are f*up
                assert (sliceFile.length() <= Integer.MAX_VALUE);
                int size = (int) sliceFile.length();
                FileInputStream fis = new FileInputStream(sliceFile);
                FileChannel fc = fis.getChannel();
                // map DB into memory
                MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, size);
                ReusableBuffer buf = new ReusableBuffer(mbb);
                fis.close();
                SliceID tmp = new SliceID(slID);
                SliceInfo info = slices.getSliceInfo(tmp);
                assert (info != null);
                // add headers for DB info (like which view we have and until
                // which sequence ID it has
                // compacted all ops
                HTTPHeaders addHdrs = new HTTPHeaders();
                addHdrs.addHeader("X-ViewID", info.getCurrentViewID());
                addHdrs.addHeader("X-Start-SequenceID", info.getLastAvailSqID());
                rq.getPinkyRequest().setResponse(HTTPUtils.SC_OKAY, buf,
                    HTTPUtils.DATA_TYPE.BINARY, addHdrs);
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Database rqquested for " + slID);
            } else {
                // FIXME: Huston, we have a problem...
                Logging.logMessage(Logging.LEVEL_ERROR, this, "request for " + slID + " but "
                    + sliceFile.getAbsolutePath() + " not found??");
                MessageUtils.marshallException(rq, new UserException(EC_SLICE_DOES_NOT_EXIST,
                    "slice does not exist"));
            }
        } catch (Exception e) {
            MessageUtils.marshallException(rq, e);
        }
        
    }
    
    /**
     * executed when a remote master wants to start replicating a slice to this
     * MRC
     */
    private void newSlaveSlice(MRCRequest rq) {
        try {
            assert (rq != null);
            assert (rq.getPinkyRequest() != null);
            
            Object o = MessageUtils.unmarshallRequest(rq);
            List<Object> args = (List) o;
            String slID = (String) args.get(0);
            String master = (String) args.get(1);
            String volName = (String) args.get(2);
            Long fileACL = (Long) args.get(3);
            Long osdPolicy = (Long) args.get(4);
            String osdPolicyArgs = (String) args.get(5);
            Long partPolicy = (Long) args.get(6);
            
            // make sure there are no special chars in the sliceID (security)
            slID = slID.replaceAll("/[^a-fA-F0-9]/", "");
            InetSocketAddress masterAddr = MessageUtils.addrFromString(master);
            
            // lets see if we already have that slice
            SliceID sliceID = new SliceID(slID);
            SliceInfo info = slices.getSliceInfo(sliceID);
            
            if (info != null) {
                // just change the info
                info.setStatus(SliceInfo.SliceStatus.OFFLINE);
                info.setReplicating(true);
                info.changeReplicationMechanism(new SlaveReplicationMechanism(masterAddr));
                slices.modifySlice(info);
            } else {
                
                // first, create a local representation of the volume if
                // none exists yet
                if (!slices.hasVolumeWithId(sliceID.getVolumeId()))
                    slices.createVolume(sliceID.getVolumeId(), volName, fileACL, osdPolicy,
                        osdPolicyArgs, partPolicy, false, false);
                
                // create the slice
                
                // create the directory..
                File dir = new File(dbDir + slID);
                dir.mkdirs();
                
                info = new SliceInfo(sliceID, new SlaveReplicationMechanism(masterAddr));
                info.setReplicating(true);
                info.setStatus(SliceInfo.SliceStatus.OFFLINE);
                slices.createSlice(info, true);
                
                // SYNC!
                rq.getPinkyRequest().setResponse(HTTPUtils.SC_OKAY);
                rq.details.persistentOperation = false;
                brListener.brainRequestDone(rq);
                
                // do the rest "offline"
            }
            // fetch the current database
            fetchMasterStatus(sliceID);
            rq.getPinkyRequest().active();
            
        } catch (Exception e) {
            e.printStackTrace();
            MessageUtils.marshallException(rq, e);
            rq.details.persistentOperation = false;
            brListener.brainRequestDone(rq);
        }
    }
    
    /**
     * executed when a remote master wants to start replicating a slice to this
     * MRC
     */
    private void nullReplication(MRCRequest rq) {
        try {
            assert (rq != null);
            assert (rq.getPinkyRequest() != null);
            
            Object o = MessageUtils.unmarshallRequest(rq);
            List<Object> args = (List) o;
            String slID = (String) args.get(0);
            
            // make sure there are no special chars in the sliceID (security)
            slID = slID.replaceAll("/[^a-fA-F0-9]/", "");
            
            // lets see if we already have that slice
            SliceID sliceID = new SliceID(slID);
            SliceInfo info = slices.getSliceInfo(sliceID);
            
            if (info != null) {
                // just change the info
                info.setStatus(SliceInfo.SliceStatus.OFFLINE);
                info.setReplicating(false);
                info.changeReplicationMechanism(new NullReplicationMechanism());
                slices.modifySlice(info);
                rq.getPinkyRequest().setResponse(HTTPUtils.SC_OKAY);
            } else {
                MessageUtils.marshallException(rq, new UserException(EC_SLICE_DOES_NOT_EXIST,
                    "slice does not exist!"));
            }
            // fetch the current database
            rq.getPinkyRequest().active();
            
        } catch (Exception e) {
            MessageUtils.marshallException(rq, e);
        }
    }
    
    /**
     * fetch missed log entries and/or full slice database from the master MRC
     */
    private void fetchMasterStatus(SliceID sl) throws IOException {
        // get local state
        SliceInfo info = slices.getSliceInfo(sl);
        assert (info != null);
        synchronized (info) {
            
            ReplicationMechanism rm = info.getReplicationMechanism();
            assert (rm != null);
            
            info.setReplicating(false);
            
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "fetch master status started");
            
            boolean retry = false;
            do {
                RPCResponse mr = null;
                
                try {
                    // ask master to send me the missing log part
                    
                    // first build a request
                    List<Object> args = new LinkedList();
                    args.add(sl.toString());
                    args.add((Integer) info.getCurrentViewID());
                    args.add((Integer) info.getCurrentSequenceID());
                    args.add((Integer) Integer.MAX_VALUE);
                    // and send it to the master
                    mr = mrcClient.sendRPC(((SlaveReplicationMechanism) rm).master,
                        ".RgetLogEntries", args, null, null);
                    // wait for answer
                    mr.waitForResponse();
                    
                    SpeedyRequest rq = mr.getSpeedyRequest();
                    
                    // we could contact the master MRC (no exception)
                    retry = false;
                    if (rq.responseBody != null) {
                        // read the response body
                        // create view buffer
                        ReusableBuffer body = rq.responseBody.createViewBuffer();
                        body.position(0);
                        
                        // we need this "null listener" for the DiskLogger
                        StupidListener nada = new StupidListener(info);
                        
                        try {
                            // do sth with this log stuff...
                            while (true) {
                                LogEntry e = new LogEntry(body);
                                // we do not really care about the result, at
                                // least
                                // at here
                                e.registerListener(nada);
                                replicateLogEntry(e, info);
                                
                                Logging.logMessage(Logging.LEVEL_DEBUG, this,
                                    "replayed remote op: " + e.operationName + " params: "
                                        + e.payload);
                                
                                diskLogger.append(e);
                            }
                        } catch (InvalidLogEntryException ex) {
                            if (ex.getLength() != 0) {
                                info.setStatus(SliceInfo.SliceStatus.OFFLINE);
                                info.setReplicating(true);
                                throw new IOException("cannot read log entries " + ex, ex);
                            }
                            // probably a half finished log entry...
                        } catch (Exception ex) {
                            info.setStatus(SliceInfo.SliceStatus.OFFLINE);
                            info.setReplicating(true);
                            throw new IOException("cannot read log entries " + ex, ex);
                        } finally {
                            BufferPool.free(body);
                        }
                    } else {
                        // nothing to replay...remote log is empty!
                    }
                    
                } catch (InterruptedException e) {
                    throw new IOException(e.getMessage());
                } catch (JSONException e) {
                    throw new IOException(e.getMessage());
                } catch (HttpErrorException e) {
                    // okay sth went wrong
                    if (retry) {
                        info.setReplicating(true);
                        info.setStatus(SliceInfo.SliceStatus.OFFLINE);
                        throw new IOException("cannot fetch database for slice " + sl + " from "
                            + ((SlaveReplicationMechanism) rm).master);
                    }
                    
                    assert (mr != null);
                    
                    SpeedyRequest rq = mr.getSpeedyRequest();
                    String body = new String(rq.responseBody.array(), HTTPUtils.ENC_UTF8);
                    Map<String, Object> exc;
                    try {
                        exc = (Map<String, Object>) JSONParser.parseJSON(new JSONString(body));
                    } catch (JSONException e1) {
                        throw new IOException(e1);
                    }
                    
                    if ((Long) exc.get("errno") == EC_LOGRANGE_NOT_AVAIL) {
                        // this means we have to get the full database first
                        retry = true;
                        
                        RPCResponse<Object> mr2 = null;
                        try {
                            // make a request object
                            List<Object> args2 = new LinkedList();
                            args2.add(sl.toString());
                            // and send request
                            mr2 = mrcClient.sendRPC(((SlaveReplicationMechanism) rm).master,
                                ".RgetSliceDB", args2, null, null);
                            mr2.waitForResponse();
                            SpeedyRequest sr = mr2.getSpeedyRequest();
                            
                            // great! we have it
                            FileOutputStream fos = new FileOutputStream(dbDir + sl + "/mrcdb."
                                + VersionManagement.getMrcDataVersion());
                            sr.responseBody.position(0);
                            fos.getChannel().write(sr.responseBody.getBuffer());
                            fos.close();
                            
                            // fetch current view and lastAvailSqID from headers
                            String tmp = sr.responseHeaders.getHeader("X-ViewID");
                            if (tmp != null) {
                                info.setCurrentViewID(Integer.valueOf(tmp));
                            } else {
                                info.setReplicating(true);
                                throw new IOException("missing or invalid X-ViewID header");
                            }
                            
                            tmp = sr.responseHeaders.getHeader("X-Start-SequenceID");
                            if (tmp != null) {
                                info.setLastAvailSqID(Integer.valueOf(tmp) - 1);
                                info.setNextSequenceID(Integer.valueOf(tmp));
                            } else {
                                info.setReplicating(true);
                                throw new IOException(
                                    "missing or invalid X-Start-SequenceID header");
                            }
                            // tell the storage manager that it has to read from
                            // disk
                            slices.reInitSlice(sl);
                            
                            Logging.logMessage(Logging.LEVEL_DEBUG, this,
                                "fetch DB done and relinked...");
                        } catch (Exception e2) {
                            // this is the end my only friend, the end ;-)
                            Logging.logMessage(Logging.LEVEL_ERROR, this, e2);
                            info.setReplicating(true);
                            info.setStatus(SliceInfo.SliceStatus.OFFLINE);
                            throw new IOException(e2.getMessage());
                        } finally {
                            if (mr2 != null)
                                mr2.freeBuffers();
                        }
                        
                    } else {
                        Logging.logMessage(Logging.LEVEL_WARN, this, "unknwon error code :" + e);
                    }
                } catch (IOException e) {
                    info.setReplicating(true);
                    info.setStatus(SliceInfo.SliceStatus.OFFLINE);
                    Logging.logMessage(Logging.LEVEL_WARN, this, "cannot contact master for slice "
                        + sl);
                    throw e;
                } finally {
                    if (mr != null)
                        mr.freeBuffers();
                }
            } while (retry);
            Logging.logMessage(Logging.LEVEL_DEBUG, this, "fetch master status done");
            info.setReplicating(true);
            info.setStatus(SliceInfo.SliceStatus.READONLY);
        }
    }
    
    /**
     * execute a remote operation locally
     */
    private void replicateLogEntry(LogEntry l, SliceInfo ri) throws Exception {
        // parse the log entry contents
        Object args = null;
        if (l.payload != null) {
            // parse JSONrequest.pr.requestBody.position(0);
            CharBuffer utf8buf = HTTPUtils.ENC_UTF8.decode(l.payload.getBuffer());
            args = JSONParser.parseJSON(new JSONCharBufferString(utf8buf));
        }
        
        // this operation works in sync mode
        storage.replayLogEntry(l.operationName, l.userID, l.groupID, args);
        
        ri.setNextSequenceID(l.sequenceID + 1);
        
        Logging.logMessage(Logging.LEVEL_DEBUG, this, "replayed remote operation "
            + l.operationName + " with " + args);
    }
    
    /**
     * setup this MRC as the master for an existing slice
     */
    private void newMasterSlice(MRCRequest rq) {
        try {
            assert (rq != null);
            assert (rq.getPinkyRequest() != null);
            
            Object o = MessageUtils.unmarshallRequest(rq);
            // unwrap the arguments
            List<Object> args = (List) o;
            String slID = (String) args.get(0);
            List<Object> slaves = (List) args.get(1);
            Boolean waitForAck = (Boolean) args.get(2);
            
            // convert the strings to socket addresses
            InetSocketAddress[] addrs = new InetSocketAddress[slaves.size()];
            int i = 0;
            for (Object sl : slaves) {
                String master = (String) sl;
                addrs[i++] = MessageUtils.addrFromString(master);
            }
            // no special chars in the slice ID!
            slID = slID.replaceAll("/[^a-fA-F0-9]/", "");
            
            SliceID sliceID = new SliceID(slID);
            // get replication info for the affected slice
            SliceInfo info = slices.getSliceInfo(sliceID);
            assert (info != null);
            
            if (info == null) {
                MessageUtils.marshallException(rq, new UserException(EC_SLICE_DOES_NOT_EXIST,
                    "no such slice"));
                return;
            }
            
            // create and save new replication info
            ReplicationMechanism rm = new MasterReplicationMechanism(addrs, waitForAck);
            info.changeReplicationMechanism(rm);
            slices.modifySlice(info);
            
            // before creating new slaves we have to compact our database
            // to keep transfers to a minimum and to make sure that there
            // is a db file for new slices
            try {
                // compact db
                slices.compactDB();
                diskLogger.cleanLog();
                slices.completeDBCompaction();
            } catch (Exception ex) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                MessageUtils.marshallException(rq, ex);
                return;
            }
            
            // notify all slaves...
            VolumeInfo ve = slices.getVolumeById(sliceID.getVolumeId());
            
            // create request object containing all slice info
            List<Object> args2 = new LinkedList();
            args2.add(slID);
            args2.add(new ServiceUUID(config.getUUID().toString()).getAddress().getHostName() + ":"
                + config.getPort());
            args2.add(ve.getName());
            args2.add(ve.getAcPolicyId());
            args2.add(ve.getOsdPolicyId());
            args2.add(ve.getOsdPolicyArgs());
            args2.add(ve.getPartitioningPolicyId());
            
            // send requests...
            for (InetSocketAddress slave : addrs) {
                mrcClient.sendRPC(slave, ".RnewSlaveSlice", args2, null, null).waitForResponse();
            }
            
            // ack to client
            rq.getPinkyRequest().setResponse(HTTPUtils.SC_OKAY);
        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, e);
            MessageUtils.marshallException(rq, e);
        }
    }
    
    /**
     * should be called on start up to ensure that all slave slices have the
     * most recent data
     */
    public void init() {
        // check all slave volumes
        for (SliceID sl : slices.getSliceList()) {
            SliceInfo info = slices.getSliceInfo(sl);
            ReplicationMechanism rm = info.getReplicationMechanism();
            if (rm instanceof SlaveReplicationMechanism) {
                try {
                    fetchMasterStatus(sl);
                } catch (IOException ex) {
                    Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                }
            } else if (rm instanceof MasterReplicationMechanism) {
                info.setReplicating(true);
                info.setStatus(SliceInfo.SliceStatus.ONLINE);
            } else {
                info.setStatus(SliceInfo.SliceStatus.ONLINE);
            }
        }
    }
    
    /**
     * return details of slices and replication info
     */
    private void getInfos(MRCRequest rq) {
        List<Object> infos = new LinkedList();
        for (SliceID slID : slices.getSliceList()) {
            Map<String, Object> slInfo = new HashMap();
            slInfo.put("sliceID", slID.toString());
            SliceInfo info = slices.getSliceInfo(slID);
            slInfo.put("replicationMechanism", info.getReplicationMechanism().getClass()
                    .getSimpleName());
            slInfo.put("viewID", info.getCurrentViewID());
            slInfo.put("sequenceID", info.getCurrentSequenceID() - 1);
            slInfo.put("replicationActive", info.isReplicating());
            slInfo.put("status", info.getStatus().toString());
            VolumeInfo ve;
            try {
                ve = slices.getVolumeById(slID.getVolumeId());
                slInfo.put("volumeName", ve.getName());
            } catch (Exception ex) {
                slInfo.put("volumeName", "<unavailable>");
            }
            
            if (info.getReplicationMechanism() instanceof SlaveReplicationMechanism) {
                slInfo.put("master",
                    ((SlaveReplicationMechanism) info.getReplicationMechanism()).master.toString());
            }
            if (info.getReplicationMechanism() instanceof MasterReplicationMechanism) {
                List<String> sl = new LinkedList();
                for (InetSocketAddress isa : ((MasterReplicationMechanism) info
                        .getReplicationMechanism()).slaves) {
                    sl.add(isa.toString());
                }
                slInfo.put("slaves", sl);
            }
            infos.add(slInfo);
        }
        MessageUtils.marshallResponse(rq, infos);
    }
    
    protected void changeStatus(MRCRequest rq) {
        try {
            assert (rq != null);
            assert (rq.getPinkyRequest() != null);
            
            Object o = MessageUtils.unmarshallRequest(rq);
            List<Object> args = (List) o;
            String slID = (String) args.get(0);
            String newStatus = (String) args.get(1);
            
            // try to get slice
            SliceInfo info = slices.getSliceInfo(new SliceID(slID));
            if (info == null)
                throw new Exception("No such slice on this server");
            
            if (newStatus.equals("ONLINE")) {
                info.setStatus(SliceInfo.SliceStatus.ONLINE);
            } else if (newStatus.equals("OFFLINE")) {
                info.setStatus(SliceInfo.SliceStatus.OFFLINE);
            } else if (newStatus.equals("READONLY")) {
                info.setStatus(SliceInfo.SliceStatus.READONLY);
            } else {
                throw new Exception("Invalid status requested");
            }
            slices.modifySlice(info);
            
            rq.getPinkyRequest().setResponse(HTTPUtils.SC_OKAY);
            rq.details.persistentOperation = false;
            
        } catch (Exception e) {
            MessageUtils.marshallException(rq, e);
            rq.details.persistentOperation = false;
        }
    }
    
    protected void forceUpdate(MRCRequest rq) {
        try {
            assert (rq != null);
            assert (rq.getPinkyRequest() != null);
            
            Object o = MessageUtils.unmarshallRequest(rq);
            List<Object> args = (List) o;
            String slID = (String) args.get(0);
            
            // try to get slice
            SliceInfo info = slices.getSliceInfo(new SliceID(slID));
            if (info == null)
                throw new UserException(EC_SLICE_DOES_NOT_EXIST, "No such slice on this server");
            
            if (info.getReplicationMechanism() instanceof SlaveReplicationMechanism) {
                fetchMasterStatus(info.sliceID);
            } else {
                throw new UserException("Only slave slices can be updated");
            }
            
            rq.getPinkyRequest().setResponse(HTTPUtils.SC_OKAY);
            rq.details.persistentOperation = false;
            
        } catch (Exception e) {
            MessageUtils.marshallException(rq, e);
            rq.details.persistentOperation = false;
        }
    }
    
    public int getQLength() {
        return this.requests.size();
    }
    
    /**
     * a listener for syncing remote operations. It does not do anything but
     * prints a message in the case of an error.
     */
    public static class StupidListener implements SyncListener {
        
        public SliceInfo info;
        
        public StupidListener(SliceInfo info) {
            this.info = info;
        }
        
        public void synced(LogEntry entry) {
            // who cares
        }
        
        public void failed(LogEntry entry, Exception ex) {
            // FIXME: should do something but don't know what...
            Logging.logMessage(Logging.LEVEL_ERROR, this, "SYNC TO DISK FAILED");
            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
        }
        
    }
    
}
