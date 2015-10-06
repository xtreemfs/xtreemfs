/*
 * Copyright (c) 2008-2011 by Christian Lorenz, Jan Stender,
 *               Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

import org.xtreemfs.babudb.config.BabuDBConfig;
import org.xtreemfs.babudb.log.DiskLogger.SyncMode;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.Schemes;
import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceClient;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicyType;

/**
 *
 * @author bjko
 */
public class SetupUtils {

    public static final String     TEST_DIR         = "/tmp/xtreemfs-test2";

    public static final String     CERT_DIR          = "tests/certs/";

    public static boolean          SSL_ON           = false;

    public static boolean          CHECKSUMS_ON     = false;

    public static final int        DEBUG_LEVEL      = Logging.LEVEL_WARN;

    public static final Category[] DEBUG_CATEGORIES = new Category[] { Category.all };

    public static final int PORT_RANGE_OFFSET = 10000;

    /**
     * Analog to nextOsdNo.
     */
    private static final int offsetFirstOsdPort             = 32640 + PORT_RANGE_OFFSET;

    private static Properties createOSDProperties(int port, String dir) {
        Properties props = new Properties();
        props.setProperty("dir_service.host", "localhost");
        props.setProperty("dir_service.port", new Integer(32638 + PORT_RANGE_OFFSET).toString());
        props.setProperty("object_dir", dir);
        props.setProperty("debug.level", "" + DEBUG_LEVEL);
        props.setProperty("debug.categories",
                "" + Arrays.toString(DEBUG_CATEGORIES).substring(1, Arrays.toString(DEBUG_CATEGORIES).length() - 1));
        props.setProperty("listen.port", "" + port);
        props.setProperty("http_port", "" + (port - 3000));
        props.setProperty("listen.address", "localhost");
        props.setProperty("local_clock_renewal", "0");
        props.setProperty("remote_time_sync", "60000");
        props.setProperty("ssl.enabled", "" + SSL_ON);
        props.setProperty("ssl.service_creds", CERT_DIR + "OSD.p12");
        props.setProperty("ssl.service_creds.pw", "passphrase");
        props.setProperty("ssl.service_creds.container", "pkcs12");
        props.setProperty("ssl.trusted_certs", CERT_DIR + "trusted.jks");
        props.setProperty("ssl.trusted_certs.pw", "passphrase");
        props.setProperty("ssl.trusted_certs.container", "jks");
        props.setProperty("report_free_space", "true");
        props.setProperty("checksums.enabled", Boolean.toString(CHECKSUMS_ON));
        props.setProperty("checksums.algorithm", "Adler32");
        props.setProperty("capability_secret", "secretPassphrase");
        props.setProperty("uuid", getUUID("localhost", port).toString());
        props.setProperty("snmp.enabled", "true");
        props.setProperty("snmp.port", "" + (port + 1000));
        props.setProperty("snmp.address", "localhost");
        props.setProperty("measure_requests", "false");
        props.setProperty("use_qos", "false");
        return props;
    }

    public static OSDConfig createOSD1Config() throws IOException {
        Properties props = createOSDProperties(32637 + PORT_RANGE_OFFSET, TEST_DIR + "/osd0");
        OSDConfig config = new OSDConfig(props);
        config.setDefaults();
        return config;
    }

    public static OSDConfig createOSD2Config() throws IOException {
        Properties props = createOSDProperties(32640 + PORT_RANGE_OFFSET, TEST_DIR + "/osd1");
        OSDConfig config = new OSDConfig(props);
        config.setDefaults();
        return config;
    }

    public static OSDConfig createOSD3Config() throws IOException {
        Properties props = createOSDProperties(32641 + PORT_RANGE_OFFSET, TEST_DIR + "/osd2");
        OSDConfig config = new OSDConfig(props);
        config.setDefaults();
        return config;
    }

    public static OSDConfig createOSD4Config() throws IOException {
        Properties props = createOSDProperties(32642 + PORT_RANGE_OFFSET, TEST_DIR + "/osd3");
        OSDConfig config = new OSDConfig(props);
        config.setDefaults();
        return config;
    }
    /**
     *
     * Creates multiple OSD configs starting at offset 0.
     *
     */
    public static OSDConfig[] createMultipleOSDConfigs(int number) throws IOException {
        return createMultipleOSDConfigs(number, 0);
    }

    /**
     *
     * Creates multiple OSD configs starting at offset "offsetNextOsd".
     *
     */
    public static OSDConfig[] createMultipleOSDConfigs(int number, int offsetNextOsd) throws IOException {
        OSDConfig[] configs = new OSDConfig[number];
        int offsetNextOsdPort = offsetFirstOsdPort + offsetNextOsd;
        for (int i = 0; i < configs.length; i++) {
            Properties props = createOSDProperties(offsetNextOsdPort, TEST_DIR + "/osd" + offsetNextOsd);
            configs[i] = new OSDConfig(props);
            configs[i].setDefaults();
            offsetNextOsdPort++;
            offsetNextOsd++;
        }
        return configs;
    }

    public static org.xtreemfs.dir.DIRConfig createDIRConfig() throws IOException {
        Properties props = new Properties();
        props.setProperty("debug.level", "" + DEBUG_LEVEL);
        props.setProperty("debug.categories",
                "" + Arrays.toString(DEBUG_CATEGORIES).substring(1, Arrays.toString(DEBUG_CATEGORIES).length() - 1));
        props.setProperty("listen.port", new Integer(32638 + PORT_RANGE_OFFSET).toString());
        props.setProperty("http_port", new Integer(30638 + PORT_RANGE_OFFSET).toString());
        props.setProperty("uuid", "UUID:localhost:" + new Integer(32638 + PORT_RANGE_OFFSET).toString());
        props.setProperty("ssl.enabled", "" + SSL_ON);
        props.setProperty("ssl.service_creds", CERT_DIR + "DIR.p12");
        props.setProperty("ssl.service_creds.pw", "passphrase");
        props.setProperty("ssl.service_creds.container", "pkcs12");
        props.setProperty("ssl.trusted_certs", CERT_DIR + "trusted.jks");
        props.setProperty("ssl.trusted_certs.pw", "passphrase");
        props.setProperty("ssl.trusted_certs.container", "jks");
        props.setProperty("authentication_provider", "org.xtreemfs.common.auth.NullAuthProvider");
        props.setProperty("snmp.enabled", "true");
        props.setProperty("snmp.port", new Integer(34638 + PORT_RANGE_OFFSET).toString());
        props.setProperty("snmp.address", "localhost");
        props.setProperty("measure_requests", "false");

        DIRConfig config = new DIRConfig(props);
        config.setDefaults();

        return config;
    }

    public static BabuDBConfig createDIRdbsConfig() throws IOException {
        Properties props = new Properties();
        props.setProperty("babudb.debug.level", "" + DEBUG_LEVEL);
        props.setProperty("babudb.debug.categories",
                "" + Arrays.toString(DEBUG_CATEGORIES).substring(1, Arrays.toString(DEBUG_CATEGORIES).length() - 1));
        props.setProperty("babudb.cfgFile", "config.db");
        props.setProperty("babudb.baseDir", TEST_DIR);
        props.setProperty("babudb.logDir", TEST_DIR);
        props.setProperty("babudb.sync", "" + SyncMode.FSYNC);
        props.setProperty("babudb.worker.maxQueueLength", "250");
        props.setProperty("babudb.worker.numThreads", "0");
        props.setProperty("babudb.maxLogfileSize", "16777216");
        props.setProperty("babudb.checkInterval", "300");
        props.setProperty("babudb.pseudoSyncWait", "200");

        return new BabuDBConfig(props);
    }

    public static MRCConfig createMRC1Config() throws IOException {
        Properties props = new Properties();
        props.setProperty("dir_service.host", "localhost");
        props.setProperty("dir_service.port", new Integer(32638 + PORT_RANGE_OFFSET).toString());
        props.setProperty("osd_check_interval", "10");
        props.setProperty("debug.level", "" + DEBUG_LEVEL);
        props.setProperty("debug.categories",
                "" + Arrays.toString(DEBUG_CATEGORIES).substring(1, Arrays.toString(DEBUG_CATEGORIES).length() - 1));
        props.setProperty("listen.port", new Integer(32636 + PORT_RANGE_OFFSET).toString());
        props.setProperty("http_port", new Integer(30636 + PORT_RANGE_OFFSET).toString());
        props.setProperty("listen.address", "localhost");
        props.setProperty("no_atime", "true");
        props.setProperty("local_clock_renewal", "0");
        props.setProperty("remote_time_sync", "60000");
        props.setProperty("ssl.enabled", "" + SSL_ON);
        props.setProperty("ssl.service_creds", CERT_DIR + "MRC.p12");
        props.setProperty("ssl.service_creds.pw", "passphrase");
        props.setProperty("ssl.service_creds.container", "pkcs12");
        props.setProperty("ssl.trusted_certs", CERT_DIR + "trusted.jks");
        props.setProperty("ssl.trusted_certs.pw", "passphrase");
        props.setProperty("ssl.trusted_certs.container", "jks");
        props.setProperty("authentication_provider", "org.xtreemfs.common.auth.NullAuthProvider");
        props.setProperty("capability_secret", "secretPassphrase");
        props.setProperty("uuid", getMRC1UUID().toString());
        props.setProperty("snmp.enabled", "true");
        props.setProperty("snmp.port", new Integer(34636 + PORT_RANGE_OFFSET).toString());
        props.setProperty("snmp.address", "localhost");
        props.setProperty("measure_requests", "false");

        MRCConfig config = new MRCConfig(props);
        config.setDefaults();
        return config;
    }

    public static BabuDBConfig createMRC1dbsConfig() throws IOException {
        Properties props = new Properties();
        props.setProperty("babudb.debug.level", "" + DEBUG_LEVEL);
        props.setProperty("debug.categories",
                "" + Arrays.toString(DEBUG_CATEGORIES).substring(1, Arrays.toString(DEBUG_CATEGORIES).length() - 1));
        props.setProperty("babudb.cfgFile", "config.db");
        props.setProperty("babudb.baseDir", TEST_DIR + "/mrc0");
        props.setProperty("babudb.logDir", TEST_DIR + "/test-brain0.log");
        props.setProperty("babudb.sync", "" + SyncMode.ASYNC);
        props.setProperty("babudb.worker.maxQueueLength", "500");
        props.setProperty("babudb.worker.numThreads", "2");
        props.setProperty("babudb.maxLogfileSize", "16777216");
        props.setProperty("babudb.checkInterval", "300");
        props.setProperty("babudb.pseudoSyncWait", "0");

        return new BabuDBConfig(props);
    }

    public static MRCConfig createMRC2Config() throws IOException {
        Properties props = new Properties();
        props.setProperty("dir_service.host", "localhost");
        props.setProperty("dir_service.port", new Integer(32638 + PORT_RANGE_OFFSET).toString());
        props.setProperty("osd_check_interval", "10");
        props.setProperty("debug.level", "" + DEBUG_LEVEL);
        props.setProperty("debug.categories",
                "" + Arrays.toString(DEBUG_CATEGORIES).substring(1, Arrays.toString(DEBUG_CATEGORIES).length() - 1));
        props.setProperty("listen.port", new Integer(32639 + PORT_RANGE_OFFSET).toString());
        props.setProperty("http_port", new Integer(30639 + PORT_RANGE_OFFSET).toString());
        props.setProperty("listen.address", "localhost");
        props.setProperty("no_atime", "true");
        props.setProperty("local_clock_renewal", "0");
        props.setProperty("remote_time_sync", "60000");
        props.setProperty("ssl.enabled", "" + SSL_ON);
        props.setProperty("ssl.service_creds", CERT_DIR + "MRC.p12");
        props.setProperty("ssl.service_creds.pw", "passphrase");
        props.setProperty("ssl.service_creds.container", "pkcs12");
        props.setProperty("ssl.trusted_certs", CERT_DIR + "trusted.jks");
        props.setProperty("ssl.trusted_certs.pw", "passphrase");
        props.setProperty("ssl.trusted_certs.container", "jks");
        props.setProperty("authentication_provider", "org.xtreemfs.common.auth.NullAuthProvider");
        props.setProperty("capability_secret", "secretPassphrase");
        props.setProperty("uuid", getMRC2UUID().toString());
        props.setProperty("snmp.enabled", "true");
        props.setProperty("snmp.port", new Integer(34639 + PORT_RANGE_OFFSET).toString());
        props.setProperty("snmp.address", "localhost");

        MRCConfig config = new MRCConfig(props);
        config.setDefaults();
        return config;
    }

    public static BabuDBConfig createMRC2dbsConfig() throws IOException {
        Properties props = new Properties();
        props.setProperty("babudb.debug.level", "" + DEBUG_LEVEL);
        props.setProperty("babudb.debug.categories",
                "" + Arrays.toString(DEBUG_CATEGORIES).substring(1, Arrays.toString(DEBUG_CATEGORIES).length() - 1));
        props.setProperty("babudb.cfgFile", "config.db");
        props.setProperty("babudb.baseDir", TEST_DIR + "/mrc1");
        props.setProperty("babudb.logDir", TEST_DIR + "/test-brain1.log");
        props.setProperty("babudb.sync", "" + SyncMode.ASYNC);
        props.setProperty("babudb.worker.maxQueueLength", "500");
        props.setProperty("babudb.worker.numThreads", "2");
        props.setProperty("babudb.maxLogfileSize", "16777216");
        props.setProperty("babudb.checkInterval", "300");
        props.setProperty("babudb.pseudoSyncWait", "0");

        return new BabuDBConfig(props);
    }

    public static InetSocketAddress getMRC1Addr() {
        return new InetSocketAddress("localhost", 32636 + PORT_RANGE_OFFSET);
    }

    public static InetSocketAddress getMRC2Addr() {
        return new InetSocketAddress("localhost", 32639 + PORT_RANGE_OFFSET);
    }

    public static InetSocketAddress getOSD1Addr() {
        return new InetSocketAddress("localhost", 32637 + PORT_RANGE_OFFSET);
    }

    public static InetSocketAddress getOSD2Addr() {
        return new InetSocketAddress("localhost", 32640 + PORT_RANGE_OFFSET);
    }

    public static InetSocketAddress getOSD3Addr() {
        return new InetSocketAddress("localhost", 32641 + PORT_RANGE_OFFSET);
    }

    public static InetSocketAddress getOSD4Addr() {
        return new InetSocketAddress("localhost", 32642 + PORT_RANGE_OFFSET);
    }

    public static InetSocketAddress getDIRAddr() {
        return new InetSocketAddress("localhost", 32638 + PORT_RANGE_OFFSET);
    }

    public static ServiceUUID getMRC1UUID() {
        return new ServiceUUID("UUID:localhost:" + new Integer(32636 + PORT_RANGE_OFFSET).toString());
    }

    public static ServiceUUID getMRC2UUID() {
        return new ServiceUUID("UUID:localhost:" + new Integer(32639 + PORT_RANGE_OFFSET).toString());
    }

    public static ServiceUUID getOSD1UUID() {
        return new ServiceUUID("UUID:localhost:" + new Integer(32637 + PORT_RANGE_OFFSET).toString());
    }

    public static ServiceUUID getOSD2UUID() {
        return new ServiceUUID("UUID:localhost:" + new Integer(32640 + PORT_RANGE_OFFSET).toString());
    }

    public static ServiceUUID getOSD3UUID() {
        return new ServiceUUID("UUID:localhost:" + new Integer(32641 + PORT_RANGE_OFFSET).toString());
    }

    public static ServiceUUID getOSD4UUID() {
        return new ServiceUUID("UUID:localhost:" + new Integer(32642 + PORT_RANGE_OFFSET).toString());
    }

    static void localResolver() {
        UUIDResolver.addLocalMapping(getMRC1UUID(), 32636 + PORT_RANGE_OFFSET, SSL_ON ? Schemes.SCHEME_PBRPCS : Schemes.SCHEME_PBRPC);
        UUIDResolver.addLocalMapping(getMRC2UUID(), 32639 + PORT_RANGE_OFFSET, SSL_ON ? Schemes.SCHEME_PBRPCS : Schemes.SCHEME_PBRPC);
        UUIDResolver.addLocalMapping(getOSD1UUID(), 32637 + PORT_RANGE_OFFSET, SSL_ON ? Schemes.SCHEME_PBRPCS : Schemes.SCHEME_PBRPC);
        UUIDResolver.addLocalMapping(getOSD2UUID(), 32640 + PORT_RANGE_OFFSET, SSL_ON ? Schemes.SCHEME_PBRPCS : Schemes.SCHEME_PBRPC);
        UUIDResolver.addLocalMapping(getOSD3UUID(), 32641 + PORT_RANGE_OFFSET, SSL_ON ? Schemes.SCHEME_PBRPCS : Schemes.SCHEME_PBRPC);
        UUIDResolver.addLocalMapping(getOSD4UUID(), 32642 + PORT_RANGE_OFFSET, SSL_ON ? Schemes.SCHEME_PBRPCS : Schemes.SCHEME_PBRPC);
    }

    private static ServiceUUID getUUID(String listenAddress, int port) {
        return new ServiceUUID("UUID:" + listenAddress + ":" + port);
    }

    public static void setupLocalResolver() throws Exception {
        TimeSync.initialize(null, 100000, 50);

        UUIDResolver.start(null, 1000, 1000);
        localResolver();
    }

    static RPCNIOSocketClient createRPCClient(int timeout) throws IOException {
        final SSLOptions sslOptions = SSL_ON ? createClientSSLOptions() : null;
        return new RPCNIOSocketClient(sslOptions, timeout, 5 * 60 * 1000, "SetupUtils");
    }

    public static SSLOptions createClientSSLOptions() throws IOException, FileNotFoundException {
        return new SSLOptions(CERT_DIR + "Client.p12", "passphrase", SSLOptions.PKCS12_CONTAINER, CERT_DIR
                + "trusted.jks", "passphrase", SSLOptions.JKS_CONTAINER, false, false, null, null);
    }

    static DIRServiceClient createDIRClient(RPCNIOSocketClient client) throws IOException {
        return new DIRServiceClient(client, new InetSocketAddress("localhost", 32638 + PORT_RANGE_OFFSET));
    }

    public static OSDConfig createOSD1ConfigForceWithoutSSL() throws IOException {
        boolean tmp = SSL_ON;
        SSL_ON = false;
        OSDConfig config = createOSD1Config();
        SSL_ON = tmp;
        return config;
    }

    public static OSDConfig createOSD2ConfigForceWithoutSSL() throws IOException {
        boolean tmp = SSL_ON;
        SSL_ON = false;
        OSDConfig config = createOSD2Config();
        SSL_ON = tmp;
        return config;
    }

    public static OSDConfig createOSD3ConfigForceWithoutSSL() throws IOException {
        boolean tmp = SSL_ON;
        SSL_ON = false;
        OSDConfig config = createOSD3Config();
        SSL_ON = tmp;
        return config;
    }

    public static MRCConfig createMRC1ConfigForceWithoutSSL() throws IOException {
        boolean tmp = SSL_ON;
        SSL_ON = false;
        MRCConfig config = createMRC1Config();
        SSL_ON = tmp;
        return config;
    }

    public static MRCConfig createMRC2ConfigForceWithoutSSL() throws IOException {
        boolean tmp = SSL_ON;
        SSL_ON = false;
        MRCConfig config = createMRC2Config();
        SSL_ON = tmp;
        return config;
    }

    public static DIRConfig createDIRConfigForceWithoutSSL() throws IOException {
        boolean tmp = SSL_ON;
        SSL_ON = false;
        DIRConfig config = createDIRConfig();
        SSL_ON = tmp;
        return config;
    }

    /**
     * @param size
     *            in byte
     */
    public static ReusableBuffer generateData(int size) {
        Random random = new Random();
        byte[] data = new byte[size];
        random.nextBytes(data);
        return ReusableBuffer.wrap(data);
    }

    /**
     * @param size
     *            in byte
     */
    public static ReusableBuffer generateData(int size, byte ch) {
        byte[] data = new byte[size];
        for (int i = 0; i < data.length; i++)
            data[i] = ch;
        return ReusableBuffer.wrap(data);
    }

    public static StripingPolicy getStripingPolicy(int width, int stripeSize) {
        return StripingPolicy.newBuilder().setType(StripingPolicyType.STRIPING_POLICY_RAID0).setStripeSize(stripeSize)
                .setWidth(width).build();
    }

}
