/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin and
    Barcelona Supercomputing Center - Centro Nacional de Supercomputacion.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB), Jesús Malo (BSC)
 */

package org.xtreemfs.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;

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
    public static final long DEFAULT_VALIDITY = 10 * 60;
    
    private final String     fileId;
    
    private final String     accessMode;
    
    private final long       expires;
    
    private final String     sharedSecret;
    
    private final String     signature;
    
    private long             epochNo;
    
    /**
     * Creates a capability from a given set of data. The expiration time stamp
     * will be generated automatically by means of the local system time, and a
     * signature will be added. This constructor is meant to initially create a
     * capability at the MRC.
     * 
     * @param fileId
     *            the file ID
     * @param accessMode
     *            the access mode
     * @param epochNo
     *            the epoch number associated with the capability; epoch numbers
     *            are incremented each time the file is truncated or deleted
     * @param sharedSecret
     *            the shared secret to be used to sign the capability
     */
    public Capability(String fileId, String accessMode, long epochNo, String sharedSecret) {
        
        this.fileId = fileId;
        this.accessMode = accessMode;
        this.epochNo = epochNo;
        this.sharedSecret = sharedSecret;
        
        this.expires = System.currentTimeMillis() / 1000 + DEFAULT_VALIDITY;
        this.signature = calcSignature();
    }
    
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
    public Capability(String fileId, String accessMode, long expires, long epochNo,
        String sharedSecret) {
        this.fileId = fileId;
        this.accessMode = accessMode;
        this.expires = expires;
        this.epochNo = epochNo;
        this.sharedSecret = sharedSecret;
        
        this.signature = calcSignature();
    }
    
    /**
     * Creates a capability from a string representation. This constructor is
     * meant to be used to verify the validity of a capability string received
     * from a remote host.
     * 
     * @param capability
     *            the capability string
     * @param sharedSecret
     *            the shared secret to be used to verify the capability
     * @throws JSONException
     *             if parsing the capability failed
     */
    public Capability(String capability, String sharedSecret) throws JSONException {
        
        List<Object> cap = (List<Object>) JSONParser.parseJSON(new JSONString(capability));
        assert (cap.size() == 6 || cap.size() == 5);
        
        this.sharedSecret = sharedSecret;
        this.fileId = (String) cap.get(0);
        this.accessMode = (String) cap.get(1);
        this.expires = (Long) cap.get(2);
        // ignore the client identity; it cannot be used because OSDs can act as
        // client proxies
        this.epochNo = (Long) cap.get(4);
        this.signature = (String) cap.get(5);
    }
    
    /**
     * Creates a capability from a string representation. This constructor is
     * meant to be used to parse and forward a received capability. <br>
     * <b>It cannot be used to verify capabilities!</b> For this purpose, please
     * use <code>Capability(String capability, String sharedSecret)</code>.
     * 
     * @param capability
     *            the capability string
     * @throws JSONException
     *             if parsing the capability failed
     */
    public Capability(String capability) throws JSONException {
        
        List<Object> cap = (List<Object>) JSONParser.parseJSON(new JSONString(capability));
        assert (cap.size() == 6);
        
        this.sharedSecret = null;
        this.fileId = (String) cap.get(0);
        this.accessMode = (String) cap.get(1);
        this.expires = (Long) cap.get(2);
        // ignore the client identity; it cannot be used because OSDs can act as
        // client proxies
        this.epochNo = (Long) cap.get(4);
        this.signature = (String) cap.get(5);
    }
    
    public String getFileId() {
        return fileId;
    }
    
    public String getAccessMode() {
        return accessMode;
    }
    
    public long getExpires() {
        return expires;
    }
    
    public String getClientIdentity() {
        return "*";
    }
    
    public long getEpochNo() {
        return epochNo;
    }
    
    public String getSignature() {
        return signature;
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
        return System.currentTimeMillis() / 1000 > expires;
    }
    
    /**
     * Checks whether the capability has a valid signature.
     * 
     * @return <code>true</code>, if the signature is valid, <code>false</code>,
     *         otherwise
     */
    public boolean hasValidSignature() {
        return signature.equals(calcSignature());
    }
    
    /**
     * Returns a string representation of the capability.
     * 
     * @return a JSON-formatted string representing the capability.
     */
    public String toString() {
        return "[\"" + fileId + "\",\"" + accessMode + "\"," + expires + ",\""
            + getClientIdentity() + "\"," + epochNo + ",\"" + signature + "\"]";
    }
    
    protected String calcSignature() {
        
        // right now, we use a shared secret between MRC and OSDs
        // as soon as we have a Public Key Infrastructure, signatures
        // will be generated and checked by means of asymmetric encryption
        // techniques
        
        String plainText = fileId + accessMode + expires + epochNo + sharedSecret;
        
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(plainText.getBytes());
            byte[] digest = md5.digest();
            
            return OutputUtils.byteArrayToHexString(digest);
        } catch (NoSuchAlgorithmException exc) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
            return null;
        }
    }
    
}
