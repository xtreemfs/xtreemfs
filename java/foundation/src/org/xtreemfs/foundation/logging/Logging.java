/*
 * Copyright (c) 2008-2010 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.logging;

import java.io.PrintStream;


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
        test,
        /**
         * log messages regarding flease
         */
        flease,
        /**
         * log messages regarding babudb
         */
        babudb,
        /**
         * log messages regarding erasure coding
         */
        ec
    }
    
    protected static final char           ABBREV_LEVEL_INFO  = 'I';

    protected static final char           ABBREV_LEVEL_DEBUG = 'D';

    protected static final char           ABBREV_LEVEL_WARN  = 'W';

    protected static final char           ABBREV_LEVEL_ERROR = 'E';

    protected static final char           ABBREV_LEVEL_TRACE = 'T';

    public static final int               LEVEL_EMERG        = 0;

    public static final int               LEVEL_ALERT        = 1;

    public static final int               LEVEL_CRIT         = 2;

    public static final int               LEVEL_ERROR        = 3;

    public static final int               LEVEL_WARN         = 4;

    public static final int               LEVEL_NOTICE       = 5;

    public static final int               LEVEL_INFO         = 6;

    public static final int               LEVEL_DEBUG        = 7;

    private static LoggingInterface       instance;
    
    static char getLevelName(int level) {
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

    private Logging() {
    }

    
    public synchronized static void start(int level, Category... categories) {
        if (instance == null) {


            String logImplClass = System.getProperty("org.xtreemfs.foundation.logging.LoggingImpl");
            if (logImplClass != null) {
                try {
                    Class<? extends LoggingInterface> clazz = Class.forName(logImplClass)
                            .asSubclass(LoggingInterface.class);
                    instance = clazz.newInstance();
                } catch (ReflectiveOperationException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                instance = new LoggingImpl();
            }

            instance.start(level, categories);
        }
    }


    private static void checkIfInitializedOrThrow() {
        if (instance == null) {
            throw new RuntimeException(
                    "Cannot log message because the logging is not initialized yet. Did you forget to call Logging.start(...) in your code?");
        }
    }
    
    public static void redirect(PrintStream out) {
        checkIfInitializedOrThrow();
        instance.redirect(out);
    }

    public static void logMessage(int level, Category cat, Object me, String formatPattern, Object... args) {
        checkIfInitializedOrThrow();
        instance.logMessage(level, cat, me, formatPattern, args);
    }

    public static void logMessage(int level, Object me, String formatPattern, Object... args) {
        checkIfInitializedOrThrow();
        instance.logMessage(level, Category.all, me, formatPattern, args);
    }
    
    public static void logError(int level, Object me, Throwable msg) {
        checkIfInitializedOrThrow();
        instance.logError(level, me, msg);
    }
    
    public static void logUserError(int level, Category cat, Object me, Throwable msg) {
        checkIfInitializedOrThrow();
        instance.logUserError(level, cat, me, msg);
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
            return instance.getLevel();
    }

    public static boolean isDebug() {
        if (instance == null)
            return false;
        else
            return instance.isDebug();
    }
    
    public static boolean isInfo() {
        if (instance == null)
            return false;
        else
            return instance.isInfo();
    }
    
    public static boolean isNotice() {
        if (instance == null)
            return false;
        else
            return instance.isNotice();
    }

    
    public static interface LoggingInterface {
        public void start(int level, Category[] categories);

        public void redirect(PrintStream out);

        public void logMessage(int level, Category cat, Object me, String formatPattern, Object[] args);

        public void logMessage(int level, Object me, String formatPattern, Object[] args);

        public void logError(int level, Object me, Throwable msg);

        public void logUserError(int level, Category cat, Object me, Throwable msg);

        public int getLevel();

        public boolean isDebug();

        public boolean isInfo();

        public boolean isNotice();
    }
}
