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

/**
 * Provides useful helper methods, that can be used in StatusServerModules.
 */
public class StatusServerHelper {

    /**
     * Finds the resource with the given name and reads it to a StringBuffer.
     * 
     * @see Class#getResourceAsStream()
     * 
     * @param name
     *            Name of the resource
     * @return StringBuffer with the contents of the template or null on errors
     */
    public static StringBuffer readTemplate(String name) {
        StringBuffer sb = null;
        BufferedReader br = null;

        try {
            InputStream is = StatusServerHelper.class.getClassLoader().getResourceAsStream(name);
            if (is == null) {
                throw new FileNotFoundException();
            }

            br = new BufferedReader(new InputStreamReader(is));
            sb = new StringBuffer();

            String line = br.readLine();
            while (line != null) {
                sb.append(line + "\n");
                line = br.readLine();
            }
        } catch (IOException ex) {
            sb = null;
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, (Object) null,
                    "could not load page template '%s': %s", name, OutputUtils.stackTraceToString(ex));
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) { }
            }
        }

        return sb;
    }

    /**
     * Finds the resource with the given name and sends it as a response to a HTTP request.
     * 
     * @see Class#getResourceAsStream()
     * 
     * @param name
     *            Name of the resource
     * @param httpExchange
     *            HTTP request the file will be delivered to
     * @throws IOException
     */
    public static void sendFile(String name, HttpExchange httpExchange) throws IOException {
        InputStream htmlFile = null;
        try {
            htmlFile = StatusServerHelper.class.getClassLoader().getResourceAsStream(name);
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

        } catch (IOException e) {
            byte[] msg = ("Sorry, could not read the requested file " + name + " (" + e.toString() + ")")
                    .getBytes("ascii");

            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            httpExchange.sendResponseHeaders(404, msg.length);            
            httpExchange.getResponseBody().write(msg);

            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, (Object) null, "could not read page '%s': %s", name,
                    OutputUtils.stackTraceToString(e));
        } finally {
            httpExchange.close();
            if (htmlFile != null) {
                try {
                    htmlFile.close();
                } catch (IOException e) { }
            }
        }
    }

}
