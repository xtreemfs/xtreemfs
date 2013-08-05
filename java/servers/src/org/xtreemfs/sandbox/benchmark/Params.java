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
 * @author jensvfischer
 */
public class Params {

    final int                 numberOfThreads;
    final int                 numberOfRepetitions;
    final long                sequentialSizeInBytes;
    final long                randomSizeInBytes;
    final long                basefileSizeInBytes;
    final int                 randomIOFilesize;
    final String              userName;
    final String              group;
    final String              osdPassword;
    final String              dirAddress;
    final String              mrcAddress;
    final RPC.UserCredentials userCredentials;
    final RPC.Auth            auth;
    final SSLOptions          sslOptions;
    final Options             options;
    int                       stripeSizeInBytes;
    int                       getStripeSizeInKiB;
    int                       stripeWidth;
    final boolean             noCleanup;
    final boolean             noCleanupOfVolumes;
    final boolean             noCleanupOfBasefile;
    boolean                   osdCleanup;


	Params(ParamsBuilder builder) throws Exception {
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
        this.stripeSizeInBytes = builder.stripeSizeInBytes;
        this.getStripeSizeInKiB = builder.stripeSizeInBytes / 1024;
        this.stripeWidth = builder.stripeWidth;
        this.mrcAddress = getMRCAddress(dirAddress, userCredentials, sslOptions, options);
        this.auth = builder.auth;
        this.noCleanup = builder.noCleanup;
        this.noCleanupOfVolumes = builder.noCleanupOfVolumes;
        this.noCleanupOfBasefile = builder.noCleanupOfBasefile;
		this.osdCleanup = builder.osdCleanup;
    }

    private String getMRCAddress(String dirAddress, RPC.UserCredentials userCredentials, SSLOptions sslOptions,
            Options options) throws Exception {

        AdminClient client = BenchmarkClientFactory.getNewClient(dirAddress, userCredentials, sslOptions, options);

        UUIDResolver resolver = (ClientImplementation) client;

        String mrcUUID = client.getServiceByType(DIR.ServiceType.SERVICE_TYPE_MRC).getServices(0).getUuid();
        String mrcAddress = resolver.uuidToAddress(mrcUUID);
        return mrcAddress;
    }

    public String getAllValues() throws IllegalAccessException {
        Field[] fields = Params.class.getDeclaredFields();
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
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, this, e.getMessage());
            e.printStackTrace();
        }
        return "Access not possible";
    }

}
