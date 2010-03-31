/*
 * Copyright (c) 2008-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
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
 * AUTHORS: Christian Lorenz (ZIB), Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.foundation.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 *
 * @author bjko
 */
abstract public class Config {

    protected final Properties props;

    public Config() {
        props = new Properties();
    }

    public Config(Properties prop) {
        this.props = new Properties(prop);
    }

    /** Creates a new instance of Config */
    public Config(String filename) throws IOException {
        props = new Properties();
        props.load(new FileInputStream(filename));
    }

    protected int readRequiredInt(String paramName) {
        String tmp = props.getProperty(paramName);
        if (tmp == null)
            throw new RuntimeException("property '" + paramName
                + "' is required but was not found");
        try {
            return Integer.parseInt(tmp.trim());
        } catch (NumberFormatException ex) {
            throw new RuntimeException("property '" + paramName
                + "' is an integer but '" + tmp + "' is not a valid number");
        }
    }

    protected String readRequiredString(String paramName) {
        String tmp = props.getProperty(paramName);
        if (tmp == null)
            throw new RuntimeException("property '" + paramName
                + "' is required but was not found");
        return tmp.trim();
    }

    protected InetSocketAddress readRequiredInetAddr(String hostParam,
        String portParam) {
        String host = readRequiredString(hostParam);
        int port = readRequiredInt(portParam);
        InetSocketAddress isa = new InetSocketAddress(host, port);
        return isa;
    }

    protected boolean readRequiredBoolean(String paramName) {
        String tmp = props.getProperty(paramName);
        if (tmp == null)
            throw new RuntimeException("property '" + paramName
                + "' is required but was not found");
        return Boolean.parseBoolean(tmp.trim());
    }

    protected boolean readOptionalBoolean(String paramName, boolean defaultValue) {
        String tmp = props.getProperty(paramName);
        if (tmp == null)
            return defaultValue;
        else
            return Boolean.parseBoolean(tmp.trim());
    }
    
    protected int readOptionalInt(String paramName, int defaultValue) {
        String tmp = props.getProperty(paramName);
        if (tmp == null)
            return defaultValue;
        else
            return Integer.parseInt(tmp.trim());
    }

    protected InetAddress readOptionalInetAddr(String paramName,
        InetAddress defaultValue) throws UnknownHostException {
        String tmp = props.getProperty(paramName);
        if (tmp == null)
            return defaultValue;
        else
            return InetAddress.getByName(tmp);
    }

    protected String readOptionalString(String paramName, String defaultValue) {
        return props.getProperty(paramName, defaultValue);
    }

    public Properties getProps() {
        return props;
    }

}
