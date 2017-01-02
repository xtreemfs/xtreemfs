/*
 * Copyright (c) 2009-2011 by Jan Stender, Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.osdselection;

import java.net.InetAddress;

import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceSet;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.VivaldiCoordinates;

/**
 * Interface for policies implementing a selection mechanism for OSDs.
 *
 * @author stender
 */
public interface OSDSelectionPolicy {

    /**
     * Selects a list of OSDs.
     *
     * @param allOSDs      a list of all available OSDs
     * @param clientIP     the client's IP address
     * @param clientCoords the client's Vivaldi coordinates
     * @param currentXLoc  the current X-Locations list
     * @param numOSDs      the number of OSDs required in a valid group. This
     *                     is only relevant for grouping and will be ignored
     *                     by filtering and sorting policies.
     * @param path         the path of the file.
     *                     might be null if the path is
     *                     currently unavailable.
     * @return a list of selected OSDs
     */
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs,
                                      InetAddress clientIP,
                                      VivaldiCoordinates clientCoords,
                                      XLocList currentXLoc,
                                      int numOSDs,
                                      String path);

    /**
     * Simplified version of
     * <code>getOSDs(ServiceSet allOSDs, InetAddress clientIP, XLocList
     * currentXLoc, int numOSDs)</code>.
     * This method will be invoked by the framework if no context is
     * available e.g., when displaying
     * the list of suitable OSDs in the webinterface or a maintenance tool.
     *
     * @param allOSDs a list of all available OSDs
     * @return a list of selected OSDs
     */
    public ServiceSet.Builder getOSDs(ServiceSet.Builder allOSDs);

    /**
     * Sets a new policy attribute. This method is invoked each time a
     * policy-related extended attribute is set.
     *
     * @param key   the attribute key
     * @param value the attribute value
     */
    public void setAttribute(String key, String value);

}
