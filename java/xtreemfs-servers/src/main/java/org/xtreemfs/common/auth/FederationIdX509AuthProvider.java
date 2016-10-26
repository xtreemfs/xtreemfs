/*
 * Copyright (c) 2008 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.auth;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.channels.ChannelIO;

/**
 * authentication provider for the Contrail project.
 * 
 * @author PS
 */
public class FederationIdX509AuthProvider implements AuthenticationProvider {


    private final static String USER_ID = "CN";
    private final static String GROUP_ID = "O";

    //    String privilegedCertificatePathname = "privileged.txt";
    //    private HashSet<String> privilegedCertificates;

    @Override
    public UserCredentials getEffectiveCredentials(
            org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials ctx,
            ChannelIO channel) throws AuthenticationException {

        // use cached info, if present!
        if (channel.getAttachment() != null) {
            if (Logging.isDebug()) {
                Logging.logMessage(
                        Logging.LEVEL_DEBUG, Category.auth, this, "using attachment...");
            }
            final UserCredentials creds = (UserCredentials)channel.getAttachment();

            if (Logging.isDebug()) {
                Logging.logMessage(
                        Logging.LEVEL_DEBUG, Category.auth, this, "using cached creds: " + creds);
            }

            return creds;
        }

        // parse cert if no cached info is present
        try {
            final Certificate[] certs = channel.getCerts();
            if (certs.length > 0) {
                final X509Certificate cert = ((X509Certificate) certs[0]);
                String fullDN = cert.getSubjectX500Principal().getName();

                final List<String> globalUIDs = getNamedElements(
                        cert.getSubjectX500Principal().getName(), USER_ID);

                // only use the UUID of the certificate
                String globalUID = null;
                if (!globalUIDs.isEmpty()) {
                    globalUID = globalUIDs.iterator().next();
                }
                else {
                    globalUID = fullDN;
                }

                final List<String> globalGIDs = getNamedElements(
                        cert.getSubjectX500Principal().getName(), GROUP_ID);

                if (globalGIDs.isEmpty()) {
                    globalGIDs.add(fullDN);
                }

                if (Logging.isDebug()) {
                    Logging.logMessage(
                            Logging.LEVEL_DEBUG, Category.auth, this,
                            "X.509-User cert present: %s, %s", globalUID, globalGIDs);
                }

                // the super user is required for the GAFS manager to
                // act in behalf of a user to create/delete volumes and
                // add policies to volumes
                boolean isSuperUser = false;
                //                for (String privilegedCert : this.privilegedCertificates) {
                //                    if (fullDN.contains(privilegedCert)) {
                //                        isSuperUser = true;
                //                        break;
                //                    }
                //                }

                final UserCredentials creds = new UserCredentials(globalUID, globalGIDs, isSuperUser);
                channel.setAttachment(creds);

                return creds;
            }
            else {
                throw new AuthenticationException("no X.509-certificates present");
            }
        } catch (Exception ex) {
            Logging.logUserError(Logging.LEVEL_ERROR, Category.auth, this, ex);
            throw new AuthenticationException("invalid credentials " + ex);
        }

    }

    private List<String> getNamedElements(String principal, String element) {
        String[] elems = principal.split(",");
        List<String> elements = new ArrayList<String>();
        for (String elem : elems) {
            String[] kv = elem.split("=");
            if (kv.length == 2
                    && kv[0].equals(element)) {
                elements.add(kv[1]);
            }
        }
        return elements;
    }

    public void initialize(boolean useSSL, Properties properties) throws RuntimeException {
        if (!useSSL) {
            throw new RuntimeException(this.getClass().getName() + " can only be used if SSL is enabled!");
        }

        //        InputStream privilegedCertificatesStream
        //        = getClass().getClassLoader().getResourceAsStream(this.privilegedCertificatePathname);

        // service certs
        //        this.privilegedCertificates = readHosts(privilegedCertificatesStream);
    }


    public static HashSet<String> readHosts(InputStream serviceCertificatesStream) {
        HashSet<String> serviceCertificates = new HashSet<String>();

        if (serviceCertificatesStream == null) {
            Logging.logMessage(
                    Logging.LEVEL_WARN, Category.auth, FederationIdX509AuthProvider.class,
                    "The list of privileged-certificates does not exist.");
            return serviceCertificates;
            //            throw new RuntimeException("The list of privileged-certificates does not exist");
        }

        InputStreamReader in = null;
        BufferedReader reader = null;
        try {
            in = new InputStreamReader(serviceCertificatesStream);
            reader = new BufferedReader(in);
            String line = null;
            while ((line = reader.readLine()) != null) {
                line.trim();

                if (line == null || line.equals("")) {
                    continue;
                }
                else {
                    serviceCertificates.add(line);

                    Logging.logMessage(Logging.LEVEL_INFO, Category.auth, FederationIdX509AuthProvider.class,
                            "Adding service-certificate: " + line);
                }
            }
        } catch (FileNotFoundException e) {
            Logging.logMessage(
                    Logging.LEVEL_WARN, Category.auth, FederationIdX509AuthProvider.class,
                    "The list of privileged-certificates does not exist.");

            //            throw new RuntimeException(
            //                    "The list of privileged-certificates does not exist.");
        } catch (IOException e) {
            Logging.logMessage(
                    Logging.LEVEL_WARN, Category.auth, FederationIdX509AuthProvider.class,
                    "Could not parse the list of privileged-certificates.");

            //            throw new RuntimeException(
            //                    "Could not parse the list of privileged-certificates.");
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
            }
        }
        return serviceCertificates;

    }
}
