/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.sandbox.benchmark;

import static org.xtreemfs.foundation.pbrpc.client.RPCAuthentication.authNone;

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

    static final int  KiB_IN_BYTES          = 1024;
    static final long MiB_IN_BYTES          = 1024 * 1024;
    static final long GiB_IN_BYTES          = 1024 * 1024 * 1024;

    int               numberOfThreads       = 1;
    int               numberOfRepetitions   = 1;
    long              sequentialSizeInBytes = 10L * MiB_IN_BYTES;
    long              randomSizeInBytes     = 10L * MiB_IN_BYTES;
    long              basefileSizeInBytes   = 3L * GiB_IN_BYTES;
    int               randomIOFilesize      = 4 * KiB_IN_BYTES;
    String            userName              = "root";
    String            group                 = "root";
    String            osdPassword           = "";
    String            dirAddress            = "127.0.0.1:32638";
    RPC.Auth          auth                  = authNone;
    SSLOptions        sslOptions            = null;
    Options           options               = new Options();
    int               stripeSizeInBytes     = 128 * KiB_IN_BYTES;
    int               stripeWidth           = 1;
    boolean           noCleanup             = false;
    boolean           noCleanupOfVolumes    = false;
    boolean           noCleanupOfBasefile   = false;
    boolean           osdCleanup            = false;


	public ParamsBuilder() {
    }

    public ParamsBuilder setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        return this;
    }

    public ParamsBuilder setNumberOfRepetitions(int numberOfRepetitions) {
        this.numberOfRepetitions = numberOfRepetitions;
		return this;
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

    public ParamsBuilder setStripeSizeInBytes(int stripeSizeInBytes) {
        this.stripeSizeInBytes = stripeSizeInBytes;
		return this;
    }

    public ParamsBuilder setStripeWidth(int stripeWidth) {
        this.stripeWidth = stripeWidth;
		return this;
    }

    public ParamsBuilder setNoCleanup(boolean noCleanup) {
        this.noCleanup = noCleanup;
		return this;
    }

    public ParamsBuilder setNoCleanupOfVolumes(boolean noCleanupOfVolumes) {
        this.noCleanupOfVolumes = noCleanupOfVolumes;
		return this;
    }

    public ParamsBuilder setNoCleanupOfBasefile(boolean noCleanupOfBasefile) {
        this.noCleanupOfBasefile = noCleanupOfBasefile;
		return this;
    }

	public ParamsBuilder setOsdCleanup(boolean osdCleanup) {
		this.osdCleanup = osdCleanup;
		return this;
	}

	public Params build() throws Exception {
        return new Params(this);
    }

}
