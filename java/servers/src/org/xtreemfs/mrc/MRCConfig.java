/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Properties;


import org.xtreemfs.common.config.ServiceConfig;
import org.xtreemfs.common.config.ServiceConfig.Parameter;
import org.xtreemfs.common.uuids.ServiceUUID;
 


/**
 * 
 * @author bjko
 */
public class MRCConfig extends ServiceConfig {
    
    final Parameter[] mrcParameter = {
            Parameter.DEBUG_LEVEL,
            Parameter.DEBUG_CATEGORIES,
            Parameter.PORT,
            Parameter.HTTP_PORT,
            Parameter.LISTEN_ADDRESS,
            Parameter.HOSTNAME,
            Parameter.OSD_CHECK_INTERVAL,
            Parameter.DIRECTORY_SERVICE,
            Parameter.NOATIME,
            Parameter.LOCAL_CLOCK_RENEW,
            Parameter.REMOTE_TIME_SYNC,
            Parameter.USE_SSL,
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
            Parameter.GEO_COORDINATES,
            Parameter.AUTHENTICATION_PROVIDER,
            Parameter.POLICY_DIR,
            Parameter.CAPABILITY_SECRET,
            Parameter.CAPABILITY_TIMEOUT,
            Parameter.ADMIN_PASSWORD,
            Parameter.RENEW_TIMED_OUT_CAPS,
            Parameter.ENABLE_LOCAL_FIFOS,
            };
    
    /**
     * Parameter which are required to connect to the DIR.
     */
    final Parameter[] connectionParameter = {
            Parameter.DEBUG_CATEGORIES,
            Parameter.DEBUG_LEVEL,
            Parameter.HOSTNAME,
            Parameter.DIRECTORY_SERVICE,
            Parameter.WAIT_FOR_DIR,
            Parameter.PORT,
            Parameter.USE_SSL,
            Parameter.UUID
            };
    
    /** Creates a new instance of MRCConfig */
    public MRCConfig(String filename) throws IOException {
        super(filename);
        read();
    }
    
    public MRCConfig(Properties prop) throws IOException {
        super(prop);
        read();
    }
    
    public MRCConfig(HashMap<String, String> hm) {
    	super(hm);
    }
    
    public void read() throws IOException {

        for (Parameter parm : mrcParameter) {
            parameter.put(parm, readParameter(parm));
        }
    }
    
    /**
     * Checks if there are all required configuration parameter to initialize a connection to the
     * DIR and request the rest of the configuration
     * @return {@link Boolean}
     */
    public Boolean isInitializable() {
        
        for (Parameter param : connectionParameter) {
            if (parameter.get(param) == null) {
                throw new RuntimeException("property '" + param.getPropertyString()
                        + "' is required but was not found");  
            }
        }
        checkSSLConfiguration();
        return true;
    }
    
    public int getOsdCheckInterval() {
        return (Integer) parameter.get(Parameter.OSD_CHECK_INTERVAL);	
    }
    
    public InetSocketAddress getDirectoryService() {
        return (InetSocketAddress) parameter.get(Parameter.DIRECTORY_SERVICE);
    }
    
    public void setDirectoryService(InetSocketAddress addr) {
        parameter.put(Parameter.DIRECTORY_SERVICE, addr);
    }
    
    public boolean isNoAtime() {
        return (Boolean) parameter.get(Parameter.NOATIME);
    }
    
    public int getLocalClockRenew() {
    	return (Integer) parameter.get(Parameter.LOCAL_CLOCK_RENEW);
    }
    
    public int getRemoteTimeSync() {
    	return (Integer) parameter.get(Parameter.REMOTE_TIME_SYNC);
    }
    
    public ServiceUUID getUUID() {
        return (ServiceUUID) parameter.get(Parameter.UUID);
    }
    
    public String getAuthenticationProvider() {
        return (String)parameter.get(Parameter.AUTHENTICATION_PROVIDER);
    }
    
    public String getCapabilitySecret() {
        return (String) parameter.get(Parameter.CAPABILITY_SECRET);
    }
    
    public int getCapabilityTimeout() {
        return (Integer) parameter.get(Parameter.CAPABILITY_TIMEOUT);
    }
    
    public boolean isLocalFIFOsEnabled() {
        return (Boolean) parameter.get(Parameter.ENABLE_LOCAL_FIFOS);
    }
    
    /**
     * @return the renewTimedOutCaps
     */
    public boolean isRenewTimedOutCaps() {
    	return (Boolean)parameter.get(Parameter.RENEW_TIMED_OUT_CAPS);

    }
    
    /**
     * Set default values according to the value in {@link Parameter} for all configuration 
     * parameter which are null.
     */
    public void setDefaults() {
        super.setDefaults(mrcParameter);
    }
    /**
     * Return the required Parameter that are necessary for a connection to the DIR
     * @return {@link Parameter}[]
     */
    public Parameter[] getConnectionParameter() {
        return this.connectionParameter;
    }
    /**
     * Check if the configuration contain all necessary values to start the service
     */
    public void checkConfig() {
        super.checkConfig(mrcParameter);
    }
}