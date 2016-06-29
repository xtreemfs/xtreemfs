/*
 * Copyright (c) 2012 by Lukas Kairies, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.sandbox;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.FileHandle;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.DirectoryEntry;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Setattrs;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;

public class CleanupDemoVolume {
    private static Client          client;
    private static Volume          volume;
    private static UserCredentials userCredentials;
    private static List<String>    exampleFiles;

    public static boolean DeleteDirectoryRecursively(String directoryPath) {
        boolean errorsOccurred = false;

        try {
            List<DirectoryEntry> dentries = volume.readDir(userCredentials, directoryPath, 0, 0, false)
                    .getEntriesList();
            for (DirectoryEntry dentry : dentries) {
                String fullPath = (directoryPath.equals("/") ? "" : directoryPath) + "/" + dentry.getName();
                if (dentry.getName().equals(".") || dentry.getName().equals("..")) {
                    continue;
                }
                boolean skipEntry = false;
                for (String file : exampleFiles) {
                    if (fullPath.equals("/" + new java.io.File(file).getName())) {
                        skipEntry = true;
                        break;
                    }
                }
                if (skipEntry) {
                    continue;
                }

                if ((dentry.getStbuf().getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFREG.getNumber()) != 0) {
                    // File.
                    try {
                        volume.unlink(userCredentials, fullPath);
                    } catch (IOException e) {
                        // No error for us.
                    }
                } else if ((dentry.getStbuf().getMode() & SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_S_IFDIR.getNumber()) != 0) {
                    // Directory.
                    DeleteDirectoryRecursively(fullPath);
                }
            }
        } catch (FileNotFoundException e) {
            // Not counted as error.
        } catch (IOException e) {
            e.printStackTrace();
            errorsOccurred = true;
        }

        // Delete directory itself.
        if (!directoryPath.equals("/")) {
            try {
                volume.removeDirectory(userCredentials, directoryPath);
            } catch (IOException e) {
                e.printStackTrace();
                errorsOccurred = true;
            }
        }

        return errorsOccurred;
    }

    public static void main(String[] args) {
        if (args.length < 1 || !args[0].equals("yesiknowwhatiamdoing")) {
            System.out
                    .println("This binary does delete all files older than one hour at demo.xtreemfs.org/demo/. Run it with \"yesiknowwhatiamdoing\" - otherwise it does abort.");
            System.exit(1);
        }

        Logging.start(Logging.LEVEL_ERROR, Category.tool);

        userCredentials = UserCredentials.newBuilder().setUsername("root").addGroups("root").build();

        String dirAddress = "demo.xtreemfs.org:32638";

        SSLOptions sslOptions = null;

        Options options = new Options();

        try {
            client = ClientFactory.createClient(dirAddress, userCredentials, sslOptions, options);
            client.start();
            volume = client.openVolume("demo", sslOptions, options);
        } catch (Exception e) {
            System.out.println("The client could not be started/the volume opened.");
            e.printStackTrace();
            System.exit(2);
        }

        // Get file names of example files.
        String exampleFilesDir = "/var/adm/xtreemfs/default_demo_files/";
        java.io.File dir = new java.io.File(exampleFilesDir);
        String[] files = dir.list();
        exampleFiles = new ArrayList<String>(files.length);
        if (files != null) {
            for (String file : files) {
                exampleFiles.add(exampleFilesDir + file);
            }
        }

        // Clean everything.
        boolean errorsOccurred = DeleteDirectoryRecursively("/");

        // Copy example files.
        for (String filename : exampleFiles) {
            java.io.File sourceFile = new java.io.File(filename);
            FileInputStream source = null;
            FileHandle target = null;
            try {
                source = new FileInputStream(sourceFile);
                target = volume.openFile(userCredentials, "/" + sourceFile.getName(),
                        SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber()
                                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_CREAT.getNumber()
                                | SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_TRUNC.getNumber());
                byte[] buffer = new byte[128 * 1024];
                int bytesRead = 0;
                long bytewritten = 0;
                while ((bytesRead = source.read(buffer)) != -1) {
                    target.write(userCredentials, buffer, bytesRead, bytewritten);
                    bytewritten += bytesRead;
                }
            } catch (FileNotFoundException e) {
                System.out.println("File " + filename + " does not exist.");
                e.printStackTrace();
                errorsOccurred = true;
            } catch (IOException e) {
                System.out.println("An IOError occurred when copying the file " + filename + ".");
                e.printStackTrace();
                errorsOccurred = true;
            } finally {
                if (source != null) {
                    try {
                        source.close();
                    } catch (IOException e) {
                        System.out.println("The source file " + filename + " could not be closed.");
                        e.printStackTrace();
                        errorsOccurred = true;
                    }
                }
                if (target != null) {
                    try {
                        target.close();
                    } catch (IOException e) {
                        System.out.println("The target file /" + sourceFile.getName()
                                + " could not be closed.");
                        e.printStackTrace();
                        errorsOccurred = true;
                    }
                }
            }
        }
        // Truncate example files to actual size and chmod them.
        for (String file : exampleFiles) {
            String fileName = new java.io.File(file).getName();
            long fileSize = new java.io.File(file).length();
            FileHandle xtreemfsFile = null;
            try {
                xtreemfsFile = volume.openFile(userCredentials, "/" + fileName,
                        SYSTEM_V_FCNTL.SYSTEM_V_FCNTL_H_O_RDWR.getNumber());
                Stat stbuf = xtreemfsFile.getAttr(userCredentials).toBuilder().setMode(0666).build();
                volume.setAttr(userCredentials, "/" + fileName, stbuf, Setattrs.SETATTR_MODE.getNumber());
                xtreemfsFile.truncate(userCredentials, fileSize);
                xtreemfsFile.close();
            } catch (IOException e) {
                System.out.println("Failed to truncate /" + fileName + " to the length " + fileSize + ".");
                e.printStackTrace();
                errorsOccurred = true;
            }
        }

        client.shutdown();

        if (errorsOccurred) {
            System.out.println("Not all items could be deleted / files copied.");
            System.exit(3);
        }
    }
}
