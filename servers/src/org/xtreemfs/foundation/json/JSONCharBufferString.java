/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin and
    Barcelona Supercomputing Center - Centro Nacional de Supercomputacion.

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
 * AUTHORS: BjÃ¶rn Kolbeck (ZIB), Jan Stender (ZIB), JesÃºs Malo (BSC)
 */

package org.xtreemfs.foundation.json;

import java.nio.BufferUnderflowException;
import java.nio.CharBuffer;

/**
 *
 * @author bjko
 */
public class JSONCharBufferString implements JSONInput {
    
    CharBuffer cb;
    
    /** Creates a new instance of JSONCharBufferString */
    public JSONCharBufferString(CharBuffer cb) {
        assert (cb != null);
        
        this.cb = cb;
        this.cb.position(0);
    }

    public char read() throws JSONException {
        try {
            return cb.get();
        } catch(BufferUnderflowException ex) {
            throw new JSONException("Reached end of buffer");
        }   
    }

    public int skip(int skip) {
        try {
           
            cb.position(cb.position()+skip);
           
            return skip;
            
        } catch (IllegalArgumentException e) {
            return 0;
        }
       
    }
    
    public String toString() {
        return "JSONCharBufferString backed by "+cb.toString();
    }

    public boolean hasMore() {
        return cb.hasRemaining();
    }
    
}
