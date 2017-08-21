/*  Copyright (c) 2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.sandbox.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.ReplicaUpdatePolicies;
import org.xtreemfs.common.clients.Client;
import org.xtreemfs.common.clients.File;
import org.xtreemfs.common.clients.RandomAccessFile;
import org.xtreemfs.common.clients.Volume;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.CLOption;
import org.xtreemfs.foundation.util.CLOption.IntegerValue;
import org.xtreemfs.foundation.util.CLOption.StringValue;
import org.xtreemfs.foundation.util.CLOption.Switch;
import org.xtreemfs.foundation.util.CLOptionParser;
import org.xtreemfs.foundation.util.InvalidUsageException;
import org.xtreemfs.foundation.util.PBRPCServiceURL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.AccessControlPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.PORTS;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;

/**
 * 
 * @author bjko
 */
public class ReplicatedTortureXtreemFS {
    
    public static void main(String[] args) {
        try {
            CLOptionParser parser = new CLOptionParser("TortureXtreemFS");
            CLOption.StringValue optVolname = (StringValue) parser.addOption(new CLOption.StringValue("v", "volname", "volume name"));
            CLOption.StringValue optPath = (StringValue) parser.addOption(new CLOption.StringValue("p", "path", "filename (default is torture.dat)"));
            CLOption.StringValue optPKCS12file = (CLOption.StringValue) parser.addOption(new CLOption.StringValue(null, "pkcs12-file-path", ""));
            CLOption.StringValue optPKCS12passphrase = (CLOption.StringValue) parser.addOption(new CLOption.StringValue(null, "pkcs12-passphrase", ""));
            CLOption.StringValue optSSLProtocol = (CLOption.StringValue) parser.addOption(
                    new CLOption.StringValue(null, "ssl-protocol",
                                             "SSL/TLS version to use: ssltls, tlsv1, tlsv11, tlsv12. 'ssltls' (default) accepts all versions, " + 
                                             "the others accept only the exact version they name. 'tlsv12' is available in JDK 7+ only. " + 
                                             "'tlsv11' comes with JDK 6 or 7, depending on the vendor."));
            CLOption.Switch      optRandomOnly = (Switch) parser.addOption(new CLOption.Switch("r", "random", "execute only random test"));
            CLOption.IntegerValue optReplicas = (IntegerValue) parser.addOption(new CLOption.IntegerValue("n", "num-replicas", "number of replicas to use (default is 1)"));
            CLOption.Switch      optTrunc = (Switch) parser.addOption(new CLOption.Switch("t", "truncae", "truncate to 0 instead of creating a new file"));
            CLOption.Switch      optAddR = (Switch) parser.addOption(new CLOption.Switch("a", "addreplica", "adds a new replica after writing and reads from it"));

            parser.parse(args);

            final List<String> arguments = parser.getArguments();
            
            Logging.start(Logging.LEVEL_WARN);
            TimeSync.initializeLocal(50);
            
            if (arguments.size() != 1) {
                usage();
                return;
            }
            
 
            
            final String path = optPath.isSet() ? optPath.getValue() : "/torture.data";
            final String volname = optVolname.isSet() ? optVolname.getValue() : "test";
            
            final PBRPCServiceURL dirURL = new PBRPCServiceURL(arguments.get(0),Schemes.SCHEME_PBRPC,PORTS.DIR_PBRPC_PORT_DEFAULT.getNumber());

            final boolean useSSL = dirURL.getProtocol().equals(Schemes.SCHEME_PBRPCG) || dirURL.getProtocol().equals(Schemes.SCHEME_PBRPCS);
            final boolean randomOnly = optRandomOnly.isSet();
            final boolean addReplica = optAddR.isSet();

            final int replicas = optReplicas.isSet() ? optReplicas.getValue() : 1;
            
            SSLOptions sslOptions = null;

            if (useSSL) {
                if (!optPKCS12file.isSet())
                    throw new InvalidUsageException("must specify a PCKS#12 file with credentials for (grid)SSL mode, use "+optPKCS12file.getName());
                if (!optPKCS12passphrase.isSet())
                    throw new InvalidUsageException("must specify a PCKS#12 passphrase for (grid)SSL mode, use "+optPKCS12passphrase.getName());

                final String sslProtocol = optSSLProtocol.isSet() ? optSSLProtocol.getValue() : null;
                final boolean gridSSL = dirURL.getProtocol().equals(Schemes.SCHEME_PBRPCG);
                sslOptions = new SSLOptions(optPKCS12file.getValue(), optPKCS12passphrase.getValue(), "PKCS12", null,
                        null, "none", false, gridSSL, sslProtocol, null);
            }


            Client c = new Client(new InetSocketAddress[]{new InetSocketAddress(dirURL.getHost(), dirURL.getPort())},
                    30*1000, 5*60*1000, sslOptions);
            c.start();
            System.out.println("file size from 64k to 512MB with record length from 4k to 1M");
            
            final int MIN_FS = 64 * 1024;
            final int MAX_FS = 512 * 1024 * 1024;
            
            final int MIN_REC = 4 * 1024;
            final int MAX_REC = 1024 * 1024;
            
            
            List<String> grs = new ArrayList(1);
            grs.add("torture");
            final UserCredentials uc = UserCredentials.newBuilder().setUsername("torture").addGroups("users").build();
            
            try {
                StripingPolicy sp = StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_RAID0).setWidth(1).setStripeSize(128).build();
                c.createVolume(volname, RPCAuthentication.authNone, uc, sp, AccessControlPolicyType.ACCESS_CONTROL_POLICY_POSIX, 0777);
            } catch (Exception ex) {
                //ignore
            }

            Volume v = c.getVolume(volname, uc);

            if (replicas > 1) {
                File f = v.getFile("/");
                f.setDefaultReplication(ReplicaUpdatePolicies.REPL_UPDATE_PC_WARONE, replicas);
            }

            RandomAccessFile tmp = v.getFile(path + ".tmp").open("rw", 0666);

            //System.out.println("Default striping policy is: " + tmp.getFile().get);
            
            if (!randomOnly) {
                for (int fsize = MIN_FS; fsize <= MAX_FS; fsize = fsize * 2) {
                    for (int recsize = MIN_REC; recsize <= MAX_REC; recsize = recsize * 2) {
                        if (testSequential_write(fsize, recsize, path, v)) {
                            continue;
                        }
                    }
                }
                System.out.println("waiting for files to be closed on OSD (100 seconds)");
                Thread.sleep(100*1000);
                int readRepl = replicas-1;
                if (addReplica) {
                    for (int fsize = MIN_FS; fsize <= MAX_FS; fsize = fsize * 2) {
                        for (int recsize = MIN_REC; recsize <= MAX_REC; recsize = recsize * 2) {
                            final int numRecs = fsize / recsize;
                            if (numRecs == 0) {
                                break;
                            }
                            File f = v.getFile(path+"."+fsize+"-"+recsize);
                            f.addReplica(1, f.getSuitableOSDs(1), 0);
                        }
                    }
                    readRepl = replicas;
                }

                for (int fsize = MIN_FS; fsize <= MAX_FS; fsize = fsize * 2) {
                    for (int recsize = MIN_REC; recsize <= MAX_REC; recsize = recsize * 2) {
                        if (testSequential_read(fsize, recsize, path, v,readRepl)) {
                            continue;
                        }
                    }
                }
            }
            
            /*System.out.println("\nrandom test\n");
            
            for (int fsize = MIN_FS; fsize <= MAX_FS; fsize = fsize * 2) {
                for (int recsize = MIN_REC; recsize <= MAX_REC; recsize = recsize * 2) {
                    if (testRandom(fsize, recsize, path, v)) {
                        continue;
                    }
                }
            }*/
            
            System.out.println("finished");
            c.stop();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        
    }
    
    private static boolean testSequential_write(int fsize, int recsize, final String path, Volume v)
        throws InterruptedException, Exception, IOException {
        final int numRecs = fsize / recsize;
        if (numRecs == 0) {
            return true;
        }
        byte[] sendBuffer = new byte[recsize];
        for (int i = 0; i < recsize; i++) {
            sendBuffer[i] = (byte) ((i % 26) + 65);
        }
        long tStart = System.currentTimeMillis();
        File f = v.getFile(path+"."+fsize+"-"+recsize);
        RandomAccessFile raf = f.open("rw", 0666);
        long tOpen = System.currentTimeMillis();
        long bytesWritten = 0;
        // do writes
        long tWrite = 0;
        for (int rec = 0; rec < numRecs; rec++) {
            long tmpStart = System.currentTimeMillis();
            bytesWritten += raf.write(sendBuffer, 0, recsize);
            tWrite += System.currentTimeMillis() - tmpStart;
        }
        assert (bytesWritten == numRecs * recsize);
        raf.flush();
        raf.seek(0);
        final long tFlush = System.currentTimeMillis();
        
        raf.close();
        
        double writeRate = ((double) fsize) / 1024.0 / (((double) (tWrite)) / 1000.0);
        System.out.format("fs: %8d   bs: %8d    write: %6d ms   %6.0f kb/s\n",
            fsize / 1024, recsize, tWrite, writeRate);
        return false;
    }

    private static boolean testSequential_read(int fsize, int recsize, final String path, Volume v, int forceReplica)
        throws  InterruptedException, Exception, IOException {
        final int numRecs = fsize / recsize;
        if (numRecs == 0) {
            return true;
        }
        byte[] sendBuffer = new byte[recsize];
        for (int i = 0; i < recsize; i++) {
            sendBuffer[i] = (byte) ((i % 26) + 65);
        }
        long tStart = System.currentTimeMillis();
        File f = v.getFile(path+"."+fsize+"-"+recsize);
        RandomAccessFile raf = f.open("rw", 0666);
        raf.forceReplica(forceReplica);
        long tOpen = System.currentTimeMillis();

        long tRead = 0;
        // do writes
        byte[] readBuffer = new byte[recsize];
        for (int rec = 0; rec < numRecs; rec++) {
            long tmpStart = System.currentTimeMillis();
            final int bytesRead = raf.read(readBuffer, 0, recsize);
            tRead += System.currentTimeMillis() - tmpStart;
            if (bytesRead != recsize) {
                System.out.println("PREMATURE END-OF-FILE AT " + rec * recsize);
                System.out.println("expected " + recsize + " bytes");
                System.out.println("got " + bytesRead + " bytes");
                System.exit(1);
            }
            for (int i = 0; i < recsize; i++) {
                if (readBuffer[i] != (byte) ((i % 26) + 65)) {
                    System.out.println("INVALID CONTENT AT " + (rec * recsize + i));
                    System.out.println("expected:  " + (byte) ((i % 26) + 65));
                    System.out.println("got     : " + readBuffer[i]);
                    System.exit(1);
                }
            }
        }

        raf.close();
        f.delete();

        final long tDelete = System.currentTimeMillis();
        double readRate = ((double) fsize) / 1024.0 / (((double) (tRead)) / 1000.0);
        System.out.format("fs: %8d   bs: %8d                                      read: %6d ms   %6.0f kb/s\n",
            fsize / 1024, recsize, tRead, readRate);
        return false;
    }
    
    /*private static boolean testRandom(int fsize, int recsize, final String path, Volume v, boolean truncate)
        throws ONCRPCException, InterruptedException, Exception, IOException {
        final int numRecs = fsize / recsize;
        int[] skips = new int[numRecs];
        if (numRecs == 0) {
            return true;
        }
        byte[] sendBuffer = new byte[recsize];
        for (int i = 0; i < recsize; i++) {
            sendBuffer[i] = (byte) ((i % 26) + 65);
        }
        long tStart = System.currentTimeMillis();
        File f = v.getFile(path);
        RandomAccessFile raf = f.open("rw", 0666);
        if (truncate)
            raf.setLength(0);
        long tOpen = System.currentTimeMillis();
        long bytesWritten = 0;
        long tWrite = 0;
        // do writes
        for (int rec = 0; rec < numRecs; rec++) {
            skips[rec] = (int) (Math.random() * ((double) recsize));
            raf.seek(raf.getFilePointer() + skips[rec]);
            long tmpStart = System.currentTimeMillis();
            bytesWritten += raf.write(sendBuffer, 0, recsize);
            tWrite += System.currentTimeMillis() - tmpStart;
        }
        if (bytesWritten != numRecs * recsize) {
            System.out.println("not all data was written!");
            System.exit(1);
        }
        raf.flush();
        raf.seek(0);
        final long tFlush = System.currentTimeMillis();
        long tRead = 0;
        // do writes
        byte[] readBuffer = new byte[recsize];
        for (int rec = 0; rec < numRecs; rec++) {
            raf.seek(raf.getFilePointer() + skips[rec]);
            long tmpStart = System.currentTimeMillis();
            final int bytesRead = raf.read(readBuffer, 0, recsize);
            tRead += System.currentTimeMillis() - tmpStart;
            if (bytesRead != recsize) {
                System.out.println("PREMATURE END-OF-FILE AT " + rec * recsize);
                System.out.println("expected " + recsize + " bytes");
                System.out.println("got " + bytesRead + " bytes");
                System.exit(1);
            }
            for (int i = 0; i < recsize; i++) {
                if (readBuffer[i] != (byte) ((i % 26) + 65)) {
                    System.out.println("INVALID CONTENT AT " + (rec * recsize + i));
                    System.out.println("expected:  " + (byte) ((i % 26) + 65));
                    System.out.println("got     : " + readBuffer[i]);
                    System.exit(1);
                }
            }
        }
        raf.close();

        if (!truncate)
            f.delete();
        
        final long tDelete = System.currentTimeMillis();
        double writeRate = ((double) fsize) / 1024.0 / (((double) (tWrite)) / 1000.0);
        double readRate = ((double) fsize) / 1024.0 / (((double) (tRead)) / 1000.0);
        System.out.format("fs: %8d   bs: %8d    write: %6d ms   %6.0f kb/s    read: %6d ms   %6.0f kb/s\n",
            fsize / 1024, recsize, tWrite, writeRate, tRead, readRate);
        return false;
    }*/
    
    private static void usage() {
        System.out.println("usage: torture [options] <dir_url> <mrc_url>");
        System.out.println("  -v <volume name>  name of the volume on the mrc (default: test)");
        System.out.println("  -p <path>   filename to use for measurements (default: /torture.dat)");
        
        System.out
                .println("            In case of a secured URL ('https://...'), it is necessary to also specify SSL credentials:");
        System.out
                .println("              -c  <creds_file>         a PKCS#12 file containing user credentials");
        System.out
                .println("              -cpass <creds_passphrase>   a pass phrase to decrypt the the user credentials file");
        System.out
                .println("              -t  <trusted_CAs>        a PKCS#12 file containing a set of certificates from trusted CAs");
        System.out
                .println("              -tpass <trusted_passphrase> a pass phrase to decrypt the trusted CAs file");
        System.out.println("  -h        show usage info");
    }
    
}
