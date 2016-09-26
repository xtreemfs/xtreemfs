/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.xtreemfs.common.config.ServiceConfig;

/**
 * 
 * @author bjko
 */
public class DIRConfig extends ServiceConfig {
    
    private final Parameter[] dirParameter = {
            Parameter.DEBUG_LEVEL,
            Parameter.DEBUG_CATEGORIES,
            Parameter.PORT,
            Parameter.BIND_RETRIES,
            Parameter.HTTP_PORT,
            Parameter.LISTEN_ADDRESS,
            Parameter.USE_SSL,
            Parameter.SSL_PROTOCOL_STRING,
            Parameter.SERVICE_CREDS_FILE,
            Parameter.SERVICE_CREDS_PASSPHRASE,
            Parameter.SERVICE_CREDS_CONTAINER,
            Parameter.TRUSTED_CERTS_FILE,
            Parameter.TRUSTED_CERTS_CONTAINER,
            Parameter.TRUSTED_CERTS_PASSPHRASE,
            Parameter.TRUST_MANAGER,
            Parameter.USE_GRID_SSL_MODE,
            Parameter.UUID,
            Parameter.WAIT_FOR_DIR,
            Parameter.ADMIN_PASSWORD,
            Parameter.AUTODISCOVER_ENABLED,
            Parameter.MONITORING_ENABLED,
            Parameter.ADMIN_EMAIL,
            Parameter.SENDER_ADDRESS,
            Parameter.MAX_WARNINGS,
            Parameter.TIMEOUT_SECONDS,
            Parameter.SENDMAIL_BIN,
            Parameter.POLICY_DIR,
            Parameter.USE_SNMP,
            Parameter.SNMP_ADDRESS,
            Parameter.SNMP_PORT,
            Parameter.SNMP_ACL,
            Parameter.MAX_CLIENT_Q,
            Parameter.VIVALDI_MAX_CLIENTS,
            Parameter.VIVALDI_CLIENT_TIMEOUT
    };
    
    private Map<String, Integer> mirrors;
    
    
    /** Creates a new instance of DIRConfig */
    public DIRConfig(String filename) throws IOException {
        super(filename);
        read();
    }
    
    public DIRConfig(Properties prop) throws IOException {
        super(prop);
        read();
    }
    
    public void read() throws IOException {
        
        for(Parameter param : dirParameter) {
            parameter.put(param, readParameter(param));
        }
        
        this.mirrors = new HashMap<String, Integer>();
        if (this.getAddress() != null) {
            this.mirrors.put(this.getAddress().getHostAddress(), this.getPort());
        }
        int id = 0;
        String host;
        InetSocketAddress addrs;
        int port;
        while ((host = this.readOptionalString("babudb.repl.participant." + id,null)) != null) {
            port = this.readRequiredInt("babudb.repl.participant." + id +
                                        ".dirPort");
            addrs = new InetSocketAddress(host, port);
            this.mirrors.put(addrs.getAddress().getHostAddress(), port);
            
            id++;
        }
    }

    /**
     * @return the autodiscoverEnabled
     */
    public boolean isAutodiscoverEnabled() {
        return (Boolean)parameter.get(Parameter.AUTODISCOVER_ENABLED);
    }

    /**
     * @return the monitoringEnabled
     */
    public boolean isMonitoringEnabled() {
        return (Boolean)parameter.get(Parameter.MONITORING_ENABLED);
    }

    /**
     * @return the adminEmail
     */
    public String getAdminEmail() {
        return (String)parameter.get(Parameter.ADMIN_EMAIL);
    }

    /**
     * @return the senderAddress
     */
    public String getSenderAddress() {
        return (String)parameter.get(Parameter.SENDER_ADDRESS);
    }

    /**
     * @return the maxWarnings
     */
    public int getMaxWarnings() {
        return (Integer)parameter.get(Parameter.MAX_WARNINGS);
    }

    /**
     * @return the timeoutSeconds
     */
    public int getTimeoutSeconds() {
        return (Integer)parameter.get(Parameter.TIMEOUT_SECONDS);
    }

    /**
     * @return the sendmailBin
     */
    public String getSendmailBin() {
        return (String)parameter.get(Parameter.SENDMAIL_BIN);
    }
    
    /**
     * @return the mirror DIRs
     */
    public Map<String, Integer> getMirrors() {
        return mirrors;
    }  
    public void setDefaults() {
        super.setDefaults(dirParameter);
    }
    
    /**
     * Check if the configuration contain all necessary values to start the Service
     */
    public void checkConfig() {
        super.checkConfig(dirParameter);
    }
    
    public int getVivaldiMaxClients() {
        return (Integer)parameter.get(Parameter.VIVALDI_MAX_CLIENTS);
    }
    
    public int getVivaldiClientTimeout() {
        return (Integer)parameter.get(Parameter.VIVALDI_CLIENT_TIMEOUT);
    }
}
