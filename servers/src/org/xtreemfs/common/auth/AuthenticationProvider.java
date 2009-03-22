/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Bjoern Kolbeck (ZIB)
 */

package org.xtreemfs.common.auth;

import org.xtreemfs.foundation.pinky.channels.ChannelIO;

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
     * @throws java.lang.RuntimeException
     *             if the provider cannot be initialized.
     */
    void initialize(boolean useSSL) throws RuntimeException;
    
    /**
     * Get the effective credentials for an operation.
     * 
     * @param authHeader
     *            content of the Authentication header sent by the client
     * @param channel
     *            the channel used, can be used to store attachments and to get
     *            certificates
     * @return the effective user credentials
     * @throws org.xtreemfs.common.auth.AuthenticationException
     *             if authentication is not possible
     */
    UserCredentials getEffectiveCredentials(org.xtreemfs.interfaces.UserCredentials ctx, ChannelIO channel) throws AuthenticationException;
    
}
