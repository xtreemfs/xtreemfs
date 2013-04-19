/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.scheduler;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.util.OutputUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class SchedulerStatusPage {
    private final static String  statusPageTemplate;

    static {
        StringBuffer sb = null;
        try {
            InputStream is = SchedulerStatusPage.class.getClassLoader().getResourceAsStream(
                    "org/xtreemfs/scheduler/templates/status.html");
            if (is == null) {
                is = SchedulerStatusPage.class.getClass().getResourceAsStream("../templates/status.html");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            sb = new StringBuffer();
            String line = br.readLine();
            while (line != null) {
                sb.append(line + "\n");
                line = br.readLine();
            }
            br.close();
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_WARN, Logging.Category.misc, (Object) null,
                    "could not load status page template: %s", OutputUtils.stackTraceToString(ex));
        }
        if (sb == null) {
            statusPageTemplate = "<H1>Template was not found, unable to show status page!</h1>";
        } else {
            statusPageTemplate = sb.toString();
        }
    }

    public static String getStatusPage(SchedulerRequestDispatcher master) {
        StringBuilder dump = new StringBuilder();

        // TODO(ckleineweber): Fill status page

        return dump.toString();
    }
}
