/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.mrc.replication;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.StringSet;

/**
 * 
 * @author bjko
 */
public class FQDNReplicaSelectionPolicy implements ReplicaSelectionPolicy {
    
    public static final short POLICY_ID = (short) 3;
    
    @Override
    public ReplicaSet getSortedReplicaList(ReplicaSet replicas, InetAddress clientAddr) {
        
        PriorityQueue<SortedReplica> list = new PriorityQueue<SortedReplica>();
        for (Replica r : replicas) {
            try {
                int match = getMatch(r.getOsd_uuids().get(0), clientAddr);
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
    
    @Override
    public StringSet getSortedOSDList(StringSet osdIDs, final InetAddress clientAddr) {
        
        Collections.sort(osdIDs, new Comparator<String>() {
            public int compare(String o1, String o2) {
                try {
                    return getMatch(o2, clientAddr) - getMatch(o1, clientAddr);
                } catch (UnknownUUIDException e) {
                    Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this, "cannot compare UUIDs");
                    Logging.logMessage(Logging.LEVEL_WARN, this, OutputUtils.stackTraceToString(e));
                    return 0;
                }
            }
        });
        
        return osdIDs;
    }
    
    private int getMatch(String osdUUID, InetAddress clientAddr) throws UnknownUUIDException {
        
        final String clientFQDN = clientAddr.getCanonicalHostName();
        
        ServiceUUID uuid = new ServiceUUID(osdUUID);
        final String osdHostName = uuid.getAddress().getAddress().getCanonicalHostName();
        final int minLen = (osdHostName.length() > clientFQDN.length()) ? clientFQDN.length() : osdHostName
                .length();
        int osdI = osdHostName.length() - 1;
        int clientI = clientFQDN.length() - 1;
        int match = 0;
        for (int i = minLen - 1; i > 0; i--) {
            if (osdHostName.charAt(osdI--) != clientFQDN.charAt(clientI--)) {
                break;
            }
            match++;
        }
        
        return match;
    }
    
    public static final class SortedReplica implements Comparable {
        public Replica replica;
        
        public int     match;
        
        public SortedReplica(Replica replica, int match) {
            this.match = match;
            this.replica = replica;
        }
        
        @Override
        public int compareTo(Object o) {
            SortedReplica other = (SortedReplica) o;
            return other.match - this.match;
        }
    }
    
}
