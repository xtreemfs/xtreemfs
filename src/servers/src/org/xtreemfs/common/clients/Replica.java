/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.common.clients;

import java.io.IOError;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.interfaces.Constants;
import org.xtreemfs.interfaces.StripingPolicyType;

/**
 *
 * @author bjko
 */
public class Replica {

    private final File parentFile;

    private final String[]            osdAddresses;

    private final String[]            osdUUIDs;

    private final StripingPolicyType  stripingPolicy;

    private final int                 stripeSize;

    private final int                 stripingWidth;

    private final int                 replicationFlags;


    Replica(File parentFile, Map<String,Object> json) throws JSONException {
        this.parentFile = parentFile;

        try {
            Map<String,Object> sp = (Map<String, Object>) json.get("striping-policy");
            String spName = (String) sp.get("pattern");
            if (spName.equals("STRIPING_POLICY_RAID0")) {
                stripingPolicy = StripingPolicyType.STRIPING_POLICY_RAID0;
            } else {
                throw new JSONException("Unknown striping policy type: "+spName);
            }
            Long tmp = (Long)sp.get("size");
            stripeSize = tmp.intValue();

            tmp = (Long) sp.get("width");
            stripingWidth = tmp.intValue();

            tmp = (Long) json.get("replication-flags");
            replicationFlags = tmp.intValue();

            List<Map<String,Object>> osds = (List<Map<String, Object>>) json.get("osds");
            osdAddresses = new String[stripingWidth];
            osdUUIDs = new String[stripingWidth];


            if (osds.size() != stripingWidth) {
                throw new JSONException("replica information incorrect, OSD count < stripingWidth: "+stripingWidth);
            }

            for (int i = 0; i < stripingWidth; i++) {
                Map<String,Object> osd = osds.get(i);
                osdAddresses[i] = (String) osd.get("address");
                osdUUIDs[i] = (String) osd.get("uuid");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new JSONException("malformed JSON replica representation: "+ex);
        }
    }

    /**
     * Get the location (inet address) of the replica's head OSD
     * @return
     */
    public InetSocketAddress getLocation() {
        return getOSDAddress(0);
    }

    public InetSocketAddress getOSDAddress(int osdNum) {
        int colon = osdAddresses[osdNum].indexOf(":");
        String hostname = osdAddresses[osdNum].substring(0,colon);
        String portStr = osdAddresses[osdNum].substring(colon);
        int port = Integer.valueOf(portStr);

        return new InetSocketAddress(hostname,port);
    }

    public String getOSDUuid(int osdNum) {
        return osdUUIDs[osdNum];
    }

    public int getStripeWidth() {
        return stripingWidth;
    }

    public int getStripeSize() {
        return stripeSize;
    }

    public StripingPolicyType getStripingPolicy() {
        return stripingPolicy;
    }

    public boolean isFullReplica() {
        return (replicationFlags & Constants.REPL_FLAG_FULL_REPLICA) != 0;
    }

    /**
     * checks if the replica is complete (holds all objects of a file).
     *
     * @return true, if the replica is marked as complete or if the
     */
    public boolean isCompleteReplica() throws IOException {
        if ((replicationFlags & Constants.REPL_FLAG_IS_COMPLETE) != 0) {
            return true;
        } else {
            RandomAccessFile raf = parentFile.open("r", 0);
            int myNum = raf.getReplicaNumber(osdUUIDs[0]);
            boolean isComplete = raf.isCompleteReplica(myNum);
            raf.close();
            return isComplete;
        }
    }

    public void removeReplica(boolean checkForCompleteReplica) throws IOException {
        final Replica[] replicas = parentFile.getReplicas();
        if (replicas.length == 1)
            throw new IOException("cannot remove last replica (delete file instead!)");

        if (checkForCompleteReplica) {
            //FIXME: make sure at least one complete replica still exists
            boolean completeReplica = false;
            for (Replica r : replicas) {
                if (r.getOSDUuid(0).equals(this.osdUUIDs[0])) {
                    //ignore myself
                    continue;
                }
                if (r.isCompleteReplica()) {
                    completeReplica = true;
                    break;
                }
            }
            if (!completeReplica) {
                throw new IOException("not complete replicas left to remove this replica!");
            }
        }

        parentFile.removeReplica(osdUUIDs[0]);
    }

}
