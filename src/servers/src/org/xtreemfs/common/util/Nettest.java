/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.common.util;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.DIRInterface.ProtocolException;
import org.xtreemfs.interfaces.NettestInterface.nopRequest;
import org.xtreemfs.interfaces.NettestInterface.nopResponse;
import org.xtreemfs.interfaces.NettestInterface.pingRequest;
import org.xtreemfs.interfaces.NettestInterface.pingResponse;
import org.xtreemfs.interfaces.utils.ONCRPCRequestHeader;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.interfaces.utils.XDRUtils;

/**
 *
 * @author bjko
 */
public class Nettest {

    public static void handleNettest(ONCRPCRequestHeader header, ONCRPCRequest rq) {
        try {
            if (header.getMessageType() != XDRUtils.TYPE_CALL) {
                rq.sendException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_GARBAGE_ARGS,
                        ErrNo.EINVAL, "message type must be CALL"));
                return;
            }
            switch (header.getProcedure()) {
                case pingRequest.TAG: {
                    pingRequest pRq = new pingRequest();
                    pRq.unmarshal(new XDRUnmarshaller(rq.getRequestFragment()));
                    ReusableBuffer data = pRq.getData();
                    data.position(0);
                    pingResponse pResp = new pingResponse(data);
                    rq.sendResponse(pResp);
                    break;
                }
                case nopRequest.TAG: {
                    nopResponse response = new nopResponse();
                    rq.sendResponse(response);
                    break;
                }
                default: {
                    rq.sendException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_PROC_UNAVAIL,
                            ErrNo.EINVAL, "requested operation is not available on this DIR"));
                    return;
                }
            }
        } catch (Exception ex) {
            try {
                rq.sendException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_SYSTEM_ERR,
                            ErrNo.EINVAL, "internal server error:"+ex));
            } catch (Throwable th) {
                //ignore
            }
        }
    }

}
