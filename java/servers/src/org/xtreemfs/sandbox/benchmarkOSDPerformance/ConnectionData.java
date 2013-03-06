/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import org.xtreemfs.common.libxtreemfs.ClientFactory;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;

import static org.xtreemfs.foundation.pbrpc.client.RPCAuthentication.authNone;

/**
 * @author jensvfischer
 */
public class ConnectionData {
    final String userName;
    final String group;
    final String dirAddress;
    final String mrcAddress;
    final UserCredentials userCredentials;
    final Auth auth;
    final SSLOptions sslOptions;
    final Options options;

    /* Convenience constructor with various default values for local setup. */
    ConnectionData() throws Exception {
        userName = "testUser";
        group = "benchmark";
        dirAddress = "127.0.0.1:32638";
        mrcAddress = "127.0.0.1:32636";
        userCredentials = UserCredentials.newBuilder().setUsername(userName).addGroups(group).build();
        auth = authNone;
        sslOptions = null;
        options = new Options();
    }

}
