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
 * Builder for the {@link Config} datastructure.
 * <p/>
 * For documentation of the parameters and default values see {@link Config}.
 * <p/>
 * 
 * Use like this: <br/>
 * <code>
 * ParamsBuilder builder = new ParamsBuilder();<br/>
 * builder.setX("x"); <br/>
 * builder.setY("y"); <br/>
 * Params params = builder.build(); <br/>
 * </code> or like this <br/>
 * <code>
 * Params params = new ParamsBuilder().setX("x").setY("y").build();<br/>
 * </code>
 * 
 * The {@link Controller}, the {@link ConfigBuilder} and {@link Config} represent the API to the benchmark library.
 * 
 * @author jensvfischer
 */
public class ConfigBuilder {

    int               numberOfThreads       = 1;
    int               numberOfRepetitions   = 1;
    long              sequentialSizeInBytes = 10L * BenchmarkUtils.getMiB_IN_BYTES();
    long              randomSizeInBytes     = 10L * BenchmarkUtils.getMiB_IN_BYTES();
    long              basefileSizeInBytes   = 3L * BenchmarkUtils.getGiB_IN_BYTES();
    int               randomIOFilesize      = 4 * BenchmarkUtils.getKiB_IN_BYTES();
    String            userName              = "root";
    String            group                 = "root";
    String            osdPassword           = "";
    String            dirAddress            = "127.0.0.1:32638";
    RPC.Auth          auth                  = authNone;
    SSLOptions        sslOptions            = null;
    Options           options               = new Options();
    String            osdSelectionPolicies  = "1000,3002";
    int               stripeSizeInBytes     = 128 * BenchmarkUtils.getKiB_IN_BYTES();
    int               stripeWidth           = 1;
    boolean           noCleanup             = false;
    boolean           noCleanupOfVolumes    = false;
    boolean           noCleanupOfBasefile   = false;
    boolean           osdCleanup            = false;

    /**
     * Instantiate an builder (all values are the default values, see {@link Config}).
     */
    public ConfigBuilder() {
    }

    /**
     * See {@link Config#numberOfRepetitions}
     * 
     * @param numberOfRepetitions
     * @return
     */
    public ConfigBuilder setNumberOfRepetitions(int numberOfRepetitions) {
        this.numberOfRepetitions = numberOfRepetitions;
        return this;
    }

    /**
     * See {@link Config#numberOfThreads}
     * 
     * @param numberOfThreads
     * @return the builder
     */
    public ConfigBuilder setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        return this;
    }

    /**
     * See {@link Config#sequentialSizeInBytes}
     * 
     * @param sequentialSizeInBytes
     * @return the builder
     */
    public ConfigBuilder setSequentialSizeInBytes(long sequentialSizeInBytes) {
        this.sequentialSizeInBytes = sequentialSizeInBytes;
        return this;
    }

    /**
     * See {@link Config#randomSizeInBytes}
     * 
     * @param randomSizeInBytes
     * @return the builder
     */
    public ConfigBuilder setRandomSizeInBytes(long randomSizeInBytes) {
        this.randomSizeInBytes = randomSizeInBytes;
        return this;
    }

    /**
     * See {@link Config#basefileSizeInBytes}
     * 
     * @param basefileSizeInBytes
     * @return the builder
     */
    public ConfigBuilder setBasefileSizeInBytes(long basefileSizeInBytes) {
        this.basefileSizeInBytes = basefileSizeInBytes;
        return this;
    }

    /**
     * See {@link Config#randomIOFilesize}
     * 
     * @param randomIOFilesize
     * @return the builder
     */
    public ConfigBuilder setRandomIOFilesize(int randomIOFilesize) {
        this.randomIOFilesize = randomIOFilesize;
        return this;
    }

    /**
     * See {@link Config#userName}
     * 
     * @param userName
     * @return the builder
     */
    public ConfigBuilder setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    /**
     * See {@link Config#group}
     * 
     * @param group
     * @return the builder
     */
    public ConfigBuilder setGroup(String group) {
        this.group = group;
        return this;
    }

    /**
     * See {@link Config#osdPassword}
     * 
     * @param osdPassword
     * @return the builder
     */
    public ConfigBuilder setOsdPassword(String osdPassword) {
        this.osdPassword = osdPassword;
        return this;
    }

    /**
     * See {@link Config#dirAddress}
     * 
     * @param dirAddress
     * @return the builder
     */
    public ConfigBuilder setDirAddress(String dirAddress) {
        this.dirAddress = dirAddress;
        return this;
    }

    /**
     * See {@link Config#auth}
     * 
     * @param auth
     * @return the builder
     */
    public ConfigBuilder setAuth(RPC.Auth auth) {
        this.auth = auth;
        return this;
    }

    /**
     * See {@link Config#sslOptions}
     * 
     * @param sslOptions
     * @return the builder
     */
    public ConfigBuilder setSslOptions(SSLOptions sslOptions) {
        this.sslOptions = sslOptions;
        return this;
    }

    /**
     * See {@link Config#options}
     * 
     * @param options
     * @return the builder
     */
    public ConfigBuilder setOptions(Options options) {
        this.options = options;
        return this;
    }

	/**
	 * See {@link Config#osdSelectionPolicies}
	 *
	 * @param policies
	 * @return the builder
	 */
	public ConfigBuilder setOsdSelectionPolicies(String policies){
		this.osdSelectionPolicies = policies;
		return this;
	}

    /**
     * See {@link Config#stripeSizeInBytes}
     * 
     * @param stripeSizeInBytes
     * @return the builder
     */
    public ConfigBuilder setStripeSizeInBytes(int stripeSizeInBytes) {
        this.stripeSizeInBytes = stripeSizeInBytes;
        return this;
    }

    /**
     * See {@link Config#stripeWidth}
     * 
     * @param stripeWidth
     * @return the builder
     */
    public ConfigBuilder setStripeWidth(int stripeWidth) {
        this.stripeWidth = stripeWidth;
        return this;
    }

    /**
     * See {@link Config#noCleanup}
     * 
     * @param noCleanup
     * @return the builder
     */
    public ConfigBuilder setNoCleanup(boolean noCleanup) {
        this.noCleanup = noCleanup;
        return this;
    }

    /**
     * See {@link Config#noCleanupOfVolumes}
     * 
     * @param noCleanupOfVolumes
     * @return the builder
     */
    public ConfigBuilder setNoCleanupOfVolumes(boolean noCleanupOfVolumes) {
        this.noCleanupOfVolumes = noCleanupOfVolumes;
        return this;
    }

    /**
     * See {@link Config#noCleanupOfBasefile}
     * 
     * @param noCleanupOfBasefile
     * @return the builder
     */
    public ConfigBuilder setNoCleanupOfBasefile(boolean noCleanupOfBasefile) {
        this.noCleanupOfBasefile = noCleanupOfBasefile;
        return this;
    }

    /**
     * See {@link Config#osdCleanup}
     * 
     * @param osdCleanup
     * @return the builder
     */
    public ConfigBuilder setOsdCleanup(boolean osdCleanup) {
        this.osdCleanup = osdCleanup;
        return this;
    }

    /**
     * Build the {@link Config} object.
     * 
     * @return the build {@link Config} object
     * @throws Exception
     */
    public Config build() throws Exception {
        return new Config(this);
    }

}
