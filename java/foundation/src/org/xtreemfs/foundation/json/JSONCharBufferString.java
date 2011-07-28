/*
 * Copyright (c) 2008-2010 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
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
