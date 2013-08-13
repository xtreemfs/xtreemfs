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
 * Builder for the {@link Params} datastructure.
 * <p/>
 * For documentation of the parameters and default values see {@link Params}.
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
 * The {@link Controller}, the {@link ParamsBuilder} and {@link Params} represent the API to the benchmark library.
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

    /**
     * Instantiate an builder (all values are the default values, see {@link Params}).
     */
    public ParamsBuilder() {
    }

    /**
     * See {@link Params#numberOfRepetitions}
     * 
     * @param numberOfRepetitions
     * @return
     */
    public ParamsBuilder setNumberOfRepetitions(int numberOfRepetitions) {
        this.numberOfRepetitions = numberOfRepetitions;
        return this;
    }

    /**
     * See {@link Params#numberOfThreads}
     * 
     * @param numberOfThreads
     * @return the builder
     */
    public ParamsBuilder setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        return this;
    }

    /**
     * See {@link Params#sequentialSizeInBytes}
     * 
     * @param sequentialSizeInBytes
     * @return the builder
     */
    public ParamsBuilder setSequentialSizeInBytes(long sequentialSizeInBytes) {
        this.sequentialSizeInBytes = sequentialSizeInBytes;
        return this;
    }

    /**
     * See {@link Params#randomSizeInBytes}
     * 
     * @param randomSizeInBytes
     * @return the builder
     */
    public ParamsBuilder setRandomSizeInBytes(long randomSizeInBytes) {
        this.randomSizeInBytes = randomSizeInBytes;
        return this;
    }

    /**
     * See {@link Params#basefileSizeInBytes}
     * 
     * @param basefileSizeInBytes
     * @return the builder
     */
    public ParamsBuilder setBasefileSizeInBytes(long basefileSizeInBytes) {
        this.basefileSizeInBytes = basefileSizeInBytes;
        return this;
    }

    /**
     * See {@link Params#randomIOFilesize}
     * 
     * @param randomIOFilesize
     * @return the builder
     */
    public ParamsBuilder setRandomIOFilesize(int randomIOFilesize) {
        this.randomIOFilesize = randomIOFilesize;
        return this;
    }

    /**
     * See {@link Params#userName}
     * 
     * @param userName
     * @return the builder
     */
    public ParamsBuilder setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    /**
     * See {@link Params#group}
     * 
     * @param group
     * @return the builder
     */
    public ParamsBuilder setGroup(String group) {
        this.group = group;
        return this;
    }

    /**
     * See {@link Params#osdPassword}
     * 
     * @param osdPassword
     * @return the builder
     */
    public ParamsBuilder setOsdPassword(String osdPassword) {
        this.osdPassword = osdPassword;
        return this;
    }

    /**
     * See {@link Params#dirAddress}
     * 
     * @param dirAddress
     * @return the builder
     */
    public ParamsBuilder setDirAddress(String dirAddress) {
        this.dirAddress = dirAddress;
        return this;
    }

    /**
     * See {@link Params#auth}
     * 
     * @param auth
     * @return the builder
     */
    public ParamsBuilder setAuth(RPC.Auth auth) {
        this.auth = auth;
        return this;
    }

    /**
     * See {@link Params#sslOptions}
     * 
     * @param sslOptions
     * @return the builder
     */
    public ParamsBuilder setSslOptions(SSLOptions sslOptions) {
        this.sslOptions = sslOptions;
        return this;
    }

    /**
     * See {@link Params#options}
     * 
     * @param options
     * @return the builder
     */
    public ParamsBuilder setOptions(Options options) {
        this.options = options;
        return this;
    }

    /**
     * See {@link Params#stripeSizeInBytes}
     * 
     * @param stripeSizeInBytes
     * @return the builder
     */
    public ParamsBuilder setStripeSizeInBytes(int stripeSizeInBytes) {
        this.stripeSizeInBytes = stripeSizeInBytes;
        return this;
    }

    /**
     * See {@link Params#stripeWidth}
     * 
     * @param stripeWidth
     * @return the builder
     */
    public ParamsBuilder setStripeWidth(int stripeWidth) {
        this.stripeWidth = stripeWidth;
        return this;
    }

    /**
     * See {@link Params#noCleanup}
     * 
     * @param noCleanup
     * @return the builder
     */
    public ParamsBuilder setNoCleanup(boolean noCleanup) {
        this.noCleanup = noCleanup;
        return this;
    }

    /**
     * See {@link Params#noCleanupOfVolumes}
     * 
     * @param noCleanupOfVolumes
     * @return the builder
     */
    public ParamsBuilder setNoCleanupOfVolumes(boolean noCleanupOfVolumes) {
        this.noCleanupOfVolumes = noCleanupOfVolumes;
        return this;
    }

    /**
     * See {@link Params#noCleanupOfBasefile}
     * 
     * @param noCleanupOfBasefile
     * @return the builder
     */
    public ParamsBuilder setNoCleanupOfBasefile(boolean noCleanupOfBasefile) {
        this.noCleanupOfBasefile = noCleanupOfBasefile;
        return this;
    }

    /**
     * See {@link Params#osdCleanup}
     * 
     * @param osdCleanup
     * @return the builder
     */
    public ParamsBuilder setOsdCleanup(boolean osdCleanup) {
        this.osdCleanup = osdCleanup;
        return this;
    }

    /**
     * Build the {@link Params} object.
     * 
     * @return the build {@link Params} object
     * @throws Exception
     */
    public Params build() throws Exception {
        return new Params(this);
    }

}
