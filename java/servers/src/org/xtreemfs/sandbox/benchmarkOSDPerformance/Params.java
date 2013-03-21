/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmarkOSDPerformance;

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

    Params(ParamsBuilder builder) {
        this.numberOfThreads = builder.numberOfThreads;
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
        this.mrcAddress = getMRCAddress(dirAddress, userCredentials, sslOptions, options);
        this.auth = builder.auth;
    }

    private String getMRCAddress(String dirAddress, RPC.UserCredentials userCredentials, SSLOptions sslOptions,
            Options options) {

        AdminClient client = BenchmarkClientFactory.getNewClient(dirAddress, userCredentials, sslOptions, options);

        UUIDResolver resolver = (ClientImplementation) client;

        String mrcUUID = null;
        String mrcAddress = null;
        try {
            mrcUUID = client.getServiceByType(DIR.ServiceType.SERVICE_TYPE_MRC).getServices(0).getUuid();
            mrcAddress = resolver.uuidToAddress(mrcUUID);
        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.tool, this,
                    "Error while trying to get the MRC Address. Errormessage: %s", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return mrcAddress;
    }

}
