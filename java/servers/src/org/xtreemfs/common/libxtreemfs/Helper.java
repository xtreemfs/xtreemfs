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
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;

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
        
        return Long.parseLong(fileId.substring(start+1, fileId.length()));
    }
}
