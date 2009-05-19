/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.osdselection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.common.logging.Logging.Category;
import org.xtreemfs.common.util.OutputUtils;
import org.xtreemfs.dir.client.DIRClient;
import org.xtreemfs.foundation.LifeCycleThread;
import org.xtreemfs.foundation.oncrpc.client.RPCResponse;
import org.xtreemfs.interfaces.Service;
import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.ServiceType;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.PolicyContainer;
import org.xtreemfs.mrc.volumes.VolumeChangeListener;
import org.xtreemfs.mrc.volumes.metadata.VolumeInfo;

/**
 * Checks regularly for suitable OSDs for each volume.
 * 
 * @author bjko
 */
public class OSDStatusManager extends LifeCycleThread implements VolumeChangeListener {
    
    /**
     * Volume and policy record.
     */
    public static class VolumeOSDs {
        /**
         * volume ID
         */
        public String     volID;
        
        /**
         * policyID used by the volume.
         */
        public short      selectionPolicyID;
        
        /**
         * OSD policy arguments used by the volume
         */
        public String     selectionPolicyArgs;
        
        /**
         * Map of suitable OSDs for that volume. Can be empty.
         */
        public ServiceSet usableOSDs;
    }
    
    /**
     * Policies available in the system.
     */
    private static final Map<Short, OSDSelectionPolicy> policies            = new HashMap<Short, OSDSelectionPolicy>();
    
    /**
     * Interval in ms to wait between two checks.
     */
    private int                                         checkIntervalMillis = 1000 * 5;
    
    /**
     * A list of volumes registered with the thread.
     */
    private final Map<String, VolumeOSDs>               volumeMap;
    
    /**
     * The latest set of all known OSDs fetched from the Directory Service.
     */
    private ServiceSet                                  knownOSDs;
    
    /**
     * An client used to send requests to the Directory Service.
     */
    private final DIRClient                             client;
    
    /**
     * Thread shuts down if true.
     */
    private boolean                                     quit                = false;
    
    /**
     * Enables debugging output.
     */
    private boolean                                     debug               = false;
    
    /**
     * The configuration for the component.
     */
    private MRCConfig                                   config;
    
    private final PolicyContainer                       policyContainer;
    
    static {
        policies.put(RandomSelectionPolicy.POLICY_ID, new RandomSelectionPolicy());
        policies.put(ProximitySelectionPolicy.POLICY_ID, new ProximitySelectionPolicy());
    }
    
    /**
     * Creates a new instance of OSDStatusManager
     * 
     * @param client
     *            a DIRClient used for contacting the direcotory service
     */
    public OSDStatusManager(MRCConfig config, DIRClient client, PolicyContainer policyContainer)
        throws IOException {
        
        super("OSDStatusManager");
        
        this.policyContainer = policyContainer;
        this.config = config;
        
        volumeMap = new HashMap<String, VolumeOSDs>();
        knownOSDs = new ServiceSet();
        
        this.client = client;
        
        int interval = config.getOsdCheckInterval();
        checkIntervalMillis = 1000 * interval;
    }
    
    public void volumeChanged(int mod, VolumeInfo volume) {
        
        switch (mod) {
        
        case VolumeChangeListener.MOD_CHANGED:

            synchronized (this) {
                
                final String volId = volume.getId();
                VolumeOSDs vol = volumeMap.get(volId);
                
                if (vol == null) {
                    
                    vol = new VolumeOSDs();
                    vol.volID = volume.getId();
                    vol.selectionPolicyID = volume.getOsdPolicyId();
                    vol.selectionPolicyArgs = volume.getOsdPolicyArgs();
                    vol.usableOSDs = new ServiceSet();
                    
                    volumeMap.put(volId, vol);
                    
                } else {
                    vol.selectionPolicyID = volume.getOsdPolicyId();
                    vol.selectionPolicyArgs = volume.getOsdPolicyArgs();
                    vol.usableOSDs.clear();
                }
                
                this.notifyAll();
            }
            
            break;
        
        case VolumeChangeListener.MOD_DELETED:

            synchronized (this) {
                volumeMap.remove(volume.getId());
            }
            
            break;
        }
    }
    
    /**
     * Shuts down the thread.
     */
    public void shutdown() {
        synchronized (this) {
            quit = true;
            this.interrupt();
            this.notifyAll();
        }
    }
    
    /**
     * Main loop.
     */
    public void run() {
        
        // initially fetch the list of OSDs from the Directory Service
        RPCResponse<ServiceSet> r = null;
        try {
            r = client.xtreemfs_service_get_by_type(null, ServiceType.SERVICE_TYPE_OSD);
            knownOSDs = r.get();
        } catch (Exception exc) {
            this.notifyCrashed(exc);
        } finally {
            if (r != null)
                r.freeBuffers();
        }
        
        notifyStarted();
        if (Logging.isInfo())
            Logging.logMessage(Logging.LEVEL_INFO, Category.lifecycle, this,
                "OSD status manager operational, using DIR %s", config.getDirectoryService().toString());
        
        while (!quit) {
            
            synchronized (this) {
                try {
                    this.wait(checkIntervalMillis);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                "sending request for OSD list to DIR...");
            r = null;
            
            try {
                // request list of registered OSDs from Directory
                // Service
                r = client.xtreemfs_service_get_by_type(null, ServiceType.SERVICE_TYPE_OSD);
                knownOSDs = r.get();
                
                Logging
                        .logMessage(Logging.LEVEL_DEBUG, Category.misc, this,
                            "... received OSD list from DIR");
                
                evaluateResponse(knownOSDs);
                
            } catch (InterruptedException ex) {
                break;
            } catch (Exception exc) {
                Logging.logMessage(Logging.LEVEL_ERROR, Category.misc, this, OutputUtils
                        .stackTraceToString(exc));
            } finally {
                if (r != null)
                    r.freeBuffers();
            }
            
        }
        
        notifyStopped();
    }
    
    /**
     * Returns the list of usable OSDs for the given volume id.
     * 
     * @param volumeId
     *            the volume id
     * @return a list of feasible OSDs. Each list entry contains a mapping from
     *         keys to values which describes a certain OSD
     */
    public synchronized ServiceSet getUsableOSDs(String volumeId) {
        
        VolumeOSDs vol = volumeMap.get(volumeId);
        if (vol == null) {
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "no volume registered at OSDStatusManager with ID '%s'", volumeId);
            return null;
        }
        
        // if no OSDs are assigned to the current volume, re-calculate the set
        // of feasible OSDs from the last set of OSDs received from the
        // Directory Service
        if (vol.usableOSDs.size() == 0) {
            
            try {
                OSDSelectionPolicy policy = policyContainer.getOSDSelectionPolicy(vol.selectionPolicyID);
                
                if (knownOSDs != null)
                    vol.usableOSDs = policy.getUsableOSDs(knownOSDs, vol.selectionPolicyArgs);
                else
                    Logging
                            .logMessage(
                                Logging.LEVEL_WARN,
                                Category.misc,
                                this,
                                "could not determine set of feasible OSDs for volume '%s': haven't yet received an OSD list from Directory Service!",
                                vol.volID);
                
            } catch (Exception exc) {
                Logging
                        .logMessage(
                            Logging.LEVEL_WARN,
                            Category.misc,
                            this,
                            "could not determine set of feasible OSDs for volume '%s': no assignment policy available!",
                            vol.volID);
            }
        }
        
        return vol.usableOSDs;
        
    }
    
    public Map<String, Object> getCurrentStatus() {
        Map<String, Object> map = new HashMap();
        for (String volID : volumeMap.keySet()) {
            map.put(volID, volumeMap.get(volID).usableOSDs);
        }
        return map;
    }
    
    public synchronized void evaluateResponse(ServiceSet knownOSDs) {
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "response...");
        
        assert (knownOSDs != null);
        
        if (Logging.isDebug())
            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "registered OSDs");
        if (knownOSDs.size() == 0)
            Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                "there are currently no OSDs available");
        if (Logging.isDebug())
            for (Service osd : knownOSDs) {
                Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "%s", osd.getUuid());
            }
        
        for (VolumeOSDs vol : volumeMap.values()) {
            
            try {
                
                OSDSelectionPolicy policy = policyContainer.getOSDSelectionPolicy(vol.selectionPolicyID);
                vol.usableOSDs = policy.getUsableOSDs(knownOSDs, vol.selectionPolicyArgs);
                
                if (Logging.isDebug()) {
                    Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "OSDs for %s", vol.volID);
                    if (vol.usableOSDs != null)
                        for (Service osd : vol.usableOSDs) {
                            Logging.logMessage(Logging.LEVEL_DEBUG, Category.misc, this, "       %s", osd
                                    .getUuid());
                        }
                }
                
            } catch (Exception exc) {
                Logging.logMessage(Logging.LEVEL_WARN, Category.misc, this,
                    "policy ID %d selected for volume ID %s does not exist!", vol.selectionPolicyID,
                    vol.volID);
            }
        }
    }
    
    /**
     * Returns the approximate amount of free space in the given volume.
     * 
     * @param volumeId
     *            the ID of the volume
     * 
     * @return the approximate number of free bytes in the volume
     * 
     */
    public long getFreeSpace(String volumeId) {
        
        long free = 0;
        
        ServiceSet usableOSDs = getUsableOSDs(volumeId);
        if (usableOSDs == null)
            return 0;
        
        for (Service entry : usableOSDs) {
            String freeStr = entry.getData().get("free");
            if (freeStr != null)
                free += Long.valueOf(freeStr);
        }
        return free;
    }

    public OSDSelectionPolicy getOSDSelectionPolicy(short policyId) {

        OSDSelectionPolicy policy = policies.get(policyId);

        // if the policy is not built-in, try to load it from the plug-in
        // directory
        if (policy == null) {
            try {
                policy = policyContainer.getOSDSelectionPolicy(policyId);
                policies.put(policyId, policy);
            } catch (Exception exc) {
                Logging.logMessage(Logging.LEVEL_WARN, this,
                    "could not load OSDSelectionPolicy with ID " + policyId);
                Logging.logError(Logging.LEVEL_WARN, this, exc);
            }
        }

        return policy;
    }

}
