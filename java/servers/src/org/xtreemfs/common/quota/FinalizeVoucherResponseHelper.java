/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.quota;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDFinalizeVouchersResponse;

/**
 * The class provides the commonly used functions for creating and validating a OSDFinalizeVouchersResponse
 */
public class FinalizeVoucherResponseHelper {

    private final String sharedSecret;

    public FinalizeVoucherResponseHelper(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    /**
     * Calculates the signature by concatenating the single values with each other, adding the sharedSecret and using
     * the MD5 algorithm (HMAC-MD5).
     * 
     * @param uuid
     *            osd uuid
     * @param filesize
     *            new filesize
     * @param truncateEpoch
     *            current truncate epoch
     * @param expireTimeSet
     *            set of all finalized expireTimes
     * @return a signature for a FinalizeVouchersResponse
     */
    public String createSignature(String uuid, long filesize, long truncateEpoch, Set<Long> expireTimeSet) {

        String plainText = uuid + Long.toString(filesize) + Long.toString(truncateEpoch);
        for (Long expireTime : expireTimeSet) {
            plainText += expireTime.toString();
        }
        plainText += sharedSecret;

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

    /**
     * Validates the signature of the server response by recreating the signature with the parameters and response
     * information
     * 
     * @param response
     * @param expireTimeSet
     * @return true, if the signature is valid
     */
    public boolean validateSignature(OSDFinalizeVouchersResponse response, Set<Long> expireTimeSet) {
        String signature = createSignature(response.getOsdUuid(), response.getSizeInBytes(),
                response.getTruncateEpoch(), expireTimeSet);

        if (signature != null && signature.equals(response.getServerSignature())) {
            return true;
        } else {
            return false;
        }

    }

}
