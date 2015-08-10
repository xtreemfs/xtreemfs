/*
 * Copyright (c) 2009-2009 by Bjoern Kolbeck, Nele Andersen,
 *                            Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.utils.xtfs_scrub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.AdminVolume;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.VersionManagement;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.foundation.util.CLIParser.CliOption;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.PORTS;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntries;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntry;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XATTR_FLAGS;
import org.xtreemfs.utils.DefaultDirConfig;
import org.xtreemfs.utils.utils;

/**
 * 
 * @author bjko
 */
public class xtfs_scrub {

    public static interface FileScrubbedListener {
        public void fileScrubbed(String FileName, long bytesScrubbed, Collection<ReturnStatus> rstatus);
    }

    public static enum ReturnStatus {
        FILE_OK, FILE_LOST, WRONG_FILE_SIZE, FAILURE_OBJECTS, FAILURE_REPLICAS, UNREACHABLE;
    };

    public static String                latestScrubAttr     = "scrubber.latestscrub";

    public static final UserCredentials credentials;

    static {
        credentials = UserCredentials.newBuilder().setUsername("root").addGroups("root").build();
    }

    private static final int            DEFAULT_NUM_THREADS = 10;

    private AdminVolume                 volume;

    private final boolean               repair, delete, silent;

    private final ExecutorService       tPool;

    private long                        lastBytesScrubbed;

    private final Stack<String>         directories;

    private final Stack<String>         files;

    private final Object                completeLock;

    private int                         returnCode;

    private int                         numInFlight;

    private final int                   numThrs;

    private boolean                     hasFinished;

    private boolean                     isLatestScrubAttrSettable;

    private String                      currentDirName      = null;

    private int                         numFiles, numReplicaFailure, numObjectFailure, numFileOk,
            numUnreachable, numWrongFS, numDead;

    private final Set<String>           removedOSDs;

    public xtfs_scrub(AdminClient client, AdminVolume volume, int numThrs, boolean repair, boolean delete,
            boolean silent) throws IOException {
        this.repair = repair;
        this.delete = delete;
        this.silent = silent;
        this.numThrs = numThrs;
        directories = new Stack<String>();
        files = new Stack<String>();
        tPool = Executors.newFixedThreadPool(numThrs);
        numInFlight = 0;
        completeLock = new Object();
        hasFinished = false;
        isLatestScrubAttrSettable = true;

        this.volume = volume;

        numFiles = 0;
        numReplicaFailure = 0;
        numObjectFailure = 0;
        numFileOk = 0;
        numUnreachable = 0;

        if (!repair && !delete) {
            System.out.println("running in check mode, no changes to files will be made");
        } else {
            if (repair)
                System.out.println("running in repair mode");
            if (delete)
                System.out
                        .println("WARNING: delete is enabled, corrupt files (data lost due to failed OSDs) will be deleted");
        }

        removedOSDs = client.getRemovedOsds();

        if (removedOSDs.size() > 0) {
            System.out
                    .println("list of OSDs that have been removed (replicas on these OSDs will be deleted):");
            for (String uuid : removedOSDs) {
                System.out.println("\t" + uuid);
            }
        }

    }

    public int scrub() {

        System.out.println("");
        directories.push("/");
        // create scrub xattr if not done yet
        try {
            volume.setXAttr(credentials, "/", latestScrubAttr, Long.toString(TimeSync.getLocalSystemTime()),
                    XATTR_FLAGS.XATTR_FLAGS_CREATE);
        } catch (PosixErrorException e) {
            // scrub xattr already exists, nothing to do here.

        } catch (IOException e) {
            isLatestScrubAttrSettable = false;
            System.out.println("\nWarning: cannot mark volume as scrubbed: " + e);
        }
        fillQueue();
        synchronized (completeLock) {
            try {
                if (!hasFinished) {
                    completeLock.wait();
                }
            } catch (InterruptedException ex) {
            }
        }
        tPool.shutdown();
        try {
            tPool.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
        }

        if (!silent)
            System.out.format("scrubbed %-42s      %15s - total %15s\n\u001b[100D\u001b[A", "all files", "",
                    OutputUtils.formatBytes(lastBytesScrubbed));

        System.out.println("\n\nsummary:");
        System.out.println("files checked                                                   : " + numFiles);
        System.out.println("  files ok                                                      : " + numFileOk);
        System.out.println("  files corrupted                                               : " + (numFiles - numFileOk));
        System.out.println("    of which had lost replicas (caused by removed OSDs)         : " + numReplicaFailure);
        System.out.println("    of which had corrupted objects (caused by invalid checksums): " + numObjectFailure);
        System.out.println("    of which had a wrong file size                              : " + numWrongFS);
        System.out.println("    of which are lost (unrecoverable)                           : " + numDead);
        System.out.println("    of which are unreachable (caused by communication errors)   : " + numUnreachable);
        System.out.println("bytes checked                                                   : " + OutputUtils.formatBytes(lastBytesScrubbed));

        return returnCode;
    }

    private void fillQueue() {
        try {
            synchronized (directories) {
                while (numInFlight < numThrs) {
                    try {
                        String fileName = files.pop();
                        try {

                            FileScrubbedListener fsListener = new FileScrubbedListener() {
                                public void fileScrubbed(String fileName, long bytesScrubbed,
                                        Collection<ReturnStatus> rstatus) {
                                    xtfs_scrub.this.fileScrubbed(fileName, bytesScrubbed, rstatus);
                                }
                            };

                            FileScrubber fi = new FileScrubber(fileName, volume, fsListener, removedOSDs,
                                    repair, delete);
                            tPool.submit(fi);
                            numInFlight++;
                        } catch (IOException ex) {
                            Logging.logError(Logging.LEVEL_WARN, this, ex);
                        }
                    } catch (EmptyStackException ex) {
                        // fetch next dir
                        fetchNextDir();
                    }
                }
            }
        } catch (EmptyStackException ex) {
            // no more entries, finished!
            if (isLatestScrubAttrSettable) {
                try {
                    // mark volume as scrubbed
                    volume.setXAttr(credentials, "/", latestScrubAttr,
                            Long.toString(TimeSync.getLocalSystemTime()), XATTR_FLAGS.XATTR_FLAGS_REPLACE);

                } catch (IOException ex2) {
                    System.out.println("\nWarning: cannot mark volume as successfully scrubbed: " + ex2);
                }
            }

            finish(0);
            return;
        }
    }

    private void finish(int returnCode) {
        synchronized (directories) {
            if (numInFlight == 0) {
                synchronized (completeLock) {
                    this.returnCode = returnCode;
                    this.hasFinished = true;
                    completeLock.notifyAll();
                }
            }
        }

    }

    public void fileScrubbed(String fileName, long bytesScrubbed, Collection<ReturnStatus> rs) {

        // update statistics

        if (fileName.length() > 42) {
            fileName = "..." + fileName.substring(fileName.length() - 39, fileName.length());
        }
        synchronized (directories) {
            lastBytesScrubbed += bytesScrubbed;
            numFiles++;
            for (ReturnStatus status : rs) {
                switch (status) {
                case FILE_OK:
                    numFileOk++;
                    break;
                case WRONG_FILE_SIZE:
                    numWrongFS++;
                    break;
                case UNREACHABLE:
                    numUnreachable++;
                    break;
                case FILE_LOST:
                    numDead++;
                    break;
                case FAILURE_REPLICAS:
                    numReplicaFailure++;
                    break;
                case FAILURE_OBJECTS:
                    numObjectFailure++;
                    break;
                }
            }
            if (!silent)
                System.out.format("scrubbed %-42s with %15s - total %15s\n\u001b[100D\u001b[A", fileName,
                        OutputUtils.formatBytes(bytesScrubbed), OutputUtils.formatBytes(lastBytesScrubbed));
            numInFlight--;
            fillQueue();
        }

    }

    private void fetchNextDir() {
        currentDirName = directories.pop();

        try {

            DirectoryEntries ls = volume.readDir(credentials, currentDirName, 0, 0, false);

            if (ls == null) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, "path %s does not exist!", currentDirName);
                return;
            }

            for (int i = 0; i < ls.getEntriesCount(); i++) {
                DirectoryEntry e = ls.getEntries(i);
                if ((e.getStbuf().getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFREG.getNumber()) != 0) {
                    // regular file
                    files.push(currentDirName + e.getName());
                } else if ((e.getStbuf().getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFDIR.getNumber()) != 0) {
                    if (!e.getName().equals(".") && !e.getName().equals(".."))
                        directories.push(currentDirName + e.getName() + "/");
                }
            }
        } catch (IOException ex) {
            System.err.println("cannot contact MRC... aborting");
            System.err.println(ex);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EmptyStackException();
        }

    }

    public static void main(String[] args) {

        Logging.start(Logging.LEVEL_WARN);

        System.out.println("XtreemFS scrubber version " + VersionManagement.RELEASE_VERSION
                + " (file system data integrity check)\n");

        Map<String, CliOption> options = utils.getDefaultAdminToolOptions(false);
        List<String> arguments = new ArrayList<String>(1);
        CliOption oDir = new CliOption(
                CliOption.OPTIONTYPE.STRING,
                "directory service to use (e.g. 'pbrpc://localhost:32638'). If no URI is specified, URI and security settings are taken from '"
                        + DefaultDirConfig.DEFAULT_DIR_CONFIG + "'", "<uri>");
        oDir.urlDefaultPort = PORTS.DIR_PBRPC_PORT_DEFAULT.getNumber();
        oDir.urlDefaultProtocol = Schemes.SCHEME_PBRPC;
        options.put("dir", oDir);
        options.put("repair", new CliOption(CliOption.OPTIONTYPE.SWITCH,
                "repair files (update file size, replace replicas) when possible", ""));
        options.put("delete", new CliOption(CliOption.OPTIONTYPE.SWITCH,
                "delete lost files (incomplete due to OSD failures)", ""));
        options.put("silent", new CliOption(CliOption.OPTIONTYPE.SWITCH, "don't show the progress bar", ""));
        options.put("thrs", new CliOption(CliOption.OPTIONTYPE.NUMBER,
                "number of concurrent file scrub threads (default=" + DEFAULT_NUM_THREADS + ")", "n"));

        CLIParser.parseCLI(args, options, arguments);

        if (arguments.size() != 1)
            error("invalid number of arguments", options);

        if (options.get(utils.OPTION_HELP).switchValue || options.get(utils.OPTION_HELP_LONG).switchValue) {
            usage(options);
            System.exit(0);
        }

        String[] dirURLs = (options.get("dir").stringValue != null) ? options.get("dir").stringValue
                .split(",") : null;

        SSLOptions sslOptions = null;
        String[] dirAddrs = null;

        if (dirURLs != null) {
            int i = 0;
            boolean gridSSL = false;
            dirAddrs = new String[dirURLs.length];
            for (String dirURL : dirURLs) {

                // parse security info if protocol is 'https'
                if (dirURL.contains(Schemes.SCHEME_PBRPCS + "://")
                        || dirURL.contains(Schemes.SCHEME_PBRPCG + "://") && sslOptions == null) {
                    String serviceCredsFile = options.get(utils.OPTION_USER_CREDS_FILE).stringValue;
                    String serviceCredsPass = options.get(utils.OPTION_USER_CREDS_PASS).stringValue;
                    if(serviceCredsPass != null && serviceCredsPass.equals("-")) {
                    	serviceCredsPass = new String(System.console().readPassword("Enter credentials password: "));
                    }
                    String trustedCAsFile = options.get(utils.OPTION_TRUSTSTORE_FILE).stringValue;
                    String trustedCAsPass = options.get(utils.OPTION_TRUSTSTORE_PASS).stringValue;
                    if(trustedCAsPass != null && trustedCAsPass.equals("-")) {
                    	trustedCAsPass = new String(System.console().readPassword("Enter trust store password: "));
                    }
                    String sslProtocolString = options.get(utils.OPTION_SSL_PROTOCOL).stringValue;
                    if (dirURL.contains(Schemes.SCHEME_PBRPCG + "://")) {
                        gridSSL = true;
                    }

                    if (serviceCredsFile == null) {
                        System.out.println("SSL requires '-" + utils.OPTION_USER_CREDS_FILE
                                + "' parameter to be specified");
                        usage(options);
                        System.exit(1);
                    } else if (trustedCAsFile == null) {
                        System.out.println("SSL requires '-" + utils.OPTION_TRUSTSTORE_FILE
                                + "' parameter to be specified");
                        usage(options);
                        System.exit(1);
                    }

                    // TODO: support custom SSL trust managers
                    try {
                        sslOptions = new SSLOptions(serviceCredsFile, serviceCredsPass, SSLOptions.PKCS12_CONTAINER,
                                trustedCAsFile, trustedCAsPass, SSLOptions.JKS_CONTAINER, false, gridSSL,
                                sslProtocolString, null);
                    } catch (Exception e) {
                        System.err.println("unable to set up SSL, because:" + e.getMessage());
                        System.exit(1);
                    }
                }

                // add URL to dirAddrs
                if (dirURL.contains("://")) {
                    // remove Protocol information
                    String[] tmp = dirURL.split("://");
                    // remove possible slash
                    dirAddrs[i++] = tmp[1].replace("/", "");
                } else {
                    // remove possible slash
                    dirAddrs[i++] = dirURL.replace("/", "");
                }
            }
        }

        // read default settings
        if (dirURLs == null) {
            try {
                DefaultDirConfig cfg = new DefaultDirConfig();
                sslOptions = cfg.getSSLOptions();
                dirAddrs = cfg.getDirectoryServices();
            } catch (Exception e) {
                System.err.println("unable to get SSL options, because: " + e.getMessage());
                System.exit(1);
            }
        }

        boolean repair = options.get("repair").switchValue;
        boolean silent = options.get("silent").switchValue;
        boolean delete = options.get("delete").switchValue;

        int numThreads = DEFAULT_NUM_THREADS;
        if (options.get("thrs").numValue != null) {
            numThreads = options.get("thrs").numValue.intValue();
        }

        final String volumeName = arguments.get(0);

        Options userOptions = new Options();

        AdminClient c = ClientFactory.createAdminClient(dirAddrs, credentials, sslOptions, userOptions);
        AdminVolume volume = null;
        try {
            c.start();
            volume = c.openVolume(volumeName, sslOptions, new Options());
            volume.start();
        } catch (Exception e) {
            System.err.println("unable to scrub Volume, because: " + e.getMessage());
            System.exit(1);
        }

        int exitCode = 1;
        try {

            xtfs_scrub scrubber = new xtfs_scrub(c, volume, numThreads, repair, delete, silent);
            exitCode = scrubber.scrub();
            if (exitCode == 0) {
                System.out.println("\n\nsuccessfully scrubbed volume '" + volumeName + "'");
            } else {
                System.out.println("\n\nscrubbing volume '" + volumeName + "' FAILED!");
            }

            System.exit(exitCode);
        } catch (Exception e) {
            System.err.println("unable to scrub Volume, because: " + e.getMessage());
            System.exit(1);
        }
        c.shutdown();

    }

    private static void error(String message, Map<String, CliOption> options) {
        System.err.println(message);
        System.out.println();
        usage(options);
        System.exit(1);
    }

    private static void usage(Map<String, CliOption> options) {
        System.out.println("usage: xtfs_scrub [options] <volume_name>");
        System.out.println("<volume_name> the volume to scrub\n");
        System.out.println("options:");

        utils.printOptions(options);
    }
}
