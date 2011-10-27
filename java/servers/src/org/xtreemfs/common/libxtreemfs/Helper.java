/*
 * Copyright (c) 2008-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.net.InetSocketAddress;
import java.util.Random;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;

/**
 * 
 * <br>
 * Sep 2, 2011
 */
public class Helper {

    /**
     * Generates a pseudorandom UUID with 32 chars length.
     * 
     * @return String The pseudorandom UUID of length 32.
     * 
     */
    // TODO: Ask michael why his uuid has length 36. oO
    public static String GenerateVersion4UUID() {

        // Base62 characters for UUID generation.
        char set[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        String uuid = new String();

        int block_length[] = { 8, 4, 4, 4, 12 };

        Random generator = new Random();

        for (int i = 0; i < block_length.length; i++) {
            for (int j = 0; j < block_length[i]; j++) {
                // choose a pseudorandom, uniformly distributed char from the array set and append it
                // to the uuid.
                uuid = uuid + set[generator.nextInt(62)];
            }
        }

        assert (uuid.length() == 32);

        // TODO: Avoid this stupid new Helper()
        if (Logging.isDebug()) {
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, new Helper(), "Generated client UUID: %s",
                    uuid);
        }

        return uuid;
    }

    /**
     * Converts a address in format "hostname:port" to a InetSocketAddress
     * 
     * @param address
     *            String which represents the address in format "hostname:port".
     * @param defaultPort
     *            Port that is used if address String is just a hostname.
     */
    static public InetSocketAddress stringToInetSocketAddress(String address, int defaultPort) {

        InetSocketAddress isa;
        int pos = 0;

        if ((pos = address.indexOf(':')) == -1) {
            isa = new InetSocketAddress(address, defaultPort);
        } else {
            isa = new InetSocketAddress(address.substring(0, pos), Integer.parseInt(address
                    .substring(pos + 1)));
        }

        return isa;
    }

    static public String getOSDUUIDFromXlocSet(XLocSet xlocs, int replicaIndex, int stripeIndex) {

        if (xlocs.getReplicasCount() == 0) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, xlocs,
                    "getOSDUUIDFromXlocSet: Empty replicas list in XlocSet: %s", xlocs.toString());
            return "";
        }

        Replica replica = xlocs.getReplicas(replicaIndex);
        if (replica.getOsdUuidsCount() == 0) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, xlocs,
                    "GetOSDUUIDFromXlocSet: No head OSD available in XlocSet: %s", xlocs.toString());
            return "";
        }

        return replica.getOsdUuids(stripeIndex);
    }

    static public String getOSDUUIDFromXlocSet(XLocSet xlocs) {
        // Get the UUID for the first replica (r=0) and the head OSD (i.e. the first
        // stripe, s=0).
        return getOSDUUIDFromXlocSet(xlocs, 0, 0);
    }

    static public long extractFileIdFromXcap(XCap xcap) {

        String fileId = xcap.getFileId();

        int start = fileId.indexOf(':');

        return Long.parseLong(fileId.substring(start + 1, fileId.length()));
    }

    static public String resolveParentDirectory(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (path.equals("/")) {
            return path;
        } else {
            // We don't allow "path" to have a trailing "/"/
            assert (lastSlash != path.length() - 1);

            return path.substring(0, lastSlash);
        }
    }

    static public String getBasename(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (path.equals("/")) {
            return path;
        } else {
            // We don't allow path to have a trailing "/".
            assert (lastSlash != path.length() - 1);

            return path.substring(lastSlash + 1);
        }
    }

    static public String concatenatePath(String directory, String file) {

        // handle ".." and "."
        if (file.equals(".")) {
            return directory;
        } else {
            if (file.equals("..")) {
                if (directory.equals("/")) {
                    return directory;
                }
                return directory.substring(0, directory.lastIndexOf('/'));
            }
        }

        if (directory.endsWith("/")) {
            return directory + file;
        } else {
            return directory + "/" + file;
        }
    }

    /**
     * Compares newResponse with currentResponse. Returns 0 iff newResponse == currentResponse; 1 iff
     * newResponse < currentResponse; -1 iff newResponse > currentResponse.
     * 
     * @param newResponse
     * 
     * @param currentResponse
     * 
     * @return {1, 0, -1}
     */
    static protected int compareOSDWriteResponses(OSDWriteResponse newResponse,
            OSDWriteResponse currentResponse) {
        if (newResponse == null && currentResponse == null) {
            return 0;
        }

        // newResponse > currentResponse
        if (newResponse != null && currentResponse == null) {
            return 1;
        }

        // newResponse < currentResponse
        if (newResponse == null && currentResponse != null) {
            return -1;
        }

        // newResponse > currentResponse
        if (newResponse.getTruncateEpoch() > currentResponse.getTruncateEpoch()
                || (newResponse.getTruncateEpoch() == currentResponse.getTruncateEpoch() && newResponse
                        .getSizeInBytes() > currentResponse.getSizeInBytes())) {
            return 1;
        }

        // newResponse < currentResponse
        if (newResponse.getTruncateEpoch() < currentResponse.getTruncateEpoch()
                || (newResponse.getTruncateEpoch() == currentResponse.getTruncateEpoch() && newResponse
                        .getSizeInBytes() < currentResponse.getSizeInBytes())) {
            return -1;
        }

        // newResponse == currentResponse
        return 0;

    }

    /**
     * Compares "lock1 "with "lock2". Returns true if lock1 equals lock2. false otherwise. They are
     * equal iff clientUuid, clientPid, offset and length are equal.
     * 
     * @param lock1
     * @param lock2
     * @return true iff lock1 equals lock2
     */
    protected static boolean checkIfLocksAreEqual(Lock lock1, Lock lock2) {

        return lock1.getClientUuid().equals(lock2.getClientUuid())
                && lock1.getClientPid() == lock2.getClientPid() && lock1.getOffset() == lock2.getOffset()
                && lock1.getLength() == lock2.getLength();
    }
    
    protected static boolean checkIfLocksDoConflict(Lock lock1, Lock lock2) { 
        
        // 0 means to lock till the end of the file.
        long lock1End = lock1.getLength() == 0 ? 0 : lock1.getLength() + lock1.getOffset();
        long lock2End = lock2.getLength() == 0 ? 0 : lock2.getLength() + lock2.getOffset();
        
        // Check for overlaps
        if (lock1End == 0) {
            if (lock2End >= lock1.getOffset() || lock2End == 0) {
                return true;
            }
        }
        
        if (lock2End == 0) {
            if (lock1End >= lock2.getOffset() || lock1End == 0) {
                return true;
            }
        }
        
        // Overlapping?
        if (!(lock1End < lock2.getOffset() || lock2End < lock1.getOffset())) {
            // Does overlap! Check for conflicting modes.
            return (lock1.getExclusive() || lock2.getExclusive());
        }
        
        return false;
    }
}
