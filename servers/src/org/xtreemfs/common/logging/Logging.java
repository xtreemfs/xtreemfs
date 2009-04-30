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

/**
 * 
 * @author bjko
 */
public class Logging {
    
    protected static final char ABBREV_LEVEL_INFO  = 'I';
    
    protected static final char ABBREV_LEVEL_DEBUG = 'D';
    
    protected static final char ABBREV_LEVEL_WARN  = 'W';
    
    protected static final char ABBREV_LEVEL_ERROR = 'E';
    
    protected static final char ABBREV_LEVEL_TRACE = 'T';
    
    public static final int     LEVEL_EMERG        = 0;
    
    public static final int     LEVEL_ALERT        = 1;
    
    public static final int     LEVEL_CRIT         = 2;
    
    public static final int     LEVEL_ERROR        = 3;
    
    public static final int     LEVEL_WARN         = 4;
    
    public static final int     LEVEL_NOTICE       = 5;
    
    public static final int     LEVEL_INFO         = 6;
    
    public static final int     LEVEL_DEBUG        = 7;
    
    public static final String  FORMAT_PATTERN     = "[ %c | %-20s | %-15s | %3d | %9s] %s";
    
    protected static Logging    instance;
    
    protected static boolean    tracingEnabled     = false;
    
    private final int           level;
    
    private long                startTime;
    
    /**
     * Creates a new instance of Logging
     */
    private Logging(int level) {
        
        if (level < 0)
            this.level = 0;
        else
            this.level = level;
        
        instance = this;
        
        startTime = System.currentTimeMillis();
    }
    
    public static void logMessage(int level, Object me, String msg) {
        if (level <= instance.level) {
            char levelName = getLevelName(level);
            if (me == null) {
                System.out.println(String.format(FORMAT_PATTERN, levelName, "-", Thread.currentThread()
                        .getName(), Thread.currentThread().getId(), getTimeStamp(), msg));
            } else {
                System.out.println(String.format(FORMAT_PATTERN, levelName, me.getClass().getSimpleName(),
                    Thread.currentThread().getName(), Thread.currentThread().getId(), getTimeStamp(), msg));
            }
        }
    }
    
    public static void logMessage(int level, Object me, Throwable msg) {
        if (level <= instance.level) {
            char levelName = getLevelName(level);
            if (me == null) {
                System.out.println(String.format(FORMAT_PATTERN, levelName, "-", Thread.currentThread()
                        .getName(), Thread.currentThread().getId(), getTimeStamp(), msg.toString()));
                for (StackTraceElement elem : msg.getStackTrace()) {
                    System.out.println(" ...                                           " + elem.toString());
                }
                if (msg.getCause() != null) {
                    System.out.println(String.format(FORMAT_PATTERN, levelName, "-", Thread.currentThread()
                            .getName(), Thread.currentThread().getId(), getTimeStamp(), "root cause: "
                        + msg.getCause()));
                    for (StackTraceElement elem : msg.getCause().getStackTrace()) {
                        System.out.println(" ...                                           "
                            + elem.toString());
                    }
                }
            } else {
                System.out.println(String.format(FORMAT_PATTERN, levelName, me.getClass().getSimpleName(),
                    Thread.currentThread().getName(), Thread.currentThread().getId(), getTimeStamp(), msg));
                for (StackTraceElement elem : msg.getStackTrace()) {
                    System.out.println(" ...                                           " + elem.toString());
                }
                if (msg.getCause() != null) {
                    System.out.println(String.format(FORMAT_PATTERN, levelName, me.getClass(), Thread
                            .currentThread().getName(), Thread.currentThread().getId(), getTimeStamp(),
                        "root cause: " + msg.getCause()));
                    for (StackTraceElement elem : msg.getCause().getStackTrace()) {
                        System.out.println(" ...                                           "
                            + elem.toString());
                    }
                }
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
    
    public synchronized static void start(int level) {
        if (instance == null) {
            instance = new Logging(level);
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
        long seconds = (System.currentTimeMillis() - instance.startTime) / 1000;
        long hours = seconds / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return hours + ":" + (mins < 10 ? "0" : "") + mins + ":" + (secs < 10 ? "0" : "") + secs;
    }
    
}
