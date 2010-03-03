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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.common.logging;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
        db, // TODO: rename to "storage"
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
    
    private long                     startTime;

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
        
        startTime = System.currentTimeMillis();
    }

    public static void redirect(PrintStream out) {
        Logging.out = out;
    }
    
    public static void logMessage(int level, Category cat, Object me, String formatPattern, Object... args) {
        
        // if the level is appropriate as well as the category, or the category
        // is 'all', log the message
        if (level <= instance.level && (cat == Category.all || (2 << cat.ordinal() & instance.catMask) > 0)) {
            
            char levelName = getLevelName(level);
            
            out.println(String.format(FORMAT_PATTERN, levelName, me == null ? "-" : me.getClass()
                    .getSimpleName(), Thread.currentThread().getName(), Thread.currentThread().getId(),
                getTimeStamp(), String.format(formatPattern, args)));
        }
    }
    
    public static void logMessage(int level, Object me, String formatPattern, Object... args) {
        logMessage(level, Category.all, me, formatPattern, args);
    }
    
    public static void logError(int level, Object me, Throwable msg) {
        
        // if the level is appropriate, log the message
        if (level <= instance.level) {
            
            char levelName = getLevelName(level);
            
            out.println(String.format(FORMAT_PATTERN, levelName, me == null ? "-" : me.getClass()
                    .getSimpleName(), Thread.currentThread().getName(), Thread.currentThread().getId(),
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
        /*long seconds = (System.currentTimeMillis() - instance.startTime) / 1000;
        long hours = seconds / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return hours + ":" + (mins < 10 ? "0" : "") + mins + ":" + (secs < 10 ? "0" : "") + secs;*/
        return dateFormat.format(new Date());
    }
    
}
