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

import java.net.InetAddress;
import org.xtreemfs.interfaces.ServiceRegistrySet;

/**
 * Interface for policies implementing a selection mechanism for OSDs.
 *
 * @author bjko, stender
 */
public interface OSDSelectionPolicy {

    /**
     * Returns the subset of all registered OSDs that match the policy.
     *
     * @param osds
     *            osds is a map containing osd info registered with the
     *            directory service
     * @return the filtered list
     */
    public ServiceRegistrySet getUsableOSDs(
        ServiceRegistrySet osds, String args);

    /**
     * Returns a list of OSDs to be allocated to a newly created file.
     *
     * @param osdMap
     *            list of osds that match the policy
     * @return a list of osds that can be used to create a new file
     */
    public String[] getOSDsForNewFile(ServiceRegistrySet osds,
        InetAddress clientAddr, int amount, String args);

}
