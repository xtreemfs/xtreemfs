/*
 * Copyright (c) 2009-2011 by Paul Seiferth,
 *                            Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.utils;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.foundation.pbrpc.client.RPCResponse;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthPassword;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.osd.drain.OSDDrain;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.serviceGetByTypeRequest;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceClient;

public class xtfs_remove_osd {

    private static final String DEFAULT_DIR_CONFIG = "/etc/xos/xtreemfs/default_dir";

    private OSDServiceClient    osd;

    private DIRServiceClient    dir;

    private MRCServiceClient    mrc;

    private RPCNIOSocketClient  dirClient;

    private RPCNIOSocketClient  osdClient;

    private RPCNIOSocketClient  mrcClient;

    private InetSocketAddress   osdAddr;

    private InetSocketAddress   mrcAddr;

    private SSLOptions          sslOptions;

    private InetSocketAddress   dirAddress;

    private UUIDResolver        resolver;
    private RPCNIOSocketClient  resolverClient;

    private Auth                authHeader;

    private UserCredentials     credentials;

    private String              osdUUIDString;
    private ServiceUUID         osdUUID;

    public static void main(String[] args) {

    }

    public xtfs_remove_osd(InetSocketAddress dirAddress, String osdUUIDString, SSLOptions sslOptions,
            String password) throws Exception {
        try {

            this.sslOptions = sslOptions;
            this.dirAddress = dirAddress;
            this.osdUUIDString = osdUUIDString;
            if (password.equals("")) {
                this.authHeader = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();
            } else {
                this.authHeader = Auth.newBuilder().setAuthType(AuthType.AUTH_PASSWORD)
                        .setAuthPasswd(AuthPassword.newBuilder().setPassword(password).build()).build();
            }

            // TODO: use REAL user credentials (this is a SECURITY HOLE)
            this.credentials = UserCredentials.newBuilder().setUsername("root").addGroups("root").build();

            resolverClient = new RPCNIOSocketClient(sslOptions, 10000, 5 * 60 * 1000);
            resolverClient.start();
            resolverClient.waitForStartup();
            this.resolver = UUIDResolver.startNonSingelton(new DIRServiceClient(resolverClient, dirAddress),
                    1000, 10 * 10 * 1000);

        } catch (Exception e) {
            shutdown();
            throw e;
        }
    }

    public void initialize() throws Exception {

        TimeSync.initializeLocal(0, 50);

        // connect to DIR
        dirClient = new RPCNIOSocketClient(sslOptions, 10000, 5 * 60 * 1000);
        dirClient.start();
        dirClient.waitForStartup();
        dir = new DIRServiceClient(dirClient, dirAddress);

        // create OSD client
        osdUUID = new ServiceUUID(osdUUIDString, resolver);
        osdUUID.resolve();
        osdAddr = osdUUID.getAddress();

        osdClient = new RPCNIOSocketClient(sslOptions, 10000, 5 * 60 * 1000);
        osdClient.start();
        osdClient.waitForStartup();
        osd = new OSDServiceClient(osdClient, osdAddr);

        // create MRC client
        serviceGetByTypeRequest getByTypeRequest = serviceGetByTypeRequest.newBuilder()
                .setType(ServiceType.SERVICE_TYPE_MRC).build();
        RPCResponse<ServiceSet> r = null;
        ServiceSet sSet = null;
        try {
            r = dir.xtreemfs_service_get_by_type(null, authHeader, credentials, getByTypeRequest);
            sSet = r.get();
        } catch (IOException ioe) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.proc, new Object(),
                    OutputUtils.stackTraceToString(ioe));
            throw ioe;
        } finally {
            if (r != null)
                r.freeBuffers();
        }

        mrcClient = new RPCNIOSocketClient(sslOptions, 100000, 5 * 60 * 10000);
        mrcClient.start();
        mrcClient.waitForStartup();

        if (sSet.getServicesCount() == 0)
            throw new IOException("No MRC is currently registred at DIR");

        String mrcUUID = sSet.getServices(0).getUuid();
        ServiceUUID UUIDService = new ServiceUUID(mrcUUID, resolver);
        UUIDService.resolve();
        mrcAddr = UUIDService.getAddress();

        mrc = new MRCServiceClient(mrcClient, mrcAddr);

    }

    public void shutdown() {
        try {

            UUIDResolver.shutdown(resolver);
            resolverClient.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes (drain) an OSD.
     * 
     * @throws Exception
     */
    public void drainOSD() throws Exception {
        OSDDrain osdDrain = new OSDDrain(dir, osd, mrc, osdUUID, authHeader, credentials, resolver);
        osdDrain.drain();
    }

    /**
     * Prints out usage informations and terminates the application.
     */
    public static void usage() {
        System.out.println("usage: xtfs_remove_osd [options] <osd_uuid>\n");
        System.out.println("  " + "<osd_uuid> the unique identifier of the OSD to clean.");
        System.out.println("  " + "options:");
        System.out.println("  " + "-dir <uri>  directory service to use (e.g. 'pbrpc://localhost:32638')");
        System.out.println("  " + "  If no URI is specified, URI and security settings are taken from '"
                + DEFAULT_DIR_CONFIG + "'");
        System.out
                .println("  "
                        + "  In case of a secured URI ('pbrpc://...'), it is necessary to also specify SSL credentials:");
        System.out
                .println("              -c  <creds_file>            a PKCS#12 file containing user credentials");
        System.out
                .println("              -cpass <creds_passphrase>   a pass phrase to decrypt the the user credentials file");
        System.out
                .println("              -t  <trusted_CAs>           a PKCS#12 file containing a set of certificates from trusted CAs");
        System.out
                .println("              -tpass <trusted_passphrase> a pass phrase to decrypt the trusted CAs file");
        System.out.println("  " + "-h     show these usage informations.");
        System.out.println("  "
                + "-p <admin_passphrase> the administrator password, authorizing osd-remove calls.");
        System.exit(1);
    }
}
