package org.xtreemfs.common.monitoring;

import javax.management.MBeanServer;

import org.xtreemfs.common.monitoring.generatedcode.Dir;




/**
 * This class represents the monitoring information exposed by the SNMP agent regarding to the
 * Directory Service.
 *
 * <br>May 5, 2011
 * 
 * @author bzcseife
 */
@SuppressWarnings("serial")
public class DirImpl extends Dir {
        
    public DirImpl(XTREEMFS_MIBImpl myMib, StatusMonitor statusMonitor) {
        super(myMib);
        
        AddressMappingCount = 0;
        ServiceCount = 0;
    
        // Set a reference to this Object to be able to access it within
        // the application
        statusMonitor.setDirGroup(this);
        
    }

    public DirImpl(XTREEMFS_MIBImpl myMib, MBeanServer server, StatusMonitor statusMonitor) {
        super(myMib, server);
        
        AddressMappingCount = 0;
        ServiceCount = 0;
        
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
        
}
