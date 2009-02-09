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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.xtreemfs.common.TimeSync;
import org.xtreemfs.common.auth.NullAuthProvider;
import org.xtreemfs.common.clients.dir.DIRClient;
import org.xtreemfs.common.clients.mrc.MRCClient;
import org.xtreemfs.common.clients.osd.OSDClient;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UUIDResolver;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.pinky.SSLOptions;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.osd.OSDConfig;

/**
 *
 * @author bjko
 */
public class SetupUtils {

    public static final String TEST_DIR    = "/tmp/xtreemfs-test";
    
    public static final String CERT_DIR    = "config/certs/";

    public static boolean      SSL_ON      = false;

    public static final int    DEBUG_LEVEL = Logging.LEVEL_DEBUG;

    public static OSDConfig createOSD1Config() throws IOException {
        Properties props = new Properties();
        props.setProperty("dir_service.host", "localhost");
        props.setProperty("dir_service.port", "33638");
        props.setProperty("object_dir", TEST_DIR + "/osd0");
        props.setProperty("debug_level", "" + DEBUG_LEVEL);
        props.setProperty("listen.port", "33637");
        //props.setProperty("listen.address", "localhost");
        props.setProperty("local_clock_renewal", "50");
        props.setProperty("remote_time_sync", "60000");
        props.setProperty("ssl.enabled", "" + SSL_ON);
        props.setProperty("ssl.service_creds", CERT_DIR + "service2.jks");
        props.setProperty("ssl.service_creds.pw", "passphrase");
        props.setProperty("ssl.service_creds.container", "jks");
        props.setProperty("ssl.trusted_certs", CERT_DIR + "trust.jks");
        props.setProperty("ssl.trusted_certs.pw", "passphrase");
        props.setProperty("ssl.trusted_certs.container", "jks");
        props.setProperty("report_free_space", "true");
        props.setProperty("checksums.enabled", "true");
        props.setProperty("checksums.algorithm", "Adler32");
        props.setProperty("capability_secret", "secretPassphrase");
        props.setProperty("uuid", getOSD1UUID().toString());

        return new OSDConfig(props);
    }

    public static OSDConfig createOSD2Config() throws IOException {
        Properties props = new Properties();
        props.setProperty("dir_service.host", "localhost");
        props.setProperty("dir_service.port", "33638");
        props.setProperty("object_dir", TEST_DIR + "/osd1");
        props.setProperty("debug_level", "" + DEBUG_LEVEL);
        props.setProperty("listen.port", "33640");
        //props.setProperty("listen.address", "localhost");
        props.setProperty("local_clock_renewal", "50");
        props.setProperty("remote_time_sync", "60000");
        props.setProperty("ssl.enabled", "" + SSL_ON);
        props.setProperty("ssl.service_creds", CERT_DIR + "service2.jks");
        props.setProperty("ssl.service_creds.pw", "passphrase");
        props.setProperty("ssl.service_creds.container", "jks");
        props.setProperty("ssl.trusted_certs", CERT_DIR + "trust.jks");
        props.setProperty("ssl.trusted_certs.pw", "passphrase");
        props.setProperty("ssl.trusted_certs.container", "jks");
        props.setProperty("report_free_space", "true");
        props.setProperty("checksums.enabled", "true");
        props.setProperty("checksums.algorithm", "Adler32");
        props.setProperty("capability_secret", "secretPassphrase");
        props.setProperty("uuid", getOSD2UUID().toString());

        return new OSDConfig(props);
    }

    public static OSDConfig createOSD3Config() throws IOException {
        Properties props = new Properties();
        props.setProperty("dir_service.host", "localhost");
        props.setProperty("dir_service.port", "33638");
        props.setProperty("object_dir", TEST_DIR + "/osd2");
        props.setProperty("debug_level", "" + DEBUG_LEVEL);
        props.setProperty("listen.port", "33641");
        props.setProperty("listen.address", "localhost");
        props.setProperty("local_clock_renewal", "50");
        props.setProperty("remote_time_sync", "60000");
        props.setProperty("ssl.enabled", "" + SSL_ON);
        props.setProperty("ssl.service_creds", CERT_DIR + "service2.jks");
        props.setProperty("ssl.service_creds_pw", "passphrase");
        props.setProperty("ssl.service_creds_container", "jks");
        props.setProperty("ssl.trusted_certs", CERT_DIR + "trust.jks");
        props.setProperty("ssl.trusted_certs.pw", "passphrase");
        props.setProperty("ssl.trusted_certs.container", "jks");
        props.setProperty("report_free_space", "true");
        props.setProperty("checksums.enabled", "true");
        props.setProperty("checksums.algorithm", "Adler32");
        props.setProperty("capability_secret", "secretPassphrase");
        props.setProperty("uuid", getOSD3UUID().toString());

        return new OSDConfig(props);
    }
    
    public static OSDConfig createOSD4Config() throws IOException {
        Properties props = new Properties();
        props.setProperty("dir_service.host", "localhost");
        props.setProperty("dir_service.port", "33638");
        props.setProperty("object_dir", TEST_DIR + "/osd3");
        props.setProperty("debug_level", "" + DEBUG_LEVEL);
        props.setProperty("listen.port", "33642");
        props.setProperty("listen.address", "localhost");
        props.setProperty("local_clock_renewal", "50");
        props.setProperty("remote_time_sync", "60000");
        props.setProperty("ssl.enabled", "" + SSL_ON);
        props.setProperty("ssl.service_creds", CERT_DIR + "service2.jks");
        props.setProperty("ssl.service_creds_pw", "passphrase");
        props.setProperty("ssl.service_creds_container", "jks");
        props.setProperty("ssl.trusted_certs", CERT_DIR + "trust.jks");
        props.setProperty("ssl.trusted_certs.pw", "passphrase");
        props.setProperty("ssl.trusted_certs.container", "jks");
        props.setProperty("report_free_space", "true");
        props.setProperty("checksums.enabled", "true");
        props.setProperty("checksums.algorithm", "Adler32");
        props.setProperty("capability_secret", "secretPassphrase");
        props.setProperty("uuid", getOSD4UUID().toString());

        return new OSDConfig(props);
    }

    public static DIRConfig createDIRConfig() throws IOException {
        Properties props = new Properties();
        props.setProperty("database.dir", TEST_DIR);
        props.setProperty("debug_level", "" + DEBUG_LEVEL);
        props.setProperty("listen.port", "33638");
        props.setProperty("ssl.enabled", "" + SSL_ON);
        props.setProperty("ssl.service_creds", CERT_DIR + "service3.jks");
        props.setProperty("ssl.service_creds.pw", "passphrase");
        props.setProperty("ssl.service_creds.container", "jks");
        props.setProperty("ssl.trusted_certs", CERT_DIR + "trust.jks");
        props.setProperty("ssl.trusted_certs.pw", "passphrase");
        props.setProperty("ssl.trusted_certs.container", "jks");
        props.setProperty("authentication_provider", "org.xtreemfs.common.auth.NullAuthProvider");

        return new DIRConfig(props);
    }

    public static MRCConfig createMRC1Config() throws IOException {
        Properties props = new Properties();
        props.setProperty("dir_service.host", "localhost");
        props.setProperty("dir_service.port", "33638");
        props.setProperty("database.dir", TEST_DIR + "/mrc0");
        props.setProperty("database.log", TEST_DIR + "/test-brain0.log");
        props.setProperty("osd_check_interval", "10");
        props.setProperty("debug_level", "" + DEBUG_LEVEL);
        props.setProperty("listen.port", "33636");
        props.setProperty("listen.address", "localhost");
        props.setProperty("no_atime", "true");
        props.setProperty("local_clock_renewal", "50");
        props.setProperty("remote_time_sync", "60000");
        props.setProperty("ssl.enabled", "" + SSL_ON);
        props.setProperty("ssl.service_creds", CERT_DIR + "service1.jks");
        props.setProperty("ssl.service_creds.pw", "passphrase");
        props.setProperty("ssl.service_creds.container", "jks");
        props.setProperty("ssl.trusted_certs", CERT_DIR + "trust.jks");
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
        props.setProperty("debug_level", "" + DEBUG_LEVEL);
        props.setProperty("listen.port", "33639");
        props.setProperty("listen.address", "localhost");
        props.setProperty("no_atime", "true");
        props.setProperty("local_clock_renewal", "50");
        props.setProperty("remote_time_sync", "60000");
        props.setProperty("ssl.enabled", "" + SSL_ON);
        props.setProperty("ssl.service_creds", CERT_DIR + "service1.jks");
        props.setProperty("ssl.service_creds.pw", "passphrase");
        props.setProperty("ssl.service_creds.container", "jks");
        props.setProperty("ssl.trusted_certs", CERT_DIR + "trust.jks");
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

    public static void setupLocalResolver() throws IOException, JSONException {
        TimeSync.initialize(null, 100000, 50, "");
        UUIDResolver.shutdown();

        UUIDResolver.start(null, 1000, 1000);
        UUIDResolver.addLocalMapping(getMRC1UUID(), 33636, SSL_ON);
        UUIDResolver.addLocalMapping(getMRC2UUID(), 33639, SSL_ON);
        UUIDResolver.addLocalMapping(getOSD1UUID(), 33637, SSL_ON);
        UUIDResolver.addLocalMapping(getOSD2UUID(), 33640, SSL_ON);
        UUIDResolver.addLocalMapping(getOSD3UUID(), 33641, SSL_ON);
    }


    public static MRCClient createMRCClient(int timeout) throws IOException {
        return SSL_ON ? new MRCClient(timeout, new SSLOptions(CERT_DIR + "client1.p12",
            "passphrase", SSLOptions.PKCS12_CONTAINER, CERT_DIR + "trust.jks", "passphrase",
            SSLOptions.JKS_CONTAINER, false)) : new MRCClient();
    }

    public static OSDClient createOSDClient(int timeout) throws IOException {
        return SSL_ON ? new OSDClient(timeout, new SSLOptions(CERT_DIR + "client1.p12",
            "passphrase", SSLOptions.PKCS12_CONTAINER, CERT_DIR + "trust.jks", "passphrase",
            SSLOptions.JKS_CONTAINER, false)) : new OSDClient(null);
    }

    public static DIRClient createDIRClient(int timeout) throws IOException {
        return SSL_ON ? new DIRClient(new InetSocketAddress("localhost", 33638), new SSLOptions(
            CERT_DIR + "client1.p12", "passphrase", SSLOptions.PKCS12_CONTAINER, CERT_DIR
                + "trust.jks", "passphrase", SSLOptions.JKS_CONTAINER, false), timeout)
            : new DIRClient(null, new InetSocketAddress("localhost", 33638));
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

    public static DIRClient initTimeSync() throws IOException, JSONException {

        try {
            TimeSync.getInstance();
            return null;

        } catch (RuntimeException ex) {
            // no time sync there, start one
            DIRClient dirClient = SetupUtils.createDIRClient(10000);
            TimeSync.initialize(dirClient, 60000, 50, NullAuthProvider.createAuthString("bla",
                "bla"));

            return dirClient;
        }
    }
}
