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
 * For documentation of the parameters and default values see {@link ConfigBuilder}.
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

    private final int                 numberOfThreads;
    private final int                 numberOfRepetitions;
    private final long                sequentialSizeInBytes;
    private final long                randomSizeInBytes;
    private final long                basefileSizeInBytes;
    private final int                 filesize;
    private final String              userName;
    private final String              group;
    private final String              osdPassword;
    private final String              dirAddress;
    private final String              mrcAddress;
    private final RPC.UserCredentials userCredentials;
    private final RPC.Auth            auth;
    private final SSLOptions          sslOptions;
    private final Options             options;
    private final String              osdSelectionPolicies;
    private String                    osdSelectionUuids;
    private final int                 stripeSizeInBytes;
    private final int                 stripeWidth;
    private final boolean             noCleanup;
    private final boolean             noCleanupOfVolumes;
    private final boolean             noCleanupOfBasefile;
    private final boolean             osdCleanup;

    /**
     * Build Params from {@link ConfigBuilder}. Should only be called from
     * {@link ConfigBuilder#build()}
     * 
     * @param builder
     * @throws Exception
     */
    public Config(ConfigBuilder builder) throws Exception {
        this.numberOfThreads = builder.getNumberOfThreads();
        this.numberOfRepetitions = builder.getNumberOfRepetitions();
        this.sequentialSizeInBytes = builder.getSequentialSizeInBytes();
        this.randomSizeInBytes = builder.getRandomSizeInBytes();
        this.basefileSizeInBytes = builder.getBasefileSizeInBytes();
        this.filesize = builder.getFilesize();
        this.userName = builder.getUserName();
        this.group = builder.getGroup();
        this.osdPassword = builder.getOsdPassword();
        this.dirAddress = builder.getDirAddress();
        this.userCredentials = RPC.UserCredentials.newBuilder().setUsername(builder.getUserName()).addGroups(builder.getGroup())
                .build();
        this.sslOptions = builder.getSslOptions();
        this.options = builder.getOptions();
        this.osdSelectionPolicies = builder.getOsdSelectionPolicies();
        this.osdSelectionUuids = builder.getOsdSelectionUuids();
        this.stripeSizeInBytes = builder.getStripeSizeInBytes();
        this.stripeWidth = builder.getStripeWidth();
        this.mrcAddress = getMRCAddress(dirAddress, userCredentials, sslOptions, options);
        this.auth = builder.getAuth();
        this.noCleanup = builder.isNoCleanup();
        this.noCleanupOfVolumes = builder.isNoCleanupOfVolumes();
        this.noCleanupOfBasefile = builder.isNoCleanupOfBasefile();
        this.osdCleanup = builder.isOsdCleanup();
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

    int getNumberOfThreads() {
        return numberOfThreads;
    }

    int getNumberOfRepetitions() {
        return numberOfRepetitions;
    }

    long getSequentialSizeInBytes() {
        return sequentialSizeInBytes;
    }

    long getRandomSizeInBytes() {
        return randomSizeInBytes;
    }

    long getBasefileSizeInBytes() {
        return basefileSizeInBytes;
    }

    int getFilesize() {
        return filesize;
    }

    String getUserName() {
        return userName;
    }

    String getGroup() {
        return group;
    }

    String getOsdPassword() {
        return osdPassword;
    }

    String getDirAddress() {
        return dirAddress;
    }

    String getMrcAddress() {
        return mrcAddress;
    }

    RPC.UserCredentials getUserCredentials() {
        return userCredentials;
    }

    RPC.Auth getAuth() {
        return auth;
    }

    SSLOptions getSslOptions() {
        return sslOptions;
    }

    Options getOptions() {
        return options;
    }

    String getOsdSelectionPolicies() {
        return osdSelectionPolicies;
    }

    boolean isOsdSelectionByUuids(){
        return !osdSelectionUuids.equals("");
    }

    String getOsdSelectionUuids() {
        return osdSelectionUuids;
    }

    int getStripeSizeInBytes() {
        return stripeSizeInBytes;
    }

    int getStripeSizeInKiB() {
        return stripeSizeInBytes/1024;
    }

    int getStripeWidth() {
        return stripeWidth;
    }

    boolean isNoCleanup() {
        return noCleanup;
    }

    boolean isNoCleanupOfVolumes() {
        return noCleanupOfVolumes;
    }

    boolean isNoCleanupOfBasefile() {
        return noCleanupOfBasefile;
    }

    boolean isOsdCleanup() {
        return osdCleanup;
    }
}
