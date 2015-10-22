/*
 * Copyright (c) 2008-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.xtreemfs.common.libxtreemfs.Volume.StripeLocation;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDWriteResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replicas;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.SYSTEM_V_FCNTL;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.StripingPolicy;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XCap;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.Stat;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.Lock;

/**
 * Some static Helper functions which are used internally by different classes.
 */
public class Helper {

    /**
     * Generates a pseudorandom UUID with 32 chars length.
     * 
     * @return String The pseudorandom UUID of length 32.
     * 
     */
    // TODO: Ask michael why his uuid has length 36. oO
    public static String generateVersion4UUID() {
        // Base62 characters for UUID generation.
        char set[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
        String uuid = "";

        int block_length[] = { 8, 4, 4, 4, 12 };

        Random generator = new Random();

        for (int i = 0; i < block_length.length; i++) {
            for (int j = 0; j < block_length[i]; j++) {
                // choose a pseudorandom, uniformly distributed char from the
                // array set and append it
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
    static protected InetSocketAddress stringToInetSocketAddress(String address, int defaultPort) {
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

    /**
     * Returns the UUID of the OSD which stores the object with number "objectNo" of the replica "replica"
     * 
     * @param replica
     * @param objectNo
     * @return
     */
    static public String getOSDUUIDFromObjectNo(Replica replica, long objectNo) {
    	return replica.getOsdUuids((int) objectNo % replica.getStripingPolicy().getWidth());
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
        // Get the UUID for the first replica (r=0) and the head OSD (i.e. the
        // first stripe, s=0).
        return getOSDUUIDFromXlocSet(xlocs, 0, 0);
    }

    /**
     * Creates a list containing the UUIDs for the head OSD of every replica in the XLocSet.
     */
    public static List<String> getOSDUUIDsFromXlocSet(XLocSet xlocs) {
        List<String> uuids = new ArrayList<String>(xlocs.getReplicasCount());

        for (int i = 0; i < xlocs.getReplicasCount(); i++) {
            uuids.add(xlocs.getReplicas(i).getOsdUuids(0));
        }

        return uuids;
    }

    /**
     * Returns a list containing the UUIDs for the OSDs responsible for the stripe
     * 
     * @param replicas
     * @param stripeIndex
     * @return list containing the OSD UUIDs
     */
    public static List<String> getOSDUUIDsFromReplicas(Replicas replicas, long stripeIndex) {
        ArrayList<String> uuids = new ArrayList<String>(replicas.getReplicasCount());
        for (Replica replica : replicas.getReplicasList()) {
            int osdIndex = (int) stripeIndex % replica.getStripingPolicy().getWidth();
            uuids.add(replica.getOsdUuids(osdIndex));
        }
        return uuids;
    }


    /**
     * Get the StripeLocations required for the Hadoop driver.
     * 
     * @param replicas
     * @param startSize
     * @param length
     * @param uuidResolver
     * @return list of StripeLocations
     */
    public static List<StripeLocation> getStripeLocationsFromReplicas(Replicas replicas, long startSize, long length,
            UUIDResolver uuidResolver) {
        long stripeSize = replicas.getReplicas(0).getStripingPolicy().getStripeSize() * 1024L;
        long indexOfFirstStripeToConsider = (startSize / stripeSize);
        long remainingLengthOfFirstStripe = Math.min(length, stripeSize - (startSize % stripeSize));
        int numberOfStrips = (int) (length / stripeSize + 1);

        List<StripeLocation> stripeLocations = new ArrayList<StripeLocation>(numberOfStrips);
        // add first Stripe
        List<String> uuids = Helper.getOSDUUIDsFromReplicas(replicas, indexOfFirstStripeToConsider);
        List<String> hostnames = Helper.getOSDHostnamesFromUUIDs(uuids, uuidResolver);
        stripeLocations.add(new StripeLocation(startSize, remainingLengthOfFirstStripe, uuids.toArray(new String[uuids
                .size()]), hostnames.toArray(new String[hostnames.size()])));

        for (long index = indexOfFirstStripeToConsider + 1; index * stripeSize < startSize + length; index++) {
            uuids = Helper.getOSDUUIDsFromReplicas(replicas, index);
            hostnames = Helper.getOSDHostnamesFromUUIDs(uuids, uuidResolver);
            stripeLocations.add(new StripeLocation(index * stripeSize, Math.min(stripeSize, startSize + length - index
                    * stripeSize), uuids.toArray(new String[uuids.size()]), hostnames.toArray(new String[hostnames
                    .size()])));
        }
        return stripeLocations;
    }

    /**
     * Returns a list containing the hostnames for the OSDs responsible for the given UUIDs.
     * 
     * @param uuids
     *            List of UUIDs as strings
     * @param uuidResolver
     * @return list containing the OSD hostnames as strings
     */
    public static List<String> getOSDHostnamesFromUUIDs(List<String> uuids, UUIDResolver uuidResolver) {
        ArrayList<String> names = new ArrayList<String>(uuids.size());
        
        for (String uuidString : uuids) {
            try {
                String addressString = uuidResolver.uuidToAddress(uuidString);
                addressString = addressString.substring(0, addressString.lastIndexOf(':'));

                InetAddress address = InetAddress.getByName(addressString);
                String hostname = address.getHostName();

                // Ignore entries, that can not be resolved to a hostname.
                if (!hostname.equals(address.getHostAddress())) {
                    names.add(hostname);
                }
                
            } catch (AddressToUUIDNotFoundException e) {
                Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.net, (Object) null,
                        "Could not find host for UUID '%s'", uuidString);
            } catch (UnknownHostException e) {
                Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.net, (Object) null,
                        "Could not find host for UUID '%s'", uuidString);
            }
        }
        return names;
    }

    static public long extractFileIdFromXcap(XCap xcap) {
        String fileId = xcap.getFileId();
        int start = fileId.indexOf(':');
        return Long.parseLong(fileId.substring(start + 1, fileId.length()));
    }

    static public String extractGlobalFileIdFromXcap(XCap xcap) {
        return xcap.getFileId();
    }

    static public String resolveParentDirectory(String path) {
        path = setLeadingSlashIfMissing(path);
        int lastSlash = path.lastIndexOf('/');
        if (path.equals("/") || lastSlash == 0) {
            return "/";
        } else {
            // We don't allow "path" to have a trailing "/"
            assert (lastSlash != path.length() - 1);
            return path.substring(0, lastSlash);
        }
    }

    private static String setLeadingSlashIfMissing(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    static protected long getNumObjects(UserCredentials userCredentials, Stat fileAttr,
            StripingPolicy stripingPolicy) throws IOException, AddressToUUIDNotFoundException,
            PosixErrorException {
        long fileSize = fileAttr.getSize();

        if (fileSize > 0) {
            int stripeSize = stripingPolicy.getStripeSize() * 1024;
            return ((fileSize - 1) / stripeSize) + 1;
        } else
            return 0;
    }

    static public String getBasename(String path) {
        path = setLeadingSlashIfMissing(path);
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
        directory = setLeadingSlashIfMissing(directory);
        // handle ".." and "."
        if (file.equals(".")) {
            return directory;
        } else {
            if (file.equals("..")) {
                ;
                if (directory.equals("/")) {
                    return "/";
                } else {
                    return directory.substring(0, directory.lastIndexOf('/'));
                }
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
     * Compares "lock1 "with "lock2". Returns true if lock1 equals lock2. false otherwise. They are equal iff
     * clientUuid, clientPid, offset and length are equal.
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

    public static String policiesToString(OSDSelectionPolicyType[] policies) {
        StringBuffer policiesSB = new StringBuffer();
        boolean firstEntry = true;
        for (OSDSelectionPolicyType policy : policies) {
            if (firstEntry) {
                firstEntry = false;
            } else {
                policiesSB.append(",");
            }

            policiesSB.append(String.valueOf(policy.getNumber()));
        }

        return policiesSB.toString();
    }

    /**
     * Convert the given flags to their corresponding bit patterns and combine them by an or.
     * 
     * @param flags
     *            Variable number of SYSTEM_V_FCNTL flags
     * @return bit pattern as an integer of the or'ed flags
     */
    public static int flagsToInt(SYSTEM_V_FCNTL... flags) {
        int flagsInt = 0;
        
        for (SYSTEM_V_FCNTL flag: flags) {
            flagsInt = flagsInt | flag.getNumber();
        }

        return flagsInt;
    }
}
