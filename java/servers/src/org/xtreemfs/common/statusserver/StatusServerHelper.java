/**
 * Copyright (c) 2013 Johannes Dillmann, Zuse Institute Berlin 
 *
 * Licensed under the BSD License, see LICENSE file for details.
 * 
 */
package org.xtreemfs.common.statusserver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.util.OutputUtils;

import com.sun.net.httpserver.HttpExchange;

public class StatusServerHelper {

    /**
     * Finds the resource with the given name and reads it to a StringBuffer
     * 
     * @see Class#getResourceAsStream()
     * 
     * @param name
     *            Name of the resource
     * @return StringBuffer with the contents of the template or null on errors
     */
    public static StringBuffer readTemplate(String name) {
        StringBuffer sb = null;
        try {
            InputStream is = StatusServerHelper.class.getClassLoader().getResourceAsStream(name);
            if (is == null) {
                throw new FileNotFoundException();
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
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, (Object) null,
                    "could not load page template '%s': %s", name, OutputUtils.stackTraceToString(ex));
        }

        return sb;
    }

    public static void sendFile(String name, HttpExchange httpExchange) throws IOException {
        try {
            InputStream htmlFile = StatusServerHelper.class.getClassLoader().getResourceAsStream(name);
            if (htmlFile == null) {
                throw new FileNotFoundException();
            }

            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            httpExchange.sendResponseHeaders(200, 0);

            byte[] buffer = new byte[4096];
            int len = 0;
            while ((len = htmlFile.read(buffer)) >= 0) {
                httpExchange.getResponseBody().write(buffer, 0, len);
            }

        } catch (Exception e) {
            byte[] msg = ("Sorry, could not read the requested file " + name + " (" + e.toString() + ")")
                    .getBytes("ascii");

            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            httpExchange.sendResponseHeaders(404, msg.length);            
            httpExchange.getResponseBody().write(msg);

            System.out.println("Ex: " + e);
        }
        finally {
            httpExchange.close();
        }
    }

}
