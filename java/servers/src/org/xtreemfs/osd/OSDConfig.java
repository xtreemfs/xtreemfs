/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


import org.xtreemfs.common.config.ServiceConfig;
import org.xtreemfs.common.config.ServiceConfig.Parameter;
import org.xtreemfs.common.uuids.ServiceUUID;

/**
 * 
 * @author bjko
 */
public class OSDConfig extends ServiceConfig {

    private final Parameter[] osdParameter = {            
            Parameter.DEBUG_LEVEL,
            Parameter.DEBUG_CATEGORIES,
            Parameter.PORT,
            Parameter.HTTP_PORT,
            Parameter.LISTEN_ADDRESS,
            Parameter.HOSTNAME,
            Parameter.DIRECTORY_SERVICE,
            Parameter.OBJECT_DIR,
            Parameter.LOCAL_CLOCK_RENEW,
            Parameter.REMOTE_TIME_SYNC,
            Parameter.USE_SSL,
            Parameter.SERVICE_CREDS_CONTAINER,
            Parameter.SERVICE_CREDS_FILE,
            Parameter.SERVICE_CREDS_PASSPHRASE,
            Parameter.TRUSTED_CERTS_CONTAINER,
            Parameter.TRUSTED_CERTS_FILE,
            Parameter.TRUSTED_CERTS_PASSPHRASE,
            Parameter.TRUST_MANAGER,
            Parameter.USE_GRID_SSL_MODE,
            Parameter.GEO_COORDINATES,
            Parameter.CHECKSUM_ENABLED,
            Parameter.CHECKSUM_PROVIDER,
            Parameter.ADMIN_PASSWORD,
            Parameter.WAIT_FOR_DIR,
            Parameter.UUID,
            Parameter.REPORT_FREE_SPACE,
            Parameter.STORAGE_LAYOUT,
            Parameter.IGNORE_CAPABILITIES,
            Parameter.FLEASE_DMAX_MS,
            Parameter.FLEASE_LEASE_TIMEOUT_MS,
            Parameter.FLEASE_MESSAGE_TO_MS,
            Parameter.FLEASE_RETRIES,
            Parameter.POLICY_DIR,
            Parameter.CAPABILITY_SECRET,
            Parameter.SOCKET_SEND_BUFFER_SIZE,
            Parameter.SOCKET_RECEIVE_BUFFER_SIZE
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
    
    public static final int     CHECKSUM_NONE    = 0;

    public static final int     CHECKSUM_ADLER32 = 1;

    public static final int     CHECKSUM_CRC32   = 2;

    private Map<String, String> customParams;

    /** Creates a new instance of OSDConfig */
    public OSDConfig(String filename) throws IOException {
        super(filename);
        read();
    }

    public OSDConfig(Properties prop) throws IOException {
        super(prop);
        read();
    }
    
    public OSDConfig(HashMap<String, String> hm) {
        super(hm);
    }

    public void read() throws IOException {

        this.customParams = new HashMap();
        for (String propName : this.props.stringPropertyNames()) {
            if (propName.startsWith("config.")) {
                customParams.put(propName, this.props.getProperty(propName));
            }
        }
 
        for (Parameter param: osdParameter) {
            parameter.put(param, readParameter(param));
        }
    }

    public InetSocketAddress getDirectoryService() {
        return (InetSocketAddress) parameter.get(Parameter.DIRECTORY_SERVICE);
    }

    public void setDirectoryService(InetSocketAddress addr) {
        parameter.put(Parameter.DIRECTORY_SERVICE, addr);
    }

    public String getObjDir() {
        return (String) parameter.get(Parameter.OBJECT_DIR);
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

    public boolean isReportFreeSpace() {
        return (Boolean) parameter.get(Parameter.REPORT_FREE_SPACE);
    }

    public void setReportFreeSpace(boolean reportFreeSpace) {
        parameter.put(Parameter.REPORT_FREE_SPACE, reportFreeSpace);
    }

    public String getChecksumProvider() {
        return (String) parameter.get(Parameter.CHECKSUM_PROVIDER);
    }

    public boolean isUseChecksums() {
        return (Boolean) parameter.get(Parameter.CHECKSUM_ENABLED);
    }

    public String getCapabilitySecret() {
        return (String) parameter.get(Parameter.CAPABILITY_SECRET);
    }

    /**
     * @return the ignoreCaps
     */
    public boolean isIgnoreCaps() {
            return (Boolean) parameter.get(Parameter.IGNORE_CAPABILITIES);
    }

    /**
     * @return the customParams
     */
    public Map<String, String> getCustomParams() {
        return customParams;
    }

    /**
     * @return the storageLayout
     */
    public String getStorageLayout() {
        return (String) parameter.get(Parameter.STORAGE_LAYOUT);
    }

    /**
     * @return the fleaseDmaxMS
     */
    public int getFleaseDmaxMS() {
        return (Integer) parameter.get(Parameter.FLEASE_DMAX_MS);
    }

    /**
     * @return the fleaseLeaseToMS
     */
    public int getFleaseLeaseToMS() {
        return (Integer) parameter.get(Parameter.FLEASE_LEASE_TIMEOUT_MS);
    }

    /**
     * @return the fleaseMsgToMS
     */
    public int getFleaseMsgToMS() {
        return (Integer) parameter.get(Parameter.FLEASE_MESSAGE_TO_MS);
    }

    /**
     * @return the fleaseRetries
     */
    public int getFleaseRetries() {
        return (Integer) parameter.get(Parameter.FLEASE_RETRIES);
    }

    /**
     * @param capabilitySecret
     *            the capabilitySecret to set
     */
    public void setCapabilitySecret(String capabilitySecret) {
        parameter.put(Parameter.CAPABILITY_SECRET, capabilitySecret);
    }
    
    public int getSocketSendBufferSize() {
        return (Integer) parameter.get(Parameter.SOCKET_SEND_BUFFER_SIZE);
    }
    
    public int getSocketReceiveBufferSize() {
        return (Integer) parameter.get(Parameter.SOCKET_RECEIVE_BUFFER_SIZE);
    }
    
    /**
     * Set default values according to the value in {@link Parameter} for all configuration 
     * parameter which are null.
     */
    public void setDefaults() {
        super.setDefaults(osdParameter);
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
        super.checkConfig(osdParameter);
    }
}
    

