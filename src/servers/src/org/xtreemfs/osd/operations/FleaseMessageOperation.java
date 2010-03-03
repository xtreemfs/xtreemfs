/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.operations;

import java.net.InetSocketAddress;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_rwr_flease_msgRequest;
import org.xtreemfs.interfaces.OSDInterface.xtreemfs_rwr_flease_msgResponse;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.OSDRequestDispatcher;

/**
 *
 * @author bjko
 */
public class FleaseMessageOperation extends OSDOperation {

    public FleaseMessageOperation(OSDRequestDispatcher master) {
        super(master);
    }

    @Override
    public int getProcedureId() {
        return xtreemfs_rwr_flease_msgRequest.TAG;
    }

    @Override
    public void startRequest(OSDRequest rq) {
        xtreemfs_rwr_flease_msgRequest args = (xtreemfs_rwr_flease_msgRequest) rq.getRequestArgs();
        try {
            InetSocketAddress sender = new InetSocketAddress(args.getSenderHostname(), args.getSenderPort());
            master.getRWReplicationStage().receiveFleaseMessage(args.getFleaseMessage(),sender);
            rq.sendSuccess(new xtreemfs_rwr_flease_msgResponse());
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_WARN, this,ex);
        }
        
    }

    @Override
    public void startInternalEvent(Object[] args) {
        throw new UnsupportedOperationException("not an internal event!");
    }

    @Override
    public boolean requiresCapability() {
        return false;
    }

    @Override
    public yidl.runtime.Object parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
        xtreemfs_rwr_flease_msgRequest rpcrq = new xtreemfs_rwr_flease_msgRequest();
        rpcrq.unmarshal(new XDRUnmarshaller(data));

        return rpcrq;
    }


}
