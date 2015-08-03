/*
 * Copyright (c) 2008-2011 by Christian Lorenz, Bjoern Kolbeck,
 *               Jan Stender, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.config;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import com.google.protobuf.ByteString;
import org.xtreemfs.foundation.logging.Logging;

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

    /** Creates a new instance of {@link Config} */
    public Config(String filename) throws IOException {
        props = new Properties();
        InputStream configInputStream = new FileInputStream(filename);
        props.load(configInputStream);
        configInputStream.close();
    }

    /**
     * Writes out a properties-compatible file at the given location.
     * @param filename
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    protected void write(String filename) throws IOException {
        OutputStream fileOutputStream = new FileOutputStream(filename);
        props.store(fileOutputStream, "");
        fileOutputStream.close();
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
    
    protected InetSocketAddress readOptionalInetSocketAddr(String hostName,
            String portParam, InetSocketAddress defaultValue) {
            String host = readOptionalString(hostName, null);
            int port = readOptionalInt(portParam, -1);
            if (host==null || port==-1)
                return defaultValue;
            else
                return new InetSocketAddress(host,port);
    }

    protected String readOptionalString(String paramName, String defaultValue) {
        return props.getProperty(paramName, defaultValue);
    }
    
    protected int readOptionalDebugLevel() {
        String level = props.getProperty("debug.level");
        if (level == null)
            return Logging.LEVEL_WARN;
        else {
            
            level = level.trim().toUpperCase();
            
            if (level.equals("EMERG")) {
                return Logging.LEVEL_EMERG;
            } else if (level.equals("ALERT")) {
                return Logging.LEVEL_ALERT;
            } else if (level.equals("CRIT")) {
                return Logging.LEVEL_CRIT;
            } else if (level.equals("ERR")) {
                return Logging.LEVEL_ERROR;
            } else if (level.equals("WARNING")) {
                return Logging.LEVEL_WARN;
            } else if (level.equals("NOTICE")) {
                return Logging.LEVEL_NOTICE;
            } else if (level.equals("INFO")) {
                return Logging.LEVEL_INFO;
            } else if (level.equals("DEBUG")) {
                return Logging.LEVEL_DEBUG;
            } else {
                
                try {
                    int levelInt = Integer.valueOf(level);
                    return levelInt;
                } catch (NumberFormatException ex) {
                    throw new RuntimeException("'" + level + 
                            "' is not a valid level name nor an integer");
                }
            }
        }
    }

    public Properties getProps() {
        return props;
    }

}
