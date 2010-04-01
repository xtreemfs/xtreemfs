/*  Copyright (c) 2009 Barcelona Supercomputing Center - Centro Nacional
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
 * AUTHORS: Bjoern Kolbeck (ZIB)
 */
package org.xtreemfs.osd.operations;


import org.xtreemfs.common.Capability;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.xloc.XLocations;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.Lock;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_lock_acquireRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_lock_acquireResponse;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_pingRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_pingResponse;
import org.xtreemfs.interfaces.VivaldiCoordinates;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCException;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.stages.PreprocStage.LockOperationCompleteCallback;
import org.xtreemfs.osd.stages.VivaldiStage;
import org.xtreemfs.osd.vivaldi.VivaldiNode;

/**
 *
 * <br>15.06.2009
 */
public class VivaldiPingOperation extends OSDOperation {
    final int procId;

    final String sharedSecret;

    final ServiceUUID localUUID;

    public VivaldiPingOperation(OSDRequestDispatcher master) {
        super(master);
        procId = xtreemfs_pingRequest.TAG;
        sharedSecret = master.getConfig().getCapabilitySecret();
        localUUID = master.getConfig().getUUID();
    }

    @Override
    public int getProcedureId() {
        return procId;
    }

    @Override
    public void startRequest(final OSDRequest rq) {
        final xtreemfs_pingRequest args = (xtreemfs_pingRequest) rq
                .getRequestArgs();
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG,Category.all,this,"vivaldi ping with coordinates %s",VivaldiNode.coordinatesToString(args.getCoordinates()));
        }


        master.getVivaldiStage().getVivaldiCoordinates(args.getCoordinates(), rq, new VivaldiStage.VivaldiPingCallback() {

            @Override
            public void coordinatesCallback(VivaldiCoordinates myCoordinates, Exception error) {
                postGetCoordinates(rq, args, myCoordinates, error);
            }
        });

    }

    public void postGetCoordinates(final OSDRequest rq, xtreemfs_pingRequest args,
            VivaldiCoordinates coordinates, Exception error) {
        if (error != null) {
            if (error instanceof ONCRPCException) {
                rq.sendException((ONCRPCException) error);
            } else {
                rq.sendInternalServerError(error);
            }
        } else {
            xtreemfs_pingResponse resp = new xtreemfs_pingResponse(coordinates);
            rq.sendSuccess(resp);
        }
    }


    @Override
    public yidl.runtime.Object parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        xtreemfs_pingRequest rpcrq = new xtreemfs_pingRequest();
        rpcrq.unmarshal(new XDRUnmarshaller(data));

        return rpcrq;
    }

    @Override
    public boolean requiresCapability() {
        return false;
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
