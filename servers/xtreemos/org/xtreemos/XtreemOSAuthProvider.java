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
package org.xtreemos;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.auth.AuthenticationException;
import org.xtreemfs.common.auth.AuthenticationProvider;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.auth.UserCredentials;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.pinky.channels.ChannelIO;
import org.xtreemos.wp35.VO;
import org.xtreemos.wp35.util.CertificateProcessor;

/**
 * authentication provider for XOS certificates.
 * @author bjko
 */
public class XtreemOSAuthProvider implements AuthenticationProvider {

    private NullAuthProvider nullAuth;

    public UserCredentials getEffectiveCredentials(String authHeader, ChannelIO channel) throws AuthenticationException {
        //use cached info!
        assert(nullAuth != null);
        if (channel.getAttachment() != null) {
            Logging.logMessage(Logging.LEVEL_DEBUG,this,"using attachment...");
            final Object[] cache = (Object[])channel.getAttachment();
            final Boolean serviceCert = (Boolean)cache[0];
            if (serviceCert) {
                Logging.logMessage(Logging.LEVEL_DEBUG,this,"service cert...");
                return nullAuth.getEffectiveCredentials(authHeader, channel);
            } else {
                Logging.logMessage(Logging.LEVEL_DEBUG,this,"using cached creds: "+cache[1]);
                return (UserCredentials)cache[1];
            }
        }
        //parse cert if no cached info is present
        try {
            final Certificate[] certs = channel.getCerts();
            if (certs.length > 0) {
                CertificateProcessor cp = new CertificateProcessor();
                byte[] content = ((X509Certificate) certs[0]).getExtensionValue(VO.Attribute.GlobalUserID.getOID());
                if (content == null) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "XOS-Service cert present");
                    channel.setAttachment(new Object[]{new Boolean(true)});
                    //use NullAuth in this case to parse JSON header
                    return nullAuth.getEffectiveCredentials(authHeader, null);
                } else {

                    final String globalUID = cp.getVOAttributeValue((X509Certificate) certs[0], VO.Attribute.GlobalUserID);
                    final String globalGID = cp.getVOAttributeValue((X509Certificate) certs[0], VO.Attribute.GlobalPrimaryGroupName);
                    final String secondaryGroupNames = cp.getVOAttributeValue((X509Certificate) certs[0], VO.Attribute.GlobalSecondaryGroupNames);
                    final String[] groupList = (secondaryGroupNames == null) ? new String[]{} : secondaryGroupNames.split(",");
                    List<String> gids = new ArrayList(groupList.length+1);
                    gids.add(globalGID);
                    for (final String gid:groupList)
                        gids.add(gid);
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "XOS-User cert present: " + globalUID + "," + globalGID+ "secondary grps: "+gids);

                    boolean isSuperUser = gids.contains("VOAdmin");
                    final UserCredentials creds = new UserCredentials(globalUID, gids, isSuperUser);
                    channel.setAttachment(new Object[]{new Boolean(false),creds});
                    return creds;
                }
            } else {
                throw new AuthenticationException("no XOS-certificates present");
            }
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, ex);
            throw new AuthenticationException("invalid credentials "+ex);
        }

    }

    public void initialize(boolean useSSL) throws RuntimeException {
        if (!useSSL) {
            throw new RuntimeException(this.getClass().getName() + " can only be used if use_ssl is enabled!");
        }
        nullAuth = new NullAuthProvider();
        nullAuth.initialize(useSSL);
    }
}
