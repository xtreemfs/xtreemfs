/*
 * Copyright (c) 2008 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.auth;

import java.util.Properties;

import org.xtreemfs.foundation.pbrpc.channels.ChannelIO;

/**
 * A simple provider that parses the JSON string sent in the authentication
 * header as described in the protocol spec.
 * 
 * @author bjko
 */
public class NullAuthProvider implements AuthenticationProvider {
    
    public NullAuthProvider() {
    }
    
    public UserCredentials getEffectiveCredentials(org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials ctx, ChannelIO channel)
        throws AuthenticationException {
        return new UserCredentials(ctx.getUsername(), ctx.getGroupsList(), ctx.getUsername().equals("root"));
    }
    
    public void initialize(boolean useSSL, Properties properties) throws RuntimeException {
    }
    
}
