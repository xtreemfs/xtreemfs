/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
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
