/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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

import org.xtreemfs.interfaces.ServiceSet;
import org.xtreemfs.interfaces.VivaldiCoordinates;
import org.xtreemfs.mrc.metadata.XLocList;

/**
 * Interface for policies implementing a selection mechanism for OSDs.
 * 
 * @author stender
 */
public interface OSDSelectionPolicy {
    
    /**
     * Selects a list of OSDs.
     * 
     * @param allOSDs
     *            a list of all available OSDs
     * @param clientIP
     *            the client's IP address
     * @param clientCoords
     *            the client's Vivaldi coordinates
     * @param currentXLoc
     *            the current X-Locations list
     * @param numOSDs
     *            the number of OSDs to select
     * @return a list of selected OSDs
     */
    public ServiceSet getOSDs(ServiceSet allOSDs, InetAddress clientIP, VivaldiCoordinates clientCoords,
        XLocList currentXLoc, int numOSDs);
    
    /**
     * Simplified version of
     * <code>getOSDs(ServiceSet allOSDs, InetAddress clientIP, XLocList currentXLoc, int numOSDs)</code>
     * . This method will be invoked by the framework if no context is
     * available.
     * 
     * @param allOSDs
     *            a list of all available OSDs
     * @return a list of selected OSDs
     */
    public ServiceSet getOSDs(ServiceSet allOSDs);
    
    /**
     * Sets a new policy attribute. This method is invoked each time a
     * policy-related extended attribute is set.
     * 
     * @param key
     *            the attribute key
     * @param value
     *            the attribute value
     */
    public void setAttribute(String key, String value);
    
}
