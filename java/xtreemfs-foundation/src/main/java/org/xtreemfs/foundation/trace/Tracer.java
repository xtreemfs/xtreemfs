/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
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
