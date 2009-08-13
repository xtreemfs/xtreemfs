/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;
import java.util.PriorityQueue;
import org.xtreemfs.common.uuids.ServiceUUID;
import org.xtreemfs.common.uuids.UnknownUUIDException;
import org.xtreemfs.interfaces.OSDSelectionPolicyType;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.StringSet;

/**
 * Selects OSDs according to their DNS names matching the client's name. The
 * OSDs are sorting according to the number of characters that match in both
 * names (starting from the last character).
 * 
 * @author bjko
 */
public class DNSSelectionPolicy extends AbstractSelectionPolicy {
    
    public static final short POLICY_ID = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_DNS.intValue();
    
    @Override
    public String[] getOSDsForNewFile(ServiceSet osdMap, InetAddress clientAddr, int amount, String args) {
        final String clientFQDN = clientAddr.getCanonicalHostName();
        
        int minPrefix = 0;
        if (args != null) {
            try {
                minPrefix = Integer.valueOf(args);
            } catch (NumberFormatException ex) {
            }
        }
        
        // first, sort out all OSDs with insufficient free capacity
        PriorityQueue<UsableOSD> list = new PriorityQueue<UsableOSD>();
        for (Service osd : osdMap) {
            if (!hasFreeCapacity(osd)) {
                continue;
            }
            try {
                ServiceUUID uuid = new ServiceUUID(osd.getUuid());
                final String osdHostName = uuid.getAddress().getAddress().getCanonicalHostName();
                final int minLen = (osdHostName.length() > clientFQDN.length()) ? clientFQDN.length()
                    : osdHostName.length();
                int osdI = osdHostName.length() - 1;
                int clientI = clientFQDN.length() - 1;
                int match = 0;
                for (int i = minLen - 1; i > 0; i--) {
                    if (osdHostName.charAt(osdI--) != clientFQDN.charAt(clientI--)) {
                        break;
                    }
                    match++;
                }
                if (match < minPrefix)
                    continue;
                
                list.add(new UsableOSD(osd.getUuid(), match));
            } catch (UnknownUUIDException ex) {
            }
        }
        
        int availOSDs = (amount > list.size()) ? list.size() : amount;
        String[] osds = new String[availOSDs];
        // from the remaining set, take a random subset of OSDs
        for (int i = 0; i < availOSDs; i++) {
            osds[i] = list.poll().osdUuid;
        }
        
        return osds;
        
    }
    
    public StringSet getSortedOSDList(StringSet osdIDs, InetAddress clientAddr) {
        return osdIDs;
    }
    
    private static final class UsableOSD implements Comparable {
        public String osdUuid;
        
        public int    match;
        
        public UsableOSD(String uuid, int match) {
            this.match = match;
            this.osdUuid = uuid;
        }
        
        @Override
        public int compareTo(Object o) {
            UsableOSD other = (UsableOSD) o;
            return other.match - this.match;
        }
        
        public String toString() {
            return osdUuid + "(" + match + ")";
        }
    }
    
}
