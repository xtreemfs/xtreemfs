/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

import static org.xtreemfs.foundation.pbrpc.client.RPCAuthentication.authNone;

import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;

/**
 * @author jensvfischer
 */
public class ConnectionData {

    final String          userName;
    final String          group;
    final String          osdPassword;
    final String          dirAddress;
    final String          mrcAddress;
    final UserCredentials userCredentials;
    final Auth            auth;
    final SSLOptions      sslOptions;
    final Options         options;

    /* Convenience constructor with various default values for local setup. */
    ConnectionData() {
        this.userName = "testUser";
        this.group = "benchmark";
        this.osdPassword = "";
        this.dirAddress = "127.0.0.1:32638";
        this.mrcAddress = "127.0.0.1:32636";
        this.userCredentials = UserCredentials.newBuilder().setUsername(userName).addGroups(group).build();
        this.auth = authNone;
        this.sslOptions = null;
        this.options = new Options();
    }

}
