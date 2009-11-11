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

package org.xtreemfs.common.config;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;

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

    private boolean    useGRIDSSLmode;
    
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

        this.useGRIDSSLmode = this.readOptionalBoolean("ssl.grid_ssl", false);

        if (!this.useSSL && this.useGRIDSSLmode) {
            throw new RuntimeException("ssl must be enabled to use the grid_ssl mode. Please make sure to set ssl.enabled = true and to configure all SSL options.");
        }
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
    
}
