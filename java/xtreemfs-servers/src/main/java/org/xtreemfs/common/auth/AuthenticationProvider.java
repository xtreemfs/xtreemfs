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
 * Authentication Providers extract the credentials (UID/GIDs/SuperUser) from
 * the authentication header and the certificates.
 * 
 * @author bjko
 */
public interface AuthenticationProvider {
    
    /**
     * initializes the provider class
     * 
     * @param useSSL
     *            true, if SSL is enabled.
     * @param properties
     *            initialization properties
     * @throws java.lang.RuntimeException
     *             if the provider cannot be initialized.
     */
    void initialize(boolean useSSL, Properties properties) throws RuntimeException;
    
    /**
     * Get the effective credentials for an operation.
     * 
     * @param ctx
     *            user credentials sent by the client
     * @param channel
     *            the channel used, can be used to store attachments and to get
     *            certificates
     * @return the effective user credentials
     * @throws org.xtreemfs.common.auth.AuthenticationException
     *             if authentication is not possible
     */
    UserCredentials getEffectiveCredentials(org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials ctx, ChannelIO channel) throws AuthenticationException;
    
}
