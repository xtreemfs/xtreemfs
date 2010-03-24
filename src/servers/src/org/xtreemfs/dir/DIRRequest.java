/*  Copyright (c) 2009-2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.dir;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.foundation.oncrpc.server.ONCRPCRequest;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.DIRInterface.DIRException;
import org.xtreemfs.interfaces.DIRInterface.RedirectException;
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.interfaces.utils.ONCRPCResponseHeader;
import org.xtreemfs.mrc.RequestDetails;

/**
 * 
 * @author bjko
 */
public class DIRRequest {
    
    private final ONCRPCRequest rpcRequest;
    
    private yidl.runtime.Object        requestMessage;
    
    private RequestDetails      details;
    
    public DIRRequest(ONCRPCRequest rpcRequest) {
        this.rpcRequest = rpcRequest;
        details = new RequestDetails();
    }
    
    public void deserializeMessage(yidl.runtime.Object message) {
        final ReusableBuffer payload = rpcRequest.getRequestFragment();
        message.unmarshal(new XDRUnmarshaller(payload));
        requestMessage = message;
    }
    
    public yidl.runtime.Object getRequestMessage() {
        return requestMessage;
    }
    
    public void sendSuccess(yidl.runtime.Object response) {
        rpcRequest.sendResponse(response);
    }
    
    public void sendInternalServerError(Throwable rootCause) {
        rpcRequest.sendErrorCode(ONCRPCResponseHeader.ACCEPT_STAT_SYSTEM_ERR);
    }
    
    public void sendRedirectException(String addr, int port) {
        rpcRequest.sendException(new RedirectException(addr,port));
    }
    
    public void sendException(ONCRPCException exception) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "sending exception return value");
            Logging.logUserError(Logging.LEVEL_DEBUG, Category.net, this, exception);
        }
        rpcRequest.sendException(exception);
    }
    
    public void sendDIRException(int errno, String message) {
        DIRException ex = new DIRException(errno, message, "");
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.stage, this, "sending errno exception %s", ex
                    .toString());
        }
        getRPCRequest().sendException(ex);
    }
    
    public RequestDetails getDetails() {
        return details;
    }
    
    public void setDetails(RequestDetails details) {
        this.details = details;
    }

    public ONCRPCRequest getRPCRequest() {
        return rpcRequest;
    }
}
