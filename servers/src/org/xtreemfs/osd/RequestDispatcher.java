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
 * AUTHORS: Jan Stender (ZIB), Felix Langner (ZIB)
 */

package org.xtreemfs.osd;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.xtreemfs.common.Request;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.striping.Location;
import org.xtreemfs.foundation.speedy.SpeedyRequest;
import org.xtreemfs.osd.ops.Operation;
import org.xtreemfs.osd.stages.Stage;
import org.xtreemfs.osd.stages.StageStatistics;

public interface RequestDispatcher {

    public static enum Operations {
        READ, WRITE, STATUS_PAGE, FETCH_GMAX, TRUNCATE, TRUNCATE_LOCAL, DELETE,
        OFT_DELETE, DELETE_LOCAL, GET_PROTOCOL_VERSION, SHUTDOWN, CHECK_OBJECT,
        GMAX, CLOSE_FILE, GET_STATS, STATS_CONFIG, ACQUIRE_LEASE, RETURN_LEASE,
        CLEAN_UP, FETCH_AND_WRITE_REPLICA
    }

    public static enum Stages {
        PARSER, AUTH, STORAGE, DELETION, STATS, REPLICATION
    }

    public Operation getOperation(Operations opCode);

    public Stage getStage(Stages stage);

    public OSDConfig getConfig();

    public StageStatistics getStatistics();

    public boolean isHeadOSD(Location xloc);

    public void sendSpeedyRequest(Request originalRequest,
        SpeedyRequest speedyRq, InetSocketAddress server) throws IOException;

    public void sendUDP(ReusableBuffer data, InetSocketAddress receiver);

    public void requestFinished(OSDRequest rq);

    public void shutdown();

    public DIRClient getDIRClient();
}
