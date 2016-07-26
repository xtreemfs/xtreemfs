/*
 * Copyright (c) 2015 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs.jni;

import java.io.File;
import java.math.BigInteger;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.jni.generated.ClientProxy;
import org.xtreemfs.common.libxtreemfs.jni.generated.IntVector;
import org.xtreemfs.common.libxtreemfs.jni.generated.OptionsProxy;
import org.xtreemfs.common.libxtreemfs.jni.generated.SSLContext;
import org.xtreemfs.common.libxtreemfs.jni.generated.SSLOptionsProxy;
import org.xtreemfs.common.libxtreemfs.jni.generated.ServiceAddresses;
import org.xtreemfs.common.libxtreemfs.jni.generated.StringMap;
import org.xtreemfs.common.libxtreemfs.jni.generated.StringVector;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;

public final class NativeHelper {

    /** Set to true after loadLibrary has been called the first time. */
    private static boolean loadLibraryCalled = false;
    /** Contains a single path where the libraries are searched. */
    private static String  xtreemfsLibPath   = null;

    /**
     * Load the library with the platform independent name. (f.ex. jni-xtreemfs instead of libjni-xtreemfs.so) <br>
     * A configured xtreemfsLibPath is tried first. Then locally built libraries from the source tree. On Linux
     * feasible directories within the FHS are searched. Finally the common library path is searched.
     * 
     * @param name
     */
    public static void loadLibrary(String name) {
        loadLibraryCalled = true;

        // Get platform specific library filename.
        String libname = System.mapLibraryName(name);

        // At first try to load the library from the configured build path
        if (tryLoadFromXtreemfsLibPath(libname)) {
            return;
        }

        // Prefer recently build libs from the source tree.
        if (tryLoadLibraryFromBuildDir(libname)) {
            return;
        }

        // Try to find the correct lib directory within the Filesystem Hierarchy Standard
        String os = System.getProperty("os.name");
        if (os.equals("Linux")) {
            if (tryLoadLibraryFromFHS(libname)) {
                return;
            }
        }

        // Finally try to load the lib from the common library path.
        System.loadLibrary(name);
    }
    
    /**
     * Try to load the library from the library path set by {@link #setXtreemfsLibPath(String)}
     * 
     * @param filename
     * @return true if the library has been loaded
     */
    private static boolean tryLoadFromXtreemfsLibPath(String filename) {
        // Skip if no library path is set.
        if (xtreemfsLibPath == null || xtreemfsLibPath.isEmpty()) {
            return false;
        }

        String libFilepath = xtreemfsLibPath + File.separator + filename;
        return tryLoadLibrary(libFilepath);
    }

    /**
     * Set the library path. This should be done before loadLibrary has been called the first time.
     * 
     * @param libraryPath
     */
    public static void setXtreemfsLibPath(String libraryPath) {
        // Ignore if the library path didn't change.
        if (libraryPath.equals(xtreemfsLibPath)) {
            return;
        }

        if (loadLibraryCalled) {
            if (Logging.getLevel() >= 0) {
                Logging.logMessage(Logging.LEVEL_WARN, (Object) null,
                        "NativeHelper.setXtreemfsLibpath has been called after loadLibrary.");
            }
        }

        xtreemfsLibPath = libraryPath;
    }

    /*
    public static boolean addToLibraryPath(String libraryPath) {
        try {
            String javaLibraryPath = System.getProperty("java.library.path");

            // Check if the path is already contained.
            if (javaLibraryPath.contains(libraryPath)) {
                return true;
            }

            // Throw an error if a library had been searched before.
            if (loadLibraryCalled) {
                if (Logging.getLevel() >= 0) {
                    Logging.logMessage(Logging.LEVEL_WARN, (Object) null,
                            "NativeHelper.setXtreemfsLibpath has been called after addToLibraryPath.");
                }
            }

            // Add the new path to the search path.
            javaLibraryPath = libraryPath + File.pathSeparator + javaLibraryPath;
            System.setProperty("java.library.path", javaLibraryPath);

            // Set sys_paths to null so that java.library.path will be reevaluated next time it is needed.
            final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            sysPathsField.set(null, null);

        } catch (SecurityException e) {
            return false;
        }
    }
    */

    /**
     * Try to load the library from the build directory.
     * 
     * @param filename
     * @return true if the library has been loaded
     */
    private static boolean tryLoadLibraryFromBuildDir(String filename) {
        // Try to load the library directly from the build directory
        // Get the URL of the current class
        URL classURL = NativeHelper.class.getResource(NativeHelper.class.getSimpleName() + ".class");
        // Abort if the class isn't a real file (and not within f.ex. a jar)
        if (classURL == null) {
            return false;
        }

        String path;
        int pos;
        if ("file".equalsIgnoreCase(classURL.getProtocol())) {
            path = classURL.getPath();
            path = path.replace(File.separator, "/");
            pos = path.lastIndexOf("/java/xtreemfs-servers/target/classes/");
        } else if ("jar".equalsIgnoreCase(classURL.getProtocol()) && classURL.getPath().startsWith("file:")) {
            // Strip the "file:" prefix and split at the "!"
            path = classURL.getPath().substring(5).split("!")[0];
            path = path.replace(File.separator, "/");
            pos = path.lastIndexOf("/java/xtreemfs-servers/target/xtreemfs.jar");
        } else {
            return false;
        }

        // Abort if the class file isn't residing within the java build directory,
        // otherwise extract the prefix
        if (pos < 0) {
            return false;
        }
        path = path.substring(0, pos);

        // Try to load the library from the build directory
        path = path + "/cpp/build/" + filename;
        path = path.replace("/", File.separator);

        return tryLoadLibrary(path);
    }

    /**
     * Try to load the library from the lib directories defined in the Filesystem Hierarchy Standard.
     * 
     * @param filename
     * @return true if the library has been loaded
     */
    private static boolean tryLoadLibraryFromFHS(String filename) {
        if (tryLoadLibrary("/usr/lib64/xtreemfs/" + filename)) {
            return true;
        }

        if (tryLoadLibrary("/usr/lib/xtreemfs/" + filename)) {
            return true;
        }

        return false;
    }

    /**
     * Try to load the library from the path.
     * 
     * @param filepath
     *            Full absolute path to the library.
     * @return true If the library has been loaded
     */
    static boolean tryLoadLibrary(String filepath) {
        try {
            System.load(filepath);
        } catch (Exception e) {
            return false;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }

        return true;
    }

    public static ClientProxy createClientProxy(String[] dirServiceAddressesArray, UserCredentials userCredentials,
            SSLOptions sslOptions, Options options) {
        StringVector dirServiceAddressesVector = StringVector.from(Arrays.asList(dirServiceAddressesArray));
        ServiceAddresses dirServiceAddresses = new ServiceAddresses(dirServiceAddressesVector);
        OptionsProxy optionsProxy = NativeHelper.migrateOptions(options);
        SSLOptionsProxy sslOptionsProxy = NativeHelper.migrateSSLOptions(sslOptions);

        return ClientProxy.createClient(dirServiceAddresses, userCredentials, sslOptionsProxy, optionsProxy);
    }

    public static OptionsProxy migrateOptions(Options o) {
        OptionsProxy op = new OptionsProxy();

        // Migrate the options, that are modifiable as Java Options
        op.setMetadata_cache_size(BigInteger.valueOf(o.getMetadataCacheSize()));
        op.setMax_tries(o.getMaxTries());
        op.setMax_read_tries(o.getMaxReadTries());
        op.setMax_write_tries(o.getMaxWriteTries());
        op.setMax_view_renewals(o.getMaxViewRenewals());
        op.setRetry_delay_s(o.getRetryDelay_s());
        op.setAsync_writes_max_request_size_kb(o.getMaxWriteahead());
        op.setEnable_async_writes(o.isEnableAsyncWrites());
        op.setPeriodic_file_size_updates_interval_s(o.getPeriodicFileSizeUpdatesIntervalS());
        op.setReaddir_chunk_size(o.getReaddirChunkSize());

        // Migrate the Java defaults, too
        op.setConnect_timeout_s(o.getConnectTimeout_s());
        // o.getInterruptSignal()
        op.setAsync_writes_max_requests(o.getMaxWriteaheadRequests());
        op.setLinger_timeout_s(o.getLingerTimeout_s());
        op.setMetadata_cache_ttl_s(BigInteger.valueOf(o.getMetadataCacheTTLs()));
        op.setPeriodic_xcap_renewal_interval_s(o.getPeriodicXcapRenewalIntervalS());
        op.setRequest_timeout_s(o.getRequestTimeout_s());
        op.setXLoc_install_poll_interval_s(o.getXLocInstallPollIntervalS());

        return op;
    }

    public static SSLOptionsProxy migrateSSLOptions(SSLOptions o) {
        if (o == null) {
            return null;
        }

        // Java is using PKCS#12 certificates
        // Ignore PEM related parameters.
        String ssl_pem_path = "";
        String ssl_pem_cert_path = "";
        String ssl_pem_key_pass = "";
        String ssl_pem_trusted_certs_path = "";

        // Get the Path to the required server certificates
        String ssl_pkcs12_path = o.getServerCredentialFilePath() != null ? o.getServerCredentialFilePath() : "";
        String ssl_pkcs12_pass = o.getServerCredentialFilePassphrase() != null ? o.getServerCredentialFilePassphrase() : "";

        // PKCS#12 certificates are converted to pem in the C++ client.
        SSLContext.file_format format = SSLContext.file_format.pem;

        // Grid mode is called FakeSSLMode in the Java client
        boolean use_grid_ssl = o.isFakeSSLMode();

        // Get the SSL protocol string that has been used for initialization
        String ssl_method_string = o.getSSLProtocolString() != null ? o.getSSLProtocolString() : "";

        // Verifying certificates is not enabled in Java
        // TODO: Ask Robert if this is true
        boolean ssl_verify_certificates = false;
        IntVector ssl_ignore_verify_errors = new IntVector(0);

        return new SSLOptionsProxy(ssl_pem_path, ssl_pem_cert_path, ssl_pem_key_pass, ssl_pem_trusted_certs_path,
                ssl_pkcs12_path, ssl_pkcs12_pass, format, use_grid_ssl, ssl_verify_certificates,
                ssl_ignore_verify_errors, ssl_method_string);
    }

    public static StringMap keyValueListToMap(List<KeyValuePair> list) {
        StringMap map = new StringMap();
        for (KeyValuePair kv : list) {
            map.set(kv.getKey(), kv.getValue());
        }
        return map;
    }
}
