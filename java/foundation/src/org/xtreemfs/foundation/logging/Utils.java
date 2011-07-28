/*
 * Copyright (c) 2008-2010 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.logging;

/**
 *
 * @author bjko
 */
public class Utils {

    public static final char LEVEL_INFO = 'I';
    public static final char LEVEL_DEBUG = 'D';
    public static final char LEVEL_WARN = 'W';
    public static final char LEVEL_ERROR = 'E';


    public static void logMessage(char level, Object me, String msg) {
        if (me == null) {
            System.out.println(String.format("[ %c | %-20s | %3d ] %s",
                            level,"?",Thread.currentThread().getId(),
                            msg));
        } else {
            System.out.println(String.format("[ %c | %-20s | %3d ] %s",
                            level,me.getClass().getSimpleName(),Thread.currentThread().getId(),
                            msg));
        }
    }

    /** Creates a new instance of Utils */
    public Utils() {
    }

}
