/*
 * Copyright (c) 2008-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.net.MalformedURLException;
import java.util.Vector;

import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.VersionManagement;

/**
 * 
 * <br>
 * Sep 2, 2011
 */
public class Options {

    
    enum XtreemFSServiceType {
        kDIR, kMRC
    };


    // XtreemFS URL Options.
    /**
     * URL to the Volume, Form: [pbrpc://]service-hostname[:port]/volume_name.
     * 
     * Depending on the type of operation the service-hostname has to point to the DIR (to open/"mount" a
     * volume) or the MRC (create/delete/list volumes). Depending on this type, the default port differs (DIR:
     * 32638; MRC: 32636).
     */
    String      xtreemfsUrl;

    /** Usually extracted from xtreemfs_url (Form: ip-address:port). */
    String      serviceAddress;
    /** Usually extracted from xtreemfs_url. */
    String      volumeName;
    /** Usually extracted from xtreemfs_url. */
    String      protocol;
    /** Mount point on local system (set by ParseCommandLine()). */
    String      mountPoint;

    // General options.
    /** Log level as string (EMERG|ALERT|CRIT|ERR|WARNING|NOTICE|INFO|DEBUG). */
    String      logLevelString;
    /** If not empty, the output will be logged to a file. */
    String      logFilePath;
    /** True, if "-h" was specified. */
    boolean     showHelp;
    /** True, if argc == 1 was at ParseCommandLine(). */
    boolean     emptyArgumentsList;
    /** True, if -V/--version was specified and the version will be shown only . */
    boolean     showVersion;

    // Optimizations.
    /** Maximum number of entries of the StatCache */
    // TODO: Find appropriate representation of uint64 in java. Now: long
    long        metadataCacheSize;

    /** Time to live for MetadataCache entries. */
    long        metadataCacheTTL_s;
    /** Maximum number of pending bytes (of async writes) per file. */
    int         maxWriteahead;
    /** Maximum number of pending async write requests per file. */
    int         maxWriteaheadRequests;
    /** Number of retrieved entries per readdir request. */
    int         readdirChunkSize;

    // Error Handling options.
    /** How often shall a failed operation get retried? */
    int         maxTries;
    /** How often shall a failed read operation get retried? */
    int         maxReadTries;
    /** How often shall a failed write operation get retried? */
    int         maxWriteTries;
    /** How long to wait after a failed request? */
    int         retryDelay_s;

    /**
     * Stops retrying to execute a synchronous request if this signal was send to the thread responsible for
     * the execution of the request.
     */
    int         interruptSignal;

    /**
     * Maximum time until a connection attempt will be aborted.
     */
    private int connectTimeout_s;
    /**
     * Maximum time until a request will be aborted and the response returned.
     */
    private int requestTimeout_s;

    /**
     * The RPC Client closes connections after "linger_timeout_s" time of inactivity.
     */
    private int lingerTimeout_s;

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

    // SSL options.
    String  sslPemCertPath;
    String  sslPemPath;
    String  sslPemKeyPass;
    String  sslPKCS2Path;
    String  sslPKCS12Pass;

    // Grid Support options.
    /**
     * True, if the XtreemFS Grid-SSL Mode (only SSL handshake, no encryption of data itself) shall be used.
     */
    boolean gridSSL;
    /** True if the Globus user mapping shall be used. */
    boolean gridAuthModeGlobus;
    /** True if the Unicore user mapping shall be used. */
    boolean gridAuthModeUnicore;
    /** Location of the gridmap file. */
    String  gridGridmapLocation;
    /** Default Location of the Globus gridmap file. */
    String  gridGridmapLocationDefaultGlobus;
    /** Default Location of the Unicore gridmap file. */
    String  gridGridmapLocationDefaultUnicore;
    /** Periodic interval after which the gridmap file will be reloaded. */
    int     gridGridmapReloadInterval_m;

    // Advanced XtreemFS options.
    /** Interval for periodic file size updates in seconds. */
    int     periodicFileSizeUpdatesInterval_s;
    /** Interval for periodic xcap renewal in seconds. */
    int     periodicXcapRenewalInterval_s;

    // User mapping.
    /** Type of the UserMapping used to resolve user and group IDs to names. */
    // TODO: Find out what this is.
    // UserMapping::UserMappingType user_mapping_type;

    // private:
    // Sums of options.
    /**
     * Contains all boost program options, needed for parsing and by ShowCommandLineHelp().
     */
    // boost::program_options::options_description all_descriptions_;

    /** Contains descriptions of advanced options. */
    // boost::program_options::options_description hidden_descriptions_;

    /** Set to true if GenerateProgramOptionsDescriptions() was executed. */
    boolean all_descriptions_initialized_;

    // Options itself.
    /** Description of general options (Logging, help). */
    // boost::program_options::options_description general_;

    /** Description of options which improve performance. */
    // boost::program_options::options_description optimizations_;

    /** Description of timeout options etc. */
    // boost::program_options::options_description error_handling_;

    /** Description of SSL related options. */
    // boost::program_options::options_description ssl_options_;

    /** Description of options of the Grid support. */
    // boost::program_options::options_description grid_options_;

    // Hidden options.
    /** Description of options of the Grid support. */
    // boost::program_options::options_description xtreemfs_advanced_options_;

    /**
    * 
    */
    public Options() {
        
    }
    
    /**
     * Temporary constructor for testing. Remove if commandline parsing is implemented
     */
    public Options(int requestTimeout_s, int connectionTimeout_s, int maxTries, int retryDelay_s) {
        this.requestTimeout_s = requestTimeout_s;
        this.connectTimeout_s = connectionTimeout_s;
        this.maxTries = maxTries;
        this.retryDelay_s = retryDelay_s;
        
        readdirChunkSize = 1024;
    }

    /**
     * Set options parsed from command line.
     * 
     * However, it does not set dir_volume_url and does not call ParseVolumeAndDir().
     * 
     * @throws InvalidCommandLineParametersException
     * @throws {@link MalformedURLException}
     */
    public Vector<String> parseCommandLine(String[] args) throws MalformedURLException {
        // TODO: Implement! Find equvivalent of InvalidCommmanLineParameterException
        return new Vector<String>();
    }

    /**
     * Outputs usage of the command line parameters of all options.
     */
    public String showCommandLineHelp() {
        // TODO: Implement!
        return "";
    }

    /**
     * Outputs usage of the command line parameters of volume creation relevant options.
     */
    public String showCommandLineHelpVolumeCreation() {
        // TODO: Implement!
        return "";
    }

    /**
     * Outputs usage of the command line parameters of volume deletion/listing relevant options.
     */
    public String showCommandLineHelpVolumeDeletionAndListing() {
        // TODO: Implement!
        return "";
    }

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
     * Creates a new SSLOptions object based on the value of the members: - ssl_pem_path - ssl_pem_cert_path -
     * ssl_pem_key_pass - ssl_pkcs12_path - ssl_pkcs12_pass - grid_ssl || protocol.
     * 
     */
    public SSLOptions generateSSLOptions() {
        // TODO: Implement
        return null;
    }

    /** Extract volume name and dir service address from dir_volume_url. */
    protected void parseURL(XtreemFSServiceType service_type) {
        // TODO: Implement!
    }
    
    /**
     * Return maxTries. 
     */
    public int getMaxTries() {
        return maxTries;
    }
    
    /**
     * Return retryDelay_s.
     */
    public int getRetryDelay_s() {
        return retryDelay_s;
    }
    
    public int getMaxWriteahead() {
        return maxWriteahead;
    }
    
    public int getReaddirChunkSize() {
        return readdirChunkSize;
    }
}
