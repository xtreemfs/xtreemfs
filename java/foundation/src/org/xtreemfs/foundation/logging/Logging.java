/*
 * Copyright (c) 2008-2010 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.logging;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 
 * @author bjko
 */
public class Logging {
    
    public enum Category {
        /**
         * enable logging for all categories (no real category)
         */
        all,
        /**
         * logs messages pertaining to buffers
         */
        buffer,
        /**
         * log messaages pertaining to service lifecycles (threads)
         */
        lifecycle,
        /**
         * network-related log messages
         */
        net,
        /**
         * authorization-related log messages
         */
        auth,
        /**
         * log messages pertaining to the request flow through the stages
         */
        stage,
        /**
         * log messages pertaining to any kind of request processing
         */
        proc,
        /**
         * 
         */
        misc,
        /**
         * log messages pertaining storage on OSD or database access on MRC/DIR
         */
        storage,
        /**
         * logs messages pertaining to replication 
         */
        replication,
        /**
         * logs messages from additional tools
         */
        tool,
        /**
         * logs messages from tests
         */
        test
    }
    
    protected static final char      ABBREV_LEVEL_INFO  = 'I';
    
    protected static final char      ABBREV_LEVEL_DEBUG = 'D';
    
    protected static final char      ABBREV_LEVEL_WARN  = 'W';
    
    protected static final char      ABBREV_LEVEL_ERROR = 'E';
    
    protected static final char      ABBREV_LEVEL_TRACE = 'T';
    
    public static final int          LEVEL_EMERG        = 0;
    
    public static final int          LEVEL_ALERT        = 1;
    
    public static final int          LEVEL_CRIT         = 2;
    
    public static final int          LEVEL_ERROR        = 3;
    
    public static final int          LEVEL_WARN         = 4;
    
    public static final int          LEVEL_NOTICE       = 5;
    
    public static final int          LEVEL_INFO         = 6;
    
    public static final int          LEVEL_DEBUG        = 7;
    
    public static final String       FORMAT_PATTERN     = "[ %c | %-20s | %-15s | %3d | %15s ] %s";
    
    private static PrintStream out                = System.out;
    
    protected static Logging         instance;
    
    protected static boolean         tracingEnabled     = false;
    
    private final int                level;
    
    private final int                catMask;
    
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss");
    
    /**
     * Creates a new instance of Logging
     */
    private Logging(int level, int catMask) {
        
        if (level < 0)
            this.level = 0;
        else
            this.level = level;
        
        this.catMask = catMask;
        
        instance = this;
        
        System.currentTimeMillis();
    }

    public static void redirect(PrintStream out) {
        Logging.out = out;
    }
    
    public static String truncateString(String string, int maxLength) {
        return (string.length() > maxLength) ? 
                (string.substring(0, maxLength - 3) + "...") : string;
    }
    
    public static void logMessage(int level, Category cat, Object me, String formatPattern, Object... args) {
        checkIfInitializedOrThrow();

        // if the level is appropriate as well as the category, or the category
        // is 'all', log the message
        if (level <= instance.level && (cat == Category.all || (2 << cat.ordinal() & instance.catMask) > 0)) {
            
            char levelName = getLevelName(level);
                 
            out.println(String.format(FORMAT_PATTERN, levelName,
                    me == null ? "-" : truncateString(me instanceof Class ? ((Class) me).getSimpleName(): me.getClass().getSimpleName(), 20),
                    truncateString(Thread.currentThread().getName(), 15),
                    Thread.currentThread().getId(),
                    getTimeStamp(), 
                    String.format(formatPattern, args)));
        }
    }

    private static void checkIfInitializedOrThrow() {
        if (instance == null) {
            throw new RuntimeException(
                    "Cannot log message because the logging is not initialized yet. Did you forget to call Logging.start(...) in your code?");
        }
    }
    
    public static void logMessage(int level, Object me, String formatPattern, Object... args) {
        logMessage(level, Category.all, me, formatPattern, args);
    }
    
    public static void logError(int level, Object me, Throwable msg) {
        checkIfInitializedOrThrow();

        // if the level is appropriate, log the message
        if (level <= instance.level) {
            
            char levelName = getLevelName(level);

            out.println(String.format(FORMAT_PATTERN, levelName,
                    me == null ? "-" : (me instanceof Class ? ((Class) me).getSimpleName(): me.getClass().getSimpleName()),
                    Thread.currentThread().getName(), Thread.currentThread().getId(),
                getTimeStamp(), msg.toString()));
            for (StackTraceElement elem : msg.getStackTrace()) {
                out.println(" ...                                           " + elem.toString());
            }
            if (msg.getCause() != null) {
                out.println(String.format(FORMAT_PATTERN, levelName, me == null ? "-" : me.getClass()
                        .getSimpleName(), Thread.currentThread().getName(), Thread.currentThread().getId(),
                    getTimeStamp(), "root cause: " + msg.getCause()));
                for (StackTraceElement elem : msg.getCause().getStackTrace()) {
                    out.println(" ...                                           " + elem.toString());
                }
            }
        }
    }
    
    public static void logUserError(int level, Category cat, Object me, Throwable msg) {
        checkIfInitializedOrThrow();

        // if the level is appropriate as well as the category, or the category
        // is 'all', log the message
        if (level <= instance.level && (cat == Category.all || (2 << cat.ordinal() & instance.catMask) > 0)) {
            
            char levelName = getLevelName(level);
            
            out.println(String.format(FORMAT_PATTERN, levelName, me == null ? "-" : me.getClass()
                    .getSimpleName(), Thread.currentThread().getName(), Thread.currentThread().getId(),
                getTimeStamp(), msg.toString()));
            for (StackTraceElement elem : msg.getStackTrace()) {
                out.println(" ...                                           " + elem.toString());
            }
        }
    }

    public static char getLevelName(int level) {
        switch (level) {
        case LEVEL_EMERG:
        case LEVEL_ALERT:
        case LEVEL_CRIT:
        case LEVEL_ERROR:
            return ABBREV_LEVEL_ERROR;
        case LEVEL_WARN:
            return ABBREV_LEVEL_WARN;
        case LEVEL_NOTICE:
        case LEVEL_INFO:
            return ABBREV_LEVEL_INFO;
        case LEVEL_DEBUG:
            return ABBREV_LEVEL_DEBUG;
        default:
            return '?';
        }
    }
    
    public synchronized static void start(int level, Category... categories) {
        if (instance == null) {
            
            int catMask = 0;
            for (Category cat : categories) {
                
                if (cat == Category.all)
                    catMask = -1;
                
                catMask |= 2 << cat.ordinal();
            }
            
            if(categories.length == 0)
                catMask = -1;
            
            instance = new Logging(level, catMask);
        }
    }
    
    /**
     * Returns the current logging level if logging has been started or a negative value (-1) otherwise.
     * 
     * @return logging level or -1
     */
    public static int getLevel() {
        if (instance == null)
            return -1;
        else
            return instance.level;
    }

    public static boolean isDebug() {
        if (instance == null)
            return false;
        else
            return instance.level >= LEVEL_DEBUG;
    }
    
    public static boolean isInfo() {
        if (instance == null)
            return false;
        else
            return instance.level >= LEVEL_INFO;
    }
    
    public static boolean isNotice() {
        if (instance == null)
            return false;
        else
            return instance.level >= LEVEL_NOTICE;
    }
    
    private static String getTimeStamp() {
        return dateFormat.format(new Date());
    }
    
}
