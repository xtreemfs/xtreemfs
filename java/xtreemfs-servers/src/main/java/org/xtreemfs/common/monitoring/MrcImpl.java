/*
 * Copyright (c) 2008-2011 by Paul Seiferth,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.monitoring;

import javax.management.MBeanServer;

import org.xtreemfs.common.monitoring.StatusMonitor.ServiceTypes;
import org.xtreemfs.common.monitoring.generatedcode.Mrc;

import com.sun.management.snmp.SnmpStatusException;
import com.sun.management.snmp.agent.SnmpMib;


/**
 * This class represents the monitoring information exposed by the SNMP agent regarding to the
 * MRC Service.
 */
@SuppressWarnings("serial")
public class MrcImpl extends Mrc {

    
    StatusMonitor statusMonitor = null;
    
    public MrcImpl(SnmpMib myMib, StatusMonitor statusMonitor) {
        super(myMib);

        VolumeCount = 0;
        
        this.statusMonitor = statusMonitor;
        // Set a reference to this Object to be able to access it within
        // the application
        statusMonitor.setMrcGroup(this);
    }
    
    public MrcImpl(SnmpMib myMib, MBeanServer server, StatusMonitor statusMonitor) {
        super(myMib, server);
        
        VolumeCount = 0;
        
        this.statusMonitor = statusMonitor;
        // Set a reference to this Object to be able to access it within
        // the application
        statusMonitor.setMrcGroup(this);
    }
    
    
    
    protected void volumeCreated() {
        VolumeCount++;
    }
    
    protected void volumeDeleted() {
        VolumeCount--;
    }
    
    @Override
    public Integer getVolumeCount() throws SnmpStatusException {
        if(!statusMonitor.getInitiatingService().equals(ServiceTypes.MRC)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        return VolumeCount;
    }
}
