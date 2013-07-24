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
    Map<String, CLIParser.CliOption> options;
    List<String>                     arguments;
    ParamsBuilder                    builder;

    private static final String SEQ_WRITE;
    private static final String SEQ_READ;
    private static final String RAND_WRITE;
    private static final String RAND_READ;
    private static final String FILEBASED_WRITE;
    private static final String FILEBASED_READ;
    private static final String THREADS;
    private static final String REPETITIONS;
    private static final String STRIPE_SIZE;
    private static final String STRIPE_WITDH;
    private static final String SIZE_SEQ;
    private static final String SIZE_RAND;
    private static final String SIZE_BASEFILE;
    private static final String SIZE_FILES;
    private static final String NO_CLEANUP;
    private static final String NO_CLEANUP_VOLUMES;
    private static final String NO_CLEANUP_BASEFILE;
          


    static {
        SEQ_WRITE = "sw";
        SEQ_READ = "sr";
        RAND_WRITE = "rw";
        RAND_READ = "rr";
        FILEBASED_WRITE = "fw";
        FILEBASED_READ = "fr";
        THREADS = "t";
        REPETITIONS = "r";
        STRIPE_SIZE = "-stripe-size";
        STRIPE_WITDH = "-stripe-width";
        SIZE_SEQ = "ssize";
        SIZE_RAND = "rsize";
        SIZE_BASEFILE = "-basefile-size";
        SIZE_FILES = "-file-size";
        NO_CLEANUP = "-no-cleanup";
        NO_CLEANUP_VOLUMES = "-no-cleanup-volumes";
        NO_CLEANUP_BASEFILE = "-no-cleanup-basefile";
    }

    public CLIOptions() {
        this.options = utils.getDefaultAdminToolOptions(true);
        this.builder = new ParamsBuilder();
        this.arguments = new ArrayList<String>(5);
    }

    void parseCLIOptions(String[] args) {
        initOptions();
        parseCLI(args, options, arguments);
    }

    private void displayUsageIfSet() {
        if (usageIsSet())
            displayUsage();
    }

    Params buildParams() {
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

    private void initOptions() {

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
        options.put(SEQ_WRITE, new CLIParser.CliOption(SWITCH, "sequential write benchmark", ""));
        options.put(SEQ_READ, new CLIParser.CliOption(SWITCH, "sequential read benchmark", ""));
        options.put(RAND_WRITE, new CLIParser.CliOption(SWITCH, "random write benchmark", ""));
        options.put(RAND_READ, new CLIParser.CliOption(SWITCH, "random read benchmark", ""));
        options.put(FILEBASED_WRITE, new CLIParser.CliOption(SWITCH, "random filebased write benchmark", ""));
        options.put(FILEBASED_READ, new CLIParser.CliOption(SWITCH, "random filebased read benchmark", ""));

        options.put(THREADS, new CLIParser.CliOption(STRING,
                "number of benchmarks to be started in parallel. default: 1", "<number>"));
        options.put(REPETITIONS, new CLIParser.CliOption(STRING, "number of repetitions of a benchmarks. default: 1",
                "<number>"));
        options.put(STRIPE_SIZE, new CLIParser.CliOption(STRING,
                "stripeSize in [B|K|M|G] (no modifier assumes bytes). default: 128K", "<stripeSize>"));
        options.put(STRIPE_WITDH, new CLIParser.CliOption(STRING, "stripe width. default: 1", "<stripe width>"));

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

    boolean usageIsSet() {
        return (options.get(utils.OPTION_HELP).switchValue || options.get(utils.OPTION_HELP_LONG).switchValue);
    }

    /**
     * Prints out displayUsage informations
     */
    void displayUsage() {

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

    private void setNumberOfThreads() {
        String optionValue = options.get("p").stringValue;
        if (null != optionValue)
            builder.setNumberOfThreads(Integer.valueOf(optionValue));
    }

    private void setNumberOfRepetitions() {
        String optionValue = options.get("r").stringValue;
        if (null != optionValue)
            builder.setNumberOfRepetitions(Integer.valueOf(optionValue));
    }

    private void setSequentialSize() {
        String optionValue;
        optionValue = options.get("ssize").stringValue;
        if (null != optionValue)
            builder.setSequentialSizeInBytes(parseSizeWithModifierToBytes(optionValue));
    }

    private void setRandomSize() {
        String optionValue;
        optionValue = options.get("rsize").stringValue;
        if (null != optionValue)
            builder.setRandomSizeInBytes(parseSizeWithModifierToBytes(optionValue));
    }

    private void setBasefileSize() {
        String optionValue;
        optionValue = options.get("-basefile-size").stringValue;
        if (null != optionValue)
            builder.setBasefileSizeInBytes(parseSizeWithModifierToBytes(optionValue));
    }

    private void setFileSize() {
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
    private void setDirAddress() {
        String dirAddressFromCLI = options.get("-dir-address").stringValue;
        String dirAddressFromConfig = getDefaultDir();
        if (null != dirAddressFromCLI)
            builder.setDirAddress(dirAddressFromCLI);
        else if (null != dirAddressFromConfig)
            builder.setDirAddress(dirAddressFromConfig);
    }

    private void setUsername() {
        String userName = options.get("-username").stringValue;
        if (null != userName)
            builder.setUserName(userName);
    }

    private void setGroup() {
        // Todo Replace with real setGroup()
        String userName = options.get("-username").stringValue;
        if (null != userName)
            builder.setGroup(userName);
    }

    private void setOSDPassword() {
        String osdPassword = options.get(utils.OPTION_ADMIN_PASS).stringValue;
        if (null != osdPassword)
            builder.setOsdPassword(osdPassword);
    }

    private void setAuth() {
        // Todo (jvf) implement?
    }

    private void setSSLOptions() {
        // Todo (jvf) Implement SSL Options?
    }

    private void setOptions() {
        // Todo (jvf) implement?
    }

    private void setStripeSize() {
        String stripeSize = options.get("-stripe-size").stringValue;
        if (null != stripeSize) {
            long stripeSizeInBytes = parseSizeWithModifierToBytes(stripeSize);
            assert stripeSizeInBytes <= Integer.MAX_VALUE : "StripeSize must be less equal than Integer.MAX_VALUE";
            builder.setStripeSizeInBytes((int) stripeSizeInBytes);
        }
    }

    private void setStripeWidth() {
        String stripeWidth = options.get("-stripe-width").stringValue;
        if (null != stripeWidth)
            builder.setStripeWidth(Integer.parseInt(stripeWidth));
    }

    private void setNoCleanup() {
        boolean switchValue = options.get("-no-cleanup").switchValue;
        builder.setNoCleanup(switchValue);
    }

    private void setNoCleanupOfVolumes() {
        boolean switchValue = options.get("-no-cleanup-volumes").switchValue;
        builder.setNoCleanupOfVolumes(switchValue);
    }

    private void setNoCleanupOfBasefile() {
        boolean switchValue = options.get("-no-cleanup-basefile").switchValue;
        builder.setNoCleanupOfBasefile(switchValue);
    }

    boolean sequentialWriteBenchmarkIsSet() {
        return options.get(SEQ_WRITE).switchValue;
    }

    boolean sequentialReadBenchmarkIsSet() {
        return options.get(SEQ_READ).switchValue;
    }

    boolean randomReadBenchmarkIsSet() {
        return options.get(RAND_READ).switchValue;
    }

    boolean randomWriteBenchmarkIsSet() {
        return options.get(RAND_WRITE).switchValue;
    }

    boolean randomFilebasedWriteBenchmarkIsSet() {
        return options.get("rfw").switchValue;
    }

    boolean randomFilebasedReadBenchmarkIsSet() {
        return options.get("rfr").switchValue;
    }

    private long parseSizeWithModifierToBytes(String size) {
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
    private String getDefaultDir() {
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
