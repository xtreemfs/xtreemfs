/*
 * Copyright (c) 2009-2009 by Bjoern Kolbeck, Nele Andersen,
 *                            Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.InvalidChecksumException;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.Replica;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.VersionManagement;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.CLIParser;
import org.xtreemfs.foundation.util.ONCRPCServiceURL;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.foundation.util.CLIParser.CliOption;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.PORTS;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.REPL_FLAG;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntry;

/**
 * 
 * @author bjko
 */
public class xtfs_scrub {
    
    public static interface FileScrubbedListener {
        public void fileScrubbed(RandomAccessFile file, long bytesScrubbed, ReturnStatus rstatus);
    }
    
    public static enum ReturnStatus {
        FILE_OK, CORRECTED_FILE_SIZE, REPLACED_OBJECTS, REPLACED_REPLICAS, UNREACHABLE, FILE_CORRUPT
    };
    
    public static class FileInfo implements Runnable {
        
        private final RandomAccessFile     file;
        
        private final FileScrubbedListener listener;
        
        private long                       nextObjectToScrub;
        
        private long                       byteCounter;
        
        private final Set<String>          removedOSDs;
        
        private final boolean              repair, delete;
        
        public FileInfo(RandomAccessFile file, FileScrubbedListener listener, Set<String> removedOSDs,
            boolean repair, boolean delete) {
            this.file = file;
            this.listener = listener;
            
            nextObjectToScrub = 0;
            byteCounter = 0;
            this.removedOSDs = removedOSDs;
            this.repair = repair;
            this.delete = delete;
        }
        
        public void run() {
            try {
                if (file.isReadOnly()) {
                    // check replicas...
                    Replica[] replicas = null;
                    long numObjs = 0;
                    try {
                        replicas = file.getFile().getReplicas();
                        numObjs = file.getNumObjects();
                    } catch (IOException ex) {
                        printFileErrorMessage(file.getFile(), file.getFileId(),
                            "cannot read file size from MRC: " + ex);
                        listener.fileScrubbed(file, byteCounter, ReturnStatus.UNREACHABLE);
                        return;
                    }
                    // read all replicas
                    int numFullReplRm = 0;
                    int numPartialReplRm = 0;
                    int numComplete = 0;
                    for (int r = 0; r < replicas.length; r++) {
                        Replica replica = replicas[r];
                        // check if an OSD was removed
                        boolean replDeleted = false;
                        for (int o = 0; o < replica.getStripeWidth(); o++) {
                            if (removedOSDs.contains(replica.getOSDUuid(o))) {
                                // FIXME: remove
                                if (repair) {
                                    try {
                                        replica.removeReplica(false);
                                        replDeleted = true;
                                    } catch (Exception ex) {
                                    }
                                }
                                if (replica.isFullReplica()) {
                                    numFullReplRm++;
                                } else {
                                    numPartialReplRm++;
                                }
                                break;
                            } else {
                                try {
                                    if (replica.isFullReplica() && replica.isCompleteReplica())
                                        numComplete++;
                                } catch (Exception ex) {
                                }
                            }
                        }
                        if (numPartialReplRm > 0) {
                            printFileErrorMessage(file.getFile(), file.getFileId(), "file has "
                                + numPartialReplRm + " dead partial replicas (non recoverable)");
                        }
                        if (replDeleted)
                            continue;
                        file.forceReplica(replica.getOSDUuid(0));
                        
                        for (long o = 0; o < numObjs; o++) {
                            try {
                                int objSize = file.checkObject(o);
                                byteCounter += objSize;
                            } catch (InvalidChecksumException ex) {
                                printFileErrorMessage(file.getFile(), file.getFileId(), "object #" + o
                                    + " has an invalid checksum on OSD " + " of replica " + r + ": " + ex);
                                continue;
                            } catch (IOException ex) {
                                printFileErrorMessage(file.getFile(), file.getFileId(),
                                    "unable to check object #" + o + " of replica " + r + ": " + ex);
                                break;
                            }
                        }
                    }
                    if (numFullReplRm > 0) {
                        // FIXME: create new replicas
                        if (repair) {
                            System.out.println("creating new replicas:");
                            // add replicas
                            int numNewReplicas = 0;
                            while (numNewReplicas < numFullReplRm) {
                                try {
                                    String[] osds = file.getFile().getSuitableOSDs(1);
                                    if (osds.length == 0) {
                                        printFileErrorMessage(file.getFile(), file.getFileId(),
                                            "cannot create new replica, not enough OSDs available");
                                        break;
                                    }
                                    file.getFile().addReplica(1, osds,
                                        REPL_FLAG.REPL_FLAG_FULL_REPLICA.getNumber());
                                    numNewReplicas++;
                                } catch (Exception ex) {
                                    printFileErrorMessage(file.getFile(), file.getFileId(),
                                        "cannot create new replica: " + ex);
                                    break;
                                }
                            }
                            if (numNewReplicas == numFullReplRm) {
                                printFileErrorMessage(file.getFile(), file.getFileId(), "lost "
                                    + numFullReplRm + " replicas due to dead OSDs. Created " + numFullReplRm
                                    + " new replicas.");
                            } else {
                                printFileErrorMessage(file.getFile(), file.getFileId(), "lost "
                                    + numFullReplRm + " replicas due to dead OSDs. Could only create "
                                    + numNewReplicas
                                    + " due to a lack of suitable OSDs or communication errors.");
                            }
                        } else {
                            printFileErrorMessage(file.getFile(), file.getFileId(), "lost " + numFullReplRm
                                + " replicas due to dead OSDs");
                        }
                    }
                    
                } else {
                    boolean eof = false;
                    do {
                        try {
                            
                            int objSize = file.checkObject(nextObjectToScrub++);
                            if (objSize < file.getCurrentReplicaStripeSize()) {
                                eof = true;
                            }
                            byteCounter += objSize;
                        } catch (InvalidChecksumException ex) {
                            printFileErrorMessage(file.getFile(), file.getFileId(), "object #"
                                + (nextObjectToScrub - 1) + " has an invalid checksum on OSD: " + ex);
                            listener.fileScrubbed(file, byteCounter, ReturnStatus.FILE_CORRUPT);
                        } catch (IOException ex) {
                            // check if there is a dead OSD...
                            boolean isDead = false;
                            for (String uuid : file.getLocationsList().getReplicas(0).getOsdUuidsList()) {
                                if (removedOSDs.contains(uuid)) {
                                    isDead = true;
                                }
                            }
                            if (isDead) {
                                String errMsg = "file data was stored on removed OSD. File is lost.";
                                if (delete) {
                                    errMsg = "file data was stored on removed OSD. File was deleted.";
                                    try {
                                        file.getFile().delete();
                                    } catch (Exception ex2) {
                                    }
                                }
                                printFileErrorMessage(file.getFile(), file.getFileId(), errMsg);
                                listener.fileScrubbed(file, byteCounter, ReturnStatus.FILE_CORRUPT);
                            } else {
                                printFileErrorMessage(file.getFile(), file.getFileId(),
                                    "unable to check object #" + (nextObjectToScrub - 1) + ": " + ex);
                                ex.printStackTrace();
                                listener.fileScrubbed(file, byteCounter, ReturnStatus.UNREACHABLE);
                            }
                            return;
                        }
                    } while (!eof);
                    
                    try {
                        long mrcFileSize = file.length();
                        if (!file.isReadOnly() && (byteCounter != mrcFileSize)) {
                            if (repair) {
                                file.forceFileSize(byteCounter);
                                printFileErrorMessage(file.getFile(), file.getFileId(),
                                    "corrected file size from " + mrcFileSize + " to " + byteCounter
                                        + " bytes");
                                listener.fileScrubbed(file, byteCounter, ReturnStatus.CORRECTED_FILE_SIZE);
                                return;
                            } else {
                                printFileErrorMessage(file.getFile(), file.getFileId(),
                                    "incorrect file size: is " + mrcFileSize + " but should be "
                                        + byteCounter + " bytes");
                            }
                        }
                        
                    } catch (IOException ex) {
                        printFileErrorMessage(file.getFile(), file.getFileId(),
                            "unable to read file size from MRC: " + ex);
                        listener.fileScrubbed(file, byteCounter, ReturnStatus.UNREACHABLE);
                        return;
                    }
                }
                listener.fileScrubbed(file, byteCounter, ReturnStatus.FILE_OK);
            } catch (Throwable th) {
                printFileErrorMessage(file.getFile(), file.getFileId(),
                    "unable to check file due to unexpected error: " + th);
                listener.fileScrubbed(file, byteCounter, ReturnStatus.UNREACHABLE);
            }
        }
        
        public static void printFileErrorMessage(File f, String fileId, String error) {
            System.err.format("file '%s' (%s):\n\t%s\n", f.getPath(), fileId, error);
        }
        
    }
    
    public static String                latestScrubAttr     = "scrubber.latestscrub";
    
    public static final UserCredentials credentials;
    
    static {
        credentials = UserCredentials.newBuilder().setUsername("root").addGroups("root").build();
    }
    
    private static final int            DEFAULT_NUM_THREADS = 10;
    
    private static final String         DEFAULT_DIR_CONFIG  = "/etc/xos/xtreemfs/default_dir";
    
    private final String                volumeName;
    
    /*
     * private final RPCNIOSocketClient rpcClient;
     * 
     * private final DIRClient dirClient;
     * 
     * private final MRCClient mrcClient;
     */
    private final Client                client;
    
    private Volume                      volume;
    
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
    
    private String                      currentDirName      = null;
    
    private int                         numFiles, numFailed, numUnreachable, numCorrectedFS, numDead;
    
    private final Set<String>           removedOSDs;
    
    public xtfs_scrub(Client client, String volume, int numThrs, boolean repair, boolean delete,
        boolean silent) throws IOException {
        /*
         * this.rpcClient = rpcClient; this.dirClient = dirClient;
         * this.mrcClient = mrcClient;
         */
        this.volumeName = volume;
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
        
        this.client = client;
        this.volume = client.getVolume(volumeName, credentials);
        
        numFiles = 0;
        numFailed = 0;
        numUnreachable = 0;
        
        if (!repair && !delete) {
            System.out.println("running in check mode, no changes to files will be made");
        } else {
            System.out.println("running in repair mode");
            if (delete)
                System.out
                        .println("WARNING: delete is enabled, corrupt files (data lost due to failed OSDs) will be deleted");
        }
        
        removedOSDs = new TreeSet();
        
        String status_removed = Integer.toString(ServiceStatus.SERVICE_STATUS_REMOVED.getNumber());
        ServiceSet servs = client.getRegistry();
        for (Service serv : servs.getServicesList()) {
            String hbAttr = KeyValuePairs.getValue(serv.getData().getDataList(), HeartbeatThread.STATUS_ATTR);
            if ((serv.getType() == ServiceType.SERVICE_TYPE_OSD) && (hbAttr != null)
                && hbAttr.equalsIgnoreCase(status_removed)) {
                removedOSDs.add(serv.getUuid());
            }
        }
        
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
        fillQueue();
        synchronized (completeLock) {
            try {
                if (!hasFinished) {
                    completeLock.wait();
                }
            } catch (InterruptedException ex) {
            }
        }
        tPool.shutdownNow();
        
        if (!silent)
            System.out.format("scrubbed %-42s      %15s - total %15s\n\u001b[100D\u001b[A", "all files", "",
                OutputUtils.formatBytes(lastBytesScrubbed));
        
        System.out.println("\n\nsummary:");
        System.out.println("files checked                         : " + numFiles);
        System.out.println("  of which had failures (correctable) : " + numFailed);
        System.out.println("  of which are corrupt (lost)         : " + numDead);
        System.out.println("  of which are unreachable            : " + numUnreachable);
        System.out.println("  of which had a wrong file size      : " + numCorrectedFS);
        System.out.println("bytes checked                         : "
            + OutputUtils.formatBytes(lastBytesScrubbed));
        
        return returnCode;
    }
    
    private void fillQueue() {
        try {
            synchronized (directories) {
                while (numInFlight < numThrs) {
                    try {
                        String filename = files.pop();
                        try {
                            File f = volume.getFile(filename);
                            RandomAccessFile file = f.open("rw", 0);
                            
                            FileScrubbedListener fsListener = new FileScrubbedListener() {
                                public void fileScrubbed(RandomAccessFile file, long bytesScrubbed,
                                    org.xtreemfs.utils.xtfs_scrub.ReturnStatus rstatus) {
                                    xtfs_scrub.this.fileScrubbed(file, bytesScrubbed, rstatus);
                                }
                            };
                            
                            FileInfo fi = new FileInfo(file, fsListener, removedOSDs, repair, delete);
                            tPool.submit(fi);
                            numInFlight++;
                        } catch (Exception ex) {
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
            
            try {
                // mark volume as scrubbed
                File root = volume.getFile("/");
                root.setxattr(latestScrubAttr, Long.toString(TimeSync.getLocalSystemTime()));
            } catch (Exception ex2) {
                System.out.println("\nWarning: cannot mark volume as successfully scrubbed: " + ex2);
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
    
    public void fileScrubbed(RandomAccessFile file, long bytesScrubbed, ReturnStatus rs) {
        
        // update statistics
        
        long total = 0;
        String fname = file.getFile().getPath();
        if (fname.length() > 42) {
            fname = "..." + fname.substring(fname.length() - 39, fname.length());
        }
        synchronized (directories) {
            lastBytesScrubbed += bytesScrubbed;
            numFiles++;
            switch (rs) {
            case CORRECTED_FILE_SIZE:
                numCorrectedFS++;
                break;
            case UNREACHABLE:
                numUnreachable++;
                break;
            case FILE_CORRUPT:
                numDead++;
                break;
            }
            if (!silent)
                System.out.format("scrubbed %-42s with %15s - total %15s\n\u001b[100D\u001b[A", fname,
                    OutputUtils.formatBytes(bytesScrubbed), OutputUtils.formatBytes(lastBytesScrubbed));
            numInFlight--;
            fillQueue();
        }
        
    }
    
    private void fetchNextDir() {
        currentDirName = directories.pop();
        
        try {
            DirectoryEntry[] ls = volume.listEntries(currentDirName);
            
            if (ls == null) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, "path %s does not exist!", currentDirName);
                return;
            }
            
            for (DirectoryEntry e : ls) {
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
    
    public static void main(String[] args) throws Exception {
        
        Logging.start(Logging.LEVEL_WARN);
        
        TimeSync.initializeLocal(60000, 50);
        
        System.out.println("XtreemFS scrubber version " + VersionManagement.RELEASE_VERSION
            + " (file system data integrity check)\n");
        
        Map<String, CliOption> options = utils.getDefaultAdminToolOptions(false);
        List<String> arguments = new ArrayList<String>(1);
        CliOption oDir = new CliOption(
            CliOption.OPTIONTYPE.URL,
            "directory service to use (e.g. 'pbrpc://localhost:32638'). If no URI is specified, URI and security settings are taken from '"
                + DEFAULT_DIR_CONFIG + "'", "<uri>");
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
        
        InetSocketAddress dirAddr = null;
        boolean useSSL = false;
        boolean gridSSL = false;
        String serviceCredsFile = null;
        String serviceCredsPass = null;
        String trustedCAsFile = null;
        String trustedCAsPass = null;
        
        ONCRPCServiceURL dirURL = options.get("dir").urlValue;
        
        // parse security info if protocol is 'https'
        if (dirURL != null
            && (Schemes.SCHEME_PBRPCS.equals(dirURL.getProtocol()) || Schemes.SCHEME_PBRPCG.equals(dirURL
                    .getProtocol()))) {
            useSSL = true;
            if ((options.get("c").stringValue == null) || (options.get("cpass").stringValue == null)
                || (options.get("t").stringValue == null) || (options.get("tpass").stringValue == null)) {
                System.err.println("Please pass a client certificate (-" + utils.OPTION_USER_CREDS_FILE
                    + " -" + utils.OPTION_USER_CREDS_PASS + ") and truststore (-"
                    + utils.OPTION_TRUSTSTORE_FILE + " -" + utils.OPTION_TRUSTSTORE_PASS + ")");
                System.exit(1);
            }
            serviceCredsFile = options.get(utils.OPTION_USER_CREDS_FILE).stringValue;
            serviceCredsPass = options.get(utils.OPTION_USER_CREDS_PASS).stringValue;
            trustedCAsFile = options.get(utils.OPTION_TRUSTSTORE_FILE).stringValue;
            trustedCAsPass = options.get(utils.OPTION_TRUSTSTORE_PASS).stringValue;
            if (Schemes.SCHEME_PBRPCG.equals(dirURL.getProtocol())) {
                gridSSL = true;
            }
        }
        
        // read default settings
        if (dirURL == null) {
            
            DefaultDirConfig cfg = new DefaultDirConfig(DEFAULT_DIR_CONFIG);
            cfg.read();
            
            dirAddr = cfg.getDirectoryService();
            useSSL = cfg.isSslEnabled();
            serviceCredsFile = cfg.getServiceCredsFile();
            serviceCredsPass = cfg.getServiceCredsPassphrase();
            trustedCAsFile = cfg.getTrustedCertsFile();
            trustedCAsPass = cfg.getTrustedCertsPassphrase();
        } else {
            dirAddr = new InetSocketAddress(dirURL.getHost(), dirURL.getPort());
        }
        
        boolean repair = options.get("repair").switchValue;
        boolean silent = options.get("silent").switchValue;
        boolean delete = options.get("delete").switchValue;
        
        int numThreads = DEFAULT_NUM_THREADS;
        if (options.get("thrs").numValue != null) {
            numThreads = options.get("thrs").numValue.intValue();
        }
        
        final String volume = arguments.get(0);
        
        SSLOptions sslOptions = useSSL ? new SSLOptions(new FileInputStream(serviceCredsFile),
            serviceCredsPass, SSLOptions.PKCS12_CONTAINER, new FileInputStream(trustedCAsFile),
            trustedCAsPass, SSLOptions.JKS_CONTAINER, false, gridSSL, null) : null;
        
        Client c = new Client(new InetSocketAddress[] { dirAddr }, 15 * 1000, 15 * 60 * 1000, sslOptions);
        c.start();
        
        int exitCode = 1;
        try {
            
            xtfs_scrub scrubber = new xtfs_scrub(c, volume, numThreads, repair, delete, silent);
            exitCode = scrubber.scrub();
            if (exitCode == 0) {
                System.out.println("\n\nsuccessfully scrubbed volume '" + volume + "'");
            } else {
                System.out.println("\n\nscrubbing volume '" + volume + "' FAILED!");
            }
            
            System.exit(exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        TimeSync.close();
        c.stop();
        
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
