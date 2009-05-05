/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.foundation;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author bjko
 */
public class CrashReporter {

    public static void reportXtreemFSCrash(String service, String version, Throwable cause) {
        try {
            URL u = new URL("http://www.xtreemfs.org/dump/dump.php?srv=server");
            HttpURLConnection con = (HttpURLConnection) u.openConnection();
            con.setRequestMethod("PUT");
            con.setDoOutput(true);
            con.connect();
            OutputStream os = con.getOutputStream();
            String report = service+"\n"+version+"\n"+cause+"\n";
            for (StackTraceElement elem : cause.getStackTrace()) {
                report += elem.toString()+"\n";
            }
            if (cause.getCause() != null) {
               report += "\nroot cause: "+cause.getCause()+"\n";
                for (StackTraceElement elem : cause.getCause().getStackTrace()) {
                    report += elem.toString()+"\n";
                }
            }
            os.write(report.getBytes());
            os.flush();
            os.close();

            InputStream is = con.getInputStream();
            is.available();
            is.close();
        } catch (Throwable th) {
            System.out.println("cannot send crash report: "+th);
        }
    }

}
