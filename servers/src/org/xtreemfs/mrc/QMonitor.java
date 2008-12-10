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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.TimerTask;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.pinky.PipelinedPinky;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.mrc.brain.BrainStage;
import org.xtreemfs.mrc.brain.storage.DiskLogger;
import org.xtreemfs.mrc.replication.ReplicationManager;

/**
 * Monitors all queues and reduces or shuts down client queues if stage queues
 * get too full.
 *
 * @author bjko
 */
public class QMonitor extends TimerTask {

    public static final String logfile = "qmonitor.log";

    public static PrintWriter  log;

    final PipelinedPinky       pinkyStage;

    final BrainStage           brainStage;

    final MultiSpeedy          speedyStage;

    final DiskLogger           loggerStage;

    final ReplicationManager   replicationStage;

    public enum QState {
        GREEN, YELLOW, ORANGE, RED
    };

    private QState          state;

    public static final int CLIENT_MAXQ_GREEN        = 400;

    public static final int CLIENT_MAXQ_YELLOW       = 50;

    public static final int CLIENT_MAXQ_ORANGE       = 10;

    public static final int CLIENT_MAXQ_RED          = 0;

    public static final int BRAINQ_THR_GREEN_YELLOW  = 500;

    public static final int BRAINQ_THR_YELLOW_GREEN  = 200;

    public static final int BRAINQ_THR_YELLOW_ORANGE = 1000;

    public static final int BRAINQ_THR_ORANGE_YELLOW = 400;

    public static final int BRAINQ_THR_ORANGE_RED    = 2000;

    public static final int BRAINQ_THR_RED_ORANGE    = 800;

    /** Creates a new instance of QMonitor */
    public QMonitor(PipelinedPinky pp, BrainStage bs, MultiSpeedy ms,
        DiskLogger dl, ReplicationManager rm) throws IOException {
        super();
        state = QState.GREEN;
        // log = new PrintWriter(new File(logfile));
        pinkyStage = pp;
        brainStage = bs;
        speedyStage = ms;
        loggerStage = dl;
        replicationStage = rm;
        pinkyStage.MAX_CLIENT_QUEUE = CLIENT_MAXQ_GREEN;
        pinkyStage.CLIENT_Q_THR = CLIENT_MAXQ_GREEN / 2;
    }

    public void run() {

        /*
         * first we try to find the longes Q
         */
        int maxQlen = brainStage.getQLength();
        if (maxQlen < loggerStage.getQLength()) {
            maxQlen = loggerStage.getQLength();
        }
        if (maxQlen < replicationStage.getQLength()) {
            maxQlen = replicationStage.getQLength();
        }
        // we want the total length
        if (maxQlen < speedyStage.getQLength()[0]) {
            maxQlen = replicationStage.getQLength();
        }

        boolean stateChanged = false;

        if ((state == QState.GREEN) && (maxQlen > BRAINQ_THR_GREEN_YELLOW)) {
            state = QState.YELLOW;
            stateChanged = true;

        }
        if ((state == QState.YELLOW) && (maxQlen > BRAINQ_THR_YELLOW_ORANGE)) {
            state = QState.ORANGE;
            stateChanged = true;
        }
        if ((state == QState.ORANGE) && (maxQlen > BRAINQ_THR_ORANGE_RED)) {
            state = QState.RED;
            stateChanged = true;
        }

        boolean wasCodeRed = false;

        if ((state == QState.RED) && (maxQlen < BRAINQ_THR_RED_ORANGE)) {
            // back from code red
            wasCodeRed = true;
            state = QState.ORANGE;
            stateChanged = true;
        }
        if ((state == QState.ORANGE) && (maxQlen < BRAINQ_THR_ORANGE_YELLOW)) {
            state = QState.YELLOW;
            stateChanged = true;
        }
        if ((state == QState.YELLOW) && (maxQlen < BRAINQ_THR_YELLOW_GREEN)) {
            state = QState.GREEN;
            stateChanged = true;

        }

        if (stateChanged) {
            // log.println("state now "+state);
        }

        if (state == QState.GREEN) {
            pinkyStage.MAX_CLIENT_QUEUE = CLIENT_MAXQ_GREEN;
            pinkyStage.CLIENT_Q_THR = CLIENT_MAXQ_GREEN / 2;
        } else if (state == QState.YELLOW) {
            pinkyStage.MAX_CLIENT_QUEUE = CLIENT_MAXQ_YELLOW;
            pinkyStage.CLIENT_Q_THR = CLIENT_MAXQ_YELLOW / 2;
        } else if (state == QState.ORANGE) {
            pinkyStage.MAX_CLIENT_QUEUE = CLIENT_MAXQ_ORANGE;
            pinkyStage.CLIENT_Q_THR = CLIENT_MAXQ_ORANGE / 2;
        } else if (state == QState.RED) {
            pinkyStage.MAX_CLIENT_QUEUE = 0;
            pinkyStage.CLIENT_Q_THR = 0;
        }
        if (wasCodeRed) {
            // make all channels readable again...
            Logging.logMessage(Logging.LEVEL_INFO, this, "resume reading");
            pinkyStage.restartReading();
        }

        // if (stateChanged) {
        // log.println(String.format("Pinky #clients: %5d MaxClientQ: %5d QThr:
        // %5d Total: %5d",
        // pinkyStage.getNumConnections(),pinkyStage.MAX_CLIENT_QUEUE,
        // pinkyStage.CLIENT_Q_THR,pinkyStage.getTotalQLength()));
        // int[] sq = speedyStage.getQLength();
        // log.println(String.format("Speedy NumServers: %5d SumQ:
        // %5d",sq[1],sq[0]));
        // log.println(String.format("Brain Q: %5d",brainStage.getQLength()));
        // log.println(String.format("Logger Q: %5d",loggerStage.getQLength()));
        // log.println(String.format("Replicat. Q:
        // %5d",replicationStage.getQLength()));
        // log.println();
        // log.flush();
        // }
    }

    public QState getState() {
        return state;
    }

    public int[] getSpeedyQueueLength() {
        return speedyStage.getQLength();
    }

    public int getPinkyConnections() {
        return pinkyStage.getNumConnections();
    }

    public int getBrainQueueLength() {
        return brainStage.getQLength();
    }

    public int getLoggerQueueLength() {
        return loggerStage.getQLength();
    }

    public int getReplicationQueueLength() {
        return replicationStage.getQLength();
    }

    public boolean cancel() {
        log.close();
        return true;
    }
}
