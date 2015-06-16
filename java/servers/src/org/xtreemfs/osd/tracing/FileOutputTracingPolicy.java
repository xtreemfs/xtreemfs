/*
 * Copyright (c) 2008-2015 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.tracing;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.osd.OSDRequest;

import java.io.*;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class FileOutputTracingPolicy implements TracingPolicy {

    public static final short   POLICY_ID = 6001;

    @Override
    public void traceRequest(OSDRequest req, TraceInfo traceInfo) {
        final String policyConfig = traceInfo.getPolicyConfig();
        if(policyConfig!= null && !policyConfig.equals("")) {
            File f = new File(policyConfig);
            try {
                FileWriter fileWriter = new FileWriter(f, true);
                BufferedWriter writer = new BufferedWriter(fileWriter);
                writer.write(traceInfo.toString() + "\n");
                writer.close();
                fileWriter.close();
            } catch (IOException ex) {
                Logging.logError(Logging.LEVEL_ERROR, this, ex);
            }
        } else {
            Logging.logMessage(Logging.LEVEL_ERROR, this, "No output file");
        }
        Logging.logMessage(Logging.LEVEL_DEBUG, this, "Incoming request");
    }
}
