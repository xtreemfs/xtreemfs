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

    private int               numberOfThreads       = 1;
    private int               numberOfRepetitions   = 1;
    private long              sequentialSizeInBytes = 10L * BenchmarkUtils.getMiB_IN_BYTES();
    private long              randomSizeInBytes     = 10L * BenchmarkUtils.getMiB_IN_BYTES();
    private long              basefileSizeInBytes   = 3L * BenchmarkUtils.getGiB_IN_BYTES();
    private int               randomIOFilesize      = 4 * BenchmarkUtils.getKiB_IN_BYTES();
    private String            userName              = "root";
    private String            group                 = "root";
    private String            osdPassword           = "";
    private String            dirAddress            = "127.0.0.1:32638";
    private RPC.Auth          auth                  = authNone;
    private SSLOptions        sslOptions            = null;
    private Options           options               = new Options();
    private String            osdSelectionPolicies  = "1000,3002";
    private int               stripeSizeInBytes     = 128 * BenchmarkUtils.getKiB_IN_BYTES();
    private int               stripeWidth           = 1;
    private boolean           noCleanup             = false;
    private boolean           noCleanupOfVolumes    = false;
    private boolean           noCleanupOfBasefile   = false;
    private boolean           osdCleanup            = false;

    /**
     * Instantiate an builder (all values are the default values, see {@link Config}).
     */
    public ConfigBuilder() {
    }

    /**
	 * Set the number of repetitions of a benchmark. <br/>
	 * Default: 1.
     * 
     * @param numberOfRepetitions
     * @return the builder
     */
    public ConfigBuilder setNumberOfRepetitions(int numberOfRepetitions) {
        this.numberOfRepetitions = numberOfRepetitions;
        return this;
    }

    /**
	 * Set the number of benchmarks (benchmark threads) to be run in parallel. <br/>
	 * Default: 1.
     * 
     * @param numberOfThreads
     * @return the builder
     */
    public ConfigBuilder setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
        return this;
    }

    /**
	 * Set the number of bytes to write or read in a sequential benchmark. <br/>
	 * Default: 10 MiB.
     * 
     * @param sequentialSizeInBytes
     * @return the builder
     */
    public ConfigBuilder setSequentialSizeInBytes(long sequentialSizeInBytes) {
        this.sequentialSizeInBytes = sequentialSizeInBytes;
        return this;
    }

    /**
	 * Set the number of bytes to write or read in a random benchmark. <br/>
	 * Default: 10 MiB.
     * 
     * @param randomSizeInBytes
     * @return the builder
     */
    public ConfigBuilder setRandomSizeInBytes(long randomSizeInBytes) {
        this.randomSizeInBytes = randomSizeInBytes;
        return this;
    }

    /**
	 * Set the size of the basefile for random benchmarks. <br/>
	 * Default: 3 GiB.
     * 
     * @param basefileSizeInBytes
     * @return the builder
     */
    public ConfigBuilder setBasefileSizeInBytes(long basefileSizeInBytes) {
        this.basefileSizeInBytes = basefileSizeInBytes;
        return this;
    }

    /**
	 * Set the size of files in filebased benchmark. <br/>
	 * Default: 4 KiB.
     * 
     * @param randomIOFilesize
     * @return the builder
     */
    public ConfigBuilder setRandomIOFilesize(int randomIOFilesize) {
        this.randomIOFilesize = randomIOFilesize;
        return this;
    }

    /**
	 * Set the username to be used when creating files and volumes <br/>
	 * Default: root.
     * 
     * @param userName
     * @return the builder
     */
    public ConfigBuilder setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    /**
	 * Set the group to be used when creating files and volumes. <br/>
	 * Default: root.
     * 
     * @param group
     * @return the builder
     */
    public ConfigBuilder setGroup(String group) {
        this.group = group;
        return this;
    }

    /**
	 * Set the password for accessing the osd(s) <br/>
	 * Default: "".
     * 
     * @param osdPassword
     * @return the builder
     */
    public ConfigBuilder setOsdPassword(String osdPassword) {
        this.osdPassword = osdPassword;
        return this;
    }

    /**
	 * Set the address of the DIR Server. <br/>
	 * Default: 127.0.0.1:3263
     * 
     * @param dirAddress
     * @return the builder
     */
    public ConfigBuilder setDirAddress(String dirAddress) {
        this.dirAddress = dirAddress;
        return this;
    }

    /**
	 * Set the RPC user credentials. Build from the user name and group name during instatiation of {@link Config}.
     * 
     * @param auth
     * @return the builder
     */
    public ConfigBuilder setAuth(RPC.Auth auth) {
        this.auth = auth;
        return this;
    }

    /**
	 * Set the SSL options for SSL Authetification Provider. <br/>
	 * Default: null.
     * 
     * @param sslOptions
     * @return the builder
     */
    public ConfigBuilder setSslOptions(SSLOptions sslOptions) {
        this.sslOptions = sslOptions;
        return this;
    }

    /**
	 * Set the libxtreemfs {@link org.xtreemfs.common.libxtreemfs.Options}.
     * 
     * @param options
     * @return the builder
     */
    public ConfigBuilder setOptions(Options options) {
        this.options = options;
        return this;
    }

	/**
	 * Set the OSD selection policies used when creating volumes. <br/>
	 * Default: "1000,3002" (Default OSD filter, Shuffling).
	 *
	 * @param policies
	 * @return the builder
	 */
	public ConfigBuilder setOsdSelectionPolicies(String policies){
		this.osdSelectionPolicies = policies;
		return this;
	}

    /**
	 * Set the size of an OSD storage block ("blocksize") in Bytes. <br/>
	 * Default: 131072 (128 KiB). <br/>
     * 
     * @param stripeSizeInBytes
     * @return the builder
     */
    public ConfigBuilder setStripeSizeInBytes(int stripeSizeInBytes) {
        this.stripeSizeInBytes = stripeSizeInBytes;
        return this;
    }

    /**
	 * Set the maximum number of OSDs a file is distributed to. <br/>
	 * Default: 1. <br/>
	 * The size of one write operation is stripeSize * stripeWidth.
     * 
     * @param stripeWidth
     * @return the builder
     */
    public ConfigBuilder setStripeWidth(int stripeWidth) {
        this.stripeWidth = stripeWidth;
        return this;
    }

    /**
	 * If set to true, the files and volumes created during the benchmarks will not be deleted. <br/>
	 * Default: false.
     * 
     * @param noCleanup
     * @return the builder
     */
    public ConfigBuilder setNoCleanup(boolean noCleanup) {
        this.noCleanup = noCleanup;
        return this;
    }

    /**
	 * If set to true, the volumes created during the benchmarks will not be deleted. <br/>
	 * Default: false.
     * 
     * @param noCleanupOfVolumes
     * @return the builder
     */
    public ConfigBuilder setNoCleanupOfVolumes(boolean noCleanupOfVolumes) {
        this.noCleanupOfVolumes = noCleanupOfVolumes;
        return this;
    }

    /**
	 * If set to true, a basefile created during benchmarks will not be deleted. <br/>
	 * Default: false.
     * 
     * @param noCleanupOfBasefile
     * @return the builder
     */
    public ConfigBuilder setNoCleanupOfBasefile(boolean noCleanupOfBasefile) {
        this.noCleanupOfBasefile = noCleanupOfBasefile;
        return this;
    }

    /**
	 * If set to true, a OSD Cleanup will be done at the end of all benchmarks. This might be needed to actually delete
	 * the storage blocks from the OSD after deleting volumes. <br/>
	 * Default: false.
     * 
     * @param osdCleanup
     * @return the builder
     */
    public ConfigBuilder setOsdCleanup(boolean osdCleanup) {
        this.osdCleanup = osdCleanup;
        return this;
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

	int getRandomIOFilesize() {
		return randomIOFilesize;
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

	int getStripeSizeInBytes() {
		return stripeSizeInBytes;
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
