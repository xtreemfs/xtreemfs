/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.benchmark;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.xtreemfs.common.config.PolicyContainer;
import org.xtreemfs.common.config.ServiceConfig;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.pbrpc.client.RPCAuthentication;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;

/**
 * @author jensvfischer
 */
public class BenchmarkConfig extends ServiceConfig {

    private static final Parameter[] benchmarkParameter = {
            Parameter.DEBUG_LEVEL,
            Parameter.DEBUG_CATEGORIES,
            Parameter.DIRECTORY_SERVICE,
            Parameter.USE_SSL,
            Parameter.SERVICE_CREDS_FILE,
            Parameter.SERVICE_CREDS_PASSPHRASE,
            Parameter.SERVICE_CREDS_CONTAINER,
            Parameter.TRUSTED_CERTS_FILE,
            Parameter.TRUSTED_CERTS_CONTAINER,
            Parameter.TRUSTED_CERTS_PASSPHRASE,
            Parameter.TRUST_MANAGER,
            Parameter.USE_GRID_SSL_MODE,
            Parameter.ADMIN_PASSWORD,
            Parameter.BASEFILE_SIZE_IN_BYTES,
            Parameter.FILESIZE,
            Parameter.USERNAME,
            Parameter.GROUP,
            Parameter.OSD_SELECTION_POLICIES,
            Parameter.REPLICATION_POLICY,
            Parameter.REPLICATION_FACTOR,
            Parameter.CHUNK_SIZE_IN_BYTES,
            Parameter.STRIPE_SIZE_IN_BYTES,
            Parameter.STRIPE_SIZE_SET,
            Parameter.STRIPE_WIDTH,
            Parameter.STRIPE_WIDTH_SET,
            Parameter.NO_CLEANUP,
            Parameter.NO_CLEANUP_VOLUMES,
            Parameter.NO_CLEANUP_BASEFILE,
            Parameter.OSD_CLEANUP,
            Parameter.USE_JNI
    };

    private Options                  options;
    private Map<String, String>      policyAttributes;
    private SSLOptions               sslOptions         = null;

    private BenchmarkConfig(Properties props, Options options, Map<String, String> policyAttributes) {
        super(props);
        read();
        this.options = options;
        this.policyAttributes = policyAttributes;
    }

    private void setDefaults() {
        super.setDefaults(benchmarkParameter);
    }

    private void read() {
        for(Parameter param : benchmarkParameter) {
            parameter.put(param, readParameter(param));
        }
    }

    public static Parameter[] getBenchmarkParameter() {
        return benchmarkParameter;
    }

    /**
     * Set the stripe size on an existing {@link BenchmarkConfig} object.
     *
     * @param size
     */
    public void setStripeSizeInBytes(Integer size) {
        parameter.put(Parameter.STRIPE_SIZE_IN_BYTES, size);
    }

    /**
     * Set the stripe size on an existing {@link BenchmarkConfig} object.
     *
     * @param size
     */
    public void setStripeWidth(Integer size) {
        parameter.put(Parameter.STRIPE_WIDTH, size);
    }


    /**
     * Get the size of the basefile for random benchmarks. <br/>
     * The basefile is a huge file to/from which the random benchmarks write/read. <br/>
     * Default: 3 GiB.
     * @return the size of the basefile for random benchmarks
     */
    public Long getBasefileSizeInBytes(){
        return (Long) parameter.get(Parameter.BASEFILE_SIZE_IN_BYTES);
    }

    /**
     * Get the size of files in filebased benchmarks. <br/>
     * Filebased benchmarks write/read a huge number of files. <br/>
     * Default: 4 KiB.
     * @return the size of files in filebased benchmarks
     */
    public Integer getFilesize(){
        return (Integer) parameter.get(Parameter.FILESIZE);
    }

    /**
     * Get the username to be used when creating files and volumes <br/>
     * Default: benchmark.
     * @return the username
     */
    public String getUsername(){
        return (String) parameter.get(Parameter.USERNAME);
    }


    /**
     * Get the group to be used when creating files and volumes. <br/>
     * Default: benchmark.
     * @return the group
     */
    public String getGroup(){
        return (String) parameter.get(Parameter.GROUP);
    }


    /**
     * Get the RPC user credentials, created from the username and group infos.
     * @return the RPC user credentials
     */
    public RPC.UserCredentials getUserCredentials() {
        return RPC.UserCredentials.newBuilder().setUsername((String) parameter.get(Parameter.USERNAME))
                .addGroups((String) parameter.get(Parameter.USERNAME)).build();
    }

    /**
     * Get the libxtreemfs {@link org.xtreemfs.common.libxtreemfs.Options}.
     *
     * @return the libxtreemfs {@link org.xtreemfs.common.libxtreemfs.Options}
     */
    public Options getOptions(){
        return options;
    }


    /**
     * Get the {@link SSLOptions} for the SSL Authetification Provider. <br/>
     * Default: null.
     * @return the {@link SSLOptions}
     */
    public SSLOptions getSslOptions() throws IOException, InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        if (isUsingSSL()) {
            if (null == sslOptions) {
                sslOptions = new SSLOptions(
                        this.getServiceCredsFile(),
                        this.getServiceCredsPassphrase(),
                        this.getServiceCredsContainer(),
                        this.getTrustedCertsFile(),
                        this.getTrustedCertsPassphrase(),
                        this.getTrustedCertsContainer(),
                        false,
                        this.isGRIDSSLmode(),
                        this.getSSLProtocolString(),
                        new PolicyContainer(this).getTrustManager()
                );
            }
        }
        return sslOptions;
    }

    /**
     * Get the OSD selection policies used when creating or opening volumes. <br/>
     *
     * Default: No policy is set. If an existing volume is used this means, that already set policies of the volume are
     * used. If a new volume is created, the defaults are used ("1000,3002": OSD filter, Shuffling).
     *
     * @return the OSD selection policies
     */
    public String getOsdSelectionPolicies(){
        return (String) parameter.get(Parameter.OSD_SELECTION_POLICIES);
    }


    /**
     * Get the policy attributes for OSD selection policies. <p/>
     * The attributes are set when the volumes are created / opened. <br/>
     * A policy attribute consists of the name of the attribute, and the value the attribute is set to. For more
     * information see the XtreemFS User Guide. <br/>
     * 
     * Attribute Format: <policy id>.<attribute name> e.g., "1002.uuids" <br/>
     * Value format: <value>, e.g. "osd01"
     * 
     * @return the policy attributes
     */
    public Map<String, String> getPolicyAttributes(){
        return this.policyAttributes;
    }

    /**
     * Get the default replication policy (used when creating or opening volumes). <br/>
     * As the {@code replicationFlags} in
     * {@link org.xtreemfs.common.libxtreemfs.Volume#setDefaultReplicationPolicy(org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials, String, String, int, int)}
     * is set to 0, this is only intended for write/read replication. <br/>
     * 
     * Default: No policy is set. If an existing volume is used this means, that a already set policy of the volume is
     * used. If a new volume is created, the defaults (no replication policy) is used.
     * 
     * @return the replication policy
     */
    public String getReplicationPolicy(){
        return (String) parameter.get(Parameter.REPLICATION_POLICY);
    }

    /**
     * Get the replication factor for the replication policy. <br/>
     * Only used when explicitly setting a replication policy for a volume. <br/>
     *
     * Default: 3 (min for WqRq)
     *
     * @return the replication policy
     */
    public Integer getReplicationFactor(){
        return (Integer) parameter.get(Parameter.REPLICATION_FACTOR);
    }

    /**
     * Get the chunk size for reads/writes in benchmarks. <br/>
     * The chuck size is the amount of data written/ret in one piece. <br/>
     * Default: 131072 bytes (128 KiB).
     * 
     * @return the chunk size
     */
    public Integer getChunkSizeInBytes(){
        return (Integer) parameter.get(Parameter.CHUNK_SIZE_IN_BYTES);
    }

    /**
     * Get the size of an OSD storage block ("blocksize") in bytes. <br/>
     * Used when creating or opening volumes. <br/>
     *
     * When opening existing volumes, by default, the stripe size of the given volume is used. When creating a new
     * volume, by default a stripe size of 131072 bytes (128 KiB) is used. <br/>
     * @return the blocksize
     */
    public Integer getStripeSizeInBytes(){
        return (Integer) parameter.get(Parameter.STRIPE_SIZE_IN_BYTES);
    }

    /**
     * Get the size of an OSD storage block ("blocksize") in kibibytes. <br/>
     *
     * @return the blocksize in KiB
     */
    public Integer getStripeSizeInKiB() {
        return getStripeSizeInBytes()/1024;
    }

    /**
     * Indicates, whether a stripeSize was explicitly set via this config, or if the default values where used.
     * @return true, if stripeSize was explicitly set
     */
    public Boolean isStripeSizeSet(){
        return (Boolean) parameter.get(Parameter.STRIPE_SIZE_SET);
    }

    /**
     * Get the maximum number of OSDs a file is distributed to. Used when creating or opening volumes <br/>
     *
     * When opening existing volumes, by default, the stripe width of the given volume is used. When creating a new
     * volume, by default a stripe width of 1 is used. <br/>
     *
     * @return the maximum number of OSDs a file is distributed to
     */
    public Integer getStripeWidth(){
        return (Integer) parameter.get(Parameter.STRIPE_WIDTH);
    }


    /**
     * Indicates, whether a stripeWidth was explicitly set via this config, or if the default values where used.
     * @return
     */
    public Boolean isStripeWidthSet(){
        return (Boolean) parameter.get(Parameter.STRIPE_WIDTH_SET);
    }

    /**
     * Indicates whether the files and volumes created during the benchmarks will be deleted. <br/>
     * Default: false.
     * @return true, if created files and volumes are not to be deleted
     */
    public Boolean isNoCleanup(){
        return (Boolean) parameter.get(Parameter.NO_CLEANUP);
    }

    /**
     * Indicates, whether the volumes created during the benchmarks will be deleted. <br/>
     * Default: false.
     * @return true, if created volumes are not to be deleted
     */
    public Boolean isNoCleanupVolumes(){
        return (Boolean) parameter.get(Parameter.NO_CLEANUP_VOLUMES);
    }

    /**
     * Indicates, whether the basefile created during the random benchmarks will be deleted. <br/>
     * Default: false.
     * @return true, if a created basefile is not to be deleted
     */
    public Boolean isNoCleanupBasefile(){
        return (Boolean) parameter.get(Parameter.NO_CLEANUP_BASEFILE);
    }

    /**
     * Indicates, whether a OSD Cleanup will be done at the end of all benchmarks. This might be needed to actually delete
     * the storage blocks from the OSD after deleting volumes. <br/>
     * Default: false.
     * @return true, if a cleanup is to be performed
     */
    public Boolean isOsdCleanup(){
        return (Boolean) parameter.get(Parameter.OSD_CLEANUP);
    }

    /**
     * Get addresses of the DIR Servers. <br/>
     * Default: 127.0.0.1:32638
     * @return
     */
    public String[] getDirAddresses() {
        InetSocketAddress[] directoryServices = getDirectoryServices();
        String[] dirAddresses = new String[directoryServices.length];
        for (int i =0; i<dirAddresses.length; i++)
            dirAddresses[i] = directoryServices[i].getAddress().getHostAddress() + ":" + directoryServices[i].getPort();
        return dirAddresses;
    }


    /**
     *
     * @return
     */
    public RPC.Auth getAuth() {
        RPC.Auth auth;
        if (getAdminPassword().equals(""))
            auth = RPCAuthentication.authNone;
        else {
            RPC.AuthPassword password = RPC.AuthPassword.newBuilder().setPassword(getAdminPassword()).build();
            auth = RPC.Auth.newBuilder().setAuthType(RPC.AuthType.AUTH_PASSWORD).setAuthPasswd(password).build();
        }
        return auth;
    }

    public Boolean isUsingJNI() {
        return (Boolean) parameter.get(Parameter.USE_JNI);
    }


    /**
     * Return a new builder to build a {@link BenchmarkConfig} object.
     *
     * @return a new builder
     */
    public static ConfigBuilder newBuilder(){
        return new ConfigBuilder();
    }


    /**
     * Builder for the {@link BenchmarkConfig} datastructure.
     * <p/>
     *
     * Use like this: <br/>
     * <code>
     * BenchmarkConfig.ConfigBuilder builder = BenchmarkConfig.newBuilder();<br/>
     * builder.setX("x"); <br/>
     * builder.setY("y"); <br/>
     * BenchmarkConfig config = builder.build(); <br/>
     * </code> or like this <br/>
     * <code>
     * BenchmarkConfig config = BenchmarkConfig.newBuilder().setX("x").setY("y").build();<br/>
     * </code>
     * <p/>
     * The {@link Controller} and the {@link BenchmarkConfig} represent the API to the benchmark library.
     *
     * @author jensvfischer
     */
    public static class ConfigBuilder {

        private Properties          props            = new Properties();
        private ServiceConfig       parent;
        private Map<String, String> policyAttributes = new HashMap<String, String>();
        private Options             options          = new Options();


        /**
         * Instantiate an builder (all values are the default values, see {@link BenchmarkConfig}).
         */
        private ConfigBuilder() {
        }

        /**
         * Set the size of the basefile for random benchmarks. <br/>
         * Default: 3 GiB.
         *
         * @param basefileSizeInBytes
         * @return the builder
         */
        public ConfigBuilder setBasefileSizeInBytes(long basefileSizeInBytes) {
            if (basefileSizeInBytes < 1)
                throw new IllegalArgumentException("basefileSizeInBytes < 1 not allowed");
            props.setProperty(Parameter.BASEFILE_SIZE_IN_BYTES.getPropertyString(),
                    Long.toString(basefileSizeInBytes));
            return this;
        }

        /**
         * Set the size of files in filebased benchmark. <br/>
         * Default: 4 KiB.
         *
         * @param filesize
         * @return the builder
         */
        public ConfigBuilder setFilesize(int filesize) {
            if (filesize < 1)
                throw new IllegalArgumentException("filesize < 1 not allowed");
            props.setProperty(Parameter.FILESIZE.getPropertyString(), Integer.toString(filesize));
            return this;
        }

        /**
         * Set the username to be used when creating files and volumes <br/>
         * Default: benchmark.
         *
         * @param userName
         * @return the builder
         */
        public ConfigBuilder setUserName(String userName) {
            if (userName.isEmpty())
                throw new IllegalArgumentException("Empty username not allowed");
            props.setProperty(Parameter.USERNAME.getPropertyString(), userName);
            return this;
        }

        /**
         * Set the group to be used when creating files and volumes. <br/>
         * Default: benchmark.
         *
         * @param group
         * @return the builder
         */
        public ConfigBuilder setGroup(String group) {
            if (group.isEmpty())
                throw new IllegalArgumentException("Empty group name not allowed");
            props.setProperty(Parameter.USERNAME.getPropertyString(), group);
            return this;
        }

        /**
         * Set the password for accessing the osd(s) <br/>
         * Default: "".
         *
         * @param adminPassword
         * @return the builder
         */
        public ConfigBuilder setAdminPassword(String adminPassword) {
            props.setProperty(Parameter.ADMIN_PASSWORD.getPropertyString(), adminPassword);
            return this;
        }


        /**
         * Set the address of the DIR Server. <br/>
         * Default: 127.0.0.1:32638
         *
         * @param dirAddress
         * @return the builder
         */
        public ConfigBuilder setDirAddress(String dirAddress) {
            return setDirAddresses(new String[]{dirAddress});
        }


        /**
         * Set the addresses of the DIR Servers. <br/>
         * Default: 127.0.0.1:32638
         *
         * @param dirAddresses
         * @return the builder
         */
        public ConfigBuilder setDirAddresses(String[] dirAddresses) {
            int i =-1;
            for (String dirAddress : dirAddresses) {
                
                /* remove protocol information */
                if (dirAddress.contains("://"))
                    dirAddress = dirAddress.split("://", 2)[1];
                
                /* remove trailing slashes */
                if (dirAddress.endsWith("/"))
                    dirAddress = dirAddress.substring(0, dirAddress.length() - 1);

                /* split address in host and port */
                String host;
                String port;
                try {
                    host = dirAddress.split(":")[0];
                    port = dirAddress.split(":")[1];
                } catch (IndexOutOfBoundsException e) {
                    throw new IllegalArgumentException(
                            "DIR Address needs to contain a host and a port, separated by \":\" (was: \"" + dirAddress
                                    + "\").");
                }
                if (dirAddresses.length == 1 || -1 == i) {
                    props.setProperty("dir_service.host", host);
                    props.setProperty("dir_service.port", port);                    
                } else {
                    props.setProperty("dir_service." + i + ".host", host);
                    props.setProperty("dir_service." + i + ".port", port);
                }
                i++;
            }
            return this;
        }

        /**
         * Set the libxtreemfs {@link org.xtreemfs.common.libxtreemfs.Options}.
         *
         * @param options
         * @return the builder
         */
        public ConfigBuilder setOptions(Options options) {
            this.options = options;
            return this;
        }

        /**
         * Set the SSL options for SSL Authetification Provider. <br/>
         * Default: null.
         *
         * @param useSSL set to true to use SSL, false otherwise
         * @param useGridSSL set to true to use GridSSL, false otherwise
         * @param serviceCredsFile the pkcs12 file with the ssl user certificate
         * @param serviceCredsFile the passphrase for the user certificate
         * @param trustedCAsFile jks truststore with the CA
         * @param trustedCAsPass passphrase for the jks truststore
         */                                                                     
        public ConfigBuilder setSslOptions(boolean useSSL, boolean useGridSSL, String serviceCredsFile,
                String serviceCredsPass, String trustedCAsFile, String trustedCAsPass) {
            props.setProperty(Parameter.USE_SSL.getPropertyString(), Boolean.toString(useSSL));
            props.setProperty(Parameter.USE_GRID_SSL_MODE.getPropertyString(), Boolean.toString(useGridSSL));
            props.setProperty(Parameter.SERVICE_CREDS_FILE.getPropertyString(), serviceCredsFile);
            props.setProperty(Parameter.SERVICE_CREDS_PASSPHRASE.getPropertyString(), serviceCredsPass);
            props.setProperty(Parameter.SERVICE_CREDS_CONTAINER.getPropertyString(), SSLOptions.PKCS12_CONTAINER);
            props.setProperty(Parameter.TRUSTED_CERTS_FILE.getPropertyString(), trustedCAsFile);
            props.setProperty(Parameter.TRUSTED_CERTS_CONTAINER.getPropertyString(), SSLOptions.JKS_CONTAINER);
            props.setProperty(Parameter.TRUSTED_CERTS_PASSPHRASE.getPropertyString(), trustedCAsPass);
            return this;
        }

        /**
         * Set the OSD selection policies used when creating or opening volumes. <br/>
         *
         * Default: No policy is set. If an existing volume is used this means, that already set policies of the volume are
         * used. If a new volume is created, the defaults are use ("1000,3002": OSD filter, Shuffling).
         *
         * @param policies
         * @return the builder
         */
        public ConfigBuilder setOsdSelectionPolicies(String policies) {
            props.setProperty(Parameter.OSD_SELECTION_POLICIES.getPropertyString(), policies);
            return this;
        }

        /**
         * Set a policy attribute for a OSD selection policies. <p/>
         * This method can be called multiple times, if multiple attributes are to be set. <br/>
         * The attributes are set when the volumes are created / opened. <br/>
         * A policy attribute consists of the name of the attribute, and the value the attribute is set to. For more information see the XtreemFS User Guide. <br/>
         *
         * Attribute Format: <policy id>.<attribute name> e.g., "1002.uuids" <br/>
         * Value format: <value>, e.g. "osd01"
         *
         * @param attribute the attribute to be set
         * @param value the value the attribute is set to
         * @return the builder
         */
        public ConfigBuilder setPolicyAttribute(String attribute, String value) {
            this.policyAttributes.put(attribute, value);
            return this;
        }

        /**
         * Set the UUID-based filter policy (ID 1002) as OSD selection policy and set the uuids to be used by the policy
         * (applied when creating/opening the volumes). It is a shortcut for setting the policy and the attributes manually. <br/>
         *
         * Default: see {@link #setOsdSelectionPolicies(String)}.
         *
         * @param uuids
         *            the uuids of osds to be used
         * @return the builder
         */
        public ConfigBuilder setSelectOsdsByUuid(String uuids) {
            String key = Parameter.OSD_SELECTION_POLICIES.getPropertyString();
            String osdSelectionPolicies = props.getProperty(key);
            if (null == osdSelectionPolicies)
                props.setProperty(key, "1002");
            else
                props.setProperty(key, osdSelectionPolicies+",1002");
            this.policyAttributes.put("1002.uuids", uuids);
            return this;
        }

        /**
         * Set the default replication policy, used when creating or opening volumes. <br/>
         * As the {@code replicationFlags} in
         * {@link org.xtreemfs.common.libxtreemfs.Volume#setDefaultReplicationPolicy(org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials, String, String, int, int)}
         * is set to 0, this is only intended for write/read replication. <br/>
         *
         * Default: No policy is set. If an existing volume is used this means, that a already set policy of the volume is
         * used. If a new volume is created, the default (no replication) is used.
         *
         * @param policy
         * @return the builder
         */
        public ConfigBuilder setReplicationPolicy(String policy) {
            props.setProperty(Parameter.REPLICATION_POLICY.getPropertyString(), policy);
            return this;
        }

        /**
         * Set the replication factor for the replication policy. <br/>
         * Only used when explicitly setting a replication policy for a volume. <br/>
         *
         * Default: 3 (min for WqRq)
         *
         * @param replicationFactor
         * @return the builder
         */
        public ConfigBuilder setReplicationFactor(int replicationFactor) {
            props.setProperty(Parameter.REPLICATION_FACTOR.getPropertyString(), Integer.toString(replicationFactor));
            return this;
        }

        /**
         * Set the chunk size for reads/writes in benchmarks. The chuck size is the amount of data written/ret in one piece. <br/>
         *
         * Default: 131072 bytes (128 KiB).
         *
         * @param chunkSizeInBytes the chunk size in bytes
         * @return the builder
         */
        public ConfigBuilder setChunkSizeInBytes(int chunkSizeInBytes) {
            props.setProperty(Parameter.CHUNK_SIZE_IN_BYTES.getPropertyString(), Integer.toString(chunkSizeInBytes));
            return this;
        }

        /**
         * Set the size of an OSD storage block ("blocksize") in bytes when creating or opening volumes. <br/>
         *
         * When opening existing volumes, by default, the stripe size of the given volume is used. When creating a new
         * volume, by default a stripe size of 131072 bytes (128 KiB) is used. <br/>
         *
         * @param stripeSizeInBytes
         *            the stripe size in bytes
         * @return the builder
         */
        public ConfigBuilder setStripeSizeInBytes(int stripeSizeInBytes) {
            props.setProperty(Parameter.STRIPE_SIZE_IN_BYTES.getPropertyString(), Integer.toString(stripeSizeInBytes));
            props.setProperty(Parameter.STRIPE_SIZE_SET.getPropertyString(), Boolean.toString(true));
            return this;
        }

        /**
         * Set the maximum number of OSDs a file is distributed to. Used when creating or opening volumes <br/>
         *
         * When opening existing volumes, by default, the stripe width of the given volume is used. When creating a new
         * volume, by default a stripe width of 1 is used. <br/>
         * @return the builder
         */
        public ConfigBuilder setStripeWidth(int stripeWidth) {
            props.setProperty(Parameter.STRIPE_WIDTH.getPropertyString(), Integer.toString(stripeWidth));
            props.setProperty(Parameter.STRIPE_WIDTH_SET.getPropertyString(), Boolean.toString(true));
            return this;
        }

        /**
         * If set, the files and volumes created during the benchmarks will not be deleted. <br/>
         * Default: false.
         *
         * @return the builder
         */
        public ConfigBuilder setNoCleanup() {
            props.setProperty(Parameter.NO_CLEANUP.getPropertyString(), Boolean.toString(true));
            return this;
        }

        /**
         * If set, the volumes created during the benchmarks will not be deleted. <br/>
         * Default: false.
         *
         * @return the builder
         */
        public ConfigBuilder setNoCleanupVolumes() {
            props.setProperty(Parameter.NO_CLEANUP_VOLUMES.getPropertyString(), Boolean.toString(true));
            return this;
        }

        /**
         * If set, a basefile created during benchmarks will not be deleted. <br/>
         * Default: false.
         *
         * @return the builder
         */
        public ConfigBuilder setNoCleanupBasefile() {
            props.setProperty(Parameter.NO_CLEANUP_BASEFILE.getPropertyString(), Boolean.toString(true));
            return this;
        }

        /**
         * If set, a OSD Cleanup will be done at the end of all benchmarks. This might be needed to actually delete
         * the storage blocks from the OSD after deleting volumes. <br/>
         * Default: false.
         *
         * @return the builder
         */
        public ConfigBuilder setOsdCleanup() {
            props.setProperty(Parameter.OSD_CLEANUP.getPropertyString(), Boolean.toString(true));
            return this;
        }

        /**
         * If set, the Benchmark is performed using JNI (and thus the C++ library) instead of the pure Java library.
         * Default: false.
         * 
         * @return the builder
         */
        public ConfigBuilder setUseJNI() {
            props.setProperty(Parameter.USE_JNI.getPropertyString(), Boolean.toString(true));
            return this;
        }

        /**
         * If set, the {@link BenchmarkConfig} will be constructed by using as many parameters as possible from the parent
         * config. <p/>
         * Only parameters which weren't set at the current builder object (at the moment of the call to
         * {@link org.xtreemfs.common.benchmark.BenchmarkConfig.ConfigBuilder#build()} will be taken from the parent.
         *
         * @param parent
         * @return
         */
        public ConfigBuilder setParent(ServiceConfig parent) {
            this.parent = parent;
            return this;
        }


        /**
         * Build the {@link BenchmarkConfig} object.
         *
         * @return the build {@link BenchmarkConfig} object
         * @throws Exception
         */
        public BenchmarkConfig build() throws Exception {
            verifyNoCleanup();
            if (null != this.parent)
                mergeParent();

            /*
             * if no DirAddress is given, either directly or in the parent config, first try the DefaultDirConfig, then
             * use default
             */
            if (null == props.getProperty("dir_service.host")) {
                String[] dirAddresses = Controller.getDefaultDir();
                if (null != dirAddresses)
                    setDirAddresses(dirAddresses);
                else
                    setDirAddresses(new String[]{"127.0.0.1:32638"});
            }

            BenchmarkConfig config = new BenchmarkConfig(props, this.options, this.policyAttributes);
            config.setDefaults();

            return config;
        }

        private void verifyNoCleanup() {
            boolean noCleanupBasefile, noCleanup, noCleanupVolumes;
            noCleanupBasefile = Boolean.parseBoolean(props.getProperty(Parameter.NO_CLEANUP_BASEFILE.getPropertyString()));
            noCleanup = Boolean.parseBoolean(props.getProperty(Parameter.NO_CLEANUP.getPropertyString()));
            noCleanupVolumes = Boolean.parseBoolean(props.getProperty(Parameter.NO_CLEANUP_VOLUMES.getPropertyString()));
            if (noCleanupBasefile && !noCleanup && !noCleanupVolumes)
                throw new IllegalArgumentException("noCleanupBasefile only works with noCleanup or noCleanupVolumes");
        }


        /* Merge props with parent props. Current props has precedence over parent */
        private void mergeParent() {
            HashMap<String, String> parentParameters = parent.toHashMap();
            for (Map.Entry<String, String> parentEntry : parentParameters.entrySet()) {
                String parentKey = parentEntry.getKey();
                String parentValue = parentEntry.getValue();

                /* only set hitherto unset properties */
                if (null == props.getProperty(parentKey)) {

                    /* Special handling for properties of type InetSocketAddress*/
                    Class parentClass = ServiceConfig.Parameter.getParameterFromString(parentKey).getPropertyClass();
                    if (parentClass == InetSocketAddress.class) {
                        setAddressSocketProperty(parentKey, parentValue);
                    } else {
                        props.setProperty(parentKey, parentValue);
                    }
                }
            }
        }

        /*
         * Handles properties of type InetSocketAddress
         *
         * Because of the String casting on InetSocketAddresses involved in parsing the configs, one ends up with a
         * string looking like "/127.0.0.1:32638" or "localhost/127.0.0.1:32638". Additionally, when handling
         * InetSocketAddresses, the ServiceConfig needs as input properties in the form of property.host and
         * property.port (while outputting them in only as property.host in a "[hostname]/ip:port" format).
         * 
         * @param parentKey
         * 
         * @param parentValue
         */
        private void setAddressSocketProperty(String parentKey, String parentValue) {
            /*
             * Ensure the format of the casted InetSocketAddress, so the string manipulations below work. Allowed format:
             * "[hostname]/ipadress:port"
             */
            String pattern = "[a-z]*/[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+:[0-9]+";
            if (!parentValue.matches(pattern))
                throw new IllegalArgumentException("Unknown address format for DIR adress [was: " + parentValue
                        + ". allowed: [hostname]/]");

            /* Remove (optional) hostname part and the "/" separator. Only IP is used to reconstruct the property. */
            String address = parentValue.split("/")[1];

            /* split in IP and port */
            String hostIP = address.split(":")[0];
            String port = address.split(":")[1];

            props.setProperty(parentKey, hostIP);
            props.setProperty(parentKey.replace("host", "port"), port);
        }
    }
}
