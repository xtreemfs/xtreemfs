/*
 * Copyright (c) 2008-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
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

import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

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
            
            KeyStore ksTrust = null;
            if (trustedCertificatesFileContainer.equals("none")) {
                ksTrust = KeyStore.getInstance(KeyStore.getDefaultType());
            } else {
                ksTrust = KeyStore.getInstance(trustedCertificatesFileContainer);
                ksTrust.load(trustedCertificatesFile, trustedCertificatesFilePassphrase);
            }
            
            // KeyManager's decide which key material to use.
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ksKeys, serverCredentialFilePassphrase);
            
            // TrustManager's decide whether to allow connections.
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ksTrust);
            
            sslContext = SSLContext.getInstance("TLS");
            if (trustedCertificatesFileContainer.equals("none")) {
                 TrustManager[] myTMs = new TrustManager [] {
                              new NoAuthTrustStore() };
                 sslContext.init(kmf.getKeyManagers(), myTMs, null);
            } else {
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

    private static class NoAuthTrustStore implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            //ignore
        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            //ignore
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }


    }
}
