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
 * AUTHORS: Christian Lorenz (ZIB), Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.config.Config;

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
