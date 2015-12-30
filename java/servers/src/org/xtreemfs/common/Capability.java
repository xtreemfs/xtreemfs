/*
 * Copyright (c) 2009 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.xtreemfs.common.quota.QuotaConstants;
import org.xtreemfs.foundation.TimeSync;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SnapConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.TraceConfig;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;


/**
 * This class implements a Java representation of a capability.
 * 
 * In general, a capability can be seen as a token granting the permission to
 * carry out an operation on a remote server.
 * 
 * When a client wants open a file, the MRC checks whether the respective kind
 * of access is granted. If so, the MRC sends a capability to the client, which
 * in turn sends the capability to the OSD when file contents are accessed or
 * modified. The OSD has to check whether the capability is valid. A capability
 * is valid as long as it has a correct signature and has not expired yet.
 * Capabilities can be renewed in order to extend their validity.
 * 
 * Each capability contains a file ID, a string representing the access mode, an
 * expiration time stamp representing the time in seconds from 1/1/1970, a
 * string containing data that can be used to verify the client identity, as
 * well as a signature added by the MRC.
 * 
 * 
 * @author stender
 * 
 */
public class Capability {
    
    private final XCap         xcap;
    
    private final String sharedSecret;

    /**
     * Creates a capability from a given set of data. A signature will be added
     * automatically. This constructor is meant to initially create a capability
     * at the MRC.
     *
     * @param fileId
     *            the file ID
     * @param accessMode
     *            the access mode
     * @param validity
     *            the relative validity time span in seconds
     * @param expires
     *            the absolute expiration time stamp (seconds since 1970)
     * @param epochNo
     *            the epoch number associated with the capability; epoch numbers
     *            are incremented each time the file is truncated or deleted
     * @param sharedSecret
     *            the shared secret to be used to sign the capability
     */
    public Capability(String fileId, int accessMode, int validity, long expires, String clientIdentity,
                      int epochNo, boolean replicateOnClose, SnapConfig snapConfig, long snapTimestamp, String sharedSecret) {
        this(fileId, accessMode, validity, expires, clientIdentity, epochNo, replicateOnClose, snapConfig,
                snapTimestamp, false, "", "", QuotaConstants.UNLIMITED_VOUCHER, 0L, FileType.DEFAULT, "", sharedSecret);
    }

    /**
     * Creates a capability from a given set of data. A signature will be added automatically. This constructor is meant
     * to initially create a capability at the MRC.
     * 
     * @param fileId
     *            the file ID
     * @param accessMode
     *            the access mode
     * @param validity
     *            the relative validity time span in seconds
     * @param expires
     *            the absolute expiration time stamp (seconds since 1970)
     * @param clientIdentity
     * @param epochNo
     *            the epoch number associated with the capability; epoch numbers are incremented each time the file is
     *            truncated or deleted
     * @param replicateOnClose
     * @param snapConfig
     * @param snapTimestamp
     * @param traceRequests
     *            trace IO requests on file / volume
     * @param tracingPolicyConfig
     *            tracing policy config
     * @param tracingPolicy
     *            tracing policy
     * @param fileType
     *            File type
     * @param relatedFileId
     *            FileID of a related File
     * @param sharedSecret
     *            the shared secret to be used to sign the capability
     */
    public Capability(String fileId, int accessMode, int validity, long expires, String clientIdentity,
        int epochNo, boolean replicateOnClose, SnapConfig snapConfig, long snapTimestamp, boolean traceRequests,
            String tracingPolicy, String tracingPolicyConfig, long voucherSize, long expireMS, FileType fileType,
            String relatedFileId, String sharedSecret) {

        this.sharedSecret = sharedSecret;

        XCap.Builder builder = XCap.newBuilder().setAccessMode(accessMode).setClientIdentity(clientIdentity).
                setExpireTimeS(expires).setExpireTimeoutS(validity).setFileId(fileId).
                setReplicateOnClose(replicateOnClose).setTruncateEpoch(epochNo).setSnapConfig(snapConfig).
                setSnapTimestamp(snapTimestamp).setVoucherSize(voucherSize).setExpireTimeMs(expireMS).
                setFileType(fileType).setRelatedFileId(relatedFileId);

        if(traceRequests && !tracingPolicy.equals("")) {
            TraceConfig.Builder traceConfigBuilder = TraceConfig.newBuilder();
            traceConfigBuilder.setTraceRequests(traceRequests);
            traceConfigBuilder.setTracingPolicyConfig(tracingPolicyConfig);
            traceConfigBuilder.setTracingPolicy(tracingPolicy);
            builder.setTraceConfig(traceConfigBuilder.build());
        }

        final String sig = calcSignature(builder);
        builder.setServerSignature(sig);
        xcap=builder.build();
    }

    public Capability(String fileId, int accessMode, int validity, long expires, String clientIdentity, int epochNo,
            boolean replicateOnClose, SnapConfig snapConfig, long snapTimestamp, long voucherSize, long expireMS,
            String sharedSecret) {
        this(fileId, accessMode, validity, expires, clientIdentity, epochNo, replicateOnClose, snapConfig,
                snapTimestamp, false, "", "", voucherSize, expireMS, FileType.DEFAULT, "", sharedSecret);
    }
    
    /**
     * Wrapper for XCap objects.
     * 
     * @param xcap
     *            the parsed XCap object
     * @param sharedSecret
     *            the shared secret (from configuration file)
     */
    public Capability(XCap xcap, String sharedSecret) {
        this.xcap = xcap;
        this.sharedSecret = sharedSecret;
    }
    
    public XCap getXCap() {
        return this.xcap;
    }
    
    public String getFileId() {
        return xcap.getFileId();
    }
    
    public int getAccessMode() {
        return xcap.getAccessMode();
    }
    
    /**
     * returns the absolute time, when the capability expires (in seconds)
     * 
     * @return
     */
    public long getExpires() {
        return xcap.getExpireTimeS();
    }
    
    public String getClientIdentity() {
        return xcap.getClientIdentity();
    }
    
    public int getEpochNo() {
        return xcap.getTruncateEpoch();
    }
    
    public String getSignature() {
        return xcap.getServerSignature();
    }
    
    /**
     * Checks whether the capability is valid.
     * 
     * @return <code>true</code>, if it hasn't expired yet and the signature is
     *         valid, <code>false</code>, otherwise
     */
    public boolean isValid() {
        return !hasExpired() && hasValidSignature();
    }
    
    /**
     * Checks whether the capability has expired.
     * 
     * @return <code>true</code>, if the current system time is after the
     *         expiration time stamp <code>false</code>, otherwise
     */
    public boolean hasExpired() {
        return TimeSync.getGlobalTime() / 1000 > xcap.getExpireTimeS();
    }
    
    /**
     * Checks whether the capability has a valid signature.
     * 
     * @return <code>true</code>, if the signature is valid, <code>false</code>,
     *         otherwise
     */
    public boolean hasValidSignature() {
        return xcap.getServerSignature().equals(calcSignature(xcap.toBuilder()));
    }
    
    public boolean isReplicateOnClose() {
        return xcap.getReplicateOnClose();
    }
    
    public SnapConfig getSnapConfig() {
        return xcap.getSnapConfig();
    }
    
    public long getSnapTimestamp() {
        return xcap.getSnapTimestamp();
    }

    public TraceConfig getTraceConfig() {
        return xcap.getTraceConfig();
    }
    
    public long getVoucherSize() {
        return xcap.getVoucherSize();
    }

    public long getExpireMs() {
        return xcap.getExpireTimeMs();
    }

    /**
     * Returns a string representation of the capability.
     * 
     * @return a JSON-formatted string representing the capability.
     */
    @Override
    public String toString() {
        return xcap.toString();
    }
    
    protected String calcSignature(XCap.Builder builder) {
        
        // right now, we use a shared secret between MRC and OSDs
        // as soon as we have a Public Key Infrastructure, signatures
        // will be generated and checked by means of asymmetric encryption
        // techniques
        
        String plainText = builder.getFileId() + Integer.toString(builder.getAccessMode())
            + Long.toString(builder.getExpireTimeS()) + Long.toString(builder.getTruncateEpoch())
            + Long.toString(builder.getSnapConfig().getNumber()) + Long.toString(builder.getSnapTimestamp())
                + Long.toString(builder.getVoucherSize()) + Long.toString(builder.getExpireTimeMs())
                + Long.toString(builder.getFileType().getNumber()) + builder.getRelatedFileId() + sharedSecret;
        
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(plainText.getBytes());
            byte[] digest = md5.digest();
            
            return OutputUtils.byteArrayToHexString(digest);
        } catch (NoSuchAlgorithmException exc) {
            Logging.logError(Logging.LEVEL_ERROR, this, exc);
            return null;
        }
    }
    
}
