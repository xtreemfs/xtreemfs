/*
 * Copyright (c) 2008-2011 by Christian Lorenz, Bjoern Kolbeck,
 *                            Jan Stender, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.xtreemfs.common.config.Config;
import org.xtreemfs.dir.DIRConfig;

/**
 * @deprecated use {@link DIRConfig} instead!
 *
 */
@Deprecated
public class DefaultDirConfig extends Config {

    protected InetSocketAddress directoryService;

    private boolean sslEnabled;
    private String serviceCredsFile;

    private String serviceCredsPassphrase;

    private String serviceCredsContainer;

    private String trustedCertsFile;

    private String trustedCertsPassphrase;

    private String trustedCertsContainer;

    public DefaultDirConfig() {
            super();
    }

    public DefaultDirConfig(Properties prop) {
            super(prop);
    }

    public DefaultDirConfig(String filename) throws IOException {
            super(filename);
    }

    public void read() throws IOException {

        this.directoryService = this.readRequiredInetAddr("dir_service.host", "dir_service.port");
        
        this.sslEnabled = readOptionalBoolean("ssl.enabled", false);
        
        if(isSslEnabled()){
            this.serviceCredsFile = this.readRequiredString("ssl.service_creds");

            this.serviceCredsPassphrase = this.readRequiredString("ssl.service_creds.pw");

            this.serviceCredsContainer = this.readRequiredString("ssl.service_creds.container");

            this.trustedCertsFile = this.readRequiredString("ssl.trusted_certs");

            this.trustedCertsPassphrase = this.readRequiredString("ssl.trusted_certs.pw");

            this.trustedCertsContainer = this.readRequiredString("ssl.trusted_certs.container");
        }

    }

    public InetSocketAddress getDirectoryService() {
        return directoryService;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public String getServiceCredsFile() {
        return serviceCredsFile;
    }

    public String getServiceCredsPassphrase() {
        return serviceCredsPassphrase;
    }

    public String getServiceCredsContainer() {
        return serviceCredsContainer;
    }

    public String getTrustedCertsFile() {
        return trustedCertsFile;
    }

    public String getTrustedCertsPassphrase() {
        return trustedCertsPassphrase;
    }

    public String getTrustedCertsContainer() {
        return trustedCertsContainer;
    }

}
