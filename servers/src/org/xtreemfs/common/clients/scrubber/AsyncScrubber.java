package org.xtreemfs.common.clients.scrubber;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.xtreemfs.common.Capability;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.RPCClient;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.clients.io.RandomAccessFile;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.pinky.HTTPHeaders;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.foundation.speedy.MultiSpeedy;
import org.xtreemfs.utils.CLIParser;
import org.xtreemfs.utils.DefaultDirConfig;
import org.xtreemfs.utils.CLIParser.CliOption;

public class AsyncScrubber {
    
    private static final int                   DEFAULT_NUM_CONS   = 10;
    
    private static final int                   DEFAULT_NUM_FILES  = 100;
    
    private static final String                DEFAULT_DIR_CONFIG = "/etc/xos/xtreemfs/default_dir";
    
    private static String                      authString;
    
    static {
        try {
            authString = NullAuthProvider.createAuthString("root", MRCClient
                    .generateStringList("root"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    private AtomicInteger                      returnCode;
    
    private long                               startTime;
    
    private long                               lastStatusPrint;
    
    private long                               lastBytes;
    
    private int                                filesRead;
    
    private int                                connectionsPerOSD;
    
    private boolean                            updateFileSize;
    
    private HashMap<ServiceUUID, OSDWorkQueue> osds;
    
    private final List<ScrubbedFile>           currentFiles, filesToRemove;
    
    private VolumeWalker                       volumeWalker;
    
    private MultiSpeedy                        speedy;
    
    private MRCClient                          mrcClient;
    
    private DIRClient                          dirClient;
    
    private InetSocketAddress                  mrcAddress;
    
    private Logger                             logger;
    
    public static String                       latestScrubAttr    = "scrubber.latestscrub";
    
    private Map<OSDWorkQueue, Long>            osdBytesMap        = new HashMap<OSDWorkQueue, Long>();
    
    private SSLOptions                         sslOptions;
    
    /**
     * @param sharedSpeedy
     * @param dirAddress
     *            the address of the directory service
     * @param mrcAddress
     *            the address of the mrc holding the volume
     * @param volumeName
     * @param updateFileSize
     *            true if the file size should be updated.
     * @throws JSONException
     *             thrown by createAuthString
     * @throws IOException
     *             thrown when creating a new MRCClient
     * @throws Exception
     *             thrown when creating a new VolumeWalker
     */
    public AsyncScrubber(final MultiSpeedy sharedSpeedy, InetSocketAddress dirAddress,
        InetSocketAddress mrcAddress, String volumeName, boolean updateFileSize,
        int connectionsPerOSD, int noFilesToFetch, SSLOptions ssl) throws Exception {
        this.connectionsPerOSD = connectionsPerOSD;
        this.updateFileSize = updateFileSize;
        this.speedy = sharedSpeedy;
        this.mrcAddress = mrcAddress;
        
        returnCode = new AtomicInteger(0);
        
        assert(sharedSpeedy != null);
        //dirClient = new DIRClient(sharedSpeedy, dirAddress);
        TimeSync.initialize(dirClient, 100000, 50, authString);
        
        mrcClient = new MRCClient(sharedSpeedy);
        //UUIDResolver.shutdown();
        //UUIDResolver.start(dirClient, 1000, 1000);
        
        volumeWalker = new VolumeWalker(volumeName, mrcAddress, noFilesToFetch, authString, ssl);
        
        currentFiles = new ArrayList<ScrubbedFile>();
        filesToRemove = new ArrayList<ScrubbedFile>();
        osds = new HashMap<ServiceUUID, OSDWorkQueue>();
        logger = new Logger(null);
        
        sslOptions = ssl;
    }
    
    public void shutdown() {
        speedy.shutdown();
        //dirClient.shutdown();
        mrcClient.shutdown();
        volumeWalker.shutdown();
        for (OSDWorkQueue que : osds.values())
            que.shutDown();
        
        //UUIDResolver.shutdown();
        //TimeSync.getInstance().shutdown();
    }
    
    public void waitForShutdown() {
        mrcClient.shutdown();
    }
    
    /**
     * Called by Main thread. Starts the scrubbing. Adds Files to the osd work
     * queues until all files in the volume has been scrubbed.
     * 
     * @throws Exception
     */
    public void start() throws Exception {
        startTime = System.currentTimeMillis();
        
        if (volumeWalker.hasNext()) {
            fillOSDs();
        }
        while (currentFiles.size() > 0 || volumeWalker.hasNext()) {
            fillOSDs();
        }
        logger.closeFileWriter();
        System.out.println("Done. Total time: " + (System.currentTimeMillis() - startTime) / 1000
            + " secs.");
    }
    
    /**
     * Called by Main thread. Prints the total number of files/bytes read and
     * the speed in KB/s. For each osd: prints the average connection speed in
     * KB/s and the number of idle connections.
     */
    private void printStatus() {
        long currentStatusPrint = System.currentTimeMillis();
        try {
            long bytes = 0;
            String msg = "";
            String osdDetails = "OSDs: ";
            
            for (OSDWorkQueue osd : osds.values()) {
                
                long osdBytes = osd.getTransferredBytes();
                
                Long lastOSDBytes = osdBytesMap.get(osd);
                if (lastOSDBytes == null)
                    lastOSDBytes = 0L;
                
                osdDetails += osd.getOSDId()
                    + ": "
                    + OutputUtils.formatBytes((osdBytes - lastOSDBytes) * 1000
                        / (currentStatusPrint - lastStatusPrint)) + "/s, "
                    + osd.getNumberOfIdleConnections() + " idle; ";
                bytes += osdBytes;
                osdBytesMap.put(osd, osdBytes);
            }
            
            msg += "#files scrubbed: "
                + filesRead
                + " ("
                + OutputUtils.formatBytes(bytes)
                + "), avrg. throughput: "
                + OutputUtils.formatBytes((bytes - lastBytes) * 1000
                    / (currentStatusPrint - lastStatusPrint)) + "/s, ";
            
            System.out.println(msg + osdDetails + "\u001b[100D\u001b[A");
            
            lastStatusPrint = currentStatusPrint;
            lastBytes = bytes;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Called by Main thread. Retrieves the next path from the VolumeWalker and
     * creates a RandomAccessFile of the file specified by the path. If no work
     * queue exists for an osd on which the file is stored a new queue is
     * created. The file is added to the list currentFiles.
     * 
     * @throws Exception
     *             thrown by hasNext
     */
    void addNextFileToCurrentFiles() throws Exception {
        if (volumeWalker.hasNext()) {
            String path = volumeWalker.removeNextFile();
            try {
                RandomAccessFile file = new RandomAccessFile("r", mrcAddress, path, speedy,
                    authString);
                for (ServiceUUID osdId : file.getOSDs()) {
                    // add new OSD to the scrubbing process
                    if (!osds.containsKey(osdId)) {
                        System.out.println("Adding OSD: " + osdId);
                        osds.put(osdId, new OSDWorkQueue(osdId, connectionsPerOSD,sslOptions));
                    }
                }
                synchronized (currentFiles) {
                    currentFiles.add(new ScrubbedFile(this, file));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Called by Main thread. Fills the osd work queues. If currentFiles does
     * not contain enough request for an osd new files are added to
     * currentFiles.
     * 
     * @throws Exception
     *             thrown by addNextFileToCurrentFiles
     */
    void fillOSDs() throws Exception {
        if (System.currentTimeMillis() - lastStatusPrint > 1000)
            printStatus();
        if (osds.isEmpty()) {
            addNextFileToCurrentFiles();
        }
        try {
            for (OSDWorkQueue osd : osds.values()) {
                fillQueue(osd);
                if (osd.getNumberOfIdleConnections() > 0) {
                    for (int i = 0; i < 10; i++) {
                        if (volumeWalker.hasNext()) {
                            addNextFileToCurrentFiles();
                        } else
                            break;
                    }
                    fillQueue(osd);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Called by Main thread. Fills the osd work queue specified by the
     * parameter osd.
     * 
     * @param osd
     */
    void fillQueue(OSDWorkQueue osd) {
        synchronized (currentFiles) {
            for (ScrubbedFile file : currentFiles) {
                while (true) { // get all possible reads for this osd
                    int objectNo = file.getRequestForOSD(osd);
                    if (objectNo == -1) {// no objects for this file
                        break;
                    }
                    if (!osd.readObjectAsync(file, objectNo))
                        return; // OSD has currently no idle connections,
                    // proceed with next OSD
                }
            }
        }
        for (ScrubbedFile file : filesToRemove) {
            currentFiles.remove(file);
        }
    }
    
    /**
     * @TODO setXattr last scrubber check setXattr(file,
     *       "xtreemfs-scrubber-lastcheck", now()) Called by MultiSpeedy or Main
     *       thread. Invoked when all objects of file has been successfully
     *       read. If result differs from the expected file size the
     *       inconsistency is logged and if updateFileSize is set to true, the
     *       filesize is updated. The file is removed from the list
     *       currentFiles.
     * @param file
     * @param result
     *            the number of bytes that has been read (the file size).
     */
    void fileFinished(ScrubbedFile file, long result, boolean isUnreadable) {
        // fileFinished can be called multiple times for a file when there are
        // outstanding requests. Cannot use remove(file) here, since it could
        // result in scrubber.shutdown() (when currentFiles.isEmpty) being
        // called before all updates are finished.
        boolean firstCall = currentFiles.contains(file);
        
        
        if (!firstCall) // do not output messages twice
            return;
        
        filesRead++;
        
        if (isUnreadable) {
            returnCode.set(2);
            logger.logError(file.getPath() + ": could not read from OSD, skipping file.");
        } else if (!(result == file.getExpectedFileSize())) {
            returnCode.compareAndSet(0, 1);
            if (updateFileSize == true) {
                try {
                    updateFileSize(file.getPath(), result);
                    logger.logError(file.getPath()
                        + ": file size in MRC is outdated, updated from "
                        + file.getExpectedFileSize() + " to " + result);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.logError(file.getPath() + ": Exception "
                        + "thrown while attempting to update file size");
                }
            } else {
                logger.logError(file.getPath() + " file size in MRC is outdated, was: "
                    + file.getExpectedFileSize() + ", found: " + result);
            }
        }
        
        try {
            setLastScrubAttr(file.getPath());
        } catch (Exception e) {
            e.printStackTrace();
            logger.logError(file.getPath() + ": Exception "
                + "thrown while attempting set lastScrub attribute");
        }
        // must be invoked after the updates have been made, because it sync.
        // with the main thread.
        filesToRemove.add(file);
    }
    
    /**
     * 
     * Called by MultiSpeedy or Main thread. Invoked when an object of file had
     * invalid checksum.
     * 
     * @param file
     */
    
    void foundInvalidChecksum(ScrubbedFile file, int objectNo) {
        returnCode.set(2);
        logger.logError(file.getPath() + ": object no. " + objectNo + " has invalid checksum.");
    }
    
    /**
     * Called by Multispeedy or Main thread. Updates the file size of the file
     * specified by path to newFileSize
     * 
     * @param path
     * @param newFileSize
     * @throws Exception
     */
    public void updateFileSize(String path, long newFileSize) throws Exception {
        Map<String, String> open = mrcClient.open(mrcAddress, path, "t", authString);
        String xcap = open.get(HTTPHeaders.HDR_XCAPABILITY);
        Capability capability = new Capability(xcap);
        String newFileSizeHeader = "[" + newFileSize + "," + capability.getEpochNo() + "]";
        mrcClient.updateFileSize(mrcAddress, xcap, newFileSizeHeader, authString);
    }
    
    void setLastScrubAttr(String path) throws Exception {
        Map<String, Object> newXAttr = new HashMap<String, Object>();
        long time = System.currentTimeMillis();
        newXAttr.put(latestScrubAttr, String.valueOf(time));
        mrcClient.setXAttrs(mrcAddress, path, newXAttr, authString);
        volumeWalker.fileOrDirScrubbed(path, time);
    }
    
    public void enableLogfile(String filename) {
        logger = new Logger(filename);
    }
    
    public int getReturnCode() {
        return returnCode.get();
    }
    
    public static void main(String[] args) throws Exception {
        
        Logging.start(Logging.LEVEL_WARN);
        
        Map<String, CliOption> options = new HashMap<String, CliOption>();
        List<String> arguments = new ArrayList<String>(1);
        options.put("h", new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put("dir", new CliOption(CliOption.OPTIONTYPE.URL));
        options.put("chk", new CliOption(CliOption.OPTIONTYPE.SWITCH));
        options.put("cons", new CliOption(CliOption.OPTIONTYPE.NUMBER));
        options.put("files", new CliOption(CliOption.OPTIONTYPE.NUMBER));
        options.put("c", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("cp", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("t", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("tp", new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put("h", new CliOption(CliOption.OPTIONTYPE.SWITCH));
        
        CLIParser.parseCLI(args, options, arguments);
        
        if (arguments.size() != 1 || options.get("h").switchValue != null) {
            usage();
            return;
        }
        
        InetSocketAddress dirAddr = null;
        boolean useSSL = false;
        String serviceCredsFile = null;
        String serviceCredsPass = null;
        String trustedCAsFile = null;
        String trustedCAsPass = null;
        
        URL dirURL = options.get("dir").urlValue;
        
        // parse security info if protocol is 'https'
        if (dirURL != null && "https".equals(dirURL.getProtocol())) {
            useSSL = true;
            serviceCredsFile = options.get("c").stringValue;
            serviceCredsPass = options.get("cp").stringValue;
            trustedCAsFile = options.get("t").stringValue;
            trustedCAsPass = options.get("tp").stringValue;
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
        } else
            dirAddr = new InetSocketAddress(dirURL.getHost(), dirURL.getPort());
        
        boolean checkOnly = options.get("chk").switchValue != null;
        
        int noConnectionsPerOSD = DEFAULT_NUM_CONS;
        if (options.get("cons").numValue != null)
            noConnectionsPerOSD = options.get("cons").numValue.intValue();
        
        int noFilesToFetch = DEFAULT_NUM_FILES;
        if (options.get("files").numValue != null)
            noFilesToFetch = options.get("files").numValue.intValue();
        
        String volume = arguments.get(0);
        boolean isVolUUID = false;
        if (volume.startsWith("uuid:")) {
            volume = volume.substring("uuid:".length());
            isVolUUID = true;
        }
        
        SSLOptions sslOptions = useSSL ? new SSLOptions(serviceCredsFile, serviceCredsPass,
                SSLOptions.PKCS12_CONTAINER,
            trustedCAsFile, trustedCAsPass, SSLOptions.JKS_CONTAINER, false) : null;
        
        // resolve volume MRC
        Map<String, Object> query = RPCClient.generateMap(isVolUUID ? "uuid" : "name", volume);
        DIRClient dirClient = new DIRClient(dirAddr, sslOptions, RPCClient.DEFAULT_TIMEOUT);
        TimeSync.initialize(dirClient, 100000, 50, authString);
        
        RPCResponse<Map<String, Map<String, Object>>> resp = dirClient.getEntities(query, RPCClient
                .generateStringList("mrc", "name"), authString);
        Map<String, Map<String, Object>> result = resp.get();
        resp.freeBuffers();
        
        
        if (result.isEmpty()) {
            System.err.println("volume '" + arguments.get(0)
                + "' could not be found at Directory Service '" + dirURL + "'");
            System.exit(3);
        }
        Map<String, Object> volMap = result.values().iterator().next();
        String mrc = (String) volMap.get("mrc");
        volume = (String) volMap.get("name");
        
        UUIDResolver.start(dirClient, 60*60, 10*60*60);
        
        ServiceUUID mrcUUID = new ServiceUUID(mrc);
        InetSocketAddress mrcAddress = mrcUUID.getAddress();
        
        try {
            
            MultiSpeedy speedy = new MultiSpeedy(sslOptions);
            speedy.start();
            AsyncScrubber scrubber = new AsyncScrubber(speedy, dirAddr, mrcAddress, volume,
                !checkOnly, noConnectionsPerOSD, noFilesToFetch,sslOptions);
            
            scrubber.start();
            scrubber.shutdown();
            System.exit(scrubber.getReturnCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        TimeSync.close();
        UUIDResolver.shutdown();
        dirClient.shutdown();
       
    }
    
    private static void usage() {
        System.out.println("usage: xtfs_scrub [options] <volume_name> | uuid:<volume_uuid>");
        System.out.println("  -dir uri  directory service to use (e.g. 'http://localhost:32638')");
        System.out
                .println("            If no URI is specified, URI and security settings are taken from '"
                    + DEFAULT_DIR_CONFIG + "'");
        System.out
                .println("            In case of a secured URI ('https://...'), it is necessary to also specify SSL credentials:");
        System.out
                .println("              -c  <creds_file>         a PKCS#12 file containing user credentials");
        System.out
                .println("              -cp <creds_passphrase>   a pass phrase to decrypt the the user credentials file");
        System.out
                .println("              -t  <trusted_CAs>        a PKCS#12 file containing a set of certificates from trusted CAs");
        System.out
                .println("              -tp <trusted_passphrase> a pass phrase to decrypt the trusted CAs file");
        System.out
                .println("  -chk      check only (do not update file sizes on the MRC in case of inconsistencies)");
        System.out.println("  -cons  n  number of connections per OSD (default=" + DEFAULT_NUM_CONS
            + ")");
        System.out.println("  -files n  number of files to fetch at once from MRC (default="
            + DEFAULT_NUM_FILES + ")");
        System.out.println("  -h        show usage info");
    }
}
