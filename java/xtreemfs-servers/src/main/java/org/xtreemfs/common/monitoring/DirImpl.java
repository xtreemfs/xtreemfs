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
import org.xtreemfs.common.monitoring.generatedcode.Dir;

import com.sun.management.snmp.SnmpStatusException;




/**
 * This class represents the monitoring information exposed by the SNMP agent regarding to the
 * Directory Service.
 *
 */
@SuppressWarnings("serial")
public class DirImpl extends Dir {
        
    private StatusMonitor statusMonitor;
    
    public DirImpl(XTREEMFS_MIBImpl myMib, StatusMonitor statusMonitor) {
        super(myMib);
        
        AddressMappingCount = 0;
        ServiceCount = 0;
        
        this.statusMonitor = statusMonitor;
        // Set a reference to this Object to be able to access it within
        // the application
        statusMonitor.setDirGroup(this);
    }

    public DirImpl(XTREEMFS_MIBImpl myMib, MBeanServer server, StatusMonitor statusMonitor) {
        super(myMib, server);
      
        AddressMappingCount = 0;
        ServiceCount = 0;
        
        this.statusMonitor = statusMonitor;
        // Set a reference to this Object to be able to access it within
        // the application
        statusMonitor.setDirGroup(this);
    }    
    
    
    /**
     * This method will be called when a AddressMapping is registered at the DIR.
     **/
    protected void addressMappingAdded() {
        AddressMappingCount++;       
    }
    
    /**
     * This method will be called when a AddressMapping is deregistered at the DIR.
     **/
    protected void addressMappingDeleted() {
        AddressMappingCount--;
    }
    
    /**
     * This method will be called when a Service is registered at the DIR.
     **/
    protected void serviceRegistered() {
        ServiceCount++;
    }
    
    /**
     * This method will be called when a Service is deregistered at the DIR.
     **/
    protected void serviceDeregistered() {
        ServiceCount--;
    }
    
    @Override
    public Integer getServiceCount() throws SnmpStatusException {
        if (!statusMonitor.getInitiatingService().equals(ServiceTypes.DIR)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        return ServiceCount;
    }

    @Override
    public Integer getAddressMappingCount() throws SnmpStatusException {
        if (!statusMonitor.getInitiatingService().equals(ServiceTypes.DIR)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        return AddressMappingCount;
    }
        
}
