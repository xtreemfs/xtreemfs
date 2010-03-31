/*
 * Copyright (c) 2009-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
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

package org.xtreemfs.foundation.util;

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
