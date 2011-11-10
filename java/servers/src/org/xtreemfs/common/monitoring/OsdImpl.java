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
import org.xtreemfs.common.monitoring.generatedcode.Osd;

import com.sun.management.snmp.SnmpStatusException;


/**
 * This class represents the monitoring information exposed by the SNMP agent regarding to the
 * OSD.
 *
 */
@SuppressWarnings("serial")
public class OsdImpl extends Osd {

    private StatusMonitor statusMonitor;
    
    public OsdImpl(XTREEMFS_MIBImpl myMib, StatusMonitor statusMonitor) {
        super(myMib);
        this.statusMonitor = statusMonitor;
        
        // Set a reference to this Object to be able to access it within
        // the application
        statusMonitor.setOsdGroup(this);
    }
   
    public OsdImpl(XTREEMFS_MIBImpl myMib, MBeanServer server, StatusMonitor statusMonitor) {
        super(myMib, server);
        this.statusMonitor = statusMonitor;
        
        // Set a reference to this Object to be able to access it within
        // the application
        statusMonitor.setOsdGroup(this);
    }
    
    /**
     * Setter for NumBytexTX(number of bytes transmitted)
     */
    public void setNumBytesTX(long numBytesTX) {
        NumBytesTX = numBytesTX;
    }
    
    /**
     * Setter for NumBytesRX(number of bytes received)
     */
    public void setNumBytesRX(long numBytesRX) {
        NumBytesRX = numBytesRX;
    }
    
    /**
     * Setter for NumReplBytesRX(number of bytes received which have to do something with replication)
     */
    public void setNumReplBytesRX(long numReplBytesRX) {
        NumReplBytesRX = numReplBytesRX;
    }
    
    /**
     * Setter for NumObjsTX(number of objects transmitted)
     */
    public void setNumObjsTX(long numObjsTX) {
        NumObjsTX = numObjsTX;
    }
    
    /**
     * Setter for NumObjsRX(number of objects received)
     */
    public void setNumObjsRX(long numObjsRX) {
        NumObjsRX = numObjsRX;
    }
    
    /**
     * Setter for getNumReplObjsRX(number of objects received which have to do something with
     * replication
     */
    public void setNumReplObjsRX(long numReplObjsRX) {
        NumReplObjsRX  = numReplObjsRX;
    }
    
    @Override
    public Integer getPreprocStageQueueLength() throws SnmpStatusException {
        if (!statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        if (statusMonitor.getMasterOSD() != null) {
            return statusMonitor.getMasterOSD().getPreprocStage().getQueueLength();
        } 
        return -1;
    }
    
    @Override
    public Integer getStorageStageQueueLength() throws SnmpStatusException {
        if (!statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        if (statusMonitor.getMasterOSD() != null) {
            return statusMonitor.getMasterOSD().getStorageStage().getQueueLength();
        }
        return -1;
    }
    
    @Override
    public Integer getDeletionStageQueueLength() throws SnmpStatusException {
        if (!statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        if (statusMonitor.getMasterOSD() != null) {
            return statusMonitor.getMasterOSD().getDeletionStage().getQueueLength();
        }
        return -1;
    }
    
    @Override
    public Long getFreeSpace() throws SnmpStatusException {
        if (!statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        if (statusMonitor.getMasterOSD() != null) {
            return statusMonitor.getMasterOSD().getFreeSpace();
        }
        return -1l;
    }
    
    @Override
    public Integer getNumOpenFiles() throws SnmpStatusException {
        if (!statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        if (statusMonitor.getMasterOSD() != null) {
            return statusMonitor.getMasterOSD().getPreprocStage().getNumOpenFiles();
        }
        return -1;
    }
    
    @Override
    public Long getNumDeletedFiles() throws SnmpStatusException {
        if (!statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        if (statusMonitor.getMasterOSD() != null) {
            return statusMonitor.getMasterOSD().getDeletionStage().getNumFilesDeleted();
        }
        return -1l;
    }
    
    @Override
    public Long getNumBytesTX() throws SnmpStatusException {
        if (!statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        return NumBytesTX;
    }

    @Override
    public Long getNumBytesRX() throws SnmpStatusException {
        if (!statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        return NumBytesRX;
    }

    @Override
    public Long getNumReplBytesRX() throws SnmpStatusException {
        if (!statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        return NumReplBytesRX;
    }

    @Override
    public Long getNumObjsTX() throws SnmpStatusException {
        if (!statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        return NumObjsTX;
    }

    @Override
    public Long getNumReplObjsRX() throws SnmpStatusException {
        if (!statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        return NumReplObjsRX;
    }

    @Override
    public Long getNumObjsRX() throws SnmpStatusException {
        if (!statusMonitor.getInitiatingService().equals(ServiceTypes.OSD)) {
            throw new SnmpStatusException(SnmpStatusException.noSuchName);
        }
        return NumObjsRX;
    }

}
