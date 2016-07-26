/*
 * Copyright (c) 2008-2011 by Paul Seiferth,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.monitoring;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.xtreemfs.common.monitoring.generatedcode.XTREEMFS_MIB;
import org.xtreemfs.dir.DIRConfig;
import org.xtreemfs.dir.DIRRequestDispatcher;
import org.xtreemfs.dir.DIRStatusListener;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.mrc.MRCConfig;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.MRCStatusListener;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.OSDStatusListener;

import com.sun.management.comm.SnmpAdaptorServer;
import com.sun.management.snmp.IPAcl.JdmkAcl;

/**
 * 
 * 
 * <br>
 * May 19, 2011
 * 
 * @author bzcseife
 */
public class StatusMonitor implements DIRStatusListener, MRCStatusListener, OSDStatusListener {

    protected static enum ServiceTypes {
        DIR, MRC, OSD
    }

    private DIRConfig            dirConfig         = null;
    private MRCConfig            mrcConfig         = null;
    private OSDConfig            osdConfig         = null;

    private DIRRequestDispatcher masterDIR         = null;
    private MRCRequestDispatcher masterMRC         = null;
    private OSDRequestDispatcher masterOSD         = null;

    /**
     * The {@link SnmpAdaptorServer} representing the SNMP adaptor.
     */
    private SnmpAdaptorServer    snmpAdaptor       = null;

    /**
     * The MIB containing all SNMP related information
     */
    private XTREEMFS_MIB         xtfsmib           = null;

    /**
     * Reference to the DIR Implementation to access it
     */
    private DirImpl              dirGroup          = null;

    /**
     * Reference to the MRC implementation to access it
     */
    private MrcImpl              mrcGroup          = null;

    /**
     * Reference to the OSD implementation to access it
     */
    private OsdImpl              osdGroup          = null;

    /**
     * Saves what kind of service (DIR, MRC or OSD) uses this class. This is used in the General Group to know
     * where to get specific information.
     */
    private ServiceTypes         initiatingService = null;

    /**
     * Constructor when this class is running inside a DIR.
     * 
     * @param dirReqDisp
     * @param port
     */
    public StatusMonitor(DIRRequestDispatcher dirReqDisp, int port) {
        this(ServiceTypes.DIR, null, port, null);
        this.masterDIR = dirReqDisp;
    }

    /**
     * Constructor when this class is running inside a DIR.
     * 
     * @param dirReqDisp
     * @param addr
     * @param port
     */
    public StatusMonitor(DIRRequestDispatcher dirReqDisp, InetAddress addr, int port) {
        this(ServiceTypes.DIR, addr, port, null);
        this.masterDIR = dirReqDisp;
    }

    /**
     * Constructor when this class is running inside a DIR.
     * 
     * @param dirReqDisp
     * @param addr
     * @param port
     */
    public StatusMonitor(DIRRequestDispatcher dirReqDisp, int port, String aclFile) {
        this(ServiceTypes.DIR, null, port, aclFile);
        this.masterDIR = dirReqDisp;
    }

    /**
     * Constructor when this class is running inside a DIR.
     * 
     * @param dirReqDisp
     * @param addr
     * @param port
     */
    public StatusMonitor(DIRRequestDispatcher dirReqDisp, InetAddress addr, int port, String aclFile) {
        this(ServiceTypes.DIR, addr, port, aclFile);
        this.masterDIR = dirReqDisp;
    }

    /**
     * Constructor when this class is running inside a MRC.
     * 
     * @param mrcReqDisp
     * @param port
     */
    public StatusMonitor(MRCRequestDispatcher mrcReqDisp, int port) {
        this(ServiceTypes.MRC, null, port, null);
        this.masterMRC = mrcReqDisp;
    }

    /**
     * Constructor when this class is running inside a MRC.
     * 
     * @param mrcReqDisp
     * @param addr
     * @param port
     */
    public StatusMonitor(MRCRequestDispatcher mrcReqDisp, InetAddress addr, int port) {
        this(ServiceTypes.MRC, addr, port, null);
        this.masterMRC = mrcReqDisp;
    }

    /**
     * Constructor when this class is running inside a MRC.
     * 
     * @param mrcReqDisp
     * @param port
     */
    public StatusMonitor(MRCRequestDispatcher mrcReqDisp, int port, String aclFile) {
        this(ServiceTypes.MRC, null, port, aclFile);
        this.masterMRC = mrcReqDisp;
    }

    /**
     * Constructor when this class is running inside a MRC.
     * 
     * @param mrcReqDisp
     * @param addr
     * @param port
     */
    public StatusMonitor(MRCRequestDispatcher mrcReqDisp, InetAddress addr, int port, String aclFile) {
        this(ServiceTypes.MRC, addr, port, aclFile);
        this.masterMRC = mrcReqDisp;
    }

    /**
     * Constructor when this class is running inside an OSD.
     * 
     * @param osdReqDisp
     * @param port
     */
    public StatusMonitor(OSDRequestDispatcher osdReqDisp, int port) {
        this(ServiceTypes.OSD, null, port, null);
        this.masterOSD = osdReqDisp;
    }

    /**
     * Constructor when this class is running inside an OSD.
     * 
     * @param osdReqDisp
     * @param addr
     * @param port
     */
    public StatusMonitor(OSDRequestDispatcher osdReqDisp, InetAddress addr, int port) {
        this(ServiceTypes.OSD, addr, port, null);
        this.masterOSD = osdReqDisp;
    }

    /**
     * Constructor when this class is running inside an OSD.
     * 
     * @param osdReqDisp
     * @param port
     * @param aclFile
     */
    public StatusMonitor(OSDRequestDispatcher osdReqDisp, int port, String aclFile) {
        this(ServiceTypes.OSD, null, port, aclFile);
        this.masterOSD = osdReqDisp;
    }

    /**
     * Constructor when this class is running inside an OSD.
     * 
     * @param osdReqDisp
     * @param port
     * @param aclFile
     */
    public StatusMonitor(OSDRequestDispatcher osdReqDisp, InetAddress addr, int port, String aclFile) {
        this(ServiceTypes.OSD, addr, port, aclFile);
        this.masterOSD = osdReqDisp;
    }

    private StatusMonitor(ServiceTypes type, InetAddress addr, int port, String aclFile) {

        initiatingService = type;

        JdmkAcl acl = null;
        if (aclFile != null) {
            // JdmkACL specifies the ACL file.
            try {
                acl = new JdmkAcl("Xtreemfs ACL", aclFile);
            } catch (IllegalArgumentException iae) {
                Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.misc, this,
                        "ACL file problem. The file %s is not a valid ACL file or did not exist.", aclFile);
            } catch (UnknownHostException uhe) {
                Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.misc, this, "", uhe.getMessage());
            }
        }

        // if there is no ACL File everyone who can access the the network can read the information
        // exposed by SNMP from everywhere within the network!
        if (acl == null) {
            Logging.logMessage(Logging.LEVEL_NOTICE, Logging.Category.misc, this,
                    "SNMP agen will start without a ACL file. Everyone on your network can access the "
                            + "information exposed by the SNMP agent!");
        }

        // create and start the SNMP adaptor
        if (addr != null) {
            snmpAdaptor = new SnmpAdaptorServer(acl, port, addr);
        } else {
            snmpAdaptor = new SnmpAdaptorServer(acl, port);
        }
        snmpAdaptor.start();

        // send a coldstart trap; every SNMP agent should do this on initialization
        try {
            snmpAdaptor.setTrapPort(port + 1);
            snmpAdaptor.snmpV1Trap(0, 0, null);

            Logging.logMessage(Logging.LEVEL_INFO, Logging.Category.misc, this,
                    "SNMP agent started at port %s", port);

        } catch (Exception e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.misc, this,
                    "Failed to start SNMP agent at port %s", port);
        }

        xtfsmib = new XTREEMFS_MIBImpl(this);

        try {
            xtfsmib.init();
        } catch (IllegalAccessException e) {
            Logging.logMessage(Logging.LEVEL_ERROR, Logging.Category.misc, this,
                    "Failed to start SNMP agent: %s", e.getMessage());

        }

        xtfsmib.setSnmpAdaptor(snmpAdaptor);

    }

    protected ServiceTypes getInitiatingService() {
        return initiatingService;
    }

    protected DIRConfig getDirConfig() {
        return dirConfig;
    }

    protected MRCConfig getMrcConfig() {
        return mrcConfig;
    }

    protected OSDConfig getOsdConfig() {
        return osdConfig;
    }

    public DIRRequestDispatcher getMasterDIR() {
        return masterDIR;
    }

    public MRCRequestDispatcher getMasterMRC() {
        return masterMRC;
    }

    public OSDRequestDispatcher getMasterOSD() {
        return masterOSD;
    }

    // Methods of the DIR Listener Interface.
    @Override
    public void addressMappingAdded() {
        dirGroup.addressMappingAdded();
    }

    @Override
    public void addressMappingDeleted() {
        dirGroup.addressMappingDeleted();
    }

    @Override
    public void DIRConfigChanged(DIRConfig config) {
        this.dirConfig = config;
    }

    @Override
    public void serviceRegistered() {
        this.dirGroup.serviceRegistered();
    }

    @Override
    public void serviceDeregistered() {
        this.dirGroup.serviceDeregistered();
    }

    // MIB Groups registrations
    /**
     * This method is used by {@link DirImpl} constructor to make itself accessible by this class.
     * 
     * @param DirImpl
     */
    protected void setDirGroup(DirImpl dirGroup) {
        this.dirGroup = dirGroup;
    }

    /**
     * This method is used by {@link MRCImpl} constructor to make itself accessible by this class.
     * 
     * @param MrcImpl
     */
    protected void setMrcGroup(MrcImpl mrcGroup) {
        this.mrcGroup = mrcGroup;
    }

    /**
     * This method is used by {@link OsdImpl} constructor to make itself accessible by this class.
     * 
     * @param OsdImpl
     */
    protected void setOsdGroup(OsdImpl osdGroup) {
        this.osdGroup = osdGroup;
    }

    //
    // MRCListener related functions
    @Override
    public void MRCConfigChanged(MRCConfig config) {
        this.mrcConfig = config;

    }

    @Override
    public void volumeCreated() {
        mrcGroup.volumeCreated();
    }

    @Override
    public void volumeDeleted() {
        mrcGroup.volumeDeleted();
    }

    //
    // OSDListener related functions
    @Override
    public void OSDConfigChanged(OSDConfig config) {
        this.osdConfig = config;
    }

    @Override
    public void numBytesTXChanged(long numBytesTX) {
        osdGroup.setNumBytesTX(numBytesTX);
    }

    @Override
    public void numBytesRXChanged(long numBytesRX) {
        osdGroup.setNumBytesRX(numBytesRX);
    }

    @Override
    public void numReplBytesRXChanged(long numReplBytesRX) {
        osdGroup.setNumReplBytesRX(numReplBytesRX);
    }

    @Override
    public void numObjsTXChanged(long numObjsTX) {
        osdGroup.setNumObjsTX(numObjsTX);
    }

    @Override
    public void numObjsRXChanged(long numObjsRX) {
        osdGroup.setNumObjsRX(numObjsRX);
    }

    @Override
    public void numReplObjsRX(long numReplObjsRX) {
        osdGroup.setNumReplObjsRX(numReplObjsRX);
    }

    @Override
    public void shuttingDown() {
        snmpAdaptor.stop();
    }
}
