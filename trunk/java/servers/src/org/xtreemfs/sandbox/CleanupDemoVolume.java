/*
 * Copyright (c) 2011 by Michael Berlin,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.sandbox;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.RandomAccessFile;
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
    private static Client          client;
    private static Volume          volume;
    private static UserCredentials user_credentials;
    private static List<String> example_files;

    public static boolean DeleteDirectoryRecursively(String directory_path) {
        boolean errors_occurred = false;

        try {
            DirectoryEntry[] dentries = volume.listEntries(directory_path, user_credentials);

            for (DirectoryEntry dentry : dentries) {
                String full_path = (directory_path.equals("/") ? "" : directory_path) + "/" + dentry.getName();
                if (dentry.getName().equals(".") || dentry.getName().equals("..")) {
                    continue;
                }
                boolean skip_entry = false;
                for (String file : example_files) {
                    if (full_path.equals("/" + new java.io.File(file).getName())) {
                        skip_entry = true;
                        break;
                    }
                }
                if (skip_entry) {
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
            System.out
                    .println("This binary does delete all files older than one hour at demo.xtreemfs.org/demo/. Run it with \"yesiknowwhatiamdoing\" - otherwise it does abort.");
            System.exit(1);
        }

        Logging.start(Logging.LEVEL_ERROR, Category.tool);

        user_credentials = UserCredentials.newBuilder().setUsername("root").addGroups("root").build();

        InetSocketAddress dirAddresses[] = new InetSocketAddress[] { new InetSocketAddress(
                "demo.xtreemfs.org", 32638) };
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
        
        // Get file names of example files.
        String example_files_dir = "/var/adm/xtreemfs/default_demo_files/";
        example_files = new ArrayList<String>();
        java.io.File dir = new java.io.File(example_files_dir);
        String[] files = dir.list();
        if (files != null) {
            for (String file : files) {
                example_files.add(example_files_dir + file);
            }
        }

        // Clean everything.
        boolean errors_occurred = DeleteDirectoryRecursively("/");

        // Copy example files.
        for (String filename : example_files) {
            java.io.File source_file = new java.io.File(filename);

            FileInputStream source = null;
            RandomAccessFile target = null;
            try {
                source = new FileInputStream(source_file);
                File file = volume.getFile("/" + source_file.getName(), user_credentials);
                target = file.open("rw", 292);
                byte[] buffer = new byte[128 * 1024];
                int bytesRead;

                while ((bytesRead = source.read(buffer)) != -1) {
                    target.write(buffer, 0, bytesRead);
                }
            } catch (FileNotFoundException e) {
                System.out.println("File " + filename + " does not exist.");
                e.printStackTrace();
                errors_occurred = true;
            } catch (IOException e) {
                System.out.println("An IOError occurred when copying the file " + filename + ".");
                e.printStackTrace();
                errors_occurred = true;
            } finally {
                if (source != null) {
                    try {
                        source.close();
                    } catch (IOException e) {
                        System.out.println("The source file " + filename + " could not be closed.");
                        e.printStackTrace();
                        errors_occurred = true;
                    }
                }
                if (target != null) {
                    try {
                        target.close();
                    } catch (IOException e) {
                        System.out.println("The target file /" + source_file.getName()
                                + " could not be closed.");
                        e.printStackTrace();
                        errors_occurred = true;
                    }
                }
            }
        }
        
        // Truncate example files to actual size and chmod them.
        for (String file : example_files) {
            String file_name = new java.io.File(file).getName();
            long file_size = new java.io.File(file).length();
            File xtreemfs_file = volume.getFile("/" + file_name, user_credentials);
            try {
                xtreemfs_file.chmod(292);
                RandomAccessFile xtreemfs_file_raf = xtreemfs_file.open("rw", 292);
                xtreemfs_file_raf.setLength(file_size);
            } catch (IOException e) {
                System.out.println("Failed to truncate /" + file_name
                        + " to the length " + file_size + ".");
                e.printStackTrace();
                errors_occurred = true;
            }
        }

        client.stop();

        if (errors_occurred) {
            System.out.println("Not all items could be deleted / files copied.");
            System.exit(3);
        }
    }
}
