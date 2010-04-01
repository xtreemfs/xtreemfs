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
 * AUTHORS: Christian Lorenz (ZIB), Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.foundation.config;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.oncrpc.utils.XDRUtils;

public class ServiceConfig extends Config {
    
    protected int         debugLevel;
    
    protected Category[]  debugCategories;
    
    protected int         port;
    
    protected int         httpPort;
    
    protected InetAddress address;
    
    protected boolean     useSSL;
    
    protected String      serviceCredsFile;
    
    protected String      serviceCredsPassphrase;
    
    protected String      serviceCredsContainer;
    
    protected String      trustedCertsFile;
    
    protected String      trustedCertsPassphrase;
    
    protected String      trustedCertsContainer;
    
    protected String      geoCoordinates;
    
    protected String      adminPassword;
    
    protected String      hostname;

    protected boolean    useGRIDSSLmode;

    protected int         waitForDIR;
    
    public ServiceConfig() {
        super();
    }
    
    public ServiceConfig(Properties prop) {
        super(prop);
    }
    
    public ServiceConfig(String filename) throws IOException {
        super(filename);
    }
    
    public void read() throws IOException {
        
        this.debugLevel = readDebugLevel();
        
        this.port = this.readRequiredInt("listen.port");
        
        this.httpPort = this.readRequiredInt("http_port");
        
        this.address = this.readOptionalInetAddr("listen.address", null);
        
        if (this.useSSL = this.readRequiredBoolean("ssl.enabled")) {
            this.serviceCredsFile = this.readRequiredString("ssl.service_creds");
            
            this.serviceCredsPassphrase = this.readRequiredString("ssl.service_creds.pw");
            
            this.serviceCredsContainer = this.readRequiredString("ssl.service_creds.container");
            
            this.trustedCertsFile = this.readRequiredString("ssl.trusted_certs");
            
            this.trustedCertsPassphrase = this.readRequiredString("ssl.trusted_certs.pw");
            
            this.trustedCertsContainer = this.readRequiredString("ssl.trusted_certs.container");
        }
        
        this.geoCoordinates = this.readOptionalString("geographic_coordinates", "");
        
        this.adminPassword = this.readOptionalString("admin_password", "");
        
        this.debugCategories = this.readCategories("debug.categories");
        
        this.hostname = readOptionalString("hostname", "");
        this.hostname = this.hostname.trim();

        this.useGRIDSSLmode = this.readOptionalBoolean("ssl.grid_ssl", false);

        if (!this.useSSL && this.useGRIDSSLmode) {
            throw new RuntimeException("ssl must be enabled to use the grid_ssl mode. Please make sure to set ssl.enabled = true and to configure all SSL options.");
        }

        this.waitForDIR = this.readOptionalInt("startup.wait_for_dir", 30);
    }
    
    protected int readDebugLevel() {
        String level = props.getProperty("debug.level");
        if (level == null)
            return Logging.LEVEL_INFO;
        else {
            
            level = level.trim().toUpperCase();
            
            if (level.equals("EMERG")) {
                return Logging.LEVEL_EMERG;
            } else if (level.equals("ALERT")) {
                return Logging.LEVEL_ALERT;
            } else if (level.equals("CRIT")) {
                return Logging.LEVEL_CRIT;
            } else if (level.equals("ERR")) {
                return Logging.LEVEL_ERROR;
            } else if (level.equals("WARNING")) {
                return Logging.LEVEL_WARN;
            } else if (level.equals("NOTICE")) {
                return Logging.LEVEL_NOTICE;
            } else if (level.equals("INFO")) {
                return Logging.LEVEL_INFO;
            } else if (level.equals("DEBUG")) {
                return Logging.LEVEL_DEBUG;
            } else {
                
                try {
                    int levelInt = Integer.valueOf(level);
                    return levelInt;
                } catch (NumberFormatException ex) {
                    throw new RuntimeException("'" + level + "' is not a valid level name nor an integer");
                }
                
            }
            
        }
        
    }
    
    protected Category[] readCategories(String property) {
        
        String tmp = this.readOptionalString(property, "");
        StringTokenizer st = new StringTokenizer(tmp, " \t,");
        
        List<Category> cats = new LinkedList<Category>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            try {
                cats.add(Category.valueOf(token));
            } catch (IllegalArgumentException exc) {
                System.err.println("invalid logging category: " + token);
            }
        }
        
        if (cats.size() == 0)
            cats.add(Category.all);
        
        return cats.toArray(new Category[cats.size()]);
    }
    
    public int getDebugLevel() {
        return this.debugLevel;
    }
    
    public Category[] getDebugCategories() {
        return this.debugCategories;
    }
    
    public int getPort() {
        return this.port;
    }
    
    public int getHttpPort() {
        return this.httpPort;
    }
    
    public InetAddress getAddress() {
        return this.address;
    }
    
    public boolean isUsingSSL() {
        return this.useSSL;
    }
    
    public String getServiceCredsContainer() {
        return this.serviceCredsContainer;
    }
    
    public String getServiceCredsFile() {
        return this.serviceCredsFile;
    }
    
    public String getServiceCredsPassphrase() {
        return this.serviceCredsPassphrase;
    }
    
    public String getTrustedCertsContainer() {
        return this.trustedCertsContainer;
    }
    
    public String getTrustedCertsFile() {
        return this.trustedCertsFile;
    }
    
    public String getTrustedCertsPassphrase() {
        return this.trustedCertsPassphrase;
    }
    
    public String getGeoCoordinates() {
        return geoCoordinates;
    }
    
    public void setGeoCoordinates(String geoCoordinates) {
        this.geoCoordinates = geoCoordinates;
    }
    
    public String getAdminPassword() {
        return this.adminPassword;
    }
    
    public String getHostName() {
        return hostname;
    }

    /**
     * @return the useFakeSSLmode
     */
    public boolean isGRIDSSLmode() {
        return useGRIDSSLmode;
    }

    public int getWaitForDIR() {
        return waitForDIR;
    }

    public String getURLScheme() {
        if (isUsingSSL()) {
            if (isGRIDSSLmode()) {
                return XDRUtils.ONCRPCG_SCHEME;
            } else {
                return XDRUtils.ONCRPCS_SCHEME;
            }
        }
        return XDRUtils.ONCRPC_SCHEME;
    }
    
}
