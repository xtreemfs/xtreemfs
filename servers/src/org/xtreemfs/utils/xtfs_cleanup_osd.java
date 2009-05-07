/*  Copyright (c) 2009 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.osd.client.OSDClient;
import org.xtreemfs.utils.CLIParser.CliOption;

/**
 * Console-tool for the cleanUp-functionality of the XtreemFS OSD.
 *
 * 24.04.2009
 * @author flangner
 */
public class xtfs_cleanup_osd {
    private static final String   DEFAULT_DIR_CONFIG   = "config/dirconfig.properties";
    
    private static OSDClient osd;
    
    private static DIRClient dir;
    
    private static RPCNIOSocketClient dirClient;
    
    private static RPCNIOSocketClient osdClient;
    
    private static InetSocketAddress osdAddr;
    
    private static String password;
    
    /**
     * Main method.
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        try {
            Logging.start(Logging.LEVEL_ERROR);
        
            // parse the call arguments
            Map<String, CliOption> options = new HashMap<String, CliOption>();
            List<String> arguments = new ArrayList<String>(1);
            options.put("h", new CliOption(CliOption.OPTIONTYPE.SWITCH));
            options.put("r", new CliOption(CliOption.OPTIONTYPE.SWITCH));
            options.put("e", new CliOption(CliOption.OPTIONTYPE.SWITCH));
            options.put("deleteVolumes", new CliOption(CliOption.OPTIONTYPE.SWITCH));
            options.put("i", new CliOption(CliOption.OPTIONTYPE.SWITCH));
            options.put("stop", new CliOption(CliOption.OPTIONTYPE.SWITCH));
            options.put("p", new CliOption(CliOption.OPTIONTYPE.STRING));
            options.put("dir", new CliOption(CliOption.OPTIONTYPE.STRING));
            
            CLIParser.parseCLI(args, options, arguments);
            
            if (arguments.size() != 1 || options.get("h").switchValue) {
                usage();
                return;
            }
              
            boolean remove = options.get("e").switchValue;
            boolean restore = options.get("r").switchValue;
            boolean deleteVolumes = options.get("deleteVolumes").switchValue;
            boolean interactive = options.get("i").switchValue;
            boolean stop = options.get("stop").switchValue;
            String dirCfgPath = (options.get("dir").stringValue != null) ? options.get("dir").stringValue : DEFAULT_DIR_CONFIG; 
            password = (options.get("p").stringValue != null) ? options.get("p").stringValue : ""; 
             
            // read default settings for the OSD
            String osdUUID = null;
            if (arguments.get(0).startsWith("uuid:")) {
            osdUUID = arguments.get(0).substring("uuid:".length());
            } else {
                error("There was no UUID for the OSD given!");
            }
            
            // load SSL options
            SSLOptions sslOptions = null;
            DIRConfig cfg = null;
            try {
                cfg = new DIRConfig(dirCfgPath);
                cfg.read();
                if (cfg.isUsingSSL()) {
                    String serviceCredsFile = cfg.getServiceCredsFile();
                    String serviceCredsPass = cfg.getServiceCredsPassphrase();
                    String trustedCAsFile = cfg.getTrustedCertsFile();
                    String trustedCAsPass = cfg.getTrustedCertsPassphrase();
                    sslOptions = new SSLOptions(new FileInputStream(serviceCredsFile), serviceCredsPass,
                            new FileInputStream(trustedCAsFile), trustedCAsPass);
                }
            } catch (IOException e){
                error("Default DIR configuration not found!");
            }

            if (remove && restore) error("Zombies cannot be deleted and restored at the same time!");
            
            // connect to the OSD
            osdClient = new RPCNIOSocketClient(sslOptions, 10000, 5*60*1000);
            osdClient.start();
            osdClient.waitForStartup();
            osd = new OSDClient(osdClient);
            
            dirClient = new RPCNIOSocketClient(sslOptions, 10000, 5*60*1000);
            dirClient.start();
            dirClient.waitForStartup();
            dir = new DIRClient(dirClient,new InetSocketAddress(cfg.getAddress(),cfg.getPort()));
                         
            try{
                TimeSync.getInstance();
            }catch (RuntimeException re){
                TimeSync.initialize(dir, 60*1000, 30*1000);
            }
            
            if (!UUIDResolver.isRunning()) {
                UUIDResolver.start(dir, 1000, 1000);
            }
            ServiceUUID UUIDService = new ServiceUUID(osdUUID);
            UUIDService.resolve();
            osdAddr = UUIDService.getAddress();
            UUIDResolver.shutdown();
              
            try {
                if (stop) {
                    if (!isRunningCleanup()) error("No cleanup running on the given OSD."); 
                stop();
                for (String result : getResult()) {
                    System.out.println(result);
                }
                System.out.println("Cleanup stopped.");
            } else if (interactive) {
                if (isRunningCleanup()) stop();
                start(remove,deleteVolumes,restore);
                
                while (isRunningCleanup()) {
                    System.out.print(getState()+"\r");
                    Thread.sleep(3000);
                }
                System.out.println();
                for (String result : getResult()) {
                    System.out.println(result);
                }
                System.out.println("Cleanup done.");
            } else {
                if (isRunningCleanup()) stop();
                
                start(remove, deleteVolumes, restore);
                System.out.println("Cleanup is running.");
                }
            } catch (Exception e) {
                error("Operation could not be performed because: "+e.getMessage());
            } finally {
                dirClient.shutdown();
                osdClient.shutdown();
                dirClient.waitForShutdown();
                osdClient.waitForShutdown();
            }
        } catch (Exception e){
            error("Error: "+e.getMessage());
        }
        System.exit(0);
    }
    
    private static boolean isRunningCleanup() throws Exception{
        RPCResponse<Boolean> r = osd.internal_cleanup_is_running(osdAddr, password);
        try {
            return r.get();
        } catch (Exception e) {
            throw new Exception("Status-request for cleanup on the given OSD failed.");
        } finally {
            r.freeBuffers();
        }
    }
    
    private static void start(boolean remove, boolean deleteVolumes, boolean restore) throws Exception {        
        RPCResponse<?> r = osd.internal_cleanup_start(osdAddr, remove, deleteVolumes, restore, password);
        try {
            r.get();
        } catch (Exception e) {
            throw new Exception("Cleanup could not be started on the given OSD, because: "+e.getMessage());
        } finally {
            r.freeBuffers();
        }
    }
    
    private static void stop() throws Exception {        
        RPCResponse<?> r = osd.internal_cleanup_stop(osdAddr, password);
        try {
            r.get();
        } catch (Exception e) {
            throw new Exception("Cleanup could not be stopped on the given OSD, because: "+e.getMessage());
        } finally {
            r.freeBuffers();
        }
    }
    
    private static String getState() throws Exception {
        RPCResponse<String> r = osd.internal_cleanup_status(osdAddr, password);
        try {
            return r.get();
        } catch (Exception e) {
            throw new Exception("Cleanup could not be stopped on the given OSD, because: "+e.getMessage());
        } finally {
            r.freeBuffers();
        }
    }
    
    private static StringSet getResult() throws Exception {
        RPCResponse<StringSet> r = osd.internal_cleanup_get_result(osdAddr, password);
        try {
            return r.get();
        } catch (Exception e) {
            throw new Exception("Cleanup could not be stopped on the given OSD, because: "+e.getMessage());
        } finally {
            r.freeBuffers();
        }
    }
    
    /**
     * Prints the error <code>message</code> and delegates to usage().
     * @param message
     */
    private static void error(String message) {
        System.err.println(message);
        usage();
    }
        
    /**
     *  Prints out usage informations and terminates the application.
     */
    public static void usage(){
        System.out.println("xtfs_cleanup [options] uuid:<osd_uuid>\n");
        System.out.println("  "+"<osd_uuid> the unique identifier of the OSD to clean.");
        System.out.println("  "+"options:");        
        System.out.println("  "+"-h show these usage informations.");
        System.out.println("  "+"-r restore zombies found on the OSD.");
        System.out.println("  "+"-e erase potential zombies.");
        System.out.println("  "+"-deleteVolumes !dangerous! deletes volumes that might be dead.");
        System.out.println("  "+"-i interactive mode.");
        System.out.println("  "+"-stop suspend the currently running cleanup.");
        System.out.println("  "+"-p set the administrator password.");
        System.out.println("  "+"-dir set the path to the DIR configuration file.");
        System.exit(1);
    }
}
