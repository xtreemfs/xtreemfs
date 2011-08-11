package org.xtreemfs.common.monitoring;

import javax.management.MBeanServer;

import org.xtreemfs.common.monitoring.generatedcode.Mrc;

import com.sun.management.snmp.agent.SnmpMib;


/**
 * This class represents the monitoring information exposed by the SNMP agent regarding to the
 * MRC Service.
 *
 * <br>May 12, 2011
 *
 * @author bzcseife
 *
 */
@SuppressWarnings("serial")
public class MrcImpl extends Mrc {

    
    
    
    public MrcImpl(SnmpMib myMib, StatusMonitor statusMonitor) {
        super(myMib);
        
        VolumeCount = 0;
        
        // Set a reference to this Object to be able to access it within
        // the application
        statusMonitor.setMrcGroup(this);
        
    }
    
    public MrcImpl(SnmpMib myMib, MBeanServer server, StatusMonitor statusMonitor) {
        super(myMib, server);
        
        VolumeCount = 0;
        
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
}
