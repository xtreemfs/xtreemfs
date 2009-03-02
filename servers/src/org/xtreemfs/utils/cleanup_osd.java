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
 * AUTHORS: Felix Langner (ZIB)
 */
package org.xtreemfs.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.HttpErrorException;
import org.xtreemfs.common.clients.RPCClient;
import org.xtreemfs.common.clients.RPCResponse;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.clients.osd.ConcurrentFileMap;
import org.xtreemfs.common.clients.osd.OSDClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.utils.CLIParser.CliOption;

/**
 * <p>
 * OSD service function for the console. Cleans an OSD up, by eliminating zombie
 * files. Supports SSL connection.
 * </p>
 * 
 * @author langner
 * 
 */

public class cleanup_osd {
    private static final String   DEFAULT_DIR_CONFIG   = "/etc/xos/xtreemfs/default_dir";
    
    private static final String   DEFAULT_RESTORE_PATH = "lost+found";
    
    private static BufferedReader answers              = new BufferedReader(new InputStreamReader(System.in));
    
    private static DIRClient      dirClient;
    
    private static MRCClient      mrcClient;
    
    private static OSDClient      osdClient;

    private static RPCNIOSocketClient rpcClient;
    
    // generate authString
    private static String         authString;
    static {
        try {
            authString = NullAuthProvider.createAuthString("root", MRCClient.generateStringList("root"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws Exception {
        
        try {
            
            Logging.start(Logging.LEVEL_WARN);
            
            // parse the call arguments
            Map<String, CliOption> options = new HashMap<String, CliOption>();
            List<String> arguments = new ArrayList<String>(1);
            options.put("h", new CliOption(CliOption.OPTIONTYPE.SWITCH));
            options.put("v", new CliOption(CliOption.OPTIONTYPE.SWITCH));
            options.put("r", new CliOption(CliOption.OPTIONTYPE.SWITCH));
            options.put("e", new CliOption(CliOption.OPTIONTYPE.SWITCH));
            options.put("d", new CliOption(CliOption.OPTIONTYPE.URL));
            
            CLIParser.parseCLI(args, options, arguments);
            
            if (arguments.size() != 1 || options.get("h").switchValue != null) {
                usage();
                return;
            }
            
            InetSocketAddress osdAddr = null;
            boolean useSSL = false;
            String serviceCredsFile = null;
            String serviceCredsPass = null;
            String trustedCAsFile = null;
            String trustedCAsPass = null;
            SSLOptions sslOptions = null;
            URL dirURL = options.get("d").urlValue;
            boolean verbose = options.get("v").switchValue != null;
            boolean erase = options.get("e").switchValue != null;
            boolean restore = options.get("r").switchValue != null;



            // read default settings for the DIR
            if (dirURL == null) {
                DefaultDirConfig cfg = new DefaultDirConfig(DEFAULT_DIR_CONFIG);
                cfg.read();
                
                // load SSL options
                useSSL = cfg.isSslEnabled();
                serviceCredsFile = cfg.getServiceCredsFile();
                serviceCredsPass = cfg.getServiceCredsPassphrase();
                trustedCAsFile = cfg.getTrustedCertsFile();
                trustedCAsPass = cfg.getTrustedCertsPassphrase();
                sslOptions = useSSL ? new SSLOptions(new FileInputStream(serviceCredsFile), serviceCredsPass,
                    new FileInputStream(trustedCAsFile), trustedCAsPass) : null;

                rpcClient = new RPCNIOSocketClient(sslOptions, 10000, 5*60*1000);

                dirClient = new DIRClient(rpcClient,cfg.getDirectoryService());
            } else {
                rpcClient = new RPCNIOSocketClient(sslOptions, 10000, 5*60*1000);
                dirClient = new DIRClient(rpcClient,new InetSocketAddress(dirURL.getHost(), dirURL.getPort()));
            }

            rpcClient.start();
            rpcClient.waitForStartup();
            
            // read default settings for the OSD
            String osdUUID = null;
            String address = arguments.get(0);
            boolean isUUID = false;
            if (address.startsWith("uuid:")) {
                address = address.substring("uuid:".length());
                isUUID = true;
            }
            
            // resolve UUID if necessary
            if (!isUUID) {
                /*
                 * URL osdURL; try{ osdURL = new URL(address); }catch
                 * (MalformedURLException mue){
                 * System.out.println("The given address could not be resolved!"
                 * ); return; } osdAddr = new
                 * InetSocketAddress(osdURL.getHost(), osdURL.getPort());
                 */

                usage();
            } else {
                TimeSync.initialize(dirClient, 60000, 60000, authString);
                if (!UUIDResolver.isRunning()) {
                    UUIDResolver.start(dirClient, 1000, 1000);
                }
                ServiceUUID service = new ServiceUUID(address);
                service.resolve();
                osdAddr = service.getAddress();
                UUIDResolver.shutdown();
                // TimeSync.getInstance().shutdown();
                
                osdUUID = address;
            }
            
            // start cleanUp process
            osdClient = new OSDClient(600000, sslOptions);
            RPCResponse<?> response = null;
            ConcurrentFileMap fileList = null;
            System.out.println("The OSD will now be checked for 'zombie' files. \n"
                + "Depending on the speed of that OSD this check can take a few minutes...\n");
            try {
                Map<String, Map<String, Map<String, String>>> rsp = null;
                
                response = osdClient.cleanUp(osdAddr, authString);
                
                if (response == null
                    || (rsp = (Map<String, Map<String, Map<String, String>>>) response.get()) == null) {
                    osdClient.shutdown();
                    System.out.println("This OSD is clean.");
                    System.exit(0);
                }
                fileList = new ConcurrentFileMap(rsp);
            } catch (NumberFormatException nfe) {
                osdClient.shutdown();
                usage();
            } catch (IllegalArgumentException ia) {
                osdClient.shutdown();
                usage();
            } catch (Exception e) {
                System.out.println("Checking the OSD was not successful. Cause: " + e.getLocalizedMessage());
                e.printStackTrace();
                osdClient.shutdown();
                System.exit(1);
            } finally {
                if (response != null)
                    response.freeBuffers();
            }
            
            String empty1 = "                                       |";
            String empty = "                         |";
            String question;
            Long fileSize;
            String filePreview;
            
            // user interaction
            long totalZombiesSize = 0L;
            for (List<String> volume : fileList.keySetList()) {
                for (String file : fileList.getFileIDSet(volume)) {
                    totalZombiesSize += fileList.getFileSize(volume, file);
                }
            }
            
            // validate(fileList);
            
            if (fileList != null && fileList.size() != 0) {
                if (fileList.size() == 1)
                    System.out.println("There is one zombie on that OSD.");
                else
                    System.out.println("There are '" + fileList.size() + "' zombies with a total size of "
                        + totalZombiesSize + " bytes on that OSD. ");
                question = ("Do you want to list " + (fileList.size() == 1 ? "it" : "them") + "? [y/n]");
                verbose = (verbose) ? true : !requestUserDecision(question);
                if (!verbose) {
                    System.out
                            .println("VolumeID:FileNumber                    |File size in byte        |Preview");
                }
                for (List<String> volume : fileList.keySetList()) {
                    for (String file : fileList.getFileIDSet(volume)) {
                        Long fileNumber = Long.valueOf(file.substring(file.indexOf(":") + 1, file.length()));
                        
                        // get the file details
                        fileSize = fileList.getFileSize(volume, file);
                        filePreview = fileList.getFilePreview(volume, file);
                        
                        if (!verbose) {
                            String f = file + (volume.get(0).equals("unknown") ? "(unknown)" : "");
                            String out = f + empty1.substring(f.length(), empty1.length())
                                + empty.substring(0, empty.length() - (fileSize.toString().length() + 2))
                                + fileSize + " |" + filePreview;
                            
                            System.out.println(out);
                        }
                        if (!volume.get(0).equals("unknown") && !erase) {
                            question = ("Do you want to restore File: '" + file + "'? Otherwise it will be permanently deleted. [y/n]");
                            if ((restore) ? true : requestUserDecision(question)) {
                                if (mrcClient == null)
                                    mrcClient = new MRCClient();
                                try {
                                    mrcClient.restoreFile(new InetSocketAddress(volume.get(1), Integer
                                            .parseInt(volume.get(2))), DEFAULT_RESTORE_PATH, fileNumber,
                                        fileList.getFileSize(volume, file), null, authString, osdUUID,
                                        fileList.getObjectSize(volume, file), volume.get(0));
                                } catch (HttpErrorException he) {
                                    System.out.println(file + " could not be restored properly. Cause: "
                                        + he.getMessage());
                                }
                            } else {
                                if ((erase) ? true
                                    : requestUserDecision("Do you really want to delete that file? [y/n]")) {
                                    response = osdClient.cleanUpDelete(osdAddr, authString, file);
                                    try {
                                        response.waitForResponse(1000);
                                    } catch (HttpErrorException he) {
                                        System.out.println(file + " could not be deleted properly. Cause: "
                                            + he.getMessage());
                                    }
                                    if (response != null)
                                        response.freeBuffers();
                                }
                            }
                        } else {
                            if ((erase) ? true
                                : requestUserDecision("Do you really want to delete that file? [y/n]")) {
                                response = osdClient.cleanUpDelete(osdAddr, authString, file);
                                try {
                                    response.waitForResponse(1000);
                                } catch (HttpErrorException he) {
                                    System.out.println(file + " could not be deleted properly. Cause: "
                                        + he.getMessage());
                                }
                                if (response != null)
                                    response.freeBuffers();
                            }
                        }
                    }
                }
            } else
                System.out.println("\n There are no zombies on that OSD.");
            
        } finally {
            if (rpcClient != null)
                rpcClient.shutdown();
            if (osdClient != null)
                osdClient.shutdown();
            if (mrcClient != null)
                mrcClient.shutdown();
            TimeSync.getInstance().shutdown();
            System.out.println("done.");
        }
    }
    
    private static void usage() {
        System.out.println("usage: xtfs_cleanup [options] uuid:<osd_uuid>\n");
        System.out.println("              -h        show usage info");
        System.out.println("              -v        verbose");
        System.out.println("              -r        restore all potential zombies");
        System.out.println("              -e        !erase all potential zombies permanently!");
        System.out
                .println("              -d <dir_address>         directory service to use (e.g. 'http://localhost:32638')");
        System.out
                .println("If no DIR URI is specified, URI and security settings are taken from '/etc/xos/xtreemfs/default_dir'");
        System.exit(1);
    }
    
    private static boolean requestUserDecision(String question) {
        System.out.println(question);
        String answer;
        try {
            answer = answers.readLine();
            assert (answer != null && answer.length() > 0);
        } catch (IOException e) {
            System.out.println("Answer could not be read due an IO Exception.");
            return false;
        }
        return (answer.charAt(0) == 'y' || answer.charAt(0) == 'Y');
    }
    /*
     * @Deprecated private static void validate(ConcurrentFileMap fm) throws
     * IOException, ClassNotFoundException{ String path =
     * "/home/flangner/temp/database/"; String volID1 =
     * "0004760EDB9818CA9248215D00000001"; String volID2 =
     * "0004760EDB982F2A024949CA00000001"; String volID3 =
     * "0004760EDB984AECE148237D00000001"; String volID4 =
     * "0004760EDB9859CFDC4884D000000001"; String volID5 =
     * "0004760EDB986FD49148F20E00000001"; String volID6 =
     * "0004760EDB989891BD4774E200000001"; String volID7 =
     * "0004760EDB98B11FA0482ECD00000001"; String volID8 =
     * "0004760EDB98CDCDDC485C4600000001"; String fileName = "/mrcdb.1"; File f
     * = null;
     * 
     * for (List<String> volume : fm.keySetList()){
     * if(volume.get(0).equals(volID1.substring(0,volID1.length()-8))){ f = new
     * File(path+volID1+fileName); }else
     * if(volume.get(0).equals(volID2.substring(0,volID2.length()-8))){ f = new
     * File(path+volID2+fileName); }else
     * if(volume.get(0).equals(volID3.substring(0,volID3.length()-8))){ f = new
     * File(path+volID3+fileName); }else
     * if(volume.get(0).equals(volID4.substring(0,volID4.length()-8))){ f = new
     * File(path+volID4+fileName); }else
     * if(volume.get(0).equals(volID5.substring(0,volID5.length()-8))){ f = new
     * File(path+volID5+fileName); }else
     * if(volume.get(0).equals(volID6.substring(0,volID6.length()-8))){ f = new
     * File(path+volID6+fileName); }else
     * if(volume.get(0).equals(volID7.substring(0,volID7.length()-8))){ f = new
     * File(path+volID7+fileName); }else
     * if(volume.get(0).equals(volID8.substring(0,volID8.length()-8))){ f = new
     * File(path+volID8+fileName); }else if(volume.get(0).equals("unknown")){
     * continue; }else{
     * System.out.println("ERROR: Volume not found! Available VolIds are: ");
     * System.out.println(volID1.substring(0,volID1.length()-8));
     * System.out.println(volID2.substring(0,volID2.length()-8));
     * System.out.println(volID3.substring(0,volID3.length()-8));
     * System.out.println(volID4.substring(0,volID4.length()-8));
     * System.out.println(volID5.substring(0,volID5.length()-8));
     * System.out.println(volID6.substring(0,volID6.length()-8));
     * System.out.println(volID7.substring(0,volID7.length()-8));
     * System.out.println(volID8.substring(0,volID8.length()-8));
     * 
     * System.out.println("But requested was: "+volume.get(0)); break; }
     * FileInputStream fis = new FileInputStream(f); ObjectInputStream ois = new
     * ObjectInputStream(fis); Map<Long, FileEntity> fileMap = (TreeMap<Long,
     * FileEntity>) ois.readObject(); Set<Long> fileNumbers = fileMap.keySet();
     * 
     * for (String file : fm.getFileNumberSet(volume)){ if
     * (fileNumbers.contains(Long.valueOf(file))){
     * System.out.println("ERROR: "+volume.get(0)+":"+file+" is no zombie!"); }
     * }
     * 
     * ois.close(); fis.close(); } }
     */
}
