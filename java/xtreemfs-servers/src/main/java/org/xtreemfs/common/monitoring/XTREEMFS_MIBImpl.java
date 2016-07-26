/*
 * Copyright (c) 2008-2011 by Paul Seiferth,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.monitoring;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.xtreemfs.common.monitoring.generatedcode.XTREEMFS_MIB;

@SuppressWarnings("serial")
public class XTREEMFS_MIBImpl extends XTREEMFS_MIB {

    private StatusMonitor statusMonitor;

    /**
     * Defines what kind of service is created the snmp agent
     */

    public XTREEMFS_MIBImpl(StatusMonitor statusMonitor) {
        super();
        this.statusMonitor = statusMonitor;
    }

    // Overwrite this method in order to ensure that your own Dir implementation is used.
    @Override
    protected Object createDirMBean(String groupName, String groupOid, ObjectName groupObjname,
            MBeanServer server) {
        // if this runs in a DIR Service, return customized Dir group. Otherwise return the default
        // one created method of the super-class
        if (server != null) {
            return new DirImpl(this, server, statusMonitor);
        } else {
            return new DirImpl(this, statusMonitor);
        }
    }

    // Overwrite this method in order to ensure that your own General implementation is used.
    @Override
    protected Object createGeneralMBean(String groupName, String groupOid, ObjectName groupObjname,
            MBeanServer server) {
        if (server != null) {
            return new GeneralImpl(this, server, statusMonitor);
        } else {
            return new GeneralImpl(this, statusMonitor);
        }
    }

    // Overwrite this method in order to ensure that your own Mrc implementation is used.
    @Override
    protected Object createMrcMBean(String groupName, String groupOid, ObjectName groupObjname,
            MBeanServer server) {
        // if this runs in a DIR Service, return customized Mrc group. Otherwise return the default
        // one created method of the super-class
        if (server != null) {
            return new MrcImpl(this, server, statusMonitor);
        } else {
            return new MrcImpl(this, statusMonitor);
        }
    }

    // Overwrite this method in order to ensure that your own Osd implementation is used.
    @Override
    protected Object createOsdMBean(String groupName, String groupOid, ObjectName groupObjname,
            MBeanServer server) {
        // if this runs in a DIR Service, return customized Dir group. Otherwhise return the default
        // one created method of the super-class
        if (server != null) {
            return new OsdImpl(this, server, statusMonitor);
        } else {
            return new OsdImpl(this, statusMonitor);
        }

    }

}
