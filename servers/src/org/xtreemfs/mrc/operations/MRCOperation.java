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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.operations;

import org.xtreemfs.interfaces.Context;
import org.xtreemfs.interfaces.MRCInterface.MRCInterface;
import org.xtreemfs.interfaces.utils.Request;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.ErrorRecord.ErrorClass;

/**
 * 
 * @author bjko
 */
public abstract class MRCOperation {
    
    protected final MRCRequestDispatcher master;
    
    public MRCOperation(MRCRequestDispatcher master) {
        this.master = master;
    }
    
    /**
     * called after request was parsed and operation assigned.
     * 
     * @param rq
     *            the new request
     */
    public abstract void startRequest(MRCRequest rq);
    
    /**
     * Parses the request arguments.
     * 
     * @param rq
     *            the request
     * 
     * @return null if successful, error message otherwise
     */
    public ErrorRecord parseRequestArgs(MRCRequest rq) {
        try {
            Request req = MRCInterface.createRequest(rq.getRPCRequest().getRequestHeader());
            req.deserialize(rq.getRPCRequest().getRequestFragment());
            rq.setRequestArgs(req);
            return null;
            
        } catch (Throwable exc) {
            return new ErrorRecord(ErrorClass.INVALID_ARGS, exc.getMessage(), exc);
        }
    }
    
    /**
     * Returns the context associated with a request. If the request is not
     * bound to a context, <code>null</code> is returned.
     * 
     * @param rq
     *            the MRC request
     * @return the context, or <code>null</code>, if not available
     */
    public abstract Context getContext(MRCRequest rq);
    
    /**
     * Completes a request. This method should be used if no error has occurred.
     * 
     * @param rq
     */
    protected void finishRequest(MRCRequest rq) {
        master.requestFinished(rq);
    }
    
    /**
     * Completes a request. This method should be used if an error has occurred.
     * 
     * @param rq
     * @param error
     */
    protected void finishRequest(MRCRequest rq, ErrorRecord error) {
        rq.setError(error);
        master.requestFinished(rq);
    }
    
}
