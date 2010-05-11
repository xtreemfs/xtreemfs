/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.utils;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.interfaces.DIRInterface.DIRInterface;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.interfaces.UserCredentials;

/**
 *
 * @author bjko
 */
public class xtreemcp {

    final static Pattern urlPattern = Pattern.compile("((oncrpc[gs]?):\\/\\/)?([^:]+)(?::([0-9]+))?/(\\w+)/(.*)");

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        

        try {

            if (args.length != 2) {
                printUsage("specify source and target file path");
            }

            final String srcName = args[0];
            final String targName = args[1];

            if (srcName.matches(urlPattern.pattern())) {
                //copy from xtreemfs
                XtreemFSFilePath src = parseXtreemFSpath(srcName);
                File targ = new File(targName);
                if (targ.exists()) {
                    System.out.println(targ+" exists, please delete first");
                    System.exit(1);
                }
                copyFile(src, targ, false);
            } else {
                //copy to xtreemfs
                File src = new File(srcName);
                XtreemFSFilePath targ = parseXtreemFSpath(targName);
                if (!src.exists()) {
                    System.out.println(src+" does not exist");
                    System.exit(2);
                }
                copyFile(targ, src, true);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public static void copyFile(XtreemFSFilePath xFile, File lFile, boolean copyTo) throws Exception {

        Logging.start(Logging.LEVEL_ERROR, Category.all);
        TimeSync.initializeLocal(50000, 50);

        Client c = new Client(new InetSocketAddress[]{xFile.dirAddress}, 1000*30, 1000*60*15, null);
        c.start();

        //guess user name
        String uname = System.getProperty("user.name");
        StringSet grps = new StringSet();
        grps.add("users");
        UserCredentials uc = new UserCredentials(uname, grps, null);

        final Volume v = c.getVolume(xFile.volumeName, uc);
        final org.xtreemfs.common.clients.File xf = v.getFile(xFile.path);
        long total = 0;
        long tStart = System.currentTimeMillis();
        if (copyTo) {

            java.io.RandomAccessFile rafIn = new RandomAccessFile(lFile, "r");

            if (xf.exists()) {
                xf.delete();
            }
            org.xtreemfs.common.clients.RandomAccessFile rafOut = xf.open("rw", 0666);

            byte[] data = new byte[1024*1024];

            int bytesRead = 0;

            do {
                bytesRead = rafIn.read(data);
                if (bytesRead <= 0)
                    break;
                total += bytesRead;
                rafOut.write(data, 0, bytesRead);

            } while (bytesRead == data.length); //not EOF
            rafIn.close();
            rafOut.close();

        } else {
            if (lFile.exists()) {
                lFile.delete();
            }
            java.io.RandomAccessFile rafOut = new RandomAccessFile(lFile, "rw");

            
            org.xtreemfs.common.clients.RandomAccessFile rafIn = xf.open("r", 0666);

            byte[] data = new byte[1024*1024*4];

            int bytesRead = 0;
            do {
                bytesRead = rafIn.read(data,0,data.length);
                if (bytesRead <= 0)
                    break;
                total += bytesRead;
                rafOut.write(data, 0, bytesRead);

            } while (bytesRead == data.length); //not EOF
            rafIn.close();
            rafOut.close();
        }
        long tEnd = System.currentTimeMillis();
        System.out.println("copied "+OutputUtils.formatBytes(total)+" in "+((tEnd-tStart)/1000.0)+" seconds");

        c.stop();
        TimeSync.close();


    }

    public static XtreemFSFilePath parseXtreemFSpath(String path) throws Exception {
        Matcher m = urlPattern.matcher(path);
        if (m.matches()) {
            int grp = 2;
            String proto = m.group(2);
            String hostname = m.group(3);
            int port = DIRInterface.ONCRPC_PORT_DEFAULT;
            if (m.group(4) != null) {
                port = Integer.valueOf(m.group(4));
            }
            String volName = m.group(5);
            String volPath = m.group(6);
            XtreemFSFilePath p = new XtreemFSFilePath();
            p.dirAddress = new InetSocketAddress(hostname, port);
            p.volumeName = volName;
            p.path = volPath;
            System.out.println("DIR: "+hostname+":"+port+"/"+volName+"/"+volPath);
            return p;
        } else {
            throw new Exception("invalid XtreemFS file path: '"+path+"'");
        }
    }

    public static void printUsage(String errorMessage) {
        System.out.println("Error: "+errorMessage);
        System.out.println("usage: xtreemcp <source file> <target file>");
        System.out.println("file can be a local file path or on an xtreemfs volume");
        System.out.println("files on xtreemfs must be specified as oncrpc://<dir address>/<volume name>/<path>");
        System.out.println("");
        System.exit(1);
    }

    public static class XtreemFSFilePath {

        public InetSocketAddress dirAddress;
        String volumeName;
        String path;

    }

}
