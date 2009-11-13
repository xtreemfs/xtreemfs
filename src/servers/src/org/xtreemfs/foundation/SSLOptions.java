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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB), Christian Lorenz (ZIB)
 */

package org.xtreemfs.foundation;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

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
     * file with the private key and the public cert for the server
     */
    private final InputStream  serverCredentialFile;
    
    /**
     * file with trusted public certs
     */
    private final InputStream  trustedCertificatesFile;
    
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

    private final boolean     useFakeSSLMode;
    
    /**
     * creates a new SSLOptions object, which uses PKCS12 Container and
     * symmetric encryption
     * 
     * @param serverCredentialFile
     *            file with the private key and the public cert for the server
     * @param serverCredentialFilePassphrase
     *            passphrase of the server credential file
     * @param trustedCertificatesFile
     *            file with trusted public certs
     * @param trustedCertificatesFilePassphrase
     *            passphrase of the trusted certificates file
     * @throws IOException
     */
    public SSLOptions(InputStream serverCredentialFile, String serverCredentialFilePassphrase,
        InputStream trustedCertificatesFile, String trustedCertificatesFilePassphrase) throws IOException {
        this(serverCredentialFile, serverCredentialFilePassphrase, PKCS12_CONTAINER, trustedCertificatesFile,
            trustedCertificatesFilePassphrase, JKS_CONTAINER, false);
    }
    
    /**
     * creates a new SSLOptions object, which uses PKCS12 Container
     * 
     * @param serverCredentialFile
     *            file with the private key and the public cert for the server
     * @param serverCredentialFilePassphrase
     *            passphrase of the server credential file
     * @param trustedCertificatesFile
     *            file with trusted public certs
     * @param trustedCertificatesFilePassphrase
     *            passphrase of the trusted certificates file
     * @param authenticationWithoutEncryption
     *            using symmetric encryption or only authenticating via certs
     * @throws IOException
     */
    public SSLOptions(InputStream serverCredentialFile, String serverCredentialFilePassphrase,
        InputStream trustedCertificatesFile, String trustedCertificatesFilePassphrase,
        boolean authenticationWithoutEncryption, boolean useGridSSL) throws IOException {
        this(serverCredentialFile, serverCredentialFilePassphrase, PKCS12_CONTAINER, trustedCertificatesFile,
            trustedCertificatesFilePassphrase, JKS_CONTAINER, authenticationWithoutEncryption,useGridSSL);
    }
    
    /**
     * creates a new SSLOptions object
     * 
     * @param serverCredentialFile
     *            file with the private key and the public cert for the server
     * @param serverCredentialFilePassphrase
     *            passphrase of the server credential file
     * @param serverCredentialFileContainer
     *            file format of the server credential file
     * @param trustedCertificatesFile
     *            file with trusted public certs
     * @param trustedCertificatesFilePassphrase
     *            passphrase of the trusted certificates file
     * @param trustedCertificatesFileContainer
     *            file format of the trusted certificates file
     * @param authenticationWithoutEncryption
     *            using symmetric encryption or only authenticating via certs
     * @throws IOException
     */
    public SSLOptions(InputStream serverCredentialFile, String serverCredentialFilePassphrase,
        String serverCredentialFileContainer, InputStream trustedCertificatesFile,
        String trustedCertificatesFilePassphrase, String trustedCertificatesFileContainer,
        boolean authenticationWithoutEncryption) throws IOException {
        this(serverCredentialFile,serverCredentialFilePassphrase,serverCredentialFileContainer,
             trustedCertificatesFile,trustedCertificatesFilePassphrase,trustedCertificatesFileContainer,
             authenticationWithoutEncryption,false);
    }

    public SSLOptions(InputStream serverCredentialFile, String serverCredentialFilePassphrase,
        String serverCredentialFileContainer, InputStream trustedCertificatesFile,
        String trustedCertificatesFilePassphrase, String trustedCertificatesFileContainer,
        boolean authenticationWithoutEncryption, boolean useFakeSSLMode) throws IOException {
        this.serverCredentialFile = serverCredentialFile;
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
        
        sslContext = createSSLContext();
    }
    
    /**
     * Create/initialize the SSLContext with key material
     * 
     * @return the created and initialized SSLContext
     * @throws IOException
     */
    private SSLContext createSSLContext() throws IOException {
        SSLContext sslContext = null;
        try {
            // First initialize the key and trust material.
            KeyStore ksKeys = KeyStore.getInstance(serverCredentialFileContainer);
            ksKeys.load(serverCredentialFile, serverCredentialFilePassphrase);
            KeyStore ksTrust = KeyStore.getInstance(trustedCertificatesFileContainer);
            ksTrust.load(trustedCertificatesFile, trustedCertificatesFilePassphrase);
            
            // KeyManager's decide which key material to use.
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ksKeys, serverCredentialFilePassphrase);
            
            // TrustManager's decide whether to allow connections.
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ksTrust);
            
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
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
    
    public String getServerCredentialFileContainer() {
        return this.serverCredentialFileContainer;
    }
    
    public String getServerCredentialFilePassphrase() {
        return this.serverCredentialFilePassphrase.toString();
    }
    
    public InputStream getTrustedCertificatesFile() {
        return this.trustedCertificatesFile;
    }
    
    public String getTrustedCertificatesFileContainer() {
        return this.trustedCertificatesFileContainer;
    }
    
    public String getTrustedCertificatesFilePassphrase() {
        return this.trustedCertificatesFilePassphrase.toString();
    }
    
    public SSLContext getSSLContext() {
        return this.sslContext;
    }

    public boolean isFakeSSLMode() {
        return this.useFakeSSLMode;
    }
}
