/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.xtreemfs.common.config.ServiceConfig;

/**
 * 
 * @author bjko
 */
public class OSDConfig extends ServiceConfig {


    /*
     * @formatter:off
     */
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
            Parameter.SSL_PROTOCOL_STRING,
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
            Parameter.SOCKET_RECEIVE_BUFFER_SIZE,
            Parameter.USE_SNMP,
            Parameter.SNMP_ADDRESS,
            Parameter.SNMP_PORT,
            Parameter.SNMP_ACL,
            Parameter.FAILOVER_MAX_RETRIES,
            Parameter.FAILOVER_WAIT,
            Parameter.MAX_CLIENT_Q,
            Parameter.MAX_REQUEST_QUEUE_LENGTH,
            Parameter.VIVALDI_RECALCULATION_INTERVAL_IN_MS,
            Parameter.VIVALDI_RECALCULATION_EPSILON_IN_MS,
            Parameter.VIVALDI_ITERATIONS_BEFORE_UPDATING,
            Parameter.VIVALDI_MAX_RETRIES_FOR_A_REQUEST,
            Parameter.VIVALDI_MAX_REQUEST_TIMEOUT_IN_MS,
            Parameter.VIVALDI_TIMER_INTERVAL_IN_MS,
            Parameter.STORAGE_THREADS,
            Parameter.USE_RENEWAL_SIGNAL,
            Parameter.USE_MULTIHOMING,
            Parameter.HEALTH_CHECK,
            Parameter.EC_MAX_RECONSTRUCTION_QUEUE,
    };
    /*
     * @formatter:on   
     */
    public static final int           CHECKSUM_NONE    = 0;

    public static final int           CHECKSUM_ADLER32 = 1;

    public static final int           CHECKSUM_CRC32   = 2;

    private final Map<String, String> customParams;

    /** Creates a new instance of OSDConfig */
    public OSDConfig(String filename) throws IOException {
        super(filename);

        this.customParams = new HashMap<String, String>();
        read();
    }

    public OSDConfig(Properties prop) throws IOException {
        super(prop);

        this.customParams = new HashMap<String, String>();
        read();
    }

    public OSDConfig(HashMap<String, String> hm) {
        super(hm);

        this.customParams = new HashMap<String, String>();
        for (Entry<String, String> entry : hm.entrySet())
            if (entry.getKey().startsWith(OSD_CUSTOM_PROPERTY_PREFIX))
                customParams.put(entry.getKey(), entry.getValue());

    }

    public void read() throws IOException {

        for (String propName : this.props.stringPropertyNames()) {
            if (propName.startsWith(ServiceConfig.OSD_CUSTOM_PROPERTY_PREFIX)) {
                customParams.put(propName, this.props.getProperty(propName));
            }
        }

        for (Parameter param : osdParameter) {
            parameter.put(param, readParameter(param));
        }

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
     * Set default values according to the value in {@link Parameter} for all configuration parameter which
     * are null.
     */
    public void setDefaults() {
        super.setDefaults(osdParameter);
    }

    /**
     * Check if the configuration contain all necessary values to start the service
     */
    public void checkConfig() {
        super.checkConfig(osdParameter);
        checkMultihomingConfiguration();
    }

    public int getMaxClientQ() {
        return (Integer) parameter.get(Parameter.MAX_CLIENT_Q);
    }

    public int getMaxRequestsQueueLength() {
        return (Integer) parameter.get(Parameter.MAX_REQUEST_QUEUE_LENGTH);
    }

    public HashMap<String, String> toHashMap() {
        HashMap<String, String> hm = super.toHashMap();
        hm.putAll(customParams);
        return hm;
    }

    public int getVivaldiRecalculationInterval() {
        return (Integer) parameter.get(Parameter.VIVALDI_RECALCULATION_INTERVAL_IN_MS);
    }

    public int getVivaldiRecalculationEpsilon() {
        return (Integer) parameter.get(Parameter.VIVALDI_RECALCULATION_EPSILON_IN_MS);
    }

    public int getVivaldiIterationsBeforeUpdating() {
        return (Integer) parameter.get(Parameter.VIVALDI_ITERATIONS_BEFORE_UPDATING);
    }

    public int getVivaldiMaxRetriesForARequest() {
        return (Integer) parameter.get(Parameter.VIVALDI_MAX_RETRIES_FOR_A_REQUEST);
    }

    public int getVivaldiMaxRequestTimeout() {
        return (Integer) parameter.get(Parameter.VIVALDI_MAX_REQUEST_TIMEOUT_IN_MS);
    }

    public int getVivaldiTimerInterval() {
        return (Integer) parameter.get(Parameter.VIVALDI_TIMER_INTERVAL_IN_MS);
    }

    public int getStorageThreads() {
        return (Integer) parameter.get(Parameter.STORAGE_THREADS);
    }
    
    public String getHealthCheckScript() {
        return (String) parameter.get(Parameter.HEALTH_CHECK);
    }

    public int getECReconstructionQueueLength() {
        return (Integer) parameter.get(Parameter.EC_MAX_RECONSTRUCTION_QUEUE);
    }
}
