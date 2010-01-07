/*  Copyright (c) 2008,2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck(ZIB), Nele Andersen (ZIB)
 */
package org.xtreemfs.common.clients.simplescrubber;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.atomic.AtomicLong;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.simplescrubber.FileInfo.ReturnStatus;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.ONCRPCServiceURL;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.DIRInterface.DIRInterface;
import org.xtreemfs.interfaces.DirectoryEntry;
import org.xtreemfs.interfaces.DirectoryEntrySet;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.UserCredentials;
import org.xtreemfs.mrc.client.MRCClient;
import org.xtreemfs.utils.CLIParser;
import org.xtreemfs.utils.DefaultDirConfig;
import org.xtreemfs.utils.CLIParser.CliOption;

/**
 *
 * @author bjko
 */
public class Scrubber implements FileInfo.FileScrubbedListener {

    public static String latestScrubAttr = "scrubber.latestscrub";

    public static final UserCredentials credentials;

    static {
        StringSet groupIDs = new StringSet();
        groupIDs.add("root");
        credentials = new UserCredentials("root", groupIDs, "");
    }
    private static final int DEFAULT_NUM_THREADS = 10;

    private static final String DEFAULT_DIR_CONFIG = "/etc/xos/xtreemfs/default_dir";

    private final String volumeName;

    /*private final RPCNIOSocketClient rpcClient;

    private final DIRClient          dirClient;

    private final MRCClient          mrcClient;*/
    private final Client client;

    private Volume volume;

    private final boolean repair, delete, silent;

    private final ExecutorService tPool;

    private long lastBytesScrubbed;

    private final Stack<String> directories;

    private final Stack<String> files;

    private final Object completeLock;

    private int returnCode;

    private int numInFlight;

    private final int numThrs;

    private boolean hasFinished;

    private String currentDirName = null;

    private int    numFiles, numFailed, numUnreachable, numCorrectedFS, numDead;
    
    private final Set<String> removedOSDs;

    public Scrubber(Client client,
            String volume, int numThrs, boolean repair, boolean delete, boolean silent) throws IOException {
        /*this.rpcClient = rpcClient;
        this.dirClient = dirClient;
        this.mrcClient = mrcClient;*/
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
                System.out.println("WARNING: delete is enabled, corrupt files (data lost due to failed OSDs) will be deleted");
        }

        removedOSDs = new TreeSet();

        String status_removed = Integer.toString(Constants.SERVICE_STATUS_REMOVED);
        ServiceSet servs = client.getRegistry();
        for (Service serv : servs) {
            if ( (serv.getType() == ServiceType.SERVICE_TYPE_OSD) &&
                 serv.getData().get("status").equalsIgnoreCase(status_removed) ) {
                removedOSDs.add(serv.getUuid());
            }
        }

        if (removedOSDs.size() > 0) {
            System.out.println("list of OSDs that have been removed (replicas on these OSDs will be deleted):");
            for (String uuid : removedOSDs) {
                System.out.println("\t"+uuid);
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

        System.out.println("\nsummary:");
        System.out.println("files checked                         : "+numFiles);
        System.out.println("  of which had failures (correctable) : "+numFailed);
        System.out.println("  of which are corrupt (lost)         : "+numDead);
        System.out.println("  of which are unreachable            : "+numUnreachable);
        System.out.println("  of which had a wrong file size      : "+numCorrectedFS);
        System.out.println("bytes checked                         : "+OutputUtils.formatBytes(lastBytesScrubbed));

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
                            RandomAccessFile file = f.open("r", 0);
                            FileInfo fi = new FileInfo(file, this, removedOSDs, repair, delete);
                            tPool.submit(fi);
                            numInFlight++;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            finish(1);
                            return;
                        }
                    } catch (EmptyStackException ex) {
                        //fetch next dir
                        fetchNextDir();
                    }
                }
            }
        } catch (EmptyStackException ex) {
            //no more entries, finished!

            try {
                //mark volume as scrubbed
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
        
        //update statistics

        long total = 0;
        synchronized (directories) {
            lastBytesScrubbed += bytesScrubbed;
            numFiles++;
            switch (rs) {
                case CORRECTED_FILE_SIZE : numCorrectedFS++; break;
                case UNREACHABLE : numUnreachable++; break;
                case FILE_CORRUPT : numDead++; break;
            }
            if (!silent)
                System.out.format("scrubbed %-42s with %15s - total %15s\n\u001b[100D\u001b[A", file.getFileId(), OutputUtils.formatBytes(bytesScrubbed), OutputUtils.formatBytes(lastBytesScrubbed));
            numInFlight--;
            fillQueue();
        }


    }

    private void fetchNextDir() {
        currentDirName = directories.pop();

        try {
            DirectoryEntry[] ls = volume.listEntries(currentDirName);

            for (DirectoryEntry e : ls) {
                if ((e.getStbuf().getMode() & Constants.SYSTEM_V_FCNTL_H_S_IFREG) != 0) {
                    //regular file
                    files.push(currentDirName + e.getName());
                } else if ((e.getStbuf().getMode() & Constants.SYSTEM_V_FCNTL_H_S_IFDIR) != 0) {
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

        System.out.println("XtreemFS scrubber version " + VersionManagement.RELEASE_VERSION + " (file system data integrity check)\n");

        Map<String, CliOption> options = new HashMap<String, CliOption>();
        List<String> arguments = new ArrayList<String>(1);
        options.put("h", new CliOption(CliOption.OPTIONTYPE.SWITCH));
        CliOption oDir = new CliOption(CliOption.OPTIONTYPE.URL);
        oDir.urlDefaultPort = DIRInterface.ONCRPC_PORT_DEFAULT;
        oDir.urlDefaultProtocol = Constants.ONCRPC_SCHEME;
        options.put("dir", oDir);
        options.put("repair", new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put("delete", new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put("silent", new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put("thrs", new CliOption(CliOption.OPTIONTYPE.NUMBER));
        options.put("c", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("cpass", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("t", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("tpass", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("-help", new CliOption(CliOption.OPTIONTYPE.SWITCH));

        CLIParser.parseCLI(args, options, arguments);

        if (arguments.size() != 1 || options.get("h").switchValue == true || options.get("-help").switchValue == true) {
            usage();
            return;
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
        if (dirURL != null && (Constants.ONCRPCS_SCHEME.equals(dirURL.getProtocol()) || Constants.ONCRPCG_SCHEME.equals(dirURL.getProtocol()))) {
            useSSL = true;
            serviceCredsFile = options.get("c").stringValue;
            serviceCredsPass = options.get("cpass").stringValue;
            trustedCAsFile = options.get("t").stringValue;
            trustedCAsPass = options.get("tpass").stringValue;
            if (Constants.ONCRPCG_SCHEME.equals(dirURL.getProtocol())) {
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
                trustedCAsPass, SSLOptions.JKS_CONTAINER, false, gridSSL) : null;



        Client c = new Client(new InetSocketAddress[]{dirAddr}, 15 * 1000, 15 * 60 * 1000, sslOptions);
        c.start();

        int exitCode = 1;
        try {

            Scrubber scrubber = new Scrubber(c, volume, numThreads, repair, delete, silent);
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

    private static void usage() {
        System.out.println("usage: xtfs_scrub [options] <volume_name>");
        System.out.println("  -dir uri  directory service to use (e.g. 'oncrpc://localhost:32638')");
        System.out.println("            If no URI is specified, URI and security settings are taken from '" + DEFAULT_DIR_CONFIG + "'");
        System.out.println("            In case of a secured URI ('oncrpcs://...'), it is necessary to also specify SSL credentials:");
        System.out.println("              -c  <creds_file>         a PKCS#12 file containing user credentials");
        System.out.println("              -cpass <creds_passphrase>   a pass phrase to decrypt the the user credentials file");
        System.out.println("              -t  <trusted_CAs>        a PKCS#12 file containing a set of certificates from trusted CAs");
        System.out.println("              -tpass <trusted_passphrase> a pass phrase to decrypt the trusted CAs file");
        System.out.println("  -repair   repair files (update file size, replace replicas) when possible");
        System.out.println("  -delete   delete lost files (incomplete due to OSD failures)");
        System.out.println("  -silent   don't show the progress bar");
        System.out.println("  -thrs  n  number of concurrent file scrub threads (default=" + DEFAULT_NUM_THREADS + ")");
        System.out.println("  -h        show usage info");
    }
}
