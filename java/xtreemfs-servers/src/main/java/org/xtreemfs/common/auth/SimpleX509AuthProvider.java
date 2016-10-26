/*
 * Copyright (c) 2008 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.auth;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.channels.ChannelIO;

/**
 * authentication provider for XOS certificates.
 * 
 * @author bjko
 */
public class SimpleX509AuthProvider implements AuthenticationProvider {
    
    /**
     * Parts of a distinguished name according to <a
     * href="http://www.ietf.org/rfc/rfc2253.txt">RFC 2253</a>.
     * 
     * @author robert
     *
     */
    public static enum X500DNElement {
        CN, // common name
        L, // locality name
        ST, // state or province name
        O, // organization name
        OU, // organizational unit name
        C, // country name
        STREET, // street address
        DC, // domain component
        UID, // user id
        DN // distinguished name ()
    };
    
    private NullAuthProvider nullAuth;
    
    /**
     * Distinguished name elements from which the user id and the group is is inferred.
     */
    private Map<String, X500DNElement> dnElementMappings;
    
    @Override
    public UserCredentials getEffectiveCredentials(org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials ctx,
        ChannelIO channel) throws AuthenticationException {
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
                String commonName = getNameElement(fullDN, X500DNElement.CN);
                
                if (commonName.startsWith("host/") || commonName.startsWith("xtreemfs-service/")) {
                    if (Logging.isDebug())
                        Logging.logMessage(Logging.LEVEL_DEBUG, Category.auth, this,
                            "X.509-host cert present");
                    channel.setAttachment(new Object[] { new Boolean(true) });
                    // use NullAuth in this case to parse JSON header
                    return nullAuth.getEffectiveCredentials(ctx, null);
                } else {
                    final String globalUID = getNameElement(fullDN, dnElementMappings.get("uid"));
                    final String globalGID = getNameElement(fullDN, dnElementMappings.get("gid"));
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
    
    private String getNameElement(String principal, X500DNElement element) {
        // entire distinguished name is required
        if (X500DNElement.DN.equals(element)) {
            return principal;
        }
        
        String[] elems = principal.split(",");
        for (String elem : elems) {
            String[] kv = elem.split("=");
            if (kv.length != 2)
                continue;
            if (X500DNElement.valueOf(kv[0]).equals(element))
                return kv[1];
        }
        return null;
    }
    
    public void initialize(boolean useSSL, String properties) throws RuntimeException {
        if (!useSSL) {
            throw new RuntimeException(this.getClass().getName() + " can only be used if SSL is enabled!");
        }
        nullAuth = new NullAuthProvider();
        nullAuth.initialize(useSSL, properties);
        
        dnElementMappings = new HashMap<String, X500DNElement>();
        dnElementMappings.put("uid", X500DNElement.DN);
        dnElementMappings.put("gid", X500DNElement.OU);
        
        if (properties != null) {
            String[] dnElements = properties.split(",");
            if (dnElements.length > 2) {
                throw new IllegalArgumentException("Too many properties specified: '"
                    + Arrays.toString(dnElements) + "', expecting at most 2.");
            }
            
            for (String dnElement : dnElements) {
                String[] mapping = dnElement.split(":");
                if (mapping.length != 2) {
                    throw new IllegalArgumentException("Malformed property found: '"
                        + dnElement + "', expecting 'key:value'");
                }
                
                if ("uid".equals(mapping[0]) || "gid".equals(mapping[0])) {
                    try {
                        dnElementMappings.put(mapping[0], X500DNElement.valueOf(mapping[1]));
                    } catch(IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid distinguished name element found: '"
                            + mapping[1] + "', expecting one of: " + Arrays.toString(X500DNElement.values()), e);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid property found: '"
                        + mapping[0] + "', expecting either 'uid' or 'gid'.");
                }
            }
        }
    }
}
