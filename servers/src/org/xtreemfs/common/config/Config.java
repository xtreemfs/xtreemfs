/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Christian Lorenz (ZIB), Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.common.config;

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
