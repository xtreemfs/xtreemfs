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
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.KeyValuePairs;
import org.xtreemfs.common.config.ServiceConfig;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceStatus;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.OSDSelectionPolicyType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;
import org.xtreemfs.pbrpc.generatedinterfaces.OSD.OSDHealthResult;

/**
 * Filters all those OSDs that haven't been assigned to the current XLoc list
 * yet, have recently sent a heartbeat signal and have sufficient space.
 * 
 * @author stender
 */
public class FilterDefaultPolicy implements OSDSelectionPolicy {
    
    public static final short       POLICY_ID           = (short) OSDSelectionPolicyType.OSD_SELECTION_POLICY_FILTER_DEFAULT
                                                                .getNumber();
    
    private static final String     OFFLINE_TIME_SECS   = "offline_time_secs";
    
    private static final String     FREE_CAPACITY_BYTES = "free_capacity_bytes";
    
    private static final String     OSD_HEALTH_CHECK   = "osd_health_check";

    private static final String     NOT_IN              = "not.";
    // default: 2GB
    private long                    minFreeCapacity     = 2 * 1024 * 1024 * 1024;
    
    // default: 5 min
    private long                    maxOfflineTime      = 300;
    
    // default: WARNING
    private OSDHealthResult         osdHealthCheck     = OSDHealthResult.OSD_HEALTH_RESULT_WARNING;

    private HashMap<String, String> customFilter        = new HashMap<String, String>();
    private HashMap<String, String> customNotFilter     = new HashMap<String, String>();
    
    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs,
                                      InetAddress clientIP,
                                      VivaldiCoordinates clientCoords,
                                      XLocList currentXLoc,
                                      int numOSDs,
                                      String path) {
        
        if (allOSDs == null)
            return null;
        
        // first, remove all OSDs from the set that have already been assigned
        // to the current XLoc list
        ServiceSet.Builder osds = PolicyHelper.removeUsedOSDs(allOSDs, currentXLoc);
        
        return getOSDs(osds);
    }
    
    @Override
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs) {
        
        if (allOSDs == null)
            return null;
        
        ServiceSet.Builder filteredOSDs = ServiceSet.newBuilder();
        for (Service osd : allOSDs.getServicesList()) {
            
            if (!hasTimedOut(osd) && hasFreeCapacity(osd) && isAvailable(osd) && isHealthy(osd)) {
                
                // if no custom filters have been assigned, add the OSD to the
                // list
                if (customFilter.isEmpty()) {
                    if (customNotFilter.isEmpty() ||
                        !checkMatch(customNotFilter, filteredOSDs, osd)) {
                        filteredOSDs.addServices(osd);
                    }
                }
                
                // otherwise, check if the filters allow the OSD to be added to
                // the list
                else {                    
                    // ckeck if a policy prohibits this OSD from being added to the list.
                    if (checkMatch(customNotFilter, filteredOSDs, osd)) {
                        continue;
                    }                   
                    else if (checkMatch(customFilter, filteredOSDs, osd)) {
                        filteredOSDs.addServices(osd);
                    }
                }
            }
        }
        
        return filteredOSDs;
    }

    private static boolean checkMatch(
            Map<String, String> customFilter, 
            ServiceSet.Builder filteredOSDs, 
            Service osd) {
        for (Entry<String, String> entry : customFilter.entrySet()) {
            String osdParameterValue 
                = KeyValuePairs.getValue(osd.getData().getDataList(),
                    ServiceConfig.OSD_CUSTOM_PROPERTY_PREFIX + entry.getKey());

            if (matches(entry.getValue(), osdParameterValue)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void setAttribute(String key, String value) {
        if (OFFLINE_TIME_SECS.equals(key)) {
            maxOfflineTime = Long.parseLong(value);
        }
        else if (FREE_CAPACITY_BYTES.equals(key)) {
            minFreeCapacity = Long.parseLong(value);
        }
        else if (OSD_HEALTH_CHECK.equals(key)){
            if (value.toUpperCase().equals("WARNING")) {
                osdHealthCheck = OSDHealthResult.OSD_HEALTH_RESULT_WARNING;
            } else if (value.toUpperCase().equals("FAILED")) {
                osdHealthCheck = OSDHealthResult.OSD_HEALTH_RESULT_FAILED;
            }
        }
        else {
            if (value == null) {
                if (key.toLowerCase().startsWith(NOT_IN)) {
                    key = key.substring(NOT_IN.length(), key.length());
                    customNotFilter.remove(key);
                } 
                else {
                    customFilter.remove(key);
                }
            }
            else {
                if (key.toLowerCase().startsWith(NOT_IN)) {                    
                    key = key.substring(NOT_IN.length(), key.length());
                    customNotFilter.put(key,value);
                } 
                else {                
                    customFilter.put(key, value);
                }
            }
        }
    }
    
    private boolean hasTimedOut(Service osd) {
        long lastUpdate = Long.parseLong(KeyValuePairs.getValue(osd.getData().getDataList(),
                "seconds_since_last_update"));
        return lastUpdate > maxOfflineTime;
    }
    
    private boolean hasFreeCapacity(Service osd) {
        String freeStr = KeyValuePairs.getValue(osd.getData().getDataList(), "free");
        if (freeStr == null) {
            if (Logging.isDebug()) {
                Logging.logMessage(Logging.LEVEL_DEBUG, this, "invalid OSD registry (free is null!): %s",
                        osd.toString());
            }
            return false;
        }
        long free = Long.parseLong(freeStr);
        return free > minFreeCapacity;
    }
    
    private boolean isAvailable(Service osd) {
        String osdStatus = KeyValuePairs.getValue(osd.getData().getDataList(), HeartbeatThread.STATUS_ATTR);
        if (osdStatus == null)
            return true;
        if (Integer.valueOf(osdStatus) == ServiceStatus.SERVICE_STATUS_AVAIL.getNumber()) {
            return true;
        } else {
            return false;
        }
    }
    
    private boolean isHealthy(Service osd) {
        String smartTestResult = KeyValuePairs.getValue(osd.getData().getDataList(), OSD_HEALTH_CHECK);
        if (smartTestResult == null) {
            return true;
        }

        if (osdHealthCheck == OSDHealthResult.OSD_HEALTH_RESULT_WARNING) {
            return Integer.valueOf(smartTestResult) != OSDHealthResult.OSD_HEALTH_RESULT_FAILED_VALUE
                    && Integer.valueOf(smartTestResult) != OSDHealthResult.OSD_HEALTH_RESULT_WARNING_VALUE;
        } else {
            return Integer.valueOf(smartTestResult) != OSDHealthResult.OSD_HEALTH_RESULT_FAILED_VALUE;
        }
    }
    
    private static boolean matches(String filterString, String customProperty) {
        
        StringTokenizer st = new StringTokenizer(filterString);
        while (st.hasMoreTokens()) {
            if (st.nextToken().equals(customProperty))
                return true;
        }
        
        return false;
    }
}
