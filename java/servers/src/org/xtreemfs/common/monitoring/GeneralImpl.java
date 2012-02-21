/*
 * Copyright (c) 2008-2011 by Paul Seiferth,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.monitoring;

import javax.management.MBeanServer;

import org.xtreemfs.babudb.BabuDBFactory;
import org.xtreemfs.common.HeartbeatThread;
import org.xtreemfs.common.monitoring.StatusMonitor.ServiceTypes;
import org.xtreemfs.common.monitoring.generatedcode.General;
import org.xtreemfs.pbrpc.generatedinterfaces.DIRServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.MRCServiceConstants;
import org.xtreemfs.pbrpc.generatedinterfaces.OSDServiceConstants;

import com.sun.management.snmp.SnmpStatusException;

/**
 * This class represents the monitoring information exposed by the SNMP agent
 * regarding to all services.
 * 
 * <br>
 * May 5, 2011
 * 
 * @author bzcseife
 */
@SuppressWarnings("serial")
public class GeneralImpl extends General {
    
    private StatusMonitor statusMonitor;
    
    public GeneralImpl(XTREEMFS_MIBImpl myMib, StatusMonitor statusMonitor) {
        super(myMib);
        
        this.statusMonitor = statusMonitor;
    }
    
    public GeneralImpl(XTREEMFS_MIBImpl myMib, MBeanServer server, StatusMonitor statusMonitor) {
        super(myMib, server);
        
        this.statusMonitor = statusMonitor;
        
    }
    
    @Override
    public Long getJvmMaxMemory() throws SnmpStatusException {
        return Runtime.getRuntime().maxMemory();
    }
    
    @Override
    public Long getJvmUsedMemory() throws SnmpStatusException {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }
    
    @Override
    public Long getJvmFreeMemory() throws SnmpStatusException {
        return Runtime.getRuntime().freeMemory();
    }
    
    @Override
    public String getDatabaseVersion() throws SnmpStatusException {
        return BabuDBFactory.BABUDB_VERSION;
    }
    
    @Override
    public Integer getTcpPort() throws SnmpStatusException {
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.DIR)) {
            return statusMonitor.getDirConfig().getPort();
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.MRC)) {
            return statusMonitor.getMrcConfig().getPort();
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            return statusMonitor.getOsdConfig().getPort();
        }
        return -1;
    }
    
    @Override
    public Integer getDebugLevel() throws SnmpStatusException {
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.DIR)) {
            return statusMonitor.getDirConfig().getDebugLevel();
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.MRC)) {
            return statusMonitor.getMrcConfig().getDebugLevel();
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            return statusMonitor.getOsdConfig().getDebugLevel();
        }
        throw new SnmpStatusException("Internal error. Couldn't fetch values.");
    }
    
    @Override
    public Integer getRpcInterface() throws SnmpStatusException {
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.DIR)) {
            return DIRServiceConstants.INTERFACE_ID;
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.MRC)) {
            return MRCServiceConstants.INTERFACE_ID;
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            return OSDServiceConstants.INTERFACE_ID;
        }
        throw new SnmpStatusException("Internal error. Couldn't fetch values.");
    }
    
    @Override
    public Integer getNumClientConnections() throws SnmpStatusException {
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.DIR)) {
            return statusMonitor.getMasterDIR().getNumConnections();
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.MRC)) {
            return statusMonitor.getMasterMRC().getNumConnections();
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            return statusMonitor.getMasterOSD().getNumClientConnections();
        }
        throw new SnmpStatusException("Internal error. Couldn't fetch values.");
    }
    
    @Override
    public Long getNumPendingRequests() throws SnmpStatusException {
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.DIR)) {
            return statusMonitor.getMasterDIR().getNumRequests();
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.MRC)) {
            return statusMonitor.getMasterMRC().getNumRequests();
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            return statusMonitor.getMasterOSD().getPendingRequests();
        }
        throw new SnmpStatusException("Internal error. Couldn't fetch values.");
    }
    
    @Override
    public Long getCurrentTime() {
        return System.currentTimeMillis();
    }
    
    @Override
    public String getServiceType() throws SnmpStatusException {
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.DIR)) {
            return "DIR";
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.MRC)) {
            return "MRC";
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            return "OSD";
        }
        throw new SnmpStatusException("Internal error. Couldn't fetch values.");
    }
    
    @Override
    public String getIsRunning() throws SnmpStatusException {
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.DIR)) {
            // TODO: Since the DIR don't have a heartbeatthread find a method to
            // determine if
            // the DIR is still alive.
            return "ONLINE";
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.MRC)) {
            // calculate the difference between the last hearbeat an the current
            // time. If these values
            // differ too much, return OFFLINE.
            long difference = System.currentTimeMillis() - statusMonitor.getMasterMRC().getLastHeartbeat();
            if (difference > 10 * HeartbeatThread.UPDATE_INTERVAL) {
                return "OFFLINE";
            } else {
                return "ONLINE";
            }
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            // calculate the difference between the last hearbeat an the current
            // time. If these values
            // differ too much, return OFFLINE.
            long difference = System.currentTimeMillis() - statusMonitor.getMasterOSD().getLastHeartbeat();
            if (difference > 10 * HeartbeatThread.UPDATE_INTERVAL) {
                return "OFFLINE";
            } else {
                return "ONLINE";
            }
            
        }
        throw new SnmpStatusException("Internal error. Couldn't fetch values.");
    }
    
    @Override
    public String getServiceUUID() throws SnmpStatusException {
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.DIR)) {
            return statusMonitor.getDirConfig().getUUID().toString();
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.MRC)) {
            return statusMonitor.getMrcConfig().getUUID().toString();
        }
        
        if (statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            return statusMonitor.getOsdConfig().getUUID().toString();
        }
        throw new SnmpStatusException("Internal error. Couldn't fetch values.");
    }
    
}