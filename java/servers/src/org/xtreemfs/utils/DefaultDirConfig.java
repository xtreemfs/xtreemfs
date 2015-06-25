/*
 * Copyright (c) 2008-2011 by Christian Lorenz, Bjoern Kolbeck,
 *                            Jan Stender, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.config.Config;
import org.xtreemfs.foundation.SSLOptions;

public class DefaultDirConfig extends Config {

    public static final String DEFAULT_DIR_CONFIG = "/etc/xos/xtreemfs/default_dir";

    private static final int   MAX_NUM_DIRS       = 5;

    private boolean            sslEnabled;

    protected String[]         directoryServices;

    private String             serviceCredsFile;

    private String             serviceCredsPassphrase;

    private String             serviceCredsContainer;

    private String             trustedCertsFile;

    private String             trustedCertsPassphrase;

    private String             trustedCertsContainer;
    
    private String             sslProtocolString;

    public DefaultDirConfig() throws IOException {
        super(DEFAULT_DIR_CONFIG);
        // TODO(lukas) this will throw NullPointerExcpetions
        directoryServices = null;
        read();
    }

    public String[] getDirectoryServices() {
        return directoryServices;
    }

    /**
     * Returns SSL Options or null if SSL is not enabled
     * 
     * @throws IOException
     * @throws FileNotFoundException
     */
    public SSLOptions getSSLOptions() throws FileNotFoundException, IOException {
        if (sslEnabled) {
            return new SSLOptions(serviceCredsFile, serviceCredsPassphrase, serviceCredsContainer, trustedCertsFile,
                    trustedCertsPassphrase, trustedCertsContainer, false, false, sslProtocolString, null);
        } else {
            return null;
        }
    }

    private void read() throws IOException {

        List<String> dirServices = new ArrayList<String>();

        // read required DIR service
        dirServices.add(this.readRequiredString("dir_service.host") + ":"
                + this.readRequiredString("dir_service.port"));

        // read optional DIR services
        for (int i = 1; i < MAX_NUM_DIRS; i++) {
            String dirHost = this.readOptionalString("dir_service" + (i + 1) + ".host", null);
            String dirPort = this.readOptionalString("dir_service" + (i + 1) + ".port", null);
            if (dirHost == null | dirPort == null) {
                break;
            }
            dirServices.add(dirHost + ":" + dirPort);
        }

        directoryServices = dirServices.toArray(new String[dirServices.size()]);

        this.sslEnabled = readOptionalBoolean("ssl.enabled", false);

        // read SSL settings if SSL is enabled
        if (sslEnabled) {
            this.serviceCredsFile = this.readRequiredString("ssl.service_creds");

            this.serviceCredsPassphrase = this.readRequiredString("ssl.service_creds.pw");

            this.serviceCredsContainer = this.readRequiredString("ssl.service_creds.container");

            this.trustedCertsFile = this.readRequiredString("ssl.trusted_certs");

            this.trustedCertsPassphrase = this.readRequiredString("ssl.trusted_certs.pw");

            this.trustedCertsContainer = this.readRequiredString("ssl.trusted_certs.container");
            
            this.sslProtocolString = this.readOptionalString("ssl.protocol", null);
        }
    }
}
