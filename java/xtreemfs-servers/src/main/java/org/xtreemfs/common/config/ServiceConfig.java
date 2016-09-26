/*
 * Copyright (c) 2008-2011 by Christian Lorenz, Bjoern Kolbeck,
 *               Jan Stender, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.Schemes;

public class ServiceConfig extends Config {

    final private static Category[] debugCategoryDefault = { Category.all };

    public static enum Parameter {
        /*
         * general configuration parameter
         */
        DEBUG_LEVEL("debug.level", 6, Integer.class, false),
        DEBUG_CATEGORIES("debug.categories", debugCategoryDefault, Category[].class, false),
        DIRECTORY_SERVICE("dir_service.host", null, InetSocketAddress.class, true),
        DIRECTORY_SERVICE0("dir_service.0.host", null, InetSocketAddress.class, false),
        DIRECTORY_SERVICE1("dir_service.1.host", null, InetSocketAddress.class, false),
        DIRECTORY_SERVICE2("dir_service.2.host", null, InetSocketAddress.class, false),
        DIRECTORY_SERVICE3("dir_service.3.host", null, InetSocketAddress.class, false),
        DIRECTORY_SERVICE4("dir_service.4.host", null, InetSocketAddress.class, false),
        PORT("listen.port", null, Integer.class, true),
        BIND_RETRIES("listen.port.bind_retries", 7, Integer.class, false),
        HTTP_PORT("http_port", null, Integer.class, true),
        LISTEN_ADDRESS("listen.address", null, InetAddress.class, false),
        USE_SSL("ssl.enabled", false, Boolean.class, false),
        SSL_PROTOCOL_STRING("ssl.protocol", null, String.class, false),
        SERVICE_CREDS_FILE("ssl.service_creds", null, String.class, false ),
        SERVICE_CREDS_PASSPHRASE("ssl.service_creds.pw", null, String.class, false),
        SERVICE_CREDS_CONTAINER("ssl.service_creds.container", null, String.class, false),
        TRUSTED_CERTS_FILE("ssl.trusted_certs", null, String.class, false),
        TRUSTED_CERTS_CONTAINER("ssl.trusted_certs.container", null, String.class, false),
        TRUSTED_CERTS_PASSPHRASE("ssl.trusted_certs.pw", null, String.class, false),
        TRUST_MANAGER("ssl.trust_manager", "", String.class, false),
        GEO_COORDINATES("geographic_coordinates", "", String.class, false ),
        ADMIN_PASSWORD("admin_password", "", String.class, false),
        HOSTNAME("hostname", "", String.class, false ),
        USE_GRID_SSL_MODE("ssl.grid_ssl", false, Boolean.class, false),
        WAIT_FOR_DIR("startup.wait_for_dir", 30, Integer.class, false),
        POLICY_DIR("policy_dir", "/etc/xos/xtreemfs/policies/", String.class, false),
        USE_SNMP("snmp.enabled", false, Boolean.class, false),
        SNMP_ADDRESS("snmp.address", null, InetAddress.class, false),
        SNMP_PORT("snmp.port", null, Integer.class, false),
        SNMP_ACL("snmp.aclfile", null, String.class, false),
        FAILOVER_MAX_RETRIES("failover.retries", 15, Integer.class, false),
        FAILOVER_WAIT("failover.wait_ms", 15 * 1000, Integer.class, false),
        MAX_CLIENT_Q("max_client_queue", 100, Integer.class, false),
        MAX_REQUEST_QUEUE_LENGTH("max_requests_queue_length", 1000, Integer.class, false),
        USE_MULTIHOMING("multihoming.enabled", false, Boolean.class, false),
        USE_RENEWAL_SIGNAL("multihoming.renewal_signal", false, Boolean.class, false ),

        /*
         * DIR specific configuration parameter
         */
        AUTODISCOVER_ENABLED("discover", true, Boolean.class, false),
        MONITORING_ENABLED("monitoring.enabled", false, Boolean.class, false ),
        ADMIN_EMAIL("monitoring.email.receiver", "", String.class, false),
        SENDER_ADDRESS("monitoring.email.sender", "XtreemFS DIR monitoring <dir@localhost>", String.class, false),
        MAX_WARNINGS("monitoring.max_warnings", 1, Integer.class, false),
        SENDMAIL_BIN("monitoring.email.programm", "/usr/sbin/sendmail", String.class, false),
        TIMEOUT_SECONDS("monitoring.service_timeout_s", 5 * 60, Integer.class, false),
        VIVALDI_MAX_CLIENTS("vivaldi.max_clients", 32, Integer.class, false),
        VIVALDI_CLIENT_TIMEOUT("vivaldi.client_timeout", 600000, Integer.class, false), // default: twice the recalculation interval


        /*
         * MRC specific configuration parameter
         */
        UUID("uuid", null, ServiceUUID.class, true),
        LOCAL_CLOCK_RENEW("local_clock_renewal", null, Integer.class, true),
        REMOTE_TIME_SYNC("remote_time_sync", null, Integer.class, true),
        OSD_CHECK_INTERVAL("osd_check_interval", null, Integer.class, true),
        NOATIME("no_atime", null, Boolean.class, true),
        AUTHENTICATION_PROVIDER("authentication_provider", null, String.class, true),
        CAPABILITY_SECRET("capability_secret", null, String.class, true),
        CAPABILITY_TIMEOUT("capability_timeout", 600, Integer.class, false),
        RENEW_TIMED_OUT_CAPS("renew_to_caps", false, Boolean.class, false),

        /*
         * OSD specific configuration parameter
         */
        OBJECT_DIR("object_dir", null, String.class, true),
        REPORT_FREE_SPACE("report_free_space", null, Boolean.class, true),
        CHECKSUM_ENABLED("checksums.enabled", false, Boolean.class, false),
        CHECKSUM_PROVIDER("checksums.algorithm", null, String.class, false),
        STORAGE_LAYOUT("storage_layout", "HashStorageLayout", String.class, false),
        IGNORE_CAPABILITIES("ignore_capabilities", false, Boolean.class, false),
        /** Maximum assumed drift between two server clocks. If the drift is higher, the system may not function properly. */
        FLEASE_DMAX_MS("flease.dmax_ms", 1000, Integer.class, false),
        FLEASE_LEASE_TIMEOUT_MS("flease.lease_timeout_ms", 14000, Integer.class, false),
        /** Message timeout. Maximum allowed in-transit time for a Flease message. */
        FLEASE_MESSAGE_TO_MS("flease.message_to_ms", 500, Integer.class, false),
        FLEASE_RETRIES("flease.retries", 3, Integer.class, false),
        SOCKET_SEND_BUFFER_SIZE("socket.send_buffer_size", -1, Integer.class, false),
        SOCKET_RECEIVE_BUFFER_SIZE("socket.recv_buffer_size", -1, Integer.class, false),
        VIVALDI_RECALCULATION_INTERVAL_IN_MS("vivaldi.recalculation_interval_ms", 300000, Integer.class, false),
        VIVALDI_RECALCULATION_EPSILON_IN_MS("vivaldi.recalculation_epsilon_ms", 30000, Integer.class, false),
        VIVALDI_ITERATIONS_BEFORE_UPDATING("vivaldi.iterations_before_updating", 12, Integer.class, false),
        VIVALDI_MAX_RETRIES_FOR_A_REQUEST("vivaldi.max_retries_for_a_request", 2, Integer.class, false),
        VIVALDI_MAX_REQUEST_TIMEOUT_IN_MS("vivaldi.max_request_timeout_ms", 10000, Integer.class, false),
        VIVALDI_TIMER_INTERVAL_IN_MS("vivaldi.timer_interval_ms", 60000, Integer.class, false),
        STORAGE_THREADS("storage_threads", 1, Integer.class, false),
        HEALTH_CHECK("health_check", "", String.class, false),

        /*
         * Benchmark specific configuration parameter
         */
        BASEFILE_SIZE_IN_BYTES("basefilesize_in_bytes", 3221225472L, Long.class, false), // 3221225472L = 3 GiB
        FILESIZE("filesize", 4096, Integer.class, false), // 4096 = 4 KiB
        USERNAME("username", "benchmark", String.class, false),
        GROUP("group", "benchmark", String.class, false),
        OSD_SELECTION_POLICIES("osd_selection_policies", "", String.class, false),
        REPLICATION_POLICY("replication_policy", "", String.class, false),
        REPLICATION_FACTOR("replication_factor", 3, Integer.class, false),
        CHUNK_SIZE_IN_BYTES("chunk_size_in_bytes", 131072, Integer.class, false), // 131072 = 128 KiB
        STRIPE_SIZE_IN_BYTES("stripe_size_in_bytes", 131072, Integer.class, false), // 131072 = 128 KiB
        STRIPE_SIZE_SET("stripe_size_set", false, Boolean.class, false),
        STRIPE_WIDTH("stripe_width", 1, Integer.class, false),
        STRIPE_WIDTH_SET("stripe_width_set", false, Boolean.class, false),
        NO_CLEANUP("no_cleanup", false, Boolean.class, false),
        NO_CLEANUP_VOLUMES("no_cleanup_volumes", false, Boolean.class, false),
        NO_CLEANUP_BASEFILE("no_cleanup_basefile", false, Boolean.class, false),
        OSD_CLEANUP("osd_cleanup", false, Boolean.class, false),
        USE_JNI("use_jni", false, Boolean.class, false);


        Parameter(String propString, Object defaultValue, Class propClass, Boolean req) {
            propertyString = propString;
            this.defaultValue = defaultValue;
            propertyClass = propClass;
            required = req;
        }

        /**
         * number of values the enumeration contains
         */
        private static final int size = Parameter.values().length;

        /**
         * String representation of the parameter in .property file
         */
        private final String     propertyString;

        /**
         * Class of the parameter. Used for deserilization. Note: If you add a new Class type, don't forget to
         * update the ServiceConfig(HashMap <String,String>) constructor
         */
        private final Class      propertyClass;

        /**
         * Default parameter which will be used if there is neither a Parameter in the properties file nor in
         * the DIR
         */
        private final Object     defaultValue;

        /**
         * True if this is a required parameter. False otherwise.
         */
        private final Boolean    required;

        public String getPropertyString() {
            return propertyString;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public Class getPropertyClass() {
            return propertyClass;
        }

        public static int getSize() {
            return size;
        }

        public Boolean isRequired() {
            return required;
        }

        public Boolean isOptional() {
            return !required;
        }

        public static Parameter getParameterFromString(String s) throws RuntimeException {
            for (Parameter parm : Parameter.values()) {
                if (s.equals(parm.getPropertyString()))
                    return parm;
            }
            throw new RuntimeException("Configuration parameter " + s + " doesn't exist!");
        }

    }
    /**
     * Parameter which are required to connect to the DIR.
     *
     */
    private final Parameter[] connectionParameter = {
            Parameter.DEBUG_CATEGORIES,
            Parameter.DEBUG_LEVEL,
            Parameter.HOSTNAME,
            Parameter.DIRECTORY_SERVICE,
            Parameter.WAIT_FOR_DIR,
            Parameter.PORT,
            Parameter.USE_SSL,
            Parameter.UUID
            };

    /**
     * Checks if there are all required configuration parameter to initialize a connection to the DIR and
     * request the rest of the configuration
     *
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

    public Parameter[] getConnectionParameter() {
        return this.connectionParameter;
    }

    /**
     * reads only the given Parameters from the config file
     *
     * @throws IOException
     */
    public void readParameters(Parameter[] params) throws IOException {
        for (Parameter param : params) {
            parameter.put(param, readParameter(param));
        }
        setDefaults(params);
    }

    protected EnumMap<Parameter, Object> parameter                  = new EnumMap<Parameter, Object>(
                                                                            Parameter.class);

    public static final String           OSD_CUSTOM_PROPERTY_PREFIX = "config.";

    public ServiceConfig() {
        super();
    }

    public ServiceConfig(Properties prop) {
        super(prop);
    }

    public ServiceConfig(String filename) throws IOException {
        super(filename);
    }

    public ServiceConfig(HashMap<String, String> hm) {
        super();

        /*
         * Create a configuration from String Key-Values of a HashMap
         */
        for (Entry<String, String> entry : hm.entrySet()) {

            // ignore custom configuration properties for OSDs here
            if (entry.getKey().startsWith(OSD_CUSTOM_PROPERTY_PREFIX)) {
                continue;
            }

            Parameter param = null;
            try {
                param = Parameter.getParameterFromString(entry.getKey());
            } catch (RuntimeException e) {
                e.printStackTrace();
            }

            /* Integer values */
            if (Integer.class == param.getPropertyClass()) {
                parameter.put(param, Integer.parseInt(entry.getValue()));
            }

            /* Long values */
            if (Long.class == param.getPropertyClass()) {
                parameter.put(param, Long.parseLong(entry.getValue()));
            }

            /* String values */
            if (String.class == param.getPropertyClass()) {
                parameter.put(param, entry.getValue());
            }

            /* Boolean values */
            if (Boolean.class == param.getPropertyClass()) {
                parameter.put(param, Boolean.valueOf(entry.getValue()));
            }

            /* ServiceUUID values */
            if (ServiceUUID.class == param.getPropertyClass()) {
                parameter.put(param, new ServiceUUID(entry.getValue()));
            }

            /* InetAddress values */
            if (InetAddress.class == param.getPropertyClass()) {

                InetAddress inetAddr = null;

                try {

                    inetAddr = InetAddress.getByName(entry.getValue().substring(
                            entry.getValue().indexOf('/') + 1));
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                parameter.put(param, inetAddr);
            }
            /* InetSocketAddress values */
            if (InetSocketAddress.class == param.getPropertyClass()) {

                /*
                 * Get a host and port of a string like 'hostname/192.168.2.141:36365' and create a
                 * InetSocketAddress
                 */
                String host = entry.getValue().substring(0, entry.getValue().indexOf("/"));
                String port = entry.getValue().substring(entry.getValue().lastIndexOf(":") + 1);
                InetSocketAddress isa = new InetSocketAddress(host, Integer.parseInt(port));

                parameter.put(param, isa);
            }

            /* Category[] values */
            if (Category[].class == param.getPropertyClass()) {

                StringTokenizer stk = new StringTokenizer(entry.getValue(), ", ");

                Category[] catArray = new Category[stk.countTokens()];
                int count = 0;
                while (stk.hasMoreElements()) {
                    catArray[count] = Category.valueOf(stk.nextToken());
                    count++;
                }

                parameter.put(param, catArray);
            }
        }
    }

    /**
     * Merges a second configuration in this one. Only required parameters which aren't set will be used from
     * the new configuration.
     *
     * @param conf
     */
    public void mergeConfig(ServiceConfig conf) {
        for (Entry<Parameter, Object> entry : conf.parameter.entrySet()) {
            if (entry.getKey().isRequired() && parameter.get(entry.getKey()) == null) {
                parameter.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Set the default value for a specific Parameter
     *
     * @param param
     *            - {@link Parameter}
     */
    public void setDefaults(Parameter param) {
        if (parameter.get(param) == null) {
            parameter.put(param, param.getDefaultValue());
        }
    }

    /**
     * Set the default values for the parameter in p
     *
     * @param p
     */
    public void setDefaults(Parameter[] p) {
        for (Parameter parm : p) {
            if (parm.isOptional() && parameter.get(parm) == null) {
                parameter.put(parm, parm.getDefaultValue());
            }
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

    /**
     * Read configuration parameter from property file and return an Object of the value if the parameter was
     * set. Else return null.
     *
     * @param param the parameter
     *
     * @return Object
     */
    protected Object readParameter(Parameter param) {

        String tmpString = props.getProperty(param.getPropertyString());
        if (tmpString == null) {
            return null;
        }

        // Integer values
        if (Integer.class == param.getPropertyClass()) {
            return Integer.parseInt(tmpString.trim());
        }


        /* Long values */
        if (Long.class == param.getPropertyClass()) {
            return(Long.parseLong(tmpString.trim()));
        }


        // Boolean values
        if (Boolean.class == param.getPropertyClass()) {
            return Boolean.parseBoolean(tmpString.trim());
        }

        // String values
        if (String.class == param.getPropertyClass()) {
            return tmpString.trim();
        }

        // ServiceUUID values
        if (ServiceUUID.class == param.getPropertyClass()) {
            return new ServiceUUID(tmpString);
        }

        // InetAddress values
        if (InetAddress.class == param.getPropertyClass()) {
            InetAddress iAddr = null;
            try {
                iAddr = InetAddress.getByName(tmpString);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return iAddr;
        }

        // InetSocketAddress values
        if (InetSocketAddress.class == param.getPropertyClass()) {
            // assumes that the parameter in the property file like
            // "foobar.host" and "foobar.port" if you
            // want to read a InetSocketAddress
            return readRequiredInetAddr(param.getPropertyString(),
                    param.getPropertyString().replaceAll("host", "port"));
        }

        // Category[] values
        if (Category[].class == param.getPropertyClass()) {
            return readCategories(param.getPropertyString());
        }

        return null;
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

    public HashMap<String, String> toHashMap() {

        HashMap<String, String> hm = new HashMap<String, String>();

        for (Parameter param : Parameter.values()) {
            if (parameter.get(param) != null) {

                if (Category[].class == param.getPropertyClass()) {
                    Category[] debugCategories = (Category[]) parameter.get(param);
                    String putString = "";

                    boolean firstValue = true;
                    for (Category cat : debugCategories) {
                        if (firstValue) {
                            putString = putString + cat.toString();
                            firstValue = false;
                        } else {
                            putString += ", " + cat.toString();
                        }
                    }

                    hm.put(param.getPropertyString(), putString);
                } else {
                    hm.put(param.getPropertyString(), parameter.get(param).toString());

                }
            }
        }
        return hm;

    }

    public int getDebugLevel() {
        return (Integer) parameter.get(Parameter.DEBUG_LEVEL);
    }

    public Category[] getDebugCategories() {
        return (Category[]) parameter.get(Parameter.DEBUG_CATEGORIES);
    }

    public int getPort() {
        return (Integer) parameter.get(Parameter.PORT);
    }

    public int getBindRetries() {
        return (Integer) parameter.get(Parameter.BIND_RETRIES);
    }

    public int getHttpPort() {
        return (Integer) parameter.get(Parameter.HTTP_PORT);
    }

    public InetAddress getAddress() {
        return (InetAddress) parameter.get(Parameter.LISTEN_ADDRESS);
    }

    public boolean isUsingSSL() {
        return (Boolean) parameter.get(Parameter.USE_SSL);
    }

    public String getServiceCredsContainer() {
        return (String) parameter.get(Parameter.SERVICE_CREDS_CONTAINER);
    }

    public String getServiceCredsFile() {
        return (String) parameter.get(Parameter.SERVICE_CREDS_FILE);
    }

    public String getServiceCredsPassphrase() {
        return (String) parameter.get(Parameter.SERVICE_CREDS_PASSPHRASE);
    }

    public String getTrustedCertsContainer() {
        return (String) parameter.get(Parameter.TRUSTED_CERTS_CONTAINER);
    }

    public String getTrustedCertsFile() {
        return (String) parameter.get(Parameter.TRUSTED_CERTS_FILE);
    }

    public String getTrustedCertsPassphrase() {
        return (String) parameter.get(Parameter.TRUSTED_CERTS_PASSPHRASE);
    }

    public String getTrustManager() {
        return (String) parameter.get(Parameter.TRUST_MANAGER);
    }

    public String getGeoCoordinates() {
        return (String) parameter.get(Parameter.GEO_COORDINATES);
    }

    public void setGeoCoordinates(String geoCoordinates) {
        parameter.put(Parameter.GEO_COORDINATES, geoCoordinates);
    }

    public String getAdminPassword() {
        return (String) parameter.get(Parameter.ADMIN_PASSWORD);
    }

    public String getHostName() {
        return (String) parameter.get(Parameter.HOSTNAME);
    }

    public ServiceUUID getUUID() {
        return (ServiceUUID) parameter.get(Parameter.UUID);
    }

    /**
     * @return the useFakeSSLmodeport
     */
    public boolean isGRIDSSLmode() {
        return parameter.get(Parameter.USE_GRID_SSL_MODE) != null
                && (Boolean) parameter.get(Parameter.USE_GRID_SSL_MODE);
    }
    
    public String getSSLProtocolString() {
        return (String) parameter.get(Parameter.SSL_PROTOCOL_STRING);
    }

    public int getWaitForDIR() {
        return (Integer) parameter.get(Parameter.WAIT_FOR_DIR);
    }

    public String getURLScheme() {
        if (isUsingSSL()) {
            if (isGRIDSSLmode()) {
                return Schemes.SCHEME_PBRPCG;
            } else {
                return Schemes.SCHEME_PBRPCS;
            }
        }
        return Schemes.SCHEME_PBRPC;
    }

    public String getPolicyDir() {
        return (String) parameter.get(Parameter.POLICY_DIR);
    }

    public Boolean isUsingSnmp() {
        return (Boolean) parameter.get(Parameter.USE_SNMP);
    }

    public InetAddress getSnmpAddress() {
        return (InetAddress) parameter.get(Parameter.SNMP_ADDRESS);
    }

    public Integer getSnmpPort() {
        return (Integer) parameter.get(Parameter.SNMP_PORT);
    }

    public String getSnmpACLFile() {
        return (String) parameter.get(Parameter.SNMP_ACL);
    }

    public Integer getFailoverMaxRetries() {
        return (Integer) parameter.get(Parameter.FAILOVER_MAX_RETRIES);
    }

    public Integer getFailoverWait() {
        return (Integer) parameter.get(Parameter.FAILOVER_WAIT);
    }
    
    public int getMaxClientQ() {
        return (Integer) parameter.get(Parameter.MAX_CLIENT_Q);
    }

    public InetSocketAddress getDirectoryService() {
        return (InetSocketAddress) parameter.get(Parameter.DIRECTORY_SERVICE);
    }

    public InetSocketAddress[] getDirectoryServices() {
        List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
        addresses.add((InetSocketAddress) parameter.get(Parameter.DIRECTORY_SERVICE));
        if (parameter.get(Parameter.DIRECTORY_SERVICE0) != null) {
            addresses.add((InetSocketAddress) parameter.get(Parameter.DIRECTORY_SERVICE0));
        }
        if (parameter.get(Parameter.DIRECTORY_SERVICE1) != null) {
            addresses.add((InetSocketAddress) parameter.get(Parameter.DIRECTORY_SERVICE1));
        }
        if (parameter.get(Parameter.DIRECTORY_SERVICE2) != null) {
            addresses.add((InetSocketAddress) parameter.get(Parameter.DIRECTORY_SERVICE2));
        }
        if (parameter.get(Parameter.DIRECTORY_SERVICE3) != null) {
            addresses.add((InetSocketAddress) parameter.get(Parameter.DIRECTORY_SERVICE3));
        }
        if (parameter.get(Parameter.DIRECTORY_SERVICE4) != null) {
            addresses.add((InetSocketAddress) parameter.get(Parameter.DIRECTORY_SERVICE4));
        }
        return addresses.toArray(new InetSocketAddress[0]);
    }

    public void setDirectoryService(InetSocketAddress addr) {
        parameter.put(Parameter.DIRECTORY_SERVICE, addr);
    }

    /**
     * Checks if the SSL Configuration is valid. If not throws a {@link RuntimeException}.
     *
     * @throws RuntimeException
     */
    public void checkSSLConfiguration() {

        Parameter[] sslRelatedParameter = { Parameter.SERVICE_CREDS_CONTAINER, Parameter.SERVICE_CREDS_FILE,
                Parameter.SERVICE_CREDS_PASSPHRASE, Parameter.TRUSTED_CERTS_CONTAINER,
                Parameter.TRUSTED_CERTS_FILE, Parameter.TRUSTED_CERTS_PASSPHRASE };

        if (isUsingSSL() == true) {
            for (Parameter param : sslRelatedParameter) {
                if (parameter.get(param) == null) {
                    throw new RuntimeException("for SSL " + param.getPropertyString() + " must be set!");
                }
            }
        } else {
            if (parameter.get(Parameter.USE_GRID_SSL_MODE) != null) {
                if (isGRIDSSLmode()) {
                    throw new RuntimeException(
                            "ssl must be enabled to use the grid_ssl mode. Please make sure to set ssl.enabled = true and to configure all SSL options.");
                }
            }
        }

    }

    /**
     * Checks if the multihoming configuration is valid. If not throws a {@link RuntimeException}.
     *
     * @throws RuntimeException
     */
    protected void checkMultihomingConfiguration() {
        if (isUsingMultihoming() && getAddress() != null) {
            throw new RuntimeException(ServiceConfig.Parameter.USE_MULTIHOMING.getPropertyString() + " and "
                    + ServiceConfig.Parameter.LISTEN_ADDRESS.getPropertyString() + " parameters are incompatible.");
        }
    }

    protected void checkConfig(Parameter[] params) {
        for (Parameter param : params) {
            if (param.isRequired() && parameter.get(param) == null) {
                throw new RuntimeException("property '" + param.getPropertyString()
                        + "' is required but was not found");

            }
        }
        this.checkSSLConfiguration();
    }

    public boolean isUsingRenewalSignal() {
        return (Boolean) parameter.get(Parameter.USE_RENEWAL_SIGNAL);
    }

    public boolean isUsingMultihoming() {
        return (Boolean) parameter.get(Parameter.USE_MULTIHOMING);
    }
}
