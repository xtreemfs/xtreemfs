/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Björn Kolbeck(ZIB)
 */

package org.xtreemfs.common.util;

import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author bjko
 */
public class ONCRPCServiceURL {

    static {

        urlPattern = Pattern.compile("((oncrpc[gs]?):\\/\\/)?([^:]+)(:([0-9]+))?/?");
    }

    private static final Pattern urlPattern;

    private final String protocol;

    private final String host;

    private final int    port;

    public ONCRPCServiceURL(String url, String defaultProtocol, int defaultPort) throws MalformedURLException {

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
