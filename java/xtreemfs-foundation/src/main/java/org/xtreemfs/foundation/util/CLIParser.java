/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.util;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CLIParser {
    
    public static final class CliOption {
        public enum OPTIONTYPE {
            NUMBER, STRING, SWITCH, URL, FILE
        };
        
        public final OPTIONTYPE optType;
        
        public Boolean          switchValue;
        
        public String           stringValue;
        
        public Long             numValue;
        
        public PBRPCServiceURL  urlValue;
        
        public File             fileValue;
        
        public String           urlDefaultProtocol;
        
        public int              urlDefaultPort;
        
        public String           usageParams;
        
        public String           usageText;
        
        public CliOption(OPTIONTYPE oType) {
            this.optType = oType;
            if (optType == OPTIONTYPE.SWITCH)
                switchValue = new Boolean(false);
        }
        
        public CliOption(OPTIONTYPE oType, String usageText, String usageParams) {
            this(oType);
            this.usageText = usageText;
            this.usageParams = usageParams == null? "": usageParams;
        }
    }
    
    public static void parseCLI(String[] args, Map<String, CliOption> options, List<String> arguments)
        throws IllegalArgumentException {
        List<String> argList = Arrays.asList(args);
        
        Iterator<String> iter = argList.iterator();
        while (iter.hasNext()) {
            final String arg = iter.next().trim();
            if (arg.startsWith("-")) {
                // option
                final String optName = arg.substring(1);
                final CliOption option = options.get(optName);
                if (option == null) {
                    throw new IllegalArgumentException(arg + " is not a valid option");
                }
                switch (option.optType) {
                case SWITCH: {
                    option.switchValue = true;
                    break;
                }
                case STRING: {
                    if (iter.hasNext()) {
                        final String value = iter.next();
                        option.stringValue = value.trim();
                    } else {
                        throw new IllegalArgumentException(arg + " requires a string argument");
                    }
                    break;
                }
                case NUMBER: {
                    if (iter.hasNext()) {
                        final String value = iter.next();
                        try {
                            option.numValue = Long.valueOf(value.trim());
                        } catch (NumberFormatException ex) {
                            throw new IllegalArgumentException(arg + " requires a integer argument and "
                                + value + " is not an integer");
                        }
                    } else {
                        throw new IllegalArgumentException(arg + " requires a string argument");
                    }
                    break;
                }
                case URL: {
                    if (iter.hasNext()) {
                        final String value = iter.next();
                        try {
                            final PBRPCServiceURL tmp = new PBRPCServiceURL(value,
                                option.urlDefaultProtocol, option.urlDefaultPort);
                            option.urlValue = tmp;
                        } catch (Exception ex) {
                            throw new IllegalArgumentException(ex);
                        }
                    } else {
                        throw new IllegalArgumentException(arg + " requires a string argument");
                    }
                    break;
                }
                    
                case FILE: {
                    if (iter.hasNext()) {
                        final String value = iter.next();
                        try {
                            final File tmp = new File(value);
                            option.fileValue = tmp;
                        } catch (Exception ex) {
                            throw new IllegalArgumentException(arg + " requires <protocol>://<host>:<port>");
                        }
                    } else {
                        throw new IllegalArgumentException(arg + " requires a string argument");
                    }
                    break;
                }
                    
                }
            } else {
                arguments.add(arg);
            }
        }
    }
    
}
