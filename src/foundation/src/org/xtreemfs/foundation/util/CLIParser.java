/*
 * Copyright (c) 2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
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

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.util.ONCRPCServiceURL;

public class CLIParser {
    
    public static final class CliOption {
        public enum OPTIONTYPE {
            NUMBER, STRING, SWITCH, URL, FILE
        };
        
        public final OPTIONTYPE optType;
        
        public Boolean          switchValue;
        
        public String           stringValue;
        
        public Long             numValue;
        
        public ONCRPCServiceURL urlValue;
        
        public File             fileValue;

        public String           urlDefaultProtocol;
        
        public int              urlDefaultPort;

        public CliOption(OPTIONTYPE oType) {
            this.optType = oType;
            if (optType == OPTIONTYPE.SWITCH)
                switchValue = new Boolean(false);
        }
    }
    
    public static void parseCLI(String[] args, Map<String, CliOption> options,
        List<String> arguments) throws IllegalArgumentException {
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
                            throw new IllegalArgumentException(arg
                                + " requires a integer argument and " + value
                                + " is not an integer");
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
                            final ONCRPCServiceURL tmp = new ONCRPCServiceURL(value,option.urlDefaultProtocol,option.urlDefaultPort);
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
