/*
 * Copyright (c) 2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: Bjoern Kolbeck (ZIB)
 */

package org.xtreemfs.foundation.trace;

import java.io.FileOutputStream;
import java.io.IOException;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;

/**
 * 
 * @author bjko
 */
public class Tracer {
    
    /**
     * Set this to true to enable trace log file for all requests.
     * 
     * @attention: MUST BE SET TO FALSE FOR NORMAL OPERATIONS.
     */
    public static final boolean COLLECT_TRACES = false;
    
    public enum TraceEvent {
        
        RECEIVED('>'), RESPONSE_SENT('<'), ERROR_SENT('E');
        
        private final char eventType;
        
        TraceEvent(char eventType) {
            this.eventType = eventType;
        }
        
        public char getEventType() {
            return this.eventType;
        }
    };
    
    private static Tracer          theInstance;
        
    private final FileOutputStream fos;
    
    private Tracer(String traceFileName) throws IOException {
        theInstance = this;
        
        fos = new FileOutputStream(traceFileName, true);
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.misc, this,
                "TRACING IS ENABLED, THIS WILL CAUSE PERFORMANCE TO BE REDUCED!");
        fos.write("#requestId;internal rq sequence no;event;component;message\n".getBytes());
    }
    
    /**
     * Initialize the tracer.
     * 
     * @param traceFileName
     *            file name to write trace data to (append mode).
     * @throws java.io.IOException
     *             if the file cannot be opened
     */
    public static void initialize(String traceFileName) throws IOException {
        new Tracer(traceFileName);
    }
    
    private void writeTraceRecord(String requestId, long intRqSeqNo, TraceEvent event, String component,
        String message) {
        StringBuffer sb = new StringBuffer();
        
        if (requestId != null)
            sb.append(requestId);
        
        sb.append(';');
        sb.append(intRqSeqNo);
        sb.append(';');
        sb.append(event.getEventType());
        sb.append(';');
        if (component != null)
            sb.append(component);
        sb.append(';');
        if (message != null)
            sb.append(message);
        sb.append("\n");
        try {
            fos.write(sb.toString().getBytes());
        } catch (IOException ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }
    
    public static void trace(String requestId, long intRqSeqNo, TraceEvent event, String component,
        String message) {
        assert (theInstance != null) : "Tracer not initialized";
        theInstance.writeTraceRecord(requestId, intRqSeqNo, event, component, message);
    }
    
    @Override
    public void finalize() {
        try {
            fos.close();
        } catch (IOException ex) {
        }
    }
    
}
