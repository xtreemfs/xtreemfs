/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.util;

import java.net.MalformedURLException;

/**
 *
 * @author bjko
 */
public abstract class CLOption {

    protected String shortName;
    protected String longName;
    protected String helpText;
    protected boolean set;

    public CLOption(String shortName, String longName, String helpText) {
        this.shortName = shortName;
        this.longName = longName;
        this.helpText = helpText;
        if ((shortName == null )&& (longName == null))
            throw new IllegalArgumentException("must specify either a shortName or a longName or both");
    }

    public String getName() {
        StringBuilder sb = new StringBuilder();
        if (shortName != null) {
            sb.append("-");
            sb.append(shortName);
            if (longName != null)
                sb.append("/");
            else
                sb.append(" ");
        }
        if (longName != null) {
            sb.append("--");
            sb.append(longName);
            sb.append("=");
        }
        return sb.toString();     
    }

    public String getName(boolean useShortName) {
        if (useShortName)
            return this.shortName;
        else
            return this.longName;
    }

    public abstract String getHelp();

    /**
     * called only when the option is present
     * @param value
     * @throws IllegalArgumentException
     */
    public void parse(String value) throws IllegalArgumentException {
        set = true;
    }

    public boolean isSet() {
        return set;
    }

    public abstract boolean requiresArgument();

    @Override
    public String toString() {
        return getHelp();
    }

    public static class Switch extends CLOption {

        protected boolean value;

        public Switch(String shortName, String longName, String helpText) {
            super(shortName,longName,helpText);
        }

        @Override
        public String getHelp() {
            return getName()+helpText;
        }

        @Override
        public void parse(String value) throws IllegalArgumentException {
            super.parse(value);
            this.value = true;
        }

        @Override
        public boolean requiresArgument() {
            return false;
        }

        public boolean getValue() {
            return value;
        }
    }

    public static class StringValue extends CLOption {

        protected String value;

        public StringValue(String shortName, String longName, String helpText) {
            super(shortName,longName,helpText);
        }

        @Override
        public String getHelp() {
            return getName()+"<string> "+helpText;
        }

        @Override
        public void parse(String value) throws IllegalArgumentException {
            super.parse(value);
            this.value = value;
        }

        @Override
        public boolean requiresArgument() {
            return true;
        }

        public String getValue() {
            return value;
        }

    }

    public static class IntegerValue extends CLOption {

        protected int value;

        public IntegerValue(String shortName, String longName, String helpText) {
            super(shortName,longName,helpText);
        }

        @Override
        public String getHelp() {
            return getName()+"<number> "+helpText;
        }

        @Override
        public void parse(String value) throws IllegalArgumentException {
            super.parse(value);
            try {
                this.value = Integer.valueOf(value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("'"+value+"' is not a valid number");
            }
        }

        @Override
        public boolean requiresArgument() {
            return true;
        }

        public int getValue() {
            return value;
        }
    }

    public static class URLValue extends CLOption {

        protected PBRPCServiceURL value;

        protected final String defaultSchema;

        protected final int    defaultPort;

        public URLValue(String shortName, String longName, String helpText,
                String defaultSchema, int defaultPort) {
            super(shortName,longName,helpText);
            this.defaultSchema = defaultSchema;
            this.defaultPort = defaultPort;
        }

        @Override
        public String getHelp() {
            return getName()+"[<schema>://]<hostname>[:<port>] "+helpText+
                    " (default schema is "+defaultSchema+", default port is "+defaultPort+")";
        }

        @Override
        public void parse(String value) throws IllegalArgumentException {
            super.parse(value);
            try {
                this.value = new PBRPCServiceURL(value, defaultSchema, defaultPort);
            } catch (MalformedURLException ex) {
                throw new IllegalArgumentException("'"+value+"' is not a valid URL ("+ex.getMessage()+")");
            }
        }

        @Override
        public boolean requiresArgument() {
            return true;
        }

        public PBRPCServiceURL getValue() {
            return value;
        }
    }

}
