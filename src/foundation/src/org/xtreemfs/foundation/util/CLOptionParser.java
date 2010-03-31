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

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author bjko
 */
public class CLOptionParser {

    private final String programName;

    private final List<CLOption> options;

    private final List<String>   arguments;


    public CLOptionParser(String programName) {
        this.programName = programName;
        options = new LinkedList<CLOption>();
        arguments = new LinkedList<String>();
    }

    public CLOption addOption(CLOption option) {
        CLOption rv = option;
        for (CLOption tmp : options) {
            if (( (tmp.getName(true) == null) || tmp.getName(true).equals(option.getName(true)) ) &&
                ( (tmp.getName(false) == null) || tmp.getName(false).equals(option.getName(false))) ) {
                rv = tmp;
                break;
            }
        }
        if (rv == option)
            options.add(option);
        return rv;
    }

    public void printUsage(String arguments) {
        System.out.println(programName+" [options] "+arguments);
        printOptionHelp();
        System.out.println("");
    }

    public void printOptionHelp() {
        for (CLOption option : options) {
            System.out.println("\t"+option.getHelp());
        }
    }

    public void parse(String[] args) throws IllegalArgumentException {
        int position = 0;
        boolean nextIsValue = false;
        CLOption currentOption = null;
        while (position < args.length) {
            if (nextIsValue) {
                assert(currentOption != null);
                currentOption.parse(args[position]);
                currentOption = null;
                nextIsValue = false;
            } else {
                if (args[position].charAt(0) == '-') {
                    boolean useShortName;
                    String name;
                    String value = null;
                    if (args[position].charAt(1) == '-') {
                        useShortName = false;
                        name = args[position].substring(2);
                        if (name.length() == 0) {
                            throw new IllegalArgumentException("-- is not a valid option");
                        }
                        int posEq = name.indexOf("=");
                        if (posEq >= 0) {
                            value = name.substring(posEq+1);
                            name = name.substring(0, posEq);
                        }
                    } else {
                        useShortName = true;
                        name = args[position].substring(1);
                        if (name.length() == 0) {
                            throw new IllegalArgumentException("- is not a valid option");
                        }
                    }
                    boolean optFound = false;
                    for (CLOption option : options) {
                        final String optName = option.getName(useShortName);
                        if ((optName != null) && optName.equals(name)) {
                            if (option.requiresArgument()) {
                                if (value == null) {
                                    currentOption = option;
                                    nextIsValue = true;
                                } else {
                                    option.parse(value);
                                }
                            } else {
                                option.parse(null);
                            }
                            optFound = true;
                            break;
                        }
                    }
                    if (!optFound)
                        throw new IllegalArgumentException("'"+args[position]+"' is not a valid option");

                } else {
                    arguments.add(args[position]);
                }
            }
            position++;
        }
        if (nextIsValue) {
            throw new IllegalArgumentException("expected value for option '"+currentOption.getName()+"'");
        }
    }

    public List<String> getArguments() {
        return this.arguments;
    }



}
