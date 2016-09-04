/*
 * Copyright (c) 2016 by Johannes Dillmann,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.logging;

import java.io.PrintStream;
import java.net.URI;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.logging.Logging.LoggingInterface;

public class Log4jImpl implements LoggingInterface {
    static final boolean  PERCLASS = true;

    static final Level[] LEVELS = {
            Level.forName("EMERG", 110),
            Level.forName("ALERT", 111),
            Level.forName("CRIT", 112),
            Level.getLevel("ERROR"), // Attention: These two are interchanged in xtfs/log4j
            Level.getLevel("WARN"),  // Attention: These two are interchanged in xtfs/log4j
            Level.forName("NOTICE", 350),
            Level.getLevel("INFO"),
            Level.getLevel("DEBUG")
    };
    
	static final Marker[] CATEGORIES;
	static {
		Category[] internalCats = Category.values();
		CATEGORIES = new Marker[internalCats.length];
		for (Category cat : internalCats) {
			CATEGORIES[cat.ordinal()] = MarkerManager.getMarker(cat.name());
		}
	}

	Logger rootLogger;
	int level;

	@Override
	public void start(int level, Category[] categories) {
		this.level = level;

		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

		if (ctx.getConfiguration().getName().startsWith("Default@")) {
			assert (ctx.getConfiguration().getConfigurationSource() == ConfigurationSource.NULL_SOURCE);

			ConfigurationFactory factory = new XtreemFSConfigurationFactory(level, categories);
			ConfigurationFactory.setConfigurationFactory(factory);
			ctx.reconfigure();
		}

		rootLogger = LogManager.getFormatterLogger(LogManager.ROOT_LOGGER_NAME);
	}

    @Override
    public void redirect(PrintStream out) {
        // TODO Auto-generated method stub

    }

    Logger getLogger(int level, Category cat, Object me) {
        if (PERCLASS && me != null) {
            return LogManager.getFormatterLogger(me.getClass());
        } else {
            return rootLogger;
        }
    }

	@Override
	public void logMessage(int level, Category cat, Object me, String formatPattern, Object[] args) {
		Logger logger = getLogger(level, cat, me);
		logger.log(LEVELS[level], CATEGORIES[cat.ordinal()], formatPattern, args);
	}

	@Override
	public void logMessage(int level, Object me, String formatPattern, Object[] args) {
		Logger logger = getLogger(level, Category.all, me);
		logger.log(LEVELS[level], CATEGORIES[Category.all.ordinal()], formatPattern, args);
	}

	@Override
	public void logError(int level, Object me, Throwable msg) {
		Logger logger = getLogger(level, Category.all, me);
		logger.log(LEVELS[level], CATEGORIES[Category.all.ordinal()], msg.getMessage(), msg);
	}

	@Override
	public void logUserError(int level, Category cat, Object me, Throwable msg) {
		Logger logger = getLogger(level, cat, me);
		logger.log(LEVELS[level], CATEGORIES[cat.ordinal()], msg.getMessage(), msg);
	}

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public boolean isDebug() {
		return rootLogger.isDebugEnabled();
    }

    @Override
    public boolean isInfo() {
		return rootLogger.isInfoEnabled();
    }

    @Override
    public boolean isNotice() {
		return rootLogger.isEnabled(LEVELS[Logging.LEVEL_NOTICE]);
    }


    public class XtreemFSConfigurationFactory extends ConfigurationFactory {

        final Configuration configuration;

        public XtreemFSConfigurationFactory(int level, Category[] categories) {
            ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder();

            builder.setConfigurationName("XtreemFSDefault");
            builder.setStatusLevel(Level.WARN);

            RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Log4jImpl.LEVELS[level], true)
                    .add(builder.newAppenderRef("STDOUT"));

            AppenderComponentBuilder appender = builder.newAppender("STDOUT", "Console").addAttribute("target",
                    ConsoleAppender.Target.SYSTEM_OUT);

            LayoutComponentBuilder layout = builder.newLayout("PatternLayout").addAttribute("pattern",
                    "[ %level{EMERG=E, ALERT=E, CRIT=E, ERROR=E, WARN=W, NOTICE=I, INFO=I, DEBUG=D, TRACE=D, length=1}"
                            + " | %-6.-6markerSimpleName" 
                            + " | %-20maxLength{%c{1}}{20}" 
                            + " | %-15maxLength{%t}{15}"
                            + " | %3tid" 
                            + " | %d{MMM dd HH:mm:ss} ]" 
                            + " %m%n" 
                            + "%notEmpty{%ex}");
            appender.add(layout);

            if (categories.length > 0) {
            	boolean allSet = false;
            	
                ComponentBuilder<FilterComponentBuilder> filters = builder.newComponent("Filters");               
                for (Category cat : categories) {
                	if (cat == Category.all) {
                		allSet = true;
                		break;
                	}
                	
                    Marker marker = Log4jImpl.CATEGORIES[cat.ordinal()];
                    FilterComponentBuilder markerFilter = builder
                            .newFilter("MarkerFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL)
                            .addAttribute("marker", marker.getName());
                    filters.addComponent(markerFilter);
                }

                if (!allSet) {
	                Marker marker = Log4jImpl.CATEGORIES[Category.all.ordinal()];
	                FilterComponentBuilder markerFilter = builder
	                        .newFilter("MarkerFilter", Filter.Result.ACCEPT, Filter.Result.DENY)
	                        .addAttribute("marker", marker.getName());
	                filters.addComponent(markerFilter);
	
	                rootLogger.addComponent(filters);
                }
            }


            builder.add(rootLogger);
            builder.add(appender);
            configuration = builder.build();
        }



        @Override
        public Configuration getConfiguration(ConfigurationSource source) {
            return configuration;
        }

        @Override
        public Configuration getConfiguration(final String name, final URI configLocation) {
            return configuration;
        }

        @Override
        protected String[] getSupportedTypes() {
            return new String[] { "*" };
        }
    }


}
