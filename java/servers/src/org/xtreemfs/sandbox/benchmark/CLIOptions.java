/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import static org.xtreemfs.foundation.logging.Logging.LEVEL_INFO;
import static org.xtreemfs.foundation.logging.Logging.logMessage;
import static org.xtreemfs.foundation.util.CLIParser.CliOption.OPTIONTYPE.STRING;
import static org.xtreemfs.foundation.util.CLIParser.CliOption.OPTIONTYPE.SWITCH;
import static org.xtreemfs.foundation.util.CLIParser.parseCLI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.utils.DefaultDirConfig;
import org.xtreemfs.utils.utils;

/**
 * @author jensvfischer
 */
public class CLIOptions {
    static Map<String, CLIParser.CliOption> options;
    static List<String>                     arguments;
    static ParamsBuilder                    builder;

    static {
        CLIOptions.options = utils.getDefaultAdminToolOptions(true);
        CLIOptions.builder = new ParamsBuilder();
        CLIOptions.arguments = new ArrayList<String>(5);
    }

    static void parseCLIOptions(String[] args) {
        initOptions();
        parseCLI(args, options, arguments);
    }

    private static void displayUsageIfSet() {
        if (usageIsSet())
            displayUsage();
    }

    static Params buildParams() {
        setNumberOfThreads();
        setNumberOfRepetitions();
        setSequentialSize();
        setRandomSize();
        setBasefileSize();
        setFileSize();
        setDirAddress();
        setUsername();
        setGroup();
        setOSDPassword();
        setAuth();
        setSSLOptions();
        setOptions();
        setStripeSize();
        setStripeWidth();
        setNoCleanup();
        setNoCleanupOfVolumes();
        setNoCleanupOfBasefile();
        return builder.build();
    }

    private static void initOptions() {

        /*
         * -sw -sr -rw -p -rr -rfr -rfw -p <number> -r <number> -ssize -rsize --file-size --basefile-size (--no-cleanup
         * | --no-cleanup-volumes) volume1 volume2
         */

        /* Connection Data */
        options.put("-dir-address", new CLIParser.CliOption(STRING,
                "directory service to use (e.g. 'localhost:32638'). If no URI is specified, URI and security settings are taken from '"
                        + DefaultDirConfig.DEFAULT_DIR_CONFIG + "'", "<uri>"));
        options.put("-username", new CLIParser.CliOption(STRING, "username to use", "<username>"));

        /* benchmark switches */
        options.put("sw", new CLIParser.CliOption(SWITCH, "sequential write benchmark", ""));
        options.put("sr", new CLIParser.CliOption(SWITCH, "sequential read benchmark", ""));
        options.put("rw", new CLIParser.CliOption(SWITCH, "random write benchmark", ""));
        options.put("rr", new CLIParser.CliOption(SWITCH, "random read benchmark", ""));
        options.put("rfw", new CLIParser.CliOption(SWITCH, "random filebased write benchmark", ""));
        options.put("rfr", new CLIParser.CliOption(SWITCH, "random filebased read benchmark", ""));

        options.put("p", new CLIParser.CliOption(STRING,
                "number of sequential benchmarks to be started in parallel. default: 1", "<number>"));
        options.put("r", new CLIParser.CliOption(STRING, "number of repetitions of a benchmarks. default: 1",
                "<number>"));
        options.put("-stripe-size", new CLIParser.CliOption(STRING,
                "stripeSize in [B|K|M|G] (no modifier assumes bytes). default: 128K", "<stripeSize>"));
        options.put("-stripe-width", new CLIParser.CliOption(STRING, "stripe width. default: 1", "<stripe width>"));

        /* sizes */
        options.put("ssize", new CLIParser.CliOption(STRING,
                "size for sequential benchmarks in [B|K|M|G] (no modifier assumes bytes)", "<size>"));
        options.put("rsize", new CLIParser.CliOption(STRING,
                "size for random benchmarks in [B|K|M|G] (no modifier assumes bytes)", "<size>"));
        options.put("-basefile-size", new CLIParser.CliOption(STRING,
                "size of the basefile for random benchmarks in [B|K|M|G] (no modifier assumes bytes)", "<size>"));
        options.put("-file-size", new CLIParser.CliOption(STRING,
                "size of the files for random filebased benchmarks in [B|K|M|G] (no modifier assumes bytes)."
                        + " The filesize for filebased random IO Benchmarks must be <= 23^31-1", "<size>"));

        /* deletion options */
        String noCleanupDescription = "do not delete created volumes and files. Volumes and files need to be removed "
                + "manually. Volumes can be removed using rmfs.xtreemfs. Files can be removed by mounting the according "
                + "volume with mount.xtreemfs and deleting the files with rm";
        options.put("-no-cleanup", new CLIParser.CliOption(SWITCH, noCleanupDescription, ""));

        options.put("-no-cleanup-volumes", new CLIParser.CliOption(SWITCH,
                "do not delete created volumes. Created volumes neet to be removed manually using rmfs.xtreemfs", ""));

        String noCleanupBasefileDescription = "do not delete created basefile (only works with --no-cleanup or "
                + "--no-cleanup-volumes. Created Files and volumes need to be removed manually";
        options.put("-no-cleanup-basefile", new CLIParser.CliOption(SWITCH, noCleanupBasefileDescription, ""));
    }

    static boolean usageIsSet() {
        return (options.get(utils.OPTION_HELP).switchValue || options.get(utils.OPTION_HELP_LONG).switchValue);
    }

    /**
     * Prints out displayUsage informations
     */
    static void displayUsage() {

        System.out.println("\nusage: xtfs_benchmark [options] volume1 volume2 ... \n");
        System.out
                .println("The number of volumes must be in accordance with the number of benchmarks run in parallel (see -p).");
        System.out
                .println("All sizes can be modified with multiplication modifiers, where K means KiB, M means MiB and G means GiB. \nIf no modifier is given, sizes are assumed to be in bytes.");
        System.out.println();
        System.out.println("  " + "options:");
        utils.printOptions(options);
        System.out.println();
        System.out.println("example: xtfs_benchmark -sw -sr -p 3 -ssize 3G volume1 volume2 volume3");
        System.out
                .println("\t\t starts a sequential write and read benchmark of 3 GiB with 3 benchmarks in parallel on volume1, volume2 and volume3\n");
    }

    private static void setNumberOfThreads() {
        String optionValue = options.get("p").stringValue;
        if (null != optionValue)
            builder.setNumberOfThreads(Integer.valueOf(optionValue));
    }

    private static void setNumberOfRepetitions() {
        String optionValue = options.get("r").stringValue;
        if (null != optionValue)
            builder.setNumberOfRepetitions(Integer.valueOf(optionValue));
    }

    private static void setSequentialSize() {
        String optionValue;
        optionValue = options.get("ssize").stringValue;
        if (null != optionValue)
            builder.setSequentialSizeInBytes(parseSizeWithModifierToBytes(optionValue));
    }

    private static void setRandomSize() {
        String optionValue;
        optionValue = options.get("rsize").stringValue;
        if (null != optionValue)
            builder.setRandomSizeInBytes(parseSizeWithModifierToBytes(optionValue));
    }

    private static void setBasefileSize() {
        String optionValue;
        optionValue = options.get("-basefile-size").stringValue;
        if (null != optionValue)
            builder.setBasefileSizeInBytes(parseSizeWithModifierToBytes(optionValue));
    }

    private static void setFileSize() {
        String optionValue;
        optionValue = options.get("-file-size").stringValue;
        if (null != optionValue) {
            long sizeInBytes = parseSizeWithModifierToBytes(optionValue);
            if (sizeInBytes > Integer.MAX_VALUE)
                throw new IllegalArgumentException("Filesize for filebased random IO Benchmarks must be <= 23^31-1");
            builder.setRandomIOFilesize((int) sizeInBytes);
        }
    }

    /* Use DirAdress from Param, if not present, use DirAddress from ConfigFile, if not preset, use Default */
    private static void setDirAddress() {
        String dirAddressFromCLI = options.get("-dir-address").stringValue;
        String dirAddressFromConfig = getDefaultDir();
        if (null != dirAddressFromCLI)
            builder.setDirAddress(dirAddressFromCLI);
        else if (null != dirAddressFromConfig)
            builder.setDirAddress(dirAddressFromConfig);
    }

    private static void setUsername() {
        String userName = options.get("-username").stringValue;
        if (null != userName)
            builder.setUserName(userName);
    }

    private static void setGroup() {
        // Todo Replace with real setGroup()
        String userName = options.get("-username").stringValue;
        if (null != userName)
            builder.setGroup(userName);
    }

    private static void setOSDPassword() {
        String osdPassword = options.get(utils.OPTION_ADMIN_PASS).stringValue;
        if (null != osdPassword)
            builder.setOsdPassword(osdPassword);
    }

    private static void setAuth() {
        // Todo (jvf) implement?
    }

    private static void setSSLOptions() {
        // Todo (jvf) Implement SSL Options?
    }

    private static void setOptions() {
        // Todo (jvf) implement?
    }

    private static void setStripeSize() {
        String stripeSize = options.get("-stripe-size").stringValue;
        if (null != stripeSize) {
            long stripeSizeInBytes = parseSizeWithModifierToBytes(stripeSize);
            assert stripeSizeInBytes <= Integer.MAX_VALUE : "StripeSize must be less equal than Integer.MAX_VALUE";
            builder.setStripeSizeInBytes((int) stripeSizeInBytes);
        }
    }

    private static void setStripeWidth() {
        String stripeWidth = options.get("-stripe-width").stringValue;
        if (null != stripeWidth)
            builder.setStripeWidth(Integer.parseInt(stripeWidth));
    }

    private static void setNoCleanup() {
        boolean switchValue = options.get("-no-cleanup").switchValue;
        builder.setNoCleanup(switchValue);
    }

    private static void setNoCleanupOfVolumes() {
        boolean switchValue = options.get("-no-cleanup-volumes").switchValue;
        builder.setNoCleanupOfVolumes(switchValue);
    }

    private static void setNoCleanupOfBasefile() {
        boolean switchValue = options.get("-no-cleanup-basefile").switchValue;
        builder.setNoCleanupOfBasefile(switchValue);
    }

    static boolean sequentialWriteBenchmarkIsSet() {
        return options.get("sw").switchValue;
    }

    static boolean sequentialReadBenchmarkIsSet() {
        return options.get("sr").switchValue;
    }

    static boolean randomReadBenchmarkIsSet() {
        return options.get("rr").switchValue;
    }

    static boolean randomWriteBenchmarkIsSet() {
        return options.get("rw").switchValue;
    }

    static boolean randomFilebasedWriteBenchmarkIsSet() {
        return options.get("rfw").switchValue;
    }

    static boolean randomFilebasedReadBenchmarkIsSet() {
        return options.get("rfr").switchValue;
    }

    private static long parseSizeWithModifierToBytes(String size) {
        size = size.toUpperCase();
        long sizeInBytes;

        if (!size.matches("[0-9]+[BKMG]?"))
            throw new IllegalArgumentException("Wrong format for size.");

        if (size.matches("[0-9]+")) {
            sizeInBytes = Long.valueOf(size);
        } else {
            char suffix = size.charAt(size.length() - 1);
            long numbers = Long.valueOf(size.substring(0, size.length() - 1));
            switch (suffix) {
            case 'B':
                sizeInBytes = numbers;
                break;
            case 'K':
                sizeInBytes = numbers * (long) xtfs_benchmark.KiB_IN_BYTES;
                break;
            case 'M':
                sizeInBytes = numbers * (long) xtfs_benchmark.MiB_IN_BYTES;
                break;
            case 'G':
                sizeInBytes = numbers * (long) xtfs_benchmark.GiB_IN_BYTES;
                break;
            default:
                sizeInBytes = 0L;
                break;
            }
        }
        return sizeInBytes;
    }

    // Todo (jvf) Move to Controller or Params?
    private static String getDefaultDir() {
        String[] dirAddresses;
        DefaultDirConfig cfg = null;
        try {
            cfg = new DefaultDirConfig();
            dirAddresses = cfg.getDirectoryServices();
            return dirAddresses[0];
        } catch (IOException e) {
            logMessage(LEVEL_INFO, Logging.Category.tool, xtfs_benchmark.class,
                    "Could not read or find Default DIR Config in %s. Errormessage: %s",
                    DefaultDirConfig.DEFAULT_DIR_CONFIG, e.getMessage());
            return null;
        }
    }
}
