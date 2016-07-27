/*
 * Copyright (c) 2008-2010 by Bjoern Kolbeck, Jan Stender, Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.util.OutputUtils;

/**
 * Encapsulates the SSLOptions for the connections of pinky and speedy
 * 
 * @author clorenz
 */
public class SSLOptions {
    /**
     * a Java JKS Keystore
     */
    public final static String JKS_CONTAINER    = "JKS";
    
    /**
     * a PKCS12 Keystore
     */
    public final static String PKCS12_CONTAINER = "PKCS12";
    
    /**
     * Default SSL/TLS Protocol to use when no or an invalid protocol was specified
     */
    public final static String DEFAULT_SSL_PROTOCOL = "TLS";
    
    /**
     * file with the private key and the public cert for the server
     */
    private final InputStream  serverCredentialFile;
    private final String       serverCredentialFilePath;
    
    /**
     * file with trusted public certs
     */
    private final InputStream  trustedCertificatesFile;
    private final String       trustedCertificatesFilePath;
    
    /**
     * passphrase of the server credential file
     */
    private final char[]       serverCredentialFilePassphrase;
    
    /**
     * passphrase of the trusted certificates file
     */
    private final char[]       trustedCertificatesFilePassphrase;
    
    /**
     * using symmetric encryption or only authenticating via certs
     */
    private boolean            authenticationWithoutEncryption;
    
    /**
     * file format of the server credential file
     */
    private final String       serverCredentialFileContainer;
    
    /**
     * file format of the trusted certificates file
     */
    private final String       trustedCertificatesFileContainer;
    
    /**
     * knows the used certs and more
     */
    private final SSLContext   sslContext;
    
    private final boolean      useFakeSSLMode;
    
    /** The SSL protocol this SSLOptions instance has been initalized with */
    private final String       sslProtocolString;

    public SSLOptions(String serverCredentialFilePath, String serverCredentialFilePassphrase,
            String serverCredentialFileContainer, String trustedCertificatesFilePath,
            String trustedCertificatesFilePassphrase, String trustedCertificatesFileContainer,
            boolean authenticationWithoutEncryption, boolean useFakeSSLMode, String sslProtocolString,
            TrustManager trustManager) throws IOException {
        this(serverCredentialFilePath, new FileInputStream(serverCredentialFilePath), serverCredentialFilePassphrase,
                serverCredentialFileContainer, trustedCertificatesFilePath, new FileInputStream(
                        trustedCertificatesFilePath), trustedCertificatesFilePassphrase,
                trustedCertificatesFileContainer, authenticationWithoutEncryption, useFakeSSLMode, sslProtocolString,
                trustManager);
    }

    public SSLOptions(InputStream serverCredentialFile, String serverCredentialFilePassphrase,
        String serverCredentialFileContainer, InputStream trustedCertificatesFile,
        String trustedCertificatesFilePassphrase, String trustedCertificatesFileContainer,
        boolean authenticationWithoutEncryption, boolean useFakeSSLMode, String sslProtocolString,
        TrustManager trustManager) throws IOException {
        this(null, serverCredentialFile, serverCredentialFilePassphrase, serverCredentialFileContainer, null,
                trustedCertificatesFile, trustedCertificatesFilePassphrase, trustedCertificatesFileContainer,
                authenticationWithoutEncryption, useFakeSSLMode, sslProtocolString, trustManager);
    }

    private SSLOptions(String serverCredentialFilePath, InputStream serverCredentialFile,
            String serverCredentialFilePassphrase, String serverCredentialFileContainer,
            String trustedCertificatesFilePath, InputStream trustedCertificatesFile,
            String trustedCertificatesFilePassphrase, String trustedCertificatesFileContainer,
            boolean authenticationWithoutEncryption, boolean useFakeSSLMode, String sslProtocolString,
            TrustManager trustManager) throws IOException {

        this.serverCredentialFilePath = serverCredentialFilePath;
        this.serverCredentialFile = serverCredentialFile;
        this.trustedCertificatesFilePath = trustedCertificatesFilePath;
        this.trustedCertificatesFile = trustedCertificatesFile;
        
        if (serverCredentialFilePassphrase != null)
            this.serverCredentialFilePassphrase = serverCredentialFilePassphrase.toCharArray();
        else
            this.serverCredentialFilePassphrase = null;
        
        if (trustedCertificatesFilePassphrase != null)
            this.trustedCertificatesFilePassphrase = trustedCertificatesFilePassphrase.toCharArray();
        else
            this.trustedCertificatesFilePassphrase = null;
        
        this.serverCredentialFileContainer = serverCredentialFileContainer;
        this.trustedCertificatesFileContainer = trustedCertificatesFileContainer;
        
        this.authenticationWithoutEncryption = authenticationWithoutEncryption;
        
        this.useFakeSSLMode = useFakeSSLMode;
        
        this.sslProtocolString = sslProtocolString;
        sslContext = createSSLContext(sslProtocolStringToProtocol(sslProtocolString), trustManager);
    }
    
    /**
     * Create/initialize the SSLContext with key material
     * 
     * @param trustManager
     *            the trust manager for the SSL context (may be
     *            <code>null</code>)
     * @return the created and initialized SSLContext
     * @throws IOException
     */
    private SSLContext createSSLContext(String sslProtocol, TrustManager trustManager) throws IOException {
        SSLContext sslContext = null;
        try {
            // First initialize the key and trust material.
            KeyStore ksKeys = KeyStore.getInstance(serverCredentialFileContainer);
            ksKeys.load(serverCredentialFile, serverCredentialFilePassphrase);
            
            // KeyManager's decide which key material to use.
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ksKeys, serverCredentialFilePassphrase);
            
            // There are quite a few issues with the OpenJDK PKCS11 provider in combination with NSS,
            // so remove it no matter what the OpenJDK version is.
            if ("OpenJDK Runtime Environment".equals(System.getProperty("java.runtime.name"))) {
                try {
                    Security.removeProvider("SunPKCS11-NSS");
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "Successfully removed faulty security provider 'SunPKCS11-NSS'.");
                    }
                } catch(SecurityException e) {
                    Logging.logMessage(Logging.LEVEL_WARN, this,
                                       "Could not remove security provider 'SunPKCS11-NSS'. This might cause TLS connections to time out. " +
                                       "Known to affect multiple OpenJDK / NSS version combinations.");
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "%s:\n%s", e.getMessage(), OutputUtils.stackTraceToString(e));
                    }
                }
            }
            
            // Re-enable disabled algorithms if the user requests it.
            final String defaultDisabledAlgorithms = Security.getProperty("jdk.tls.disabledAlgorithms");
            removeDisabledEntailedProtocolSupportForProtocol(sslProtocol);
            
            try {
                sslContext = SSLContext.getInstance(sslProtocol);
            } catch (NoSuchAlgorithmException e) {
                Logging.logMessage(Logging.LEVEL_WARN, this, "Unsupported algorithm '%s', defaulting to '%s'.",
                                   sslProtocol, DEFAULT_SSL_PROTOCOL);
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "%s:\n%s", e.getMessage(), OutputUtils.stackTraceToString(e));
                }
                
                // Reset disabled algorithms because the context could not be created.
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, this, "Trying to reset disabled algorithms.");
                }
                try {
                    Security.setProperty("jdk.tls.disabledAlgorithms", defaultDisabledAlgorithms);
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "Successfully reset disabled algorithms.");
                    }
                } catch (SecurityException e1) {
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "Could not reset disabled algorithms: %s", OutputUtils.stackTraceToString(e1));
                    }
                }
                
                // Setup everything anew for the default SSL protocol.
                removeDisabledEntailedProtocolSupportForProtocol(DEFAULT_SSL_PROTOCOL);
                sslContext = SSLContext.getInstance(DEFAULT_SSL_PROTOCOL);
            }
            
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "Disabling the following algorithms: %s", Security.getProperty("jdk.tls.disabledAlgorithms"));
            }
            
            if (trustManager != null) {
                // if a user-defined trust manager is set ...
                trustManager.init(trustedCertificatesFileContainer, trustedCertificatesFile,
                    trustedCertificatesFilePassphrase);
                sslContext.init(kmf.getKeyManagers(), new TrustManager[] { trustManager }, null);
            } else if (trustedCertificatesFileContainer.equals("none")) {
                TrustManager[] myTMs = new TrustManager[] { new NoAuthTrustStore() };
                sslContext.init(kmf.getKeyManagers(), myTMs, null);
            } else {
                
                // TrustManager's decide whether to allow connections.
                KeyStore ksTrust = null;
                if (trustedCertificatesFileContainer.equals("none")) {
                    ksTrust = KeyStore.getInstance(KeyStore.getDefaultType());
                } else {
                    ksTrust = KeyStore.getInstance(trustedCertificatesFileContainer);
                    ksTrust.load(trustedCertificatesFile, trustedCertificatesFilePassphrase);
                }
                
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(ksTrust);
                
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            }
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        return sslContext;
    }
    
    public boolean isAuthenticationWithoutEncryption() {
        return this.authenticationWithoutEncryption;
    }
    
    public void setAuthenticationWithoutEncryption(boolean authenticationWithoutEncryption) {
        this.authenticationWithoutEncryption = authenticationWithoutEncryption;
    }
    
    public InputStream getServerCredentialFile() {
        return this.serverCredentialFile;
    }
    
    public String getServerCredentialFilePath() {
        return this.serverCredentialFilePath;
    }

    public String getServerCredentialFileContainer() {
        return this.serverCredentialFileContainer;
    }
    
    public String getServerCredentialFilePassphrase() {
        return String.valueOf(this.serverCredentialFilePassphrase);
    }
    
    public InputStream getTrustedCertificatesFile() {
        return this.trustedCertificatesFile;
    }
    
    public String getTrustedCertificatesFilePath() {
        return this.trustedCertificatesFilePath;
    }

    public String getTrustedCertificatesFileContainer() {
        return this.trustedCertificatesFileContainer;
    }
    
    public String getTrustedCertificatesFilePassphrase() {
        return String.valueOf(this.trustedCertificatesFilePassphrase);
    }
    
    public SSLContext getSSLContext() {
        return this.sslContext;
    }
    
    public boolean isFakeSSLMode() {
        return this.useFakeSSLMode;
    }
    
    public String getSSLProtocol() {
        return sslContext.getProtocol();
    }
    
    public String getSSLProtocolString() {
        return sslProtocolString;
    }

    public boolean isSSLEngineProtocolSupported(String sslEngineProtocol) {
        // Protocol names in JDK 5, 6: SSLv2Hello, SSLv3, TLSv1
        // Additionally in JDK 7, 8: TLSv1.2
        // TLSv1.1 seems to depend on the vendor
        String sslProtocol = getSSLProtocol();
        if ("SSLv3".equals(sslProtocol)) {
            return "SSLv3".equals(sslEngineProtocol);
        } else if ("TLS".equals(sslProtocol)) {
            return "SSLv3".equals(sslEngineProtocol) ||
                   "TLSv1".equals(sslEngineProtocol) ||
                   "TLSv1.1".equals(sslEngineProtocol) ||
                   "TLSv1.2".equals(sslEngineProtocol);
        } else if ("TLSv1".equals(sslProtocol)) {
            return "TLSv1".equals(sslEngineProtocol);
        } else if ("TLSv1.1".equals(sslProtocol)) {
            return "TLSv1.1".equals(sslEngineProtocol);
        } else if ("TLSv1.2".equals(sslProtocol)) {
            return "TLSv1.2".equals(sslEngineProtocol);
        } else {
            return false;
        }
    }
    
    private String sslProtocolStringToProtocol(String sslProtocolString) {
        // SSL Context Protocol Strings:
        // JDK 6: SSL, SSLv2, SSLv3, TLS, TLSv1
        // additionally in JDK 7: TLSv1.2
        // TLSv1.1 seems to depend on the vendor
        if ("sslv3".equals(sslProtocolString)) {
            return  "SSLv3";
        } else if ("ssltls".equals(sslProtocolString)) {
            return "TLS";
        } else if ("tlsv1".equals(sslProtocolString)) {
            return "TLSv1";
        } else if ("tlsv11".equals(sslProtocolString)) {
            return "TLSv1.1";
        } else if ("tlsv12".equals(sslProtocolString)) {
            return "TLSv1.2";
        } else {
            if (sslProtocolString != null) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.net, this,
                                   "Unknown SSL Context Protocol: '%s', defaulting to '%s'.",
                                   sslProtocolString, DEFAULT_SSL_PROTOCOL);
            }
            return DEFAULT_SSL_PROTOCOL;
        }
    }
    
    /**
     * Removes all protocols that should be supported when using {@code sslProtocol} from the disabled
     * algorithms list that is set as system default, e.g. in  /usr/lib/jvm/default-java/jre/lib/security/java.security.
     * 
     * @param sslProtocol
     */
    private void removeDisabledEntailedProtocolSupportForProtocol(String sslProtocol) {
        if (Security.getProperty("jdk.tls.disabledAlgorithms") == null) {
            return; // no disabled algorithms, everything is allowed by default
        }
        
        String[] entailedSupportedProtocols = new String[] {};
        if ("SSLv3".equals(sslProtocol)) {
            entailedSupportedProtocols = new String[] { "SSLv3" };
        } else if ("TLS".equals(sslProtocol)) {
            entailedSupportedProtocols = new String[] { "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2" };
        } else if ("TLSv1".equals(sslProtocol)) {
            entailedSupportedProtocols = new String[] { "TLSv1" };
        } else if ("TLSv1.1".equals(sslProtocol)) {
            entailedSupportedProtocols = new String[] { "TLSv1.1" };
        } else if ("TLSv1.2".equals(sslProtocol)) {
            entailedSupportedProtocols = new String[] { "TLSv1.2" };
        }
        
        // For each protocol whose support is entailed by the requested protocol,
        // remove it from the disabled algorithms list if possible.
        for (String supportedSSLProtocol : entailedSupportedProtocols) {
            if (Security.getProperty("jdk.tls.disabledAlgorithms").contains(supportedSSLProtocol)) {
                Logging.logMessage(Logging.LEVEL_WARN, this,
                      "Algorithm '%s' is disabled in your java.security configuration file (see key 'jdk.tls.disabledAlgorithms'). " +
                      "Trying to enable algorithm '%s' manually as specified in your configuration file (see key 'ssl.protocol'). " +
                      "Consider using a newer SSL/TLS algorithm for your setup, " +
                      "as algorithm '%s' has been disabled by default because of security issues.",
                      supportedSSLProtocol, supportedSSLProtocol, supportedSSLProtocol);
                try {
                    Security.setProperty("jdk.tls.disabledAlgorithms",
                            Security.getProperty("jdk.tls.disabledAlgorithms").replace(supportedSSLProtocol, "").replace("  ", ""));
                    if (Logging.isDebug()) {
                        Logging.logMessage(Logging.LEVEL_DEBUG, this, "Successfully removed algorithm '%s' from disabled algorithms.",
                                supportedSSLProtocol);
                    }
                } catch (SecurityException e) {
                    Logging.logMessage(Logging.LEVEL_WARN, this, "Could not remove algorithm '%s' from disabled algorithm. " +
                        "This might cause SSL Handshake exceptions. For SSLv3 this is known to affect all JDKs fixing issue CVE-2014-3566.",
                        supportedSSLProtocol);
                }
            }
        }
    }
    
    private static class NoAuthTrustStore implements TrustManager, X509TrustManager {
        
        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            // ignore
        }
        
        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            // ignore
        }
        
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }
        
        @Override
        public void init(String trustedCertificatesFileContainer, InputStream trustedCertificatesFile,
            char[] trustedCertificatesFilePassphrase) {
            // ignore
        }
        
    }
    
    public static interface TrustManager extends javax.net.ssl.TrustManager {
        public void init(String trustedCertificatesFileContainer, InputStream trustedCertificatesFile,
            char[] trustedCertificatesFilePassphrase);
    }
}
