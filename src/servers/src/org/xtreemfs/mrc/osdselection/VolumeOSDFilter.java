package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.interfaces.Replica;
import org.xtreemfs.interfaces.ReplicaSet;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.StringSet;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.metadata.XAttr;
import org.xtreemfs.mrc.metadata.XLoc;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.Converter;
import org.xtreemfs.mrc.utils.MRCHelper;

/**
 * Volume and policy record.
 */
public class VolumeOSDFilter {
    
    private MRCRequestDispatcher           master;
    
    /**
     * volume ID
     */
    private String                         volId;
    
    /**
     * OSD selection policy
     */
    private short[]                        osdPolicy;
    
    /**
     * replica selection policy
     */
    private short[]                        replPolicy;
    
    /**
     * map containing instances of all OSD policies
     */
    private Map<Short, OSDSelectionPolicy> policyMap;
    
    /**
     * map containing all known OSDs
     */
    private Map<String, Service>           knownOSDMap;
    
    public VolumeOSDFilter(MRCRequestDispatcher master, Map<String, Service> knownOSDMap) {
        this.master = master;
        this.knownOSDMap = knownOSDMap;
    }
    
    public void init(VolumeInfo volume) throws DatabaseException {
        
        this.volId = volume.getId();
        this.osdPolicy = volume.getOsdPolicy();
        this.replPolicy = volume.getReplicaPolicy();
        
        // initialize the policy map
        policyMap = new HashMap<Short, OSDSelectionPolicy>();
        for (short pol : osdPolicy) {
            try {
                if (!policyMap.containsKey(pol))
                    policyMap.put(pol, master.getPolicyContainer().getOSDSelectionPolicy(pol));
            } catch (Exception e) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.misc,
                    "could not instantiate OSDSelectionPolicy %d", pol);
                Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, OutputUtils.stackTraceToString(e));
            }
        }
        
        for (short pol : replPolicy) {
            try {
                if (!policyMap.containsKey(pol))
                    policyMap.put(pol, master.getPolicyContainer().getOSDSelectionPolicy(pol));
            } catch (Exception e) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.misc,
                    "could not instantiate OSDSelectionPolicy %d", pol);
                Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, OutputUtils.stackTraceToString(e));
            }
        }
        
        // get all policy attributes
        final String prefix = "xtreemfs." + MRCHelper.POLICY_ATTR_PREFIX + ".";
        
        try {
            Iterator<XAttr> xattrs = master.getVolumeManager().getStorageManager(volId).getXAttrs(1,
                StorageManager.SYSTEM_UID);
            
            // set the
            while (xattrs.hasNext()) {
                XAttr xattr = xattrs.next();
                if (xattr.getKey().startsWith(prefix))
                    for (OSDSelectionPolicy pol : policyMap.values())
                        pol.setAttribute(xattr.getKey().substring(prefix.length()), xattr.getValue());
            }
            
        } catch (Exception exc) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, "could not set policy attributes");
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, OutputUtils.stackTraceToString(exc));
        }
        
    }
    
    public ServiceSet filterByOSDSelectionPolicy(ServiceSet knownOSDs, InetAddress clientIP,
        XLocList currentXLoc, int numOSDs) {
        
        ServiceSet result = (ServiceSet) knownOSDs.clone();
        for (short id : osdPolicy)
            result = policyMap.get(id).getOSDs(result, clientIP, currentXLoc, numOSDs);
        
        return result;
    }
    
    public ServiceSet filterByOSDSelectionPolicy(ServiceSet knownOSDs) {
        
        ServiceSet result = (ServiceSet) knownOSDs.clone();
        for (short id : osdPolicy)
            result = policyMap.get(id).getOSDs(result);
        
        return result;
    }
    
    public ReplicaSet sortByReplicaSelectionPolicy(InetAddress clientIP, XLocList xLocList) {
        
        ReplicaSet repls = new ReplicaSet();
        
        // get a list of all head OSDs in the XLoc
        ServiceSet headOSDs = new ServiceSet();
        for (int i = 0; i < xLocList.getReplicaCount(); i++) {
            
            XLoc repl = xLocList.getReplica(i);
            assert (repl.getOSDCount() > 0);
            
            String headOSD = repl.getOSD(0);
            Service s = knownOSDMap.get(headOSD);
            if (s == null) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, "unknown OSD: %s", headOSD);
                Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, "cannot sort replica list");
                return repls;
            }
            
            headOSDs.add(s);
        }
        
        // sort the list of head OSDs according to the policy
        for (short id : replPolicy)
            headOSDs = policyMap.get(id).getOSDs(headOSDs, clientIP, xLocList, headOSDs.size());
        
        // sort the list of replicas in the same order as the head OSDs
        ReplicaSet newRepls = new ReplicaSet();
        for (Service headOSD : headOSDs)
            for (int i = 0; i < xLocList.getReplicaCount(); i++) {
                
                XLoc r = xLocList.getReplica(i);
                
                StringSet osds = new StringSet();
                for (int j = 0; j < r.getOSDCount(); j++)
                    osds.add(r.getOSD(j));
                
                if (r.getOSD(0).equals(headOSD.getUuid()))
                    newRepls.add(new Replica(Converter.stripingPolicyToStripingPolicy(r.getStripingPolicy()),
                        r.getReplicationFlags(), osds));
            }
        
        return newRepls;
    }
}