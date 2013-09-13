/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import java.lang.reflect.Field;

import org.xtreemfs.common.libxtreemfs.AdminClient;
import org.xtreemfs.common.libxtreemfs.ClientImplementation;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.UUIDResolver;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR;

/**
 * Datastructure holding all parameters for the benchmark library.
 * <p/>
 * 
 * {@link ConfigBuilder} should be used to build this (the given default values are only present if the builder is used
 * <p/>
 * 
 * The {@link Controller}, the {@link ConfigBuilder} and {@link Config} represent the API to the benchmark library.
 * 
 * @author jensvfischer
 */
public class Config {

    /**
     * Number of benchmarks (benchmark threads) to be run in parallel. <br/>
     * Default: 1.
     */
    public final int                 numberOfThreads;

    /**
     * Number of repetitions of a benchmark. <br/>
     * Default: 1.
     */
    public final int                 numberOfRepetitions;

    /**
     * Number of bytes to write or read in a sequential benchmark. <br/>
     * Default: 10 MiB.
     */
    public final long                sequentialSizeInBytes;

    /**
     * Number of bytes to write or read in a random benchmark. <br/>
     * Default: 10 MiB.
     */
    public final long                randomSizeInBytes;

    /**
     * Size of basefile for random benchmarks. <br/>
     * Default: 3 GiB.
     */
    public final long                basefileSizeInBytes;

    /**
     * Size of files in filebased benchmark. <br/>
     * Default: 4 KiB.
     */
    public final int                 randomIOFilesize;

    /**
     * The username to be used when creating files and volumes <br/>
     * Default: root.
     */
    public final String              userName;

    /**
     * The group to be used when creating files and volumes. <br/>
     * Default: root.
     */
    public final String              group;

    /**
     * The password for accessing the osd * <br/>
     * Default: "".
     */
    public final String              osdPassword;

    /**
     * The address of the DIR Server. <br/>
     * Default: 127.0.0.1:32638.
     */
    public final String              dirAddress;

    /**
     * The address of the MRC Server, fetched from the DIR the instantiation of {@link Config}.
     */
//    public final String              mrcAddress;

    /**
     * The RPC user credentials. Build from {@link #userName} and {@link #group} during instatiation of {@link Config}.
     */
    public final RPC.UserCredentials userCredentials;

    /**
     * Authentication Provider. <br/>
     * Default: NullAuthProvider.
     */
    public final RPC.Auth            auth;

    /**
     * SSL Options for SSL Authetification Provider. <br/>
     * Default: null.
     */
    public final SSLOptions          sslOptions;

    /**
     * The libxtreemfs {@link org.xtreemfs.common.libxtreemfs.Options}.
     */
    public final Options             options;


	/**
	 * The OSD selection policies used when creating volumes. <br/>
	 * Default: "1000,3002" (Default OSD filter, Shuffling).
	 */
	public final String osdSelectionPolicies;

    /**
     * The size of an OSD storage block ("blocksize") in Bytes. <br/>
     * Default: 131072 (128 KiB). <br/>
     * The size of one write operation is stripeSize * stripeWidth.
     */
    public final int                 stripeSizeInBytes;

    /**
     * The size of an OSD storage block ("blocksize") in KiB. Calculated during instantiation of {@link Config}.
     */
    public final int                 getStripeSizeInKiB;

    /**
     * The maximum number of OSDs a file is distributed to. <br/>
     * Default: 1. <br/>
     * The size of one write operation is stripeSize * stripeWidth.
     */
    public final int                 stripeWidth;

    /**
     * If set to true, the files and volumes created during the benchmarks will not be deleted. <br/>
     * Default: false.
     */
    public final boolean             noCleanup;

    /**
     * If set to true, the volumes created during the benchmarks will not be deleted. <br/>
     * Default: false.
     */
    public final boolean             noCleanupOfVolumes;

    /**
     * If set to true, a basefile created during benchmarks will not be deleted. <br/>
     * Default: false.
     */
    public final boolean             noCleanupOfBasefile;

    /**
     * If set to true, a OSD Cleanup will be done at the end of all benchmarks. This might be needed to actually delete
     * the storage blocks from the OSD after deleting volumes. <br/>
     * Default: false.
     * 
     */
    public final boolean             osdCleanup;

    /**
     * Build Params from {@link ConfigBuilder}. Should only be called from
     * {@link ConfigBuilder#build()}
     * 
     * @param builder
     * @throws Exception
     */
    public Config(ConfigBuilder builder) throws Exception {
        this.numberOfThreads = builder.numberOfThreads;
        this.numberOfRepetitions = builder.numberOfRepetitions;
        this.sequentialSizeInBytes = builder.sequentialSizeInBytes;
        this.randomSizeInBytes = builder.randomSizeInBytes;
        this.basefileSizeInBytes = builder.basefileSizeInBytes;
        this.randomIOFilesize = builder.randomIOFilesize;
        this.userName = builder.userName;
        this.group = builder.group;
        this.osdPassword = builder.osdPassword;
        this.dirAddress = builder.dirAddress;
        this.userCredentials = RPC.UserCredentials.newBuilder().setUsername(builder.userName).addGroups(builder.group)
                .build();
        this.sslOptions = builder.sslOptions;
        this.options = builder.options;
		this.osdSelectionPolicies = builder.osdSelectionPolicies;
        this.stripeSizeInBytes = builder.stripeSizeInBytes;
        this.getStripeSizeInKiB = builder.stripeSizeInBytes / 1024;
        this.stripeWidth = builder.stripeWidth;
//        this.mrcAddress = getMRCAddress(dirAddress, userCredentials, sslOptions, options);
        this.auth = builder.auth;
        this.noCleanup = builder.noCleanup;
        this.noCleanupOfVolumes = builder.noCleanupOfVolumes;
        this.noCleanupOfBasefile = builder.noCleanupOfBasefile;
        this.osdCleanup = builder.osdCleanup;
    }

    /* Gets the MRC address from the giben DIR */
    private String getMRCAddress(String dirAddress, RPC.UserCredentials userCredentials, SSLOptions sslOptions,
            Options options) throws Exception {

        AdminClient client = ClientManager.getInstance().getNewClient(dirAddress, userCredentials, sslOptions, options);

        UUIDResolver resolver = (ClientImplementation) client;

        String mrcUUID = client.getServiceByType(DIR.ServiceType.SERVICE_TYPE_MRC).getServices(0).getUuid();
        String mrcAddress = resolver.uuidToAddress(mrcUUID);
        return mrcAddress;
    }

    /* Build string with all the instance parameters as key-value pairs */
    private String getAllValues() throws IllegalAccessException {
        Field[] fields = Config.class.getDeclaredFields();
        StringBuffer result = new StringBuffer();
        for (Field field : fields) {
            String name = field.getName();
            Object value = field.get(this);
            if (name != "userCredentials") {
                result.append(name + ": " + value + ";\n");
            }
        }
        return result.toString();
    }

    @Override
    public String toString() {
        try {
            return getAllValues();
        } catch (IllegalAccessException e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, this,
                    "Access to Params fields not possible.");
            Logging.logError(Logging.LEVEL_ERROR, Logging.Category.tool, e);
        }
        return "Access not possible";
    }

}
