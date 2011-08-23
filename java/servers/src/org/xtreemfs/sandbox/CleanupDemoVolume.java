/*
 * Copyright (c) 2011 by Michael Berlin,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.sandbox;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntry;

/**
 * 
 * Aug 22, 2011
 * 
 * @author mberlin
 */
public class CleanupDemoVolume {
    private static Client client;
    private static Volume volume;
    private static UserCredentials user_credentials;

    public static boolean DeleteDirectoryRecursively(String directory_path) {
        boolean errors_occurred = false;
        
        try {
            DirectoryEntry[] dentries = volume.listEntries(directory_path, user_credentials);

            for (DirectoryEntry dentry : dentries) {
                if (dentry.getName().equals(".") || dentry.getName().equals("..")) {
                    continue;
                }
                
                if ((dentry.getStbuf().getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFREG.getNumber()) != 0) {
                    // File.
                    try {
                        File file = volume.getFile(directory_path + "/" + dentry.getName(), user_credentials);
                        file.delete();
                    } catch (FileNotFoundException e) {
                        // No error for us.
                    }
                } else if ((dentry.getStbuf().getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFDIR.getNumber()) != 0) {
                    // Directory.
                    DeleteDirectoryRecursively(directory_path + "/" + dentry.getName());
                }
            }
        } catch (FileNotFoundException e) {
            // Not counted as error.
        } catch (IOException e) {
            e.printStackTrace();
            errors_occurred = true;
        }
        
        // Delete directory itself.
        if (!directory_path.equals("/")) {
            try {
                File file = volume.getFile(directory_path, user_credentials);
                // No special rmdir available.
                file.delete();
            } catch (FileNotFoundException e) {
                // Not counted as error.
            } catch (IOException e) {
                e.printStackTrace();
                errors_occurred = true;
            }
        }
        
        return errors_occurred;
    }

    public static void main(String[] args) {
        if (args.length < 1 || !args[0].equals("yesiknowwhatiamdoing")) {
            System.out.println("This binary does delete all files older than one hour at demo.xtreemfs.org/demo/. Run it with \"yesiknowwhatiamdoing\" - otherwise it does abort.");
            System.exit(1);
        }

        Logging.start(Logging.LEVEL_ERROR, Category.tool);

        user_credentials = UserCredentials.newBuilder()
            .setUsername("root")
            .addGroups("root").build();

        InetSocketAddress dirAddresses[] = new InetSocketAddress[] {
            new InetSocketAddress("demo.xtreemfs.org", 32638)};
        SSLOptions sslOptions = null;

            try {
                client = new Client(dirAddresses, 15000, 60000, sslOptions);
                client.start();
                volume = client.getVolume("demo", null);
            } catch (Exception e) {
                System.out.println("The client could not be started/the volume opened.");
                e.printStackTrace();
                System.exit(2);
            }

            boolean errors_occurred = DeleteDirectoryRecursively("/");

            client.stop();
            
            if (errors_occurred) {
                System.out.println("Not all items could be deleted.");
                System.exit(3);
            }
    }
}
