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

package org.xtreemfs.mrc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.common.HeartbeatThread.ServiceDataGenerator;
import org.xtreemfs.common.auth.AuthenticationProvider;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.RPCClient;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.foundation.LifeCycleListener;
import org.xtreemfs.foundation.json.JSONCharBufferString;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.pinky.HTTPUtils;
import org.xtreemfs.foundation.pinky.PinkyRequest;
import org.xtreemfs.foundation.pinky.PinkyRequestListener;
import org.xtreemfs.foundation.pinky.PipelinedPinky;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.foundation.speedy.SpeedyResponseListener;
import org.xtreemfs.mrc.brain.BrainHelper;
import org.xtreemfs.mrc.brain.BrainRequestListener;
import org.xtreemfs.mrc.brain.BrainStage;
import org.xtreemfs.mrc.brain.storage.DiskLogger;
import org.xtreemfs.mrc.brain.storage.InvalidLogEntryException;
import org.xtreemfs.mrc.brain.storage.LogEntry;
import org.xtreemfs.mrc.brain.storage.SyncListener;
import org.xtreemfs.mrc.osdselection.OSDStatusManager;
import org.xtreemfs.mrc.replication.ReplicationManager;
import org.xtreemfs.mrc.replication.ReplicationRequestListener;
import org.xtreemfs.mrc.slices.SliceInfo;
import org.xtreemfs.mrc.slices.SliceManager;
import org.xtreemfs.mrc.slices.VolumeInfo;
import org.xtreemfs.mrc.utils.MessageUtils;

/**
 * This class comtains the workflow of the MRC server and directs the requestst
 * to the appropriate stages
 *
 * @author bjko
 */
public class RequestController implements SpeedyResponseListener, PinkyRequestListener,
    SyncListener, BrainRequestListener, ReplicationRequestListener, LifeCycleListener {

    private static final String      CMD_SHUTDOWN  = ".shutdown";

    private static final String      CMD_DBDUMP    = ".dumpdb";

    private static final String      CMD_DBRESTORE = ".restoredb";

    private final PipelinedPinky     pinkyStage;

    private final BrainStage         brainStage;

    private final MultiSpeedy        speedyStage;

    private final DiskLogger         loggerStage;

    private final ReplicationManager replicationStage;

    private final MRCConfig          config;

    private final OSDStatusManager   osdMonitor;

    private final SliceManager       slices;

    private final Timer              qTimer;

    private final Timer              cpTimer;

    private final String             authString;

    private final DIRClient          dirClient;

    private final MRCClient          mrcClient;

    private final QMonitor           qMon;

    private final AtomicInteger      dbgId         = new AtomicInteger(1);

    private long                     lastRequestTimeStamp;

    private final PolicyContainer    policyContainer;

    private final HeartbeatThread    heartbeatThread;

    /** Creates a new instance of RequestController */
    public RequestController(final MRCConfig config) throws Exception {

        try {

            this.config = config;

            // generate an authorization string for Directory Service operations
            authString = NullAuthProvider.createAuthString(config.getUUID().toString(), config
                    .getUUID().toString());

            policyContainer = new PolicyContainer(config);

            final AuthenticationProvider authProvider = policyContainer.getAuthenticationProvider();
            authProvider.initialize(config.isUsingSSL());
            if (Logging.isInfo())
                Logging.logMessage(Logging.LEVEL_INFO, this, "using authentication provider '"
                    + authProvider.getClass().getName() + "'");

            qTimer = new Timer();
            cpTimer = new Timer();

            File dbDir = new File(config.getDbDir());
            if (!dbDir.exists())
                dbDir.mkdirs();

            speedyStage = config.isUsingSSL() ? new MultiSpeedy(new SSLOptions(config
                    .getServiceCredsFile(), config.getServiceCredsPassphrase(), config
                    .getServiceCredsContainer(), config.getTrustedCertsFile(), config
                    .getTrustedCertsPassphrase(), config.getTrustedCertsContainer(), false))
                : new MultiSpeedy();
            speedyStage.registerSingleListener(this);
            speedyStage.setLifeCycleListener(this);

            Logging.logMessage(Logging.LEVEL_DEBUG, this, "use SSL=" + config.isUsingSSL());

            pinkyStage = config.isUsingSSL() ? new PipelinedPinky(config.getPort(), config
                    .getAddress(), this, new SSLOptions(config.getServiceCredsFile(), config
                    .getServiceCredsPassphrase(), config.getServiceCredsContainer(), config
                    .getTrustedCertsFile(), config.getTrustedCertsPassphrase(), config
                    .getTrustedCertsContainer(), false)) : new PipelinedPinky(config.getPort(),
                config.getAddress(), this);
            pinkyStage.setLifeCycleListener(this);

            dirClient = new DIRClient(speedyStage, config.getDirectoryService());

            TimeSync.initialize(dirClient, config.getRemoteTimeSync(), config.getLocalClockRenew(),
                authString);

            UUIDResolver.start(dirClient, 10 * 1000, 600 * 1000);
            UUIDResolver.addLocalMapping(config.getUUID(), config.getPort(), config.isUsingSSL());

            mrcClient = new MRCClient(speedyStage);

            osdMonitor = new OSDStatusManager(config, dirClient, policyContainer, authString);
            osdMonitor.setLifeCycleListener(this);

            slices = new SliceManager(config);
            slices.init();
            try {
                // cross stage listeners
                slices.addVolumeChangeListener(osdMonitor);
            } catch (Exception ex) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
            }

            loggerStage = new DiskLogger(config.getAppendLogFileName(), config.isNoFsync());
            loggerStage.setLifeCycleListener(this);

            replicationStage = new ReplicationManager(config, mrcClient, loggerStage, slices);
            replicationStage.registerBrainListener(this);
            replicationStage.registerReplicationListener(this);
            replicationStage.setLifeCycleListener(this);

            brainStage = new BrainStage(config, dirClient, osdMonitor, slices, policyContainer,
                authProvider, authString);
            brainStage.setRequestListener(this);
            brainStage.setLifeCycleListener(this);

            ServiceDataGenerator gen = new ServiceDataGenerator() {
                public Map<String, Map<String, Object>> getServiceData() {

                    String uuid = RequestController.this.config.getUUID().toString();

                    OperatingSystemMXBean osb = ManagementFactory.getOperatingSystemMXBean();
                    String load = String.valueOf((int) (osb.getSystemLoadAverage() * 100 / osb
                            .getAvailableProcessors()));

                    long totalRAM = Runtime.getRuntime().maxMemory();
                    long usedRAM = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();

                    // get service data
                    Map<String, Map<String, Object>> map = new HashMap<String, Map<String, Object>>();
                    map.put(uuid, RPCClient.generateMap("type", "MRC", "load", load,
                        "prot_versions", VersionManagement.getSupportedProtVersAsString(),
                        "totalRAM", Long.toString(totalRAM),
                         "usedRAM", Long.toString(usedRAM),
                         "geoCoordinates",config.getGeoCoordinates()));

                    // get volume data
                    for (VolumeInfo vol : slices.getVolumes()) {
                        if (!vol.isRegisterAtDS())
                            continue;
                        Map<String, Object> dsVolumeInfo = BrainHelper.createDSVolumeInfo(vol,
                            osdMonitor, uuid);
                        map.put(vol.getId(), dsVolumeInfo);
                    }

                    return map;
                }
            };

            heartbeatThread = new HeartbeatThread("MRC Heartbeat Thread", dirClient, config
                    .getUUID(), gen, authString,config);

            replicationStage.setBrainStage(brainStage);

            qMon = new QMonitor(pinkyStage, brainStage, speedyStage, loggerStage, replicationStage);

            // recover database / replay log if necessary
            brainStage.restoreDB();
            replayLog();

        } catch (Exception exc) {
            exc.printStackTrace();
            shutdown();
            throw exc;
        }
    }

    public void startup() throws Exception {

        try {
            speedyStage.start();
            speedyStage.waitForStartup();

            osdMonitor.start();
            replicationStage.start();
            replicationStage.init();
            brainStage.start();
            loggerStage.start();
            pinkyStage.start();
            heartbeatThread.start();

            osdMonitor.waitForStartup();
            brainStage.waitForStartup();
            loggerStage.waitForStartup();
            pinkyStage.waitForStartup();
            replicationStage.waitForStartup();
            heartbeatThread.waitForStartup();

            qTimer.scheduleAtFixedRate(qMon, 0, 1000);

            lastRequestTimeStamp = TimeSync.getGlobalTime();
            TimerTask cpTask = new TimerTask() {
                public void run() {
                    if (TimeSync.getGlobalTime() - lastRequestTimeStamp > config
                            .getIdleIntervalForDBCheckpoint()
                        && loggerStage.getLogFileSize() > config.getLogFileSizeForDBCheckpoint())

                        try {
                            checkpoint();
                        } catch (Exception exc) {
                            Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
                        }
                }
            };

            cpTimer.scheduleAtFixedRate(cpTask, config.getDBCheckpointInterval(), config
                    .getDBCheckpointInterval());

            Logging.logMessage(Logging.LEVEL_INFO, this, "operational on port " + config.getPort());

        } catch (Exception exc) {
            // shutdown();
            throw exc;
        }

    }

    /**
     * Creates a database checkpoint.
     */
    public void checkpoint() {

        // FIXME: block all incoming requests and make sure
        // that no requests are being processed while
        // checkpointing! The current approach is a
        // workaround, not a valid solution!!!

        try {
            brainStage.block(); // block new requests during CP
            brainStage.checkpointDB(); // checkpoint the DB
            loggerStage.cleanLog(); // clean the DB log
            brainStage.completeDBCheckpoint(); // complete CP
            brainStage.unblock(); // process incoming requests

        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "CANNOT CREATE DATABASE CHECKPOINT!");
            Logging.logMessage(Logging.LEVEL_ERROR, this, e);
        }
    }

    public void replayLog() throws IOException {
        File lf = new File(config.getAppendLogFileName());

        if (lf.exists()) {

            int numOps = 0;
            Logging.logMessage(Logging.LEVEL_INFO, this,
                "there is an old log file. Starting log replay..");

            FileInputStream fis = new FileInputStream(lf);
            FileChannel fc = fis.getChannel();

            boolean replaySuccess = true;

            while (fc.position() < fc.size()) {

                LogEntry l = null;
                try {
                    // unmarshall log entry from disk log
                    l = new LogEntry(fc);
                    // parse the log entry contents
                    Object args = null;
                    if (l.payload != null) {
                        // parse JSONrequest.pr.requestBody.position(0);
                        CharBuffer utf8buf = HTTPUtils.ENC_UTF8.decode(l.payload.getBuffer());
                        args = JSONParser.parseJSON(new JSONCharBufferString(utf8buf));
                    }

                    // this one works sync
                    brainStage.replayLogEntry(l.operationName, l.userID, l.groupID, args);

                    SliceInfo info = slices.getSliceInfo(l.slID);
                    assert (info != null);

                    if (info.isDeleted())
                        slices.removeSliceFromIndex(l.slID);
                    else
                        info.setNextSequenceID(l.sequenceID + 1);

                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "replayed operation "
                        + l.operationName);
                    numOps++;

                } catch (JSONException ex) {
                    Logging.logMessage(Logging.LEVEL_WARN, this,
                        "log entry with invalid JSON message encountered: " + ex);
                } catch (InvalidLogEntryException ex) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this,
                        "encountered corrupted entry in logfile!");
                    if (fc.size() - (fc.position() + ex.getLength()) > 0) {
                        Logging.logMessage(Logging.LEVEL_ERROR, this, "DON'T PANIC");
                        Logging.logMessage(Logging.LEVEL_ERROR, this,
                            "FOUND A CORRUPTED ENTRY IN THE LOG FILE WHICH IS NOT THE LAST ONE!!!");
                        Logging.logMessage(Logging.LEVEL_ERROR, this,
                            "CANNOT START UP SERVICE, PLEASE CHECK LOGFILE");
                        System.exit(1);
                    }
                } catch (Exception ex) {
                    Logging.logMessage(Logging.LEVEL_ERROR, this, "DON'T PANIC");
                    Logging.logMessage(Logging.LEVEL_ERROR, this,
                        "CANNOT START UP SERVICE, PLEASE CHECK LOGFILE");
                    Logging
                            .logMessage(Logging.LEVEL_ERROR, this, "cannot execute log entry: "
                                + ex);
                    Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                    System.exit(1);
                }
            }

            Logging.logMessage(Logging.LEVEL_INFO, this, "replayed " + numOps + " operations.");

            if (replaySuccess && (numOps > 0)) {
                // make a checkpoint to keep the log short for the next crash
                // ;-)
                this.checkpoint();
            }
        }

    }

    public void truncateLog() throws IOException {
        // TODO: check whether it is necessary to stop and restart the logger
        // thread
        if (loggerStage != null)
            loggerStage.cleanLog();

        // if it is null we are in the startup phase and do not need to truncate
        // the log
    }

    public void shutdown() throws Exception {

        // create status page snapshot for debugging purposes
        try {
            String statusPageSnapshot = getStatusPage();
            BufferedWriter writer = new BufferedWriter(new FileWriter(config.getDbDir()
                + "/.status.html"));
            writer.write(statusPageSnapshot);
            writer.close();
        } catch (Exception exc) {
            // ignore
        }

        if (slices != null && loggerStage != null
            && loggerStage.getLogFileSize() > config.getLogFileSizeForDBCheckpoint())
            checkpoint();

        if (heartbeatThread != null)
            heartbeatThread.shutdown();
        if (qTimer != null)
            qTimer.cancel();
        if (cpTimer != null)
            cpTimer.cancel();
        if (pinkyStage != null)
            pinkyStage.shutdown();
        if (osdMonitor != null)
            osdMonitor.shutdown();
        if (brainStage != null)
            brainStage.shutdown();
        if (loggerStage != null)
            loggerStage.shutdown();
        if (speedyStage != null)
            speedyStage.shutdown();
        if (replicationStage != null)
            replicationStage.shutdown();

        if (heartbeatThread != null && heartbeatThread != Thread.currentThread())
            heartbeatThread.waitForShutdown();
        if (pinkyStage != null && pinkyStage != Thread.currentThread())
            pinkyStage.waitForShutdown();
        if (osdMonitor != null && osdMonitor != Thread.currentThread())
            osdMonitor.waitForShutdown();
        if (brainStage != null && brainStage != Thread.currentThread())
            brainStage.waitForShutdown();
        if (loggerStage != null && loggerStage != Thread.currentThread())
            loggerStage.waitForShutdown();
        if (speedyStage != null && speedyStage != Thread.currentThread())
            speedyStage.waitForShutdown();
        if (replicationStage != null && replicationStage != Thread.currentThread())
            replicationStage.waitForShutdown();

    }

    /**
     * This operation KILLS all threads immediately.
     *
     * @attention EXTRA EVIL OPERATION! FOR TESTING PURPOSES ONLY! DO NOT USE
     *            OTHERWISE!
     */
    public void dropDead() throws Exception {
        heartbeatThread.shutdown();
        qTimer.cancel();
        cpTimer.cancel();
        pinkyStage.shutdown();
        osdMonitor.shutdown();
        brainStage.shutdown();
        loggerStage.shutdown();
        speedyStage.shutdown();
        replicationStage.shutdown();
    }

    // --------------------- LISTENERS ------------------------------

    public void receiveRequest(SpeedyRequest theRequest) {
        // all SpeedyRequests come from the brain
        // hence, we give requeue it w/ brain
        // TODO: handle theRequest.attachment over to brain
        assert (theRequest.attachment != null);
        brainStage.processRequest(theRequest.attachment);
    }

    /**
     * Here starts the action of the RequestController
     */
    public void receiveRequest(PinkyRequest theRequest) {
        MRCRequest rq = new MRCRequest(theRequest);
        theRequest.debugRqId = dbgId.getAndIncrement();
        Logging.logMessage(Logging.LEVEL_DEBUG, this, "received request #" + theRequest.debugRqId);
        try {

            if (theRequest.requestURI.charAt(0) == '/') {

                if (theRequest.requestURI.length() == 1) {

                    // generate status HTTP page
                    String statusPage = getStatusPage();

                    ReusableBuffer bbuf = ReusableBuffer.wrap(statusPage
                            .getBytes(HTTPUtils.ENC_ASCII));
                    theRequest.setResponse(HTTPUtils.SC_OKAY, bbuf, HTTPUtils.DATA_TYPE.HTML);
                    pinkyStage.sendResponse(rq.getPinkyRequest());
                    return;

                } else
                    // process normal request
                    theRequest.requestURI = theRequest.requestURI.substring(1);
            }

            if (theRequest.requestURI.length() > 0) {
                if (theRequest.requestURI.charAt(0) == '.') {
                    // system command
                    handleSystemCall(rq);
                } else {
                    try {
                        // everything else goes directly to the brain

                        // handle over tosp the brainStage
                        brainStage.processRequest(rq);
                    } catch (IllegalStateException e) {
                        // brain's queue is full
                        theRequest.setClose(true);
                        theRequest.setResponse(HTTPUtils.SC_SERV_UNAVAIL);
                        pinkyStage.sendResponse(theRequest);
                    }

                }
            } else {
                theRequest.setClose(true);
                theRequest.setResponse(HTTPUtils.SC_BAD_REQUEST);
                pinkyStage.sendResponse(theRequest);
            }

        } catch (IndexOutOfBoundsException e) {
            theRequest.setClose(true);
            theRequest.setResponse(HTTPUtils.SC_BAD_REQUEST);
            pinkyStage.sendResponse(theRequest);
        } catch (Exception exc) {
            theRequest.setClose(true);
            theRequest.setResponse(HTTPUtils.SC_SERVER_ERROR);
            pinkyStage.sendResponse(theRequest);
        }
    }

    public void handleSystemCall(MRCRequest rq) {
        try {
            if (rq.getPinkyRequest().requestURI.startsWith(".R")) {
                replicationStage.addRequest(rq);
            } else if (rq.getPinkyRequest().requestURI.equals(CMD_SHUTDOWN)) {
                // shutdown the whole MRC!!!
                rq.getPinkyRequest().setResponse(HTTPUtils.SC_OKAY);
                pinkyStage.sendResponse(rq.getPinkyRequest());
                shutdown();
            } else if (rq.getPinkyRequest().requestURI.equals(CMD_DBDUMP)) {
                // dump database to file
                try {
                    List args = (List) MessageUtils.unmarshallRequest(rq);
                    dumpDB((String) args.get(0));
                    rq.getPinkyRequest().setResponse(HTTPUtils.SC_OKAY);
                } catch (Exception exc) {
                    rq.getPinkyRequest().setResponse(HTTPUtils.SC_USER_EXCEPTION, "could not create dump file");
                }

                pinkyStage.sendResponse(rq.getPinkyRequest());
            } else if (rq.getPinkyRequest().requestURI.equals(CMD_DBRESTORE)) {
                // dump database to file
                try {
                    List args = (List) MessageUtils.unmarshallRequest(rq);
                    restoreDBFromDump((String) args.get(0));
                    rq.getPinkyRequest().setResponse(HTTPUtils.SC_OKAY);
                } catch (Exception exc) {
                    rq.getPinkyRequest().setResponse(HTTPUtils.SC_USER_EXCEPTION, "could not process dump file: "
                        + OutputUtils.stackTraceToString(exc));
                }

                pinkyStage.sendResponse(rq.getPinkyRequest());
            } else {
                rq.getPinkyRequest().setResponse(HTTPUtils.SC_NOT_IMPLEMENTED);
                pinkyStage.sendResponse(rq.getPinkyRequest());
            }

        } catch (IOException ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
        }
    }

    public void brainRequestDone(MRCRequest rq) {

        lastRequestTimeStamp = TimeSync.getGlobalTime();

        if (rq.getPinkyRequest() == null) {
            if (rq.sr != null) {
                try {
                    speedyStage.sendRequest(rq.sr, rq.srEndpoint);
                } catch (Exception ex) {
                    rq.getPinkyRequest().setResponse(HTTPUtils.SC_SERVER_ERROR);
                    pinkyStage.sendResponse(rq.getPinkyRequest());
                    Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                }
            }
        } else {
            if (rq.getPinkyRequest().isReady()) {
                // we have a response we can pass on to the disk logger
                // or send a response to the client
                if (rq.details.persistentOperation) {
                    SliceInfo info = slices.getSliceInfo(rq.details.sliceId);

                    assert (info != null);
                    LogEntry e = new LogEntry(info.getCurrentViewID(), info.getNextSequenceID(),
                        rq.details.sliceId, DiskLogger.OPTYPE_MRC, rq.getPinkyRequest().requestURI, rq.details.userId, rq.details.groupIds
                                .get(0), rq.getPinkyRequest().requestBody.createViewBuffer(), rq);
                    e.registerListener(this);
                    assert (rq.logEntry == null);
                    rq.logEntry = e;
                    loggerStage.append(e);
                    assert (e.payload == rq.logEntry.payload);
                } else {
                    // direct response!
                    pinkyStage.sendResponse(rq.getPinkyRequest());
                }
            } else {
                // if there is a speedy request we have to send it out
                if (rq.sr != null) {
                    try {
                        rq.sr.attachment = rq;
                        speedyStage.sendRequest(rq.sr, rq.srEndpoint);
                    } catch (Exception ex) {
                        rq.details.persistentOperation = false;
                        rq.getPinkyRequest().setResponse(HTTPUtils.SC_SERVER_ERROR);
                        pinkyStage.sendResponse(rq.getPinkyRequest());
                        Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
                    }
                } else {
                    throw new RuntimeException(
                        "[ E | RequestController ] Brain must send either a response or a SpeedyRequest, but sr is null!");
                }
            }
        }
    }

    /**
     * called by the loggerStage after the Entry was written to disk
     */
    public void synced(LogEntry entry) {

        MRCRequest rq = entry.attachment;
        assert (entry == rq.logEntry);

        // initiate replication
        if (replicationStage.replicate(rq)) {
            // okay! send response to client
            // otherwise we have to wait for
            // the replication to finish

            // check if a deferred deletion of the slice is necessary
            if (rq.details.sliceId != null && slices.getSliceInfo(rq.details.sliceId).isDeleted())
                slices.removeSliceFromIndex(rq.details.sliceId);

            pinkyStage.sendResponse(rq.getPinkyRequest());
        }
    }

    public void failed(LogEntry entry, Exception ex) {
        MRCRequest rq = entry.attachment;
        BufferPool.free(entry.payload);
        // all we can do is send a 500 to the client
        Logging.logMessage(Logging.LEVEL_ERROR, this, "write to disk log failed");
        Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
        rq.getPinkyRequest().setResponse(500);
        rq.details.persistentOperation = false;
        pinkyStage.sendResponse(rq.getPinkyRequest());
    }

    public void replicationDone(MRCRequest rq) {
        BufferPool.free(rq.logEntry.payload);

        // check if a deferred deletion of the slice is necessary
        if (rq.details.sliceId != null && slices.getSliceInfo(rq.details.sliceId).isDeleted())
            slices.removeSliceFromIndex(rq.details.sliceId);

        pinkyStage.sendResponse(rq.getPinkyRequest());
    }

    public void dumpDB(String dumpFilePath) throws Exception {
        brainStage.dumpDB(dumpFilePath);
    }

    public void restoreDBFromDump(String dumpFilePath) throws Exception {
        brainStage.restoreDBFromDump(dumpFilePath);
    }

    public void crashPerformed() {
        try {
            shutdown();
        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, e);
        }
    }

    public void shutdownPerformed() {
        // ignore
    }

    public void startupPerformed() {
        // ignore
    }

    public String getStatusPage() throws Exception {

        int pinkyQL = pinkyStage.getTotalQLength();
        int pinkyCons = pinkyStage.getNumConnections();
        String me = "http://" + InetAddress.getLocalHost().getHostName() + ":" + config.getPort();
        String dirServiceURL = "http://" + config.getDirectoryService().getHostName() + ":"
            + config.getDirectoryService().getPort();

        StringBuffer rqTableBuf = new StringBuffer();
        long totalRequests = 0;
        for (String req : brainStage._statMap.keySet()) {

            long count = brainStage._statMap.get(req);
            totalRequests += count;

            rqTableBuf.append("<tr><td align=\"left\">'");
            rqTableBuf.append(req);
            rqTableBuf.append("'</td><td>");
            rqTableBuf.append(count);
            rqTableBuf.append("</td></tr>");
        }

        StringBuffer volTableBuf = new StringBuffer();
        List<VolumeInfo> vols = slices.getVolumes();
        for (VolumeInfo v : vols) {

            Map<String, Map<String, Object>> osdList = osdMonitor.getUsableOSDs(v.getId());

            volTableBuf.append("<tr><td align=\"left\">");
            volTableBuf.append(v.getName());
            volTableBuf
                    .append("</td><td><table border=\"0\" cellpadding=\"0\"><tr><td class=\"subtitle\">selectable OSDs</td><td align=\"right\">");
            Iterator<String> it = osdList.keySet().iterator();
            while (it.hasNext()) {
                final ServiceUUID osdUUID = new ServiceUUID(it.next());
                volTableBuf.append("<a href=\"");
                volTableBuf.append(osdUUID.toURL());
                volTableBuf.append("\">");
                volTableBuf.append(osdUUID);
                volTableBuf.append("</a>");
                if (it.hasNext())
                    volTableBuf.append(", ");
            }
            volTableBuf.append("</td></tr><tr><td class=\"subtitle\">striping policy</td><td>");
            volTableBuf.append(slices.getSliceDB(v.getId(), 1, 'r').getVolumeStripingPolicy());
            volTableBuf.append("</td></tr><tr><td class=\"subtitle\">access policy</td><td>");
            volTableBuf.append(v.getAcPolicyId());
            volTableBuf.append("</td></tr><tr><td class=\"subtitle\">osd policy</td><td>");
            volTableBuf.append(v.getOsdPolicyId());
            volTableBuf.append("</td></tr><tr><td class=\"subtitle\">partitioning policy</td><td>");
            volTableBuf.append(v.getPartitioningPolicyId());
            volTableBuf.append("</td></tr></table></td></tr>");
        }

        String status = "<html><head><title>XtreemFS MRC "
            + me
            + "</title>"
            + "<style>td.title { border-top:1px solid black; border-bottom: 1px solid black; background-color:#EEEEEE; font-weight:bold;}</style>"
            + "<style>td.subtitle { text-align:right; border-right:10px solid white;}</style>"
            + "</head>"
            + "<body>"
            + "<H1><A HREF=\"http://www.XtreemFS.org\"><IMG SRC=\"http://www.xtreemfs.com/imgs/Logo_200px.jpg\" border=\"0\" align=\"bottom\"></A> MRC "
            + me
            + "</H1><BR><table border=\"0\">"
            + "<tr><td colspan=\"2\" class=\"title\">Configuration</td></tr>"
            + "<tr><td align=\"left\">port</td><td>"
            + config.getPort()
            + "</td></tr><tr class=\"title\"><td align=\"left\">Directory Service</td><td><a href=\""
            + dirServiceURL + "\">" + dirServiceURL
            + "</a></td></tr><tr><td align=\"left\">debug level</td><td>" + config.getDebugLevel()
            + "</td></tr>" + "<tr><td colspan=\"2\" class=\"title\">Load</td></tr>"
            + "<tr><td align=\"left\">Pinky #connections</td><td>" + pinkyCons
            + "</td></tr><tr><td align=\"left\">Pinky total requests in queue</td><td>" + pinkyQL
            + "</td></tr>" + "<tr><td align=\"left\">last request received</td><td>"
            + lastRequestTimeStamp + " (" + new Date(lastRequestTimeStamp) + ")" + "</td></tr>"
            + "<tr><td colspan=\"2\" class=\"title\">Operation Statistics</td></tr>"
            + "<tr><td align=\"left\">total requests</td><td>" + totalRequests + "</td></tr>"
            + rqTableBuf + "<tr><td colspan=\"2\" class=\"title\">Volumes</td></tr>" + volTableBuf
            + "<tr><td colspan=\"2\" class=\"title\">Database Statistics</td></tr>"
            + "<tr><td align=\"left\">total database size</td><td>"
            + OutputUtils.formatBytes(brainStage.getTotalDBSize()) + "</td></tr>"
            + "<tr><td align=\"left\">database log size</td><td>"
            + OutputUtils.formatBytes(loggerStage.getLogFileSize()) + "</td></tr>"
            + "<tr><td align=\"left\">total #files</td><td>" + brainStage.getTotalNumberOfFiles()
            + "</td></tr>" + "<tr><td align=\"left\">total #directories</td><td>"
            + brainStage.getTotalNumberOfDirs() + "</td></tr>"
            + "<tr><td colspan=\"2\" class=\"title\">VM Info / Memory</td></tr>"
            + "<tr><td align=\"left\">Memory free</td><td>"
            + OutputUtils.formatBytes(Runtime.getRuntime().freeMemory()) + "</td></tr>"
            + "<tr><td align=\"left\">Memory total</td><td>"
            + OutputUtils.formatBytes(Runtime.getRuntime().totalMemory()) + "</td></tr>"
            + "<tr><td align=\"left\">Memory max</td><td>"
            + OutputUtils.formatBytes(Runtime.getRuntime().maxMemory()) + "</td></tr>"
            + "<tr><td align=\"left\">BufferPool stats</td><td><PRE>" + BufferPool.getStatus()
            + "</PRE></td></tr>" + "<tr><td align=\"left\">avail. processors</td><td>"
            + Runtime.getRuntime().availableProcessors() + "</td></tr>"
            + "<tr><td colspan=\"2\" class=\"title\">Time</td></tr>"
            + "<tr><td align=\"left\">global xtreemfs time</td><td>" + TimeSync.getGlobalTime()
            + " (" + new Date(TimeSync.getGlobalTime()) + ")" + "</td></tr>"
            + "<tr><td align=\"left\">global time sync interval</td><td>"
            + config.getRemoteTimeSync() + " ms" + "</td></tr>"
            + "<tr><td align=\"left\">local time</td><td>" + TimeSync.getLocalSystemTime() + " ("
            + new Date(TimeSync.getLocalSystemTime()) + ")" + "</td></tr>"
            + "<tr><td align=\"left\">local time granularity</td><td>"
            + TimeSync.getLocalRenewInterval() + " ms</td></tr>"
            + "<tr><td colspan=\"2\" class=\"title\">UUID Mapping Cache</td></tr>"
            + "<tr><td colspan=\"2\"><PRE>" + UUIDResolver.getCache() + "</PRE></TD></TR>"
            + "</TD></TR></table></body></html>";

        return status;
    }

}
