/*
 * Copyright (c) 2010 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation;

import java.util.Map;

/**
 *
 * @author bjko
 */
public class CrashReporter {

    public static void reportXtreemFSCrash(String report) {
        /*try {
            URL u = new URL("http://www.xtreemfs.org/dump/dump.php?srv=server");
            HttpURLConnection con = (HttpURLConnection) u.openConnection();
            con.setRequestMethod("PUT");
            con.setDoOutput(true);
            con.connect();
            OutputStream os = con.getOutputStream();
            os.write(report.getBytes());
            os.flush();
            os.close();

            InputStream is = con.getInputStream();
            is.available();
            is.close();
        } catch (Throwable th) {
            System.out.println("cannot send crash report: "+th);
        }*/
    }

    public static String createCrashReport(String service, String version, Throwable cause) {
        try {
            StringBuilder report = new StringBuilder();
            report.append("----------------------------------------------------------------\n");
            report.append("We are sorry, but your "+service+" has crashed. To report this bug\n");
            report.append("please go to http://www.xtreemfs.org and file an issue and attach\n");
            report.append("this crash report.\n\n");
            report.append("service: ");
            report.append(service);
            report.append("    version: ");
            report.append(version);
            report.append("\n");
            report.append("JVM version: ");
            report.append(System.getProperty("java.version"));
            report.append(" ");
            report.append(System.getProperty("java.vendor"));
            report.append(" on ");
            report.append(System.getProperty("os.name"));
            report.append(" ");
            report.append(System.getProperty("os.version"));
            report.append("\n");
            report.append("exception: ");
            report.append(cause.toString());
            report.append("\n");
            for (StackTraceElement elem : cause.getStackTrace()) {
                report.append(elem.toString());
                report.append("\n");
            }
            if (cause.getCause() != null) {
                report.append("\nroot cause: ");
                report.append(cause.getCause());
                report.append("\n");
                for (StackTraceElement elem : cause.getCause().getStackTrace()) {
                    report.append(elem.toString());
                    report.append("\n");
                }
            }
            reportThreadStates(report);

            report.append("----------------------------------------------------------------\n");
            return report.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Could not write crash report for: "+service+","+version+","+cause+" due to "+ex;
        }
    }

    /** Logs the stack trace of each thread into {@code report}. */
    public static void reportThreadStates(StringBuilder report) {
        report.append("\n--- THREAD STATES ---\n");
        final Map<Thread,StackTraceElement[]> traces =  Thread.getAllStackTraces();
        for (Thread t : traces.keySet()) {
            report.append("thread: ");
            report.append(t.getName());
            report.append("\n");
            for (StackTraceElement e : traces.get(t)) {
                report.append(e.toString());
                report.append("\n");
            }
            report.append("\n");
        }
    }

}
