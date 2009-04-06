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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.mrc.metadata;

import java.util.Iterator;

/**
 * Interface for accessing an X-Locations List. X-Locations Lists contain
 * information about storage locations (OSDs) used for different replicas of a
 * file.
 */
public interface XLocList {
    
    /**
     * Returns the number of replicas currently stored in the X-Locations List.
     * 
     * @return the number of replicas
     */
    public int getReplicaCount();
    
    /**
     * Returns the replica at the given index position.
     * 
     * @param index
     *            position for the replica to return
     * @return the replica at position <code>index</code>
     */
    public XLoc getReplica(int index);
    
    /**
     * Returns the version number associated with the X-Locations List.
     * 
     * @return the version number
     */
    public int getVersion();
    
    /**
     * Returns an iterator for all replicas.
     * 
     * @return an iterator
     */
    public Iterator<XLoc> iterator();
    
    /**
     * Returns the replica update policy.
     * 
     * @return the replica update policy
     */
    public String getReplUpdatePolicy();
    
}