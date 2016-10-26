/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.xtreemfs.common.auth.AuthenticationException;
import org.xtreemfs.common.auth.AuthenticationProvider;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.auth.UserCredentials;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.channels.ChannelIO;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;

/**
 * authentication provider for XOS certificates.
 * 
 * @author bjko
 */
public class LocalX509AuthProvider implements AuthenticationProvider {
    
    private NullAuthProvider nullAuth;
    
    public UserCredentials getEffectiveCredentials(RPC.UserCredentials ctx, ChannelIO channel) throws AuthenticationException {
        // use cached info!
        assert (nullAuth != null);
        if (channel.getAttachment() != null) {
            
            if (Logging.isDebug())
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.auth, this, "using attachment...");
            final Object[] cache = (Object[]) channel.getAttachment();
            final Boolean serviceCert = (Boolean) cache[0];
            if (serviceCert) {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.auth, this, "service cert...");
                return nullAuth.getEffectiveCredentials(ctx, channel);
            } else {
                if (Logging.isDebug())
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.auth, this, "using cached creds: "
                        + cache[1]);
                return (UserCredentials) cache[1];
            }
        }
        // parse cert if no cached info is present
        try {
            final Certificate[] certs = channel.getCerts();
            if (certs.length > 0) {
                final X509Certificate cert = ((X509Certificate) certs[0]);
                String fullDN = cert.getSubjectX500Principal().getName();
                String commonName = getNameElement(cert.getSubjectX500Principal().getName(), "CN");
                
                if (commonName.startsWith("host/") || commonName.startsWith("xtreemfs-service/")) {
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.auth, this,
                            "X.509-host cert present");
                    channel.setAttachment(new Object[] { new Boolean(true) });
                    // use NullAuth in this case to parse JSON header
                    return nullAuth.getEffectiveCredentials(ctx, null);
                } else {
                    
                    final String globalUID = commonName;
                    final String globalGID = getNameElement(cert.getSubjectX500Principal().getName(), "OU");
                    List<String> gids = new ArrayList<String>(1);
                    gids.add(globalGID);
                    
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.auth, this,
                            "X.509-User cert present: %s, %s", globalUID, globalGID);
                    
                    boolean isSuperUser = globalGID.contains("xtreemfs-admin");
                    final UserCredentials creds = new UserCredentials(globalUID, gids, isSuperUser);
                    channel.setAttachment(new Object[] { new Boolean(false), creds });
                    return creds;
                }
            } else {
                throw new AuthenticationException("no X.509-certificates present");
            }
        } catch (Exception ex) {
            Logging.logUserError(Logging.LEVEL_ERROR, Category.auth, this, ex);
            throw new AuthenticationException("invalid credentials " + ex);
        }
        
    }
    
    private String getNameElement(String principal, String element) {
        String[] elems = principal.split(",");
        for (String elem : elems) {
            String[] kv = elem.split("=");
            if (kv.length != 2)
                continue;
            if (kv[0].equals(element))
                return kv[1];
        }
        return null;
    }
    
    public void initialize(boolean useSSL, Properties properties) throws RuntimeException {
        if (!useSSL) {
            throw new RuntimeException(this.getClass().getName() + " can only be used if SSL is enabled!");
        }
        nullAuth = new NullAuthProvider();
        nullAuth.initialize(useSSL, properties);
    }
}
