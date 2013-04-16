/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;

/**
 * Use like this: <br>
 * 1) <br>
 * ParamsBuilder builder = new ParamsBuilder();<br>
 * builder.setX("x"); <br>
 * builder.setY("y"); <br>
 * Params params = builder.build(); <br>
 * 
 * 2) <br>
 * Params params = new ParamsBuilder().setX("x").setY("y").build();<br>
 * 
 * @author jensvfischer
 */
public class ParamsBuilder {

    int        numberOfThreads       = DefaultParams.numberOfThreads;
    int        numberOfRepetitions   = DefaultParams.numberOfRepetitions;
    long       sequentialSizeInBytes = DefaultParams.sequentialSizeInBytes;
    long       randomSizeInBytes     = DefaultParams.randomSizeInBytes;
    long       basefileSizeInBytes   = DefaultParams.basefileSizeInBytes;
    int        randomIOFilesize      = DefaultParams.randomIOFilesize;
    String     userName              = DefaultParams.userName;
    String     group                 = DefaultParams.group;
    String     osdPassword           = DefaultParams.osdPassword;
    String     dirAddress            = DefaultParams.dirAddress;
    RPC.Auth   auth                  = DefaultParams.auth;
    SSLOptions sslOptions            = DefaultParams.sslOptions;
    Options    options               = DefaultParams.options;
    int        stripeSizeInBytes     = DefaultParams.stripeSizeInBytes;
    int        stripeWidth           = DefaultParams.stripeWidth;
    boolean    noCleanup             = DefaultParams.noCleanup;
    boolean    noCleanupOfVolumes    = DefaultParams.noCleanupOfVolumes;
    boolean    noCleanupOfBasefile   = DefaultParams.isNoCleanupOfBasefile;

    public ParamsBuilder() {
    }

    public ParamsBuilder setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        return this;
    }

    public void setNumberOfRepetitions(int numberOfRepetitions) {
        this.numberOfRepetitions = numberOfRepetitions;
    }

    public ParamsBuilder setSequentialSizeInBytes(long sequentialSizeInBytes) {
        this.sequentialSizeInBytes = sequentialSizeInBytes;
        return this;
    }

    public ParamsBuilder setRandomSizeInBytes(long randomSizeInBytes) {
        this.randomSizeInBytes = randomSizeInBytes;
        return this;
    }

    public ParamsBuilder setBasefileSizeInBytes(long basefileSizeInBytes) {
        this.basefileSizeInBytes = basefileSizeInBytes;
        return this;
    }

    public ParamsBuilder setRandomIOFilesize(int randomIOFilesize) {
        this.randomIOFilesize = randomIOFilesize;
        return this;
    }

    public ParamsBuilder setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public ParamsBuilder setGroup(String group) {
        this.group = group;
        return this;
    }

    public ParamsBuilder setOsdPassword(String osdPassword) {
        this.osdPassword = osdPassword;
        return this;
    }

    public ParamsBuilder setDirAddress(String dirAddress) {
        this.dirAddress = dirAddress;
        return this;
    }

    public ParamsBuilder setAuth(RPC.Auth auth) {
        this.auth = auth;
        return this;
    }

    public ParamsBuilder setSslOptions(SSLOptions sslOptions) {
        this.sslOptions = sslOptions;
        return this;
    }

    public ParamsBuilder setOptions(Options options) {
        this.options = options;
        return this;
    }

    public void setStripeSizeInBytes(int stripeSizeInBytes) {
        this.stripeSizeInBytes = stripeSizeInBytes;
    }

    public void setStripeWidth(int stripeWidth) {
        this.stripeWidth = stripeWidth;
    }

    public void setNoCleanup(boolean noCleanup) {
        this.noCleanup = noCleanup;
    }

    public void setNoCleanupOfVolumes(boolean noCleanupOfVolumes) {
        this.noCleanupOfVolumes = noCleanupOfVolumes;
    }

    public void setNoCleanupOfBasefile(boolean noCleanupOfBasefile) {
        this.noCleanupOfBasefile = noCleanupOfBasefile;
    }

    public Params build() {
        return new Params(this);
    }

}
