/*
 * Copyright (c) 2010 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;

/**
 *
 * @author bjko
 */
public class GlobalConstants {

    public static final Auth     AUTH_NONE;

    static {
        AUTH_NONE = Auth.newBuilder().setAuthType(AuthType.AUTH_NONE).build();
    }

}
