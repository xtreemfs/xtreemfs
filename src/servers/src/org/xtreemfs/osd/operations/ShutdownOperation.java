/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.operations;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_shutdownRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_shutdownResponse;
import org.xtreemfs.osd.ErrorCodes;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;

/**
 *
 * @author bjko
 */
public class ShutdownOperation extends OSDOperation {

    private final int procId;

    public ShutdownOperation(OSDRequestDispatcher master) {
        super(master);
        procId = xtreemfs_shutdownRequest.TAG;
    }

    @Override
    public int getProcedureId() {
        return procId;
    }

    @Override
    public void startRequest(OSDRequest rq) {

        //check password
        xtreemfs_shutdownRequest args = (xtreemfs_shutdownRequest)rq.getRequestArgs();

        UserCredentials uc = rq.getRPCRequest().getUserCredentials();

        if (uc.getPassword().equals(master.getConfig().getAdminPassword())) {
            //shutdown
            try {
                rq.sendSuccess(new xtreemfs_shutdownResponse());
                Thread.sleep(100);
                master.asyncShutdown();
            } catch (Throwable thr) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, "exception during shutdown");
                Logging.logError(Logging.LEVEL_ERROR, this, thr);
            }
        } else {
            rq.sendOSDException(ErrorCodes.AUTH_FAILED, "password is not valid");
        }
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public yidl.runtime.Object parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        xtreemfs_shutdownRequest rpcrq = new xtreemfs_shutdownRequest();
        rpcrq.unmarshal(new XDRUnmarshaller(data));

        return rpcrq;
    }

    @Override
    public boolean requiresCapability() {
        return false;
    }

}
