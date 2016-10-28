/*
 * Copyright (c) 2008-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.xtreemfs.common.config.ServiceConfig;
import org.xtreemfs.common.config.ServiceConfig.Parameter;
import org.xtreemfs.mrc.stages.XLocSetCoordinator;

/**
 * 
 * @author bjko
 */
public class MRCConfig extends ServiceConfig {
    /*
     * @formatter:off
     */
    final Parameter[] mrcParameter = {
            Parameter.DEBUG_LEVEL,
            Parameter.DEBUG_CATEGORIES,
            Parameter.PORT,
            Parameter.BIND_RETRIES,
            Parameter.HTTP_PORT,
            Parameter.LISTEN_ADDRESS,
            Parameter.HOSTNAME,
            Parameter.OSD_CHECK_INTERVAL,
            Parameter.DIRECTORY_SERVICE,
            Parameter.NOATIME,
            Parameter.LOCAL_CLOCK_RENEW,
            Parameter.REMOTE_TIME_SYNC,
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
            Parameter.GEO_COORDINATES,
            Parameter.AUTHENTICATION_PROVIDER,
            Parameter.AUTHENTICATION_PROVIDER_PROPERTIES,
            Parameter.POLICY_DIR,
            Parameter.CAPABILITY_SECRET,
            Parameter.CAPABILITY_TIMEOUT,
            Parameter.ADMIN_PASSWORD,
            Parameter.RENEW_TIMED_OUT_CAPS,
            Parameter.USE_SNMP,
            Parameter.SNMP_ADDRESS,
            Parameter.SNMP_PORT,
            Parameter.SNMP_ACL,
            Parameter.FAILOVER_MAX_RETRIES,
            Parameter.FAILOVER_WAIT,
            Parameter.MAX_CLIENT_Q,
            Parameter.USE_RENEWAL_SIGNAL,
            Parameter.USE_MULTIHOMING,
            Parameter.FLEASE_LEASE_TIMEOUT_MS
            };
    /*
     * @formatter:on
     */

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

    public int getOsdCheckInterval() {
        return (Integer) parameter.get(Parameter.OSD_CHECK_INTERVAL);
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

    public String getAuthenticationProvider() {
        return (String) parameter.get(Parameter.AUTHENTICATION_PROVIDER);
    }
    
    public String getAuthenticationProviderProperties() {
        return (String) parameter.get(Parameter.AUTHENTICATION_PROVIDER_PROPERTIES);
    }
    
    public Properties getAuthenticationProviderPropertiesAsProperties() {
        String propertiesString = getAuthenticationProviderProperties();
        Properties properties = new Properties();
        if (propertiesString != null) {
            String[] propertyStrings = propertiesString.split(",");
            for (String propertyString : propertyStrings) {
                String[] keyValue = propertyString.split(":");
                if (keyValue.length == 2) {
                    properties.setProperty(keyValue[0], keyValue[1]);
                }
            }
        }
        return properties;
    }

    public String getCapabilitySecret() {
        return (String) parameter.get(Parameter.CAPABILITY_SECRET);
    }

    public int getCapabilityTimeout() {
        return (Integer) parameter.get(Parameter.CAPABILITY_TIMEOUT);
    }

    /**
     * @return the renewTimedOutCaps
     */
    public boolean isRenewTimedOutCaps() {
        return (Boolean) parameter.get(Parameter.RENEW_TIMED_OUT_CAPS);

    }

    /**
     * Set default values according to the value in {@link Parameter} for all configuration parameter which
     * are null.
     */
    public void setDefaults() {
        super.setDefaults(mrcParameter);
    }

    /**
     * Check if the configuration contain all necessary values to start the service
     */
    public void checkConfig() {
        super.checkConfig(mrcParameter);
        checkMultihomingConfiguration();
    }

    /**
     * The flease lease timeout is needed for the {@link XLocSetCoordinator}.
     * 
     * @return the fleaseLeaseToMS
     */
    public int getFleaseLeaseToMS() {
        return (Integer) parameter.get(Parameter.FLEASE_LEASE_TIMEOUT_MS);
    }
}