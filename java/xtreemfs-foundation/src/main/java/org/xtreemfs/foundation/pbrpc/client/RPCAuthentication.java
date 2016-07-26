/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc.client;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;

/**
 *
 * @author bjko
 */
public class RPCAuthentication {

    public static final Auth authNone;

    public static final UserCredentials userService;

    static {
        authNone = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();
        userService = UserCredentials.newBuilder().setUsername("srv").addGroups("xtreemfs").build();
    }

}
