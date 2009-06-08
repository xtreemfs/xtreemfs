/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.mrc.replication;

import java.net.InetAddress;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;

/**
 *
 * @author bjko
 */
public class FQDNReplicaSelectionPolicy implements ReplicaSelectionPolicy {

    public static final short POLICY_ID = (short) 2;

    @Override
    public ReplicaSet getSortedReplicaList(ReplicaSet replicas, InetAddress clientAddr) {
        final String clientFQDN = clientAddr.getCanonicalHostName();

        PriorityQueue<SortedReplica> list = new PriorityQueue<SortedReplica>();
        for (Replica r : replicas) {
            try {
                ServiceUUID uuid = new ServiceUUID(r.getOsd_uuids().get(0));
                final String osdHostName = uuid.getAddress().getAddress().getCanonicalHostName();
                final int minLen = (osdHostName.length() > clientFQDN.length()) ? clientFQDN.length()  : osdHostName.length();
                int osdI = osdHostName.length()-1;
                int clientI = clientFQDN.length()-1;
                int match = 0;
                for (int i = minLen-1; i > 0; i--) {
                    if (osdHostName.charAt(osdI--) != clientFQDN.charAt(clientI--)) {
                        break;
                    }
                    match++;
                }

                list.add(new SortedReplica(r, match));
            } catch (UnknownUUIDException ex) {
                Logger.getLogger(FQDNReplicaSelectionPolicy.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        ReplicaSet sortedSet = new ReplicaSet();

        for (int i = 0; i < replicas.size(); i++) {
            sortedSet.add(list.poll().replica);
        }

        return sortedSet;
    }

    public static final class SortedReplica implements Comparable {
        public Replica replica;
        public int    match;

        public SortedReplica(Replica replica, int match) {
            this.match = match;
            this.replica = replica;
        }

        @Override
        public int compareTo(Object o) {
            SortedReplica other = (SortedReplica)o;
            return other.match-this.match;
        }
    }



}
