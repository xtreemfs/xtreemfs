/*
 * Copyright (c) 2008-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.VersionManagement;

/**
 * Represents all possible options for libxtreemfs.
 */
public class Options {

    enum XtreemFSServiceType {
        kDIR, kMRC
    };

    // Optimizations.
    /**
     * Maximum number of entries of the StatCache. Default: 100000
     */
    private int        metadataCacheSize     = 100000;

    /**
     * Time to live for MetadataCache entries. Default: 120
     */
    private final long metadataCacheTTLs     = 120;

    /**
     * Enable asynchronous writes. <br>
     * Currently only operative through the native C++ client.
     */
    private boolean    enableAsyncWrites     = false;

    /**
     * Maximum number of pending bytes (of async writes) per file.
     */
    private int        maxWriteahead         = 128 * 1024;

    /**
     * Maximum number of pending async write requests per file. Default: 10
     */
    private final int  maxWriteaheadRequests = 10;

    /**
     * Number of retrieved entries per readdir request. Default: 1024
     */
    private int        readdirChunkSize      = 1024;

    // Error Handling options.
    /**
     * How often shall a failed operation get retried? Default: 40
     */
    private int        maxTries              = 40;
    /**
     * How often shall a failed read operation get retried? Default: 40
     */
    private int        maxReadTries          = 40;
    /**
     * How often shall a failed write operation get retried? Default: 40
     */
    private final int  maxWriteTries         = 40;
    /**
     * How often shall a view be tried to renewed?
     */
    private int        maxViewRenewals     = 5;
    /**
     * How long to wait after a failed request? Default: 15
     */
    private final int  retryDelay_s          = 15;

    /**
     * Stops retrying to execute a synchronous request if this signal was send to the thread responsible for
     * the execution of the request. Default: 0
     */
    private final int  interruptSignal       = 0;

    /**
     * Maximum time until a connection attempt will be aborted. Default: 60
     */
    private final int  connectTimeout_s      = 60;
    /**
     * Maximum time until a request will be aborted and the response returned. Default:
     */
    private final int  requestTimeout_s      = 30;

    /**
     * The RPC Client closes connections after "linger_timeout_s" time of inactivity. Default: 600
     */
    private final int  lingerTimeout_s       = 600;
    
    // SSL options.
    private final String  sslPemCertPath                    = "";
    private final String  sslPemPath                        = "";
    private final String  sslPemKeyPass                     = "";
    private final String  sslPKCS2Path                      = "";
    private final String  sslPKCS12Pass                     = "";

    // Grid Support options.
    /**
     * True, if the XtreemFS Grid-SSL Mode (only SSL handshake, no encryption of data itself) shall be used.
     * Default: false
     */
    private final boolean gridSSL                           = false;
    /**
     * True if the Globus user mapping shall be used. Default: false
     * */
    private final boolean gridAuthModeGlobus                = false;
    /**
     * True if the Unicore user mapping shall be used. Default: false
     * */
    private final boolean gridAuthModeUnicore               = false;
    /**
     * Location of the gridmap file. Default: ""
     */
    private final String  gridGridmapLocation               = "";
    /**
     * Default Location of the Globus gridmap file. Default: "/etc/grid-security/grid-mapfile"
     */
    private final String  gridGridmapLocationDefaultGlobus  = "/etc/grid-security/grid-mapfile";
    /**
     * Default Location of the Unicore gridmap file. Default: "/etc/grid-security/d-grid_uudb"
     */
    private final String  gridGridmapLocationDefaultUnicore = "/etc/grid-security/d-grid_uudb";
    /**
     * Periodic interval after which the gridmap file will be reloaded. Default: 60
     */
    private final int     gridGridmapReloadInterval_m       = 60;                               // 60 minutes = 1
                                                                                           // hour

    // Advanced XtreemFS options
    /**
     * Interval for periodic file size updates in seconds. Default: 60
     */
    private int     periodicFileSizeUpdatesIntervalS  = 60;

    /**
     * Interval for periodic xcap renewal in seconds. Default: 60
     */
    private final int     periodicXcapRenewalIntervalS      = 60;

    /** Interval between requests while waiting for the installation of a new xLocSet. Default: 5 */
    private final int     xLocInstallPollIntervalS          = 5;

    /**
     * Returns the version string and prepends "component".
     */
    String showVersion(String component) {
        return component + VersionManagement.RELEASE_VERSION;
    }

    /**
     * Return the version.
     * 
     */
    String getVersion() {
        return VersionManagement.RELEASE_VERSION;
    }

    /**
     * Creates a new SSLOptions object based on the value of the members: - sslPem_path - sslPemCertPath -
     * sslPemKeyPass - sslPkcs12Path - sslPkcs12Pass - gridSsl || protocol.
     * 
     */
    public SSLOptions generateSSLOptions() {
        SSLOptions sslOptions = null;
        if (sslEnabled()) {
            // TODO: Find out how to create SSLOptions object.
            // sslOptions = new SSLOptions(new FileInputStream(new File(sslPemPath)),
            // sslPemKeyPass, sslPemCertPath,
            // new FileInputStream(new File(sslPKCS2Path)),
            // sslPKCS12Pass, new String(), gridSSL || protocol == Schemes.SCHEME_PBRPCG );
        }
        return null;
    }

    /** Extract volume name and dir service address from dir_volume_url. */
    protected void parseURL(XtreemFSServiceType service_type) {
        // TODO: Implement!
    }

    public boolean sslEnabled() {
        return !sslPemCertPath.isEmpty() || !sslPKCS2Path.isEmpty();
    }

    public int getMaxTries() {
        return maxTries;
    }

    public void setMaxTries(int maxTries) {
        this.maxTries = maxTries;
    }

    public int getMaxWriteTries() {
        return maxWriteTries;
    }

    public int getMaxViewRenewals() {
        return maxViewRenewals;
    }

    public void setMaxViewRenewals(int maxViewRenewals) {
        this.maxViewRenewals = maxViewRenewals;
    }

    public int getRetryDelay_s() {
        return retryDelay_s;
    }

    public boolean isEnableAsyncWrites() {
        return enableAsyncWrites;
    }

    public int getMaxWriteahead() {
        return maxWriteahead;
    }

    public int getMaxWriteaheadRequests() {
        return maxWriteaheadRequests;
    }

    public int getReaddirChunkSize() {
        return readdirChunkSize;
    }
    
    public void setReaddirChunkSize(int readdirChunkSize) {
        this.readdirChunkSize = readdirChunkSize;
    }

    public void setPeriodicFileSizeUpdatesIntervalS(int periodicFileSizeUpdatesIntervalS) {
        this.periodicFileSizeUpdatesIntervalS = periodicFileSizeUpdatesIntervalS;
    }

    public void setEnableAsyncWrites(boolean enableAsyncWrites) {
        this.enableAsyncWrites = enableAsyncWrites;
    }
    
    public void setMaxWriteAhead(int maxWriteAhead) {
        this.maxWriteahead = maxWriteAhead; 
    }

    public void setMaxReadTries(int maxReadTries) {
        this.maxReadTries = maxReadTries;
    }

    public int getMaxReadTries() {
        return maxReadTries;
    }

    public int getMetadataCacheSize() {
        return metadataCacheSize;
    }

    public void setMetadataCacheSize(int metadataCacheSize) {
        this.metadataCacheSize = metadataCacheSize;
    }

    public long getMetadataCacheTTLs() {
        return metadataCacheTTLs;
    }

    public int getInterruptSignal() {
        return interruptSignal;
    }

    public int getConnectTimeout_s() {
        return connectTimeout_s;
    }

    public int getRequestTimeout_s() {
        return requestTimeout_s;
    }

    public int getLingerTimeout_s() {
        return lingerTimeout_s;
    }

    public int getPeriodicXcapRenewalIntervalS() {
        return periodicXcapRenewalIntervalS;
    }

    public int getPeriodicFileSizeUpdatesIntervalS() {
        return periodicFileSizeUpdatesIntervalS;
    }

    public int getXLocInstallPollIntervalS() {
        return xLocInstallPollIntervalS;
    }

}
