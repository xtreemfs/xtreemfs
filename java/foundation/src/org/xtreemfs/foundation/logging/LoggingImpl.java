/*
 * Copyright (c) 2016 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.logging;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.logging.Logging.LoggingInterface;

public class LoggingImpl implements LoggingInterface {
    private static final String           FORMAT_PATTERN = "[ %c | %-6.6s | %-20s | %-15s | %3d | %15s ] %s";
    private static final SimpleDateFormat dateFormat     = new SimpleDateFormat("MMM dd HH:mm:ss");

    private PrintStream                   out            = System.out;

    protected boolean                     tracingEnabled = false;

    private int                           level;

    private int                           catMask;

    @Override
    public void start(int level, Category[] categories) {
        if (level < 0)
            this.level = 0;
        else
            this.level = level;



        int catMask = 0;
        for (Category cat : categories) {

            if (cat == Category.all)
                catMask = -1;

            catMask |= 2 << cat.ordinal();
        }

        if (categories.length == 0)
            catMask = -1;

        this.catMask = catMask;
    }

    @Override
    public void redirect(PrintStream out) {
        this.out = out;
    }

    public static String truncateString(String string, int maxLength) {
        return (string.length() > maxLength) ? (string.substring(0, maxLength - 3) + "...") : string;
    }

    @Override
    public void logMessage(int level, Category cat, Object me, String formatPattern, Object[] args) {

        // if the level is appropriate as well as the category, or the category is 'all', log the message
        if (level <= this.level && (cat == Category.all || (2 << cat.ordinal() & this.catMask) > 0)) {

            char levelName = Logging.getLevelName(level);

            out.println(String.format(FORMAT_PATTERN, levelName, cat, me == null ? "-"
                    : truncateString(me instanceof Class ? ((Class) me).getSimpleName() : me.getClass().getSimpleName(),
                            20),
                    truncateString(Thread.currentThread().getName(), 15), Thread.currentThread().getId(),
                    getTimeStamp(), String.format(formatPattern, args)));
        }
    }

    @Override
    public void logMessage(int level, Object me, String formatPattern, Object[] args) {
        logMessage(level, Category.all, me, formatPattern, args);
    }

    @Override
    public void logError(int level, Object me, Throwable msg) {
        // if the level is appropriate, log the message
        if (level <= this.level) {

            char levelName = Logging.getLevelName(level);

            out.println(String.format(FORMAT_PATTERN, levelName, Category.all,
                    me == null ? "-"
                            : (me instanceof Class ? ((Class) me).getSimpleName() : me.getClass().getSimpleName()),
                    Thread.currentThread().getName(), Thread.currentThread().getId(), getTimeStamp(), msg.toString()));
            for (StackTraceElement elem : msg.getStackTrace()) {
                out.println(" ...                                           " + elem.toString());
            }
            if (msg.getCause() != null) {
                out.println(String.format(FORMAT_PATTERN, levelName, Category.all,
                        me == null ? "-" : me.getClass().getSimpleName(), Thread.currentThread().getName(),
                        Thread.currentThread().getId(), getTimeStamp(), "root cause: " + msg.getCause()));
                for (StackTraceElement elem : msg.getCause().getStackTrace()) {
                    out.println(" ...                                           " + elem.toString());
                }
            }
        }
    }

    @Override
    public void logUserError(int level, Category cat, Object me, Throwable msg) {
        // if the level is appropriate as well as the category, or the category
        // is 'all', log the message
        if (level <= this.level && (cat == Category.all || (2 << cat.ordinal() & this.catMask) > 0)) {

            char levelName = Logging.getLevelName(level);

            out.println(String.format(FORMAT_PATTERN, levelName, cat, me == null ? "-" : me.getClass().getSimpleName(),
                    Thread.currentThread().getName(), Thread.currentThread().getId(), getTimeStamp(), msg.toString()));
            for (StackTraceElement elem : msg.getStackTrace()) {
                out.println(" ...                                           " + elem.toString());
            }
        }
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public boolean isDebug() {
        return level >= Logging.LEVEL_DEBUG;
    }

    @Override
    public boolean isInfo() {
        return level >= Logging.LEVEL_INFO;
    }

    @Override
    public boolean isNotice() {
        return level >= Logging.LEVEL_NOTICE;
    }

    String getTimeStamp() {
        return dateFormat.format(new Date());
    }
}
