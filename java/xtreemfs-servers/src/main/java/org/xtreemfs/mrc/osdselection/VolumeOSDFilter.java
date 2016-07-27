/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.util.OutputUtils;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.DatabaseResultSet;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeInfo;
import org.xtreemfs.mrc.metadata.XAttr;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.utils.MRCHelper;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceDataMap;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replica;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.Replicas;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

/**
 * Volume and policy record.
 */
public class VolumeOSDFilter {

    private final MRCRequestDispatcher           master;

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
    private final Map<String, Service>           knownOSDMap;

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
                if (!policyMap.containsKey(pol)) {
                    policyMap.put(pol, master.getPolicyContainer().getOSDSelectionPolicy(pol));
                }
            } catch (Exception e) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, "could not instantiate OSDSelectionPolicy %d",
                        pol);
                Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, OutputUtils.stackTraceToString(e));
            }
        }

        for (short pol : replPolicy) {
            try {
                if (!policyMap.containsKey(pol)) {
                    policyMap.put(pol, master.getPolicyContainer().getOSDSelectionPolicy(pol));
                }
            } catch (Exception e) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, "could not instantiate OSDSelectionPolicy %d",
                        pol);
                Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, OutputUtils.stackTraceToString(e));
            }
        }

        // get all policy attributes

        try {
            DatabaseResultSet<XAttr> xattrs = master.getVolumeManager().getStorageManager(this.volId)
                    .getXAttrs(1, StorageManager.SYSTEM_UID);

            while (xattrs.hasNext()) {
                XAttr xattr = xattrs.next();
                if (xattr.getKey().startsWith(MRCHelper.XTREEMFS_POLICY_ATTR_PREFIX)) {
                    setAttribute(xattr.getKey(), new String(xattr.getValue()));
                }
            }

            xattrs.destroy();

        } catch (Exception exc) {
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, "could not set policy attributes");
            Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, OutputUtils.stackTraceToString(exc));
        }

    }

    public void setAttribute(String key, String value) {

        assert (key.startsWith(MRCHelper.XTREEMFS_POLICY_ATTR_PREFIX));
        key = key.substring(MRCHelper.XTREEMFS_POLICY_ATTR_PREFIX.length());

        int index = key.indexOf('.');

        // TODO refactored. is now moved to MRCHelper.setPolicyValue()!
        //		so: delete this?
        if (index == -1) {
            Logging.logMessage(
                    Logging.LEVEL_WARN,
                    Category.misc,
                    this,
                    "'%s=%s :' XtreemFS no longer supports global policy attributes. It is necessary to specify a policy e.g., '1000.%s=%s'",
                    key, value, key, value);
//            for (OSDSelectionPolicy pol : policyMap.values())
//                pol.setAttribute(key, value);
        } else {
            short policyId = Short.parseShort(key.substring(0, index));
            OSDSelectionPolicy pol = policyMap.get(policyId);
            if (pol != null) {
                pol.setAttribute(key.substring(index + 1), value);
            }
        }

    }

    public ServiceSet.Builder filterByOSDSelectionPolicy(ServiceSet.Builder knownOSDs, InetAddress clientIP,
            VivaldiCoordinates clientCoords, XLocList currentXLoc, int numOSDs) {

        ServiceSet.Builder result = ServiceSet.newBuilder().addAllServices(knownOSDs.getServicesList());
        for (short id : osdPolicy) {
            OSDSelectionPolicy policy = policyMap.get(id);
            if (policy == null) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.proc, this,
                        "could not find OSD selection policy with ID=%d, will be ignored", id);
                continue;
            }

            result = policy.getOSDs(result, clientIP, clientCoords, currentXLoc, numOSDs);
        }

        return result;
    }

    public ServiceSet.Builder filterByOSDSelectionPolicy(ServiceSet.Builder knownOSDs) {

        ServiceSet.Builder result = ServiceSet.newBuilder().addAllServices(knownOSDs.getServicesList());
        for (short id : osdPolicy) {

            OSDSelectionPolicy policy = policyMap.get(id);

            if (policy == null) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                        "could not find OSD selection policy with ID %d, will be ignored", id);
                continue;
            }

            result = policy.getOSDs(result);
        }

        return result;
    }

    public Replicas sortByReplicaSelectionPolicy(InetAddress clientIP, VivaldiCoordinates clientCoords,
            List<Replica> unsortedRepls, XLocList xLocList) {

        // head OSD -> replica
        Map<String, Replica> replMap = new HashMap<String, Replica>();

        // get a list of all head OSDs in the XLoc
        ServiceSet.Builder headOSDServiceSetBuilder = ServiceSet.newBuilder();
        for (int i = 0; i < unsortedRepls.size(); i++) {

            Replica repl = unsortedRepls.get(i);
            assert (repl.getOsdUuidsCount() > 0);

            String headOSD = repl.getOsdUuids(0);

            // store the mapping in a temporary replica map
            replMap.put(headOSD, repl);

            // retrieve the service name from the 'known OSDs' map; if no such
            // service has been registered, create a dummy service object from
            // the OSD UUID
            Service s = knownOSDMap.get(headOSD);
            if (s == null) {
                s = Service.newBuilder().setData(ServiceDataMap.newBuilder()).setLastUpdatedS(0)
                        .setName("OSD @ " + headOSD).setType(ServiceType.SERVICE_TYPE_OSD).setUuid(headOSD)
                        .setVersion(0).build();
            }

            headOSDServiceSetBuilder.addServices(s);
        }

        // sort the list of head OSDs according to the policy
        for (short id : replPolicy) {
            OSDSelectionPolicy policy = policyMap.get(id);

            if (policy == null) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                        "could not find Replica selection policy with ID %d, will be ignored", id);
                continue;
            }

            headOSDServiceSetBuilder = policy.getOSDs(headOSDServiceSetBuilder, clientIP, clientCoords,
                    xLocList, headOSDServiceSetBuilder.getServicesCount());
        }

        // arrange the resulting list of replicas in the same order as the list
        // of sorted head OSDs
        Replicas.Builder sortedReplsBuilder = Replicas.newBuilder();
        for (Service headOSD : headOSDServiceSetBuilder.getServicesList()) {

            Replica r = replMap.get(headOSD.getUuid());
            assert (r != null);
            assert (r.getOsdUuidsCount() > 0);

            Replica.Builder replBuilder = Replica.newBuilder().setReplicationFlags(r.getReplicationFlags())
                    .setStripingPolicy(r.getStripingPolicy());

            for (int j = 0; j < r.getOsdUuidsCount(); j++) {
                replBuilder.addOsdUuids(r.getOsdUuids(j));
            }

            sortedReplsBuilder.addReplicas(replBuilder);
        }

        return sortedReplsBuilder.build();
    }
}