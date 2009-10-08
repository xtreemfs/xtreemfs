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
 * AUTHORS: Christian Lorenz (ZIB), Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

import org.xtreemfs.babudb.log.DiskLogger.SyncMode;
import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.oncrpc.client.RPCNIOSocketClient;
import org.xtreemfs.include.common.config.BabuDBConfig;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.osd.OSDConfig;

/**
 * 
 * @author bjko
 */
public class SetupUtils {
    
    public static final String     TEST_DIR         = "/tmp/xtreemfs-test2";
    
    public static final String     CERT_DIR         = "test/certs/";
    
    public static boolean          SSL_ON           = false;
    
    public static boolean          CHECKSUMS_ON     = false;
    
    public static final int        DEBUG_LEVEL      = Logging.LEVEL_DEBUG;
    
    public static final Category[] DEBUG_CATEGORIES = new Category[] { Logging.Category.db, Logging.Category.net };
    
    private static Properties createOSDProperties(int port, String dir) {
        Properties props = new Properties();
        props.setProperty("dir_service.host", "localhost");
        props.setProperty("dir_service.port", "33638");
        props.setProperty("object_dir", dir);
        props.setProperty("debug.level", "" + DEBUG_LEVEL);
        props.setProperty("debug.categories", ""
            + Arrays.toString(DEBUG_CATEGORIES).substring(1, Arrays.toString(DEBUG_CATEGORIES).length() - 1));
        props.setProperty("listen.port", "" + port);
        props.setProperty("http_port", "" + (port - 3000));
        props.setProperty("listen.address", "localhost");
        props.setProperty("local_clock_renewal", "50");
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
        return props;
    }
    
    public static OSDConfig createOSD1Config() throws IOException {
        Properties props = createOSDProperties(33637, TEST_DIR + "/osd0");
        return new OSDConfig(props);
    }
    
    public static OSDConfig createOSD2Config() throws IOException {
        Properties props = createOSDProperties(33640, TEST_DIR + "/osd1");
        return new OSDConfig(props);
    }
    
    public static OSDConfig createOSD3Config() throws IOException {
        Properties props = createOSDProperties(33641, TEST_DIR + "/osd2");
        return new OSDConfig(props);
    }
    
    public static OSDConfig createOSD4Config() throws IOException {
        Properties props = createOSDProperties(33642, TEST_DIR + "/osd3");
        return new OSDConfig(props);
    }
    
    public static OSDConfig[] createMultipleOSDConfigs(int number) throws IOException {
        OSDConfig[] configs = new OSDConfig[number];
        int startPort = 33640;
        
        for (int i = 0; i < configs.length; i++) {
            Properties props = createOSDProperties(startPort, TEST_DIR + "/osd" + i);
            configs[i] = new OSDConfig(props);
            startPort++;
        }
        return configs;
    }
    
    public static org.xtreemfs.dir.DIRConfig createDIRConfig() throws IOException {
        Properties props = new Properties();
        props.setProperty("debug.level", "" + DEBUG_LEVEL);
        props.setProperty("debug.categories", ""
            + Arrays.toString(DEBUG_CATEGORIES).substring(1, Arrays.toString(DEBUG_CATEGORIES).length() - 1));
        props.setProperty("listen.port", "33638");
        props.setProperty("http_port", "30638");
        props.setProperty("ssl.enabled", "" + SSL_ON);
        props.setProperty("ssl.service_creds", CERT_DIR + "DIR.p12");
        props.setProperty("ssl.service_creds.pw", "passphrase");
        props.setProperty("ssl.service_creds.container", "pkcs12");
        props.setProperty("ssl.trusted_certs", CERT_DIR + "trusted.jks");
        props.setProperty("ssl.trusted_certs.pw", "passphrase");
        props.setProperty("ssl.trusted_certs.container", "jks");
        props.setProperty("authentication_provider", "org.xtreemfs.common.auth.NullAuthProvider");
        
        return new org.xtreemfs.dir.DIRConfig(props);
    }
    
    public static BabuDBConfig createDIRdbsConfig() throws IOException {
        Properties props = new Properties();
        props.setProperty("debug.level", "" + DEBUG_LEVEL);
        props.setProperty("debug.categories", ""
                + Arrays.toString(DEBUG_CATEGORIES).substring(1, Arrays.toString(DEBUG_CATEGORIES).length() - 1));
        props.setProperty("db.cfgFile", "config.db");
        props.setProperty("db.baseDir", TEST_DIR);
        props.setProperty("db.logDir", TEST_DIR);
        props.setProperty("db.sync", "" + SyncMode.FSYNC);
        props.setProperty("worker.maxQueueLength", "250");
        props.setProperty("worker.numThreads", "0");
        props.setProperty("db.maxLogfileSize", "16777216");
        props.setProperty("db.checkInterval", "300");
        props.setProperty("db.pseudoSyncWait", "200");
        
        return new BabuDBConfig(props);
    }
    
    public static MRCConfig createMRC1Config() throws IOException {
        Properties props = new Properties();
        props.setProperty("dir_service.host", "localhost");
        props.setProperty("dir_service.port", "33638");
        props.setProperty("database.dir", TEST_DIR + "/mrc0");
        props.setProperty("database.log", TEST_DIR + "/test-brain0.log");
        props.setProperty("osd_check_interval", "10");
        props.setProperty("debug.level", "" + DEBUG_LEVEL);
        props.setProperty("debug.categories", ""
            + Arrays.toString(DEBUG_CATEGORIES).substring(1, Arrays.toString(DEBUG_CATEGORIES).length() - 1));
        props.setProperty("listen.port", "33636");
        props.setProperty("http_port", "30636");
        props.setProperty("listen.address", "localhost");
        props.setProperty("no_atime", "true");
        props.setProperty("local_clock_renewal", "50");
        props.setProperty("remote_time_sync", "60000");
        props.setProperty("ssl.enabled", "" + SSL_ON);
        props.setProperty("ssl.service_creds", CERT_DIR + "MRC.p12");
        props.setProperty("ssl.service_creds.pw", "passphrase");
        props.setProperty("ssl.service_creds.container", "pkcs12");
        props.setProperty("ssl.trusted_certs", CERT_DIR + "trusted.jks");
        props.setProperty("ssl.trusted_certs.pw", "passphrase");
        props.setProperty("ssl.trusted_certs.container", "jks");
        props.setProperty("database.checkpoint.interval", "1800000");
        props.setProperty("database.checkpoint.idle_interval", "1000");
        props.setProperty("database.checkpoint.logfile_size", "16384");
        props.setProperty("authentication_provider", "org.xtreemfs.common.auth.NullAuthProvider");
        props.setProperty("capability_secret", "secretPassphrase");
        props.setProperty("uuid", getMRC1UUID().toString());
        
        return new MRCConfig(props);
    }
    
    public static MRCConfig createMRC2Config() throws IOException {
        Properties props = new Properties();
        props.setProperty("dir_service.host", "localhost");
        props.setProperty("dir_service.port", "33638");
        props.setProperty("database.dir", TEST_DIR + "/mrc1");
        props.setProperty("database.log", TEST_DIR + "/test-brain1.log");
        props.setProperty("osd_check_interval", "10");
        props.setProperty("debug.level", "" + DEBUG_LEVEL);
        props.setProperty("debug.categories", ""
            + Arrays.toString(DEBUG_CATEGORIES).substring(1, Arrays.toString(DEBUG_CATEGORIES).length() - 1));
        props.setProperty("listen.port", "33639");
        props.setProperty("http_port", "30639");
        props.setProperty("listen.address", "localhost");
        props.setProperty("no_atime", "true");
        props.setProperty("local_clock_renewal", "50");
        props.setProperty("remote_time_sync", "60000");
        props.setProperty("ssl.enabled", "" + SSL_ON);
        props.setProperty("ssl.service_creds", CERT_DIR + "MRC.p12");
        props.setProperty("ssl.service_creds.pw", "passphrase");
        props.setProperty("ssl.service_creds.container", "pkcs12");
        props.setProperty("ssl.trusted_certs", CERT_DIR + "trusted.jks");
        props.setProperty("ssl.trusted_certs.pw", "passphrase");
        props.setProperty("ssl.trusted_certs.container", "jks");
        props.setProperty("database.checkpoint.interval", "1800000");
        props.setProperty("database.checkpoint.idle_interval", "1000");
        props.setProperty("database.checkpoint.logfile_size", "16384");
        props.setProperty("authentication_provider", "org.xtreemfs.common.auth.NullAuthProvider");
        props.setProperty("capability_secret", "secretPassphrase");
        props.setProperty("uuid", getMRC2UUID().toString());
        
        return new MRCConfig(props);
    }
    
    public static InetSocketAddress getMRC1Addr() {
        return new InetSocketAddress("localhost", 33636);
    }
    
    public static InetSocketAddress getMRC2Addr() {
        return new InetSocketAddress("localhost", 33639);
    }
    
    public static InetSocketAddress getOSD1Addr() {
        return new InetSocketAddress("localhost", 33637);
    }
    
    public static InetSocketAddress getOSD2Addr() {
        return new InetSocketAddress("localhost", 33640);
    }
    
    public static InetSocketAddress getOSD3Addr() {
        return new InetSocketAddress("localhost", 33641);
    }
    
    public static InetSocketAddress getOSD4Addr() {
        return new InetSocketAddress("localhost", 33642);
    }
    
    public static InetSocketAddress getDIRAddr() {
        return new InetSocketAddress("localhost", 33638);
    }
    
    public static ServiceUUID getMRC1UUID() {
        return new ServiceUUID("UUID:localhost:33636");
    }
    
    public static ServiceUUID getMRC2UUID() {
        return new ServiceUUID("UUID:localhost:33639");
    }
    
    public static ServiceUUID getOSD1UUID() {
        return new ServiceUUID("UUID:localhost:33637");
    }
    
    public static ServiceUUID getOSD2UUID() {
        return new ServiceUUID("UUID:localhost:33640");
    }
    
    public static ServiceUUID getOSD3UUID() {
        return new ServiceUUID("UUID:localhost:33641");
    }
    
    public static ServiceUUID getOSD4UUID() {
        return new ServiceUUID("UUID:localhost:33642");
    }
    
    static void localResolver() {
        UUIDResolver.addLocalMapping(getMRC1UUID(), 33636, SSL_ON ? Constants.ONCRPCS_SCHEME
            : Constants.ONCRPC_SCHEME);
        UUIDResolver.addLocalMapping(getMRC2UUID(), 33639, SSL_ON ? Constants.ONCRPCS_SCHEME
            : Constants.ONCRPC_SCHEME);
        UUIDResolver.addLocalMapping(getOSD1UUID(), 33637, SSL_ON ? Constants.ONCRPCS_SCHEME
            : Constants.ONCRPC_SCHEME);
        UUIDResolver.addLocalMapping(getOSD2UUID(), 33640, SSL_ON ? Constants.ONCRPCS_SCHEME
            : Constants.ONCRPC_SCHEME);
        UUIDResolver.addLocalMapping(getOSD3UUID(), 33641, SSL_ON ? Constants.ONCRPCS_SCHEME
            : Constants.ONCRPC_SCHEME);
        UUIDResolver.addLocalMapping(getOSD4UUID(), 33642, SSL_ON ? Constants.ONCRPCS_SCHEME
            : Constants.ONCRPC_SCHEME);
    }
    
    private static ServiceUUID getUUID(String listenAddress, int port) {
        return new ServiceUUID("UUID:" + listenAddress + ":" + port);
    }
    
    public static void setupLocalResolver() throws Exception {
        TimeSync.initialize(null, 100000, 50);
        UUIDResolver.shutdown();
        
        UUIDResolver.start(null, 1000, 1000);
        localResolver();
    }
    
    static RPCNIOSocketClient createRPCClient(int timeout) throws IOException {
        final SSLOptions sslOptions = SSL_ON ? new SSLOptions(new FileInputStream(CERT_DIR + "Client.p12"),
            "passphrase", SSLOptions.PKCS12_CONTAINER, new FileInputStream(CERT_DIR + "trusted.jks"),
            "passphrase", SSLOptions.JKS_CONTAINER, false) : null;
        return new RPCNIOSocketClient(sslOptions, timeout, 5 * 60 * 1000);
    }
    
    static DIRClient createDIRClient(RPCNIOSocketClient client) throws IOException {
        return new DIRClient(client, new InetSocketAddress("localhost", 33638));
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
    
}
