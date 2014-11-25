/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.util;

import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author bjko
 */
public class PBRPCServiceURL {

    static {

        urlPattern = Pattern.compile("((pbrpc[gs]?):\\/\\/)?([^:]+)(:([0-9]+))?/?");
    }

    private static final Pattern urlPattern;

    private final String protocol;

    private final String host;

    private final int    port;

    public PBRPCServiceURL(String url, String defaultProtocol, int defaultPort) throws MalformedURLException {

        //parse URL
        Matcher m = urlPattern.matcher(url);
        if (m.matches()) {

            if (m.group(2) != null)
                protocol = m.group(2);
            else
                protocol = defaultProtocol;

            host = m.group(3);

            if (m.group(4) != null)
                port = Integer.valueOf(m.group(4).substring(1));
            else
                port = defaultPort;

        } else
            throw new MalformedURLException("'"+url+"' is not a valid XtreemFS service URL");

    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    public String toString() {
        return protocol+"://"+host+":"+port;
    }

}
