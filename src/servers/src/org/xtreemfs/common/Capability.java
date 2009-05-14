/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.interfaces.XCap;

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
    
    /**
     * default validity for capabilities in seconds
     */
    public static final long DEFAULT_VALIDITY = Long.MAX_VALUE;
        //10 * 60;

    private XCap             xcap;

    private final String     sharedSecret;
    
    
    /**
     * Creates a capability from a given set of data. A signature will be added
     * automatically. This constructor is meant to initially create a capability
     * at the MRC.
     * 
     * @param fileId
     *            the file ID
     * @param accessMode
     *            the access mode
     * @param expires
     *            the expiration time stamp
     * @param epochNo
     *            the epoch number associated with the capability; epoch numbers
     *            are incremented each time the file is truncated or deleted
     * @param sharedSecret
     *            the shared secret to be used to sign the capability
     */
    public Capability(String fileId, int accessMode, long expires,
            String clientIdentity, int epochNo, String sharedSecret) {

        this.sharedSecret = sharedSecret;

        xcap = new XCap(fileId, accessMode, expires, clientIdentity, epochNo, null);
        
        final String sig = calcSignature();
        xcap.setServer_signature(sig);
    }

    /**
     * Wrapper for XCap objects.
     * @param xcap the parsed XCap object
     * @param sharedSecret the shared secret (from configuration file)
     */
    public Capability(XCap xcap, String sharedSecret) {
        this.xcap = xcap;
        this.sharedSecret = sharedSecret;
    }

    public XCap getXCap() {
        return this.xcap;
    }
    
    
    public String getFileId() {
        return xcap.getFile_id();
    }
    
    public int getAccessMode() {
        return xcap.getAccess_mode();
    }
    
    public long getExpires() {
        return xcap.getExpires_s();
    }
    
    public String getClientIdentity() {
        return xcap.getClient_identity();
    }
    
    public int getEpochNo() {
        return xcap.getTruncate_epoch();
    }
    
    public String getSignature() {
        return xcap.getServer_signature();
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
        return TimeSync.getGlobalTime() / 1000 > xcap.getExpires_s();
    }
    
    /**
     * Checks whether the capability has a valid signature.
     * 
     * @return <code>true</code>, if the signature is valid, <code>false</code>,
     *         otherwise
     */
    public boolean hasValidSignature() {
        return xcap.getServer_signature().equals(calcSignature());
    }
    
    /**
     * Returns a string representation of the capability.
     * 
     * @return a JSON-formatted string representing the capability.
     */
    public String toString() {
        return xcap.toString();
    }
    
    protected String calcSignature() {
        
        // right now, we use a shared secret between MRC and OSDs
        // as soon as we have a Public Key Infrastructure, signatures
        // will be generated and checked by means of asymmetric encryption
        // techniques
        
        String plainText = xcap.getFile_id() + Integer.toString(xcap.getAccess_mode()) +
                Long.toString(xcap.getExpires_s()) + Long.toString(xcap.getTruncate_epoch()) +
                sharedSecret;
        
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
