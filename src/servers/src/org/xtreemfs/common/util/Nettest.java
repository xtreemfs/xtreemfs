/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.common.util;

import org.xtreemfs.foundation.ErrNo;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.DIRInterface.ProtocolException;
import org.xtreemfs.interfaces.NettestInterface.nopRequest;
import org.xtreemfs.interfaces.NettestInterface.nopResponse;
import org.xtreemfs.interfaces.NettestInterface.recv_bufferRequest;
import org.xtreemfs.interfaces.NettestInterface.recv_bufferResponse;
import org.xtreemfs.interfaces.NettestInterface.send_bufferRequest;
import org.xtreemfs.interfaces.NettestInterface.send_bufferResponse;
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
                case send_bufferRequest.TAG: {
                    send_bufferRequest pRq = new send_bufferRequest();
                    pRq.unmarshal(new XDRUnmarshaller(rq.getRequestFragment()));
                    send_bufferResponse pResp = new send_bufferResponse();
                    BufferPool.free(pRq.getData());
                    rq.sendResponse(pResp);
                    break;
                }
                case recv_bufferRequest.TAG: {
                    recv_bufferRequest pRq = new recv_bufferRequest();
                    pRq.unmarshal(new XDRUnmarshaller(rq.getRequestFragment()));
                    if (pRq.getSize() > 1024*1024*2) {
                        rq.sendException(new ProtocolException(ONCRPCResponseHeader.ACCEPT_STAT_GARBAGE_ARGS,
                            ErrNo.EINVAL, "max buffer size is 2MB"));
                        return;
                    }
                    ReusableBuffer data = BufferPool.allocate(pRq.getSize());
                    data.position(0);
                    data.limit(data.capacity());
                    recv_bufferResponse pResp = new recv_bufferResponse(data);
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
