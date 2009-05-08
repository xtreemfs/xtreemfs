/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
import org.xtreemfs.interfaces.utils.ONCRPCException;
import org.xtreemfs.interfaces.utils.Serializable;

/**
 * 
 * @author bjko
 */
public class DIRRequest {
    
    private final ONCRPCRequest rpcRequest;
    
    private Serializable        requestMessage;
    
    public DIRRequest(ONCRPCRequest rpcRequest) {
        this.rpcRequest = rpcRequest;
    }
    
    public void deserializeMessage(Serializable message) {
        final ReusableBuffer payload = rpcRequest.getRequestFragment();
        message.deserialize(payload);
        requestMessage = message;
    }
    
    public Serializable getRequestMessage() {
        return requestMessage;
    }
    
    public void sendSuccess(Serializable response) {
        rpcRequest.sendResponse(response);
    }
    
    public void sendInternalServerError(Throwable rootCause) {
        rpcRequest.sendInternalServerError(rootCause);
    }
    
    public void sendException(ONCRPCException exception) {
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.net, this, "sending exception return value");
            Logging.logUserError(Logging.LEVEL_DEBUG, Category.net, this, exception);
        }
        rpcRequest.sendGenericException(exception);
    }
    
}
