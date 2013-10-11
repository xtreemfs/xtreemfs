/*
 * Copyright (c) 2012-2013 by Jens V. Fischer, Zuse Institute Berlin
 *               
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.benchmark;

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

    private int        numberOfThreads       = 1;
    private int        numberOfRepetitions   = 1;
    private long       sequentialSizeInBytes = 10L * BenchmarkUtils.MiB_IN_BYTES;
    private long       randomSizeInBytes     = 10L * BenchmarkUtils.MiB_IN_BYTES;
    private long       basefileSizeInBytes   = 3L * BenchmarkUtils.GiB_IN_BYTES;
    private int        filesize              = 4 * BenchmarkUtils.KiB_IN_BYTES;
    private String     userName              = "root";
    private String     group                 = "root";
    private String     adminPassword         = "";
    private String     dirAddress            = "127.0.0.1:32638";
    private RPC.Auth   auth                  = authNone;
    private SSLOptions sslOptions            = null;
    private Options    options               = new Options();
    private String     osdSelectionPolicies  = "1000,3002";
    private String     osdSelectionUuids     = "";
    private int        stripeSizeInBytes     = 128 * BenchmarkUtils.KiB_IN_BYTES;
    private int        stripeWidth           = 1;
    private boolean    noCleanup             = false;
    private boolean    noCleanupOfVolumes    = false;
    private boolean    noCleanupOfBasefile   = false;
    private boolean    osdCleanup            = false;

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
        if (numberOfRepetitions < 1)
            throw new IllegalArgumentException("numberOfRepetitions < 1 not allowed");
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
        if (numberOfThreads < 1)
            throw new IllegalArgumentException("numberOfThreads < 1 not allowed");

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
        if (sequentialSizeInBytes < 0)
            throw new IllegalArgumentException("sequentialSizeInBytes < 0 not allowed");
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
        if (randomSizeInBytes < 0)
            throw new IllegalArgumentException("randomSizeInBytes < 0 not allowed");
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
        if (basefileSizeInBytes < 1)
            throw new IllegalArgumentException("basefileSizeInBytes < 1 not allowed");
        this.basefileSizeInBytes = basefileSizeInBytes;
        return this;
    }

    /**
     * Set the size of files in filebased benchmark. <br/>
     * Default: 4 KiB.
     * 
     * @param filesize
     * @return the builder
     */
    public ConfigBuilder setFilesize(int filesize) {
        if (filesize < 1)
            throw new IllegalArgumentException("filesize < 1 not allowed");
        this.filesize = filesize;
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
        if (userName.isEmpty())
            throw new IllegalArgumentException("Empty username not allowed");
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
        if (group.isEmpty())
            throw new IllegalArgumentException("Empty group name not allowed");
        this.group = group;
        return this;
    }

    /**
     * Set the password for accessing the osd(s) <br/>
     * Default: "".
     * 
     * @param adminPassword
     * @return the builder
     */
    public ConfigBuilder setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
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
     * This method assumes, that {@link #setSelectOsdsByUuid(String)} is not used.<br/>
     * Default: "1000,3002" (Default OSD filter, Shuffling).
     * 
     * @param policies
     * @return the builder
     */
    public ConfigBuilder setOsdSelectionPolicies(String policies) {
        if (!this.osdSelectionUuids.equals(""))
            throw new IllegalArgumentException("Setting a OSD selection policy is not allowed if selecting the OSD by UUID is used.");
        this.osdSelectionPolicies = policies;
        return this;
    }

    /**
     * Set the UUID-based filter policy (ID 1002) as OSD selection policy and set the uuids to be used by the policy
     * (applied when creating volumes). <br/>
     * This method assumes, that {@link #setOsdSelectionPolicies(String)} is not used.   <br/>
     * 
     * Default: see {@link #setOsdSelectionPolicies(String)}.
     * 
     * @param uuids
     *            the uuids
     * @return the builder
     */
    public ConfigBuilder setSelectOsdsByUuid(String uuids) {
        if (!this.osdSelectionPolicies.equals("1000,3002"))
            throw new IllegalArgumentException("Selecting the OSD by UUID is not allowed if Setting a OSD selection policy is used.");
        this.osdSelectionPolicies = "1002";
        this.osdSelectionUuids = uuids;
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
     * If set, the files and volumes created during the benchmarks will not be deleted. <br/>
     * Default: false.
     *
     * @return the builder
     */
    public ConfigBuilder setNoCleanup() {
        this.noCleanup = true;
        return this;
    }

    /**
     * If set, the volumes created during the benchmarks will not be deleted. <br/>
     * Default: false.
     * 
     * @return the builder
     */
    public ConfigBuilder setNoCleanupOfVolumes() {
        this.noCleanupOfVolumes = true;
        return this;
    }

    /**
     * If set, a basefile created during benchmarks will not be deleted. <br/>
     * Default: false.
     * 
     * @return the builder
     */
    public ConfigBuilder setNoCleanupOfBasefile() {
        this.noCleanupOfBasefile = true;
        return this;
    }

    /**
     * If set, a OSD Cleanup will be done at the end of all benchmarks. This might be needed to actually delete
     * the storage blocks from the OSD after deleting volumes. <br/>
     * Default: false.
     *
     * @return the builder
     */
    public ConfigBuilder setOsdCleanup() {
        this.osdCleanup = true;
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

    int getFilesize() {
        return filesize;
    }

    String getUserName() {
        return userName;
    }

    String getGroup() {
        return group;
    }

    String getAdminPassword() {
        return adminPassword;
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

    String getOsdSelectionUuids() {
        return osdSelectionUuids;
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
        verifySizes();
        verifyNoCleanup();
        return new Config(this);
    }

    private void verifyNoCleanup() {
        if (noCleanupOfBasefile && !noCleanup && !noCleanupOfVolumes)
            throw new IllegalArgumentException("noCleanupOfBasefile only works with noCleanup or noCleanupVolumes");
    }

    private void verifySizes() {

        if (sequentialSizeInBytes % (stripeSizeInBytes * stripeWidth) != 0)
            throw new IllegalArgumentException(
                    "sequentialSizeInBytes must satisfy: size mod (stripeSize * stripeWidth) == 0");
        if (randomSizeInBytes % (stripeSizeInBytes * stripeWidth) != 0)
            throw new IllegalArgumentException("randomSizeInBytes: size mod (stripeSize * stripeWidth) == 0");
        if (randomSizeInBytes % filesize != 0)
            throw new IllegalArgumentException(
                    "Size for filebased benchmarks (i.e. randomSizeInBytes) must satisfy: size mod filesize == 0");
        if (basefileSizeInBytes < randomSizeInBytes)
            throw new IllegalArgumentException("Basefile < random size");
    }
}
